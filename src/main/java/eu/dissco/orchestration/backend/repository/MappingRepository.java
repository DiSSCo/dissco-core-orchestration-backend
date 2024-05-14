package eu.dissco.orchestration.backend.repository;

import static eu.dissco.orchestration.backend.database.jooq.Tables.MAPPING;
import static eu.dissco.orchestration.backend.repository.RepositoryUtils.getOffset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.orchestration.backend.domain.Mapping;
import eu.dissco.orchestration.backend.domain.MappingRecord;
import eu.dissco.orchestration.backend.domain.SourceDataStandard;
import eu.dissco.orchestration.backend.exception.DisscoJsonBMappingException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
    context.insertInto(MAPPING)
        .set(MAPPING.ID, mappingRecord.id())
        .set(MAPPING.VERSION, mappingRecord.version())
        .set(MAPPING.NAME, mappingRecord.mapping().name())
        .set(MAPPING.DESCRIPTION, mappingRecord.mapping().description())
        .set(MAPPING.SOURCEDATASTANDARD, mappingRecord.mapping().sourceDataStandard().toString())
        .set(MAPPING.MAPPING_, JSONB.valueOf(mappingRecord.mapping().mapping().toString()))
        .set(MAPPING.CREATED, mappingRecord.created())
        .set(MAPPING.CREATOR, mappingRecord.creator())
        .execute();
  }

  public void updateMapping(MappingRecord mappingRecord) {
    context.update(MAPPING)
        .set(MAPPING.VERSION, mappingRecord.version())
        .set(MAPPING.NAME, mappingRecord.mapping().name())
        .set(MAPPING.DESCRIPTION, mappingRecord.mapping().description())
        .set(MAPPING.SOURCEDATASTANDARD, mappingRecord.mapping().sourceDataStandard().toString())
        .set(MAPPING.MAPPING_,
            JSONB.valueOf(mappingRecord.mapping().mapping().toString()))
        .set(MAPPING.CREATED, mappingRecord.created())
        .set(MAPPING.CREATOR, mappingRecord.creator())
        .where(MAPPING.ID.eq(mappingRecord.id()))
        .execute();
  }


  public MappingRecord getMapping(String id) {
    return context.select(MAPPING.asterisk())
        .distinctOn(MAPPING.ID)
        .from(MAPPING)
        .where(MAPPING.ID.eq(id))
        .orderBy(MAPPING.ID, MAPPING.VERSION.desc())
        .fetchOne(this::mapToMappingRecord);
  }

  public Optional<MappingRecord> getActiveMapping(String id) {
    return context.select(MAPPING.asterisk())
        .distinctOn(MAPPING.ID)
        .from(MAPPING)
        .where(MAPPING.ID.eq(id))
        .and(MAPPING.DELETED.isNull())
        .orderBy(MAPPING.ID, MAPPING.VERSION.desc())
        .fetchOptional(this::mapToMappingRecord);
  }

  public List<MappingRecord> getMappings(int pageNum, int pageSize) {
    int offset = getOffset(pageNum, pageSize);

    return context.select(MAPPING.asterisk())
        .from(MAPPING)
        .where(MAPPING.DELETED.isNull())
        .offset(offset)
        .limit(pageSize + 1)
        .fetch(this::mapToMappingRecord);
  }

  public void deleteMapping(String id, Instant deleted) {
    context.update(MAPPING)
        .set(MAPPING.DELETED, deleted)
        .where(MAPPING.ID.eq(id))
        .execute();
  }

  private MappingRecord mapToMappingRecord(Record dbRecord) {
    var mapping = new Mapping(
        dbRecord.get(MAPPING.NAME),
        dbRecord.get(MAPPING.DESCRIPTION),
        mapToJson(dbRecord.get(MAPPING.MAPPING_)),
        SourceDataStandard.fromString(dbRecord.get(MAPPING.SOURCEDATASTANDARD)));
    return new MappingRecord(
        dbRecord.get(MAPPING.ID),
        dbRecord.get(MAPPING.VERSION),
        dbRecord.get(MAPPING.CREATED),
        dbRecord.get(MAPPING.DELETED),
        dbRecord.get(MAPPING.CREATOR),
        mapping
    );
  }

  private JsonNode mapToJson(JSONB jsonb) {
    try {
      return mapper.readTree(jsonb.data());
    } catch (JsonProcessingException e) {
      throw new DisscoJsonBMappingException("Failed to parse jsonb field to json: " + jsonb.data(),
          e);
    }
  }

  public void rollbackMappingCreation(String id) {
    context.deleteFrom(MAPPING)
        .where(MAPPING.ID.eq(id))
        .execute();
  }
}
