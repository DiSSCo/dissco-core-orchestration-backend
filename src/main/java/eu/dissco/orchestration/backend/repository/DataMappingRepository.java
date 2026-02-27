package eu.dissco.orchestration.backend.repository;

import static eu.dissco.orchestration.backend.database.jooq.Tables.DATA_MAPPING;
import static eu.dissco.orchestration.backend.repository.RepositoryUtils.getOffset;
import static eu.dissco.orchestration.backend.utils.HandleUtils.removeProxy;

import eu.dissco.orchestration.backend.schema.DataMapping;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record1;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
@RequiredArgsConstructor
public class DataMappingRepository {

  private final JsonMapper mapper;
  private final DSLContext context;

  public void createDataMapping(DataMapping dataMapping) {
    context.insertInto(DATA_MAPPING)
        .set(DATA_MAPPING.ID, removeProxy(dataMapping.getId()))
        .set(DATA_MAPPING.VERSION, dataMapping.getSchemaVersion())
        .set(DATA_MAPPING.NAME, dataMapping.getSchemaName())
        .set(DATA_MAPPING.CREATED, dataMapping.getSchemaDateCreated().toInstant())
        .set(DATA_MAPPING.MODIFIED, dataMapping.getSchemaDateModified().toInstant())
        .set(DATA_MAPPING.CREATOR, dataMapping.getSchemaCreator().getId())
        .set(DATA_MAPPING.MAPPING_DATA_STANDARD, dataMapping.getOdsMappingDataStandard().value())
        .set(DATA_MAPPING.DATA, mapToJSONB(dataMapping))
        .execute();
  }

  private JSONB mapToJSONB(DataMapping dataMapping) {
    return JSONB.valueOf(mapper.writeValueAsString(dataMapping));
  }

  public void updateDataMapping(DataMapping dataMapping) {
    context.update(DATA_MAPPING)
        .set(DATA_MAPPING.VERSION, dataMapping.getSchemaVersion())
        .set(DATA_MAPPING.NAME, dataMapping.getSchemaName())
        .set(DATA_MAPPING.CREATED, dataMapping.getSchemaDateCreated().toInstant())
        .set(DATA_MAPPING.MODIFIED, dataMapping.getSchemaDateModified().toInstant())
        .set(DATA_MAPPING.CREATOR, dataMapping.getSchemaCreator().getId())
        .set(DATA_MAPPING.MAPPING_DATA_STANDARD, dataMapping.getOdsMappingDataStandard().value())
        .set(DATA_MAPPING.DATA, mapToJSONB(dataMapping))
        .where(DATA_MAPPING.ID.eq(removeProxy(dataMapping.getId())))
        .execute();
  }

  public DataMapping getDataMapping(String id) {
    return context.select(DATA_MAPPING.DATA)
        .distinctOn(DATA_MAPPING.ID)
        .from(DATA_MAPPING)
        .where(DATA_MAPPING.ID.eq(removeProxy(id)))
        .fetchOne(this::mapToDataMapping);
  }

  public Optional<DataMapping> getActiveDataMapping(String id) {
    return context.select(DATA_MAPPING.DATA)
        .distinctOn(DATA_MAPPING.ID)
        .from(DATA_MAPPING)
        .where(DATA_MAPPING.ID.eq(removeProxy(id)))
        .and(DATA_MAPPING.TOMBSTONED.isNull())
        .fetchOptional(this::mapToDataMapping);
  }

  public List<DataMapping> getDataMappings(int pageNum, int pageSize) {
    int offset = getOffset(pageNum, pageSize);
    return context.select(DATA_MAPPING.DATA)
        .from(DATA_MAPPING)
        .where(DATA_MAPPING.TOMBSTONED.isNull())
        .offset(offset)
        .limit(pageSize + 1)
        .fetch(this::mapToDataMapping);
  }

  public void tombstoneDataMapping(DataMapping tombstoneDataMapping, Instant timestamp) {
    context.update(DATA_MAPPING)
        .set(DATA_MAPPING.TOMBSTONED, timestamp)
        .set(DATA_MAPPING.MODIFIED, timestamp)
        .set(DATA_MAPPING.VERSION, tombstoneDataMapping.getSchemaVersion())
        .set(DATA_MAPPING.DATA, mapToJSONB(tombstoneDataMapping))
        .where(DATA_MAPPING.ID.eq(removeProxy(tombstoneDataMapping.getId())))
        .execute();
  }

  private DataMapping mapToDataMapping(Record1<JSONB> record1) {
    return mapper.readValue(record1.get(DATA_MAPPING.DATA).data(), DataMapping.class);
  }

  public void rollbackDataMappingCreation(String id) {
    context.deleteFrom(DATA_MAPPING)
        .where(DATA_MAPPING.ID.eq(removeProxy(id)))
        .execute();
  }
}
