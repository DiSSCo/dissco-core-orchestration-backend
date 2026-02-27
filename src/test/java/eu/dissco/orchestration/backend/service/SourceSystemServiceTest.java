package eu.dissco.orchestration.backend.service;

import static eu.dissco.orchestration.backend.testutils.TestUtils.APP_HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.BARE_HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.CREATED;
import static eu.dissco.orchestration.backend.testutils.TestUtils.DWC_DP_S3_URI;
import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPING_PATH;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SANDBOX_URI;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SOURCE_SYSTEM_TYPE_DOI;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SYSTEM_PATH;
import static eu.dissco.orchestration.backend.testutils.TestUtils.UPDATED;
import static eu.dissco.orchestration.backend.testutils.TestUtils.flattenSourceSystem;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenAgent;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenDataMapping;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMas;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMasScheduleData;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystem;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemRequest;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemResponse;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemSingleJsonApiWrapper;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenTombstoneSourceSystem;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import eu.dissco.orchestration.backend.domain.ExportType;
import eu.dissco.orchestration.backend.domain.MasScheduleData;
import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiData;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.exception.PidException;
import eu.dissco.orchestration.backend.exception.ProcessingFailedException;
import eu.dissco.orchestration.backend.properties.FdoProperties;
import eu.dissco.orchestration.backend.properties.TranslatorJobProperties;
import eu.dissco.orchestration.backend.properties.TranslatorJobProperties.Export;
import eu.dissco.orchestration.backend.repository.SourceSystemRepository;
import eu.dissco.orchestration.backend.schema.SourceSystem;
import eu.dissco.orchestration.backend.schema.SourceSystem.OdsTranslatorType;
import eu.dissco.orchestration.backend.schema.SourceSystemRequest;
import eu.dissco.orchestration.backend.schema.TombstoneMetadata;
import eu.dissco.orchestration.backend.web.HandleComponent;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.BatchV1Api.APIcreateNamespacedCronJobRequest;
import io.kubernetes.client.openapi.apis.BatchV1Api.APIcreateNamespacedJobRequest;
import io.kubernetes.client.openapi.apis.BatchV1Api.APIdeleteNamespacedCronJobRequest;
import io.kubernetes.client.openapi.apis.BatchV1Api.APIlistNamespacedCronJobRequest;
import io.kubernetes.client.openapi.apis.BatchV1Api.APIreplaceNamespacedCronJobRequest;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1CronJob;
import io.kubernetes.client.openapi.models.V1CronJobList;
import io.kubernetes.client.openapi.models.V1CronJobSpec;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1EnvVarSource;
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobSpec;
import io.kubernetes.client.openapi.models.V1JobTemplateSpec;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import io.kubernetes.client.openapi.models.V1SecretKeySelector;
import io.kubernetes.client.openapi.models.V1VolumeMount;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import tools.jackson.core.JacksonException;

@ExtendWith(MockitoExtension.class)
class SourceSystemServiceTest {

  private static final String NAMESPACE = "translator-services";
  private static final String EXPORT_NAMESPACE = "data-export-job";

  private final YAMLMapper yamlMapper = new YAMLMapper(
      YAMLMapper
          .builder()
          .findAndAddModules()
          .build());
  private final TranslatorJobProperties jobProperties = new TranslatorJobProperties();

  private final Configuration configuration = new Configuration(Configuration.VERSION_2_3_32);

  private final Random random = new Random();
  Clock updatedClock = Clock.fixed(UPDATED, ZoneOffset.UTC);
  private SourceSystemService service;
  @Mock
  private RabbitMqPublisherService rabbitMqPublisherService;
  @Mock
  private SourceSystemRepository repository;
  @Mock
  private FdoRecordService fdoRecordService;
  @Mock
  private HandleComponent handleComponent;
  @Mock
  private DataMappingService dataMappingService;
  @Mock
  private BatchV1Api batchV1Api;
  @Mock
  private FdoProperties fdoProperties;
  @Mock
  private S3Client s3Client;
  @Mock
  private MachineAnnotationServiceService machineAnnotationService;
  private MockedStatic<Instant> mockedStatic;
  private MockedStatic<Clock> mockedClock;

  private static V1CronJob getV1CronJob(String image, String name) {
    var container = new V1Container();
    container.setImage(image);
    container.setName(name);
    container.setEnv(List.of(
            new V1EnvVar().name("spring.profiles.active").value("biocase"),
            new V1EnvVar().name("spring.rabbitmq.host")
                .value("rabbitmq-cluster.rabbitmq.svc.cluster.local"),
            new V1EnvVar().name("spring.rabbitmq.username").valueFrom(new V1EnvVarSource().secretKeyRef(
                new V1SecretKeySelector().key("rabbitmq-username").name("aws-secrets"))),
            new V1EnvVar().name("spring.rabbitmq.password").valueFrom(new V1EnvVarSource().secretKeyRef(
                new V1SecretKeySelector().key("rabbitmq-password").name("aws-secrets"))),
            new V1EnvVar().name("application.sourceSystemId").value("20.5000.1025/GW0-POP-XSL"),
            new V1EnvVar().name("spring.datasource.url")
                .value("jdbc:postgresql://localhost:5432/translator"),
            new V1EnvVar().name("spring.datasource.username")
                .valueFrom(new V1EnvVarSource().secretKeyRef(
                    new V1SecretKeySelector().key("db-username").name("db-secrets"))),
            new V1EnvVar().name("spring.datasource.password")
                .valueFrom(new V1EnvVarSource().secretKeyRef(
                    new V1SecretKeySelector().key("db-password").name("db-secrets"))),
            new V1EnvVar().name("fdo.digital-media-type")
                .value("https://doi.org/21.T11148/bbad8c4e101e8af01115"),
            new V1EnvVar().name("fdo.digital-specimen-type")
                .value("https://doi.org/21.T11148/894b1e6cad57e921764e"),
            new V1EnvVar().name("JAVA_TOOL_OPTIONS").value("-XX:MaxRAMPercentage=85")
        )
    );
    container.setVolumeMounts(List.of(
        new V1VolumeMount().name("db-secrets").mountPath("/mnt/secrets-store/db-secrets")
            .readOnly(true),
        new V1VolumeMount().name("aws-secrets").mountPath("/mnt/secrets-store/aws-secrets")
            .readOnly(true)
    ));
    var podSpec = new V1PodSpec();
    podSpec.addContainersItem(container);
    var podTemplateSpec = new V1PodTemplateSpec();
    podTemplateSpec.setSpec(podSpec);
    var jobSpec = new V1JobSpec();
    jobSpec.setTemplate(podTemplateSpec);
    var job = new V1Job();
    job.setSpec(jobSpec);
    var jobTemplateSpec = new V1JobTemplateSpec();
    jobTemplateSpec.setSpec(jobSpec);
    var cronJobSpec = new V1CronJobSpec();
    cronJobSpec.setJobTemplate(jobTemplateSpec);
    var cronJob = new V1CronJob().metadata(new V1ObjectMeta().name(name));
    cronJob.setSpec(cronJobSpec);
    return cronJob;
  }

  static Stream<Arguments> equalSourceSystems() {
    return Stream.of(
        Arguments.of(givenSourceSystemRequest(), givenSourceSystem()),
        Arguments.of(
            givenSourceSystemRequest().withOdsFilters(List.of("filter")),
            givenSourceSystem().withOdsFilters(List.of("filter"))
        ),
        Arguments.of(
            givenSourceSystemRequest().withOdsMediaMachineAnnotationServices(List.of(HANDLE)),
            givenSourceSystem().withOdsMediaMachineAnnotationServices(List.of(HANDLE))),
        Arguments.of(
            givenSourceSystemRequest().withOdsSpecimenMachineAnnotationServices(List.of(HANDLE)),
            givenSourceSystem().withOdsSpecimenMachineAnnotationServices(List.of(HANDLE))));
  }

  static Stream<Arguments> updateMasSourceSystems() {
    return Stream.of(
        Arguments.of(givenSourceSystemRequest()
                .withOdsSpecimenMachineAnnotationServices(List.of(APP_HANDLE)), givenSourceSystem(),
            true),
        Arguments.of(givenSourceSystemRequest(),
            givenSourceSystem().withOdsSpecimenMachineAnnotationServices(List.of(APP_HANDLE)),
            false)
    );

  }

  private static Export givenExport() {
    var export = new Export();
    export.setDisscoDomain("dev.dissco.tech");
    export.setExportImage("public.ecr.aws/dissco/source-system-job-scheduler:latest");
    export.setKeycloak("https://login-demo.dissco.eu/");
    export.setNamespace(EXPORT_NAMESPACE);
    return export;
  }

  private static V1CronJob getV1DwcaCronJob(String dwcaCronName) {
    var container = new V1Container();
    container.setImage("public.ecr.aws/dissco/source-system-job-scheduler:latest");
    container.setName(dwcaCronName);
    container.setEnv(List.of(
            new V1EnvVar().name("KEYCLOAK_SERVER").value("https://login-demo.dissco.eu/"),
            new V1EnvVar().name("REALM").value("dissco"),
            new V1EnvVar().name("CLIENT_ID").valueFrom(new V1EnvVarSource().secretKeyRef(
                new V1SecretKeySelector().key("export-client-id").name("aws-secrets"))),
            new V1EnvVar().name("CLIENT_SECRET")
                .valueFrom(new V1EnvVarSource().secretKeyRef(
                    new V1SecretKeySelector().key("export-client-secret").name("aws-secrets"))),
            new V1EnvVar().name("SOURCE_SYSTEM_ID")
                .value("https://hdl.handle.net/20.5000.1025/GW0-POP-XSL"),
            new V1EnvVar().name("DISSCO_DOMAIN").value("dev.dissco.tech"),
            new V1EnvVar().name("EXPORT_TYPE").value("DWCA")
        )
    );
    var podSpec = new V1PodSpec();
    podSpec.addContainersItem(container);
    var podTemplateSpec = new V1PodTemplateSpec();
    podTemplateSpec.setSpec(podSpec);
    var jobSpec = new V1JobSpec();
    jobSpec.setTemplate(podTemplateSpec);
    var job = new V1Job();
    job.setSpec(jobSpec);
    var jobTemplateSpec = new V1JobTemplateSpec();
    jobTemplateSpec.setSpec(jobSpec);
    var cronJobSpec = new V1CronJobSpec();
    cronJobSpec.setJobTemplate(jobTemplateSpec);
    var cronJob = new V1CronJob().metadata(
        new V1ObjectMeta().name(dwcaCronName).namespace(EXPORT_NAMESPACE));
    cronJob.setSpec(cronJobSpec);
    return cronJob;
  }

  @BeforeEach
  void setup() throws IOException {
    jobProperties.setDatabaseUrl("jdbc:postgresql://localhost:5432/translator");
    jobProperties.setExport(givenExport());
    service = new SourceSystemService(fdoRecordService, handleComponent, repository,
        dataMappingService, machineAnnotationService, rabbitMqPublisherService, MAPPER, yamlMapper,
        jobProperties,
        configuration, batchV1Api, random, fdoProperties, s3Client);
    initTime();
    initFreeMaker();
  }

  private void initFreeMaker() throws IOException {
    configuration.setDirectoryForTemplateLoading(new File("src/main/resources/templates/"));
  }

  @AfterEach
  void destroy() {
    mockedStatic.close();
    mockedClock.close();
  }

  @Test
  void testCreateSourceSystem() throws Exception {
    // Given
    var expected = givenSourceSystemSingleJsonApiWrapper();
    var sourceSystem = givenSourceSystemRequest();
    given(fdoProperties.getSourceSystemType()).willReturn(SOURCE_SYSTEM_TYPE_DOI);
    given(handleComponent.postHandle(any())).willReturn(BARE_HANDLE);
    given(dataMappingService.getActiveDataMapping(sourceSystem.getOdsDataMappingID())).willReturn(
        Optional.of(givenDataMapping(sourceSystem.getOdsDataMappingID(), 1)));
    var createCron = mock(APIcreateNamespacedCronJobRequest.class);
    given(batchV1Api.createNamespacedCronJob(eq(NAMESPACE), any(V1CronJob.class)))
        .willReturn(createCron);
    given(batchV1Api.createNamespacedCronJob(eq(EXPORT_NAMESPACE), any(V1CronJob.class)))
        .willReturn(createCron);

    var createJob = mock(APIcreateNamespacedJobRequest.class);
    given(
        batchV1Api.createNamespacedJob(eq(NAMESPACE), any(V1Job.class))).willReturn(createJob);

    // When
    var result = service.createSourceSystem(sourceSystem, givenAgent(), SYSTEM_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
    then(repository).should().createSourceSystem(givenSourceSystem());
    then(rabbitMqPublisherService).should()
        .publishCreateEvent(givenSourceSystem(), givenAgent());
  }

  @Test
  void testCreateSourceSystemMappingNotFound() {
    // Given
    var sourceSystem = givenSourceSystemRequest();
    given(dataMappingService.getActiveDataMapping(sourceSystem.getOdsDataMappingID())).willReturn(
        Optional.empty());

    assertThrowsExactly(NotFoundException.class,
        () -> service.createSourceSystem(sourceSystem, givenAgent(), SYSTEM_PATH));
  }

  @Test
  void testCreateSourceSystemMasNotFound() {
    // Given
    var sourceSystem = givenSourceSystemRequest().withOdsSpecimenMachineAnnotationServices(
        List.of(HANDLE));
    given(machineAnnotationService.getMachineAnnotationServices(Set.of(HANDLE))).willReturn(
        List.of());

    assertThrowsExactly(NotFoundException.class,
        () -> service.createSourceSystem(sourceSystem, givenAgent(), SYSTEM_PATH));
  }

  @Test
  void testCreateSourceSystemCronFails() throws Exception {
    // Given
    var sourceSystem = givenSourceSystemRequest();
    given(handleComponent.postHandle(any())).willReturn(BARE_HANDLE);
    given(fdoProperties.getSourceSystemType()).willReturn(SOURCE_SYSTEM_TYPE_DOI);
    given(dataMappingService.getActiveDataMapping(sourceSystem.getOdsDataMappingID())).willReturn(
        Optional.of(givenDataMapping(sourceSystem.getOdsDataMappingID(), 1)));
    var createCron = mock(APIcreateNamespacedCronJobRequest.class);
    given(batchV1Api.createNamespacedCronJob(eq(NAMESPACE), any(V1CronJob.class)))
        .willReturn(createCron);
    given(createCron.execute()).willThrow(ApiException.class);

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.createSourceSystem(sourceSystem, givenAgent(), SYSTEM_PATH));

    // Then
    then(repository).should().createSourceSystem(givenSourceSystem());
    then(fdoRecordService).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());
    then(repository).should().rollbackSourceSystemCreation(HANDLE);
  }

  @Test
  void testCreateSourceSystemTranslatorJobFails() throws Exception {
    // Given
    var sourceSystem = givenSourceSystemRequest();
    given(handleComponent.postHandle(any())).willReturn(BARE_HANDLE);
    given(fdoProperties.getSourceSystemType()).willReturn(SOURCE_SYSTEM_TYPE_DOI);
    given(dataMappingService.getActiveDataMapping(sourceSystem.getOdsDataMappingID())).willReturn(
        Optional.of(givenDataMapping(sourceSystem.getOdsDataMappingID(), 1)));
    var createCron = mock(APIcreateNamespacedCronJobRequest.class);
    given(batchV1Api.createNamespacedCronJob(eq(NAMESPACE), any(V1CronJob.class)))
        .willReturn(createCron);
    var deleteCron = mock(APIdeleteNamespacedCronJobRequest.class);
    given(batchV1Api.deleteNamespacedCronJob(anyString(), eq(NAMESPACE))).willReturn(deleteCron);
    var createJob = mock(APIcreateNamespacedJobRequest.class);
    given(
        batchV1Api.createNamespacedJob(eq(NAMESPACE), any(V1Job.class))).willReturn(createJob);
    given(createJob.execute()).willThrow(ApiException.class);

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.createSourceSystem(sourceSystem, givenAgent(), SYSTEM_PATH));

    // Then
    then(repository).should().createSourceSystem(givenSourceSystem());
    then(fdoRecordService).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());
    then(repository).should().rollbackSourceSystemCreation(HANDLE);
  }

  @Test
  void testCreateSourceSystemEventFails() throws Exception {
    // Given
    var sourceSystem = givenSourceSystemRequest();
    given(handleComponent.postHandle(any())).willReturn(BARE_HANDLE);
    given(fdoProperties.getSourceSystemType()).willReturn(SOURCE_SYSTEM_TYPE_DOI);
    given(dataMappingService.getActiveDataMapping(sourceSystem.getOdsDataMappingID())).willReturn(
        Optional.of(givenDataMapping(sourceSystem.getOdsDataMappingID(), 1)));
    var createCron = mock(APIcreateNamespacedCronJobRequest.class);
    given(batchV1Api.createNamespacedCronJob(eq(NAMESPACE), any(V1CronJob.class)))
        .willReturn(createCron);
    given(batchV1Api.createNamespacedCronJob(eq(EXPORT_NAMESPACE), any(V1CronJob.class)))
        .willReturn(createCron);
    var deleteCron = mock(APIdeleteNamespacedCronJobRequest.class);
    given(batchV1Api.deleteNamespacedCronJob(anyString(), eq(NAMESPACE))).willReturn(deleteCron);
    var createJob = mock(APIcreateNamespacedJobRequest.class);
    given(
        batchV1Api.createNamespacedJob(eq(NAMESPACE), any(V1Job.class))).willReturn(createJob);
    willThrow(JacksonException.class).given(rabbitMqPublisherService)
        .publishCreateEvent(givenSourceSystem(), givenAgent());

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.createSourceSystem(sourceSystem, givenAgent(), SYSTEM_PATH));

    // Then
    then(repository).should().createSourceSystem(givenSourceSystem());
    then(fdoRecordService).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());
    then(repository).should().rollbackSourceSystemCreation(HANDLE);
  }

  @Test
  void testCreateMasHandleFails() throws Exception {
    // Given
    var sourceSystem = givenSourceSystemRequest();
    given(dataMappingService.getActiveDataMapping(sourceSystem.getOdsDataMappingID())).willReturn(
        Optional.of(givenDataMapping(sourceSystem.getOdsDataMappingID(), 1)));
    willThrow(PidException.class).given(handleComponent).postHandle(any());

    // When / Then
    assertThrowsExactly(ProcessingFailedException.class, () ->
        service.createSourceSystem(sourceSystem, givenAgent(), MAPPING_PATH));
  }

  @Test
  void testCreateSourceSystemEventAndRollbackFails() throws Exception {
    // Given
    var sourceSystem = givenSourceSystemRequest(SourceSystemRequest.OdsTranslatorType.DWCA);
    given(handleComponent.postHandle(any())).willReturn(BARE_HANDLE);
    given(fdoProperties.getSourceSystemType()).willReturn(SOURCE_SYSTEM_TYPE_DOI);
    given(dataMappingService.getActiveDataMapping(sourceSystem.getOdsDataMappingID())).willReturn(
        Optional.of(givenDataMapping(sourceSystem.getOdsDataMappingID(), 1)));
    willThrow(JacksonException.class).given(rabbitMqPublisherService)
        .publishCreateEvent(givenSourceSystem(OdsTranslatorType.DWCA), givenAgent());
    willThrow(PidException.class).given(handleComponent).rollbackHandleCreation(any());
    var createCron = mock(APIcreateNamespacedCronJobRequest.class);
    given(batchV1Api.createNamespacedCronJob(eq(NAMESPACE), any(V1CronJob.class)))
        .willReturn(createCron);
    given(batchV1Api.createNamespacedCronJob(eq(EXPORT_NAMESPACE), any(V1CronJob.class)))
        .willReturn(createCron);
    var deleteCron = mock(APIdeleteNamespacedCronJobRequest.class);
    given(batchV1Api.deleteNamespacedCronJob(anyString(), eq(NAMESPACE))).willReturn(deleteCron);
    var createJob = mock(APIcreateNamespacedJobRequest.class);
    given(
        batchV1Api.createNamespacedJob(eq(NAMESPACE), any(V1Job.class))).willReturn(createJob);

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.createSourceSystem(sourceSystem, givenAgent(), SYSTEM_PATH));

    // Then
    then(repository).should().createSourceSystem(givenSourceSystem(OdsTranslatorType.DWCA));
    then(fdoRecordService).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());
    then(repository).should().rollbackSourceSystemCreation(HANDLE);
  }

  @Test
  void testRunSourceSystemById() {
    // Given
    var sourceSystem = givenSourceSystem();
    given(repository.getSourceSystem(HANDLE)).willReturn(sourceSystem);
    var createJob = mock(APIcreateNamespacedJobRequest.class);
    given(
        batchV1Api.createNamespacedJob(eq(NAMESPACE), any(V1Job.class))).willReturn(createJob);

    // Then
    assertDoesNotThrow(() -> service.runSourceSystemById(HANDLE, new MasScheduleData()));
  }

  @Test
  void testRunSourceSystemByIdMasScheduleData() {
    // Given
    var sourceSystem = givenSourceSystem();
    given(repository.getSourceSystem(HANDLE)).willReturn(sourceSystem);
    var createJob = mock(APIcreateNamespacedJobRequest.class);
    given(
        batchV1Api.createNamespacedJob(eq(NAMESPACE), any(V1Job.class))).willReturn(createJob);

    // Then
    assertDoesNotThrow(() -> service.runSourceSystemById(HANDLE, givenMasScheduleData()));
  }

  @Test
  void testRunSourceSystemByIdNotFound() {
    // Given
    given(repository.getSourceSystem(HANDLE)).willReturn(null);

    // When / Then
    assertThrowsExactly(NotFoundException.class, () -> service.runSourceSystemById(HANDLE,
        new MasScheduleData()));
  }

  @Test
  void testRunSourceSystemByIdTombstoned() {
    // Given
    var sourceSystem = givenSourceSystem().withOdsHasTombstoneMetadata(new TombstoneMetadata());
    given(repository.getSourceSystem(HANDLE)).willReturn(sourceSystem);

    // When / Then
    assertThrowsExactly(NotFoundException.class, () -> service.runSourceSystemById(HANDLE,
        new MasScheduleData()));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testUpdateSourceSystem(boolean triggerTranslator) throws Exception {
    var sourceSystem = givenSourceSystemRequest();
    var prevSourceSystem = Optional.of(
        givenSourceSystem(OdsTranslatorType.DWCA).withOdsMaximumRecords(25));
    given(fdoProperties.getSourceSystemType()).willReturn(SOURCE_SYSTEM_TYPE_DOI);
    var expected = givenSourceSystemSingleJsonApiWrapper(2);
    given(repository.getActiveSourceSystem(BARE_HANDLE)).willReturn(prevSourceSystem);
    var updateCron = mock(APIreplaceNamespacedCronJobRequest.class);
    given(batchV1Api.replaceNamespacedCronJob(anyString(), eq(NAMESPACE), any(V1CronJob.class)))
        .willReturn(updateCron);
    if (triggerTranslator) {
      var createJob = mock(APIcreateNamespacedJobRequest.class);
      given(
          batchV1Api.createNamespacedJob(eq(NAMESPACE), any(V1Job.class))).willReturn(createJob);
    }
    given(dataMappingService.getActiveDataMapping(any())).willReturn(
        Optional.of(givenDataMapping()));

    // When
    var result = service.updateSourceSystem(BARE_HANDLE, sourceSystem, givenAgent(), SYSTEM_PATH,
        triggerTranslator);

    // Then
    assertThat(result).isEqualTo(expected);
    then(repository).should().updateSourceSystem(givenSourceSystem(2));
    if (!triggerTranslator) {
      then(batchV1Api).shouldHaveNoMoreInteractions();
    } else {
      then(batchV1Api).should().createNamespacedJob(eq(NAMESPACE), any(V1Job.class));
    }
    then(rabbitMqPublisherService).should()
        .publishUpdateEvent(givenSourceSystem(2), prevSourceSystem.get(), givenAgent());
  }

  @ParameterizedTest
  @MethodSource("updateMasSourceSystems")
  void testUpdateSourceSystemUpdateMas(SourceSystemRequest sourceSystemRequest,
      SourceSystem sourceSystem, boolean checkDb) throws Exception {
    var prevSourceSystem = Optional.of(sourceSystem);
    given(fdoProperties.getSourceSystemType()).willReturn(SOURCE_SYSTEM_TYPE_DOI);
    given(repository.getActiveSourceSystem(BARE_HANDLE)).willReturn(prevSourceSystem);
    var updateCron = mock(APIreplaceNamespacedCronJobRequest.class);
    given(batchV1Api.replaceNamespacedCronJob(anyString(), eq(NAMESPACE), any(V1CronJob.class)))
        .willReturn(updateCron);
    given(dataMappingService.getActiveDataMapping(any())).willReturn(
        Optional.of(givenDataMapping()));
    if (checkDb) {
      given(machineAnnotationService.getMachineAnnotationServices(Set.of(APP_HANDLE))).willReturn(
          List.of(givenMas()));
    }

    // When
    service.updateSourceSystem(BARE_HANDLE, sourceSystemRequest, givenAgent(), SYSTEM_PATH,
        false);

    // Then
    then(repository).should().updateSourceSystem(any());
    then(batchV1Api).shouldHaveNoMoreInteractions();
    then(rabbitMqPublisherService).should()
        .publishUpdateEvent(any(), any(), any());
  }

  @Test
  void testUpdateSourceSystemCronJobFails() throws Exception {
    var sourceSystem = givenSourceSystemRequest();
    var prevSourceSystem = Optional.of(givenSourceSystem(OdsTranslatorType.DWCA));
    given(repository.getActiveSourceSystem(BARE_HANDLE)).willReturn(prevSourceSystem);
    given(fdoProperties.getSourceSystemType()).willReturn(SOURCE_SYSTEM_TYPE_DOI);
    var updateCron = mock(APIreplaceNamespacedCronJobRequest.class);
    given(batchV1Api.replaceNamespacedCronJob(anyString(), eq(NAMESPACE), any(V1CronJob.class)))
        .willReturn(updateCron);
    given(updateCron.execute()).willThrow(ApiException.class);
    given(dataMappingService.getActiveDataMapping(any())).willReturn(
        Optional.of(givenDataMapping()));

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.updateSourceSystem(BARE_HANDLE, sourceSystem, givenAgent(), SYSTEM_PATH,
            false));

    // Then
    then(repository).should().updateSourceSystem(givenSourceSystem(2));
    then(repository).should().updateSourceSystem(prevSourceSystem.get());
  }

  @Test
  void testUpdateSourceSystemEventFails() throws Exception {
    var sourceSystem = givenSourceSystemRequest();
    var prevSourceSystem = Optional.of(givenSourceSystem(OdsTranslatorType.DWCA));
    given(repository.getActiveSourceSystem(BARE_HANDLE)).willReturn(prevSourceSystem);
    given(fdoProperties.getSourceSystemType()).willReturn(SOURCE_SYSTEM_TYPE_DOI);
    willThrow(JacksonException.class).given(rabbitMqPublisherService)
        .publishUpdateEvent(givenSourceSystem(2), prevSourceSystem.get(), givenAgent());
    var updateCron = mock(APIreplaceNamespacedCronJobRequest.class);
    given(batchV1Api.replaceNamespacedCronJob(anyString(), eq(NAMESPACE), any(V1CronJob.class)))
        .willReturn(updateCron);
    given(dataMappingService.getActiveDataMapping(any())).willReturn(
        Optional.of(givenDataMapping()));

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.updateSourceSystem(BARE_HANDLE, sourceSystem, givenAgent(), SYSTEM_PATH,
            false));

    // Then
    then(repository).should().updateSourceSystem(givenSourceSystem(2));
    then(repository).should().updateSourceSystem(prevSourceSystem.get());
  }

  @Test
  void testUpdateSourceSystemTriggerTranslatorFails() throws Exception {
    var sourceSystem = givenSourceSystemRequest();
    var prevSourceSystem = Optional.of(givenSourceSystem(OdsTranslatorType.DWCA));
    given(repository.getActiveSourceSystem(BARE_HANDLE)).willReturn(prevSourceSystem);
    given(fdoProperties.getSourceSystemType()).willReturn(SOURCE_SYSTEM_TYPE_DOI);
    var updateCron = mock(APIreplaceNamespacedCronJobRequest.class);
    given(batchV1Api.replaceNamespacedCronJob(anyString(), eq(NAMESPACE), any(V1CronJob.class)))
        .willReturn(updateCron);
    var createJob = mock(APIcreateNamespacedJobRequest.class);
    given(
        batchV1Api.createNamespacedJob(eq(NAMESPACE), any(V1Job.class))).willReturn(createJob);
    given(createJob.execute()).willThrow(ApiException.class);
    given(dataMappingService.getActiveDataMapping(any())).willReturn(
        Optional.of(givenDataMapping()));

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.updateSourceSystem(BARE_HANDLE, sourceSystem, givenAgent(), SYSTEM_PATH,
            true));

    // Then
    then(repository).should().updateSourceSystem(givenSourceSystem(2));
    then(repository).should().updateSourceSystem(prevSourceSystem.get());
    then(batchV1Api).should(times(2)).replaceNamespacedCronJob(anyString(), eq(NAMESPACE),
        any(V1CronJob.class));
  }

  @Test
  void testUpdateSourceSystemNotFound() {
    // Given
    var sourceSystem = givenSourceSystemRequest();
    given(repository.getActiveSourceSystem(HANDLE)).willReturn(Optional.empty());

    // Then
    assertThrowsExactly(NotFoundException.class,
        () -> service.updateSourceSystem(HANDLE, sourceSystem, givenAgent(), SYSTEM_PATH, true));
  }

  @ParameterizedTest
  @MethodSource("equalSourceSystems")
  void testUpdateSourceSystemNoChanges(SourceSystemRequest request, SourceSystem sourceSystem)
      throws Exception {
    // Given
    given(repository.getActiveSourceSystem(BARE_HANDLE)).willReturn(
        Optional.of(sourceSystem));
    given(dataMappingService.getActiveDataMapping(any())).willReturn(
        Optional.of(givenDataMapping()));
    lenient().when(machineAnnotationService.getMachineAnnotationServices(Set.of(HANDLE)))
        .thenReturn(List.of(givenMas()));

    // When
    var result = service.updateSourceSystem(BARE_HANDLE, request, givenAgent(), SYSTEM_PATH,
        false);

    // Then
    assertThat(result).isNull();
  }

  @Test
  void testGetSourceSystemById() throws NotFoundException {
    // Given
    var sourceSystem = givenSourceSystem();
    var expected = givenSourceSystemSingleJsonApiWrapper();

    given(repository.getSourceSystem(HANDLE)).willReturn(sourceSystem);

    // When
    var result = service.getSourceSystemById(HANDLE, SYSTEM_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testGetSourceSystemDwcDp() throws URISyntaxException, NotFoundException {
    // Given
    var exportType = ExportType.DWC_DP;
    given(repository.getExportLink(HANDLE, exportType)).willReturn(DWC_DP_S3_URI);
    var getObjectRequest = GetObjectRequest.builder()
        .key("2025-06-20/36a61c1d-0734-4549-b3f3-ba78233bcb5d.zip")
        .bucket("dissco-data-export-test").build();
    given(s3Client.getObject(getObjectRequest)).willReturn(mock(ResponseInputStream.class));

    // When
    service.getSourceSystemDownload(HANDLE, exportType);

    // Then
    then(s3Client).should().getObject(getObjectRequest);
  }

  @Test
  void testGetSourceSystemDwcDpNotFound() {
    // Given
    var exportType = ExportType.DWCA;
    given(repository.getExportLink(HANDLE, exportType)).willReturn(null);

    // When / Then
    assertThrows(NotFoundException.class,
        () -> service.getSourceSystemDownload(HANDLE, exportType));
  }

  @Test
  void testGetSourceSystemByIdIsDeleted() throws NotFoundException {
    // Given
    var sourceSystem = givenSourceSystem();
    var expected = new JsonApiWrapper(
        new JsonApiData(HANDLE, ObjectType.SOURCE_SYSTEM,
            flattenSourceSystem(sourceSystem)),
        new JsonApiLinks(SYSTEM_PATH));

    given(repository.getSourceSystem(HANDLE)).willReturn(sourceSystem);

    // When
    var result = service.getSourceSystemById(HANDLE, SYSTEM_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testGetSourceSystemNotFound() {
    // Given

    // When / Then
    assertThrows(NotFoundException.class, () -> service.getSourceSystemById(HANDLE, SYSTEM_PATH));
  }

  @Test
  void getSourceSystems() {
    // Given
    int pageNum = 1;
    int pageSize = 10;
    String path = SANDBOX_URI;
    List<SourceSystem> sourceSystems = Collections.nCopies(pageSize + 1,
        givenSourceSystem());
    given(repository.getSourceSystems(pageNum, pageSize)).willReturn(sourceSystems);
    var linksNode = new JsonApiLinks(pageSize, pageNum, true, path);
    var expected = givenSourceSystemResponse(sourceSystems.subList(0, pageSize), linksNode);

    // When
    var result = service.getSourceSystems(pageNum, pageSize, path);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getSourceSystemsLastPage() {
    // Given
    int pageNum = 2;
    int pageSize = 10;
    String path = SANDBOX_URI;
    List<SourceSystem> sourceSystems = Collections.nCopies(pageSize, givenSourceSystem());
    given(repository.getSourceSystems(pageNum, pageSize)).willReturn(sourceSystems);
    var linksNode = new JsonApiLinks(pageSize, pageNum, false, path);
    var expected = givenSourceSystemResponse(sourceSystems, linksNode);

    // When
    var result = service.getSourceSystems(pageNum, pageSize, path);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testTombstoneSourceSystem() throws Exception {
    // Given
    given(repository.getActiveSourceSystem(BARE_HANDLE)).willReturn(
        Optional.of(givenSourceSystem()));
    var deleteCron = mock(APIdeleteNamespacedCronJobRequest.class);
    given(batchV1Api.deleteNamespacedCronJob(anyString(), eq(NAMESPACE)))
        .willReturn(deleteCron);
    given(batchV1Api.deleteNamespacedCronJob(anyString(), eq(EXPORT_NAMESPACE)))
        .willReturn(deleteCron);
    mockedStatic.when(Instant::now).thenReturn(UPDATED);
    mockedClock.when(Clock::systemUTC).thenReturn(updatedClock);

    // When
    service.tombstoneSourceSystem(BARE_HANDLE, givenAgent());

    // Then
    then(repository).should().tombstoneSourceSystem(givenTombstoneSourceSystem(), UPDATED);
    then(handleComponent).should().tombstoneHandle(any(), eq(BARE_HANDLE));
    then(rabbitMqPublisherService).should().publishTombstoneEvent(any(), any(), eq(givenAgent()));
  }

  @Test
  void testTombstoneSourceSystemCronFailed() throws ApiException {
    // Given
    given(repository.getActiveSourceSystem(BARE_HANDLE)).willReturn(
        Optional.of(givenSourceSystem()));
    var deleteCron = mock(APIdeleteNamespacedCronJobRequest.class);
    given(batchV1Api.deleteNamespacedCronJob(anyString(), eq(NAMESPACE)))
        .willReturn(deleteCron);
    given(deleteCron.execute()).willThrow(ApiException.class);

    // Then
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.tombstoneSourceSystem(BARE_HANDLE, givenAgent()));
  }

  @Test
  void testTombstoneSourceSystemNotFound() {
    // Given
    given(repository.getActiveSourceSystem(BARE_HANDLE)).willReturn(Optional.empty());

    // Then
    assertThrowsExactly(NotFoundException.class,
        () -> service.tombstoneSourceSystem(BARE_HANDLE, givenAgent()));
  }

  @Test
  void synchronizeMissingSourceSystem() throws ApiException, TemplateException, IOException {
    // Given
    var cronResponse = mock(APIlistNamespacedCronJobRequest.class);
    given(batchV1Api.listNamespacedCronJob(NAMESPACE)).willReturn(cronResponse);
    given(batchV1Api.listNamespacedCronJob(EXPORT_NAMESPACE)).willReturn(cronResponse);
    given(cronResponse.execute()).willReturn(new V1CronJobList());
    given(repository.getSourceSystems(anyInt(), anyInt())).willReturn(List.of(givenSourceSystem()));
    given(batchV1Api.createNamespacedCronJob(eq(NAMESPACE), any(V1CronJob.class))).willReturn(
        mock(APIcreateNamespacedCronJobRequest.class));
    given(
        batchV1Api.createNamespacedCronJob(eq(EXPORT_NAMESPACE), any(V1CronJob.class))).willReturn(
        mock(APIcreateNamespacedCronJobRequest.class));

    // When
    service.setup();

    // Then
    then(batchV1Api).should().createNamespacedCronJob(eq(NAMESPACE), any(V1CronJob.class));
    then(batchV1Api).should().createNamespacedCronJob(eq(EXPORT_NAMESPACE), any(V1CronJob.class));
  }

  @Test
  void synchronizeExcessSourceSystem() throws ApiException, TemplateException, IOException {
    // Given
    var cronResponse = mock(APIlistNamespacedCronJobRequest.class);
    var dwcaCronResponse = mock(APIlistNamespacedCronJobRequest.class);
    var cronName = "biocase-gw0-pop-xsl-translator-service";
    var expectedCron = getV1CronJob(jobProperties.getImage(), cronName);
    var dwcaCronName = "dwca-gw0-pop-xsl";
    var expectedDwcaCron = getV1DwcaCronJob(dwcaCronName);
    given(batchV1Api.listNamespacedCronJob(NAMESPACE)).willReturn(cronResponse);
    given(batchV1Api.listNamespacedCronJob(EXPORT_NAMESPACE)).willReturn(dwcaCronResponse);
    given(cronResponse.execute()).willReturn(new V1CronJobList().addItemsItem(expectedCron));
    given(dwcaCronResponse.execute()).willReturn(
        new V1CronJobList().addItemsItem(expectedDwcaCron));
    given(repository.getSourceSystems(anyInt(), anyInt())).willReturn(List.of());
    given(batchV1Api.deleteNamespacedCronJob(anyString(), anyString())).willReturn(
        mock(APIdeleteNamespacedCronJobRequest.class));

    // When
    service.setup();

    // Then
    then(batchV1Api).should().deleteNamespacedCronJob(cronName, NAMESPACE);
    then(batchV1Api).should().deleteNamespacedCronJob(dwcaCronName, EXPORT_NAMESPACE);
  }

  @Test
  void synchronizeOutOfSyncSourceSystem() throws ApiException, TemplateException, IOException {
    // Given
    var cronResponse = mock(APIlistNamespacedCronJobRequest.class);
    var dwcaCronResponse = mock(APIlistNamespacedCronJobRequest.class);
    var cronName = "biocase-gw0-pop-xsl-translator-service";
    var expectedCron = getV1CronJob(jobProperties.getImage(), cronName);
    expectedCron.getSpec().getJobTemplate().getSpec().getTemplate().getSpec().getContainers().get(0)
        .setEnv(List.of());
    var dwcaCronName = "dwca-gw0-pop-xsl";
    var expectedDwcaCron = getV1DwcaCronJob(dwcaCronName);
    expectedDwcaCron.getSpec().getJobTemplate().getSpec().getTemplate().getSpec().getContainers()
        .get(0).setEnv(List.of());
    given(batchV1Api.listNamespacedCronJob(NAMESPACE)).willReturn(cronResponse);
    given(batchV1Api.listNamespacedCronJob(EXPORT_NAMESPACE)).willReturn(dwcaCronResponse);
    given(cronResponse.execute()).willReturn(new V1CronJobList().addItemsItem(expectedCron));
    given(dwcaCronResponse.execute()).willReturn(
        new V1CronJobList().addItemsItem(expectedDwcaCron));
    given(repository.getSourceSystems(anyInt(), anyInt())).willReturn(List.of(givenSourceSystem()));
    given(batchV1Api.replaceNamespacedCronJob(eq(cronName), eq(NAMESPACE),
        any(V1CronJob.class))).willReturn(mock(APIreplaceNamespacedCronJobRequest.class));
    given(batchV1Api.replaceNamespacedCronJob(eq(dwcaCronName), eq(EXPORT_NAMESPACE),
        any(V1CronJob.class))).willReturn(mock(APIreplaceNamespacedCronJobRequest.class));

    // When
    service.setup();

    // Then
    then(batchV1Api).should()
        .replaceNamespacedCronJob(eq(cronName), eq(NAMESPACE), any(V1CronJob.class));
    then(batchV1Api).should()
        .replaceNamespacedCronJob(eq(dwcaCronName), eq(EXPORT_NAMESPACE), any(V1CronJob.class));
  }

  @Test
  void synchronizeInSyncSourceSystem() throws ApiException, TemplateException, IOException {
    // Given
    var cronResponse = mock(APIlistNamespacedCronJobRequest.class);
    var dwcaCronResponse = mock(APIlistNamespacedCronJobRequest.class);
    var cronName = "biocase-gw0-pop-xsl-translator-service";
    var expectedCron = getV1CronJob(jobProperties.getImage(), cronName);
    var dwcaCronName = "dwca-gw0-pop-xsl";
    var expectedDwcaCron = getV1DwcaCronJob(dwcaCronName);
    given(batchV1Api.listNamespacedCronJob(NAMESPACE)).willReturn(cronResponse);
    given(batchV1Api.listNamespacedCronJob(EXPORT_NAMESPACE)).willReturn(dwcaCronResponse);
    given(cronResponse.execute()).willReturn(new V1CronJobList().addItemsItem(expectedCron));
    given(dwcaCronResponse.execute()).willReturn(
        new V1CronJobList().addItemsItem(expectedDwcaCron));
    given(repository.getSourceSystems(anyInt(), anyInt())).willReturn(List.of(givenSourceSystem()));

    // When
    service.setup();

    // Then
    then(batchV1Api).shouldHaveNoMoreInteractions();
  }

  private void initTime() {
    Clock clock = Clock.fixed(CREATED, ZoneOffset.UTC);
    mockedClock = mockStatic(Clock.class);
    mockedClock.when(Clock::systemUTC).thenReturn(clock);
    Instant instant = Instant.now(clock);
    mockedStatic = mockStatic(Instant.class);
    mockedStatic.when(Instant::now).thenReturn(instant);
    mockedStatic.when(() -> Instant.from(any())).thenReturn(instant);
  }

}
