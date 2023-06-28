package eu.dissco.orchestration.backend.service;

import static eu.dissco.orchestration.backend.service.MappingService.SUBJECT_TYPE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.CREATED;
import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPING_PATH;
import static eu.dissco.orchestration.backend.testutils.TestUtils.OBJECT_CREATOR;
import static eu.dissco.orchestration.backend.testutils.TestUtils.OBJECT_DESCRIPTION;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SANDBOX_URI;
import static eu.dissco.orchestration.backend.testutils.TestUtils.flattenMappingRecord;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMapping;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMappingRecord;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMappingRecordResponse;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMappingSingleJsonApiWrapper;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mockStatic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import eu.dissco.orchestration.backend.domain.HandleType;
import eu.dissco.orchestration.backend.domain.Mapping;
import eu.dissco.orchestration.backend.domain.MappingRecord;
import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiData;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.exception.PidCreationException;
import eu.dissco.orchestration.backend.exception.ProcessingFailedException;
import eu.dissco.orchestration.backend.repository.MappingRepository;
import eu.dissco.orchestration.backend.web.FdoRecordBuilder;
import eu.dissco.orchestration.backend.web.HandleComponent;
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
class MappingServiceTest {

  private MappingService service;
  @Mock
  private FdoRecordBuilder fdoRecordBuilder;
  @Mock
  private HandleComponent handleComponent;
  @Mock
  private KafkaPublisherService kafkaPublisherService;
  @Mock
  private MappingRepository repository;

  private MockedStatic<Instant> mockedStatic;
  private MockedStatic<Clock> mockedClock;

  @BeforeEach
  void setup() {
    service = new MappingService(fdoRecordBuilder,handleComponent, kafkaPublisherService, repository, MAPPER);
    initTime();
  }

  @AfterEach
  void destroy() {
    mockedStatic.close();
    mockedClock.close();
  }

  @Test
  void testCreateMapping() throws Exception {
    // Given
    var mapping = givenMapping();
    var expected = givenMappingSingleJsonApiWrapper();
    given(handleComponent.postHandle(any())).willReturn(HANDLE);

    // When
    var result = service.createMapping(mapping, OBJECT_CREATOR, MAPPING_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
    then(fdoRecordBuilder).should().buildCreateRequest(mapping, ObjectType.MAPPING);
    then(repository).should().createMapping(givenMappingRecord(HANDLE, 1));
    then(kafkaPublisherService).should()
        .publishCreateEvent(HANDLE, MAPPER.valueToTree(givenMappingRecord(HANDLE, 1)),
            SUBJECT_TYPE);
  }

  @Test
  void testCreateMappingKafkaFails() throws Exception {
    // Given
    var mapping = givenMapping();
    given(handleComponent.postHandle(any())).willReturn(HANDLE);
    willThrow(JsonProcessingException.class).given(kafkaPublisherService)
        .publishCreateEvent(HANDLE, MAPPER.valueToTree(givenMappingRecord(HANDLE, 1)),
            SUBJECT_TYPE);

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.createMapping(mapping, OBJECT_CREATOR, MAPPING_PATH));

    // Then
    then(fdoRecordBuilder).should().buildCreateRequest(mapping, ObjectType.MAPPING);
    then(repository).should().createMapping(givenMappingRecord(HANDLE, 1));
    then(fdoRecordBuilder).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());
    then(repository).should().rollbackMappingCreation(HANDLE);
  }

  @Test
  void testCreateMappingKafkaAndRollbackFails() throws Exception {
    // Given
    var mapping = givenMapping();
    given(handleComponent.postHandle(any())).willReturn(HANDLE);
    willThrow(JsonProcessingException.class).given(kafkaPublisherService)
        .publishCreateEvent(HANDLE, MAPPER.valueToTree(givenMappingRecord(HANDLE, 1)),
            SUBJECT_TYPE);
    willThrow(PidCreationException.class).given(handleComponent).rollbackHandleCreation(any());

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.createMapping(mapping, OBJECT_CREATOR, MAPPING_PATH));

    // Then
    then(fdoRecordBuilder).should().buildCreateRequest(mapping, ObjectType.MAPPING);
    then(repository).should().createMapping(givenMappingRecord(HANDLE, 1));
    then(fdoRecordBuilder).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());
    then(repository).should().rollbackMappingCreation(HANDLE);
  }


  @Test
  void testUpdateMapping() throws Exception {
    // Given
    var prevMapping = new Mapping("old name", OBJECT_DESCRIPTION, MAPPER.createObjectNode(),
        "dwc");
    var prevRecord = Optional.of(
        new MappingRecord(HANDLE, 1, CREATED, null, OBJECT_CREATOR, prevMapping));
    var mapping = givenMapping();
    var expected = givenMappingSingleJsonApiWrapper(2);

    given(repository.getActiveMapping(HANDLE)).willReturn(prevRecord);

    // When
    var result = service.updateMapping(HANDLE, mapping, OBJECT_CREATOR, MAPPING_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
    then(repository).should().updateMapping(givenMappingRecord(HANDLE, 2));
    then(kafkaPublisherService).should()
        .publishUpdateEvent(HANDLE, MAPPER.valueToTree(givenMappingRecord(HANDLE, 2)),
            givenJsonPatch(), SUBJECT_TYPE);
  }

  @Test
  void testUpdateMappingKafkaFails() throws Exception {
    // Given
    var prevMapping = new Mapping("old name", OBJECT_DESCRIPTION, MAPPER.createObjectNode(),
        "dwc");
    var prevRecord = Optional.of(
        new MappingRecord(HANDLE, 1, CREATED, null, OBJECT_CREATOR, prevMapping));
    var mapping = givenMapping();
    given(repository.getActiveMapping(HANDLE)).willReturn(prevRecord);
    willThrow(JsonProcessingException.class).given(kafkaPublisherService)
        .publishUpdateEvent(HANDLE, MAPPER.valueToTree(givenMappingRecord(HANDLE, 2)),
            givenJsonPatch(), SUBJECT_TYPE);

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.updateMapping(HANDLE, mapping, OBJECT_CREATOR, MAPPING_PATH));

    // Then
    then(repository).should().updateMapping(givenMappingRecord(HANDLE, 2));
    then(repository).should().updateMapping(prevRecord.get());
  }

  private JsonNode givenJsonPatch() throws JsonProcessingException {
    return MAPPER.readTree("[{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"old name\"}]");
  }

  @Test
  void testUpdateMappingNoChanges() throws Exception {
    // Given
    var prevMapping = Optional.of(givenMappingRecord(HANDLE, 1));
    var mapping = givenMapping();

    given(repository.getActiveMapping(HANDLE)).willReturn(prevMapping);

    // When
    var result = service.updateMapping(HANDLE, mapping, OBJECT_CREATOR, MAPPING_PATH);

    // Then
    assertThat(result).isNull();
  }

  @Test
  void testUpdateMappingNotFound() {
    // Given
    given(repository.getActiveMapping(HANDLE)).willReturn(Optional.empty());

    // Then
    assertThrows(NotFoundException.class,
        () -> service.updateMapping(HANDLE, givenMapping(), OBJECT_CREATOR, OBJECT_CREATOR));
  }


  @Test
  void testGetMappingById() {
    // Given
    var mappingRecord = givenMappingRecord(HANDLE, 1);
    given(repository.getMapping(HANDLE)).willReturn(mappingRecord);
    var expected = givenMappingSingleJsonApiWrapper();

    // When
    var result = service.getMappingById(HANDLE, MAPPING_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testGetMappingByIdIsDeleted() {
    // Given
    var mappingRecord = new MappingRecord(HANDLE, 1, CREATED, CREATED, OBJECT_CREATOR,
        givenMapping());
    given(repository.getMapping(HANDLE)).willReturn(mappingRecord);
    var expected = new JsonApiWrapper(
        new JsonApiData(HANDLE, HandleType.MAPPING, flattenMappingRecord(mappingRecord)),
        new JsonApiLinks(MAPPING_PATH));

    // When
    var result = service.getMappingById(HANDLE, MAPPING_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testDeleteMapping() {
    // Given
    given(repository.getActiveMapping(HANDLE)).willReturn(
        Optional.of(givenMappingRecord(HANDLE, 1)));

    // Then
    assertDoesNotThrow(() -> service.deleteMapping(HANDLE));
  }

  @Test
  void testDeleteMappingNotFound() {
    // Given
    given(repository.getActiveMapping(HANDLE)).willReturn(Optional.empty());

    // Then
    assertThrowsExactly(NotFoundException.class, () -> service.deleteMapping(HANDLE));
  }

  @Test
  void testGetMappings() {
    // Given
    int pageSize = 10;
    int pageNum = 1;
    List<MappingRecord> mappingRecords = Collections.nCopies(pageSize + 1,
        givenMappingRecord(HANDLE, 1));
    given(repository.getMappings(pageNum, pageSize)).willReturn(mappingRecords);
    var linksNode = new JsonApiLinks(pageSize, pageNum, true, SANDBOX_URI);
    var expected = givenMappingRecordResponse(mappingRecords.subList(0, pageSize), linksNode);

    // When
    var result = service.getMappings(pageNum, pageSize, SANDBOX_URI);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testGetMappingsLastPage() {
    // Given
    int pageSize = 10;
    int pageNum = 2;
    List<MappingRecord> mappingRecords = Collections.nCopies(pageSize,
        givenMappingRecord(HANDLE, 1));
    given(repository.getMappings(pageNum, pageSize)).willReturn(mappingRecords);
    var linksNode = new JsonApiLinks(pageSize, pageNum, false, SANDBOX_URI);
    var expected = givenMappingRecordResponse(mappingRecords, linksNode);

    // When
    var result = service.getMappings(pageNum, pageSize, SANDBOX_URI);

    // Then
    assertThat(result).isEqualTo(expected);
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
