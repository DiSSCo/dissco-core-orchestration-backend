package eu.dissco.orchestration.backend.service;

import static eu.dissco.orchestration.backend.testutils.TestUtils.BARE_HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.CREATED;
import static eu.dissco.orchestration.backend.testutils.TestUtils.DATA_MAPPING_TYPE_DOI;
import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPING_PATH;
import static eu.dissco.orchestration.backend.testutils.TestUtils.OBJECT_CREATOR;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SANDBOX_URI;
import static eu.dissco.orchestration.backend.testutils.TestUtils.flattenDataMapping;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenDataMapping;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenDataMappingRequest;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenDataMappingSingleJsonApiWrapper;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMappingResponse;
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
import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiData;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.exception.PidCreationException;
import eu.dissco.orchestration.backend.exception.ProcessingFailedException;
import eu.dissco.orchestration.backend.properties.FdoProperties;
import eu.dissco.orchestration.backend.repository.DataMappingRepository;
import eu.dissco.orchestration.backend.schema.DataMapping;
import eu.dissco.orchestration.backend.testutils.TestUtils;
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
class DataMappingServiceTest {

  private DataMappingService service;
  @Mock
  private FdoRecordService fdoRecordService;
  @Mock
  private HandleComponent handleComponent;
  @Mock
  private KafkaPublisherService kafkaPublisherService;
  @Mock
  private DataMappingRepository repository;
  @Mock
  private FdoProperties fdoProperties;

  private MockedStatic<Instant> mockedStatic;
  private MockedStatic<Clock> mockedClock;

  @BeforeEach
  void setup() {
    service = new DataMappingService(fdoRecordService, handleComponent, kafkaPublisherService,
        repository, MAPPER, fdoProperties);
    initTime();
  }

  @AfterEach
  void destroy() {
    mockedStatic.close();
    mockedClock.close();
  }

  @Test
  void testCreateDataMapping() throws Exception {
    // Given
    var dataMapping = givenDataMappingRequest();
    given(fdoProperties.getDataMappingType()).willReturn(DATA_MAPPING_TYPE_DOI);
    var expected = givenDataMappingSingleJsonApiWrapper();
    given(handleComponent.postHandle(any())).willReturn(BARE_HANDLE);

    // When
    var result = service.createDataMapping(dataMapping, OBJECT_CREATOR, MAPPING_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
    then(fdoRecordService).should().buildCreateRequest(dataMapping, ObjectType.DATA_MAPPING);
    then(repository).should().createDataMapping(givenDataMapping(HANDLE, 1));
    then(kafkaPublisherService).should()
        .publishCreateEvent(MAPPER.valueToTree(givenDataMapping(HANDLE, 1)));
  }

  @Test
  void testCreateDataMappingKafkaFails() throws Exception {
    // Given
    var dataMapping = givenDataMappingRequest();
    given(fdoProperties.getDataMappingType()).willReturn(DATA_MAPPING_TYPE_DOI);
    given(handleComponent.postHandle(any())).willReturn(BARE_HANDLE);
    willThrow(JsonProcessingException.class).given(kafkaPublisherService)
        .publishCreateEvent(MAPPER.valueToTree(givenDataMapping(HANDLE, 1)));

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.createDataMapping(dataMapping, OBJECT_CREATOR, MAPPING_PATH));

    // Then
    then(fdoRecordService).should().buildCreateRequest(dataMapping, ObjectType.DATA_MAPPING);
    then(repository).should().createDataMapping(givenDataMapping(HANDLE, 1));
    then(fdoRecordService).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());
    then(repository).should().rollbackDataMappingCreation(HANDLE);
  }

  @Test
  void testCreateMasHandleFails() throws Exception {
    // Given
    var dataMapping = givenDataMappingRequest();
    willThrow(PidCreationException.class).given(handleComponent).postHandle(any());

    // Then
    assertThrowsExactly(ProcessingFailedException.class, () ->
        service.createDataMapping(dataMapping, OBJECT_CREATOR, MAPPING_PATH));
  }

  @Test
  void testCreateDataMappingKafkaAndRollbackFails() throws Exception {
    // Given
    var dataMapping = givenDataMappingRequest();
    given(fdoProperties.getDataMappingType()).willReturn(DATA_MAPPING_TYPE_DOI);
    given(handleComponent.postHandle(any())).willReturn(BARE_HANDLE);
    willThrow(JsonProcessingException.class).given(kafkaPublisherService)
        .publishCreateEvent(MAPPER.valueToTree(givenDataMapping(HANDLE, 1)));
    willThrow(PidCreationException.class).given(handleComponent).rollbackHandleCreation(any());

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.createDataMapping(dataMapping, OBJECT_CREATOR, MAPPING_PATH));

    // Then
    then(fdoRecordService).should().buildCreateRequest(dataMapping, ObjectType.DATA_MAPPING);
    then(repository).should().createDataMapping(givenDataMapping(HANDLE, 1));
    then(fdoRecordService).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());
    then(repository).should().rollbackDataMappingCreation(HANDLE);
  }


  @Test
  void testUpdateDataMapping() throws Exception {
    // Given
    var prevDataMapping = givenDataMapping(HANDLE, 1, "old name");
    var dataMapping = givenDataMappingRequest();
    var expected = TestUtils.givenDataMappingSingleJsonApiWrapper(2);
    given(fdoProperties.getDataMappingType()).willReturn(DATA_MAPPING_TYPE_DOI);
    given(repository.getActiveDataMapping(BARE_HANDLE)).willReturn(Optional.of(prevDataMapping));

    // When
    var result = service.updateDataMapping(BARE_HANDLE, dataMapping, OBJECT_CREATOR, MAPPING_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
    then(repository).should().updateDataMapping(givenDataMapping(HANDLE, 2));
    then(kafkaPublisherService).should()
        .publishUpdateEvent(MAPPER.valueToTree(givenDataMapping(HANDLE, 2)),
            MAPPER.valueToTree(prevDataMapping));
  }

  @Test
  void testUpdateDataMappingKafkaFails() throws Exception {
    // Given
    var prevDataMapping = givenDataMapping(HANDLE, 1, "old name");
    var dataMapping = givenDataMappingRequest();
    given(repository.getActiveDataMapping(BARE_HANDLE)).willReturn(Optional.of(prevDataMapping));
    given(fdoProperties.getDataMappingType()).willReturn(DATA_MAPPING_TYPE_DOI);
    willThrow(JsonProcessingException.class).given(kafkaPublisherService)
        .publishUpdateEvent(MAPPER.valueToTree(givenDataMapping(HANDLE, 2)),
            MAPPER.valueToTree(prevDataMapping));

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.updateDataMapping(BARE_HANDLE, dataMapping, OBJECT_CREATOR, MAPPING_PATH));

    // Then
    then(repository).should().updateDataMapping(givenDataMapping(HANDLE, 2));
    then(repository).should().updateDataMapping(prevDataMapping);
  }

  @Test
  void testUpdateDataMappingNoChanges() throws Exception {
    // Given
    var prevMapping = Optional.of(givenDataMapping(HANDLE, 1));
    var dataMapping = givenDataMappingRequest();

    given(repository.getActiveDataMapping(BARE_HANDLE)).willReturn(prevMapping);

    // When
    var result = service.updateDataMapping(BARE_HANDLE, dataMapping, OBJECT_CREATOR, MAPPING_PATH);

    // Then
    assertThat(result).isNull();
  }

  @Test
  void testUpdateDataMappingNotFound() {
    // Given
    given(repository.getActiveDataMapping(BARE_HANDLE)).willReturn(Optional.empty());

    // Then
    assertThrows(NotFoundException.class,
        () -> service.updateDataMapping(BARE_HANDLE, givenDataMappingRequest(), OBJECT_CREATOR,
            OBJECT_CREATOR));
  }


  @Test
  void testGetDataMappingById() {
    // Given
    var dataMapping = givenDataMapping(HANDLE, 1);
    given(repository.getDataMapping(BARE_HANDLE)).willReturn(dataMapping);
    var expected = givenDataMappingSingleJsonApiWrapper();

    // When
    var result = service.getDataMappingById(BARE_HANDLE, MAPPING_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testGetDataMappingByIdIsDeleted() {
    // Given
    var dataMapping = givenDataMapping();
    given(repository.getDataMapping(BARE_HANDLE)).willReturn(dataMapping);
    var expected = new JsonApiWrapper(
        new JsonApiData(HANDLE, ObjectType.DATA_MAPPING, flattenDataMapping(dataMapping)),
        new JsonApiLinks(MAPPING_PATH));

    // When
    var result = service.getDataMappingById(BARE_HANDLE, MAPPING_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testDeleteDataMapping() {
    // Given
    given(repository.getActiveDataMapping(BARE_HANDLE)).willReturn(
        Optional.of(givenDataMapping(HANDLE, 1)));

    // Then
    assertDoesNotThrow(() -> service.deleteDataMapping(BARE_HANDLE, OBJECT_CREATOR));
  }

  @Test
  void testDeleteDataMappingNotFound() {
    // Given
    given(repository.getActiveDataMapping(BARE_HANDLE)).willReturn(Optional.empty());

    // Then
    assertThrowsExactly(NotFoundException.class,
        () -> service.deleteDataMapping(BARE_HANDLE, OBJECT_CREATOR));
  }

  @Test
  void testGetDataMappings() {
    // Given
    int pageSize = 10;
    int pageNum = 1;
    List<DataMapping> dataMappings = Collections.nCopies(pageSize + 1,
        givenDataMapping(HANDLE, 1));
    given(repository.getDataMappings(pageNum, pageSize)).willReturn(dataMappings);
    var linksNode = new JsonApiLinks(pageSize, pageNum, true, SANDBOX_URI);
    var expected = givenMappingResponse(dataMappings.subList(0, pageSize), linksNode);

    // When
    var result = service.getDataMappings(pageNum, pageSize, SANDBOX_URI);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testGetDataMappingsLastPage() {
    // Given
    int pageSize = 10;
    int pageNum = 2;
    List<DataMapping> dataMappings = Collections.nCopies(pageSize,
        givenDataMapping(HANDLE, 1));
    given(repository.getDataMappings(pageNum, pageSize)).willReturn(dataMappings);
    var linksNode = new JsonApiLinks(pageSize, pageNum, false, SANDBOX_URI);
    var expected = givenMappingResponse(dataMappings, linksNode);

    // When
    var result = service.getDataMappings(pageNum, pageSize, SANDBOX_URI);

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
