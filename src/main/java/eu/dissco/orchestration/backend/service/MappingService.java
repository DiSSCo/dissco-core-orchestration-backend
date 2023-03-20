package eu.dissco.orchestration.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.orchestration.backend.domain.HandleType;
import eu.dissco.orchestration.backend.domain.Mapping;
import eu.dissco.orchestration.backend.domain.MappingRecord;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiData;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiListWrapper;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.repository.MappingRepository;
import java.time.Instant;
import java.util.List;
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
    var mappingRecord = new MappingRecord(handle, 1, Instant.now(), userId, mapping);
    repository.createMapping(mappingRecord);
    return wrapSingleResponse(handle, mappingRecord, path);
  }
  public JsonApiWrapper updateMapping(String id, Mapping mapping, String userId, String path) {
    var currentVersion = repository.getMapping(id);
    if (!currentVersion.mapping().equals(mapping)){
      var mappingRecord = new MappingRecord(id, currentVersion.version() + 1, Instant.now(), userId,
          mapping);
      repository.createMapping(mappingRecord);
      return wrapSingleResponse(id, mappingRecord, path);
    } else {
      return null;
    }
  }

  public JsonApiWrapper getMappingById(String id, String path){
    var mappingRecord = repository.getMapping(id);
    return wrapSingleResponse(id, mappingRecord, path);
  }

  private JsonApiWrapper wrapSingleResponse(String id, MappingRecord mappingRecord, String path){
    return new JsonApiWrapper(
        new JsonApiData(id, HandleType.MAPPING, mapper.valueToTree(mappingRecord.mapping())),
        new JsonApiLinks(path)
    );
  }

  public JsonApiListWrapper getMappings(int pageNum, int pageSize, String
     path){
    var mappingRecords = repository.getMappings(pageNum, pageSize+1);
    return wrapResponse(mappingRecords, pageNum, pageSize, path);
  }


  private JsonApiListWrapper wrapResponse(List<MappingRecord> mappingRecords, int pageNum,
      int pageSize, String path) {
    boolean hasNext = mappingRecords.size() > pageSize;
    mappingRecords = hasNext ? mappingRecords.subList(0, pageSize) : mappingRecords;
    var linksNode = new JsonApiLinks(pageSize, pageNum, hasNext, path);
    var dataNode = wrapData(mappingRecords);
    return new JsonApiListWrapper(dataNode, linksNode);
  }

  List<JsonApiData> wrapData(List<MappingRecord> mappingRecords) {
    return mappingRecords.stream()
        .map(r -> new JsonApiData(r.id(), HandleType.MAPPING, mapper.valueToTree(r)))
        .toList();
  }

}
