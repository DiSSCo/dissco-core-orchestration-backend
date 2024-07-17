package eu.dissco.orchestration.backend.repository;

import static eu.dissco.orchestration.backend.database.jooq.Tables.MACHINE_ANNOTATION_SERVICE;
import static eu.dissco.orchestration.backend.repository.RepositoryUtils.getOffset;
import static eu.dissco.orchestration.backend.utils.HandleUtils.removeProxy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.orchestration.backend.exception.DisscoJsonBMappingException;
import eu.dissco.orchestration.backend.schema.MachineAnnotationService;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record1;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MachineAnnotationServiceRepository {

  private final DSLContext context;
  private final ObjectMapper mapper;

  public void createMachineAnnotationService(MachineAnnotationService mas) {
    mas.setOdsTimeToLive(getTTL(mas));
    context.insertInto(MACHINE_ANNOTATION_SERVICE)
        .set(MACHINE_ANNOTATION_SERVICE.ID, removeProxy(mas.getId()))
        .set(MACHINE_ANNOTATION_SERVICE.VERSION, mas.getSchemaVersion())
        .set(MACHINE_ANNOTATION_SERVICE.NAME, mas.getSchemaName())
        .set(MACHINE_ANNOTATION_SERVICE.DATE_CREATED, mas.getSchemaDateCreated().toInstant())
        .set(MACHINE_ANNOTATION_SERVICE.DATE_MODIFIED, mas.getSchemaDateModified().toInstant())
        .set(MACHINE_ANNOTATION_SERVICE.CREATOR, mas.getSchemaCreator().getId())
        .set(MACHINE_ANNOTATION_SERVICE.CONTAINER_IMAGE, mas.getOdsContainerImage())
        .set(MACHINE_ANNOTATION_SERVICE.CONTAINER_IMAGE_TAG, mas.getOdsContainerTag())
        .set(MACHINE_ANNOTATION_SERVICE.CREATIVE_WORK_STATE,
            mas.getSchemaCreativeWorkStatus())
        .set(MACHINE_ANNOTATION_SERVICE.SERVICE_AVAILABILITY,
            mas.getOdsServiceAvailability())
        .set(MACHINE_ANNOTATION_SERVICE.SOURCE_CODE_REPOSITORY,
            mas.getSchemaCodeRepository())
        .set(MACHINE_ANNOTATION_SERVICE.CODE_MAINTAINER, mas.getSchemaMaintainer().getId())
        .set(MACHINE_ANNOTATION_SERVICE.CODE_LICENSE, mas.getSchemaLicense())
        .set(MACHINE_ANNOTATION_SERVICE.BATCHING_PERMITTED, mas.getOdsBatchingPermitted())
        .set(MACHINE_ANNOTATION_SERVICE.TIME_TO_LIVE, mas.getOdsTimeToLive())
        .set(MACHINE_ANNOTATION_SERVICE.DATA, mapToJSONB(mas))
        .execute();
  }

  private JSONB mapToJSONB(MachineAnnotationService mas) {
    try {
      return JSONB.valueOf(mapper.writeValueAsString(mas));
    } catch (JsonProcessingException e) {
      throw new DisscoJsonBMappingException("Unable to map data mapping to jsonb", e);
    }
  }

  public Optional<MachineAnnotationService> getActiveMachineAnnotationService(String id) {
    return context.select(MACHINE_ANNOTATION_SERVICE.DATA)
        .from(MACHINE_ANNOTATION_SERVICE)
        .where(MACHINE_ANNOTATION_SERVICE.ID.eq(removeProxy(id)))
        .and(MACHINE_ANNOTATION_SERVICE.DATE_TOMBSTONED.isNull())
        .fetchOptional(this::mapToMas);
  }

  private MachineAnnotationService mapToMas(Record1<JSONB> record1) {
    try {
      return mapper.readValue(record1.get(MACHINE_ANNOTATION_SERVICE.DATA).data(),
          MachineAnnotationService.class);
    } catch (JsonProcessingException e) {
      throw new DisscoJsonBMappingException("Unable to convert jsonb to machine annotation service",
          e);
    }
  }

  public void deleteMachineAnnotationService(String id, Date deleted) {
    context.update(MACHINE_ANNOTATION_SERVICE)
        .set(MACHINE_ANNOTATION_SERVICE.DATE_TOMBSTONED, deleted.toInstant())
        .where(MACHINE_ANNOTATION_SERVICE.ID.eq(removeProxy(id)))
        .execute();
  }

  public MachineAnnotationService getMachineAnnotationService(String id) {
    return context.select(MACHINE_ANNOTATION_SERVICE.DATA)
        .from(MACHINE_ANNOTATION_SERVICE)
        .where(MACHINE_ANNOTATION_SERVICE.ID.eq(removeProxy(id)))
        .fetchOne(this::mapToMas);
  }

  public List<MachineAnnotationService> getMachineAnnotationServices(int pageNum,
      int pageSize) {
    int offset = getOffset(pageNum, pageSize);
    return context.select(MACHINE_ANNOTATION_SERVICE.DATA)
        .from(MACHINE_ANNOTATION_SERVICE)
        .where(MACHINE_ANNOTATION_SERVICE.DATE_TOMBSTONED.isNull())
        .limit(pageSize + 1)
        .offset(offset)
        .fetch(this::mapToMas);
  }

  public void updateMachineAnnotationService(MachineAnnotationService mas) {
    mas.setOdsTimeToLive(getTTL(mas));
    context.update(MACHINE_ANNOTATION_SERVICE)
        .set(MACHINE_ANNOTATION_SERVICE.VERSION, mas.getSchemaVersion())
        .set(MACHINE_ANNOTATION_SERVICE.NAME, mas.getSchemaName())
        .set(MACHINE_ANNOTATION_SERVICE.DATE_CREATED, mas.getSchemaDateCreated().toInstant())
        .set(MACHINE_ANNOTATION_SERVICE.DATE_MODIFIED, mas.getSchemaDateModified().toInstant())
        .set(MACHINE_ANNOTATION_SERVICE.CREATOR, mas.getSchemaCreator().getId())
        .set(MACHINE_ANNOTATION_SERVICE.CONTAINER_IMAGE, mas.getOdsContainerImage())
        .set(MACHINE_ANNOTATION_SERVICE.CONTAINER_IMAGE_TAG, mas.getOdsContainerTag())
        .set(MACHINE_ANNOTATION_SERVICE.CREATIVE_WORK_STATE,
            mas.getSchemaCreativeWorkStatus())
        .set(MACHINE_ANNOTATION_SERVICE.SERVICE_AVAILABILITY,
            mas.getOdsServiceAvailability())
        .set(MACHINE_ANNOTATION_SERVICE.SOURCE_CODE_REPOSITORY,
            mas.getSchemaCodeRepository())
        .set(MACHINE_ANNOTATION_SERVICE.CODE_MAINTAINER, mas.getSchemaMaintainer().getId())
        .set(MACHINE_ANNOTATION_SERVICE.CODE_LICENSE, mas.getSchemaLicense())
        .set(MACHINE_ANNOTATION_SERVICE.BATCHING_PERMITTED, mas.getOdsBatchingPermitted())
        .set(MACHINE_ANNOTATION_SERVICE.TIME_TO_LIVE, mas.getOdsTimeToLive())
        .set(MACHINE_ANNOTATION_SERVICE.DATA, mapToJSONB(mas))
        .where(MACHINE_ANNOTATION_SERVICE.ID.eq(removeProxy(mas.getId())))
        .execute();
  }

  public void rollbackMasCreation(String pid) {
    context.deleteFrom(MACHINE_ANNOTATION_SERVICE)
        .where(MACHINE_ANNOTATION_SERVICE.ID.eq(removeProxy(pid)))
        .execute();
  }

  private Integer getTTL(MachineAnnotationService mas) {
    return mas.getOdsTimeToLive() == null ? 86400 : mas.getOdsTimeToLive();
  }

}
