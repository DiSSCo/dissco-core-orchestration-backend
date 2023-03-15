package eu.dissco.orchestration.backend.repository;

import static eu.dissco.orchestration.backend.database.jooq.Tables.NEW_MAPPING;
import static eu.dissco.orchestration.backend.repository.RepositoryUtils.getOffset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.orchestration.backend.domain.Mapping;
import eu.dissco.orchestration.backend.domain.MappingRecord;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MappingRepository {

  private final ObjectMapper mapper;
  private final DSLContext context;

  public void createMapping(MappingRecord mappingRecord) {
    try {
      context.insertInto(NEW_MAPPING)
          .set(NEW_MAPPING.ID, mappingRecord.id())
          .set(NEW_MAPPING.VERSION, mappingRecord.version())
          .set(NEW_MAPPING.NAME, mappingRecord.mapping().name())
          .set(NEW_MAPPING.DESCRIPTION, mappingRecord.mapping().description())
          .set(NEW_MAPPING.MAPPING,
              JSONB.valueOf(mapper.writeValueAsString(mappingRecord.mapping().mapping())))
          .set(NEW_MAPPING.CREATED, mappingRecord.created())
          .set(NEW_MAPPING.CREATOR, mappingRecord.creator())
          .execute();
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public MappingRecord getMapping(String id) {
    return context.select(NEW_MAPPING.asterisk())
        .distinctOn(NEW_MAPPING.ID)
        .from(NEW_MAPPING)
        .where(NEW_MAPPING.ID.eq(id))
        .orderBy(NEW_MAPPING.ID, NEW_MAPPING.VERSION.desc())
        .fetchOne(this::mapToMappingRecord);
  }

  public MappingRecord getMappingOmitDeleted(String id) {
    return context.select(NEW_MAPPING.asterisk())
        .distinctOn(NEW_MAPPING.ID)
        .from(NEW_MAPPING)
        .where(NEW_MAPPING.ID.eq(id))
        .and(NEW_MAPPING.DELETED.isNull())
        .orderBy(NEW_MAPPING.ID, NEW_MAPPING.VERSION.desc())
        .fetchOne(this::mapToMappingRecord);
  }

  public List<MappingRecord> getMappings(int pageNum, int pageSize){
    int offset = getOffset(pageNum, pageSize);

    return context.select(NEW_MAPPING.asterisk())
        .from(NEW_MAPPING)
        .where(NEW_MAPPING.DELETED.isNull())
        .offset(offset)
        .limit(pageSize)
        .fetch(this::mapToMappingRecord);
  }

  public int mappingExists(String id){
    return context.select(NEW_MAPPING.ID)
        .from(NEW_MAPPING)
        .where(NEW_MAPPING.ID.eq(id))
        .and(NEW_MAPPING.DELETED.isNull())
        .fetch().size();
  }
  public void deleteMapping(String id, Instant deleted){
    context.update(NEW_MAPPING)
        .set(NEW_MAPPING.DELETED, deleted)
        .where(NEW_MAPPING.ID.eq(id))
        .execute();
  }

  private MappingRecord mapToMappingRecord(Record dbRecord) {
    try {
      var mapping = new Mapping(
          dbRecord.get(NEW_MAPPING.NAME),
          dbRecord.get(NEW_MAPPING.DESCRIPTION),
          mapper.readTree(dbRecord.get(NEW_MAPPING.MAPPING).data())
      );
      return new MappingRecord(
          dbRecord.get(NEW_MAPPING.ID),
          dbRecord.get(NEW_MAPPING.VERSION),
          dbRecord.get(NEW_MAPPING.CREATED),
          dbRecord.get(NEW_MAPPING.CREATOR),
          mapping
      );
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
