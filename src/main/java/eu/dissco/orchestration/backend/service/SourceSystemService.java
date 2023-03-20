package eu.dissco.orchestration.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.dissco.orchestration.backend.domain.HandleType;
import eu.dissco.orchestration.backend.domain.SourceSystem;
import eu.dissco.orchestration.backend.domain.SourceSystemRecord;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiData;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiListWrapper;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.repository.SourceSystemRepository;
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

  private final SourceSystemRepository repository;
  private final HandleService handleService;

  private final ObjectMapper mapper;

  public JsonApiWrapper createSourceSystem(SourceSystem sourceSystem, String path)
      throws TransformerException {
    var handle = handleService.createNewHandle(HandleType.SOURCE_SYSTEM);
    var sourceSystemRecord = new SourceSystemRecord(handle, Instant.now(), sourceSystem);
    repository.createSourceSystem(sourceSystemRecord);
    return wrapSingleResponse(handle, sourceSystemRecord, path);
  }

  public JsonApiWrapper updateSourceSystem(String id, SourceSystem sourceSystem, String path) {
    var sourceSystemRecord = new SourceSystemRecord(id, Instant.now(), sourceSystem);
    repository.updateSourceSystem(sourceSystemRecord);
    return wrapSingleResponse(id, sourceSystemRecord, path);
  }

  public JsonApiWrapper getSourceSystemById(String id, String path) {
    var sourceSystemRecord = repository.getSourceSystemById(id);
    return wrapSingleResponse(id, sourceSystemRecord, path);
  }

  public JsonApiListWrapper getSourceSystemRecords(int pageNum, int pageSize, String path) {
    var sourceSystemRecords = repository.getSourceSystems(pageNum, pageSize + 1);
    return wrapResponse(sourceSystemRecords, pageNum, pageSize, path);
  }

  private JsonApiWrapper wrapSingleResponse(String id, SourceSystemRecord sourceSystemRecord, String path){
    return new JsonApiWrapper(
        new JsonApiData(id, HandleType.SOURCE_SYSTEM, flattenSourceSystemRecord(sourceSystemRecord)),
        new JsonApiLinks(path)
    );
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

  private JsonNode flattenSourceSystemRecord(SourceSystemRecord sourceSystemRecord){
    var sourceSystemNode =  (ObjectNode) mapper.valueToTree(sourceSystemRecord.sourceSystem());
    sourceSystemNode.put("created", sourceSystemRecord.created().toString());
    return sourceSystemNode;
  }
}
