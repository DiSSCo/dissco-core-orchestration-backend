package eu.dissco.orchestration.backend.repository;

import static eu.dissco.orchestration.backend.database.jooq.Tables.NEW_MAPPING;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.orchestration.backend.domain.Mapping;
import eu.dissco.orchestration.backend.domain.MappingRecord;
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

  private MappingRecord mapToMappingRecord(Record record) {
    try {
      var mapping = new Mapping(
          record.get(NEW_MAPPING.NAME),
          record.get(NEW_MAPPING.DESCRIPTION),
          mapper.readTree(record.get(NEW_MAPPING.MAPPING).data())
      );
      return new MappingRecord(
          record.get(NEW_MAPPING.ID),
          record.get(NEW_MAPPING.VERSION),
          record.get(NEW_MAPPING.CREATED),
          record.get(NEW_MAPPING.CREATOR),
          mapping
      );
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
