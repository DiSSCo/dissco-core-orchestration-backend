package eu.dissco.orchestration.backend.service;

import static eu.dissco.orchestration.backend.service.MachineAnnotationServiceService.SUBJECT_TYPE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.CREATED;
import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAS_PATH;
import static eu.dissco.orchestration.backend.testutils.TestUtils.OBJECT_CREATOR;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMas;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMasRecord;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMasRecordResponse;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMasSingleJsonApiWrapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import eu.dissco.orchestration.backend.domain.MachineAnnotationService;
import eu.dissco.orchestration.backend.domain.MachineAnnotationServiceRecord;
import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.exception.PidCreationException;
import eu.dissco.orchestration.backend.exception.ProcessingFailedException;
import eu.dissco.orchestration.backend.repository.MachineAnnotationServiceRepository;
import eu.dissco.orchestration.backend.web.HandleComponent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MachineAnnotationServiceServiceTest {

  @Mock
  private MachineAnnotationServiceRepository repository;
  @Mock
  private KafkaPublisherService kafkaPublisherService;
  @Mock
  private HandleComponent handleComponent;
  @Mock
  private FdoRecordService fdoRecordService;

  private MachineAnnotationServiceService service;

  private MockedStatic<Instant> mockedStatic;
  private MockedStatic<Clock> mockedClock;

  @BeforeEach
  void setup() {
    initTime();
    service = new MachineAnnotationServiceService(handleComponent, fdoRecordService,
        kafkaPublisherService, repository, MAPPER);
  }

  @AfterEach
  void destroy() {
    mockedStatic.close();
    mockedClock.close();
  }

  @Test
  void testCreateMas() throws Exception {
    // Given
    var expected = givenMasSingleJsonApiWrapper();
    var mas = givenMas();
    given(handleComponent.postHandle(any())).willReturn(HANDLE);

    // When
    var result = service.createMachineAnnotationService(mas, OBJECT_CREATOR, MAS_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
    then(fdoRecordService).should().buildCreateRequest(mas, ObjectType.MAS);
    then(repository).should().createMachineAnnotationService(givenMasRecord());
    then(kafkaPublisherService).should()
        .publishCreateEvent(HANDLE, MAPPER.valueToTree(givenMasRecord()), SUBJECT_TYPE);
  }

  @Test
  void testCreateMasHandleFails() throws Exception {
    // Given
    var mas = givenMas();
    willThrow(PidCreationException.class).given(handleComponent).postHandle(any());

    // Then
    assertThrowsExactly(ProcessingFailedException.class, () ->
        service.createMachineAnnotationService(mas, OBJECT_CREATOR, MAS_PATH));
  }

  @Test
  void testCreateMasKafkaFails() throws Exception {
    // Given
    var mas = givenMas();
    given(handleComponent.postHandle(any())).willReturn(HANDLE);
    willThrow(JsonProcessingException.class).given(kafkaPublisherService)
        .publishCreateEvent(HANDLE, MAPPER.valueToTree(givenMasRecord()),
            SUBJECT_TYPE);

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.createMachineAnnotationService(mas, OBJECT_CREATOR, MAS_PATH));

    // Then
    then(fdoRecordService).should().buildCreateRequest(mas, ObjectType.MAS);
    then(repository).should().createMachineAnnotationService(givenMasRecord());
    then(fdoRecordService).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());
    then(repository).should().rollbackMasCreation(HANDLE);
  }

  @Test
  void testCreateMasKafkaAndRollbackFails() throws Exception {
    // Given
    var mas = givenMas();
    given(handleComponent.postHandle(any())).willReturn(HANDLE);
    willThrow(JsonProcessingException.class).given(kafkaPublisherService)
        .publishCreateEvent(HANDLE, MAPPER.valueToTree(givenMasRecord()),
            SUBJECT_TYPE);
    willThrow(PidCreationException.class).given(handleComponent).rollbackHandleCreation(any());

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.createMachineAnnotationService(mas, OBJECT_CREATOR, MAS_PATH));

    // Then
    then(fdoRecordService).should().buildCreateRequest(mas, ObjectType.MAS);
    then(repository).should().createMachineAnnotationService(givenMasRecord());
    then(fdoRecordService).should().buildRollbackCreateRequest(HANDLE);
    then(handleComponent).should().rollbackHandleCreation(any());
    then(repository).should().rollbackMasCreation(HANDLE);
  }

  @Test
  void testUpdateMas() throws Exception {
    // Given
    var expected = givenMasSingleJsonApiWrapper(2);
    var prevRecord = buildOptionalPrevRecord();
    var mas = givenMas();
    given(repository.getActiveMachineAnnotationService(HANDLE)).willReturn(prevRecord);

    // When
    var result = service.updateMachineAnnotationService(HANDLE, mas, OBJECT_CREATOR, MAS_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
    then(repository).should().updateMachineAnnotationService(givenMasRecord(2));
    then(kafkaPublisherService).should()
        .publishUpdateEvent(HANDLE, MAPPER.valueToTree(givenMasRecord(2)), givenJsonPatch(), SUBJECT_TYPE);
  }

  @Test
  void testUpdateMasKafkaFails() throws Exception {
    // Given
    var prevRecord = buildOptionalPrevRecord();
    var mas = givenMas();
    given(repository.getActiveMachineAnnotationService(HANDLE)).willReturn(prevRecord);
    willThrow(JsonProcessingException.class).given(kafkaPublisherService)
        .publishUpdateEvent(HANDLE, MAPPER.valueToTree(givenMasRecord(2)), givenJsonPatch(),
            SUBJECT_TYPE);

    // When
    assertThrowsExactly(ProcessingFailedException.class,
        () -> service.updateMachineAnnotationService(HANDLE, mas, OBJECT_CREATOR, MAS_PATH));

    // Then
    then(repository).should().updateMachineAnnotationService(givenMasRecord(2));
    then(repository).should().updateMachineAnnotationService(prevRecord.get());
  }

  private JsonNode givenJsonPatch() throws JsonProcessingException {
    return MAPPER.readTree("[{\"op\":\"replace\",\"path\":\"/sourceCodeRepository\","
        + "\"value\":null},{\"op\":\"replace\",\"path\":\"/supportContact\",\"value\":null},"
        + "{\"op\":\"replace\",\"path\":\"/dependencies\",\"value\":null},{\"op\":\"replace\","
        + "\"path\":\"/slaDocumentation\",\"value\":null},{\"op\":\"replace\","
        + "\"path\":\"/codeLicense\",\"value\":null},{\"op\":\"replace\",\"path\":\"/serviceState\""
        + ",\"value\":null},{\"op\":\"replace\",\"path\":\"/name\",\"value\":\"Another name\"}"
        + ",{\"op\":\"replace\",\"path\":\"/codeMaintainer\",\"value\":null},{\"op\":\"replace\","
        + "\"path\":\"/serviceAvailability\",\"value\":null},{\"op\":\"replace\","
        + "\"path\":\"/serviceDescription\",\"value\":null},{\"op\":\"replace\","
        + "\"path\":\"/containerTag\",\"value\":\"less-fancy\"}]");
  }

  @Test
  void testUpdateMasEqual() throws Exception {
    // Given
    var mas = givenMas();
    given(repository.getActiveMachineAnnotationService(HANDLE)).willReturn(
        Optional.of(givenMasRecord()));

    // When
    var result = service.updateMachineAnnotationService(HANDLE, mas, OBJECT_CREATOR, MAS_PATH);

    // Then
    assertThat(result).isNull();
  }

  @Test
  void testUpdateMasNotFount() {
    // Given
    var mas = givenMas();
    given(repository.getActiveMachineAnnotationService(HANDLE)).willReturn(Optional.empty());

    // When/Then
    assertThrowsExactly(NotFoundException.class,
        () -> service.updateMachineAnnotationService(HANDLE, mas, OBJECT_CREATOR, MAS_PATH));
  }

  @Test
  void testGetMasById() {
    // Given
    var masRecord = givenMasRecord();
    var expected = givenMasSingleJsonApiWrapper();
    given(repository.getMachineAnnotationService(HANDLE)).willReturn(masRecord);

    // When
    var result = service.getMachineAnnotationService(HANDLE, MAS_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testGetMas() {
    // Given
    int pageNum = 1;
    int pageSize = 10;
    var masRecords = Collections.nCopies(pageSize + 1, givenMasRecord());
    given(repository.getMachineAnnotationServices(pageNum, pageSize)).willReturn(masRecords);
    var linksNode = new JsonApiLinks(pageSize, pageNum, true, MAS_PATH);
    var expected = givenMasRecordResponse(masRecords.subList(0, pageSize), linksNode);

    // When
    var result = service.getMachineAnnotationServices(pageNum, pageSize, MAS_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testGetMasSecondPage() {
    // Given
    int pageNum = 2;
    int pageSize = 10;
    var masRecords = Collections.nCopies(pageSize, givenMasRecord());
    given(repository.getMachineAnnotationServices(pageNum, pageSize)).willReturn(masRecords);
    var linksNode = new JsonApiLinks(pageSize, pageNum, false, MAS_PATH);
    var expected = givenMasRecordResponse(masRecords.subList(0, pageSize), linksNode);

    // When
    var result = service.getMachineAnnotationServices(pageNum, pageSize, MAS_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testDeletedMas() {
    // Given
    given(repository.getActiveMachineAnnotationService(HANDLE)).willReturn(
        Optional.of(givenMasRecord()));

    // When / Then
    assertDoesNotThrow(() -> service.deleteMachineAnnotationService(HANDLE));
  }

  @Test
  void testDeletedMasNotFound() {
    // Given
    given(repository.getActiveMachineAnnotationService(HANDLE)).willReturn(Optional.empty());

    // Then
    assertThrowsExactly(NotFoundException.class,
        () -> service.deleteMachineAnnotationService(HANDLE));
  }

  private Optional<MachineAnnotationServiceRecord> buildOptionalPrevRecord() {
    return Optional.of(new MachineAnnotationServiceRecord(
        HANDLE,
        1,
        CREATED,
        OBJECT_CREATOR,
        new MachineAnnotationService("Another name", "public.ecr.aws/dissco/fancy-mas",
            "less-fancy", MAPPER.createObjectNode(), null, null, null, null, null, null, null, null,
            null),
        null
    ));
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
