package eu.dissco.orchestration.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.orchestration.backend.domain.HandleType;
import eu.dissco.orchestration.backend.domain.Mapping;
import eu.dissco.orchestration.backend.domain.MappingRecord;
import eu.dissco.orchestration.backend.domain.SourceSystemRecord;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiData;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.repository.MappingRepository;
import java.time.Instant;
import java.util.ArrayList;
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

  public MappingRecord createMapping(Mapping mapping, String userId) throws TransformerException {
    var handle = handleService.createNewHandle(HandleType.MAPPING);
    var mappingRecord = new MappingRecord(handle, 1, Instant.now(), userId, mapping);
    repository.createMapping(mappingRecord);
    return mappingRecord;
  }

  public JsonApiWrapper getMappings(int pageNum, int pageSize, String path){
    var mappingRecords = repository.getMappings(pageNum, pageSize);
    var dataNode = wrapData(mappingRecords);
    return new JsonApiWrapper(dataNode, new JsonApiLinks(path));
  }

  List<JsonApiData> wrapData(List<MappingRecord> mappingRecords){
    List<JsonApiData> dataNode = new ArrayList<>();
    mappingRecords.forEach(m -> dataNode.add(new JsonApiData(m.id(), HandleType.MAPPING, mapper.valueToTree(m))));
    return dataNode;
  }

  public MappingRecord updateMapping(String id, Mapping mapping, String userId) {
    var currentVersion = repository.getMapping(id);
    if (!currentVersion.mapping().equals(mapping)){
      var mappingRecord = new MappingRecord(id, currentVersion.version() + 1, Instant.now(), userId,
          mapping);
      repository.createMapping(mappingRecord);
      return mappingRecord;
    } else {
      return null;
    }
  }
}
