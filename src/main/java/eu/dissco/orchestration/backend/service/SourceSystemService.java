package eu.dissco.orchestration.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.orchestration.backend.domain.HandleType;
import eu.dissco.orchestration.backend.domain.SourceSystem;
import eu.dissco.orchestration.backend.domain.SourceSystemRecord;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiData;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.repository.SourceSystemRepository;
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
public class SourceSystemService {

  private final SourceSystemRepository repository;
  private final HandleService handleService;

  private final ObjectMapper mapper;

  public SourceSystemRecord createSourceSystem(SourceSystem sourceSystem) throws TransformerException {
    var handle = handleService.createNewHandle(HandleType.SOURCE_SYSTEM);
    var sourceSystemRecord = new SourceSystemRecord(handle, Instant.now(), sourceSystem);
    repository.createSourceSystem(sourceSystemRecord);
    return sourceSystemRecord;
  }

  public SourceSystemRecord updateSourceSystem(String id, SourceSystem sourceSystem) {
    var sourceSystemRecord = new SourceSystemRecord(id, Instant.now(), sourceSystem);
    repository.updateSourceSystem(sourceSystemRecord);
    return sourceSystemRecord;
  }

  public JsonApiWrapper getSourceSystemRecords(int pageNum, int pageSize, String path){
    var ssRecords = repository.getSourceSystems(pageNum, pageSize);
    var dataNode = wrapData(ssRecords);
    return new JsonApiWrapper(dataNode, new JsonApiLinks(path));
  }

  List<JsonApiData> wrapData(List<SourceSystemRecord> ssRecords){
    List<JsonApiData> dataNode = new ArrayList<>();
    ssRecords.forEach(ss -> dataNode.add(new JsonApiData(ss.id(), HandleType.SOURCE_SYSTEM, mapper.valueToTree(ss))));
    return dataNode;
  }



}
