package eu.dissco.orchestration.backend.service;

import static eu.dissco.orchestration.backend.service.SourceSystemService.SUBJECT_TYPE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.CREATED;
import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mockStatic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import eu.dissco.orchestration.backend.domain.HandleType;
import eu.dissco.orchestration.backend.domain.SourceSystem;
import eu.dissco.orchestration.backend.domain.SourceSystemRecord;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiData;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.exception.ProcessingFailedException;
import eu.dissco.orchestration.backend.repository.SourceSystemRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SourceSystemServiceTest {

  private SourceSystemService service;
  @Mock
  private HandleService handleService;
  @Mock
  private KafkaPublisherService kafkaPublisherService;
  @Mock
  private SourceSystemRepository repository;
  @Mock
  private MappingService mappingService;

  private MockedStatic<Instant> mockedStatic;
  private MockedStatic<Clock> mockedClock;

  @BeforeEach
  void setup() {
    service = new SourceSystemService(repository, handleService, mappingService,
        kafkaPublisherService, MAPPER);
    initTime();
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
    given(handleService.createNewHandle(HandleType.SOURCE_SYSTEM)).willReturn(HANDLE);
    given(mappingService.getActiveMapping(sourceSystem.mappingId())).willReturn(
        Optional.of(givenMappingRecord(sourceSystem.mappingId(), 1)));

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
  void testCreateSourceSystemKafakFails() throws Exception {
    // Given
    var sourceSystem = givenSourceSystem();
    given(handleService.createNewHandle(HandleType.SOURCE_SYSTEM)).willReturn(HANDLE);
    given(mappingService.getActiveMapping(sourceSystem.mappingId())).willReturn(
        Optional.of(givenMappingRecord(sourceSystem.mappingId(), 1)));
    willThrow(JsonProcessingException.class).given(kafkaPublisherService)
        .publishCreateEvent(HANDLE, MAPPER.valueToTree(givenSourceSystemRecord()), SUBJECT_TYPE);

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.createSourceSystem(sourceSystem, OBJECT_CREATOR, SYSTEM_PATH));

    // Then
    then(repository).should().createSourceSystem(givenSourceSystemRecord());
    then(handleService).should().rollbackHandleCreation(HANDLE);
    then(repository).should().rollbackSourceSystemCreation(HANDLE);
  }

  @Test
  void testUpdateSourceSystem() throws Exception {
    var sourceSystem = givenSourceSystem();
    var prevRecord = Optional.of(new SourceSystemRecord(
        HANDLE,
        1,
        OBJECT_CREATOR,
        CREATED,
        null, new SourceSystem("name", "endpoint", "description", "id")
    ));
    var expected = givenSourceSystemSingleJsonApiWrapper(2);
    given(repository.getActiveSourceSystem(HANDLE)).willReturn(prevRecord);

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
  void testUpdateSourceSystemKafkaFails() throws Exception {
    var sourceSystem = givenSourceSystem();
    var prevRecord = Optional.of(new SourceSystemRecord(
        HANDLE,
        1,
        OBJECT_CREATOR,
        CREATED,
        null, new SourceSystem("name", "endpoint", "description", "id")
    ));
    given(repository.getActiveSourceSystem(HANDLE)).willReturn(prevRecord);
    willThrow(JsonProcessingException.class).given(kafkaPublisherService)
        .publishUpdateEvent(HANDLE, MAPPER.valueToTree(givenSourceSystemRecord(2)),
            givenJsonPatch(), SUBJECT_TYPE);

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.updateSourceSystem(HANDLE, sourceSystem, OBJECT_CREATOR, SYSTEM_PATH));

    // Then
    then(repository).should().updateSourceSystem(givenSourceSystemRecord(2));
    then(repository).should().updateSourceSystem(prevRecord.get());
  }

  private JsonNode givenJsonPatch() throws JsonProcessingException {
    return MAPPER.readTree("[{\"op\":\"replace\",\"path\":\"/mappingId\",\"value\":\"id\"},"
        + "{\"op\":\"replace\",\"path\":\"/endpoint\",\"value\":\"endpoint\"},{\"op\":\"replace\""
        + ",\"path\":\"/name\",\"value\":\"name\"},{\"op\":\"replace\",\"path\":\"/description\""
        + ",\"value\":\"description\"}]");
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
  void testGetSourceSystemById() throws Exception {
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
        new JsonApiData(HANDLE, HandleType.SOURCE_SYSTEM,
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
    // Then
    assertDoesNotThrow(() -> service.deleteSourceSystem(HANDLE));
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
