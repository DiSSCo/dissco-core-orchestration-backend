package eu.dissco.orchestration.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.fge.jsonpatch.diff.JsonDiff;
import eu.dissco.orchestration.backend.domain.HandleType;
import eu.dissco.orchestration.backend.domain.MachineAnnotationService;
import eu.dissco.orchestration.backend.domain.MachineAnnotationServiceRecord;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiData;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiListWrapper;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.exception.KubernetesFailedException;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.exception.ProcessingFailedException;
import eu.dissco.orchestration.backend.properties.KubernetesProperties;
import eu.dissco.orchestration.backend.repository.MachineAnnotationServiceRepository;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.models.V1Deployment;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.transform.TransformerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MachineAnnotationServiceService {

  public static final String SUBJECT_TYPE = "MachineAnnotationService";
  public static final String DEPLOYMENT = "-deployment";

  private final HandleService handleService;
  private final KafkaPublisherService kafkaPublisherService;
  private final MachineAnnotationServiceRepository repository;
  private final AppsV1Api appsV1Api;
  private final CustomObjectsApi customObjectsApi;
  private final Configuration configuration;
  private final ObjectMapper mapper;
  private final KubernetesProperties kubernetesProperties;
  @Qualifier("yaml-mapper")
  private final ObjectMapper yamlMapper;

  private static String getName(MachineAnnotationServiceRecord masRecord) {
    return masRecord.pid().substring(masRecord.pid().indexOf('/') + 1).toLowerCase();
  }

  public JsonApiWrapper createMachineAnnotationService(MachineAnnotationService mas, String userId,
      String path)
      throws TransformerException, ProcessingFailedException {
    var handle = handleService.createNewHandle(HandleType.MACHINE_ANNOTATION_SERVICE);
    var masRecord = new MachineAnnotationServiceRecord(handle, 1, Instant.now(), userId, mas, null);
    repository.createMachineAnnotationService(masRecord);
    createDeployment(masRecord);
    publishCreateEvent(handle, masRecord);
    return wrapSingleResponse(handle, masRecord, path);
  }

  private void createDeployment(MachineAnnotationServiceRecord masRecord) {
    try {
      deployMasToCluster(masRecord, true);
    } catch (KubernetesFailedException e) {
      rollbackMasCreation(masRecord, false);
    }
  }

  private void deployMasToCluster(MachineAnnotationServiceRecord masRecord, boolean create)
      throws KubernetesFailedException {
    var name = getName(masRecord);
    var templateProperties = getTemplateProperties(masRecord, name);
    try {
      var deploymentString = fillTemplate(templateProperties);
      var deployment = yamlMapper.readValue(deploymentString.toString(), V1Deployment.class);
      var custom = customObjectsApi.createClusterCustomObject()
      V1Deployment result = null;
      if (create) {
        result = appsV1Api.createNamespacedDeployment(kubernetesProperties.getMasNamespace(),
            deployment, null, null, null, null);
      } else {
        result = appsV1Api.replaceNamespacedDeployment(name + DEPLOYMENT,
            kubernetesProperties.getMasNamespace(), deployment, null, null, null, null);
      }
      log.info(result.toString());
    } catch (IOException | TemplateException e) {
      log.error("Failed to create deployment files for: {}", masRecord, e);
      throw new KubernetesFailedException("Failed to deploy to cluster");
    } catch (ApiException e) {
      log.error("Failed to deploy kubernetes deployment to cluster with code: {} and message: {}",
          e.getCode(), e.getResponseBody());
      throw new KubernetesFailedException("Failed to deploy to cluster");
    }
  }

  private Map<String, Object> getTemplateProperties(MachineAnnotationServiceRecord masRecord,
      String name) {
    var map = new HashMap<String, Object>();
    map.put("image", masRecord.mas().containerImage());
    map.put("imageTag", masRecord.mas().containerTag());
    map.put("name", name);
    return map;
  }

  private StringWriter fillTemplate(Map<String, Object> templateProperties)
      throws IOException, TemplateException {
    var writer = new StringWriter();
    var template = configuration.getTemplate("mas-template.ftl");
    template.process(templateProperties, writer);
    return writer;
  }

  private void publishCreateEvent(String handle, MachineAnnotationServiceRecord masRecord)
      throws ProcessingFailedException {
    try {
      kafkaPublisherService.publishCreateEvent(handle, mapper.valueToTree(masRecord), SUBJECT_TYPE);
    } catch (JsonProcessingException e) {
      log.error("Unable to publish message to Kafka", e);
      rollbackMasCreation(masRecord, true);
      throw new ProcessingFailedException("Failed to create new machine annotation service", e);
    }
  }

  private void rollbackMasCreation(MachineAnnotationServiceRecord masRecord,
      boolean rollbackDeployment) {
    handleService.rollbackHandleCreation(masRecord.pid());
    repository.rollbackMasCreation(masRecord.pid());
    if (rollbackDeployment) {
      try {
        appsV1Api.deleteNamespacedDeployment(getName(masRecord) + DEPLOYMENT,
            kubernetesProperties.getMasNamespace(), null, null, null, null, null, null);
      } catch (ApiException e) {
        log.error(
            "Fatal exception, unable to rollback kubernetes deployment for: {} error message with code: {} and message: {}",
            masRecord, e.getCode(), e.getResponseBody());
      }
    }
  }

  public JsonApiWrapper updateMachineAnnotationService(String id,
      MachineAnnotationService mas, String userId, String path)
      throws NotFoundException, ProcessingFailedException {
    var currentMasOptional = repository.getActiveMachineAnnotationService(id);
    if (currentMasOptional.isPresent()) {
      var currentMasRecord = currentMasOptional.get();
      if (mas.equals(currentMasRecord.mas())) {
        return null;
      } else {
        var newMasRecord = new MachineAnnotationServiceRecord(currentMasRecord.pid(),
            currentMasRecord.version() + 1, Instant.now(), userId, mas, null);
        repository.updateMachineAnnotationService(newMasRecord);
        updateDeployment(newMasRecord, currentMasRecord);
        publishUpdateEvent(newMasRecord, currentMasRecord);
        return wrapSingleResponse(newMasRecord.pid(), newMasRecord, path);
      }
    } else {
      throw new NotFoundException("Requested machine annotation system: " + id + "does not exist");
    }
  }

  private void updateDeployment(MachineAnnotationServiceRecord newMasRecord,
      MachineAnnotationServiceRecord currentMasRecord) {
    try {
      deployMasToCluster(newMasRecord, false);
    } catch (KubernetesFailedException e) {
      rollbackToPreviousVersion(currentMasRecord, false);
    }
  }

  private void publishUpdateEvent(MachineAnnotationServiceRecord newMasRecord,
      MachineAnnotationServiceRecord currentMasRecord) throws ProcessingFailedException {
    JsonNode jsonPatch = JsonDiff.asJson(mapper.valueToTree(newMasRecord.mas()),
        mapper.valueToTree(currentMasRecord.mas()));
    try {
      kafkaPublisherService.publishUpdateEvent(newMasRecord.pid(), mapper.valueToTree(newMasRecord),
          jsonPatch,
          SUBJECT_TYPE);
    } catch (JsonProcessingException e) {
      log.error("Unable to publish message to Kafka", e);
      rollbackToPreviousVersion(currentMasRecord, true);
      throw new ProcessingFailedException("Failed to create new machine annotation service", e);
    }
  }

  private void rollbackToPreviousVersion(MachineAnnotationServiceRecord currentMasRecord,
      boolean rollbackDeployment) {
    repository.updateMachineAnnotationService(currentMasRecord);
    if (rollbackDeployment) {
      try {
        deployMasToCluster(currentMasRecord, false);
      } catch (KubernetesFailedException e) {
        log.error(
            "Fatal exception, unable to rollback kubernetes deployment for: {} ",
            currentMasRecord.pid(), e);
      }
    }
  }

  public void deleteMachineAnnotationService(String id) throws NotFoundException {
    var currentMasOptional = repository.getActiveMachineAnnotationService(id);
    if (currentMasOptional.isPresent()) {
      var deleted = Instant.now();
      repository.deleteMachineAnnotationService(id, deleted);
      deleteDeployment(currentMasOptional.get());
    } else {
      throw new NotFoundException("Requested machine annotation system: " + id + "does not exist");
    }
  }

  private void deleteDeployment(MachineAnnotationServiceRecord currentMasOptional) {
    try {
      appsV1Api.deleteNamespacedDeployment(getName(currentMasOptional) + DEPLOYMENT,
          kubernetesProperties.getMasNamespace(), null, null, null, null, null, null);
    } catch (ApiException e) {
      log.error(
          "Deletion of kubernetes resource failed for record: {}, with code: {} and message: {}",
          currentMasOptional, e.getCode(), e.getResponseBody());
      repository.createMachineAnnotationService(currentMasOptional);
    }
  }

  public JsonApiWrapper getMachineAnnotationService(String id, String path) {
    var masRecord = repository.getMachineAnnotationService(id);
    return wrapSingleResponse(id, masRecord, path);
  }

  public JsonApiListWrapper getMachineAnnotationServices(int pageNum, int pageSize, String path) {
    var masRecords = repository.getMachineAnnotationServices(pageNum, pageSize);
    return wrapResponse(masRecords, pageNum, pageSize, path);
  }

  private JsonApiWrapper wrapSingleResponse(String id, MachineAnnotationServiceRecord masRecord,
      String path) {
    return new JsonApiWrapper(
        new JsonApiData(id, HandleType.MACHINE_ANNOTATION_SERVICE,
            flattenMasRecord(masRecord)),
        new JsonApiLinks(path)
    );
  }

  private JsonApiListWrapper wrapResponse(List<MachineAnnotationServiceRecord> masRecords,
      int pageNum,
      int pageSize, String path) {
    boolean hasNext = masRecords.size() > pageSize;
    masRecords = hasNext ? masRecords.subList(0, pageSize) : masRecords;
    var linksNode = new JsonApiLinks(pageSize, pageNum, hasNext, path);
    var dataNode = wrapData(masRecords);
    return new JsonApiListWrapper(dataNode, linksNode);
  }

  private List<JsonApiData> wrapData(List<MachineAnnotationServiceRecord> masRecords) {
    return masRecords.stream()
        .map(r -> new JsonApiData(r.pid(), HandleType.MACHINE_ANNOTATION_SERVICE,
            flattenMasRecord(r)))
        .toList();
  }

  private JsonNode flattenMasRecord(MachineAnnotationServiceRecord masRecord) {
    var mappingNode = (ObjectNode) mapper.valueToTree(masRecord.mas());
    mappingNode.put("version", masRecord.version());
    mappingNode.put("created", masRecord.created().toString());
    if (masRecord.deleted() != null) {
      mappingNode.put("deleted", masRecord.deleted().toString());
    }
    return mappingNode;
  }
}
