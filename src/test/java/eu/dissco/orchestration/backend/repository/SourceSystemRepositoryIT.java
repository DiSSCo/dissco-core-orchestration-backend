package eu.dissco.orchestration.backend.repository;

import static eu.dissco.orchestration.backend.database.jooq.Tables.NEW_SOURCE_SYSTEM;
import static eu.dissco.orchestration.backend.testutils.TestUtils.CREATED;
import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE_ALT;
import static eu.dissco.orchestration.backend.testutils.TestUtils.OBJECT_DESCRIPTION;
import static eu.dissco.orchestration.backend.testutils.TestUtils.OBJECT_NAME;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SS_ENDPOINT;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystem;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemRecord;
import static org.assertj.core.api.Assertions.assertThat;

import eu.dissco.orchestration.backend.domain.SourceSystem;
import eu.dissco.orchestration.backend.domain.SourceSystemRecord;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.jooq.Query;
import org.jooq.Record;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SourceSystemRepositoryIT extends BaseRepositoryIT {

  private SourceSystemRepository repository;

  @BeforeEach
  void setup() {
    repository = new SourceSystemRepository(context);
  }

  @AfterEach
  void destroy() {
    context.truncate(NEW_SOURCE_SYSTEM).execute();
  }

  @Test
  void testCreateSourceSystem() {
    // Given
    var ssRecord = givenSourceSystemRecord();

    // When
    repository.createSourceSystem(ssRecord);
    var result = getAllSourceSystems();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(ssRecord);
  }

  @Test
  void testUpdateSourceSystem() {
    // Given
    var originalRecord = givenSourceSystemRecord();
    postSourceSystem(List.of(originalRecord));
    var updatedRecord = new SourceSystemRecord(HANDLE, CREATED, null, new SourceSystem(
        "new name",
        SS_ENDPOINT,
        OBJECT_DESCRIPTION,
        HANDLE_ALT
    ));

    // When
    repository.updateSourceSystem(updatedRecord);
    var result = getAllSourceSystems();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(updatedRecord);
  }

  @Test
  void testGetSourceSystemById() {
    // Given
    var expected = givenSourceSystemRecord();
    postSourceSystem(List.of(expected));

    // When
    var result = repository.getSourceSystem(HANDLE);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testGetSourceSystemByIdWasDeleted() {
    // Given
    var expected = new SourceSystemRecord(HANDLE, CREATED, CREATED, givenSourceSystem());
    postSourceSystem(List.of(expected));

    // When
    var result = repository.getSourceSystem(HANDLE);

    // Then
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testGetSourceSystems() {
    // Given
    int pageNum = 1;
    int pageSize = 10;
    List<SourceSystemRecord> ssRecords = IntStream.range(0, pageSize).boxed()
        .map(this::givenSourceSystemRecordWithId).toList();
    postSourceSystem(ssRecords);

    // When
    var result = repository.getSourceSystems(pageNum, pageSize);

    assertThat(result).isEqualTo(ssRecords);
  }

  @Test
  void testGetSourceSystemsSecondPage() {
    // Given
    int pageNum = 2;
    int pageSize = 10;

    List<SourceSystemRecord> ssRecords = IntStream.range(0, pageSize + 1).boxed()
        .map(this::givenSourceSystemRecordWithId).toList();
    postSourceSystem(ssRecords);

    // When
    var result = repository.getSourceSystems(pageNum, pageSize);

    assertThat(result).hasSize(1);
  }

  @Test
  void testDeleteSourceSystem() {
    // Given
    var sourceSystemRecord = givenSourceSystemRecord();
    postSourceSystem(List.of(sourceSystemRecord));

    // When
    repository.deleteSourceSystem(sourceSystemRecord.id(), CREATED);
    var result = getDeleted(HANDLE);

    // Then
    assertThat(result).isEqualTo(CREATED);
  }

  @Test
  void testGetActiveSourceSystem() {
    // Given
    var sourceSystemRecord = givenSourceSystemRecord();
    postSourceSystem(List.of(sourceSystemRecord));

    // When
    var result = repository.getActiveSourceSystem(sourceSystemRecord.id());

    // Then
    assertThat(result).contains(sourceSystemRecord);
  }

  @Test
  void testGetActiveSourceSystemWasDeleted() {
    var sourceSystemRecord = givenSourceSystemRecord();
    postSourceSystem(List.of(sourceSystemRecord));
    context.update(NEW_SOURCE_SYSTEM)
        .set(NEW_SOURCE_SYSTEM.DELETED, CREATED)
        .where(NEW_SOURCE_SYSTEM.ID.eq(HANDLE))
        .execute();

    // When
    var result = repository.getActiveSourceSystem(HANDLE);

    // Then
    assertThat(result).isEmpty();
  }

  private SourceSystemRecord givenSourceSystemRecordWithId(int id) {
    return new SourceSystemRecord(String.valueOf(id), CREATED, null,
        givenSourceSystemWithId(id + "a"));
  }

  private SourceSystem givenSourceSystemWithId(String endPoint) {
    return new SourceSystem(
        OBJECT_NAME,
        endPoint,
        OBJECT_DESCRIPTION,
        HANDLE_ALT
    );
  }

  private List<SourceSystemRecord> getAllSourceSystems() {
    return context.select(NEW_SOURCE_SYSTEM.asterisk())
        .from(NEW_SOURCE_SYSTEM)
        .fetch(this::mapToSourceSystemRecord);
  }

  private Instant getDeleted(String id) {
    return context.select(NEW_SOURCE_SYSTEM.ID, NEW_SOURCE_SYSTEM.DELETED)
        .from(NEW_SOURCE_SYSTEM)
        .where(NEW_SOURCE_SYSTEM.ID.eq(id))
        .fetchOne(this::getInstantDeleted);
  }

  private Instant getInstantDeleted(Record dbRecord) {
    return dbRecord.get(NEW_SOURCE_SYSTEM.DELETED);
  }

  private SourceSystemRecord mapToSourceSystemRecord(Record row) {
    return new SourceSystemRecord(
        row.get(NEW_SOURCE_SYSTEM.ID),
        row.get(NEW_SOURCE_SYSTEM.CREATED),
        null, new SourceSystem(
        row.get(NEW_SOURCE_SYSTEM.NAME),
        row.get(NEW_SOURCE_SYSTEM.ENDPOINT),
        row.get(NEW_SOURCE_SYSTEM.DESCRIPTION),
        row.get(NEW_SOURCE_SYSTEM.MAPPING_ID)));
  }

  private void postSourceSystem(List<SourceSystemRecord> ssRecords) {
    List<Query> queryList = new ArrayList<>();
    for (var sourceSystemRecord : ssRecords) {
      queryList.add(context.insertInto(NEW_SOURCE_SYSTEM)
          .set(NEW_SOURCE_SYSTEM.ID, sourceSystemRecord.id())
          .set(NEW_SOURCE_SYSTEM.NAME, sourceSystemRecord.sourceSystem().name())
          .set(NEW_SOURCE_SYSTEM.ENDPOINT, sourceSystemRecord.sourceSystem().endpoint())
          .set(NEW_SOURCE_SYSTEM.DESCRIPTION, sourceSystemRecord.sourceSystem().description())
          .set(NEW_SOURCE_SYSTEM.MAPPING_ID, sourceSystemRecord.sourceSystem().mappingId())
          .set(NEW_SOURCE_SYSTEM.DELETED, sourceSystemRecord.deleted())
          .set(NEW_SOURCE_SYSTEM.CREATED, sourceSystemRecord.created()));
    }
    context.batch(queryList).execute();
  }

}
