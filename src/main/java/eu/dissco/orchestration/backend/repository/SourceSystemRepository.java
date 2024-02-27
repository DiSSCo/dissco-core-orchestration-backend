package eu.dissco.orchestration.backend.repository;

import static eu.dissco.orchestration.backend.database.jooq.Tables.SOURCE_SYSTEM;
import static eu.dissco.orchestration.backend.repository.RepositoryUtils.getOffset;

import eu.dissco.orchestration.backend.domain.SourceSystem;
import eu.dissco.orchestration.backend.domain.SourceSystemRecord;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SourceSystemRepository {

  private final DSLContext context;

  public int createSourceSystem(SourceSystemRecord sourceSystemRecord) {
    return context.insertInto(SOURCE_SYSTEM)
        .set(SOURCE_SYSTEM.ID, sourceSystemRecord.id())
        .set(SOURCE_SYSTEM.VERSION, sourceSystemRecord.version())
        .set(SOURCE_SYSTEM.CREATOR, sourceSystemRecord.creator())
        .set(SOURCE_SYSTEM.CREATED, sourceSystemRecord.created())
        .set(SOURCE_SYSTEM.NAME, sourceSystemRecord.sourceSystem().name())
        .set(SOURCE_SYSTEM.ENDPOINT, sourceSystemRecord.sourceSystem().endpoint())
        .set(SOURCE_SYSTEM.DESCRIPTION, sourceSystemRecord.sourceSystem().description())
        .set(SOURCE_SYSTEM.MAPPING_ID, sourceSystemRecord.sourceSystem().mappingId())
        .set(SOURCE_SYSTEM.TRANSLATOR_TYPE, sourceSystemRecord.sourceSystem().translatorType())
        .execute();
  }

  public int updateSourceSystem(SourceSystemRecord sourceSystemRecord) {
    return context.update(SOURCE_SYSTEM)
        .set(SOURCE_SYSTEM.VERSION, sourceSystemRecord.version())
        .set(SOURCE_SYSTEM.CREATOR, sourceSystemRecord.creator())
        .set(SOURCE_SYSTEM.CREATED, sourceSystemRecord.created())
        .set(SOURCE_SYSTEM.NAME, sourceSystemRecord.sourceSystem().name())
        .set(SOURCE_SYSTEM.ENDPOINT, sourceSystemRecord.sourceSystem().endpoint())
        .set(SOURCE_SYSTEM.DESCRIPTION, sourceSystemRecord.sourceSystem().description())
        .set(SOURCE_SYSTEM.MAPPING_ID, sourceSystemRecord.sourceSystem().mappingId())
        .set(SOURCE_SYSTEM.TRANSLATOR_TYPE, sourceSystemRecord.sourceSystem().translatorType())
        .where(SOURCE_SYSTEM.ID.eq(sourceSystemRecord.id())).execute();
  }

  public SourceSystemRecord getSourceSystem(String id) {
    return context.select(SOURCE_SYSTEM.asterisk())
        .from(SOURCE_SYSTEM)
        .where(SOURCE_SYSTEM.ID.eq(id))
        .fetchOne(this::mapToSourceSystemRecord);
  }

  public Optional<SourceSystemRecord> getActiveSourceSystem(String id) {
    return context.select(SOURCE_SYSTEM.asterisk())
        .from(SOURCE_SYSTEM)
        .where(SOURCE_SYSTEM.ID.eq(id))
        .and(SOURCE_SYSTEM.DELETED.isNull())
        .fetchOptional(this::mapToSourceSystemRecord);
  }

  public void deleteSourceSystem(String id, Instant deleted) {
    context.update(SOURCE_SYSTEM)
        .set(SOURCE_SYSTEM.DELETED, deleted)
        .where(SOURCE_SYSTEM.ID.eq(id))
        .execute();
  }

  public List<SourceSystemRecord> getSourceSystems(int pageNum, int pageSize) {
    int offset = getOffset(pageNum, pageSize);
    return context.select(SOURCE_SYSTEM.asterisk())
        .from(SOURCE_SYSTEM)
        .where(SOURCE_SYSTEM.DELETED.isNull())
        .limit(pageSize + 1)
        .offset(offset)
        .fetch(this::mapToSourceSystemRecord);
  }

  private SourceSystemRecord mapToSourceSystemRecord(
      Record row) {
    return new SourceSystemRecord(
        row.get(SOURCE_SYSTEM.ID),
        row.get(SOURCE_SYSTEM.VERSION),
        row.get(SOURCE_SYSTEM.CREATOR),
        row.get(SOURCE_SYSTEM.CREATED),
        row.get(SOURCE_SYSTEM.DELETED),
        new SourceSystem(
            row.get(SOURCE_SYSTEM.NAME),
            row.get(SOURCE_SYSTEM.ENDPOINT),
            row.get(SOURCE_SYSTEM.DESCRIPTION),
            row.get(SOURCE_SYSTEM.TRANSLATOR_TYPE),
            row.get(SOURCE_SYSTEM.MAPPING_ID)));
  }


  public void rollbackSourceSystemCreation(String id) {
    context.deleteFrom(SOURCE_SYSTEM)
        .where(SOURCE_SYSTEM.ID.eq(id))
        .execute();
  }
}
