package eu.dissco.orchestration.backend.service;

import static eu.dissco.orchestration.backend.configuration.ApplicationConfiguration.HANDLE_PROXY;
import static eu.dissco.orchestration.backend.utils.HandleUtils.removeProxy;
import static eu.dissco.orchestration.backend.utils.TombstoneUtils.buildTombstoneMetadata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.orchestration.backend.domain.Enrichment;
import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiData;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiListWrapper;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.exception.PidException;
import eu.dissco.orchestration.backend.exception.ProcessingFailedException;
import eu.dissco.orchestration.backend.properties.FdoProperties;
import eu.dissco.orchestration.backend.properties.TranslatorJobProperties;
import eu.dissco.orchestration.backend.repository.SourceSystemRepository;
import eu.dissco.orchestration.backend.schema.Agent;
import eu.dissco.orchestration.backend.schema.SourceSystem;
import eu.dissco.orchestration.backend.schema.SourceSystem.OdsStatus;
import eu.dissco.orchestration.backend.schema.SourceSystem.OdsTranslatorType;
import eu.dissco.orchestration.backend.schema.SourceSystemRequest;
import eu.dissco.orchestration.backend.web.HandleComponent;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1CronJob;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1Job;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SourceSystemService {

  private final FdoRecordService fdoRecordService;
  private final HandleComponent handleComponent;
  private final SourceSystemRepository repository;
  private final DataMappingService dataMappingService;
  private final KafkaPublisherService kafkaPublisherService;
  private final ObjectMapper mapper;
  @Qualifier("yamlMapper")
  private final ObjectMapper yamlMapper;
  private final TranslatorJobProperties jobProperties;
  private final Configuration configuration;
  private final BatchV1Api batchV1Api;
  private final Random random;
  private final FdoProperties fdoProperties;

  private static String getSuffix(String sourceSystemId) {
    return sourceSystemId.substring(sourceSystemId.lastIndexOf('/') + 1).toLowerCase();
  }

  private static String generateJobName(SourceSystem sourceSystem, boolean isCron) {
    var name =
        sourceSystem.getOdsTranslatorType().value().toLowerCase() + "-" +
            getSuffix(sourceSystem.getId()) + "-translator-service";
    if (!isCron) {
      name = name + "-" + RandomStringUtils.insecure().nextAlphabetic(6).toLowerCase();
    }
    return name;
  }

  private static void logException(SourceSystem sourceSystem, Exception e) {
    if (e instanceof IOException || e instanceof TemplateException) {
      log.error("Failed to create translator template for: {}", sourceSystem, e);
    } else if (e instanceof ApiException apiException) {
      log.error("Failed to deploy kubernetes deployment to cluster with code: {} and message: {}",
          apiException.getCode(), apiException.getResponseBody());
    }
  }

  private static boolean isEquals(SourceSystem sourceSystem, SourceSystem currentSourceSystem) {
    return Objects.equals(sourceSystem.getSchemaName(), currentSourceSystem.getSchemaName())
        &&
        Objects.equals(sourceSystem.getSchemaUrl().toString(),
            currentSourceSystem.getSchemaUrl().toString()) &&
        Objects.equals(sourceSystem.getSchemaDescription(),
            currentSourceSystem.getSchemaDescription()) &&
        Objects.equals(sourceSystem.getOdsTranslatorType().value(),
            (currentSourceSystem.getOdsTranslatorType().value())) &&
        Objects.equals(sourceSystem.getOdsDataMappingID(),
            currentSourceSystem.getOdsDataMappingID()) &&
        Objects.equals(sourceSystem.getLtcCollectionManagementSystem(),
            currentSourceSystem.getLtcCollectionManagementSystem()) &&
        Objects.equals(sourceSystem.getOdsMaximumRecords(),
            currentSourceSystem.getOdsMaximumRecords());
  }

  private static SourceSystem buildTombstoneSourceSystem(SourceSystem sourceSystem,
      Agent tombstoningAgent,
      Instant timestamp) {
    return new SourceSystem()
        .withId(sourceSystem.getId())
        .withType(sourceSystem.getType())
        .withSchemaIdentifier(sourceSystem.getSchemaIdentifier())
        .withOdsFdoType(sourceSystem.getOdsFdoType())
        .withOdsStatus(OdsStatus.TOMBSTONE)
        .withSchemaVersion(sourceSystem.getSchemaVersion() + 1)
        .withSchemaName(sourceSystem.getSchemaName())
        .withSchemaDescription(sourceSystem.getSchemaDescription())
        .withSchemaDateCreated(sourceSystem.getSchemaDateCreated())
        .withSchemaDateModified(Date.from(timestamp))
        .withSchemaCreator(sourceSystem.getSchemaCreator())
        .withSchemaUrl(sourceSystem.getSchemaUrl())
        .withLtcCollectionManagementSystem(sourceSystem.getLtcCollectionManagementSystem())
        .withOdsTranslatorType(sourceSystem.getOdsTranslatorType())
        .withOdsMaximumRecords(sourceSystem.getOdsMaximumRecords())
        .withOdsDataMappingID(sourceSystem.getOdsDataMappingID())
        .withOdsHasTombstoneMetadata(
            buildTombstoneMetadata(tombstoningAgent,
                "Source System tombstoned by agent through the orchestration backend", timestamp));
  }

  @PostConstruct
  public void setup() throws ApiException {
    updateCronsToImageTag();
  }

  private void updateCronsToImageTag() throws ApiException {
    log.info("Updating all cron jobs to use image tag: {}", jobProperties.getImage());
    var cronJobs = batchV1Api.listNamespacedCronJob(jobProperties.getNamespace()).execute();
    log.info("Found a total of: {} cron jobs", cronJobs.getItems().size());
    for (var cron : cronJobs.getItems()) {
      var currentImage = cron.getSpec().getJobTemplate().getSpec().getTemplate().getSpec()
          .getContainers().get(0).getImage();
      if (!jobProperties.getImage().equals(currentImage)) {
        log.info("Updating cron job: {} to use image tag: {}", cron.getMetadata().getName(),
            jobProperties.getImage());
        cron.getSpec().getJobTemplate().getSpec().getTemplate().getSpec().getContainers().get(0)
            .setImage(jobProperties.getImage());
        batchV1Api.replaceNamespacedCronJob(cron.getMetadata().getName(),
            jobProperties.getNamespace(), cron).execute();
      } else {
        log.info("Cron job: {} already uses image tag: {}", cron.getMetadata().getName(),
            jobProperties.getImage());
      }
    }
    log.info("Successfully updated all cron jobs to use image tag: {}", jobProperties.getImage());
  }

  private SourceSystem buildSourceSystem(
      SourceSystemRequest sourceSystemRequest, int version, Agent agent, String handle,
      Date created) {
    var id = HANDLE_PROXY + handle;
    return new SourceSystem()
        .withId(id)
        .withSchemaIdentifier(id)
        .withType(ObjectType.SOURCE_SYSTEM.getFullName())
        .withOdsFdoType(fdoProperties.getSourceSystemType())
        .withSchemaVersion(version)
        .withOdsStatus(OdsStatus.ACTIVE)
        .withSchemaName(sourceSystemRequest.getSchemaName())
        .withSchemaDescription(sourceSystemRequest.getSchemaDescription())
        .withSchemaDateCreated(created)
        .withSchemaDateModified(Date.from(Instant.now()))
        .withSchemaCreator(agent)
        .withSchemaUrl(sourceSystemRequest.getSchemaUrl())
        .withOdsDataMappingID(sourceSystemRequest.getOdsDataMappingID())
        .withOdsTranslatorType(
            OdsTranslatorType.fromValue(sourceSystemRequest.getOdsTranslatorType().value()))
        .withOdsMaximumRecords(sourceSystemRequest.getOdsMaximumRecords())
        .withLtcCollectionManagementSystem(sourceSystemRequest.getLtcCollectionManagementSystem());
  }

  public JsonApiWrapper createSourceSystem(SourceSystemRequest sourceSystemRequest, Agent agent,
      String path)
      throws NotFoundException, ProcessingFailedException {
    validateMappingExists(sourceSystemRequest.getOdsDataMappingID());
    String handle = createHandle(sourceSystemRequest);
    var sourceSystem = buildSourceSystem(sourceSystemRequest, 1, agent, handle,
        Date.from(Instant.now()));
    repository.createSourceSystem(sourceSystem);
    createCronJob(sourceSystem);
    createTranslatorJob(sourceSystem, true);
    publishCreateEvent(sourceSystem, agent);
    return wrapSingleResponse(sourceSystem, path);
  }

  private String createHandle(SourceSystemRequest sourceSystemRequest)
      throws ProcessingFailedException {
    var request = fdoRecordService.buildCreateRequest(sourceSystemRequest, ObjectType.SOURCE_SYSTEM
    );
    try {
      return handleComponent.postHandle(request);
    } catch (PidException e) {
      throw new ProcessingFailedException(e.getMessage(), e);
    }
  }

  private void createTranslatorJob(SourceSystem sourceSystem, boolean rollbackOnFailure)
      throws ProcessingFailedException {
    try {
      triggerTranslatorJob(sourceSystem);
    } catch (IOException | TemplateException | ApiException e) {
      logException(sourceSystem, e);
      if (rollbackOnFailure) {
        rollbackSourceSystemCreation(sourceSystem, true);
      }
      throw new ProcessingFailedException("Failed to deploy job to cluster", e);
    }
  }

  private void createCronJob(SourceSystem sourceSystem)
      throws ProcessingFailedException {
    try {
      deployCronJob(sourceSystem);
    } catch (IOException | TemplateException | ApiException e) {
      logException(sourceSystem, e);
      rollbackSourceSystemCreation(sourceSystem, false);
      throw new ProcessingFailedException("Failed to create new source system", e);
    }
  }

  private void deployCronJob(SourceSystem sourceSystem)
      throws IOException, TemplateException, ApiException {
    var k8sCron = setCronJobProperties(sourceSystem);
    batchV1Api.createNamespacedCronJob(jobProperties.getNamespace(), k8sCron).execute();
    log.info("Successfully published cronJob: {} to Kubernetes for source system: {}",
        k8sCron.getMetadata().getName(), sourceSystem.getId());
  }

  private V1CronJob setCronJobProperties(SourceSystem sourceSystem)
      throws IOException, TemplateException {
    var jobProps = getTemplateProperties(sourceSystem, true);
    var job = fillTemplate(jobProps, sourceSystem.getOdsTranslatorType(), true);
    var k8sCron = yamlMapper.readValue(job.toString(), V1CronJob.class);
    addEnrichmentService(
        k8sCron.getSpec().getJobTemplate().getSpec().getTemplate().getSpec().getContainers().get(0),
        List.of());
    return k8sCron;
  }

  private void publishCreateEvent(SourceSystem sourceSystem, Agent agent)
      throws ProcessingFailedException {
    try {
      kafkaPublisherService.publishCreateEvent(mapper.valueToTree(sourceSystem), agent);
    } catch (JsonProcessingException e) {
      log.error("Unable to publish message to Kafka", e);
      rollbackSourceSystemCreation(sourceSystem, true);
      throw new ProcessingFailedException("Failed to create new machine annotation service", e);
    }
  }

  private void rollbackSourceSystemCreation(SourceSystem sourceSystem,
      boolean removeCron) {
    var request = fdoRecordService.buildRollbackCreateRequest(sourceSystem.getId());
    try {
      handleComponent.rollbackHandleCreation(request);
    } catch (PidException e) {
      log.error(
          "Unable to rollback handle creation for source system. Manually delete the following handle: {}. Cause of error: ",
          sourceSystem.getId(), e);
    }
    repository.rollbackSourceSystemCreation(sourceSystem.getId());
    if (removeCron) {
      try {
        batchV1Api.deleteNamespacedCronJob(generateJobName(sourceSystem, true),
            jobProperties.getNamespace()).execute();
      } catch (ApiException e) {
        log.error("Unable to delete cronJob for source system: {}", sourceSystem.getId(), e);
      }
    }
  }

  private void validateMappingExists(String mappingId) throws NotFoundException {
    var dataMapping = dataMappingService.getActiveDataMapping(mappingId);
    if (dataMapping.isEmpty()) {
      throw new NotFoundException("Unable to locate Data Mapping with id " + mappingId);
    }
  }

  public JsonApiWrapper updateSourceSystem(String id, SourceSystemRequest sourceSystemRequest,
      Agent agent, String path, boolean trigger)
      throws NotFoundException, ProcessingFailedException {
    var currentSourceSystemOptional = repository.getActiveSourceSystem(id);
    if (currentSourceSystemOptional.isEmpty()) {
      throw new NotFoundException(
          "Could not update Source System " + id + ". Verify resource exists.");
    }
    var currentSourceSystem = currentSourceSystemOptional.get();
    var sourceSystem = buildSourceSystem(sourceSystemRequest,
        currentSourceSystem.getSchemaVersion() + 1, agent, id,
        currentSourceSystem.getSchemaDateCreated());
    if (isEquals(sourceSystem, currentSourceSystem)) {
      log.info(
          "Update request for source system: {} is identical to current version, no action taken",
          id);
      return null;
    }
    repository.updateSourceSystem(sourceSystem);
    updateCronJob(sourceSystem, currentSourceSystem);
    if (trigger) {
      log.info("Translator Job requested for updated source system: {}", id);
      triggerTranslatorForUpdatedSourceSystem(sourceSystem, currentSourceSystem);
    }
    publishUpdateEvent(sourceSystem, currentSourceSystem, agent);
    return wrapSingleResponse(sourceSystem, path);
  }

  private void triggerTranslatorForUpdatedSourceSystem(SourceSystem sourceSystem,
      SourceSystem currentSourceSystem) throws ProcessingFailedException {
    try {
      triggerTranslatorJob(sourceSystem);
    } catch (IOException | TemplateException | ApiException e) {
      logException(sourceSystem, e);
      rollbackToPreviousVersion(currentSourceSystem, true);
      throw new ProcessingFailedException("Failed to deploy job to cluster", e);
    }
  }

  private void updateCronJob(SourceSystem sourceSystem, SourceSystem currentSource)
      throws ProcessingFailedException {
    try {
      var cronjob = setCronJobProperties(sourceSystem);
      batchV1Api.replaceNamespacedCronJob(generateJobName(currentSource, true),
          jobProperties.getNamespace(), cronjob).execute();
    } catch (IOException | TemplateException | ApiException e) {
      logException(sourceSystem, e);
      rollbackToPreviousVersion(currentSource, false);
      throw new ProcessingFailedException("Failed to update new source system", e);
    }
  }

  private void publishUpdateEvent(SourceSystem newSourceSystem,
      SourceSystem currentSourceSystem, Agent agent) throws ProcessingFailedException {
    try {
      kafkaPublisherService.publishUpdateEvent(mapper.valueToTree(newSourceSystem),
          mapper.valueToTree(currentSourceSystem), agent);
    } catch (JsonProcessingException e) {
      log.error("Unable to publish message to Kafka", e);
      rollbackToPreviousVersion(currentSourceSystem, true);
      throw new ProcessingFailedException("Failed to create new machine annotation service", e);
    }
  }

  private void rollbackToPreviousVersion(SourceSystem currentSourceSystem,
      boolean rollbackCron) {
    repository.updateSourceSystem(currentSourceSystem);
    if (rollbackCron) {
      try {
        var cronjob = setCronJobProperties(currentSourceSystem);
        batchV1Api.replaceNamespacedCronJob(generateJobName(currentSourceSystem, true),
            jobProperties.getNamespace(), cronjob).execute();
      } catch (IOException | TemplateException | ApiException e) {
        log.error("Fatal error, unable to rollback to previous cronjob, manual action necessary",
            e);
      }
    }
  }

  public JsonApiWrapper getSourceSystemById(String id, String path) {
    var sourceSystem = repository.getSourceSystem(id);
    return wrapSingleResponse(sourceSystem, path);
  }

  public JsonApiListWrapper getSourceSystems(int pageNum, int pageSize, String path) {
    var sourceSystems = repository.getSourceSystems(pageNum, pageSize);
    return wrapResponse(sourceSystems, pageNum, pageSize, path);
  }

  private JsonApiWrapper wrapSingleResponse(SourceSystem sourceSystem, String path) {
    return new JsonApiWrapper(
        new JsonApiData(sourceSystem.getId(), ObjectType.SOURCE_SYSTEM,
            mapper.valueToTree(sourceSystem)),
        new JsonApiLinks(path)
    );
  }

  public void tombstoneSourceSystem(String id, Agent agent)
      throws NotFoundException, ProcessingFailedException {
    var result = repository.getActiveSourceSystem(id);
    if (result.isPresent()) {
      var sourceSystem = result.get();
      try {
        batchV1Api.deleteNamespacedCronJob(generateJobName(result.get(), true),
            jobProperties.getNamespace()).execute();
      } catch (ApiException e) {
        throw new ProcessingFailedException("Failed to delete cronJob for source system: " + id, e);
      }
      tombstoneHandle(id);
      var timestamp = Instant.now();
      var tombstoneSourceSystem = buildTombstoneSourceSystem(sourceSystem, agent, timestamp);
      repository.tombstoneSourceSystem(tombstoneSourceSystem, timestamp);
      try {
        kafkaPublisherService.publishTombstoneEvent(mapper.valueToTree(tombstoneSourceSystem),
            mapper.valueToTree(sourceSystem), agent);
      } catch (JsonProcessingException e) {
        log.error("Unable to publish tombstone event to provenance service", e);
        throw new ProcessingFailedException(
            "Unable to publish tombstone event to provenance service", e);
      }
      log.info("Delete request for source system: {} was successful", id);
    } else {
      throw new NotFoundException("Requested source system: " + id + " does not exist");
    }
  }

  private void tombstoneHandle(String handle) throws ProcessingFailedException {
    var request = fdoRecordService.buildTombstoneRequest(ObjectType.SOURCE_SYSTEM, handle);
    try {
      handleComponent.tombstoneHandle(request, handle);
    } catch (PidException e) {
      log.error("Unable to tombstone handle {}", handle, e);
      throw new ProcessingFailedException("Unable to tombstone handle", e);
    }
  }

  private JsonApiListWrapper wrapResponse(List<SourceSystem> sourceSystems, int pageNum,
      int pageSize, String path) {
    boolean hasNext = sourceSystems.size() > pageSize;
    sourceSystems = hasNext ? sourceSystems.subList(0, pageSize) : sourceSystems;
    var linksNode = new JsonApiLinks(pageSize, pageNum, hasNext, path);
    var dataNode = wrapData(sourceSystems);
    return new JsonApiListWrapper(dataNode, linksNode);
  }

  private List<JsonApiData> wrapData(List<SourceSystem> sourceSystems) {
    return sourceSystems.stream()
        .map(
            r -> new JsonApiData(r.getId(), ObjectType.SOURCE_SYSTEM, flattenSourceSystem(r)))
        .toList();
  }

  private JsonNode flattenSourceSystem(SourceSystem sourceSystem) {
    return mapper.valueToTree(sourceSystem);
  }

  public void runSourceSystemById(String id) throws ProcessingFailedException, NotFoundException {
    var sourceSystem = repository.getSourceSystem(id);
    if (sourceSystem == null || sourceSystem.getOdsHasTombstoneMetadata() != null) {
      var msg = sourceSystem == null ? "Source system {} does not exist"
          : "Source system {} is tombstoned";
      log.error(msg, id);
      throw new NotFoundException("No active source system with ID " + id + " was found");
    }
    createTranslatorJob(sourceSystem, false);
  }

  private void triggerTranslatorJob(SourceSystem sourceSystem)
      throws IOException, TemplateException, ApiException {
    var jobProps = getTemplateProperties(sourceSystem, false);
    var job = fillTemplate(jobProps, sourceSystem.getOdsTranslatorType(), false);
    var k8sJob = yamlMapper.readValue(job.toString(), V1Job.class);
    addEnrichmentService(k8sJob.getSpec().getTemplate().getSpec().getContainers().get(0),
        List.of());
    batchV1Api.createNamespacedJob(jobProperties.getNamespace(), k8sJob).execute();
    log.info("Successfully published job: {} to Kubernetes for source system: {}",
        k8sJob.getMetadata().getName(), sourceSystem.getId());
  }

  private Map<String, Object> getTemplateProperties(SourceSystem sourceSystem,
      boolean isCronJob) {
    var map = new HashMap<String, Object>();
    var jobName = generateJobName(sourceSystem, isCronJob);
    map.put("image", jobProperties.getImage());
    map.put("sourceSystemId", removeProxy(sourceSystem.getId()));
    map.put("maxItems", sourceSystem.getOdsMaximumRecords());
    map.put("jobName", jobName);
    map.put("namespace", jobProperties.getNamespace());
    map.put("containerName", jobName);
    map.put("kafkaHost", jobProperties.getKafkaHost());
    map.put("kafkaTopic", jobProperties.getKafkaTopic());
    map.put("database_url", jobProperties.getDatabaseUrl());
    if (isCronJob) {
      map.put("cron", generateCron());
    }
    return map;
  }

  private String generateCron() {
    String day = String.valueOf(random.nextInt(7));
    String hour = String.valueOf(random.nextInt(23));
    return "0 " + hour + " * * " + day;
  }

  private StringWriter fillTemplate(Map<String, Object> templateProperties,
      OdsTranslatorType translatorType, boolean isCron) throws IOException, TemplateException {
    var writer = new StringWriter();
    var templateFile = determineTemplate(translatorType, isCron);
    var template = configuration.getTemplate(templateFile);
    template.process(templateProperties, writer);
    return writer;
  }

  private String determineTemplate(OdsTranslatorType translatorType, boolean isCron) {
    if (isCron) {
      return switch (translatorType) {
        case DWCA -> "dwca-cron-job.ftl";
        case BIOCASE -> "biocase-cron-job.ftl";
      };
    } else {
      return switch (translatorType) {
        case DWCA -> "dwca-translator-job.ftl";
        case BIOCASE -> "biocase-translator-job.ftl";
      };
    }
  }

  private void addEnrichmentService(V1Container container, List<Enrichment> enrichmentList) {
    for (int i = 0; i < enrichmentList.size(); i++) {
      var envName = new V1EnvVar();
      envName.setName("ENRICHMENT_LIST_" + i + "_NAME");
      envName.setValue(enrichmentList.get(i).getName());
      container.addEnvItem(envName);
      var envImageOnly = new V1EnvVar();
      envImageOnly.setName("ENRICHMENT_LIST_" + i + "_IMAGE_ONLY");
      envImageOnly.setValue(enrichmentList.get(i).getImageOnly());
      container.addEnvItem(envImageOnly);
    }
  }
}
