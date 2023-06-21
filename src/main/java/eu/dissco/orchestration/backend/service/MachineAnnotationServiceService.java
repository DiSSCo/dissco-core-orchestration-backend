package eu.dissco.orchestration.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.diff.JsonDiff;
import com.google.gson.JsonParser;
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
import eu.dissco.orchestration.backend.properties.MachineAnnotationServiceProperties;
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
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MachineAnnotationServiceService {

  public static final String SUBJECT_TYPE = "MachineAnnotationService";
  private static final String DEPLOYMENT = "-deployment";
  private static final String SCALED_OBJECT = "-scaled-object";
  private static final String KEDA_GROUP = "keda.sh";
  private static final String KEDA_VERSION = "v1alpha1";
  private static final String KEDA_RESOURCE = "scaledobjects";

  private final HandleService handleService;
  private final KafkaPublisherService kafkaPublisherService;
  private final MachineAnnotationServiceRepository repository;
  private final AppsV1Api appsV1Api;
  private final CustomObjectsApi customObjectsApi;
  private final Configuration configuration;
  private final ObjectMapper mapper;
  private final MachineAnnotationServiceProperties properties;

  private static String getName(String pid) {
    return pid.substring(pid.indexOf('/') + 1).toLowerCase();
  }

  public JsonApiWrapper createMachineAnnotationService(MachineAnnotationService mas, String userId,
      String path) throws TransformerException {
    var handle = handleService.createNewHandle(HandleType.MACHINE_ANNOTATION_SERVICE);
    setDefaultMas(mas, handle);
    var masRecord = new MachineAnnotationServiceRecord(handle, 1, Instant.now(), userId, mas, null);
    repository.createMachineAnnotationService(masRecord);
    createDeployment(masRecord);
    publishCreateEvent(handle, masRecord);
    return wrapSingleResponse(handle, masRecord, path);
  }

  private void setDefaultMas(MachineAnnotationService mas, String handle) {
    if (mas.getTopicName() == null) {
      mas.setTopicName(getName(handle));
    }
    if (mas.getMaxReplicas() <= 0) {
      mas.setMaxReplicas(1);
    }
  }

  private void createDeployment(MachineAnnotationServiceRecord masRecord) {
    var successfulDeployment = false;
    var successfulKeda = false;
    try {
      successfulDeployment = deployMasToCluster(masRecord, true);
      deployKedaToCluster(masRecord);
    } catch (KubernetesFailedException e) {
      rollbackMasCreation(masRecord, successfulDeployment, successfulKeda);
      throw new ProcessingFailedException("Failed to create kubernetes resources", e);
    }
  }

  private void deployKedaToCluster(MachineAnnotationServiceRecord masRecord)
      throws KubernetesFailedException {
    var name = getName(masRecord.pid());
    try {
      var keda = createKedaFiles(masRecord, name);
      customObjectsApi.createNamespacedCustomObject(KEDA_GROUP, KEDA_VERSION,
          properties.getNamespace(), KEDA_RESOURCE, keda, null, null, null);
    } catch (TemplateException | IOException e) {
      log.error("Failed to create keda scaledObject files for: {}", masRecord, e);
      throw new KubernetesFailedException("Failed to deploy keda to cluster");
    } catch (ApiException e) {
      log.error("Failed to deploy keda scaledObject to cluster with code: {} and message: {}",
          e.getCode(), e.getResponseBody());
      throw new KubernetesFailedException("Failed to deploy keda to cluster");
    }
  }

  private boolean deployMasToCluster(MachineAnnotationServiceRecord masRecord, boolean create)
      throws KubernetesFailedException {
    var name = getName(masRecord.pid());
    try {
      var deployment = getV1Deployment(masRecord, name);
      if (create) {
        appsV1Api.createNamespacedDeployment(properties.getNamespace(),
            deployment, null, null, null, null);
      } else {
        appsV1Api.replaceNamespacedDeployment(name + DEPLOYMENT,
            properties.getNamespace(), deployment, null, null, null, null);
      }
    } catch (IOException | TemplateException e) {
      log.error("Failed to create deployment files for: {}", masRecord, e);
      throw new KubernetesFailedException("Failed to deploy to cluster");
    } catch (ApiException e) {
      log.error("Failed to deploy kubernetes deployment to cluster with code: {} and message: {}",
          e.getCode(), e.getResponseBody());
      throw new KubernetesFailedException("Failed to deploy to cluster");
    }
    return true;
  }

  private V1Deployment getV1Deployment(MachineAnnotationServiceRecord masRecord, String name)
      throws IOException, TemplateException {
    var templateProperties = getDeploymentTemplateProperties(masRecord, name);
    var deploymentString = fillDeploymentTemplate(templateProperties);
    return mapper.readValue(deploymentString.toString(), V1Deployment.class);
  }

  private Object createKedaFiles(MachineAnnotationServiceRecord masRecord, String name)
      throws TemplateException, IOException {
    var templateProperties = getKedaTemplateProperties(masRecord, name);
    var kedaString = fillKedaTemplate(templateProperties);
    return JsonParser.parseString(kedaString.toString()).getAsJsonObject();
  }

  private Map<String, Object> getKedaTemplateProperties(MachineAnnotationServiceRecord masRecord,
      String name) {
    var map = new HashMap<String, Object>();
    map.put("name", name);
    map.put("kafkaHost", properties.getKafkaHost());
    map.put("maxReplicas", masRecord.mas().getMaxReplicas());
    map.put("topicName", masRecord.mas().getTopicName());
    return map;
  }

  private StringWriter fillKedaTemplate(Map<String, Object> templateProperties)
      throws IOException, TemplateException {
    var writer = new StringWriter();
    var template = configuration.getTemplate("keda-template.ftl");
    template.process(templateProperties, writer);
    return writer;
  }

  private Map<String, Object> getDeploymentTemplateProperties(
      MachineAnnotationServiceRecord masRecord,
      String name) {
    var map = new HashMap<String, Object>();
    map.put("image", masRecord.mas().getContainerImage());
    map.put("imageTag", masRecord.mas().getContainerTag());
    map.put("name", name);
    map.put("kafkaHost", properties.getKafkaHost());
    map.put("topicName", masRecord.mas().getTopicName());
    return map;
  }

  private StringWriter fillDeploymentTemplate(Map<String, Object> templateProperties)
      throws IOException, TemplateException {
    var writer = new StringWriter();
    var template = configuration.getTemplate("mas-template.ftl");
    template.process(templateProperties, writer);
    return writer;
  }

  private void publishCreateEvent(String handle, MachineAnnotationServiceRecord masRecord) {
    try {
      kafkaPublisherService.publishCreateEvent(handle, mapper.valueToTree(masRecord), SUBJECT_TYPE);
    } catch (JsonProcessingException e) {
      log.error("Unable to publish message to Kafka", e);
      rollbackMasCreation(masRecord, true, true);
      throw new ProcessingFailedException("Failed to create new machine annotation service", e);
    }
  }

  private void rollbackMasCreation(MachineAnnotationServiceRecord masRecord,
      boolean rollbackDeployment, boolean rollbackKeda) {
    handleService.rollbackHandleCreation(masRecord.pid());
    repository.rollbackMasCreation(masRecord.pid());
    var name = getName(masRecord.pid());
    if (rollbackDeployment) {
      try {
        appsV1Api.deleteNamespacedDeployment(name + DEPLOYMENT,
            properties.getNamespace(), null, null, null, null, null, null);
      } catch (ApiException e) {
        log.error(
            "Fatal exception, unable to rollback kubernetes deployment for: {} error message with code: {} and message: {}",
            masRecord, e.getCode(), e.getResponseBody());
      }
    }
    if (rollbackKeda) {
      try {
        customObjectsApi.deleteNamespacedCustomObject(KEDA_GROUP, KEDA_VERSION,
            properties.getNamespace(), KEDA_RESOURCE, name + SCALED_OBJECT, null,
            null, null, null, null);
      } catch (ApiException e) {
        log.error(
            "Fatal exception, unable to rollback kubernetes keda for: {} error message with code: {} and message: {}",
            masRecord, e.getCode(), e.getResponseBody());
      }
    }
  }

  public JsonApiWrapper updateMachineAnnotationService(String id,
      MachineAnnotationService mas, String userId, String path)
      throws NotFoundException {
    var currentMasOptional = repository.getActiveMachineAnnotationService(id);
    if (currentMasOptional.isPresent()) {
      var currentMasRecord = currentMasOptional.get();
      setDefaultMas(mas, id);
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
    var successfulDeployment = false;
    try {
      successfulDeployment = deployMasToCluster(newMasRecord, false);
      updateKedaResource(newMasRecord, currentMasRecord);
    } catch (KubernetesFailedException e) {
      rollbackToPreviousVersion(currentMasRecord, successfulDeployment, false);
      throw new ProcessingFailedException("Failed to update kubernetes resources", e);
    }
  }

  private void updateKedaResource(MachineAnnotationServiceRecord masRecord,
      MachineAnnotationServiceRecord rollbackRecord)
      throws KubernetesFailedException {
    var name = getName(masRecord.pid());
    try {
      customObjectsApi.deleteNamespacedCustomObject(KEDA_GROUP, KEDA_VERSION,
          properties.getNamespace(), KEDA_RESOURCE, name + SCALED_OBJECT, null,
          null, null, null, null);
    } catch (ApiException e) {
      log.error(
          "Deletion of kubernetes keda failed for record: {}, with code: {} and message: {}",
          masRecord, e.getCode(), e.getResponseBody());
      throw new KubernetesFailedException("Failed to remove keda from cluster");
    }
    try {
      deployKedaToCluster(masRecord);
    } catch (KubernetesFailedException e) {
      log.error(
          "Failed to deploy new version of keda to the cluster. Trying to rollback to previous version",
          e);
      if (rollbackRecord != null) {
        try {
          deployKedaToCluster(rollbackRecord);
        } catch (KubernetesFailedException ex) {
          log.error("Fatal error, unable to redeploy previous keda configuration");
        }
      }
      throw new KubernetesFailedException("Failed to remove keda from cluster");
    }
  }

  private void publishUpdateEvent(MachineAnnotationServiceRecord newMasRecord,
      MachineAnnotationServiceRecord currentMasRecord) {
    JsonNode jsonPatch = JsonDiff.asJson(mapper.valueToTree(newMasRecord.mas()),
        mapper.valueToTree(currentMasRecord.mas()));
    try {
      kafkaPublisherService.publishUpdateEvent(newMasRecord.pid(), mapper.valueToTree(newMasRecord),
          jsonPatch,
          SUBJECT_TYPE);
    } catch (JsonProcessingException e) {
      log.error("Unable to publish message to Kafka", e);
      rollbackToPreviousVersion(currentMasRecord, true, true);
      throw new ProcessingFailedException("Failed to create new machine annotation service", e);
    }
  }

  private void rollbackToPreviousVersion(MachineAnnotationServiceRecord currentMasRecord,
      boolean rollbackDeployment, boolean rollbackKeda) {
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
    if (rollbackKeda) {
      try {
        updateKedaResource(currentMasRecord, null);
      } catch (KubernetesFailedException e) {
        log.error(
            "Fatal exception, unable to rollback keda for: {} ",
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

  private void deleteDeployment(MachineAnnotationServiceRecord currentMasRecord) {
    var name = getName(currentMasRecord.pid());
    try {
      appsV1Api.deleteNamespacedDeployment(name + DEPLOYMENT,
          properties.getNamespace(), null, null, null, null, null, null);
    } catch (ApiException e) {
      log.error(
          "Deletion of kubernetes deployment failed for record: {}, with code: {} and message: {}",
          currentMasRecord, e.getCode(), e.getResponseBody());
      repository.createMachineAnnotationService(currentMasRecord);
      throw new ProcessingFailedException("Failed to delete kubernetes resources", e);
    }
    try {
      customObjectsApi.deleteNamespacedCustomObject(KEDA_GROUP, KEDA_VERSION,
          properties.getNamespace(), KEDA_RESOURCE, name + SCALED_OBJECT, null,
          null, null, null, null);
    } catch (ApiException e) {
      log.error(
          "Deletion of kubernetes keda failed for record: {}, with code: {} and message: {}",
          currentMasRecord, e.getCode(), e.getResponseBody());
      repository.createMachineAnnotationService(currentMasRecord);
      try {
        deployMasToCluster(currentMasRecord, true);
      } catch (KubernetesFailedException ex) {
        log.error("Failed error, unable to create deployment after failed keda deletion");
      }
      throw new ProcessingFailedException("Failed to delete kubernetes resources", e);
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
