package eu.dissco.orchestration.backend.repository;

import static eu.dissco.orchestration.backend.database.jooq.Tables.MAPPING;
import static eu.dissco.orchestration.backend.database.jooq.Tables.SOURCE_SYSTEM;
import static eu.dissco.orchestration.backend.testutils.TestUtils.CREATED;
import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.OBJECT_CREATOR;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMapping;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMappingRecord;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.dissco.orchestration.backend.domain.Mapping;
import eu.dissco.orchestration.backend.domain.MappingRecord;
import eu.dissco.orchestration.backend.domain.SourceDataStandard;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.jooq.JSONB;
import org.jooq.Query;
import org.jooq.Record;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MappingRepositoryIT extends BaseRepositoryIT {

  private MappingRepository repository;

  @BeforeEach
  void setup() {
    repository = new MappingRepository(MAPPER, context);
  }

  @AfterEach
  void destroy() {
    context.truncate(MAPPING).execute();
  }

  @Test
  void testCreateMapping() {
    // Given
    var mappingRecord = givenMappingRecord(HANDLE, 1);

    // When
    repository.createMapping(mappingRecord);
    var result = readAllMappings();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(mappingRecord);
  }

  @Test
  void testUpdateMapping() {
    // Given
    var mappingRecord = givenMappingRecord(HANDLE, 1);
    postMappingRecords(List.of(mappingRecord));
    var updatedRecord = givenUpdatedRecord();

    // When
    repository.updateMapping(updatedRecord);
    var result = readAllMappings();

    // Then
    assertThat(result).containsExactly(updatedRecord);
  }

  private MappingRecord givenUpdatedRecord() {
    return new MappingRecord(
        HANDLE,
        2,
        CREATED,
        null,
        OBJECT_CREATOR,
        new Mapping(
            "An updated name",
            "With a nice new description",
            MAPPER.createObjectNode(),
            SourceDataStandard.ABCD
        )
    );
  }

  @Test
  void testGetMapping() {
    // Given
    var mappingRecord = givenMappingRecord(HANDLE, 1);
    postMappingRecords(List.of(mappingRecord));

    // When
    var result = repository.getMapping(HANDLE);

    // Then
    assertThat(result).isEqualTo(mappingRecord);
  }

  @Test
  void testGetMappingIsDeleted() {
    // Given
    var mappingRecord = new MappingRecord(HANDLE, 1, CREATED, CREATED, OBJECT_CREATOR,
        givenMapping());
    postMappingRecords(List.of(mappingRecord));

    var result = repository.getMapping(HANDLE);

    // Then
    assertThat(result).isEqualTo(mappingRecord);
  }

  @Test
  void testGetActiveMappingIsPresent() {
    // Given
    var mappingRecord = givenMappingRecord(HANDLE, 1);
    postMappingRecords(List.of(mappingRecord));

    // When
    var result = repository.getActiveMapping(HANDLE);

    // Then
    assertThat(result).contains(mappingRecord);
  }

  @Test
  void testGetActiveMappingNotPresent() {
    // Given
    var mappingRecord = givenMappingRecord(HANDLE, 1);
    postMappingRecords(List.of(mappingRecord));
    context.update(MAPPING)
        .set(MAPPING.DELETED, CREATED)
        .where(MAPPING.ID.eq(HANDLE))
        .execute();

    // When
    var result = repository.getActiveMapping(HANDLE);

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void testGetMappings() {
    // Given
    int pageNum = 1;
    int pageSize = 10;
    List<MappingRecord> mappingRecords = IntStream.range(0, pageSize).boxed()
        .map(i -> givenMappingRecord(String.valueOf(i), 1)).toList();
    postMappingRecords(mappingRecords);

    // When
    var result = repository.getMappings(pageNum, pageSize);

    //Then
    assertThat(result).isEqualTo(mappingRecords);
  }

  @Test
  void testGetMappingsLastPage() {
    // Given
    int pageNum = 2;
    int pageSize = 10;
    List<MappingRecord> mappingRecords = IntStream.range(0, pageSize + 1).boxed()
        .map(i -> givenMappingRecord(String.valueOf(i), 1)).toList();
    postMappingRecords(mappingRecords);

    // When
    var result = repository.getMappings(pageNum, pageSize);

    //Then
    assertThat(result).hasSize(1);
  }

  @Test
  void testDeleteMapping() {
    // Given
    MappingRecord mapping = givenMappingRecord(HANDLE, 1);
    postMappingRecords(List.of(mapping));

    // When
    repository.deleteMapping(HANDLE, CREATED);
    var result = getDeleted(HANDLE);

    // Then
    assertThat(result).isEqualTo(CREATED);
  }

  private Instant getDeleted(String id) {
    return context.select(MAPPING.ID, MAPPING.DELETED)
        .from(MAPPING)
        .where(MAPPING.ID.eq(id))
        .fetchOne(this::getInstantDeleted);
  }

  private Instant getInstantDeleted(Record dbRecord) {
    return dbRecord.get(SOURCE_SYSTEM.DELETED);
  }

  List<MappingRecord> readAllMappings() {
    return context.select(MAPPING.asterisk())
        .from(MAPPING)
        .fetch(this::mapToMappingRecord);
  }

  private MappingRecord mapToMappingRecord(Record dbRecord) {
    try {
      var mapping = new Mapping(
          dbRecord.get(MAPPING.NAME),
          dbRecord.get(MAPPING.DESCRIPTION),
          MAPPER.readTree(dbRecord.get(MAPPING.MAPPING_).data()),
          SourceDataStandard.fromString(dbRecord.get(MAPPING.SOURCEDATASTANDARD)));
      return new MappingRecord(
          dbRecord.get(MAPPING.ID),
          dbRecord.get(MAPPING.VERSION),
          dbRecord.get(MAPPING.CREATED),
          null, dbRecord.get(MAPPING.CREATOR),
          mapping
      );
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }


  private void postMappingRecords(List<MappingRecord> mappingRecords) {
    List<Query> queryList = new ArrayList<>();
    for (var mappingRecord : mappingRecords) {
      queryList.add(context.insertInto(MAPPING)
          .set(MAPPING.ID, mappingRecord.id())
          .set(MAPPING.VERSION, mappingRecord.version())
          .set(MAPPING.NAME, mappingRecord.mapping().name())
          .set(MAPPING.DESCRIPTION, mappingRecord.mapping().description())
          .set(MAPPING.SOURCEDATASTANDARD, mappingRecord.mapping().sourceDataStandard().toString())
          .set(MAPPING.MAPPING_,
              JSONB.valueOf(mappingRecord.mapping().mapping().toString()))
          .set(MAPPING.CREATED, mappingRecord.created())
          .set(MAPPING.DELETED, mappingRecord.deleted())
          .set(MAPPING.CREATOR, mappingRecord.creator()));
    }
    context.batch(queryList).execute();
  }

  @Test
  void testRollback() {
    // Given
    postMappingRecords(List.of(givenMappingRecord(HANDLE, 1)));

    // When
    repository.rollbackMappingCreation(HANDLE);

    // Then
    var result = repository.getMapping(HANDLE);
    assertThat(result).isNull();
  }

}
