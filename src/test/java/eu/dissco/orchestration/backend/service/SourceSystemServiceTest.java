package eu.dissco.orchestration.backend.service;

import static eu.dissco.orchestration.backend.database.jooq.enums.TranslatorType.dwca;
import static eu.dissco.orchestration.backend.service.SourceSystemService.SUBJECT_TYPE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.CREATED;
import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPING_PATH;
import static eu.dissco.orchestration.backend.testutils.TestUtils.OBJECT_CREATOR;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SANDBOX_URI;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SYSTEM_PATH;
import static eu.dissco.orchestration.backend.testutils.TestUtils.flattenSourceSystemRecord;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMappingRecord;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystem;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemRecord;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemRecordResponse;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemSingleJsonApiWrapper;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.domain.SourceSystem;
import eu.dissco.orchestration.backend.domain.SourceSystemRecord;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiData;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.exception.PidCreationException;
import eu.dissco.orchestration.backend.exception.ProcessingFailedException;
import eu.dissco.orchestration.backend.properties.TranslatorJobProperties;
import eu.dissco.orchestration.backend.repository.SourceSystemRepository;
import eu.dissco.orchestration.backend.web.HandleComponent;
import freemarker.template.Configuration;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.BatchV1Api;
import io.kubernetes.client.openapi.apis.BatchV1Api.APIcreateNamespacedCronJobRequest;
import io.kubernetes.client.openapi.apis.BatchV1Api.APIcreateNamespacedJobRequest;
import io.kubernetes.client.openapi.apis.BatchV1Api.APIdeleteNamespacedCronJobRequest;
import io.kubernetes.client.openapi.apis.BatchV1Api.APIreplaceNamespacedCronJobRequest;
import io.kubernetes.client.openapi.models.V1CronJob;
import io.kubernetes.client.openapi.models.V1Job;
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

  private SourceSystemService service;
  @Mock
  private KafkaPublisherService kafkaPublisherService;
  @Mock
  private SourceSystemRepository repository;
  @Mock
  private FdoRecordService builder;
  @Mock
  private HandleComponent handleComponent;
  @Mock
  private MappingService mappingService;
  @Mock
  private BatchV1Api batchV1Api;
  private Random random = new Random();


  private MockedStatic<Instant> mockedStatic;
  private MockedStatic<Clock> mockedClock;

  @BeforeEach
  void setup() throws IOException {
    jobProperties.setDatabaseUrl("jdbc:postgresql://localhost:5432/translator");
    service = new SourceSystemService(builder, handleComponent, repository, mappingService,
        kafkaPublisherService, MAPPER, yamlMapper, jobProperties, configuration, batchV1Api, random);
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
    var sourceSystem = givenSourceSystem();
    given(handleComponent.postHandle(any())).willReturn(HANDLE);
    given(mappingService.getActiveMapping(sourceSystem.mappingId())).willReturn(
        Optional.of(givenMappingRecord(sourceSystem.mappingId(), 1)));
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
    then(repository).should().createSourceSystem(givenSourceSystemRecord());
    then(kafkaPublisherService).should()
        .publishCreateEvent(HANDLE, MAPPER.valueToTree(givenSourceSystemRecord()), SUBJECT_TYPE);
  }

  @Test
  void testCreateSourceSystemMappingNotFound() {
    // Given
    var sourceSystem = givenSourceSystem();
    given(mappingService.getActiveMapping(sourceSystem.mappingId())).willReturn(Optional.empty());

    assertThrowsExactly(NotFoundException.class,
        () -> service.createSourceSystem(sourceSystem, OBJECT_CREATOR, SYSTEM_PATH));
  }

  @Test
  void testCreateSourceSystemCronFails() throws Exception {
    // Given
    var sourceSystem = givenSourceSystem();
    given(handleComponent.postHandle(any())).willReturn(HANDLE);
    given(mappingService.getActiveMapping(sourceSystem.mappingId())).willReturn(
        Optional.of(givenMappingRecord(sourceSystem.mappingId(), 1)));
    var createCron = mock(APIcreateNamespacedCronJobRequest.class);
    given(batchV1Api.createNamespacedCronJob(eq(NAMESPACE), any(V1CronJob.class)))
        .willReturn(createCron);
    given(createCron.execute()).willThrow(ApiException.class);

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.createSourceSystem(sourceSystem, OBJECT_CREATOR, SYSTEM_PATH));

    // Then
    then(repository).should().createSourceSystem(givenSourceSystemRecord());
    then(builder).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());
    then(repository).should().rollbackSourceSystemCreation(HANDLE);
  }

  @Test
  void testCreateSourceSystemTranslatorJobFails() throws Exception {
    // Given
    var sourceSystem = givenSourceSystem();
    given(handleComponent.postHandle(any())).willReturn(HANDLE);
    given(mappingService.getActiveMapping(sourceSystem.mappingId())).willReturn(
        Optional.of(givenMappingRecord(sourceSystem.mappingId(), 1)));
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
    then(repository).should().createSourceSystem(givenSourceSystemRecord());
    then(builder).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());
    then(repository).should().rollbackSourceSystemCreation(HANDLE);
  }

  @Test
  void testCreateSourceSystemKafkaFails() throws Exception {
    // Given
    var sourceSystem = givenSourceSystem();
    given(handleComponent.postHandle(any())).willReturn(HANDLE);
    given(mappingService.getActiveMapping(sourceSystem.mappingId())).willReturn(
        Optional.of(givenMappingRecord(sourceSystem.mappingId(), 1)));
    var createCron = mock(APIcreateNamespacedCronJobRequest.class);
    given(batchV1Api.createNamespacedCronJob(eq(NAMESPACE), any(V1CronJob.class)))
        .willReturn(createCron);
    var deleteCron = mock(APIdeleteNamespacedCronJobRequest.class);
    given(batchV1Api.deleteNamespacedCronJob(anyString(), eq(NAMESPACE))).willReturn(deleteCron);
    var createJob = mock(APIcreateNamespacedJobRequest.class);
    given(
        batchV1Api.createNamespacedJob(eq(NAMESPACE), any(V1Job.class))).willReturn(createJob);
    willThrow(JsonProcessingException.class).given(kafkaPublisherService)
        .publishCreateEvent(HANDLE, MAPPER.valueToTree(givenSourceSystemRecord()), SUBJECT_TYPE);

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.createSourceSystem(sourceSystem, OBJECT_CREATOR, SYSTEM_PATH));

    // Then
    then(repository).should().createSourceSystem(givenSourceSystemRecord());
    then(builder).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());
    then(repository).should().rollbackSourceSystemCreation(HANDLE);
  }

  @Test
  void testCreateMasHandleFails() throws Exception {
    // Given
    var sourceSystem = givenSourceSystem();
    given(mappingService.getActiveMapping(sourceSystem.mappingId())).willReturn(
        Optional.of(givenMappingRecord(sourceSystem.mappingId(), 1)));
    willThrow(PidCreationException.class).given(handleComponent).postHandle(any());

    // When / Then
    assertThrowsExactly(ProcessingFailedException.class, () ->
        service.createSourceSystem(sourceSystem, OBJECT_CREATOR, MAPPING_PATH));
  }

  @Test
  void testCreateSourceSystemKafkaAndRollbackFails() throws Exception {
    // Given
    var sourceSystem = givenSourceSystem(dwca);
    given(handleComponent.postHandle(any())).willReturn(HANDLE);
    given(mappingService.getActiveMapping(sourceSystem.mappingId())).willReturn(
        Optional.of(givenMappingRecord(sourceSystem.mappingId(), 1)));
    willThrow(JsonProcessingException.class).given(kafkaPublisherService)
        .publishCreateEvent(HANDLE, MAPPER.valueToTree(givenSourceSystemRecord(dwca)),
            SUBJECT_TYPE);
    willThrow(PidCreationException.class).given(handleComponent).rollbackHandleCreation(any());
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
    then(repository).should().createSourceSystem(givenSourceSystemRecord(dwca));
    then(builder).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());
    then(repository).should().rollbackSourceSystemCreation(HANDLE);
  }

  @Test
  void testRunSourceSystemById() {
    // Given
    var sourceSystemRecord = givenSourceSystemRecord();
    given(repository.getSourceSystem(HANDLE)).willReturn(sourceSystemRecord);
    var createJob = mock(APIcreateNamespacedJobRequest.class);
    given(
        batchV1Api.createNamespacedJob(eq(NAMESPACE), any(V1Job.class))).willReturn(createJob);

    // Then
    assertDoesNotThrow(() -> service.runSourceSystemById(HANDLE));
  }

  @Test
  void testUpdateSourceSystem() throws Exception {
    var sourceSystem = givenSourceSystem();
    var prevRecord = Optional.of(new SourceSystemRecord(
        HANDLE,
        1,
        OBJECT_CREATOR,
        CREATED,
        null, new SourceSystem("name", "endpoint", "description", dwca, "id")
    ));
    var expected = givenSourceSystemSingleJsonApiWrapper(2);
    given(repository.getActiveSourceSystem(HANDLE)).willReturn(prevRecord);
    var updateCron = mock(APIreplaceNamespacedCronJobRequest.class);
    given(batchV1Api.replaceNamespacedCronJob(anyString(), eq(NAMESPACE), any(V1CronJob.class)))
        .willReturn(updateCron);

    // When
    var result = service.updateSourceSystem(HANDLE, sourceSystem, OBJECT_CREATOR, SYSTEM_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
    then(repository).should().updateSourceSystem(givenSourceSystemRecord(2));
    then(kafkaPublisherService).should()
        .publishUpdateEvent(HANDLE, MAPPER.valueToTree(givenSourceSystemRecord(2)),
            givenJsonPatch(), SUBJECT_TYPE);
  }

  @Test
  void testUpdateSourceSystemCronJobFails() throws Exception {
    var sourceSystem = givenSourceSystem();
    var prevRecord = Optional.of(new SourceSystemRecord(
        HANDLE,
        1,
        OBJECT_CREATOR,
        CREATED,
        null, new SourceSystem("name", "endpoint", "description", dwca, "id")
    ));
    given(repository.getActiveSourceSystem(HANDLE)).willReturn(prevRecord);
    var updateCron = mock(APIreplaceNamespacedCronJobRequest.class);
    given(batchV1Api.replaceNamespacedCronJob(anyString(), eq(NAMESPACE), any(V1CronJob.class)))
        .willReturn(updateCron);
    given(updateCron.execute()).willThrow(ApiException.class);

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.updateSourceSystem(HANDLE, sourceSystem, OBJECT_CREATOR, SYSTEM_PATH));

    // Then
    then(repository).should().updateSourceSystem(givenSourceSystemRecord(2));
    then(repository).should().updateSourceSystem(prevRecord.get());
  }

  @Test
  void testUpdateSourceSystemKafkaFails() throws Exception {
    var sourceSystem = givenSourceSystem();
    var prevRecord = Optional.of(new SourceSystemRecord(
        HANDLE,
        1,
        OBJECT_CREATOR,
        CREATED,
        null, new SourceSystem("name", "endpoint", "description", dwca, "id")
    ));
    given(repository.getActiveSourceSystem(HANDLE)).willReturn(prevRecord);
    willThrow(JsonProcessingException.class).given(kafkaPublisherService)
        .publishUpdateEvent(HANDLE, MAPPER.valueToTree(givenSourceSystemRecord(2)),
            givenJsonPatch(), SUBJECT_TYPE);
    var updateCron = mock(APIreplaceNamespacedCronJobRequest.class);
    given(batchV1Api.replaceNamespacedCronJob(anyString(), eq(NAMESPACE), any(V1CronJob.class)))
        .willReturn(updateCron);

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.updateSourceSystem(HANDLE, sourceSystem, OBJECT_CREATOR, SYSTEM_PATH));

    // Then
    then(repository).should().updateSourceSystem(givenSourceSystemRecord(2));
    then(repository).should().updateSourceSystem(prevRecord.get());
  }

  private JsonNode givenJsonPatch() throws JsonProcessingException {
    return MAPPER.readTree(
        "[{\"op\":\"replace\",\"path\":\"/mappingId\",\"value\":\"id\"},{\"op\":\"replace\","
            + "\"path\":\"/endpoint\",\"value\":\"endpoint\"},{\"op\":\"replace\","
            + "\"path\":\"/translatorType\",\"value\":\"dwca\"},{\"op\":\"replace\","
            + "\"path\":\"/name\",\"value\":\"name\"},{\"op\":\"replace\",\"path\":\"/description\","
            + "\"value\":\"description\"}]");
  }

  @Test
  void testUpdateSourceSystemNotFound() {
    // Given
    var sourceSystem = givenSourceSystem();
    given(repository.getActiveSourceSystem(HANDLE)).willReturn(Optional.empty());

    // Then
    assertThrowsExactly(NotFoundException.class,
        () -> service.updateSourceSystem(HANDLE, sourceSystem, OBJECT_CREATOR, SYSTEM_PATH));
  }

  @Test
  void testUpdateSourceSystemNoChanges() throws Exception {
    // Given
    var sourceSystem = givenSourceSystem();
    given(repository.getActiveSourceSystem(HANDLE)).willReturn(
        Optional.of(givenSourceSystemRecord()));

    // When
    var result = service.updateSourceSystem(HANDLE, sourceSystem, OBJECT_CREATOR, SYSTEM_PATH);

    // Then
    assertThat(result).isNull();
  }

  @Test
  void testGetSourceSystemById() {
    // Given
    var sourceSystemRecord = givenSourceSystemRecord();
    var expected = givenSourceSystemSingleJsonApiWrapper();

    given(repository.getSourceSystem(HANDLE)).willReturn(sourceSystemRecord);

    // When
    var result = service.getSourceSystemById(HANDLE, SYSTEM_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testGetSourceSystemByIdIsDeleted() {
    // Given
    var sourceSystemRecord = new SourceSystemRecord(
        HANDLE, 1, OBJECT_CREATOR, CREATED, CREATED, givenSourceSystem());
    var expected = new JsonApiWrapper(
        new JsonApiData(HANDLE, ObjectType.SOURCE_SYSTEM,
            flattenSourceSystemRecord(sourceSystemRecord)),
        new JsonApiLinks(SYSTEM_PATH));

    given(repository.getSourceSystem(HANDLE)).willReturn(sourceSystemRecord);

    // When
    var result = service.getSourceSystemById(HANDLE, SYSTEM_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getSourceSystemRecords() {
    // Given
    int pageNum = 1;
    int pageSize = 10;
    String path = SANDBOX_URI;
    List<SourceSystemRecord> ssRecords = Collections.nCopies(pageSize + 1,
        givenSourceSystemRecord());
    given(repository.getSourceSystems(pageNum, pageSize)).willReturn(ssRecords);
    var linksNode = new JsonApiLinks(pageSize, pageNum, true, path);
    var expected = givenSourceSystemRecordResponse(ssRecords.subList(0, pageSize), linksNode);

    // When
    var result = service.getSourceSystemRecords(pageNum, pageSize, path);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void getSourceSystemRecordsLastPage() {
    // Given
    int pageNum = 2;
    int pageSize = 10;
    String path = SANDBOX_URI;
    List<SourceSystemRecord> ssRecords = Collections.nCopies(pageSize, givenSourceSystemRecord());
    given(repository.getSourceSystems(pageNum, pageSize)).willReturn(ssRecords);
    var linksNode = new JsonApiLinks(pageSize, pageNum, false, path);
    var expected = givenSourceSystemRecordResponse(ssRecords, linksNode);

    // When
    var result = service.getSourceSystemRecords(pageNum, pageSize, path);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testDeleteSourceSystem() {
    // Given
    given(repository.getActiveSourceSystem(HANDLE)).willReturn(
        Optional.of(givenSourceSystemRecord()));
    var deleteCron = mock(APIdeleteNamespacedCronJobRequest.class);
    given(batchV1Api.deleteNamespacedCronJob(anyString(), eq(NAMESPACE)))
        .willReturn(deleteCron);

    // Then
    assertDoesNotThrow(() -> service.deleteSourceSystem(HANDLE));
  }

  @Test
  void testDeleteSourceSystemCronFailed() throws ApiException {
    // Given
    given(repository.getActiveSourceSystem(HANDLE)).willReturn(
        Optional.of(givenSourceSystemRecord()));
    var deleteCron = mock(APIdeleteNamespacedCronJobRequest.class);
    given(batchV1Api.deleteNamespacedCronJob(anyString(), eq(NAMESPACE)))
        .willReturn(deleteCron);
    given(deleteCron.execute()).willThrow(ApiException.class);

    // Then
    assertThrowsExactly(ProcessingFailedException.class, () -> service.deleteSourceSystem(HANDLE));
  }

  @Test
  void testDeleteSourceSystemNotFound() {
    // Given
    given(repository.getActiveSourceSystem(HANDLE)).willReturn(Optional.empty());

    // Then
    assertThrowsExactly(NotFoundException.class, () -> service.deleteSourceSystem(HANDLE));
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
