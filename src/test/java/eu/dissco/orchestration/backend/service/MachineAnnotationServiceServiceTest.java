package eu.dissco.orchestration.backend.service;

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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;

import eu.dissco.orchestration.backend.domain.HandleType;
import eu.dissco.orchestration.backend.domain.MachineAnnotationService;
import eu.dissco.orchestration.backend.domain.MachineAnnotationServiceRecord;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.repository.MachineAnnotationServiceRepository;
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
  private HandleService handleService;
  @Mock
  private MachineAnnotationServiceRepository repository;

  private MachineAnnotationServiceService service;

  private MockedStatic<Instant> mockedStatic;

  @BeforeEach
  void setup() {
    initTime();
    service = new MachineAnnotationServiceService(handleService, repository, MAPPER);
  }

  @AfterEach
  void destroy() {
    mockedStatic.close();
  }

  @Test
  void testCreateMas() throws Exception {
    // Given
    var expected = givenMasSingleJsonApiWrapper();
    var mas = givenMas();
    given(handleService.createNewHandle(HandleType.MACHINE_ANNOTATION_SERVICE)).willReturn(HANDLE);

    // When
    var result = service.createMachineAnnotationService(mas, OBJECT_CREATOR, MAS_PATH);

    // Then
    assertThat(result).isEqualTo(expected);
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
  }

  @Test
  void testUpdateMasEqual() throws Exception {
    // Given
    var expected = givenMasSingleJsonApiWrapper(2);
    var mas = givenMas();
    given(repository.getActiveMachineAnnotationService(HANDLE)).willReturn(Optional.of(givenMasRecord()));

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
  void testGetMass() {
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
  void testGetMassSecondPage() {
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
    Instant instant = Instant.now(clock);
    mockedStatic = mockStatic(Instant.class);
    mockedStatic.when(Instant::now).thenReturn(instant);
    mockedStatic.when(() -> Instant.from(any())).thenReturn(instant);
  }

}
