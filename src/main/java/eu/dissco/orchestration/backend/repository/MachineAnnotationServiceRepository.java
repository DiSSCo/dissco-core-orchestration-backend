package eu.dissco.orchestration.backend.repository;

import static eu.dissco.orchestration.backend.database.jooq.Tables.MACHINE_ANNOTATION_SERVICES;
import static eu.dissco.orchestration.backend.repository.RepositoryUtils.getOffset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.orchestration.backend.database.jooq.tables.records.MachineAnnotationServicesRecord;
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
    context.insertInto(MACHINE_ANNOTATION_SERVICES)
        .set(MACHINE_ANNOTATION_SERVICES.ID, masRecord.id())
        .set(MACHINE_ANNOTATION_SERVICES.VERSION, masRecord.version())
        .set(MACHINE_ANNOTATION_SERVICES.NAME, masRecord.mas().getName())
        .set(MACHINE_ANNOTATION_SERVICES.CREATED, masRecord.created())
        .set(MACHINE_ANNOTATION_SERVICES.ADMINISTRATOR, masRecord.administrator())
        .set(MACHINE_ANNOTATION_SERVICES.CONTAINER_IMAGE, masRecord.mas().getContainerImage())
        .set(MACHINE_ANNOTATION_SERVICES.CONTAINER_IMAGE_TAG, masRecord.mas().getContainerTag())
        .set(MACHINE_ANNOTATION_SERVICES.TARGET_DIGITAL_OBJECT_FILTERS,
            masRecord.mas().getTargetDigitalObjectFilters() != null ? JSONB.jsonb(
                masRecord.mas().getTargetDigitalObjectFilters().toString()) : null
        )
        .set(MACHINE_ANNOTATION_SERVICES.SERVICE_DESCRIPTION,
            masRecord.mas().getServiceDescription())
        .set(MACHINE_ANNOTATION_SERVICES.SERVICE_STATE, masRecord.mas().getServiceState())
        .set(MACHINE_ANNOTATION_SERVICES.SERVICE_AVAILABILITY,
            masRecord.mas().getServiceAvailability())
        .set(MACHINE_ANNOTATION_SERVICES.SOURCE_CODE_REPOSITORY,
            masRecord.mas().getSourceCodeRepository())
        .set(MACHINE_ANNOTATION_SERVICES.CODE_MAINTAINER, masRecord.mas().getCodeMaintainer())
        .set(MACHINE_ANNOTATION_SERVICES.CODE_LICENSE, masRecord.mas().getCodeLicense())
        .set(MACHINE_ANNOTATION_SERVICES.DEPENDENCIES,
            masRecord.mas().getDependencies() != null ? masRecord.mas().getDependencies()
                .toArray(new String[0]) : null
        )
        .set(MACHINE_ANNOTATION_SERVICES.SUPPORT_CONTACT, masRecord.mas().getSupportContact())
        .set(MACHINE_ANNOTATION_SERVICES.SLA_DOCUMENTATION, masRecord.mas().getSlaDocumentation())
        .set(MACHINE_ANNOTATION_SERVICES.TOPICNAME, masRecord.mas().getTopicName())
        .set(MACHINE_ANNOTATION_SERVICES.MAXREPLICAS, masRecord.mas().getMaxReplicas())
        .set(MACHINE_ANNOTATION_SERVICES.DELETED_ON, masRecord.deleted())
        .execute();
  }

  public Optional<MachineAnnotationServiceRecord> getActiveMachineAnnotationService(String id) {
    return context.selectFrom(MACHINE_ANNOTATION_SERVICES)
        .where(MACHINE_ANNOTATION_SERVICES.ID.eq(id))
        .and(MACHINE_ANNOTATION_SERVICES.DELETED_ON.isNull())
        .fetchOptional(this::mapToMasRecord);
  }

  private MachineAnnotationServiceRecord mapToMasRecord(
      MachineAnnotationServicesRecord machineAnnotationServicesRecord) {
    return new MachineAnnotationServiceRecord(
        machineAnnotationServicesRecord.getId(),
        machineAnnotationServicesRecord.getVersion(),
        machineAnnotationServicesRecord.getCreated(),
        machineAnnotationServicesRecord.getAdministrator(),
        new MachineAnnotationService(
            machineAnnotationServicesRecord.getName(),
            machineAnnotationServicesRecord.getContainerImage(),
            machineAnnotationServicesRecord.getContainerImageTag(),
            mapToJson(machineAnnotationServicesRecord.getTargetDigitalObjectFilters()),
            machineAnnotationServicesRecord.getServiceDescription(),
            machineAnnotationServicesRecord.getServiceState(),
            machineAnnotationServicesRecord.getSourceCodeRepository(),
            machineAnnotationServicesRecord.getServiceAvailability(),
            machineAnnotationServicesRecord.getCodeMaintainer(),
            machineAnnotationServicesRecord.getCodeLicense(),
            machineAnnotationServicesRecord.getDependencies() != null ? Arrays.stream(
                machineAnnotationServicesRecord.getDependencies()).toList() : null,
            machineAnnotationServicesRecord.getSupportContact(),
            machineAnnotationServicesRecord.getSlaDocumentation(),
            machineAnnotationServicesRecord.getTopicname(),
            machineAnnotationServicesRecord.getMaxreplicas()
        ),
        machineAnnotationServicesRecord.getDeletedOn()
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
    context.update(MACHINE_ANNOTATION_SERVICES)
        .set(MACHINE_ANNOTATION_SERVICES.DELETED_ON, deleted)
        .where(MACHINE_ANNOTATION_SERVICES.ID.eq(id))
        .execute();
  }

  public MachineAnnotationServiceRecord getMachineAnnotationService(String id) {
    return context.selectFrom(MACHINE_ANNOTATION_SERVICES)
        .where(MACHINE_ANNOTATION_SERVICES.ID.eq(id))
        .fetchOne(this::mapToMasRecord);
  }

  public List<MachineAnnotationServiceRecord> getMachineAnnotationServices(int pageNum,
      int pageSize) {
    int offset = getOffset(pageNum, pageSize);
    return context.selectFrom(MACHINE_ANNOTATION_SERVICES)
        .where(MACHINE_ANNOTATION_SERVICES.DELETED_ON.isNull())
        .limit(pageSize + 1)
        .offset(offset)
        .fetch(this::mapToMasRecord);
  }

  public void updateMachineAnnotationService(MachineAnnotationServiceRecord masRecord) {
    context.update(MACHINE_ANNOTATION_SERVICES)
        .set(MACHINE_ANNOTATION_SERVICES.VERSION, masRecord.version())
        .set(MACHINE_ANNOTATION_SERVICES.NAME, masRecord.mas().getName())
        .set(MACHINE_ANNOTATION_SERVICES.CREATED, masRecord.created())
        .set(MACHINE_ANNOTATION_SERVICES.ADMINISTRATOR, masRecord.administrator())
        .set(MACHINE_ANNOTATION_SERVICES.CONTAINER_IMAGE, masRecord.mas().getContainerImage())
        .set(MACHINE_ANNOTATION_SERVICES.CONTAINER_IMAGE_TAG, masRecord.mas().getContainerTag())
        .set(MACHINE_ANNOTATION_SERVICES.TARGET_DIGITAL_OBJECT_FILTERS,
            masRecord.mas().getTargetDigitalObjectFilters() != null ? JSONB.jsonb(
                masRecord.mas().getTargetDigitalObjectFilters().toString()) : null
        )
        .set(MACHINE_ANNOTATION_SERVICES.SERVICE_DESCRIPTION,
            masRecord.mas().getServiceDescription())
        .set(MACHINE_ANNOTATION_SERVICES.SERVICE_STATE, masRecord.mas().getServiceState())
        .set(MACHINE_ANNOTATION_SERVICES.SERVICE_AVAILABILITY,
            masRecord.mas().getServiceAvailability())
        .set(MACHINE_ANNOTATION_SERVICES.SOURCE_CODE_REPOSITORY,
            masRecord.mas().getSourceCodeRepository())
        .set(MACHINE_ANNOTATION_SERVICES.CODE_MAINTAINER, masRecord.mas().getCodeMaintainer())
        .set(MACHINE_ANNOTATION_SERVICES.CODE_LICENSE, masRecord.mas().getCodeLicense())
        .set(MACHINE_ANNOTATION_SERVICES.DEPENDENCIES,
            masRecord.mas().getDependencies() != null ? masRecord.mas().getDependencies()
                .toArray(new String[0]) : null
        )
        .set(MACHINE_ANNOTATION_SERVICES.SUPPORT_CONTACT, masRecord.mas().getSupportContact())
        .set(MACHINE_ANNOTATION_SERVICES.SLA_DOCUMENTATION, masRecord.mas().getSlaDocumentation())
        .set(MACHINE_ANNOTATION_SERVICES.TOPICNAME, masRecord.mas().getTopicName())
        .set(MACHINE_ANNOTATION_SERVICES.MAXREPLICAS, masRecord.mas().getMaxReplicas())
        .set(MACHINE_ANNOTATION_SERVICES.DELETED_ON, masRecord.deleted())
        .where(MACHINE_ANNOTATION_SERVICES.ID.eq(masRecord.id()))
        .execute();
  }

  public void rollbackMasCreation(String pid) {
    context.deleteFrom(MACHINE_ANNOTATION_SERVICES)
        .where(MACHINE_ANNOTATION_SERVICES.ID.eq(pid))
        .execute();
  }
}
