package eu.dissco.orchestration.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.dissco.orchestration.backend.domain.HandleType;
import eu.dissco.orchestration.backend.domain.Mapping;
import eu.dissco.orchestration.backend.domain.MappingRecord;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiData;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiListWrapper;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.repository.MappingRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import javax.xml.transform.TransformerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MappingService {

  private final HandleService handleService;
  private final MappingRepository repository;
  private final ObjectMapper mapper;

  public JsonApiWrapper createMapping(Mapping mapping, String userId, String path) throws TransformerException {
    var handle = handleService.createNewHandle(HandleType.MAPPING);
    var mappingRecord = new MappingRecord(handle, 1, Instant.now(), null, userId, mapping);
    repository.createMapping(mappingRecord);
    return wrapSingleResponse(handle, mappingRecord, path);
  }
  public JsonApiWrapper  updateMapping(String id, Mapping mapping, String userId, String path) throws NotFoundException{
    var currentVersion = repository.getActiveMapping(id);
    if (currentVersion.isEmpty()){
      throw new NotFoundException("Requested mapping does not exist");
    }
    if (!currentVersion.get().mapping().equals(mapping)){
      var mappingRecord = new MappingRecord(id, currentVersion.get().version() + 1, Instant.now(),
          null, userId,
          mapping);
      repository.deleteMapping(id, Instant.now());
      repository.createMapping(mappingRecord);
      return wrapSingleResponse(id, mappingRecord, path);
    } else {
      return null;
    }
  }

  public void deleteMapping(String id) throws NotFoundException {
    var result = repository.getActiveMapping(id);
    if (result.isPresent()){
      Instant deleted = Instant.now();
      repository.deleteMapping(id, deleted);
    }
    else throw new NotFoundException("Requested mapping "+id +" does not exist");
  }

  protected Optional<MappingRecord> getActiveMapping(String id){
    return repository.getActiveMapping(id);
  }

  public JsonApiListWrapper getMappings(int pageNum, int pageSize, String//When
     path){
    var mappingRecords = repository.getMappings(pageNum, pageSize+1);
    return wrapResponse(mappingRecords, pageNum, pageSize, path);
  }

  public JsonApiWrapper getMappingById(String id, String path){
    var mappingRecord = repository.getMapping(id);
    return wrapSingleResponse(id, mappingRecord, path);
  }

  private JsonApiWrapper wrapSingleResponse(String id, MappingRecord mappingRecord, String path){
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

  private JsonNode flattenMappingRecord(MappingRecord mappingRecord){
    var mappingNode = (ObjectNode) mapper.valueToTree(mappingRecord.mapping());
    mappingNode.put("version", mappingRecord.version());
    mappingNode.put("created", mappingRecord.created().toString());
    return mappingNode;
  }

}
