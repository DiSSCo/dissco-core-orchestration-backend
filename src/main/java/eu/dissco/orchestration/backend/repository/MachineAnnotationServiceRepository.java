package eu.dissco.orchestration.backend.repository;

import static eu.dissco.orchestration.backend.database.jooq.Tables.MACHINE_ANNOTATION_SERVICE;
import static eu.dissco.orchestration.backend.repository.RepositoryUtils.getOffset;
import static eu.dissco.orchestration.backend.utils.HandleUtils.removeProxy;

import eu.dissco.orchestration.backend.schema.Agent;
import eu.dissco.orchestration.backend.schema.MachineAnnotationService;
import eu.dissco.orchestration.backend.utils.HandleUtils;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.Record1;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
@RequiredArgsConstructor
public class MachineAnnotationServiceRepository {

  private final DSLContext context;
  private final JsonMapper mapper;

  public void createMachineAnnotationService(MachineAnnotationService mas) {
    mas.setOdsTimeToLive(getTTL(mas));
    context.insertInto(MACHINE_ANNOTATION_SERVICE)
        .set(MACHINE_ANNOTATION_SERVICE.ID, removeProxy(mas.getId()))
        .set(MACHINE_ANNOTATION_SERVICE.VERSION, mas.getSchemaVersion())
        .set(MACHINE_ANNOTATION_SERVICE.NAME, mas.getSchemaName())
        .set(MACHINE_ANNOTATION_SERVICE.CREATED, mas.getSchemaDateCreated().toInstant())
        .set(MACHINE_ANNOTATION_SERVICE.MODIFIED, mas.getSchemaDateModified().toInstant())
        .set(MACHINE_ANNOTATION_SERVICE.CREATOR, mas.getSchemaCreator().getId())
        .set(MACHINE_ANNOTATION_SERVICE.CONTAINER_IMAGE, mas.getOdsContainerImage())
        .set(MACHINE_ANNOTATION_SERVICE.CONTAINER_IMAGE_TAG, mas.getOdsContainerTag())
        .set(MACHINE_ANNOTATION_SERVICE.CREATIVE_WORK_STATE,
            mas.getSchemaCreativeWorkStatus())
        .set(MACHINE_ANNOTATION_SERVICE.SERVICE_AVAILABILITY,
            mas.getOdsServiceAvailability())
        .set(MACHINE_ANNOTATION_SERVICE.SOURCE_CODE_REPOSITORY,
            mas.getSchemaCodeRepository())
        .set(MACHINE_ANNOTATION_SERVICE.CODE_MAINTAINER,
            getSchemaMaintainerId(mas.getSchemaMaintainer()))
        .set(MACHINE_ANNOTATION_SERVICE.CODE_LICENSE, mas.getSchemaLicense())
        .set(MACHINE_ANNOTATION_SERVICE.BATCHING_PERMITTED, mas.getOdsBatchingPermitted())
        .set(MACHINE_ANNOTATION_SERVICE.TIME_TO_LIVE, mas.getOdsTimeToLive())
        .set(MACHINE_ANNOTATION_SERVICE.DATA, mapToJSONB(mas))
        .execute();
  }

  private JSONB mapToJSONB(MachineAnnotationService mas) {
    return JSONB.valueOf(mapper.writeValueAsString(mas));
  }

  public Optional<MachineAnnotationService> getActiveMachineAnnotationService(String id) {
    return context.select(MACHINE_ANNOTATION_SERVICE.DATA)
        .from(MACHINE_ANNOTATION_SERVICE)
        .where(MACHINE_ANNOTATION_SERVICE.ID.eq(removeProxy(id)))
        .and(MACHINE_ANNOTATION_SERVICE.TOMBSTONED.isNull())
        .fetchOptional(this::mapToMas);
  }

  public List<MachineAnnotationService> getActiveMachineAnnotationServices(Set<String> ids) {
    return context.select(MACHINE_ANNOTATION_SERVICE.DATA)
        .from(MACHINE_ANNOTATION_SERVICE)
        .where(
            MACHINE_ANNOTATION_SERVICE.ID.in(ids.stream().map(HandleUtils::removeProxy).toList()))
        .and(MACHINE_ANNOTATION_SERVICE.TOMBSTONED.isNull())
        .fetch(this::mapToMas);
  }

  private MachineAnnotationService mapToMas(Record1<JSONB> record1) {
      return mapper.readValue(record1.get(MACHINE_ANNOTATION_SERVICE.DATA).data(),
          MachineAnnotationService.class);
  }

  public void tombstoneMachineAnnotationService(MachineAnnotationService tombstoneMas,
      Instant timestamp) {
    context.update(MACHINE_ANNOTATION_SERVICE)
        .set(MACHINE_ANNOTATION_SERVICE.TOMBSTONED, timestamp)
        .set(MACHINE_ANNOTATION_SERVICE.MODIFIED, timestamp)
        .set(MACHINE_ANNOTATION_SERVICE.VERSION, tombstoneMas.getSchemaVersion())
        .set(MACHINE_ANNOTATION_SERVICE.DATA, mapToJSONB(tombstoneMas))
        .where(MACHINE_ANNOTATION_SERVICE.ID.eq(removeProxy(tombstoneMas.getId())))
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
        .where(MACHINE_ANNOTATION_SERVICE.TOMBSTONED.isNull())
        .limit(pageSize + 1)
        .offset(offset)
        .fetch(this::mapToMas);
  }

  public void updateMachineAnnotationService(MachineAnnotationService mas) {
    mas.setOdsTimeToLive(getTTL(mas));
    context.update(MACHINE_ANNOTATION_SERVICE)
        .set(MACHINE_ANNOTATION_SERVICE.VERSION, mas.getSchemaVersion())
        .set(MACHINE_ANNOTATION_SERVICE.NAME, mas.getSchemaName())
        .set(MACHINE_ANNOTATION_SERVICE.CREATED, mas.getSchemaDateCreated().toInstant())
        .set(MACHINE_ANNOTATION_SERVICE.MODIFIED, mas.getSchemaDateModified().toInstant())
        .set(MACHINE_ANNOTATION_SERVICE.CREATOR, mas.getSchemaCreator().getId())
        .set(MACHINE_ANNOTATION_SERVICE.CONTAINER_IMAGE, mas.getOdsContainerImage())
        .set(MACHINE_ANNOTATION_SERVICE.CONTAINER_IMAGE_TAG, mas.getOdsContainerTag())
        .set(MACHINE_ANNOTATION_SERVICE.CREATIVE_WORK_STATE,
            mas.getSchemaCreativeWorkStatus())
        .set(MACHINE_ANNOTATION_SERVICE.SERVICE_AVAILABILITY,
            mas.getOdsServiceAvailability())
        .set(MACHINE_ANNOTATION_SERVICE.SOURCE_CODE_REPOSITORY,
            mas.getSchemaCodeRepository())
        .set(MACHINE_ANNOTATION_SERVICE.CODE_MAINTAINER,
            getSchemaMaintainerId(mas.getSchemaMaintainer()))
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

  private static Integer getTTL(MachineAnnotationService mas) {
    return mas.getOdsTimeToLive() == null ? 86400 : mas.getOdsTimeToLive();
  }

  private static String getSchemaMaintainerId(Agent schemaMaintainer) {
    if (schemaMaintainer == null) {
      return null;
    } else {
      return schemaMaintainer.getId();
    }
  }

}
