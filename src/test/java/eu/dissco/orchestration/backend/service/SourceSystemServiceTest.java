package eu.dissco.orchestration.backend.service;

import static eu.dissco.orchestration.backend.testutils.TestUtils.BARE_HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.CREATED;
import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPING_PATH;
import static eu.dissco.orchestration.backend.testutils.TestUtils.OBJECT_CREATOR;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SANDBOX_URI;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SOURCE_SYSTEM_TYPE_DOI;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SYSTEM_PATH;
import static eu.dissco.orchestration.backend.testutils.TestUtils.UPDATED;
import static eu.dissco.orchestration.backend.testutils.TestUtils.flattenSourceSystem;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenAgent;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenDataMapping;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystem;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemRequest;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemResponse;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemSingleJsonApiWrapper;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenTombstoneSourceSystem;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiData;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.exception.PidException;
import eu.dissco.orchestration.backend.exception.ProcessingFailedException;
import eu.dissco.orchestration.backend.properties.FdoProperties;
import eu.dissco.orchestration.backend.properties.TranslatorJobProperties;
import eu.dissco.orchestration.backend.repository.SourceSystemRepository;
import eu.dissco.orchestration.backend.schema.SourceSystem;
import eu.dissco.orchestration.backend.schema.SourceSystem.OdsTranslatorType;
import eu.dissco.orchestration.backend.schema.SourceSystemRequest;
import eu.dissco.orchestration.backend.web.HandleComponent;
import freemarker.template.Configuration;
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
import io.kubernetes.client.openapi.models.V1Job;
import io.kubernetes.client.openapi.models.V1JobSpec;
import io.kubernetes.client.openapi.models.V1JobTemplateSpec;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1PodSpec;
import io.kubernetes.client.openapi.models.V1PodTemplateSpec;
import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SourceSystemServiceTest {

  private static final String NAMESPACE = "translator-services";

  private final ObjectMapper yamlMapper = new ObjectMapper(
      new YAMLFactory()).findAndRegisterModules();
  private final TranslatorJobProperties jobProperties = new TranslatorJobProperties();

  private final Configuration configuration = new Configuration(Configuration.VERSION_2_3_32);

  private final Random random = new Random();

  private SourceSystemService service;
  @Mock
  private KafkaPublisherService kafkaPublisherService;
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

  private MockedStatic<Instant> mockedStatic;
  private MockedStatic<Clock> mockedClock;
  Clock updatedClock = Clock.fixed(UPDATED, ZoneOffset.UTC);


  private static V1CronJob getV1CronJob(String image, String name) {
    var container = new V1Container();
    container.setImage(image);
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

  @BeforeEach
  void setup() throws IOException {
    jobProperties.setDatabaseUrl("jdbc:postgresql://localhost:5432/translator");
    service = new SourceSystemService(fdoRecordService, handleComponent, repository,
        dataMappingService, kafkaPublisherService, MAPPER, yamlMapper, jobProperties, configuration,
        batchV1Api, random, fdoProperties);
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
    var createJob = mock(APIcreateNamespacedJobRequest.class);
    given(
        batchV1Api.createNamespacedJob(eq(NAMESPACE), any(V1Job.class))).willReturn(createJob);

    // When
    var result = service.createSourceSystem(sourceSystem, OBJECT_CREATOR, SYSTEM_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
    then(repository).should().createSourceSystem(givenSourceSystem());
    then(kafkaPublisherService).should()
        .publishCreateEvent(MAPPER.valueToTree(givenSourceSystem()));
  }

  @Test
  void testCreateSourceSystemMappingNotFound() {
    // Given
    var sourceSystem = givenSourceSystemRequest();
    given(dataMappingService.getActiveDataMapping(sourceSystem.getOdsDataMappingID())).willReturn(
        Optional.empty());

    assertThrowsExactly(NotFoundException.class,
        () -> service.createSourceSystem(sourceSystem, OBJECT_CREATOR, SYSTEM_PATH));
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
        () -> service.createSourceSystem(sourceSystem, OBJECT_CREATOR, SYSTEM_PATH));

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
        () -> service.createSourceSystem(sourceSystem, OBJECT_CREATOR, SYSTEM_PATH));

    // Then
    then(repository).should().createSourceSystem(givenSourceSystem());
    then(fdoRecordService).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());
    then(repository).should().rollbackSourceSystemCreation(HANDLE);
  }

  @Test
  void testCreateSourceSystemKafkaFails() throws Exception {
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
    willThrow(JsonProcessingException.class).given(kafkaPublisherService)
        .publishCreateEvent(MAPPER.valueToTree(givenSourceSystem()));

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.createSourceSystem(sourceSystem, OBJECT_CREATOR, SYSTEM_PATH));

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
        service.createSourceSystem(sourceSystem, OBJECT_CREATOR, MAPPING_PATH));
  }

  @Test
  void testCreateSourceSystemKafkaAndRollbackFails() throws Exception {
    // Given
    var sourceSystem = givenSourceSystemRequest(SourceSystemRequest.OdsTranslatorType.DWCA);
    given(handleComponent.postHandle(any())).willReturn(BARE_HANDLE);
    given(fdoProperties.getSourceSystemType()).willReturn(SOURCE_SYSTEM_TYPE_DOI);
    given(dataMappingService.getActiveDataMapping(sourceSystem.getOdsDataMappingID())).willReturn(
        Optional.of(givenDataMapping(sourceSystem.getOdsDataMappingID(), 1)));
    willThrow(JsonProcessingException.class).given(kafkaPublisherService)
        .publishCreateEvent(MAPPER.valueToTree(givenSourceSystem(OdsTranslatorType.DWCA)));
    willThrow(PidException.class).given(handleComponent).rollbackHandleCreation(any());
    var createCron = mock(APIcreateNamespacedCronJobRequest.class);
    given(batchV1Api.createNamespacedCronJob(eq(NAMESPACE), any(V1CronJob.class)))
        .willReturn(createCron);
    var deleteCron = mock(APIdeleteNamespacedCronJobRequest.class);
    given(batchV1Api.deleteNamespacedCronJob(anyString(), eq(NAMESPACE))).willReturn(deleteCron);
    var createJob = mock(APIcreateNamespacedJobRequest.class);
    given(
        batchV1Api.createNamespacedJob(eq(NAMESPACE), any(V1Job.class))).willReturn(createJob);

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.createSourceSystem(sourceSystem, OBJECT_CREATOR, SYSTEM_PATH));

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
    assertDoesNotThrow(() -> service.runSourceSystemById(HANDLE));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testUpdateSourceSystem(boolean triggerTranslator) throws Exception {
    var sourceSystem = givenSourceSystemRequest();
    var prevSourceSystem = Optional.of(givenSourceSystem(OdsTranslatorType.DWCA).withOdsMaximumRecords(25));
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

    // When
    var result = service.updateSourceSystem(BARE_HANDLE, sourceSystem, OBJECT_CREATOR, SYSTEM_PATH,
        triggerTranslator);

    // Then
    assertThat(result).isEqualTo(expected);
    then(repository).should().updateSourceSystem(givenSourceSystem(2));
    if (!triggerTranslator) {
      then(batchV1Api).shouldHaveNoMoreInteractions();
    } else {
      then(batchV1Api).should().createNamespacedJob(eq(NAMESPACE), any(V1Job.class));
    }
    then(kafkaPublisherService).should()
        .publishUpdateEvent(MAPPER.valueToTree(givenSourceSystem(2)),
            MAPPER.valueToTree(prevSourceSystem.get()));
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

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.updateSourceSystem(BARE_HANDLE, sourceSystem, OBJECT_CREATOR, SYSTEM_PATH,
            false));

    // Then
    then(repository).should().updateSourceSystem(givenSourceSystem(2));
    then(repository).should().updateSourceSystem(prevSourceSystem.get());
  }

  @Test
  void testUpdateSourceSystemKafkaFails() throws Exception {
    var sourceSystem = givenSourceSystemRequest();
    var prevSourceSystem = Optional.of(givenSourceSystem(OdsTranslatorType.DWCA));
    given(repository.getActiveSourceSystem(BARE_HANDLE)).willReturn(prevSourceSystem);
    given(fdoProperties.getSourceSystemType()).willReturn(SOURCE_SYSTEM_TYPE_DOI);
    willThrow(JsonProcessingException.class).given(kafkaPublisherService)
        .publishUpdateEvent(MAPPER.valueToTree(givenSourceSystem(2)),
            MAPPER.valueToTree(prevSourceSystem.get()));
    var updateCron = mock(APIreplaceNamespacedCronJobRequest.class);
    given(batchV1Api.replaceNamespacedCronJob(anyString(), eq(NAMESPACE), any(V1CronJob.class)))
        .willReturn(updateCron);

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.updateSourceSystem(BARE_HANDLE, sourceSystem, OBJECT_CREATOR, SYSTEM_PATH,
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

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.updateSourceSystem(BARE_HANDLE, sourceSystem, OBJECT_CREATOR, SYSTEM_PATH,
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
        () -> service.updateSourceSystem(HANDLE, sourceSystem, OBJECT_CREATOR, SYSTEM_PATH, true));
  }

  @Test
  void testUpdateSourceSystemNoChanges() throws Exception {
    // Given
    var sourceSystem = givenSourceSystemRequest();
    given(repository.getActiveSourceSystem(BARE_HANDLE)).willReturn(
        Optional.of(givenSourceSystem()));

    // When
    var result = service.updateSourceSystem(BARE_HANDLE, sourceSystem, OBJECT_CREATOR, SYSTEM_PATH,
        false);

    // Then
    assertThat(result).isNull();
  }

  @Test
  void testGetSourceSystemById() {
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
  void testGetSourceSystemByIdIsDeleted() {
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
    mockedStatic.when(Instant::now).thenReturn(UPDATED);
    mockedClock.when(Clock::systemUTC).thenReturn(updatedClock);

    // When
    service.tombstoneSourceSystem(BARE_HANDLE, givenAgent());

    // Then
    then(repository).should().tombstoneSourceSystem(givenTombstoneSourceSystem(), UPDATED);
    then(handleComponent).should().tombstoneHandle(any(), eq(BARE_HANDLE));
    then(kafkaPublisherService).should().publishTombstoneEvent(any(), any());
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
  void setupTest() throws ApiException {
    // Given
    var cronResponse = mock(APIlistNamespacedCronJobRequest.class);
    var cronName = "cron-1";
    var updatedCron = getV1CronJob("a-random-image", cronName);
    var expectedCron = getV1CronJob(jobProperties.getImage(), cronName);
    var equalCron = getV1CronJob(jobProperties.getImage(), "cron-2");
    given(batchV1Api.listNamespacedCronJob(NAMESPACE)).willReturn(cronResponse);
    given(cronResponse.execute()).willReturn(
        new V1CronJobList().addItemsItem(updatedCron).addItemsItem(equalCron));
    var replaceResponse = mock(APIreplaceNamespacedCronJobRequest.class);
    given(batchV1Api.replaceNamespacedCronJob(cronName, NAMESPACE, expectedCron)).willReturn(
        replaceResponse);

    // When
    service.setup();

    // Then
    then(batchV1Api).should(times(1)).replaceNamespacedCronJob(cronName, NAMESPACE,
        expectedCron);
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
