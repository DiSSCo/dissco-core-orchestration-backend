package eu.dissco.orchestration.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.diff.JsonDiff;
import eu.dissco.orchestration.backend.domain.HandleType;
import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.domain.SourceSystem;
import eu.dissco.orchestration.backend.domain.SourceSystemRecord;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiData;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiListWrapper;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.exception.PidAuthenticationException;
import eu.dissco.orchestration.backend.exception.PidCreationException;
import eu.dissco.orchestration.backend.exception.ProcessingFailedException;
import eu.dissco.orchestration.backend.repository.SourceSystemRepository;
import eu.dissco.orchestration.backend.web.FdoRecordBuilder;
import eu.dissco.orchestration.backend.web.HandleComponent;
import java.time.Instant;
import java.util.List;
import javax.xml.transform.TransformerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SourceSystemService {

  public static final String SUBJECT_TYPE = "SourceSystem";
  private final FdoRecordBuilder fdoRecordBuilder;
  private final HandleComponent handleComponent;
  private final SourceSystemRepository repository;
  private final MappingService mappingService;
  private final KafkaPublisherService kafkaPublisherService;
  private final ObjectMapper mapper;

  public JsonApiWrapper createSourceSystem(SourceSystem sourceSystem, String userId, String path)
      throws NotFoundException, PidAuthenticationException, PidCreationException {
    var request = fdoRecordBuilder.buildCreateRequest(sourceSystem, ObjectType.SOURCE_SYSTEM);
    var handle = handleComponent.postHandle(request);
    validateMappingExists(sourceSystem.mappingId());
    var sourceSystemRecord = new SourceSystemRecord(handle, 1, userId, Instant.now(), null,
        sourceSystem);
    repository.createSourceSystem(sourceSystemRecord);
    publishCreateEvent(handle, sourceSystemRecord);
    return wrapSingleResponse(handle, sourceSystemRecord, path);
  }

  private void publishCreateEvent(String handle, SourceSystemRecord sourceSystemRecord) {
    try {
      kafkaPublisherService.publishCreateEvent(handle, mapper.valueToTree(sourceSystemRecord),
          SUBJECT_TYPE);
    } catch (JsonProcessingException e) {
      log.error("Unable to publish message to Kafka", e);
      rollbackSourceSystemCreation(sourceSystemRecord);
      throw new ProcessingFailedException("Failed to create new machine annotation service", e);
    }
  }

  private void rollbackSourceSystemCreation(SourceSystemRecord sourceSystemRecord) {
    var request = fdoRecordBuilder.buildRollbackCreateRequest(sourceSystemRecord.id());
    try {
      handleComponent.rollbackHandleCreation(request);
    } catch (PidAuthenticationException | PidCreationException e) {
      log.error(
          "Unable to rollback handle creation for source system. Manually delete the following handle: {}. Cause of error: ",
          sourceSystemRecord.id(), e);
    }
    repository.rollbackSourceSystemCreation(sourceSystemRecord.id());
  }

  private void validateMappingExists(String mappingId) throws NotFoundException {
    var mappingRecord = mappingService.getActiveMapping(mappingId);
    if (mappingRecord.isEmpty()) {
      throw new NotFoundException("Unable to locate Mapping record with id " + mappingId);
    }
  }

  public JsonApiWrapper updateSourceSystem(String id, SourceSystem sourceSystem, String userId,
      String path) throws NotFoundException {
    var currentSourceSystemOptional = repository.getActiveSourceSystem(id);
    if (currentSourceSystemOptional.isEmpty()) {
      throw new NotFoundException(
          "Could not update Source System " + id + ". Verify resource exists.");
    }
    if ((currentSourceSystemOptional.get().sourceSystem()).equals(sourceSystem)) {
      return null;
    }
    var currentSourceSystem = currentSourceSystemOptional.get();
    var sourceSystemRecord = new SourceSystemRecord(id, currentSourceSystem.version() + 1, userId,
        Instant.now(), null, sourceSystem);
    repository.updateSourceSystem(sourceSystemRecord);
    publishUpdateEvent(sourceSystemRecord, currentSourceSystem);
    return wrapSingleResponse(id, sourceSystemRecord, path);
  }

  private void publishUpdateEvent(SourceSystemRecord newSourceSystemRecord,
      SourceSystemRecord currentSourceSystemRecord) {
    JsonNode jsonPatch = JsonDiff.asJson(mapper.valueToTree(newSourceSystemRecord.sourceSystem()),
        mapper.valueToTree(currentSourceSystemRecord.sourceSystem()));
    try {
      kafkaPublisherService.publishUpdateEvent(newSourceSystemRecord.id(),
          mapper.valueToTree(newSourceSystemRecord),
          jsonPatch, SUBJECT_TYPE);
    } catch (JsonProcessingException e) {
      log.error("Unable to publish message to Kafka", e);
      rollbackToPreviousVersion(currentSourceSystemRecord);
      throw new ProcessingFailedException("Failed to create new machine annotation service", e);
    }
  }

  private void rollbackToPreviousVersion(SourceSystemRecord currentSourceSystemRecord) {
    repository.updateSourceSystem(currentSourceSystemRecord);
  }

  public JsonApiWrapper getSourceSystemById(String id, String path) {
    var sourceSystemRecord = repository.getSourceSystem(id);
    return wrapSingleResponse(id, sourceSystemRecord, path);
  }

  public JsonApiListWrapper getSourceSystemRecords(int pageNum, int pageSize, String path) {
    var sourceSystemRecords = repository.getSourceSystems(pageNum, pageSize);
    return wrapResponse(sourceSystemRecords, pageNum, pageSize, path);
  }

  private JsonApiWrapper wrapSingleResponse(String id, SourceSystemRecord sourceSystemRecord,
      String path) {
    return new JsonApiWrapper(
        new JsonApiData(id, HandleType.SOURCE_SYSTEM,
            flattenSourceSystemRecord(sourceSystemRecord)),
        new JsonApiLinks(path)
    );
  }

  public void deleteSourceSystem(String id) throws NotFoundException {
    var result = repository.getActiveSourceSystem(id);
    if (result.isPresent()) {
      var deleted = Instant.now();
      repository.deleteSourceSystem(id, deleted);
    } else {
      throw new NotFoundException("Requested source system: " + id + " does not exist");
    }
  }


  private JsonApiListWrapper wrapResponse(List<SourceSystemRecord> sourceSystemRecords, int pageNum,
      int pageSize, String path) {
    boolean hasNext = sourceSystemRecords.size() > pageSize;
    sourceSystemRecords = hasNext ? sourceSystemRecords.subList(0, pageSize) : sourceSystemRecords;
    var linksNode = new JsonApiLinks(pageSize, pageNum, hasNext, path);
    var dataNode = wrapData(sourceSystemRecords);
    return new JsonApiListWrapper(dataNode, linksNode);
  }

  private List<JsonApiData> wrapData(List<SourceSystemRecord> sourceSystemRecords) {
    return sourceSystemRecords.stream()
        .map(r -> new JsonApiData(r.id(), HandleType.SOURCE_SYSTEM, flattenSourceSystemRecord(r)))
        .toList();
  }

  private JsonNode flattenSourceSystemRecord(SourceSystemRecord sourceSystemRecord) {
    var sourceSystemNode = (ObjectNode) mapper.valueToTree(sourceSystemRecord.sourceSystem());
    sourceSystemNode.put("created", sourceSystemRecord.created().toString());
    sourceSystemNode.put("version", sourceSystemRecord.version());
    if (sourceSystemRecord.deleted() != null) {
      sourceSystemNode.put("deleted", sourceSystemRecord.deleted().toString());
    }
    return sourceSystemNode;
  }
}
