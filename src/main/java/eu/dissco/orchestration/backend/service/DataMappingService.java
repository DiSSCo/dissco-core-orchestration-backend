package eu.dissco.orchestration.backend.service;

import static eu.dissco.orchestration.backend.configuration.ApplicationConfiguration.HANDLE_PROXY;
import static eu.dissco.orchestration.backend.utils.TombstoneUtils.buildTombstoneMetadata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiData;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiListWrapper;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.exception.PidException;
import eu.dissco.orchestration.backend.exception.ProcessingFailedException;
import eu.dissco.orchestration.backend.properties.FdoProperties;
import eu.dissco.orchestration.backend.repository.DataMappingRepository;
import eu.dissco.orchestration.backend.schema.Agent;
import eu.dissco.orchestration.backend.schema.DataMapping;
import eu.dissco.orchestration.backend.schema.DataMapping.OdsMappingDataStandard;
import eu.dissco.orchestration.backend.schema.DataMapping.OdsStatus;
import eu.dissco.orchestration.backend.schema.DataMappingRequest;
import eu.dissco.orchestration.backend.schema.DefaultMapping;
import eu.dissco.orchestration.backend.schema.TermMapping;
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
  private final RabbitMqPublisherService rabbitMqPublisherService;
  private final DataMappingRepository repository;
  private final ObjectMapper mapper;
  private final FdoProperties fdoProperties;

  private static boolean isEqual(DataMapping dataMapping,
      DataMapping currentDataMapping) {
    return Objects.equals(dataMapping.getSchemaName(), currentDataMapping.getSchemaName()) &&
        Objects.equals(dataMapping.getSchemaDescription(),
            currentDataMapping.getSchemaDescription()) &&
        Objects.equals(dataMapping.getOdsHasDefaultMapping(),
            currentDataMapping.getOdsHasDefaultMapping()) &&
        Objects.equals(dataMapping.getOdsHasTermMapping(),
            currentDataMapping.getOdsHasTermMapping()) &&
        Objects.equals(dataMapping.getOdsMappingDataStandard(),
            currentDataMapping.getOdsMappingDataStandard());
  }

  private static List<DefaultMapping> buildDefaultMapping(
      DataMappingRequest dataMappingRequest) {
    var mappedList = new ArrayList<DefaultMapping>();
    for (var odsDefaultMapping : dataMappingRequest.getOdsHasDefaultMapping()) {
      var mappedOdsDefaultMapping = new DefaultMapping();
      for (var property : odsDefaultMapping.getAdditionalProperties()
          .entrySet()) {
        mappedOdsDefaultMapping.setAdditionalProperty(property.getKey(), property.getValue());
      }
      mappedList.add(mappedOdsDefaultMapping);
    }
    return mappedList;
  }

  private static List<TermMapping> buildTermMapping(DataMappingRequest dataMappingRequest) {
    var mappedList = new ArrayList<TermMapping>();
    for (var odsDefaultMapping : dataMappingRequest.getOdsHasTermMapping()) {
      var mappedOdsTermMapping = new TermMapping();
      for (var property : odsDefaultMapping.getAdditionalProperties()
          .entrySet()) {
        mappedOdsTermMapping.setAdditionalProperty(property.getKey(), property.getValue());
      }
      mappedList.add(mappedOdsTermMapping);
    }
    return mappedList;
  }

  private static DataMapping buildTombstoneDataMapping(DataMapping dataMapping,
      Agent tombstoningAgent, Instant timestamp) {
    return new DataMapping()
        .withId(dataMapping.getId())
        .withType(dataMapping.getType())
        .withSchemaIdentifier(dataMapping.getSchemaIdentifier())
        .withOdsFdoType(dataMapping.getOdsFdoType())
        .withOdsStatus(OdsStatus.TOMBSTONE)
        .withSchemaVersion(dataMapping.getSchemaVersion() + 1)
        .withSchemaName(dataMapping.getSchemaName())
        .withSchemaDescription(dataMapping.getSchemaDescription())
        .withSchemaDateCreated(dataMapping.getSchemaDateCreated())
        .withSchemaDateModified(Date.from(timestamp))
        .withSchemaCreator(dataMapping.getSchemaCreator())
        .withOdsHasDefaultMapping(dataMapping.getOdsHasDefaultMapping())
        .withOdsHasTermMapping(dataMapping.getOdsHasTermMapping())
        .withOdsMappingDataStandard(dataMapping.getOdsMappingDataStandard())
        .withOdsHasTombstoneMetadata(buildTombstoneMetadata(tombstoningAgent,
            "Data Mapping tombstoned by agent through the orchestration backend", timestamp));

  }

  public JsonApiWrapper createDataMapping(DataMappingRequest mappingRequest, Agent agent,
      String path)
      throws ProcessingFailedException {
    var requestBody = fdoRecordService.buildCreateRequest(mappingRequest, ObjectType.DATA_MAPPING);
    String handle = null;
    try {
      handle = handleComponent.postHandle2(requestBody);
    } catch (PidException e) {
      throw new ProcessingFailedException(e.getMessage(), e);
    }
    var dataMapping = buildDataMapping(mappingRequest, 1, agent, handle,
        Date.from(Instant.now()));
    repository.createDataMapping(dataMapping);
    publishCreateEvent(dataMapping, agent);
    return wrapSingleResponse(dataMapping, path);
  }

  private DataMapping buildDataMapping(DataMappingRequest dataMappingRequest, int version,
      Agent agent, String handle, Date created) {
    var id = HANDLE_PROXY + handle;
    return new DataMapping()
        .withId(id)
        .withSchemaIdentifier(id)
        .withType(ObjectType.DATA_MAPPING.getFullName())
        .withOdsFdoType(fdoProperties.getDataMappingType())
        .withSchemaVersion(version)
        .withOdsStatus(OdsStatus.ACTIVE)
        .withSchemaName(dataMappingRequest.getSchemaName())
        .withSchemaDescription(dataMappingRequest.getSchemaDescription())
        .withSchemaDateCreated(created)
        .withSchemaDateModified(Date.from(Instant.now()))
        .withSchemaCreator(agent)
        .withOdsHasDefaultMapping(buildDefaultMapping(dataMappingRequest))
        .withOdsHasTermMapping(buildTermMapping(dataMappingRequest))
        .withOdsMappingDataStandard(OdsMappingDataStandard.fromValue(
            dataMappingRequest.getOdsMappingDataStandard().value()));
  }

  private void publishCreateEvent(DataMapping dataMapping, Agent agent)
      throws ProcessingFailedException {
    try {
      rabbitMqPublisherService.publishCreateEvent(dataMapping, agent);
    } catch (JsonProcessingException e) {
      log.error("Unable to publish message to RabbitMQ", e);
      rollbackMappingCreation(dataMapping);
      throw new ProcessingFailedException("Failed to create new machine annotation service", e);
    }
  }

  private void rollbackMappingCreation(DataMapping dataMapping) {
    var request = fdoRecordService.buildRollbackCreateRequest(dataMapping.getId());
    try {
      handleComponent.rollbackHandleCreation(request);
    } catch (PidException e) {
      log.error(
          "Unable to rollback handle creation for data mapping. Manually delete the following handle: {}. Cause of error: ",
          dataMapping.getId(), e);
    }
    repository.rollbackDataMappingCreation(dataMapping.getId());
  }

  public JsonApiWrapper updateDataMapping(String id, DataMappingRequest dataMappingRequest,
      Agent agent, String path) throws NotFoundException, ProcessingFailedException {
    var currentDataMappingOptional = repository.getActiveDataMapping(id);
    if (currentDataMappingOptional.isEmpty()) {
      throw new NotFoundException("Requested data mapping does not exist");
    }
    var currentDataMapping = currentDataMappingOptional.get();
    var dataMapping = buildDataMapping(dataMappingRequest,
        currentDataMapping.getSchemaVersion() + 1,
        agent, id, currentDataMapping.getSchemaDateCreated());
    if (isEqual(dataMapping, currentDataMapping)) {
      return null;
    } else {
      repository.updateDataMapping(dataMapping);
      publishUpdateEvent(dataMapping, currentDataMappingOptional.get(), agent);
      return wrapSingleResponse(dataMapping, path);
    }
  }

  private void publishUpdateEvent(DataMapping dataMapping,
      DataMapping currentDataMapping, Agent agent) throws ProcessingFailedException {
    try {
      rabbitMqPublisherService.publishUpdateEvent(dataMapping, currentDataMapping, agent);
    } catch (JsonProcessingException e) {
      log.error("Unable to publish message to RabbitMQ", e);
      rollbackToPreviousVersion(currentDataMapping);
      throw new ProcessingFailedException("Failed to create new machine annotation service", e);
    }
  }

  private void rollbackToPreviousVersion(DataMapping currentDataMapping) {
    repository.updateDataMapping(currentDataMapping);
  }

  public void tombstoneDataMapping(String id, Agent agent)
      throws NotFoundException, ProcessingFailedException {
    var result = repository.getActiveDataMapping(id);
    if (result.isPresent()) {
      var dataMapping = result.get();
      tombstoneHandle(id);
      var timestamp = Instant.now();
      var tombstoneDataMapping = buildTombstoneDataMapping(dataMapping, agent, timestamp);
      repository.tombstoneDataMapping(tombstoneDataMapping, timestamp);
      try {
        rabbitMqPublisherService.publishTombstoneEvent(tombstoneDataMapping, dataMapping, agent);
      } catch (JsonProcessingException e) {
        log.error("Unable to publish tombstone event to provenance service", e);
        throw new ProcessingFailedException(
            "Unable to publish tombstone event to provenance service", e);
      }
    } else {
      throw new NotFoundException("Requested data mapping " + id + " does not exist");
    }
  }

  private void tombstoneHandle(String handle) throws ProcessingFailedException {
    var request = fdoRecordService.buildTombstoneRequest(ObjectType.DATA_MAPPING, handle);
    try {
      var tmp = fdoRecordService.buildRollbackCreateRequest(handle);
      handleComponent.rollbackHandleCreation2(tmp);
    } catch (PidException e) {
      log.error("Unable to tombstone handle {}", handle, e);
      throw new ProcessingFailedException("Unable to tombstone handle", e);
    }
  }

  protected Optional<DataMapping> getActiveDataMapping(String id) {
    return repository.getActiveDataMapping(id);
  }

  public JsonApiListWrapper getDataMappings(int pageNum, int pageSize, String path) {
    var dataMappings = repository.getDataMappings(pageNum, pageSize);
    return wrapResponse(dataMappings, pageNum, pageSize, path);
  }

  public JsonApiWrapper getDataMappingById(String id, String path) throws NotFoundException {
    var dataMapping = repository.getDataMapping(id);
    if (dataMapping != null) {
      return wrapSingleResponse(dataMapping, path);
    }
    log.warn("Unable to find source system {}", id);
    throw new NotFoundException("Unable to find source system " + id);

  }

  private JsonApiWrapper wrapSingleResponse(DataMapping dataMapping, String path) {
    return new JsonApiWrapper(
        new JsonApiData(dataMapping.getId(), ObjectType.DATA_MAPPING,
            flattenDataMapping(dataMapping)),
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
