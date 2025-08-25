package eu.dissco.orchestration.backend.repository;

import static eu.dissco.orchestration.backend.database.jooq.Tables.SOURCE_SYSTEM;
import static eu.dissco.orchestration.backend.repository.RepositoryUtils.getOffset;
import static eu.dissco.orchestration.backend.utils.HandleUtils.removeProxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.orchestration.backend.database.jooq.enums.TranslatorType;
import eu.dissco.orchestration.backend.domain.ExportType;
import eu.dissco.orchestration.backend.exception.DisscoJsonBMappingException;
import eu.dissco.orchestration.backend.schema.SourceSystem;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record1;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SourceSystemRepository {

  private final DSLContext context;
  private final ObjectMapper mapper;

  public void createSourceSystem(SourceSystem sourceSystem) {
    context.insertInto(SOURCE_SYSTEM)
        .set(SOURCE_SYSTEM.ID, removeProxy(sourceSystem.getId()))
        .set(SOURCE_SYSTEM.VERSION, sourceSystem.getSchemaVersion())
        .set(SOURCE_SYSTEM.NAME, sourceSystem.getSchemaName())
        .set(SOURCE_SYSTEM.ENDPOINT, sourceSystem.getSchemaUrl().toString())
        .set(SOURCE_SYSTEM.FILTERS, sourceSystem.getOdsFilters().toArray(new String[0]))
        .set(SOURCE_SYSTEM.CREATOR, sourceSystem.getSchemaCreator().getId())
        .set(SOURCE_SYSTEM.CREATED, sourceSystem.getSchemaDateCreated().toInstant())
        .set(SOURCE_SYSTEM.MODIFIED, sourceSystem.getSchemaDateModified().toInstant())
        .set(SOURCE_SYSTEM.MAPPING_ID, removeProxy(sourceSystem.getOdsDataMappingID()))
        .set(SOURCE_SYSTEM.TRANSLATOR_TYPE, TranslatorType.valueOf(
            sourceSystem.getOdsTranslatorType().value()))
        .set(SOURCE_SYSTEM.DATA, mapToJSONB(sourceSystem))
        .execute();
  }

  private JSONB mapToJSONB(SourceSystem sourceSystem) {
    try {
      return JSONB.valueOf(mapper.writeValueAsString(sourceSystem));
    } catch (JsonProcessingException e) {
      throw new DisscoJsonBMappingException("Unable to map source system to jsonb", e);
    }
  }

  public void updateSourceSystem(SourceSystem sourceSystem) {
    context.update(SOURCE_SYSTEM)
        .set(SOURCE_SYSTEM.VERSION, sourceSystem.getSchemaVersion())
        .set(SOURCE_SYSTEM.NAME, sourceSystem.getSchemaName())
        .set(SOURCE_SYSTEM.ENDPOINT, sourceSystem.getSchemaUrl().toString())
        .set(SOURCE_SYSTEM.FILTERS, sourceSystem.getOdsFilters().toArray(new String[0]))
        .set(SOURCE_SYSTEM.CREATOR, sourceSystem.getSchemaCreator().getId())
        .set(SOURCE_SYSTEM.CREATED, sourceSystem.getSchemaDateCreated().toInstant())
        .set(SOURCE_SYSTEM.MODIFIED, sourceSystem.getSchemaDateModified().toInstant())
        .set(SOURCE_SYSTEM.MAPPING_ID, removeProxy(sourceSystem.getOdsDataMappingID()))
        .set(SOURCE_SYSTEM.TRANSLATOR_TYPE, TranslatorType.valueOf(
            sourceSystem.getOdsTranslatorType().value()))
        .set(SOURCE_SYSTEM.DATA, mapToJSONB(sourceSystem))
        .where(SOURCE_SYSTEM.ID.eq(removeProxy(sourceSystem.getId()))).execute();
  }

  public SourceSystem getSourceSystem(String id) {
    return context.select(SOURCE_SYSTEM.DATA)
        .from(SOURCE_SYSTEM)
        .where(SOURCE_SYSTEM.ID.eq(removeProxy(id)))
        .fetchOne(this::mapToSourceSystem);
  }

  public Optional<SourceSystem> getActiveSourceSystem(String id) {
    return context.select(SOURCE_SYSTEM.DATA)
        .from(SOURCE_SYSTEM)
        .where(SOURCE_SYSTEM.ID.eq(removeProxy(id)))
        .and(SOURCE_SYSTEM.TOMBSTONED.isNull())
        .fetchOptional(this::mapToSourceSystem);
  }

  public void tombstoneSourceSystem(SourceSystem tombstoneSourceSystem, Instant timestamp) {
    context.update(SOURCE_SYSTEM)
        .set(SOURCE_SYSTEM.TOMBSTONED, timestamp)
        .set(SOURCE_SYSTEM.MODIFIED, timestamp)
        .set(SOURCE_SYSTEM.VERSION, tombstoneSourceSystem.getSchemaVersion())
        .set(SOURCE_SYSTEM.DATA, mapToJSONB(tombstoneSourceSystem))
        .where(SOURCE_SYSTEM.ID.eq(removeProxy(tombstoneSourceSystem.getId())))
        .execute();
  }

  public List<SourceSystem> getSourceSystems(int pageNum, int pageSize) {
    int offset = getOffset(pageNum, pageSize);
    return context.select(SOURCE_SYSTEM.DATA)
        .from(SOURCE_SYSTEM)
        .where(SOURCE_SYSTEM.TOMBSTONED.isNull())
        .limit(pageSize + 1)
        .offset(offset)
        .fetch(this::mapToSourceSystem);
  }

  private SourceSystem mapToSourceSystem(Record1<JSONB> record1) {
    try {
      return mapper.readValue(record1.get(SOURCE_SYSTEM.DATA).data(), SourceSystem.class);
    } catch (JsonProcessingException e) {
      throw new DisscoJsonBMappingException("Unable to convert jsonb to source system", e);
    }
  }

  public void rollbackSourceSystemCreation(String id) {
    context.deleteFrom(SOURCE_SYSTEM)
        .where(SOURCE_SYSTEM.ID.eq(removeProxy(id)))
        .execute();
  }

  public String getExportLink(String id, ExportType exportType) {
    var linkColumn = switch (exportType) {
      case DWC_DP -> SOURCE_SYSTEM.DWC_DP_LINK;
      case DWCA -> SOURCE_SYSTEM.DWCA_LINK;
    };
    return context.select(linkColumn)
        .from(SOURCE_SYSTEM)
        .where(SOURCE_SYSTEM.ID.eq(id))
        .fetchOne(Record1::value1);
  }
}
