package eu.dissco.orchestration.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.orchestration.backend.domain.Enrichment;
import eu.dissco.orchestration.backend.domain.TranslatorRequest;
import eu.dissco.orchestration.backend.domain.TranslatorResponse;
import eu.dissco.orchestration.backend.domain.TranslatorType;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.properties.TranslatorJobProperties;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobStatus;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TranslatorService {

  private static final String NAMESPACE = "default";

  private final TranslatorJobProperties jobProperties;
  private final Configuration configuration;
  private final ObjectMapper mapper;
  private final BatchV1Api batchV1Api;

  public TranslatorService(
      TranslatorJobProperties jobProperties, Configuration configuration,
      @Qualifier("yaml-mapper") ObjectMapper mapper, BatchV1Api batchV1Api) {
    this.jobProperties = jobProperties;
    this.configuration = configuration;
    this.mapper = mapper;
    this.batchV1Api = batchV1Api;
  }

  private static String getSuffix(String sourceSystemId) {
    return sourceSystemId.substring(sourceSystemId.indexOf('/') + 1).toLowerCase();
  }

  public TranslatorResponse createTranslator(TranslatorRequest request)
      throws TemplateException, IOException, ApiException {
    var jobProps = getTemplateProperties(request);
    var job = fillTemplate(jobProps, request.translatorType());
    var k8sJob = mapper.readValue(job.toString(), V1Job.class);
    addEnrichmentService(k8sJob, request.enrichmentList());
    var result = batchV1Api.createNamespacedJob(NAMESPACE, k8sJob, "true", null,
        null, null);
    return mapToResponse(result);
  }

  private void addEnrichmentService(V1Job k8sJob, List<Enrichment> enrichmentList) {
    var container = k8sJob.getSpec().getTemplate().getSpec().getContainers().get(0);
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

  private TranslatorResponse mapToResponse(V1Job result) {
    return TranslatorResponse.builder().
        jobName(Objects.requireNonNull(result.getMetadata()).getName())
        .jobStatus(determineStatus(Objects.requireNonNull(result.getStatus())))
        .completedAt(
            result.getStatus().getCompletionTime() != null ? result.getStatus().getCompletionTime()
                .toInstant() : null)
        .startTime(result.getStatus().getStartTime() != null ? result.getStatus().getStartTime()
            .toInstant() : null)
        .build();
  }

  private String determineStatus(V1JobStatus status) {
    if (status.getActive() != null) {
      return "Active";
    } else if (status.getFailed() != null) {
      return "Failed";
    } else if (status.getSucceeded() != null) {
      return "Completed";
    }
    return "Unknown";
  }

  private Map<String, Object> getTemplateProperties(TranslatorRequest request) {
    var map = new HashMap<String, Object>();
    map.put("image", jobProperties.getImage());
    map.put("sourceSystemId", request.sourceSystemId());
    map.put("jobName", "job-" + getSuffix(request.sourceSystemId()));
    map.put("containerName", "container-" + getSuffix(request.sourceSystemId()));
    map.put("kafkaHost", jobProperties.getKafkaHost());
    map.put("kafkaTopic", jobProperties.getKafkaTopic());
    return map;
  }

  private StringWriter fillTemplate(Map<String, Object> templateProperties,
      TranslatorType translatorType) throws IOException, TemplateException {
    var writer = new StringWriter();
    var templateFile = determineTemplate(translatorType);
    var template = configuration.getTemplate(templateFile);
    template.process(templateProperties, writer);
    return writer;
  }

  private String determineTemplate(TranslatorType translatorType) {
    return switch (translatorType) {
      case DWCA -> "dwca-translator-job.ftl";
      case BIOCASE -> "biocase-translator-job.ftl";
    };
  }

  public List<TranslatorResponse> getAll() throws ApiException {
    return batchV1Api.listNamespacedJob(NAMESPACE, null, null, null, null, null, null, null, null,
        null, null, null).getItems().stream().map(this::mapToResponse).toList();
  }

  public Optional<TranslatorResponse> get(String id) throws ApiException {
    return batchV1Api.listNamespacedJob(NAMESPACE, null, null, null, null, null, null, null, null,
            null, null, null).getItems().stream().filter(Objects::nonNull)
        .filter(v1Job -> v1Job.getMetadata().getName().equals(id))
        .map(this::mapToResponse).findAny();
  }

  public void deleteJob(String id) throws ApiException, NotFoundException {
    var job = get(id);
    if (job.isPresent()) {
      batchV1Api.deleteNamespacedJob(id, NAMESPACE, null, null, null, false, null, null);
    } else {
      throw new NotFoundException("Job with id: " + id + " not found in Kubernetes");
    }
  }
}
