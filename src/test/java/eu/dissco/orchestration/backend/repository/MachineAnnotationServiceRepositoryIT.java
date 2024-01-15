package eu.dissco.orchestration.backend.repository;

import static eu.dissco.orchestration.backend.database.jooq.Tables.MACHINE_ANNOTATION_SERVICES;
import static eu.dissco.orchestration.backend.testutils.TestUtils.CREATED;
import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.OBJECT_CREATOR;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMas;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMasInput;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMasRecord;
import static org.assertj.core.api.Assertions.assertThat;

import eu.dissco.orchestration.backend.domain.MachineAnnotationService;
import eu.dissco.orchestration.backend.domain.MachineAnnotationServiceRecord;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MachineAnnotationServiceRepositoryIT extends BaseRepositoryIT {

  private MachineAnnotationServiceRepository repository;

  @BeforeEach
  void setup() {
    repository = new MachineAnnotationServiceRepository(context, MAPPER);
  }

  @AfterEach
  void destroy() {
    context.truncate(MACHINE_ANNOTATION_SERVICES).execute();
  }

  @Test
  void testCreateMas() {
    // Given
    var masRecord = givenMasRecord();

    // When
    repository.createMachineAnnotationService(masRecord);
    var result = repository.getMachineAnnotationServices(1, 10);

    // Then
    assertThat(result).containsOnly(masRecord);
  }

  @Test
  void testCreateMasNoMasInput() {
    // Given
    var masRecord = givenMasRecord(1, null);

    // When
    repository.createMachineAnnotationService(masRecord);
    var result = repository.getMachineAnnotationServices(1, 10);

    // Then
    assertThat(result).containsOnly(masRecord);
  }


  @Test
  void testUpdateMas() {
    // Given
    var originalRecord = givenMasRecord();
    postMass(List.of(originalRecord));
    var updatedRecord = givenUpdatedRecord();

    // When
    repository.updateMachineAnnotationService(updatedRecord);
    var result = repository.getMachineAnnotationServices(1, 10);

    // Then
    assertThat(result).containsOnly(updatedRecord);
  }

  @Test
  void testGetMasById() {
    // Given
    var expected = givenMasRecord();
    postMass(List.of(expected));

    // When
    var result = repository.getMachineAnnotationService(HANDLE);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testGetMasByIdNoMasInput() {
    // Given
    var expected = givenMasRecord(1, null);
    postMass(List.of(expected));

    // When
    var result = repository.getMachineAnnotationService(HANDLE);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testGetMass() {
    // Given
    int pageNum = 1;
    int pageSize = 10;
    var masRecords = IntStream.range(0, pageSize).boxed().map(this::givenMasRecordWithId).toList();
    postMass(masRecords);

    // When
    var result = repository.getMachineAnnotationServices(pageNum, pageSize);

    // Then
    assertThat(result).isEqualTo(masRecords);
  }

  @Test
  void testGetMassSecondPage() {
    // Given
    int pageNum = 2;
    int pageSize = 10;
    var masRecords = IntStream.range(0, pageSize + 1).boxed().map(this::givenMasRecordWithId)
        .toList();
    postMass(masRecords);

    // When
    var result = repository.getMachineAnnotationServices(pageNum, pageSize);

    // Then
    assertThat(result).hasSize(1);
  }

  @Test
  void testDeleteMas() {
    // Given
    var masRecord = givenMasRecord();
    postMass(List.of(masRecord));

    // When
    repository.deleteMachineAnnotationService(HANDLE, CREATED);
    var result = repository.getMachineAnnotationService(HANDLE);

    // Then
    assertThat(result.deleted()).isEqualTo(CREATED);
  }

  private MachineAnnotationServiceRecord givenMasRecordWithId(Integer i) {
    return new MachineAnnotationServiceRecord(String.valueOf(i), 1, CREATED, OBJECT_CREATOR,
        givenMas(), null);
  }


  @Test
  void testGetActiveMas() {
    // Given
    var expected = givenMasRecord();
    postMass(List.of(expected));

    // When
    var result = repository.getActiveMachineAnnotationService(HANDLE);

    // Then
    assertThat(result).hasValue(expected);
  }

  @Test
  void testGetActiveMasWasDeleted() {
    // Given
    var expected = givenMasRecord();
    postMass(List.of(expected));
    repository.deleteMachineAnnotationService(HANDLE, Instant.now());

    // When
    var result = repository.getActiveMachineAnnotationService(HANDLE);

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void testRollback() {
    // Given
    var expected = givenMasRecord();
    postMass(List.of(expected));

    // When
    repository.rollbackMasCreation(HANDLE);

    // Then
    var result = repository.getMachineAnnotationService(HANDLE);
    assertThat(result).isNull();
  }

  private MachineAnnotationServiceRecord givenUpdatedRecord() {
    return new MachineAnnotationServiceRecord(
        HANDLE,
        2,
        CREATED,
        OBJECT_CREATOR,
        new MachineAnnotationService(
            "A Improved Machine Annotation Service",
            "public.ecr.aws/dissco/fancy-mas",
            "sha-542asaw",
            null,
            "An even beter service",
            "Definitely production ready-ish",
            "https://github.com/DiSSCo/fancy-mas",
            "public",
            "Died last year",
            "https://www.apache.org/licenses/LICENSE-2.0",
            null,
            "dontmail@dissco.eu",
            "https://www.know.dissco.tech/no_sla",
            "fancy-topic-name",
            2,
            givenMasInput()
        ),
        null
    );
  }

  private void postMass(List<MachineAnnotationServiceRecord> originalRecord) {
    originalRecord.forEach(masRecord -> repository.createMachineAnnotationService(masRecord));
  }

}
