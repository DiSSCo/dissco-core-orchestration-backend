package eu.dissco.orchestration.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.diff.JsonDiff;
import eu.dissco.orchestration.backend.domain.HandleType;
import eu.dissco.orchestration.backend.domain.Mapping;
import eu.dissco.orchestration.backend.domain.MappingRecord;
import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiData;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiListWrapper;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.exception.PidAuthenticationException;
import eu.dissco.orchestration.backend.exception.PidCreationException;
import eu.dissco.orchestration.backend.exception.ProcessingFailedException;
import eu.dissco.orchestration.backend.repository.MappingRepository;
import eu.dissco.orchestration.backend.web.HandleComponent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MappingService {

  public static final String SUBJECT_TYPE = "Mapping";
  private final FdoRecordService fdoRecordService;
  private final HandleComponent handleComponent;
  private final KafkaPublisherService kafkaPublisherService;
  private final MappingRepository repository;
  private final ObjectMapper mapper;

  public JsonApiWrapper createMapping(Mapping mapping, String userId, String path) {
    var requestBody = fdoRecordService.buildCreateRequest(mapping, ObjectType.MAPPING);
    String handle = null;
    try {
      handle = handleComponent.postHandle(requestBody);
    } catch (PidAuthenticationException | PidCreationException e) {
      throw new ProcessingFailedException(e.getMessage(), e);
    }
    var mappingRecord = new MappingRecord(handle, 1, Instant.now(), null, userId, mapping);
    repository.createMapping(mappingRecord);
    publishCreateEvent(handle, mappingRecord);
    return wrapSingleResponse(handle, mappingRecord, path);
  }

  private void publishCreateEvent(String handle, MappingRecord mappingRecord) {
    try {
      kafkaPublisherService.publishCreateEvent(handle, mapper.valueToTree(mappingRecord),
          SUBJECT_TYPE);
    } catch (JsonProcessingException e) {
      log.error("Unable to publish message to Kafka", e);
      rollbackMappingCreation(mappingRecord);
      throw new ProcessingFailedException("Failed to create new machine annotation service", e);
    }
  }

  private void rollbackMappingCreation(MappingRecord mappingRecord) {
    var request = fdoRecordService.buildRollbackCreateRequest(mappingRecord.id());
    try {
      handleComponent.rollbackHandleCreation(request);
    } catch (PidAuthenticationException | PidCreationException e) {
      log.error(
          "Unable to rollback handle creation for mapping. Manually delete the following handle: {}. Cause of error: ",
          mappingRecord.id(), e);
    }
    repository.rollbackMappingCreation(mappingRecord.id());
  }

  public JsonApiWrapper updateMapping(String id, Mapping mapping, String userId, String path)
      throws NotFoundException {
    var currentVersion = repository.getActiveMapping(id);
    if (currentVersion.isEmpty()) {
      throw new NotFoundException("Requested mapping does not exist");
    }
    if (!currentVersion.get().mapping().equals(mapping)) {
      var newMappingRecord = new MappingRecord(id, currentVersion.get().version() + 1,
          Instant.now(),
          null, userId, mapping);
      repository.updateMapping(newMappingRecord);
      publishUpdateEvent(newMappingRecord, currentVersion.get());
      return wrapSingleResponse(id, newMappingRecord, path);
    } else {
      return null;
    }
  }

  private void publishUpdateEvent(MappingRecord newMappingRecord,
      MappingRecord currentMappingRecord) {
    JsonNode jsonPatch = JsonDiff.asJson(mapper.valueToTree(newMappingRecord.mapping()),
        mapper.valueToTree(currentMappingRecord.mapping()));
    try {
      kafkaPublisherService.publishUpdateEvent(newMappingRecord.id(),
          mapper.valueToTree(newMappingRecord), jsonPatch,
          SUBJECT_TYPE);
    } catch (JsonProcessingException e) {
      log.error("Unable to publish message to Kafka", e);
      rollbackToPreviousVersion(currentMappingRecord);
      throw new ProcessingFailedException("Failed to create new machine annotation service", e);
    }
  }

  private void rollbackToPreviousVersion(MappingRecord currentMappingRecord) {
    repository.updateMapping(currentMappingRecord);
  }

  public void deleteMapping(String id) throws NotFoundException {
    var result = repository.getActiveMapping(id);
    if (result.isPresent()) {
      Instant deleted = Instant.now();
      repository.deleteMapping(id, deleted);
    } else {
      throw new NotFoundException("Requested mapping " + id + " does not exist");
    }
  }

  protected Optional<MappingRecord> getActiveMapping(String id) {
    return repository.getActiveMapping(id);
  }

  public JsonApiListWrapper getMappings(int pageNum, int pageSize, String path) {
    var mappingRecords = repository.getMappings(pageNum, pageSize);
    return wrapResponse(mappingRecords, pageNum, pageSize, path);
  }

  public JsonApiWrapper getMappingById(String id, String path) {
    var mappingRecord = repository.getMapping(id);
    return wrapSingleResponse(id, mappingRecord, path);
  }

  private JsonApiWrapper wrapSingleResponse(String id, MappingRecord mappingRecord, String path) {
    return new JsonApiWrapper(
        new JsonApiData(id, HandleType.MAPPING, flattenMappingRecord(mappingRecord)),
        new JsonApiLinks(path)
    );
  }

  private JsonApiListWrapper wrapResponse(List<MappingRecord> mappingRecords, int pageNum,
      int pageSize, String path) {
    boolean hasNext = mappingRecords.size() > pageSize;
    mappingRecords = hasNext ? mappingRecords.subList(0, pageSize) : mappingRecords;
    var linksNode = new JsonApiLinks(pageSize, pageNum, hasNext, path);
    var dataNode = wrapData(mappingRecords);
    return new JsonApiListWrapper(dataNode, linksNode);
  }

  private List<JsonApiData> wrapData(List<MappingRecord> mappingRecords) {
    return mappingRecords.stream()
        .map(r -> new JsonApiData(r.id(), HandleType.MAPPING, flattenMappingRecord(r)))
        .toList();
  }

  private JsonNode flattenMappingRecord(MappingRecord mappingRecord) {
    var mappingNode = (ObjectNode) mapper.valueToTree(mappingRecord.mapping());
    mappingNode.put("version", mappingRecord.version());
    mappingNode.put("created", mappingRecord.created().toString());
    if (mappingRecord.deleted() != null) {
      mappingNode.put("deleted", mappingRecord.deleted().toString());
    }
    return mappingNode;
  }

}
