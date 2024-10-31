package eu.dissco.orchestration.backend.repository;

import static eu.dissco.orchestration.backend.database.jooq.Tables.MACHINE_ANNOTATION_SERVICE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.BARE_HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.CREATED;
import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAS_NAME;
import static eu.dissco.orchestration.backend.testutils.TestUtils.TTL;
import static eu.dissco.orchestration.backend.testutils.TestUtils.UPDATED;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMas;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenTombstoneMetadata;
import static org.assertj.core.api.Assertions.assertThat;

import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.schema.MachineAnnotationService;
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
    context.truncate(MACHINE_ANNOTATION_SERVICE).execute();
  }

  @Test
  void testCreateMas() {
    // Given
    var mas = givenMas();

    // When
    repository.createMachineAnnotationService(mas);
    var result = repository.getMachineAnnotationServices(1, 10);

    // Then
    assertThat(result).containsOnly(mas);
  }

  @Test
  void testCreateMasNoMasInput() {
    // Given
    var mas = givenMas(1);

    // When
    repository.createMachineAnnotationService(mas);
    var result = repository.getMachineAnnotationServices(1, 10);

    // Then
    assertThat(result).containsOnly(mas);
  }


  @Test
  void testUpdateMas() {
    // Given
    var originalMas = givenMas();
    postMass(List.of(originalMas));
    var updatedMas = givenMas(2, "Another name", TTL);

    // When
    repository.updateMachineAnnotationService(updatedMas);
    var result = repository.getMachineAnnotationServices(1, 10);

    // Then
    assertThat(result).containsOnly(updatedMas);
  }

  @Test
  void testGetMasById() {
    // Given
    var expected = givenMas();
    postMass(List.of(expected));

    // When
    var result = repository.getMachineAnnotationService(HANDLE);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testGetMasByIdNoMasInput() {
    // Given
    var expected = givenMas(1);
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
    var machineAnnotationServices = IntStream.range(0, pageSize).boxed()
        .map(this::givenMasWithId).toList();
    postMass(machineAnnotationServices);

    // When
    var result = repository.getMachineAnnotationServices(pageNum, pageSize);

    // Then
    assertThat(result).isEqualTo(machineAnnotationServices);
  }

  @Test
  void testGetMassSecondPage() {
    // Given
    int pageNum = 2;
    int pageSize = 10;
    var machineAnnotationServices = IntStream.range(0, pageSize + 1).boxed()
        .map(this::givenMasWithId)
        .toList();
    postMass(machineAnnotationServices);

    // When
    var result = repository.getMachineAnnotationServices(pageNum, pageSize);

    // Then
    assertThat(result).hasSize(1);
  }

  @Test
  void testDeleteMas() {
    // Given
    var mas = givenMas();
    mas.setOdsTombstoneMetadata(givenTombstoneMetadata(ObjectType.MAS));
    postMass(List.of(mas));

    // When
    repository.tombstoneMachineAnnotationService(mas, UPDATED);
    var result = context.select(MACHINE_ANNOTATION_SERVICE.TOMBSTONED)
        .from(MACHINE_ANNOTATION_SERVICE)
        .where(MACHINE_ANNOTATION_SERVICE.ID.eq(BARE_HANDLE))
        .fetchOne(MACHINE_ANNOTATION_SERVICE.TOMBSTONED, Instant.class);

    // Then
    assertThat(result).isEqualTo(UPDATED);
  }

  private MachineAnnotationService givenMasWithId(Integer i) {
    return givenMas(String.valueOf(i), 1, MAS_NAME, TTL);
  }


  @Test
  void testGetActiveMas() {
    // Given
    var expected = givenMas();
    postMass(List.of(expected));

    // When
    var result = repository.getActiveMachineAnnotationService(HANDLE);

    // Then
    assertThat(result).hasValue(expected);
  }

  @Test
  void testGetActiveMasWasDeleted() {
    // Given
    var expected = givenMas();
    postMass(List.of(expected));
    repository.tombstoneMachineAnnotationService(expected, CREATED);

    // When
    var result = repository.getActiveMachineAnnotationService(HANDLE);

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void testRollback() {
    // Given
    var expected = givenMas();
    postMass(List.of(expected));

    // When
    repository.rollbackMasCreation(HANDLE);

    // Then
    var result = repository.getMachineAnnotationService(HANDLE);
    assertThat(result).isNull();
  }

  @Test
  void testCreateMasNullTTL() {
    // Given
    var mas = givenMas(1, MAS_NAME, null);

    // When
    repository.createMachineAnnotationService(mas);
    var result = repository.getMachineAnnotationServices(1, 10);

    // Then
    assertThat(result.get(0).getOdsTimeToLive()).isEqualTo(TTL);
  }

  @Test
  void testCreateMasNullSchemaMaintainer() {
    // Given
    var mas = givenMas()
        .withSchemaMaintainer(null);

    // When
    repository.createMachineAnnotationService(mas);
    var result = repository.getMachineAnnotationServices(1, 10);

    // Then
    assertThat(result.get(0).getSchemaMaintainer()).isNull();
  }


  private void postMass(List<MachineAnnotationService> originalMas) {
    originalMas.forEach(mas -> repository.createMachineAnnotationService(mas));
  }

}
