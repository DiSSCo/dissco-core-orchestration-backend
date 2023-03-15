package eu.dissco.orchestration.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.orchestration.backend.domain.HandleType;
import eu.dissco.orchestration.backend.domain.SourceSystem;
import eu.dissco.orchestration.backend.domain.SourceSystemRecord;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiData;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.exception.NotFoundException;
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

  public SourceSystemRecord createSourceSystem(SourceSystem sourceSystem)
      throws TransformerException {
    var handle = handleService.createNewHandle(HandleType.SOURCE_SYSTEM);
    var sourceSystemRecord = new SourceSystemRecord(handle, Instant.now(), sourceSystem);
    repository.createSourceSystem(sourceSystemRecord);
    return sourceSystemRecord;
  }

  public SourceSystemRecord updateSourceSystem(String id, SourceSystem sourceSystem) throws NotFoundException{
    var sourceSystemExists = repository.sourceSystemExists(id);
    if (sourceSystemExists < 1){
      throw new NotFoundException("Could not update Source System " + id + ". Verify resource exists.");
    }
    var sourceSystemRecord = new SourceSystemRecord(id, Instant.now(), sourceSystem);
    repository.updateSourceSystem(sourceSystemRecord);
    return sourceSystemRecord;
  }

  public SourceSystemRecord getSourceSystemById(String id) {
    return repository.getSourceSystemById(id);
  }

  public JsonApiWrapper getSourceSystemRecords(int pageNum, int pageSize, String path) {
    var sourceSystemRecords = repository.getSourceSystems(pageNum, pageSize + 1);
    return wrapResponse(sourceSystemRecords, pageNum, pageSize, path);
  }

  public void deleteSourceSystem(String id) throws NotFoundException {
    int result = repository.sourceSystemExists(id);
    if (result > 0){
      Instant deleted = Instant.now();
      repository.deleteSourceSystem(id, deleted);
    }
    else throw new NotFoundException("Requested mapping does not exist");
  }

  private JsonApiWrapper wrapResponse(List<SourceSystemRecord> sourceSystemRecords, int pageNum,
      int pageSize, String path) {
    boolean hasNext = sourceSystemRecords.size() > pageSize;
    sourceSystemRecords = hasNext ? sourceSystemRecords.subList(0, pageSize) : sourceSystemRecords;
    var linksNode = new JsonApiLinks(pageSize, pageNum, hasNext, path);
    var dataNode = wrapData(sourceSystemRecords);
    return new JsonApiWrapper(dataNode, linksNode);
  }

  List<JsonApiData> wrapData(List<SourceSystemRecord> sourceSystemRecords) {
    return sourceSystemRecords.stream()
        .map(r -> new JsonApiData(r.id(), HandleType.SOURCE_SYSTEM, mapper.valueToTree(r)))
        .toList();
  }


}
