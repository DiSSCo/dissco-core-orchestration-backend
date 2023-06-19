package eu.dissco.orchestration.backend.repository;

import static eu.dissco.orchestration.backend.database.jooq.Tables.NEW_MAPPING;
import static eu.dissco.orchestration.backend.database.jooq.Tables.NEW_SOURCE_SYSTEM;
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
    context.truncate(NEW_MAPPING).execute();
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
  void testGetMapping() throws Exception {
    // Given
    var mappingRecord = givenMappingRecord(HANDLE, 1);
    postMappingRecords(List.of(mappingRecord));

    // When
    var result = repository.getMapping(HANDLE);

    // Then
    assertThat(result).isEqualTo(mappingRecord);
  }

  @Test
  void testGetMappingIsDeleted() throws Exception {
    // Given
    var mappingRecord = new MappingRecord(HANDLE, 1, CREATED, CREATED, OBJECT_CREATOR,
        givenMapping());
    postMappingRecords(List.of(mappingRecord));

    var result = repository.getMapping(HANDLE);

    // Then
    assertThat(result).isEqualTo(mappingRecord);
  }

  @Test
  void testGetActiveMappingIsPresent() throws Exception {
    // Given
    var mappingRecord = givenMappingRecord(HANDLE, 1);
    postMappingRecords(List.of(mappingRecord));

    // When
    var result = repository.getActiveMapping(HANDLE);

    // Then
    assertThat(result).contains(mappingRecord);
  }

  @Test
  void testGetActiveMappingNotPresent() throws Exception {
    // Given
    var mappingRecord = givenMappingRecord(HANDLE, 1);
    postMappingRecords(List.of(mappingRecord));
    context.update(NEW_MAPPING)
        .set(NEW_MAPPING.DELETED, CREATED)
        .where(NEW_MAPPING.ID.eq(HANDLE))
        .execute();

    // When
    var result = repository.getActiveMapping(HANDLE);

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void testGetMappings() throws Exception {
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
  void testGetMappingsLastPage() throws Exception {
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
  void testDeleteMapping() throws Exception {
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
    return context.select(NEW_MAPPING.ID, NEW_MAPPING.DELETED)
        .from(NEW_MAPPING)
        .where(NEW_MAPPING.ID.eq(id))
        .fetchOne(this::getInstantDeleted);
  }

  private Instant getInstantDeleted(Record dbRecord) {
    return dbRecord.get(NEW_SOURCE_SYSTEM.DELETED);
  }

  List<MappingRecord> readAllMappings() {
    return context.select(NEW_MAPPING.asterisk())
        .from(NEW_MAPPING)
        .fetch(this::mapToMappingRecord);
  }

  private MappingRecord mapToMappingRecord(Record dbRecord) {
    try {
      var mapping = new Mapping(
          dbRecord.get(NEW_MAPPING.NAME),
          dbRecord.get(NEW_MAPPING.DESCRIPTION),
          MAPPER.readTree(dbRecord.get(NEW_MAPPING.MAPPING).data()),
          "dwc");
      return new MappingRecord(
          dbRecord.get(NEW_MAPPING.ID),
          dbRecord.get(NEW_MAPPING.VERSION),
          dbRecord.get(NEW_MAPPING.CREATED),
          null, dbRecord.get(NEW_MAPPING.CREATOR),
          mapping
      );
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }


  private void postMappingRecords(List<MappingRecord> mappingRecords)
      throws Exception {
    List<Query> queryList = new ArrayList<>();
    for (var mappingRecord : mappingRecords) {
      queryList.add(context.insertInto(NEW_MAPPING)
          .set(NEW_MAPPING.ID, mappingRecord.id())
          .set(NEW_MAPPING.VERSION, mappingRecord.version())
          .set(NEW_MAPPING.NAME, mappingRecord.mapping().name())
          .set(NEW_MAPPING.DESCRIPTION, mappingRecord.mapping().description())
          .set(NEW_MAPPING.SOURCEDATASTANDARD, mappingRecord.mapping().sourceDataStandard())
          .set(NEW_MAPPING.MAPPING,
              JSONB.valueOf(MAPPER.writeValueAsString(mappingRecord.mapping().mapping())))
          .set(NEW_MAPPING.CREATED, mappingRecord.created())
          .set(NEW_MAPPING.DELETED, mappingRecord.deleted())
          .set(NEW_MAPPING.CREATOR, mappingRecord.creator()));
    }
    context.batch(queryList).execute();
  }


}
