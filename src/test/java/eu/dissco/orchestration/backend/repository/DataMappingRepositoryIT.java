package eu.dissco.orchestration.backend.repository;

import static eu.dissco.orchestration.backend.database.jooq.Tables.DATA_MAPPING;
import static eu.dissco.orchestration.backend.testutils.TestUtils.BARE_HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.CREATED;
import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenDataMapping;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenTombstoneMetadata;
import static eu.dissco.orchestration.backend.utils.HandleUtils.removeProxy;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.exception.DisscoJsonBMappingException;
import eu.dissco.orchestration.backend.schema.DataMapping;
import eu.dissco.orchestration.backend.schema.DataMapping.OdsStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.jooq.JSONB;
import org.jooq.Query;
import org.jooq.Record1;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DataMappingRepositoryIT extends BaseRepositoryIT {

  private DataMappingRepository repository;

  @BeforeEach
  void setup() {
    repository = new DataMappingRepository(MAPPER, context);
  }

  @AfterEach
  void destroy() {
    context.truncate(DATA_MAPPING).execute();
  }

  @Test
  void testCreateDataMapping() {
    // Given
    var dataMapping = givenDataMapping(HANDLE, 1);

    // When
    repository.createDataMapping(dataMapping);
    var result = readAllDataMappings();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(dataMapping);
  }

  @Test
  void testUpdateDataMapping() throws JsonProcessingException {
    // Given
    var dataMapping = givenDataMapping(HANDLE, 1);
    postDataMappings(List.of(dataMapping));
    var updatedDataMapping = givenDataMapping(HANDLE, 2, "Updated name");

    // When
    repository.updateDataMapping(updatedDataMapping);
    var result = readAllDataMappings();

    // Then
    assertThat(result).containsExactly(updatedDataMapping);
  }

  @Test
  void testGetDataMapping() throws JsonProcessingException {
    // Given
    var dataMapping = givenDataMapping(HANDLE, 1);
    postDataMappings(List.of(dataMapping));

    // When
    var result = repository.getDataMapping(HANDLE);

    // Then
    assertThat(result).isEqualTo(dataMapping);
  }

  @Test
  void testGetDataMappingIsDeleted() throws JsonProcessingException {
    // Given
    var dataMapping = givenDataMapping();
    postDataMappings(List.of(dataMapping));

    var result = repository.getDataMapping(HANDLE);

    // Then
    assertThat(result).isEqualTo(dataMapping);
  }

  @Test
  void testGetActiveDataMappingIsPresent() throws JsonProcessingException {
    // Given
    var dataMapping = givenDataMapping(HANDLE, 1);
    postDataMappings(List.of(dataMapping));

    // When
    var result = repository.getActiveDataMapping(HANDLE);

    // Then
    assertThat(result).contains(dataMapping);
  }

  @Test
  void testGetActiveDataMappingNotPresent() throws JsonProcessingException {
    // Given
    var dataMapping = givenDataMapping(HANDLE, 1);
    postDataMappings(List.of(dataMapping));
    context.update(DATA_MAPPING)
        .set(DATA_MAPPING.TOMBSTONED, CREATED)
        .where(DATA_MAPPING.ID.eq(BARE_HANDLE))
        .execute();

    // When
    var result = repository.getActiveDataMapping(HANDLE);

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void testGetDataMappings() throws JsonProcessingException {
    // Given
    int pageNum = 1;
    int pageSize = 10;
    List<DataMapping> mappingRecords = IntStream.range(0, pageSize).boxed()
        .map(i -> givenDataMapping(String.valueOf(i), 1)).toList();
    postDataMappings(mappingRecords);

    // When
    var result = repository.getDataMappings(pageNum, pageSize);

    //Then
    assertThat(result).isEqualTo(mappingRecords);
  }

  @Test
  void testGetDataMappingsLastPage() throws JsonProcessingException {
    // Given
    int pageNum = 2;
    int pageSize = 10;
    List<DataMapping> mappingRecords = IntStream.range(0, pageSize + 1).boxed()
        .map(i -> givenDataMapping(String.valueOf(i), 1)).toList();
    postDataMappings(mappingRecords);

    // When
    var result = repository.getDataMappings(pageNum, pageSize);

    //Then
    assertThat(result).hasSize(1);
  }

  @Test
  void testTombstoneDataMapping() throws JsonProcessingException {
    // Given
    var dataMapping = givenDataMapping(HANDLE, 1);
    dataMapping.setOdsStatus(OdsStatus.TOMBSTONE);
    dataMapping.setOdsHasTombstoneMetadata(givenTombstoneMetadata(ObjectType.DATA_MAPPING));
    postDataMappings(List.of(dataMapping));

    // When
    repository.tombstoneDataMapping(dataMapping, CREATED);
    var result = getDeleted(BARE_HANDLE);

    // Then
    assertThat(result).isEqualTo(dataMapping);
  }

  private DataMapping getDeleted(String id) {
    return context.select(DATA_MAPPING.DATA)
        .from(DATA_MAPPING)
        .where(DATA_MAPPING.ID.eq(id))
        .fetchOne(this::mapToDataMapping);
  }

  List<DataMapping> readAllDataMappings() {
    return context.select(DATA_MAPPING.DATA)
        .from(DATA_MAPPING)
        .fetch(this::mapToDataMapping);
  }

  private DataMapping mapToDataMapping(Record1<JSONB> record1) {
    try {
      return MAPPER.readValue(record1.get(DATA_MAPPING.DATA).data(), DataMapping.class);
    } catch (JsonProcessingException e) {
      throw new DisscoJsonBMappingException("Unable to convert jsonb to data mapping", e);
    }
  }

  private void postDataMappings(List<DataMapping> dataMappings) throws JsonProcessingException {
    List<Query> queryList = new ArrayList<>();
    for (var dataMapping : dataMappings) {
      queryList.add(context.insertInto(DATA_MAPPING)
          .set(DATA_MAPPING.ID, removeProxy(dataMapping.getId()))
          .set(DATA_MAPPING.VERSION, dataMapping.getSchemaVersion())
          .set(DATA_MAPPING.NAME, dataMapping.getSchemaName())
          .set(DATA_MAPPING.CREATED, dataMapping.getSchemaDateCreated().toInstant())
          .set(DATA_MAPPING.MODIFIED, dataMapping.getSchemaDateModified().toInstant())
          .set(DATA_MAPPING.CREATOR, dataMapping.getSchemaCreator().getId())
          .set(DATA_MAPPING.MAPPING_DATA_STANDARD, dataMapping.getOdsMappingDataStandard().value())
          .set(DATA_MAPPING.DATA, JSONB.valueOf(MAPPER.writeValueAsString(dataMapping))));
    }
    context.batch(queryList).execute();
  }

  @Test
  void testRollback() throws JsonProcessingException {
    // Given
    postDataMappings(List.of(givenDataMapping(HANDLE, 1)));

    // When
    repository.rollbackDataMappingCreation(HANDLE);

    // Then
    var result = repository.getDataMapping(HANDLE);
    assertThat(result).isNull();
  }

}
