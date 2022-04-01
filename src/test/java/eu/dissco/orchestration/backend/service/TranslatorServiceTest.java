package eu.dissco.orchestration.backend.service;

import static eu.dissco.orchestration.backend.TestUtils.CONTAINER_IMAGE;
import static eu.dissco.orchestration.backend.TestUtils.CONTAINER_NAME;
import static eu.dissco.orchestration.backend.TestUtils.JOB_NAME;
import static eu.dissco.orchestration.backend.TestUtils.KAFKA_HOST;
import static eu.dissco.orchestration.backend.TestUtils.KAFKA_TOPIC;
import static eu.dissco.orchestration.backend.TestUtils.QUERY;
import static eu.dissco.orchestration.backend.TestUtils.REQUEST_ENDPOINT;
import static eu.dissco.orchestration.backend.TestUtils.givenRequest;
import static eu.dissco.orchestration.backend.TestUtils.givenResponseActive;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.properties.TranslatorJobProperties;
import freemarker.template.TemplateException;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobList;
import io.kubernetes.client.openapi.models.V1JobSpec;
import io.kubernetes.client.openapi.models.V1JobStatus;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.openapi.models.V1SecurityContext;
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.freemarker.FreeMarkerConfigurationFactoryBean;

@ExtendWith(MockitoExtension.class)
class TranslatorServiceTest {

  private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();
  @Mock
  private TranslatorJobProperties jobProperties;
  @Mock
  private BatchV1Api batchV1Api;

  private TranslatorService service;

  @BeforeEach
  void setup() throws TemplateException, IOException {
    var freeMarkerBean = new FreeMarkerConfigurationFactoryBean();
    freeMarkerBean.setTemplateLoaderPath("classpath:/templates/");

    service = new TranslatorService(jobProperties, freeMarkerBean.createConfiguration(), mapper,
        batchV1Api);
  }

  @Test
  void testCreateTranslator() throws TemplateException, IOException, ApiException {
    // Given
    var job = givenJob();
    given(jobProperties.getImage()).willReturn(CONTAINER_IMAGE);
    given(jobProperties.getKafkaHost()).willReturn(KAFKA_HOST);
    given(jobProperties.getKafkaTopic()).willReturn(KAFKA_TOPIC);
    given(
        batchV1Api.createNamespacedJob(anyString(), any(), anyString(), any(), any())).willReturn(
        job);

    // When
    var result = service.createTranslator(givenRequest());

    // Then
    assertThat(result).isEqualTo(givenResponseActive());
  }

  @Test
  void testRetrieveAll() throws ApiException {
    // Given
    given(
        batchV1Api.listNamespacedJob(anyString(), any(), any(), any(), any(), any(), any(), any(),
            any(), any(), any())).willReturn(givenJobList(givenJob()));

    // When
    var result = service.getAll();

    // Then
    assertThat(result).isEqualTo(List.of(givenResponseActive()));
  }

  @Test
  void testRetrieve() throws ApiException {
    // Given
    given(
        batchV1Api.listNamespacedJob(anyString(), any(), any(), any(), any(), any(), any(), any(),
            any(), any(), any())).willReturn(givenJobList(givenJob()));

    // When
    var result = service.get(JOB_NAME);

    // Then
    assertThat(result).isEqualTo(Optional.of(givenResponseActive()));
  }

  @Test
  void testEmptyRetrieve() throws ApiException {
    // Given
    given(
        batchV1Api.listNamespacedJob(anyString(), any(), any(), any(), any(), any(), any(), any(),
            any(), any(), any())).willReturn(givenJobList(null));

    // When
    var result = service.get(JOB_NAME);

    // Then
    assertThat(result).isNotPresent();
  }

  @Test
  void testDeleteJob() throws ApiException, NotFoundException {
    // Given
    given(
        batchV1Api.listNamespacedJob(anyString(), any(), any(), any(), any(), any(), any(), any(),
            any(), any(), any())).willReturn(givenJobList(givenJob()));

    // When
    service.deleteJob(JOB_NAME);

    // Then
    then(batchV1Api).should()
        .deleteNamespacedJob(eq(JOB_NAME), anyString(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void testDeleteJobNotFound() throws ApiException {
    // Given
    given(
        batchV1Api.listNamespacedJob(anyString(), any(), any(), any(), any(), any(), any(), any(),
            any(), any(), any())).willReturn(givenJobList(null));

    // When
    assertThrows(NotFoundException.class, () -> service.deleteJob(JOB_NAME));

    // Then
    then(batchV1Api).shouldHaveNoMoreInteractions();
  }


  private V1JobList givenJobList(V1Job v1Job) {
    var jobList = new V1JobList();
    jobList.addItemsItem(v1Job);
    return jobList;
  }

  private V1Job givenJob() {
    var job = new V1Job();
    var metadata = new V1ObjectMeta();
    metadata.setName(JOB_NAME);
    job.setMetadata(metadata);
    var spec = new V1JobSpec();
    var podTemplate = givenPodTemplate();
    spec.setTemplate(podTemplate);
    job.setSpec(spec);
    job.metadata(metadata);
    job.setStatus(givenStatus());
    return job;
  }

  private V1JobStatus givenStatus() {
    var status = new V1JobStatus();
    status.setActive(1);
    status.setStartTime(OffsetDateTime.ofInstant(Instant.parse("2022-03-23T23:00:00.00Z"),
        ZoneOffset.UTC));
    return status;
  }

  private V1PodTemplateSpec givenPodTemplate() {
    var podTemplate = new V1PodTemplateSpec();
    var spec = new V1PodSpec();
    var container = new V1Container();
    container.setName(CONTAINER_NAME);
    container.image(CONTAINER_IMAGE);
    container.setEnv(givenEnvs());
    container.setSecurityContext(givenSecurityContext());
    spec.addContainersItem(container);
    podTemplate.setSpec(spec);
    return podTemplate;
  }

  private V1SecurityContext givenSecurityContext() {
    var context = new V1SecurityContext();
    context.runAsNonRoot(true);
    context.allowPrivilegeEscalation(false);
    return context;
  }

  private List<V1EnvVar> givenEnvs() {
    return List.of(givenEnvVar("spring.profiles.active", "geoCase"),
        givenEnvVar("webclient.endpoint", REQUEST_ENDPOINT),
        givenEnvVar("webclient.query-params", QUERY),
        givenEnvVar("kafka.host", KAFKA_HOST),
        givenEnvVar("kafka.topic", KAFKA_TOPIC));
  }

  private V1EnvVar givenEnvVar(String name, String value) {
    var env = new V1EnvVar();
    env.setName(name);
    env.setValue(value);
    return env;
  }

}
