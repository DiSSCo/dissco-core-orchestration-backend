package eu.dissco.orchestration.backend.repository;

import static eu.dissco.orchestration.backend.database.jooq.Tables.NEW_SOURCE_SYSTEM;

import eu.dissco.orchestration.backend.domain.SourceSystemRecord;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
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
        .set(NEW_SOURCE_SYSTEM.CREATED, sourceSystemRecord.created())
        .execute();
  }

  public int updateSourceSystem(SourceSystemRecord sourceSystemRecord) {
    return context.update(NEW_SOURCE_SYSTEM)
        .set(NEW_SOURCE_SYSTEM.NAME, sourceSystemRecord.sourceSystem().name())
        .set(NEW_SOURCE_SYSTEM.ENDPOINT, sourceSystemRecord.sourceSystem().endpoint())
        .set(NEW_SOURCE_SYSTEM.DESCRIPTION, sourceSystemRecord.sourceSystem().description())
        .set(NEW_SOURCE_SYSTEM.MAPPING_ID, sourceSystemRecord.sourceSystem().mappingId())
        .set(NEW_SOURCE_SYSTEM.CREATED, sourceSystemRecord.created())
        .where(NEW_SOURCE_SYSTEM.ID.eq(sourceSystemRecord.id()))
        .execute();
  }
}
