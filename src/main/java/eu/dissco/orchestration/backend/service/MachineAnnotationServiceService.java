package eu.dissco.orchestration.backend.service;

import static eu.dissco.orchestration.backend.configuration.ApplicationConfiguration.HANDLE_PROXY;
import static eu.dissco.orchestration.backend.utils.HandleUtils.removeProxy;
import static eu.dissco.orchestration.backend.utils.TombstoneUtils.buildTombstoneMetadata;
import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiData;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiListWrapper;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.exception.KubernetesFailedException;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.exception.PidException;
import eu.dissco.orchestration.backend.exception.ProcessingFailedException;
import eu.dissco.orchestration.backend.properties.FdoProperties;
import eu.dissco.orchestration.backend.properties.KubernetesProperties;
import eu.dissco.orchestration.backend.properties.MachineAnnotationServiceProperties;
import eu.dissco.orchestration.backend.repository.MachineAnnotationServiceRepository;
import eu.dissco.orchestration.backend.schema.Agent;
import eu.dissco.orchestration.backend.schema.MachineAnnotationService;
import eu.dissco.orchestration.backend.schema.MachineAnnotationService.OdsStatus;
import eu.dissco.orchestration.backend.schema.MachineAnnotationServiceRequest;
import eu.dissco.orchestration.backend.schema.OdsHasTargetDigitalObjectFilter;
import eu.dissco.orchestration.backend.schema.OdsHasTargetDigitalObjectFilter__1;
import eu.dissco.orchestration.backend.schema.SchemaContactPoint;
import eu.dissco.orchestration.backend.schema.SchemaContactPoint__1;
import eu.dissco.orchestration.backend.web.HandleComponent;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.models.V1Deployment;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MachineAnnotationServiceService {

  private static final String DEPLOYMENT = "-deployment";
  private static final String SCALED_OBJECT = "-scaled-object";
  private static final String MAS_PREFIX = "mas-";
  private static final String BINDING = "-binding";
  private static final String QUEUE = "-queue";
  private static final String NAME = "name";
  private static final String VALUE = "value";
  private static final String ITEMS = "items";
  private static final String METADATA = "metadata";

  private final HandleComponent handleComponent;
  private final FdoRecordService fdoRecordService;
  private final RabbitMqPublisherService rabbitMqPublisherService;
  private final MachineAnnotationServiceRepository repository;
  private final AppsV1Api appsV1Api;
  private final CustomObjectsApi customObjectsApi;
  @Qualifier("kedaTemplate")
  private final Template kedaTemplate;
  @Qualifier("deploymentTemplate")
  private final Template deploymentTemplate;
  @Qualifier("masRabbitBindingTemplate")
  private final Template rabbitBindingTemplate;
  @Qualifier("masRabbitQueueTemplate")
  private final Template rabbitQueueTemplate;
  private final ObjectMapper mapper;
  @Qualifier("yamlMapper")
  private final ObjectMapper yamlMapper;
  private final MachineAnnotationServiceProperties properties;
  private final KubernetesProperties kubernetesProperties;
  private final FdoProperties fdoProperties;

  private static String getName(String pid) {
    return pid.substring(pid.lastIndexOf('/') + 1).toLowerCase();
  }

  private static SchemaContactPoint__1 buildContactPoint(SchemaContactPoint schemaContactPoint) {
    if (schemaContactPoint == null) {
      return new SchemaContactPoint__1();
    }
    return new SchemaContactPoint__1()
        .withSchemaDescription(schemaContactPoint.getSchemaDescription())
        .withSchemaEmail(schemaContactPoint.getSchemaEmail())
        .withSchemaUrl(schemaContactPoint.getSchemaUrl())
        .withSchemaTelephone(schemaContactPoint.getSchemaTelephone());
  }

  private static MachineAnnotationService buildTombstoneMachineAnnotationService(
      MachineAnnotationService mas,
      Agent tombstoningAgent, Instant timestamp) {
    return new MachineAnnotationService()
        .withId(mas.getId())
        .withType(mas.getType())
        .withSchemaIdentifier(mas.getSchemaIdentifier())
        .withOdsFdoType(mas.getOdsFdoType())
        .withOdsStatus(OdsStatus.TOMBSTONE)
        .withSchemaVersion(mas.getSchemaVersion() + 1)
        .withSchemaName(mas.getSchemaName())
        .withSchemaDescription(mas.getSchemaDescription())
        .withSchemaDateCreated(mas.getSchemaDateCreated())
        .withSchemaDateModified(Date.from(timestamp))
        .withSchemaCreator(mas.getSchemaCreator())
        .withOdsContainerImage(mas.getOdsContainerImage())
        .withOdsContainerTag(mas.getOdsContainerTag())
        .withOdsHasTargetDigitalObjectFilter(mas.getOdsHasTargetDigitalObjectFilter())
        .withSchemaCreativeWorkStatus(mas.getSchemaCreativeWorkStatus())
        .withSchemaCodeRepository(mas.getSchemaCodeRepository())
        .withSchemaProgrammingLanguage(mas.getSchemaProgrammingLanguage())
        .withOdsServiceAvailability(mas.getOdsServiceAvailability())
        .withSchemaMaintainer(mas.getSchemaMaintainer())
        .withSchemaLicense(mas.getSchemaLicense())
        .withSchemaContactPoint(mas.getSchemaContactPoint())
        .withOdsSlaDocumentation(mas.getOdsSlaDocumentation())
        .withOdsTopicName(mas.getOdsTopicName())
        .withOdsMaxReplicas(mas.getOdsMaxReplicas())
        .withOdsBatchingPermitted(mas.getOdsBatchingPermitted())
        .withOdsTimeToLive(mas.getOdsTimeToLive())
        .withOdsHasTombstoneMetadata(buildTombstoneMetadata(tombstoningAgent,
            "Machine Annotation Service tombstoned by agent through the orchestration backend",
            timestamp))
        .withOdsHasEnvironmentalVariables(mas.getOdsHasEnvironmentalVariables())
        .withOdsHasSecretVariables(mas.getOdsHasSecretVariables());
  }

  private static boolean equalsCheckKeda(JsonObject kedaObject, JsonObject existingKedaObject) {
    var existingSpec = existingKedaObject.get("spec").getAsJsonObject();
    var kedaSpec = kedaObject.get("spec").getAsJsonObject();
    return Objects.equals(kedaSpec.get("maxReplicaCount").getAsString(),
        existingSpec.get("maxReplicaCount").getAsString()) &&
        Objects.equals(kedaSpec.get("scaleTargetRef").getAsJsonObject().get("name"),
            existingSpec.get("scaleTargetRef").getAsJsonObject().get("name")) &&
        Objects.equals(kedaSpec.get("triggers"), existingSpec.get("triggers"));
  }

  @PostConstruct
  public void setup() throws ApiException, TemplateException, IOException, InterruptedException {
    var existingMasList = repository.getMachineAnnotationServices(0, 5000);
    synchronizeDeployment(existingMasList);
    synchronizeKeda(existingMasList);
    synchronizeRabbitBinding(existingMasList);
    synchronizeRabbitQueue(existingMasList);
  }

  private void synchronizeRabbitQueue(List<MachineAnnotationService> existingMasList)
      throws ApiException, TemplateException, IOException, InterruptedException {
    log.info("Synchronizing Rabbit queue resources of Machine Annotation Service");
    var existingRabbitQueueList = customObjectsApi.listNamespacedCustomObject(
        kubernetesProperties.getRabbitGroup(), kubernetesProperties.getRabbitVersion(),
        properties.getNamespace(), kubernetesProperties.getRabbitQueueResource()).execute();
    var items = new Gson().toJsonTree(existingRabbitQueueList).getAsJsonObject().get(ITEMS)
        .getAsJsonArray();
    if (items != null) {
      var existingRabbitQueueMap = items.asList().stream()
          .collect(toMap(queue -> (((JsonObject) queue).get(METADATA).getAsJsonObject()).get("name")
                  .getAsString(),
              queue -> queue));
      for (var machineAnnotationService : existingMasList) {
        var name = getName(machineAnnotationService.getId());
        var expectedRabbitQueue = JsonParser.parseString(createRabbitQueueResource(name));
        var existingRabbitQueue = existingRabbitQueueMap.get(MAS_PREFIX + name + QUEUE);
        if (existingRabbitQueue == null) {
          log.warn(
              "Found a machine annotation service: {} without a rabbit queue, creating one",
              machineAnnotationService.getId());
          customObjectsApi.createNamespacedCustomObject(kubernetesProperties.getRabbitGroup(),
              kubernetesProperties.getRabbitVersion(), properties.getNamespace(),
              kubernetesProperties.getRabbitQueueResource(), expectedRabbitQueue).execute();
        } else if (equalsCheckRabbitSpec((JsonObject) expectedRabbitQueue,
            existingRabbitQueue.getAsJsonObject())) {
          log.debug(
              "Rabbit queue resource for machine annotation service: {} is in sync with the database",
              machineAnnotationService.getId());
        } else {
          log.warn(
              "Found an out of sync Rabbit queue for machine annotation service: {}, synchronizing",
              machineAnnotationService.getId());
          customObjectsApi.deleteNamespacedCustomObject(kubernetesProperties.getRabbitGroup(),
              kubernetesProperties.getRabbitVersion(), properties.getNamespace(),
              kubernetesProperties.getRabbitQueueResource(), MAS_PREFIX + name + QUEUE).execute();
          Thread.sleep(kubernetesProperties.getKedaPatchWait());
          customObjectsApi.createNamespacedCustomObject(kubernetesProperties.getRabbitGroup(),
              kubernetesProperties.getRabbitVersion(), properties.getNamespace(),
              kubernetesProperties.getRabbitQueueResource(), expectedRabbitQueue).execute();
        }
      }
      existingRabbitQueueMap.keySet().removeAll(existingMasList.stream()
          .map(mas -> MAS_PREFIX + getName(mas.getId()) + QUEUE)
          .collect(Collectors.toSet()));
      for (var existingKeda : existingRabbitQueueMap.entrySet()) {
        log.warn(
            "Found a Rabbit Queue Resource: {} without a machine annotation service, deleting it",
            existingKeda.getKey());
        customObjectsApi.deleteNamespacedCustomObject(kubernetesProperties.getRabbitGroup(),
            kubernetesProperties.getRabbitVersion(), properties.getNamespace(),
            kubernetesProperties.getRabbitQueueResource(), existingKeda.getKey()).execute();
      }
    }
  }

  private void synchronizeRabbitBinding(List<MachineAnnotationService> existingMasList)
      throws TemplateException, IOException, ApiException, InterruptedException {
    log.info("Synchronizing Rabbit binding resources of Machine Annotation Service");
    var existingRabbitBindingList = customObjectsApi.listNamespacedCustomObject(
        kubernetesProperties.getRabbitGroup(), kubernetesProperties.getRabbitVersion(),
        properties.getNamespace(), kubernetesProperties.getRabbitBindingResource()).execute();
    var items = new Gson().toJsonTree(existingRabbitBindingList).getAsJsonObject().get(ITEMS)
        .getAsJsonArray();
    if (items != null) {
      var existingRabbitBindingMap = items.asList().stream()
          .collect(
              toMap(binding -> (((JsonObject) binding).get(METADATA).getAsJsonObject()).get("name")
                      .getAsString(),
                  binding -> binding));
      for (var machineAnnotationService : existingMasList) {
        var name = getName(machineAnnotationService.getId());
        var expectedRabbitBinding = JsonParser.parseString(createRabbitBindingResource(name));
        var existingRabbitBinding = existingRabbitBindingMap.get(MAS_PREFIX + name + BINDING);
        if (existingRabbitBinding == null) {
          log.warn(
              "Found a machine annotation service: {} without a rabbit binding, creating one",
              machineAnnotationService.getId());
          customObjectsApi.createNamespacedCustomObject(kubernetesProperties.getRabbitGroup(),
              kubernetesProperties.getRabbitVersion(), properties.getNamespace(),
              kubernetesProperties.getRabbitBindingResource(), expectedRabbitBinding).execute();
        } else if (equalsCheckRabbitSpec((JsonObject) expectedRabbitBinding,
            existingRabbitBinding.getAsJsonObject())) {
          log.debug(
              "Rabbit binding resource for machine annotation service: {} is in sync with the database",
              machineAnnotationService.getId());
        } else {
          log.warn(
              "Found an out of sync Rabbit binding for machine annotation service: {}, synchronizing",
              machineAnnotationService.getId());
          customObjectsApi.deleteNamespacedCustomObject(kubernetesProperties.getRabbitGroup(),
                  kubernetesProperties.getRabbitVersion(), properties.getNamespace(),
                  kubernetesProperties.getRabbitBindingResource(), MAS_PREFIX + name + BINDING)
              .execute();
          Thread.sleep(kubernetesProperties.getKedaPatchWait());
          customObjectsApi.createNamespacedCustomObject(kubernetesProperties.getRabbitGroup(),
              kubernetesProperties.getRabbitVersion(), properties.getNamespace(),
              kubernetesProperties.getRabbitBindingResource(), expectedRabbitBinding).execute();
        }
      }
      existingRabbitBindingMap.keySet().removeAll(existingMasList.stream()
          .map(mas -> MAS_PREFIX + getName(mas.getId()) + BINDING)
          .collect(Collectors.toSet()));
      for (var existingKeda : existingRabbitBindingMap.entrySet()) {
        log.warn(
            "Found a Rabbit Binding Resource: {} without a machine annotation service, deleting it",
            existingKeda.getKey());
        customObjectsApi.deleteNamespacedCustomObject(kubernetesProperties.getRabbitGroup(),
            kubernetesProperties.getRabbitVersion(), properties.getNamespace(),
            kubernetesProperties.getRabbitBindingResource(), existingKeda.getKey()).execute();
      }
    }
  }

  private boolean equalsCheckRabbitSpec(JsonObject kedaObject, JsonObject existingKeda) {
    return Objects.equals(kedaObject.get("spec"), existingKeda.get("spec"));
  }

  private void synchronizeKeda(List<MachineAnnotationService> existingMasList)
      throws ApiException, TemplateException, IOException, InterruptedException {
    log.info("Synchronizing KEDA resources of Machine Annotation Service");
    var existingKedaList = customObjectsApi.listNamespacedCustomObject(
        kubernetesProperties.getKedaGroup(), kubernetesProperties.getKedaVersion(),
        properties.getNamespace(), kubernetesProperties.getKedaResource()).execute();
    var items = new Gson().toJsonTree(existingKedaList).getAsJsonObject().get(ITEMS)
        .getAsJsonArray();
    if (items != null) {
      var existingKedaMap = items.asList().stream()
          .collect(toMap(keda -> (((JsonObject) keda).get(METADATA).getAsJsonObject()).get("name")
                  .getAsString(),
              keda -> keda));
      for (var machineAnnotationService : existingMasList) {
        var name = getName(machineAnnotationService.getId());
        var expectedKedaObject = createKedaFiles(machineAnnotationService, name);
        var existingKedaObject = existingKedaMap.get(name + SCALED_OBJECT);
        if (existingKedaObject == null) {
          log.warn(
              "Found a machine annotation service: {} without a keda scaled object, creating one",
              machineAnnotationService.getId());
          customObjectsApi.createNamespacedCustomObject(kubernetesProperties.getKedaGroup(),
              kubernetesProperties.getKedaVersion(), properties.getNamespace(),
              kubernetesProperties.getKedaResource(), expectedKedaObject).execute();
        } else if (equalsCheckKeda((JsonObject) expectedKedaObject,
            existingKedaObject.getAsJsonObject())) {
          log.debug("Keda resource for machine annotation service: {} is in sync with the database",
              machineAnnotationService.getId());
        } else {
          log.warn(
              "Found an out of sync keda scaled object for machine annotation service: {}, synchronizing",
              machineAnnotationService.getId());
          customObjectsApi.deleteNamespacedCustomObject(kubernetesProperties.getKedaGroup(),
              kubernetesProperties.getKedaVersion(), properties.getNamespace(),
              kubernetesProperties.getKedaResource(), name + SCALED_OBJECT).execute();
          Thread.sleep(kubernetesProperties.getKedaPatchWait());
          customObjectsApi.createNamespacedCustomObject(kubernetesProperties.getKedaGroup(),
              kubernetesProperties.getKedaVersion(), properties.getNamespace(),
              kubernetesProperties.getKedaResource(), expectedKedaObject).execute();
        }
      }
      existingKedaMap.keySet().removeAll(existingMasList.stream()
          .map(mas -> getName(mas.getId()) + SCALED_OBJECT)
          .collect(Collectors.toSet()));
      for (var existingKeda : existingKedaMap.entrySet()) {
        log.warn("Found a KEDA resource: {} without a machine annotation service, deleting it",
            existingKeda.getKey());
        customObjectsApi.deleteNamespacedCustomObject(kubernetesProperties.getKedaGroup(),
            kubernetesProperties.getKedaVersion(), properties.getNamespace(),
            kubernetesProperties.getKedaResource(), existingKeda.getKey()).execute();
      }
    }
  }

  private void synchronizeDeployment(List<MachineAnnotationService> existingMasList)
      throws ApiException, TemplateException, IOException {
    log.info("Synchronizing deployment of Machine Annotation Service");
    var existingDeployment = appsV1Api.listNamespacedDeployment(properties.getNamespace()).execute()
        .getItems().stream()
        .collect(toMap(mas -> mas.getMetadata().getName(), mas -> mas));
    for (var machineAnnotationService : existingMasList) {
      var expectedMasDeploy = getV1Deployment(machineAnnotationService,
          getName(machineAnnotationService.getId()));
      var existingMasDeploy = existingDeployment.get(
          getName(machineAnnotationService.getId()) + DEPLOYMENT);
      if (existingMasDeploy == null) {
        log.warn("Found a machine annotation service: {} without a deployment, creating one",
            machineAnnotationService.getId());
        appsV1Api.createNamespacedDeployment(properties.getNamespace(), expectedMasDeploy)
            .execute();
      } else if (equalsCheckDeployment(expectedMasDeploy, existingMasDeploy)) {
        log.debug("Deployment for machine annotation service: {} is in sync with the database",
            machineAnnotationService.getId());
      } else {
        log.warn(
            "Found an out of sync deployment for machine annotation service: {}, synchronizing",
            machineAnnotationService.getId());
        appsV1Api.replaceNamespacedDeployment(existingMasDeploy.getMetadata().getName(),
            properties.getNamespace(), expectedMasDeploy).execute();
      }
    }
    existingDeployment.keySet()
        .removeAll(existingMasList.stream().map(mas -> getName(mas.getId()) + DEPLOYMENT)
            .collect(Collectors.toSet()));
    for (var existingDeploy : existingDeployment.values()) {
      log.warn("Found a deployment: {} without a machine annotation service, deleting it",
          existingDeploy.getMetadata().getName());
      appsV1Api.deleteNamespacedDeployment(existingDeploy.getMetadata().getName(),
          properties.getNamespace()).execute();
    }
  }

  private boolean equalsCheckDeployment(V1Deployment databaseMasDeploy, V1Deployment existingMas) {
    var existingContainer = existingMas.getSpec().getTemplate().getSpec().getContainers().get(0);
    var databaseContainer = databaseMasDeploy.getSpec().getTemplate().getSpec().getContainers()
        .get(0);
    return Objects.equals(existingContainer.getName(), databaseContainer.getName()) &&
        Objects.equals(existingContainer.getImage(), databaseContainer.getImage()) &&
        Objects.equals(existingContainer.getEnv(), databaseContainer.getEnv()) &&
        Objects.equals(existingContainer.getVolumeMounts(), databaseContainer.getVolumeMounts());
  }

  public JsonApiWrapper createMachineAnnotationService(
      MachineAnnotationServiceRequest masRequest,
      Agent agent, String path) throws ProcessingFailedException {
    var requestBody = fdoRecordService.buildCreateRequest(masRequest, ObjectType.MAS);
    try {
      var handle = handleComponent.postHandle(requestBody);
      setDefaultMas(masRequest, handle);
      var mas = buildMachineAnnotationService(masRequest, 1, agent, handle,
          Instant.now());
      repository.createMachineAnnotationService(mas);
      createDeployment(mas);
      publishCreateEvent(mas, agent);
      return wrapSingleResponse(mas, path);
    } catch (PidException e) {
      throw new ProcessingFailedException(e.getMessage(), e);
    }
  }

  private MachineAnnotationService buildMachineAnnotationService(
      MachineAnnotationServiceRequest mas, int version, Agent agent, String handle,
      Instant created) {
    var id = HANDLE_PROXY + handle;
    return new MachineAnnotationService()
        .withId(id)
        .withSchemaIdentifier(id)
        .withType(ObjectType.MAS.getFullName())
        .withOdsFdoType(fdoProperties.getMasType())
        .withOdsStatus(OdsStatus.ACTIVE)
        .withSchemaVersion(version)
        .withSchemaName(mas.getSchemaName())
        .withSchemaDescription(mas.getSchemaDescription())
        .withSchemaDateCreated(Date.from(created))
        .withSchemaDateModified(Date.from(Instant.now()))
        .withSchemaCreator(agent)
        .withOdsContainerTag(mas.getOdsContainerTag())
        .withOdsContainerImage(mas.getOdsContainerImage())
        .withOdsHasTargetDigitalObjectFilter(
            buildTargetFilters(mas.getOdsHasTargetDigitalObjectFilter()))
        .withSchemaCreativeWorkStatus(mas.getSchemaCreativeWorkStatus())
        .withSchemaCodeRepository(mas.getSchemaCodeRepository())
        .withSchemaProgrammingLanguage(mas.getSchemaProgrammingLanguage())
        .withOdsServiceAvailability(mas.getOdsServiceAvailability())
        .withSchemaMaintainer(mas.getSchemaMaintainer())
        .withSchemaLicense(mas.getSchemaLicense())
        .withSchemaContactPoint(buildContactPoint(mas.getSchemaContactPoint()))
        .withOdsSlaDocumentation(mas.getOdsSlaDocumentation())
        .withOdsTopicName(mas.getOdsTopicName())
        .withOdsMaxReplicas(mas.getOdsMaxReplicas())
        .withOdsBatchingPermitted(mas.getOdsBatchingPermitted())
        .withOdsTimeToLive(mas.getOdsTimeToLive())
        .withOdsHasSecretVariables(mas.getOdsHasSecretVariables())
        .withOdsHasEnvironmentalVariables(mas.getOdsHasEnvironmentalVariables());
  }

  private OdsHasTargetDigitalObjectFilter__1 buildTargetFilters(
      OdsHasTargetDigitalObjectFilter odsTargetDigitalObjectFilter) {
    var filter = new OdsHasTargetDigitalObjectFilter__1();
    for (var prop : odsTargetDigitalObjectFilter.getAdditionalProperties().entrySet()) {
      filter.setAdditionalProperty(prop.getKey(), prop.getValue());
    }
    return filter;
  }

  private void setDefaultMas(MachineAnnotationServiceRequest mas, String handle) {
    if (mas.getOdsTopicName() == null) {
      mas.setOdsTopicName(getName(handle));
    }
    if (mas.getOdsMaxReplicas() == null || mas.getOdsMaxReplicas() <= 0) {
      mas.setOdsMaxReplicas(1);
    }
  }

  private void createDeployment(MachineAnnotationService mas)
      throws ProcessingFailedException {
    var successfulDeployment = false;
    var successfulKeda = false;
    var successfulRabbitBinding = false;
    try {
      successfulDeployment = deployMasToCluster(mas, true);
      successfulKeda = deployKedaToCluster(mas);
      successfulRabbitBinding = deployRabbitBindingToCluster(mas);
      deployRabbitQueueToCluster(mas);
    } catch (KubernetesFailedException e) {
      rollbackMasCreation(mas, successfulDeployment, successfulKeda, successfulRabbitBinding,
          false);
      throw new ProcessingFailedException("Failed to create kubernetes resources", e);
    }
  }

  private boolean deployRabbitBindingToCluster(MachineAnnotationService mas)
      throws KubernetesFailedException {
    var name = getName(mas.getId());
    try {
      var rabbitResource = createRabbitBindingResource(name);
      var rabbitObject = JsonParser.parseString(rabbitResource);
      customObjectsApi.createNamespacedCustomObject(kubernetesProperties.getRabbitGroup(),
          kubernetesProperties.getRabbitVersion(), properties.getNamespace(),
          kubernetesProperties.getRabbitBindingResource(), rabbitObject).execute();
      return true;
    } catch (TemplateException | IOException e) {
      log.error("Failed to create rabbitmq binding kubernetes files for: {}", mas, e);
      throw new KubernetesFailedException("Failed to deploy rabbit binding to cluster");
    } catch (ApiException e) {
      log.error(
          "Failed to deploy rabbitmq binding objects to cluster with code: {} and message: {}",
          e.getCode(), e.getResponseBody());
      throw new KubernetesFailedException("Failed to deploy rabbit binding to cluster");
    }
  }

  private void deployRabbitQueueToCluster(MachineAnnotationService mas)
      throws KubernetesFailedException {
    var name = getName(mas.getId());
    try {
      var rabbitResource = createRabbitQueueResource(name);
      var rabbitObject = JsonParser.parseString(rabbitResource);
      customObjectsApi.createNamespacedCustomObject(kubernetesProperties.getRabbitGroup(),
          kubernetesProperties.getRabbitVersion(), properties.getNamespace(),
          kubernetesProperties.getRabbitQueueResource(), rabbitObject).execute();
    } catch (TemplateException | IOException e) {
      log.error("Failed to create rabbitmq queue kubernetes files for: {}", mas, e);
      throw new KubernetesFailedException("Failed to deploy rabbit queue to cluster");
    } catch (ApiException e) {
      log.error("Failed to deploy rabbitmq queue to cluster with code: {} and message: {}",
          e.getCode(), e.getResponseBody());
      throw new KubernetesFailedException("Failed to deploy rabbit queue to cluster");
    }
  }

  private String createRabbitBindingResource(String name)
      throws TemplateException, IOException {
    var templateProperties = Map.of(
        NAME, name, "exchangeName",
        properties.getMasRabbitExchange());
    return getRabbitResourceString(templateProperties, rabbitBindingTemplate);
  }

  private String createRabbitQueueResource(String name)
      throws TemplateException, IOException {
    var templateProperties = Map.of(NAME, name);
    return getRabbitResourceString(templateProperties, rabbitQueueTemplate);
  }

  private String getRabbitResourceString(Map<String, String> templateProperties,
      Template rabbitTemplate)
      throws TemplateException, IOException {
    var writer = new StringWriter();
    rabbitTemplate.process(templateProperties, writer);
    return writer.toString();
  }

  private boolean deployKedaToCluster(MachineAnnotationService mas)
      throws KubernetesFailedException {
    var name = getName(mas.getId());
    try {
      var keda = createKedaFiles(mas, name);
      customObjectsApi.createNamespacedCustomObject(kubernetesProperties.getKedaGroup(),
          kubernetesProperties.getKedaVersion(), properties.getNamespace(),
          kubernetesProperties.getKedaResource(), keda).execute();
      return true;
    } catch (TemplateException | IOException e) {
      log.error("Failed to create keda scaledObject files for: {}", mas, e);
      throw new KubernetesFailedException("Failed to deploy keda to cluster");
    } catch (ApiException e) {
      log.error("Failed to deploy keda scaledObject to cluster with code: {} and message: {}",
          e.getCode(), e.getResponseBody());
      throw new KubernetesFailedException("Failed to deploy keda to cluster");
    }
  }

  private boolean deployMasToCluster(MachineAnnotationService mas, boolean create)
      throws KubernetesFailedException {
    var shortPid = getName(mas.getId());
    try {
      var deployment = getV1Deployment(mas, shortPid);
      if (create) {
        appsV1Api.createNamespacedDeployment(properties.getNamespace(), deployment).execute();
      } else {
        appsV1Api.replaceNamespacedDeployment(shortPid + DEPLOYMENT,
            properties.getNamespace(), deployment).execute();
      }
    } catch (IOException | TemplateException e) {
      log.error("Failed to create deployment files for: {}", mas, e);
      throw new KubernetesFailedException("Failed to deploy to cluster");
    } catch (ApiException e) {
      log.error("Failed to deploy kubernetes deployment to cluster with code: {} and message: {}",
          e.getCode(), e.getResponseBody());
      throw new KubernetesFailedException("Failed to deploy to cluster");
    }
    return true;
  }

  private V1Deployment getV1Deployment(MachineAnnotationService mas, String shortPid)
      throws IOException, TemplateException {
    var templateProperties = getDeploymentTemplateProperties(mas, shortPid);
    var deploymentString = fillDeploymentTemplate(templateProperties, mas);
    return mapper.readValue(deploymentString, V1Deployment.class);
  }

  private Object createKedaFiles(MachineAnnotationService mas, String name)
      throws TemplateException, IOException {
    var templateProperties = getKedaTemplateProperties(mas, name);
    var kedaString = fillKedaTemplate(templateProperties);
    return JsonParser.parseString(kedaString.toString());
  }

  private Map<String, Object> getKedaTemplateProperties(MachineAnnotationService mas,
      String name) {
    var map = new HashMap<String, Object>();
    map.put(NAME, name);
    map.put("maxReplicas", mas.getOdsMaxReplicas());
    map.put("topicName", mas.getOdsTopicName());
    return map;
  }

  private StringWriter fillKedaTemplate(Map<String, Object> templateProperties)
      throws IOException, TemplateException {
    var writer = new StringWriter();
    kedaTemplate.process(templateProperties, writer);
    return writer;
  }

  private Map<String, Object> getDeploymentTemplateProperties(MachineAnnotationService mas,
      String shortPid) {
    var map = new HashMap<String, Object>();
    map.put("image", mas.getOdsContainerImage());
    map.put("imageTag", mas.getOdsContainerTag());
    map.put("pid", shortPid);
    map.put(NAME, mas.getSchemaName());
    map.put("id", removeProxy(mas.getId()));
    map.put("topicName", mas.getOdsTopicName());
    map.put("runningEndpoint", properties.getRunningEndpoint());
    return map;
  }

  private List<JsonNode> addMasKeys(MachineAnnotationService mas) {
    var keyNode = new ArrayList<JsonNode>();
    if (mas.getOdsHasEnvironmentalVariables() != null) {
      mas.getOdsHasEnvironmentalVariables().forEach(env -> {
        if (env.getSchemaValue() instanceof String stringVal) {
          keyNode.add(mapper.createObjectNode()
              .put(NAME, env.getSchemaName())
              .put(VALUE, stringVal));
        } else if (env.getSchemaValue() instanceof Integer intVal) {
          keyNode.add(mapper.createObjectNode()
              .put(NAME, env.getSchemaName())
              .put(VALUE, intVal));
        } else if (env.getSchemaValue() instanceof Boolean boolValue) {
          keyNode.add(mapper.createObjectNode()
              .put(NAME, env.getSchemaName())
              .put(VALUE, boolValue));
        } else {
          throw new IllegalArgumentException();
        }
      });
    }
    if (mas.getOdsHasSecretVariables() != null) {
      mas.getOdsHasSecretVariables().forEach(secret -> keyNode.add(mapper.createObjectNode()
          .put(NAME, secret.getSchemaName())
          .set("valueFrom", mapper.createObjectNode()
              .set("secretKeyRef", mapper.createObjectNode()
                  .put(NAME, properties.getMasSecretStore())
                  .put("key", secret.getOdsSecretKeyRef())))));
    }
    return keyNode;
  }

  private String fillDeploymentTemplate(Map<String, Object> templateProperties,
      MachineAnnotationService mas)
      throws IOException, TemplateException {
    var writer = new StringWriter();
    deploymentTemplate.process(templateProperties, writer);
    var templateAsNode = (ObjectNode) yamlMapper.readTree(writer.toString());
    var defaultKeyNode = (ArrayNode) templateAsNode.get("spec").get("template").get("spec")
        .get("containers").get(0).get("env");
    defaultKeyNode.addAll(addMasKeys(mas));
    return mapper.writeValueAsString(templateAsNode);
  }

  private void publishCreateEvent(MachineAnnotationService mas, Agent agent)
      throws ProcessingFailedException {
    try {
      rabbitMqPublisherService.publishCreateEvent(mapper.valueToTree(mas), agent);
    } catch (JsonProcessingException e) {
      log.error("Unable to publish message to RabbitMQ", e);
      rollbackMasCreation(mas, true, true, true, true);
      throw new ProcessingFailedException("Failed to create new machine annotation service", e);
    }
  }

  private void rollbackMasCreation(MachineAnnotationService mas,
      boolean rollbackDeployment, boolean rollbackKeda, boolean rollbackRabbitBinding,
      boolean rollbackRabbitQueue) {
    var request = fdoRecordService.buildRollbackCreateRequest(mas.getId());
    try {
      handleComponent.rollbackHandleCreation(request);
    } catch (PidException e) {
      log.error(
          "Unable to rollback handle creation for MAS. Manually delete the following handle: {}. Cause of error: ",
          mas.getId(), e);
    }
    repository.rollbackMasCreation(mas.getId());
    var name = getName(mas.getId());
    if (rollbackDeployment) {
      try {
        appsV1Api.deleteNamespacedDeployment(name + DEPLOYMENT,
            properties.getNamespace()).execute();
      } catch (ApiException e) {
        log.error(
            "Fatal exception, unable to rollback kubernetes deployment for: {} error message with code: {} and message: {}",
            mas, e.getCode(), e.getResponseBody());
      }
    }
    if (rollbackKeda) {
      try {
        customObjectsApi.deleteNamespacedCustomObject(kubernetesProperties.getKedaGroup(),
            kubernetesProperties.getKedaVersion(), properties.getNamespace(),
            kubernetesProperties.getKedaResource(), name + SCALED_OBJECT).execute();
      } catch (ApiException e) {
        log.error(
            "Fatal exception, unable to rollback kubernetes keda for: {} error message with code: {} and message: {}",
            mas, e.getCode(), e.getResponseBody());
      }
    }
    if (rollbackRabbitBinding) {
      try {
        customObjectsApi.deleteNamespacedCustomObject(kubernetesProperties.getRabbitGroup(),
            kubernetesProperties.getRabbitVersion(), properties.getNamespace(),
            kubernetesProperties.getRabbitBindingResource(), "mas-" + name + BINDING).execute();
      } catch (ApiException e) {
        log.error(
            "Fatal exception, unable to rollback kubernetes rabbitmq binding for: {} error message with code: {} and message: {}",
            mas, e.getCode(), e.getResponseBody());
      }
    }
    if (rollbackRabbitQueue) {
      try {
        customObjectsApi.deleteNamespacedCustomObject(kubernetesProperties.getRabbitGroup(),
            kubernetesProperties.getRabbitVersion(), properties.getNamespace(),
            kubernetesProperties.getRabbitQueueResource(), "mas-" + name + QUEUE).execute();
      } catch (ApiException e) {
        log.error(
            "Fatal exception, unable to rollback kubernetes rabbitmq queue for: {} error message with code: {} and message: {}",
            mas, e.getCode(), e.getResponseBody());
      }
    }
  }

  public JsonApiWrapper updateMachineAnnotationService(String id,
      MachineAnnotationServiceRequest masRequest, Agent agent, String path)
      throws NotFoundException, ProcessingFailedException {
    var currentMasOptional = repository.getActiveMachineAnnotationService(id);
    if (currentMasOptional.isPresent()) {
      var currentMas = currentMasOptional.get();
      setDefaultMas(masRequest, id);
      var machineAnnotationService = buildMachineAnnotationService(masRequest,
          currentMas.getSchemaVersion() + 1, agent, id, Instant.now());
      if (isEqual(machineAnnotationService, currentMas)) {
        log.debug("No changes found for MAS");
        return null;
      } else {
        repository.updateMachineAnnotationService(machineAnnotationService);
        updateDeployment(machineAnnotationService, currentMas);
        publishUpdateEvent(machineAnnotationService, currentMas, agent);
        return wrapSingleResponse(machineAnnotationService, path);
      }
    } else {
      throw new NotFoundException("Requested machine annotation service: " + id + "does not exist");
    }
  }

  private boolean isEqual(MachineAnnotationService mas, MachineAnnotationService currentMas) {
    return Objects.equals(mas.getSchemaName(), currentMas.getSchemaName()) &&
        Objects.equals(mas.getSchemaDescription(), currentMas.getSchemaDescription()) &&
        Objects.equals(mas.getOdsContainerTag(), currentMas.getOdsContainerTag()) &&
        Objects.equals(mas.getOdsContainerImage(), currentMas.getOdsContainerImage()) &&
        Objects.equals(mas.getOdsHasTargetDigitalObjectFilter(),
            currentMas.getOdsHasTargetDigitalObjectFilter()) &&
        Objects.equals(mas.getSchemaCreativeWorkStatus(),
            currentMas.getSchemaCreativeWorkStatus()) &&
        Objects.equals(mas.getSchemaCodeRepository(), currentMas.getSchemaCodeRepository())
        && Objects.equals(mas.getSchemaProgrammingLanguage(),
        currentMas.getSchemaProgrammingLanguage()) &&
        Objects.equals(mas.getOdsServiceAvailability(), currentMas.getOdsServiceAvailability()) &&
        Objects.equals(mas.getSchemaMaintainer(), currentMas.getSchemaMaintainer()) &&
        Objects.equals(mas.getSchemaLicense(), currentMas.getSchemaLicense()) &&
        Objects.equals(mas.getSchemaContactPoint(), currentMas.getSchemaContactPoint()) &&
        Objects.equals(mas.getOdsSlaDocumentation(), currentMas.getOdsSlaDocumentation()) &&
        Objects.equals(mas.getOdsTopicName(), currentMas.getOdsTopicName()) &&
        Objects.equals(mas.getOdsMaxReplicas(), currentMas.getOdsMaxReplicas()) &&
        Objects.equals(mas.getOdsBatchingPermitted(), currentMas.getOdsBatchingPermitted())
        && Objects.equals(mas.getOdsTimeToLive(), currentMas.getOdsTimeToLive());
  }

  private void updateDeployment(MachineAnnotationService mas,
      MachineAnnotationService currentMas)
      throws ProcessingFailedException {
    var successfulDeployment = false;
    try {
      successfulDeployment = deployMasToCluster(mas, false);
      updateKedaResource(mas, currentMas);
    } catch (KubernetesFailedException e) {
      rollbackToPreviousVersion(currentMas, successfulDeployment, false);
      throw new ProcessingFailedException("Failed to update kubernetes resources", e);
    }
  }

  private void updateKedaResource(MachineAnnotationService mas,
      MachineAnnotationService rollbackRecord)
      throws KubernetesFailedException, ProcessingFailedException {
    var name = getName(mas.getId());
    try {
      customObjectsApi.deleteNamespacedCustomObject(kubernetesProperties.getKedaGroup(),
          kubernetesProperties.getKedaVersion(), properties.getNamespace(),
          kubernetesProperties.getKedaResource(), name + SCALED_OBJECT).execute();
    } catch (ApiException e) {
      log.error(
          "Deletion of kubernetes keda failed for record: {}, with code: {} and message: {}",
          mas, e.getCode(), e.getResponseBody());
      throw new KubernetesFailedException("Failed to remove keda from cluster");
    }
    try {
      Thread.sleep(kubernetesProperties.getKedaPatchWait());
    } catch (InterruptedException e) {
      log.error("Application interrupted", e);
      Thread.currentThread().interrupt();
      throw new ProcessingFailedException("Application was interrupted during KEDA recreate", e);
    }
    try {
      deployKedaToCluster(mas);
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

  private void publishUpdateEvent(MachineAnnotationService mas,
      MachineAnnotationService currentMas, Agent agent)
      throws ProcessingFailedException {
    try {
      rabbitMqPublisherService.publishUpdateEvent(mapper.valueToTree(mas),
          mapper.valueToTree(currentMas), agent);
    } catch (JsonProcessingException e) {
      log.error("Unable to publish message to RabbitMQ", e);
      rollbackToPreviousVersion(currentMas, true, true);
      throw new ProcessingFailedException("Failed to create new machine annotation service", e);
    }
  }

  private void rollbackToPreviousVersion(MachineAnnotationService currentMas,
      boolean rollbackDeployment, boolean rollbackKeda)
      throws ProcessingFailedException {
    repository.updateMachineAnnotationService(currentMas);
    if (rollbackDeployment) {
      try {
        log.warn(
            "Rolling back to previous version of kubernetes deployment with environmental variables provided in update request. Environment may be out of sync with deployment");
        deployMasToCluster(currentMas, false);
      } catch (KubernetesFailedException e) {
        log.error(
            "Fatal exception, unable to rollback kubernetes deployment for: {} ",
            currentMas.getId(), e);
      }
    }
    if (rollbackKeda) {
      try {
        updateKedaResource(currentMas, null);
      } catch (KubernetesFailedException e) {
        log.error(
            "Fatal exception, unable to rollback keda for: {} ",
            currentMas.getId(), e);
      }
    }
  }

  public void tombstoneMachineAnnotationService(String id, Agent agent)
      throws NotFoundException, ProcessingFailedException {
    var currentMasOptional = repository.getActiveMachineAnnotationService(id);
    if (currentMasOptional.isPresent()) {
      var mas = currentMasOptional.get();
      deleteDeployment(mas);
      tombstoneHandle(id);
      var timestamp = Instant.now();
      var tombstoneMas = buildTombstoneMachineAnnotationService(mas, agent, timestamp);
      repository.tombstoneMachineAnnotationService(tombstoneMas, timestamp);
      try {
        rabbitMqPublisherService.publishTombstoneEvent(mapper.valueToTree(tombstoneMas),
            mapper.valueToTree(mas), agent);
      } catch (JsonProcessingException e) {
        log.error("Unable to publish tombstone event to provenance service", e);
        throw new ProcessingFailedException(
            "Unable to publish tombstone event to provenance service", e);
      }
    } else {
      throw new NotFoundException("Requested machine annotation service: " + id + "does not exist");
    }
  }

  private void tombstoneHandle(String handle) throws ProcessingFailedException {
    var request = fdoRecordService.buildTombstoneRequest(ObjectType.MAS, handle);
    try {
      handleComponent.tombstoneHandle(request, handle);
    } catch (PidException e) {
      log.error("Unable to tombstone handle {}", handle, e);
      throw new ProcessingFailedException("Unable to tombstone handle", e);
    }
  }

  private void deleteDeployment(MachineAnnotationService currentMas)
      throws ProcessingFailedException {
    var name = getName(currentMas.getId());
    try {
      appsV1Api.deleteNamespacedDeployment(name + DEPLOYMENT, properties.getNamespace()).execute();
    } catch (ApiException e) {
      log.error(
          "Deletion of kubernetes deployment failed for record: {}, with code: {} and message: {}",
          currentMas, e.getCode(), e.getResponseBody());
      throw new ProcessingFailedException("Failed to delete kubernetes resources", e);
    }
    try {
      customObjectsApi.deleteNamespacedCustomObject(kubernetesProperties.getKedaGroup(),
          kubernetesProperties.getKedaVersion(), properties.getNamespace(),
          kubernetesProperties.getKedaResource(), name + SCALED_OBJECT).execute();
    } catch (ApiException e) {
      log.error(
          "Deletion of kubernetes keda failed for record: {}, with code: {} and message: {}",
          currentMas, e.getCode(), e.getResponseBody());
      try {
        deployMasToCluster(currentMas, true);
      } catch (KubernetesFailedException ex) {
        log.error("Failed error, unable to create deployment after failed keda deletion");
      }
      throw new ProcessingFailedException("Failed to delete kubernetes resources", e);
    }
    removeRabbitResources(currentMas);
  }

  private void removeRabbitResources(MachineAnnotationService currentMas) {
    try {
      var name = getName(currentMas.getId());
      customObjectsApi.deleteNamespacedCustomObject(kubernetesProperties.getRabbitGroup(),
          kubernetesProperties.getRabbitVersion(), properties.getNamespace(),
          kubernetesProperties.getRabbitBindingResource(), "mas-" + name + BINDING).execute();
      customObjectsApi.deleteNamespacedCustomObject(kubernetesProperties.getRabbitGroup(),
          kubernetesProperties.getRabbitVersion(), properties.getNamespace(),
          kubernetesProperties.getRabbitQueueResource(), "mas-" + name + QUEUE).execute();
    } catch (ApiException e) {
      log.error(
          "Deletion of kubernetes rabbit resources failed for record: {}, with code: {} and message: {}. "
              + "Resources need to be removed manually.",
          currentMas, e.getCode(), e.getResponseBody());
    }
  }

  public JsonApiWrapper getMachineAnnotationService(String id, String path) {
    var mas = repository.getMachineAnnotationService(id);
    return wrapSingleResponse(mas, path);
  }

  public JsonApiListWrapper getMachineAnnotationServices(int pageNum, int pageSize, String path) {
    var mass = repository.getMachineAnnotationServices(pageNum, pageSize);
    return wrapResponse(mass, pageNum, pageSize, path);
  }

  private JsonApiWrapper wrapSingleResponse(MachineAnnotationService mas,
      String path) {
    return new JsonApiWrapper(
        new JsonApiData(mas.getId(), ObjectType.MAS, flattenMas(mas)),
        new JsonApiLinks(path)
    );
  }

  private JsonApiListWrapper wrapResponse(List<MachineAnnotationService> mass,
      int pageNum, int pageSize, String path) {
    boolean hasNext = mass.size() > pageSize;
    mass = hasNext ? mass.subList(0, pageSize) : mass;
    var linksNode = new JsonApiLinks(pageSize, pageNum, hasNext, path);
    var dataNode = wrapData(mass);
    return new JsonApiListWrapper(dataNode, linksNode);
  }

  private List<JsonApiData> wrapData(List<MachineAnnotationService> mass) {
    return mass.stream()
        .map(r -> new JsonApiData(r.getId(), ObjectType.MAS,
            flattenMas(r)))
        .toList();
  }

  private JsonNode flattenMas(MachineAnnotationService mas) {
    return mapper.valueToTree(mas);
  }
}
