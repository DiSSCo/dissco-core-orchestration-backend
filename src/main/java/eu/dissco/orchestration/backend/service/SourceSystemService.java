package eu.dissco.orchestration.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.diff.JsonDiff;
import eu.dissco.orchestration.backend.database.jooq.enums.TranslatorType;
import eu.dissco.orchestration.backend.domain.Enrichment;
import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.domain.SourceSystem;
import eu.dissco.orchestration.backend.domain.SourceSystemRecord;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiData;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiListWrapper;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.exception.PidAuthenticationException;
import eu.dissco.orchestration.backend.exception.PidCreationException;
import eu.dissco.orchestration.backend.exception.ProcessingFailedException;
import eu.dissco.orchestration.backend.properties.TranslatorJobProperties;
import eu.dissco.orchestration.backend.repository.SourceSystemRepository;
import eu.dissco.orchestration.backend.web.HandleComponent;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1CronJob;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1Job;
import java.io.IOException;
import java.io.StringWriter;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  public static final String SUBJECT_TYPE = "SourceSystem";
  private final FdoRecordService fdoRecordService;
  private final HandleComponent handleComponent;
  private final SourceSystemRepository repository;
  private final MappingService mappingService;
  private final KafkaPublisherService kafkaPublisherService;
  private final ObjectMapper mapper;
  @Qualifier("yamlMapper")
  private final ObjectMapper yamlMapper;
  private final TranslatorJobProperties jobProperties;
  private final Configuration configuration;
  private final BatchV1Api batchV1Api;
  private final Random random;

  private static String getSuffix(String sourceSystemId) {
    return sourceSystemId.substring(sourceSystemId.indexOf('/') + 1).toLowerCase();
  }

  private static String generateJobName(SourceSystemRecord sourceSystem, boolean isCron) {
    var name =
        sourceSystem.sourceSystem().translatorType().getLiteral().toLowerCase() + "-" +
            getSuffix(sourceSystem.id()) + "-translator-service";
    if (!isCron) {
      name = name + "-" + RandomStringUtils.randomAlphabetic(6).toLowerCase();
    }
    return name;
  }

  private static void logException(SourceSystemRecord sourceSystemRecord, Exception e) {
    if (e instanceof IOException || e instanceof TemplateException) {
      log.error("Failed to create translator template for: {}", sourceSystemRecord, e);
    } else if (e instanceof ApiException apiException) {
      log.error("Failed to deploy kubernetes deployment to cluster with code: {} and message: {}",
          apiException.getCode(), apiException.getResponseBody());
    }
  }

  public JsonApiWrapper createSourceSystem(SourceSystem sourceSystem, String userId, String path)
      throws NotFoundException, ProcessingFailedException {
    validateMappingExists(sourceSystem.mappingId());
    String handle = createHandle(sourceSystem);
    var sourceSystemRecord = new SourceSystemRecord(handle, 1, userId, Instant.now(), null,
        sourceSystem);
    repository.createSourceSystem(sourceSystemRecord);
    createCronJob(sourceSystemRecord);
    createTranslatorJob(sourceSystemRecord, true);
    publishCreateEvent(handle, sourceSystemRecord);
    return wrapSingleResponse(handle, sourceSystemRecord, path);
  }

  private String createHandle(SourceSystem sourceSystem) throws ProcessingFailedException {
    var request = fdoRecordService.buildCreateRequest(sourceSystem, ObjectType.SOURCE_SYSTEM
    );
    try {
      return handleComponent.postHandle(request);
    } catch (PidAuthenticationException | PidCreationException e) {
      throw new ProcessingFailedException(e.getMessage(), e);
    }
  }

  private void createTranslatorJob(SourceSystemRecord sourceSystemRecord, boolean rollbackOnFailure)
      throws ProcessingFailedException {
    try {
      triggerTranslatorJob(sourceSystemRecord);
    } catch (IOException | TemplateException | ApiException e) {
      logException(sourceSystemRecord, e);
      if (rollbackOnFailure) {
        rollbackSourceSystemCreation(sourceSystemRecord, true);
      }
      throw new ProcessingFailedException("Failed to deploy job to cluster", e);
    }
  }

  private void createCronJob(SourceSystemRecord sourceSystemRecord)
      throws ProcessingFailedException {
    try {
      deployCronJob(sourceSystemRecord);
    } catch (IOException | TemplateException | ApiException e) {
      logException(sourceSystemRecord, e);
      rollbackSourceSystemCreation(sourceSystemRecord, false);
      throw new ProcessingFailedException("Failed to create new source system", e);
    }
  }

  private void deployCronJob(SourceSystemRecord sourceSystemRecord)
      throws IOException, TemplateException, ApiException {
    var k8sCron = setCronJobProperties(sourceSystemRecord);
    batchV1Api.createNamespacedCronJob(jobProperties.getNamespace(), k8sCron).execute();
    log.info("Successfully published cronJob: {} to Kubernetes for source system: {}",
        k8sCron.getMetadata().getName(), sourceSystemRecord.id());
  }

  private V1CronJob setCronJobProperties(SourceSystemRecord sourceSystemRecord)
      throws IOException, TemplateException {
    var jobProps = getTemplateProperties(sourceSystemRecord, true);
    var job = fillTemplate(jobProps, sourceSystemRecord.sourceSystem().translatorType(), true);
    var k8sCron = yamlMapper.readValue(job.toString(), V1CronJob.class);
    addEnrichmentService(
        k8sCron.getSpec().getJobTemplate().getSpec().getTemplate().getSpec().getContainers().get(0),
        List.of());
    return k8sCron;
  }

  private void publishCreateEvent(String handle, SourceSystemRecord sourceSystemRecord)
      throws ProcessingFailedException {
    try {
      kafkaPublisherService.publishCreateEvent(handle, mapper.valueToTree(sourceSystemRecord),
          SUBJECT_TYPE);
    } catch (JsonProcessingException e) {
      log.error("Unable to publish message to Kafka", e);
      rollbackSourceSystemCreation(sourceSystemRecord, true);
      throw new ProcessingFailedException("Failed to create new machine annotation service", e);
    }
  }

  private void rollbackSourceSystemCreation(SourceSystemRecord sourceSystemRecord,
      boolean removeCron) {
    var request = fdoRecordService.buildRollbackCreateRequest(sourceSystemRecord.id());
    try {
      handleComponent.rollbackHandleCreation(request);
    } catch (PidAuthenticationException | PidCreationException e) {
      log.error(
          "Unable to rollback handle creation for source system. Manually delete the following handle: {}. Cause of error: ",
          sourceSystemRecord.id(), e);
    }
    repository.rollbackSourceSystemCreation(sourceSystemRecord.id());
    if (removeCron) {
      try {
        batchV1Api.deleteNamespacedCronJob(generateJobName(sourceSystemRecord, true),
            jobProperties.getNamespace()).execute();
      } catch (ApiException e) {
        log.error("Unable to delete cronJob for source system: {}", sourceSystemRecord.id(), e);
      }
    }
  }

  private void validateMappingExists(String mappingId) throws NotFoundException {
    var mappingRecord = mappingService.getActiveMapping(mappingId);
    if (mappingRecord.isEmpty()) {
      throw new NotFoundException("Unable to locate Mapping record with id " + mappingId);
    }
  }

  public JsonApiWrapper updateSourceSystem(String id, SourceSystem sourceSystem, String userId,
      String path)
      throws NotFoundException, ProcessingFailedException {
    var currentSourceSystemOptional = repository.getActiveSourceSystem(id);
    if (currentSourceSystemOptional.isEmpty()) {
      throw new NotFoundException(
          "Could not update Source System " + id + ". Verify resource exists.");
    }
    if ((currentSourceSystemOptional.get().sourceSystem()).equals(sourceSystem)) {
      return null;
    }
    var currentSourceSystem = currentSourceSystemOptional.get();
    var sourceSystemRecord = new SourceSystemRecord(id, currentSourceSystem.version() + 1, userId,
        Instant.now(), null, sourceSystem);
    repository.updateSourceSystem(sourceSystemRecord);
    updateCronJob(sourceSystemRecord, currentSourceSystem);
    publishUpdateEvent(sourceSystemRecord, currentSourceSystem);
    return wrapSingleResponse(id, sourceSystemRecord, path);
  }

  private void updateCronJob(SourceSystemRecord sourceSystemRecord,
      SourceSystemRecord currentSourceSystem) throws ProcessingFailedException {
    try {
      var cronjob = setCronJobProperties(sourceSystemRecord);
      batchV1Api.replaceNamespacedCronJob(generateJobName(currentSourceSystem, true),
          jobProperties.getNamespace(), cronjob).execute();
    } catch (IOException | TemplateException | ApiException e) {
      logException(sourceSystemRecord, e);
      rollbackToPreviousVersion(currentSourceSystem, false);
      throw new ProcessingFailedException("Failed to update new source system", e);
    }
  }

  private void publishUpdateEvent(SourceSystemRecord newSourceSystemRecord,
      SourceSystemRecord currentSourceSystemRecord) throws ProcessingFailedException {
    JsonNode jsonPatch = JsonDiff.asJson(mapper.valueToTree(newSourceSystemRecord.sourceSystem()),
        mapper.valueToTree(currentSourceSystemRecord.sourceSystem()));
    try {
      kafkaPublisherService.publishUpdateEvent(newSourceSystemRecord.id(),
          mapper.valueToTree(newSourceSystemRecord),
          jsonPatch, SUBJECT_TYPE);
    } catch (JsonProcessingException e) {
      log.error("Unable to publish message to Kafka", e);
      rollbackToPreviousVersion(currentSourceSystemRecord, true);
      throw new ProcessingFailedException("Failed to create new machine annotation service", e);
    }
  }

  private void rollbackToPreviousVersion(SourceSystemRecord currentSourceSystemRecord,
      boolean rollbackCron) {
    repository.updateSourceSystem(currentSourceSystemRecord);
    if (rollbackCron) {
      try {
        var cronjob = setCronJobProperties(currentSourceSystemRecord);
        batchV1Api.replaceNamespacedCronJob(generateJobName(currentSourceSystemRecord, true),
            jobProperties.getNamespace(), cronjob).execute();
      } catch (IOException | TemplateException | ApiException e) {
        log.error("Fatal error, unable to rollback to previous cronjob, manual action necessary",
            e);
      }
    }
  }

  public JsonApiWrapper getSourceSystemById(String id, String path) {
    var sourceSystemRecord = repository.getSourceSystem(id);
    return wrapSingleResponse(id, sourceSystemRecord, path);
  }

  public JsonApiListWrapper getSourceSystemRecords(int pageNum, int pageSize, String path) {
    var sourceSystemRecords = repository.getSourceSystems(pageNum, pageSize);
    return wrapResponse(sourceSystemRecords, pageNum, pageSize, path);
  }

  private JsonApiWrapper wrapSingleResponse(String id, SourceSystemRecord sourceSystemRecord,
      String path) {
    return new JsonApiWrapper(
        new JsonApiData(id, ObjectType.SOURCE_SYSTEM,
            flattenSourceSystemRecord(sourceSystemRecord)),
        new JsonApiLinks(path)
    );
  }

  public void deleteSourceSystem(String id) throws NotFoundException, ProcessingFailedException {
    var result = repository.getActiveSourceSystem(id);
    if (result.isPresent()) {
      var deleted = Instant.now();
      try {
        batchV1Api.deleteNamespacedCronJob(generateJobName(result.get(), true),
            jobProperties.getNamespace()).execute();
      } catch (ApiException e) {
        throw new ProcessingFailedException("Failed to delete cronJob for source system: " + id, e);
      }
      repository.deleteSourceSystem(id, deleted);
      log.info("Delete request for source system: {} was successful", id);
    } else {
      throw new NotFoundException("Requested source system: " + id + " does not exist");
    }
  }

  private JsonApiListWrapper wrapResponse(List<SourceSystemRecord> sourceSystemRecords, int pageNum,
      int pageSize, String path) {
    boolean hasNext = sourceSystemRecords.size() > pageSize;
    sourceSystemRecords = hasNext ? sourceSystemRecords.subList(0, pageSize) : sourceSystemRecords;
    var linksNode = new JsonApiLinks(pageSize, pageNum, hasNext, path);
    var dataNode = wrapData(sourceSystemRecords);
    return new JsonApiListWrapper(dataNode, linksNode);
  }

  private List<JsonApiData> wrapData(List<SourceSystemRecord> sourceSystemRecords) {
    return sourceSystemRecords.stream()
        .map(r -> new JsonApiData(r.id(), ObjectType.SOURCE_SYSTEM, flattenSourceSystemRecord(r)))
        .toList();
  }

  private JsonNode flattenSourceSystemRecord(SourceSystemRecord sourceSystemRecord) {
    var sourceSystemNode = (ObjectNode) mapper.valueToTree(sourceSystemRecord.sourceSystem());
    sourceSystemNode.put("created", sourceSystemRecord.created().toString());
    sourceSystemNode.put("version", sourceSystemRecord.version());
    if (sourceSystemRecord.deleted() != null) {
      sourceSystemNode.put("deleted", sourceSystemRecord.deleted().toString());
    }
    return sourceSystemNode;
  }

  public void runSourceSystemById(String id) throws ProcessingFailedException {
    var sourceSystemRecord = repository.getSourceSystem(id);
    createTranslatorJob(sourceSystemRecord, false);
  }

  private void triggerTranslatorJob(SourceSystemRecord sourceSystemRecord)
      throws IOException, TemplateException, ApiException {
    var jobProps = getTemplateProperties(sourceSystemRecord, false);
    var job = fillTemplate(jobProps, sourceSystemRecord.sourceSystem().translatorType(), false);
    var k8sJob = yamlMapper.readValue(job.toString(), V1Job.class);
    addEnrichmentService(k8sJob.getSpec().getTemplate().getSpec().getContainers().get(0),
        List.of());
    batchV1Api.createNamespacedJob(jobProperties.getNamespace(), k8sJob).execute();
    log.info("Successfully published job: {} to Kubernetes for source system: {}",
        k8sJob.getMetadata().getName(), sourceSystemRecord.id());
  }

  private Map<String, Object> getTemplateProperties(SourceSystemRecord sourceSystem,
      boolean isCronJob) {
    var map = new HashMap<String, Object>();
    var jobName = generateJobName(sourceSystem, isCronJob);
    map.put("image", jobProperties.getImage());
    map.put("sourceSystemId", sourceSystem.id());
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
      TranslatorType translatorType, boolean isCron) throws IOException, TemplateException {
    var writer = new StringWriter();
    var templateFile = determineTemplate(translatorType, isCron);
    var template = configuration.getTemplate(templateFile);
    template.process(templateProperties, writer);
    return writer;
  }

  private String determineTemplate(TranslatorType translatorType, boolean isCron) {
    if (isCron) {
      return switch (translatorType) {
        case dwca -> "dwca-cron-job.ftl";
        case biocase -> "biocase-cron-job.ftl";
      };
    } else {
      return switch (translatorType) {
        case dwca -> "dwca-translator-job.ftl";
        case biocase -> "biocase-translator-job.ftl";
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
