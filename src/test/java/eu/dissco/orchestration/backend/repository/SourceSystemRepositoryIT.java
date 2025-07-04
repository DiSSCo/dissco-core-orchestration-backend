package eu.dissco.orchestration.backend.repository;

import static eu.dissco.orchestration.backend.configuration.ApplicationConfiguration.HANDLE_PROXY;
import static eu.dissco.orchestration.backend.database.jooq.Tables.SOURCE_SYSTEM;
import static eu.dissco.orchestration.backend.testutils.TestUtils.BARE_HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.CREATED;
import static eu.dissco.orchestration.backend.testutils.TestUtils.DWC_DP_S3_URI;
import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystem;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenTombstoneMetadata;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.dissco.orchestration.backend.database.jooq.enums.TranslatorType;
import eu.dissco.orchestration.backend.domain.ExportType;
import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.exception.DisscoJsonBMappingException;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.schema.SourceSystem;
import eu.dissco.orchestration.backend.schema.SourceSystem.OdsStatus;
import eu.dissco.orchestration.backend.schema.SourceSystem.OdsTranslatorType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.jooq.JSONB;
import org.jooq.Query;
import org.jooq.Record1;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SourceSystemRepositoryIT extends BaseRepositoryIT {

  private SourceSystemRepository repository;

  private static String removeProxy(String id) {
    return id.replace(HANDLE_PROXY, "");
  }

  @BeforeEach
  void setup() {
    repository = new SourceSystemRepository(context, MAPPER);
  }

  @AfterEach
  void destroy() {
    context.truncate(SOURCE_SYSTEM).execute();
  }

  @Test
  void testCreateSourceSystem() {
    // Given
    var sourceSystem = givenSourceSystem();

    // When
    repository.createSourceSystem(sourceSystem);
    var result = getAllSourceSystems();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(sourceSystem);
  }

  @Test
  void testUpdateSourceSystem() throws JsonProcessingException {
    // Given
    var orginalSourceSystem = givenSourceSystem();
    postSourceSystem(List.of(orginalSourceSystem));
    var updatedSourceSystem = orginalSourceSystem.withSchemaName("An updated name");

    // When
    repository.updateSourceSystem(updatedSourceSystem);
    var result = getAllSourceSystems();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(updatedSourceSystem);
  }

  @Test
  void testGetSourceSystemById() throws JsonProcessingException {
    // Given
    var expected = givenSourceSystem();
    postSourceSystem(List.of(expected));

    // When
    var result = repository.getSourceSystem(HANDLE);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testGetDownloadLink() throws JsonProcessingException {
    // Given
    var exportType = ExportType.DWC_DP;
    postSourceSystem(List.of(givenSourceSystem()));

    // When
    var result = repository.getExportLink(BARE_HANDLE, exportType);

    // Then
    assertThat(result).isEqualTo(DWC_DP_S3_URI);
  }

  @Test
  void testGetSourceSystems() throws JsonProcessingException {
    // Given
    int pageNum = 1;
    int pageSize = 10;
    List<SourceSystem> sourceSystems = IntStream.range(0, pageSize).boxed()
        .map(this::givenSourceSystemWithId).toList();
    postSourceSystem(sourceSystems);

    // When
    var result = repository.getSourceSystems(pageNum, pageSize);

    assertThat(result).isEqualTo(sourceSystems);
  }

  @Test
  void testGetSourceSystemsSecondPage() throws JsonProcessingException {
    // Given
    int pageNum = 2;
    int pageSize = 10;

    List<SourceSystem> sourceSystems = IntStream.range(0, pageSize + 1).boxed()
        .map(this::givenSourceSystemWithId).toList();
    postSourceSystem(sourceSystems);

    // When
    var result = repository.getSourceSystems(pageNum, pageSize);

    assertThat(result).hasSize(1);
  }

  @Test
  void testTombstoneSourceSystem() throws JsonProcessingException {
    // Given
    var sourceSystem = givenSourceSystem();
    sourceSystem.withOdsStatus(OdsStatus.TOMBSTONE);
    sourceSystem.withOdsHasTombstoneMetadata(givenTombstoneMetadata(ObjectType.SOURCE_SYSTEM));
    postSourceSystem(List.of(sourceSystem));

    // When
    repository.tombstoneSourceSystem(sourceSystem, CREATED);
    var result = getDeleted(BARE_HANDLE);

    // Then
    assertThat(result).isEqualTo(sourceSystem);
  }

  @Test
  void testGetActiveSourceSystem() throws JsonProcessingException {
    // Given
    var sourceSystem = givenSourceSystem();
    postSourceSystem(List.of(sourceSystem));

    // When
    var result = repository.getActiveSourceSystem(sourceSystem.getId());

    // Then
    assertThat(result).contains(sourceSystem);
  }

  @Test
  void testGetActiveSourceSystemWasDeleted() throws JsonProcessingException {
    var sourceSystem = givenSourceSystem();
    postSourceSystem(List.of(sourceSystem));
    context.update(SOURCE_SYSTEM)
        .set(SOURCE_SYSTEM.TOMBSTONED, CREATED)
        .where(SOURCE_SYSTEM.ID.eq(BARE_HANDLE))
        .execute();

    // When
    var result = repository.getActiveSourceSystem(HANDLE);

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void testRollback() throws JsonProcessingException {
    // Given
    var sourceSystem = givenSourceSystem();
    postSourceSystem(List.of(sourceSystem));

    // When
    repository.rollbackSourceSystemCreation(HANDLE);

    // Then
    var result = repository.getSourceSystem(HANDLE);
    assertThat(result).isNull();
  }

  private SourceSystem givenSourceSystemWithId(int id) {
    return givenSourceSystem(String.valueOf(id), 1, OdsTranslatorType.DWCA);
  }

  private List<SourceSystem> getAllSourceSystems() {
    return context.select(SOURCE_SYSTEM.DATA)
        .from(SOURCE_SYSTEM)
        .fetch(this::mapToSourceSystem);
  }

  private SourceSystem getDeleted(String id) {
    return context.select(SOURCE_SYSTEM.DATA)
        .from(SOURCE_SYSTEM)
        .where(SOURCE_SYSTEM.ID.eq(id))
        .fetchOne(this::mapToSourceSystem);
  }

  private SourceSystem mapToSourceSystem(Record1<JSONB> record1) {
    try {
      return MAPPER.readValue(record1.get(SOURCE_SYSTEM.DATA).data(), SourceSystem.class);
    } catch (JsonProcessingException e) {
      throw new DisscoJsonBMappingException("Unable to convert jsonb to source system", e);
    }
  }

  private void postSourceSystem(List<SourceSystem> sourceSystems) throws JsonProcessingException {
    List<Query> queryList = new ArrayList<>();
    for (var sourceSystem : sourceSystems) {
      queryList.add(context.insertInto(SOURCE_SYSTEM)
          .set(SOURCE_SYSTEM.ID, removeProxy(sourceSystem.getId()))
          .set(SOURCE_SYSTEM.VERSION, sourceSystem.getSchemaVersion())
          .set(SOURCE_SYSTEM.NAME, sourceSystem.getSchemaName())
          .set(SOURCE_SYSTEM.ENDPOINT, sourceSystem.getSchemaUrl().toString())
          .set(SOURCE_SYSTEM.CREATOR, sourceSystem.getSchemaCreator().getId())
          .set(SOURCE_SYSTEM.CREATED, sourceSystem.getSchemaDateCreated().toInstant())
          .set(SOURCE_SYSTEM.MODIFIED, sourceSystem.getSchemaDateModified().toInstant())
          .set(SOURCE_SYSTEM.MAPPING_ID, sourceSystem.getOdsDataMappingID())
          .set(SOURCE_SYSTEM.TRANSLATOR_TYPE, TranslatorType.valueOf(
              sourceSystem.getOdsTranslatorType().value()))
          .set(SOURCE_SYSTEM.DWC_DP_LINK, DWC_DP_S3_URI)
          .set(SOURCE_SYSTEM.DATA, mapToJSONB(sourceSystem)));
    }
    context.batch(queryList).execute();
  }

  private JSONB mapToJSONB(SourceSystem sourceSystem) throws JsonProcessingException {
    return JSONB.valueOf(MAPPER.writeValueAsString(sourceSystem));
  }

}
