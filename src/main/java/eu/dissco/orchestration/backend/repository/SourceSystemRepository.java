package eu.dissco.orchestration.backend.repository;

import static eu.dissco.orchestration.backend.database.jooq.Tables.NEW_SOURCE_SYSTEM;

import eu.dissco.orchestration.backend.domain.SourceSystem;
import eu.dissco.orchestration.backend.domain.SourceSystemRecord;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record6;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SourceSystemRepository {

  private final DSLContext context;

  public int createSourceSystem(SourceSystemRecord sourceSystemRecord) {
    return context.insertInto(NEW_SOURCE_SYSTEM)
        .set(NEW_SOURCE_SYSTEM.ID, sourceSystemRecord.id())
        .set(NEW_SOURCE_SYSTEM.NAME, sourceSystemRecord.sourceSystem().name())
        .set(NEW_SOURCE_SYSTEM.ENDPOINT, sourceSystemRecord.sourceSystem().endpoint())
        .set(NEW_SOURCE_SYSTEM.DESCRIPTION, sourceSystemRecord.sourceSystem().description())
        .set(NEW_SOURCE_SYSTEM.MAPPING_ID, sourceSystemRecord.sourceSystem().mappingId())
        .set(NEW_SOURCE_SYSTEM.CREATED, sourceSystemRecord.created()).execute();
  }

  public int updateSourceSystem(SourceSystemRecord sourceSystemRecord) {
    return context.update(NEW_SOURCE_SYSTEM)
        .set(NEW_SOURCE_SYSTEM.NAME, sourceSystemRecord.sourceSystem().name())
        .set(NEW_SOURCE_SYSTEM.ENDPOINT, sourceSystemRecord.sourceSystem().endpoint())
        .set(NEW_SOURCE_SYSTEM.DESCRIPTION, sourceSystemRecord.sourceSystem().description())
        .set(NEW_SOURCE_SYSTEM.MAPPING_ID, sourceSystemRecord.sourceSystem().mappingId())
        .set(NEW_SOURCE_SYSTEM.CREATED, sourceSystemRecord.created())
        .where(NEW_SOURCE_SYSTEM.ID.eq(sourceSystemRecord.id())).execute();
  }

  public List<SourceSystemRecord> getSourceSystems(int pageNum, int pageSize) {
    int offset = getOffset(pageNum, pageSize);
    return context.select(NEW_SOURCE_SYSTEM.ID, NEW_SOURCE_SYSTEM.CREATED, NEW_SOURCE_SYSTEM.NAME,
            NEW_SOURCE_SYSTEM.ENDPOINT, NEW_SOURCE_SYSTEM.DESCRIPTION, NEW_SOURCE_SYSTEM.MAPPING_ID)
        .from(NEW_SOURCE_SYSTEM)
        .limit(pageSize)
        .offset(offset)
        .fetch(this::mapToSourceSystemRecord);
  }

  private SourceSystemRecord mapToSourceSystemRecord(
      Record6<String, Instant, String, String, String, String> row) {
    return new SourceSystemRecord(
        row.get(NEW_SOURCE_SYSTEM.ID),
        row.get(NEW_SOURCE_SYSTEM.CREATED),
        new SourceSystem(
            row.get(NEW_SOURCE_SYSTEM.NAME),
            row.get(NEW_SOURCE_SYSTEM.ENDPOINT),
            row.get(NEW_SOURCE_SYSTEM.DESCRIPTION),
            row.get(NEW_SOURCE_SYSTEM.MAPPING_ID)));
  }

  private int getOffset(int pageNum, int pageSize) {
    int offset = 0;
    if (pageNum > 1) {
      offset = offset + (pageSize * (pageNum - 1));
    }
    return offset;
  }


}
