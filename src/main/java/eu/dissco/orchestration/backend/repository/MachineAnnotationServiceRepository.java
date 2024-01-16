package eu.dissco.orchestration.backend.repository;

import static eu.dissco.orchestration.backend.database.jooq.Tables.MACHINE_ANNOTATION_SERVICES_TMP;
import static eu.dissco.orchestration.backend.repository.RepositoryUtils.getOffset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.orchestration.backend.database.jooq.tables.records.MachineAnnotationServicesTmpRecord;
import eu.dissco.orchestration.backend.domain.MachineAnnotationService;
import eu.dissco.orchestration.backend.domain.MachineAnnotationServiceRecord;
import eu.dissco.orchestration.backend.exception.DisscoJsonBMappingException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MachineAnnotationServiceRepository {

  private final DSLContext context;
  private final ObjectMapper mapper;

  public void createMachineAnnotationService(MachineAnnotationServiceRecord masRecord) {
    context.insertInto(MACHINE_ANNOTATION_SERVICES_TMP)
        .set(MACHINE_ANNOTATION_SERVICES_TMP.ID, masRecord.id())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.VERSION, masRecord.version())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.NAME, masRecord.mas().getName())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.CREATED, masRecord.created())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.ADMINISTRATOR, masRecord.administrator())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.CONTAINER_IMAGE, masRecord.mas().getContainerImage())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.CONTAINER_IMAGE_TAG, masRecord.mas().getContainerTag())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.TARGET_DIGITAL_OBJECT_FILTERS,
            masRecord.mas().getTargetDigitalObjectFilters() != null ? JSONB.jsonb(
                masRecord.mas().getTargetDigitalObjectFilters().toString()) : null
        )
        .set(MACHINE_ANNOTATION_SERVICES_TMP.SERVICE_DESCRIPTION,
            masRecord.mas().getServiceDescription())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.SERVICE_STATE, masRecord.mas().getServiceState())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.SERVICE_AVAILABILITY,
            masRecord.mas().getServiceAvailability())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.SOURCE_CODE_REPOSITORY,
            masRecord.mas().getSourceCodeRepository())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.CODE_MAINTAINER, masRecord.mas().getCodeMaintainer())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.CODE_LICENSE, masRecord.mas().getCodeLicense())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.DEPENDENCIES,
            masRecord.mas().getDependencies() != null ? masRecord.mas().getDependencies()
                .toArray(new String[0]) : null
        )
        .set(MACHINE_ANNOTATION_SERVICES_TMP.SUPPORT_CONTACT, masRecord.mas().getSupportContact())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.SLA_DOCUMENTATION, masRecord.mas().getSlaDocumentation())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.TOPICNAME, masRecord.mas().getTopicName())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.MAXREPLICAS, masRecord.mas().getMaxReplicas())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.DELETED_ON, masRecord.deleted())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.BATCHING_PERMITTED, masRecord.mas().isBatchingPermitted())
        .execute();
  }

  public Optional<MachineAnnotationServiceRecord> getActiveMachineAnnotationService(String id) {
    return context.selectFrom(MACHINE_ANNOTATION_SERVICES_TMP)
        .where(MACHINE_ANNOTATION_SERVICES_TMP.ID.eq(id))
        .and(MACHINE_ANNOTATION_SERVICES_TMP.DELETED_ON.isNull())
        .fetchOptional(this::mapToMasRecord);
  }

  private MachineAnnotationServiceRecord mapToMasRecord(
      MachineAnnotationServicesTmpRecord machineAnnotationServicesTmpRecord) {
    return new MachineAnnotationServiceRecord(
        machineAnnotationServicesTmpRecord.getId(),
        machineAnnotationServicesTmpRecord.getVersion(),
        machineAnnotationServicesTmpRecord.getCreated(),
        machineAnnotationServicesTmpRecord.getAdministrator(),
        new MachineAnnotationService(
            machineAnnotationServicesTmpRecord.getName(),
            machineAnnotationServicesTmpRecord.getContainerImage(),
            machineAnnotationServicesTmpRecord.getContainerImageTag(),
            mapToJson(machineAnnotationServicesTmpRecord.getTargetDigitalObjectFilters()),
            machineAnnotationServicesTmpRecord.getServiceDescription(),
            machineAnnotationServicesTmpRecord.getServiceState(),
            machineAnnotationServicesTmpRecord.getSourceCodeRepository(),
            machineAnnotationServicesTmpRecord.getServiceAvailability(),
            machineAnnotationServicesTmpRecord.getCodeMaintainer(),
            machineAnnotationServicesTmpRecord.getCodeLicense(),
            machineAnnotationServicesTmpRecord.getDependencies() != null ? Arrays.stream(
                machineAnnotationServicesTmpRecord.getDependencies()).toList() : null,
            machineAnnotationServicesTmpRecord.getSupportContact(),
            machineAnnotationServicesTmpRecord.getSlaDocumentation(),
            machineAnnotationServicesTmpRecord.getTopicname(),
            machineAnnotationServicesTmpRecord.getMaxreplicas(),
            machineAnnotationServicesTmpRecord.getBatchingPermitted()
        ),
        machineAnnotationServicesTmpRecord.getDeletedOn()
    );
  }

  private JsonNode mapToJson(JSONB jsonb) {
    try {
      if (jsonb != null) {
        return mapper.readTree(jsonb.data());
      }
    } catch (JsonProcessingException e) {
      throw new DisscoJsonBMappingException("Failed to parse jsonb field to json: " + jsonb.data(),
          e);
    }
    return null;
  }

  public void deleteMachineAnnotationService(String id, Instant deleted) {
    context.update(MACHINE_ANNOTATION_SERVICES_TMP)
        .set(MACHINE_ANNOTATION_SERVICES_TMP.DELETED_ON, deleted)
        .where(MACHINE_ANNOTATION_SERVICES_TMP.ID.eq(id))
        .execute();
  }

  public MachineAnnotationServiceRecord getMachineAnnotationService(String id) {
    return context.selectFrom(MACHINE_ANNOTATION_SERVICES_TMP)
        .where(MACHINE_ANNOTATION_SERVICES_TMP.ID.eq(id))
        .fetchOne(this::mapToMasRecord);
  }

  public List<MachineAnnotationServiceRecord> getMachineAnnotationServices(int pageNum,
      int pageSize) {
    int offset = getOffset(pageNum, pageSize);
    return context.selectFrom(MACHINE_ANNOTATION_SERVICES_TMP)
        .where(MACHINE_ANNOTATION_SERVICES_TMP.DELETED_ON.isNull())
        .limit(pageSize + 1)
        .offset(offset)
        .fetch(this::mapToMasRecord);
  }

  public void updateMachineAnnotationService(MachineAnnotationServiceRecord masRecord) {
    context.update(MACHINE_ANNOTATION_SERVICES_TMP)
        .set(MACHINE_ANNOTATION_SERVICES_TMP.VERSION, masRecord.version())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.NAME, masRecord.mas().getName())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.CREATED, masRecord.created())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.ADMINISTRATOR, masRecord.administrator())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.CONTAINER_IMAGE, masRecord.mas().getContainerImage())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.CONTAINER_IMAGE_TAG, masRecord.mas().getContainerTag())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.TARGET_DIGITAL_OBJECT_FILTERS,
            masRecord.mas().getTargetDigitalObjectFilters() != null ? JSONB.jsonb(
                masRecord.mas().getTargetDigitalObjectFilters().toString()) : null
        )
        .set(MACHINE_ANNOTATION_SERVICES_TMP.SERVICE_DESCRIPTION,
            masRecord.mas().getServiceDescription())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.SERVICE_STATE, masRecord.mas().getServiceState())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.SERVICE_AVAILABILITY,
            masRecord.mas().getServiceAvailability())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.SOURCE_CODE_REPOSITORY,
            masRecord.mas().getSourceCodeRepository())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.CODE_MAINTAINER, masRecord.mas().getCodeMaintainer())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.CODE_LICENSE, masRecord.mas().getCodeLicense())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.DEPENDENCIES,
            masRecord.mas().getDependencies() != null ? masRecord.mas().getDependencies()
                .toArray(new String[0]) : null
        )
        .set(MACHINE_ANNOTATION_SERVICES_TMP.SUPPORT_CONTACT, masRecord.mas().getSupportContact())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.SLA_DOCUMENTATION, masRecord.mas().getSlaDocumentation())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.TOPICNAME, masRecord.mas().getTopicName())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.MAXREPLICAS, masRecord.mas().getMaxReplicas())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.DELETED_ON, masRecord.deleted())
        .set(MACHINE_ANNOTATION_SERVICES_TMP.BATCHING_PERMITTED, masRecord.mas().isBatchingPermitted())
        .where(MACHINE_ANNOTATION_SERVICES_TMP.ID.eq(masRecord.id()))
        .execute();
  }

  public void rollbackMasCreation(String pid) {
    context.deleteFrom(MACHINE_ANNOTATION_SERVICES_TMP)
        .where(MACHINE_ANNOTATION_SERVICES_TMP.ID.eq(pid))
        .execute();
  }
}
