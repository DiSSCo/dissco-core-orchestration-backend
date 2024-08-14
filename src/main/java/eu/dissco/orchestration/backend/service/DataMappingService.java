package eu.dissco.orchestration.backend.service;

import static eu.dissco.orchestration.backend.configuration.ApplicationConfiguration.HANDLE_PROXY;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiData;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiListWrapper;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.exception.PidAuthenticationException;
import eu.dissco.orchestration.backend.exception.PidCreationException;
import eu.dissco.orchestration.backend.exception.ProcessingFailedException;
import eu.dissco.orchestration.backend.properties.FdoProperties;
import eu.dissco.orchestration.backend.repository.DataMappingRepository;
import eu.dissco.orchestration.backend.schema.Agent;
import eu.dissco.orchestration.backend.schema.Agent.Type;
import eu.dissco.orchestration.backend.schema.DataMapping;
import eu.dissco.orchestration.backend.schema.DataMapping.OdsMappingDataStandard;
import eu.dissco.orchestration.backend.schema.DataMapping.OdsStatus;
import eu.dissco.orchestration.backend.schema.DataMappingRequest;
import eu.dissco.orchestration.backend.schema.DefaultMapping;
import eu.dissco.orchestration.backend.schema.FieldMapping;
import eu.dissco.orchestration.backend.utils.TombstoneUtils;
import eu.dissco.orchestration.backend.web.HandleComponent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataMappingService {

  private final FdoRecordService fdoRecordService;
  private final HandleComponent handleComponent;
  private final KafkaPublisherService kafkaPublisherService;
  private final DataMappingRepository repository;
  private final ObjectMapper mapper;
  private final FdoProperties fdoProperties;

  private static boolean isEqual(DataMapping dataMapping,
      DataMapping currentDataMapping) {
    return Objects.equals(dataMapping.getSchemaName(), currentDataMapping.getSchemaName()) &&
        Objects.equals(dataMapping.getSchemaDescription(),
            currentDataMapping.getSchemaDescription()) &&
        Objects.equals(dataMapping.getOdsDefaultMapping(),
            currentDataMapping.getOdsDefaultMapping()) &&
        Objects.equals(dataMapping.getOdsFieldMapping(), currentDataMapping.getOdsFieldMapping()) &&
        Objects.equals(dataMapping.getOdsMappingDataStandard(),
            currentDataMapping.getOdsMappingDataStandard());
  }

  private static List<DefaultMapping> buildDefaultMapping(
      DataMappingRequest dataMappingRequest) {
    var mappedList = new ArrayList<DefaultMapping>();
    for (var odsDefaultMapping : dataMappingRequest.getOdsDefaultMapping()) {
      var mappedOdsDefaultMapping = new DefaultMapping();
      for (var property : odsDefaultMapping.getAdditionalProperties()
          .entrySet()) {
        mappedOdsDefaultMapping.setAdditionalProperty(property.getKey(), property.getValue());
      }
      mappedList.add(mappedOdsDefaultMapping);
    }
    return mappedList;
  }

  private static List<FieldMapping> buildFieldMapping(
      DataMappingRequest dataMappingRequest) {
    var mappedList = new ArrayList<FieldMapping>();
    for (var odsDefaultMapping : dataMappingRequest.getOdsFieldMapping()) {
      var mappedOdsFieldMapping = new FieldMapping();
      for (var property : odsDefaultMapping.getAdditionalProperties()
          .entrySet()) {
        mappedOdsFieldMapping.setAdditionalProperty(property.getKey(), property.getValue());
      }
      mappedList.add(mappedOdsFieldMapping);
    }
    return mappedList;
  }

  public JsonApiWrapper createDataMapping(DataMappingRequest mappingRequest, String userId,
      String path)
      throws ProcessingFailedException {
    var requestBody = fdoRecordService.buildCreateRequest(mappingRequest, ObjectType.DATA_MAPPING);
    String handle = null;
    try {
      handle = handleComponent.postHandle(requestBody);
    } catch (PidAuthenticationException | PidCreationException e) {
      throw new ProcessingFailedException(e.getMessage(), e);
    }
    var dataMapping = buildDataMapping(mappingRequest, 1, userId, handle,
        Date.from(Instant.now()));
    repository.createDataMapping(dataMapping);
    publishCreateEvent(dataMapping);
    return wrapSingleResponse(dataMapping, path);
  }

  private DataMapping buildDataMapping(DataMappingRequest dataMappingRequest, int version,
      String userId, String handle, Date created) {
    var id = HANDLE_PROXY + handle;
    return new DataMapping()
        .withId(id)
        .withOdsID(id)
        .withType(ObjectType.DATA_MAPPING.getFullName())
        .withOdsType(fdoProperties.getDataMappingType())
        .withSchemaVersion(version)
        .withOdsStatus(OdsStatus.ODS_ACTIVE)
        .withSchemaName(dataMappingRequest.getSchemaName())
        .withSchemaDescription(dataMappingRequest.getSchemaDescription())
        .withSchemaDateCreated(created)
        .withSchemaDateModified(Date.from(Instant.now()))
        .withSchemaCreator(new Agent().withId(userId).withType(Type.SCHEMA_PERSON))
        .withOdsDefaultMapping(buildDefaultMapping(dataMappingRequest))
        .withOdsFieldMapping(buildFieldMapping(dataMappingRequest))
        .withOdsMappingDataStandard(OdsMappingDataStandard.fromValue(
            dataMappingRequest.getOdsMappingDataStandard().value()));
  }

  private void publishCreateEvent(DataMapping dataMapping)
      throws ProcessingFailedException {
    try {
      kafkaPublisherService.publishCreateEvent(mapper.valueToTree(dataMapping));
    } catch (JsonProcessingException e) {
      log.error("Unable to publish message to Kafka", e);
      rollbackMappingCreation(dataMapping);
      throw new ProcessingFailedException("Failed to create new machine annotation service", e);
    }
  }

  private void rollbackMappingCreation(DataMapping dataMapping) {
    var request = fdoRecordService.buildRollbackCreateRequest(dataMapping.getId());
    try {
      handleComponent.rollbackHandleCreation(request);
    } catch (PidAuthenticationException | PidCreationException e) {
      log.error(
          "Unable to rollback handle creation for data mapping. Manually delete the following handle: {}. Cause of error: ",
          dataMapping.getId(), e);
    }
    repository.rollbackDataMappingCreation(dataMapping.getId());
  }

  public JsonApiWrapper updateDataMapping(String id, DataMappingRequest dataMappingRequest,
      String userId, String path) throws NotFoundException, ProcessingFailedException {
    var currentDataMappingOptional = repository.getActiveDataMapping(id);
    if (currentDataMappingOptional.isEmpty()) {
      throw new NotFoundException("Requested data mapping does not exist");
    }
    var currentDataMapping = currentDataMappingOptional.get();
    var dataMapping = buildDataMapping(dataMappingRequest,
        currentDataMapping.getSchemaVersion() + 1,
        userId, id, currentDataMapping.getSchemaDateCreated());
    if (isEqual(dataMapping, currentDataMapping)) {
      return null;
    } else {
      repository.updateDataMapping(dataMapping);
      publishUpdateEvent(dataMapping, currentDataMappingOptional.get());
      return wrapSingleResponse(dataMapping, path);
    }
  }

  private void publishUpdateEvent(DataMapping dataMapping,
      DataMapping currentDataMapping) throws ProcessingFailedException {
    try {
      kafkaPublisherService.publishUpdateEvent(mapper.valueToTree(dataMapping),
          mapper.valueToTree(currentDataMapping));
    } catch (JsonProcessingException e) {
      log.error("Unable to publish message to Kafka", e);
      rollbackToPreviousVersion(currentDataMapping);
      throw new ProcessingFailedException("Failed to create new machine annotation service", e);
    }
  }

  private void rollbackToPreviousVersion(DataMapping currentDataMapping) {
    repository.updateDataMapping(currentDataMapping);
  }

  public void deleteDataMapping(String id, String userId) throws NotFoundException {
    var result = repository.getActiveDataMapping(id);
    if (result.isPresent()) {
      var dataMapping = result.get();
      dataMapping.setOdsStatus(OdsStatus.ODS_TOMBSTONE);
      dataMapping.setOdsTombstoneMetadata(TombstoneUtils.buildTombstoneMetadata(userId,
          "Data Mapping tombstoned by the user through the orchestration backend"));
      repository.deleteDataMapping(id, dataMapping.getOdsTombstoneMetadata().getOdsTombstoneDate());
    } else {
      throw new NotFoundException("Requested data mapping " + id + " does not exist");
    }
  }

  protected Optional<DataMapping> getActiveDataMapping(String id) {
    return repository.getActiveDataMapping(id);
  }

  public JsonApiListWrapper getDataMappings(int pageNum, int pageSize, String path) {
    var dataMappings = repository.getDataMappings(pageNum, pageSize);
    return wrapResponse(dataMappings, pageNum, pageSize, path);
  }

  public JsonApiWrapper getDataMappingById(String id, String path) {
    var dataMapping = repository.getDataMapping(id);
    return wrapSingleResponse(dataMapping, path);
  }

  private JsonApiWrapper wrapSingleResponse(DataMapping dataMapping, String path) {
    return new JsonApiWrapper(
        new JsonApiData(dataMapping.getId(), ObjectType.DATA_MAPPING, flattenDataMapping(dataMapping)),
        new JsonApiLinks(path)
    );
  }

  private JsonApiListWrapper wrapResponse(List<DataMapping> dataMappings, int pageNum,
      int pageSize, String path) {
    boolean hasNext = dataMappings.size() > pageSize;
    dataMappings = hasNext ? dataMappings.subList(0, pageSize) : dataMappings;
    var linksNode = new JsonApiLinks(pageSize, pageNum, hasNext, path);
    var dataNode = wrapData(dataMappings);
    return new JsonApiListWrapper(dataNode, linksNode);
  }

  private List<JsonApiData> wrapData(List<DataMapping> dataMappings) {
    return dataMappings.stream()
        .map(r -> new JsonApiData(r.getId(), ObjectType.DATA_MAPPING, flattenDataMapping(r)))
        .toList();
  }

  private JsonNode flattenDataMapping(DataMapping dataMapping) {
    return mapper.valueToTree(dataMapping);
  }

}
