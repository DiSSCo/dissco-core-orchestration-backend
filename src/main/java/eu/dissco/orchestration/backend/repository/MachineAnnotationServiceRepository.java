package eu.dissco.orchestration.backend.repository;

import static eu.dissco.orchestration.backend.database.jooq.Tables.NEW_MACHINE_ANNOTATION_SERVICES;
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
    context.insertInto(NEW_MACHINE_ANNOTATION_SERVICES)
        .set(NEW_MACHINE_ANNOTATION_SERVICES.ID, removeProxy(mas.getId()))
        .set(NEW_MACHINE_ANNOTATION_SERVICES.VERSION, mas.getSchemaVersion())
        .set(NEW_MACHINE_ANNOTATION_SERVICES.NAME, mas.getSchemaName())
        .set(NEW_MACHINE_ANNOTATION_SERVICES.DATE_CREATED, mas.getSchemaDateCreated().toInstant())
        .set(NEW_MACHINE_ANNOTATION_SERVICES.DATE_MODIFIED, mas.getSchemaDateModified().toInstant())
        .set(NEW_MACHINE_ANNOTATION_SERVICES.CREATOR, mas.getSchemaCreator().getId())
        .set(NEW_MACHINE_ANNOTATION_SERVICES.CONTAINER_IMAGE, mas.getOdsContainerImage())
        .set(NEW_MACHINE_ANNOTATION_SERVICES.CONTAINER_IMAGE_TAG, mas.getOdsContainerTag())
        .set(NEW_MACHINE_ANNOTATION_SERVICES.CREATIVE_WORK_STATE,
            mas.getSchemaCreativeWorkStatus())
        .set(NEW_MACHINE_ANNOTATION_SERVICES.SERVICE_AVAILABILITY,
            mas.getOdsServiceAvailability())
        .set(NEW_MACHINE_ANNOTATION_SERVICES.SOURCE_CODE_REPOSITORY,
            mas.getSchemaCodeRepository())
        .set(NEW_MACHINE_ANNOTATION_SERVICES.CODE_MAINTAINER, mas.getSchemaMaintainer().getId())
        .set(NEW_MACHINE_ANNOTATION_SERVICES.CODE_LICENSE, mas.getSchemaLicense())
        .set(NEW_MACHINE_ANNOTATION_SERVICES.BATCHING_PERMITTED, mas.getOdsBatchingPermitted())
        .set(NEW_MACHINE_ANNOTATION_SERVICES.TIME_TO_LIVE, mas.getOdsTimeToLive())
        .set(NEW_MACHINE_ANNOTATION_SERVICES.DATA, mapToJSONB(mas))
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
    return context.select(NEW_MACHINE_ANNOTATION_SERVICES.DATA)
        .from(NEW_MACHINE_ANNOTATION_SERVICES)
        .where(NEW_MACHINE_ANNOTATION_SERVICES.ID.eq(removeProxy(id)))
        .and(NEW_MACHINE_ANNOTATION_SERVICES.DATE_TOMBSTONED.isNull())
        .fetchOptional(this::mapToMas);
  }

  private MachineAnnotationService mapToMas(Record1<JSONB> record1) {
    try {
      return mapper.readValue(record1.get(NEW_MACHINE_ANNOTATION_SERVICES.DATA).data(),
          MachineAnnotationService.class);
    } catch (JsonProcessingException e) {
      throw new DisscoJsonBMappingException("Unable to convert jsonb to machine annotation service",
          e);
    }
  }

  public void deleteMachineAnnotationService(String id, Date deleted) {
    context.update(NEW_MACHINE_ANNOTATION_SERVICES)
        .set(NEW_MACHINE_ANNOTATION_SERVICES.DATE_TOMBSTONED, deleted.toInstant())
        .where(NEW_MACHINE_ANNOTATION_SERVICES.ID.eq(removeProxy(id)))
        .execute();
  }

  public MachineAnnotationService getMachineAnnotationService(String id) {
    return context.select(NEW_MACHINE_ANNOTATION_SERVICES.DATA)
        .from(NEW_MACHINE_ANNOTATION_SERVICES)
        .where(NEW_MACHINE_ANNOTATION_SERVICES.ID.eq(removeProxy(id)))
        .fetchOne(this::mapToMas);
  }

  public List<MachineAnnotationService> getMachineAnnotationServices(int pageNum,
      int pageSize) {
    int offset = getOffset(pageNum, pageSize);
    return context.select(NEW_MACHINE_ANNOTATION_SERVICES.DATA)
        .from(NEW_MACHINE_ANNOTATION_SERVICES)
        .where(NEW_MACHINE_ANNOTATION_SERVICES.DATE_TOMBSTONED.isNull())
        .limit(pageSize + 1)
        .offset(offset)
        .fetch(this::mapToMas);
  }

  public void updateMachineAnnotationService(MachineAnnotationService mas) {
    mas.setOdsTimeToLive(getTTL(mas));
    context.update(NEW_MACHINE_ANNOTATION_SERVICES)
        .set(NEW_MACHINE_ANNOTATION_SERVICES.VERSION, mas.getSchemaVersion())
        .set(NEW_MACHINE_ANNOTATION_SERVICES.NAME, mas.getSchemaName())
        .set(NEW_MACHINE_ANNOTATION_SERVICES.DATE_CREATED, mas.getSchemaDateCreated().toInstant())
        .set(NEW_MACHINE_ANNOTATION_SERVICES.DATE_MODIFIED, mas.getSchemaDateModified().toInstant())
        .set(NEW_MACHINE_ANNOTATION_SERVICES.CREATOR, mas.getSchemaCreator().getId())
        .set(NEW_MACHINE_ANNOTATION_SERVICES.CONTAINER_IMAGE, mas.getOdsContainerImage())
        .set(NEW_MACHINE_ANNOTATION_SERVICES.CONTAINER_IMAGE_TAG, mas.getOdsContainerTag())
        .set(NEW_MACHINE_ANNOTATION_SERVICES.CREATIVE_WORK_STATE,
            mas.getSchemaCreativeWorkStatus())
        .set(NEW_MACHINE_ANNOTATION_SERVICES.SERVICE_AVAILABILITY,
            mas.getOdsServiceAvailability())
        .set(NEW_MACHINE_ANNOTATION_SERVICES.SOURCE_CODE_REPOSITORY,
            mas.getSchemaCodeRepository())
        .set(NEW_MACHINE_ANNOTATION_SERVICES.CODE_MAINTAINER, mas.getSchemaMaintainer().getId())
        .set(NEW_MACHINE_ANNOTATION_SERVICES.CODE_LICENSE, mas.getSchemaLicense())
        .set(NEW_MACHINE_ANNOTATION_SERVICES.BATCHING_PERMITTED, mas.getOdsBatchingPermitted())
        .set(NEW_MACHINE_ANNOTATION_SERVICES.TIME_TO_LIVE, mas.getOdsTimeToLive())
        .set(NEW_MACHINE_ANNOTATION_SERVICES.DATA, mapToJSONB(mas))
        .where(NEW_MACHINE_ANNOTATION_SERVICES.ID.eq(removeProxy(mas.getId())))
        .execute();
  }

  public void rollbackMasCreation(String pid) {
    context.deleteFrom(NEW_MACHINE_ANNOTATION_SERVICES)
        .where(NEW_MACHINE_ANNOTATION_SERVICES.ID.eq(removeProxy(pid)))
        .execute();
  }

  private Integer getTTL(MachineAnnotationService mas) {
    return mas.getOdsTimeToLive() == null ? 86400 : mas.getOdsTimeToLive();
  }

}
