package eu.dissco.orchestration.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.orchestration.backend.domain.HandleType;
import eu.dissco.orchestration.backend.domain.SourceSystem;
import eu.dissco.orchestration.backend.domain.SourceSystemRecord;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiData;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiListWrapper;
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
  private final MappingService mappingService;

  private final ObjectMapper mapper;

  public JsonApiWrapper createSourceSystem(SourceSystem sourceSystem, String path)
      throws TransformerException, NotFoundException {
    var handle = handleService.createNewHandle(HandleType.SOURCE_SYSTEM);
    validateMappingExists(sourceSystem.mappingId());

    var sourceSystemRecord = new SourceSystemRecord(handle, Instant.now(), sourceSystem);
    repository.createSourceSystem(sourceSystemRecord);
    return wrapSingleResponse(handle, sourceSystemRecord, path);
  }

  private void validateMappingExists(String mappingId) throws NotFoundException{
    var mappingRecord = mappingService.getActiveMapping(mappingId);
    if (mappingRecord.isEmpty()){
      throw new NotFoundException("Unable to locate Mapping record with id " + mappingId);
    }
  }

  public JsonApiWrapper updateSourceSystem(String id, SourceSystem sourceSystem, String path) throws NotFoundException{
    var prevSourceSystem = repository.getActiveSourceSystem(id);
    if (prevSourceSystem.isEmpty()){
      throw new NotFoundException("Could not update Source System " + id + ". Verify resource exists.");
    }
    if ((prevSourceSystem.get().sourceSystem()).equals(sourceSystem)){
      return null;
    }
    var sourceSystemRecord = new SourceSystemRecord(id, Instant.now(), sourceSystem);
    repository.updateSourceSystem(sourceSystemRecord);
    return wrapSingleResponse(id, sourceSystemRecord, path);
  }

  public JsonApiWrapper getSourceSystemById(String id, String path) {
    var sourceSystemRecord = repository.getSourceSystem(id);
    return wrapSingleResponse(id, sourceSystemRecord, path);
  }


  public JsonApiListWrapper getSourceSystemRecords(int pageNum, int pageSize, String path) {
    var sourceSystemRecords = repository.getSourceSystems(pageNum, pageSize + 1);
    return wrapResponse(sourceSystemRecords, pageNum, pageSize, path);
  }

  private JsonApiWrapper wrapSingleResponse(String id, SourceSystemRecord sourceSystemRecord, String path){
    return new JsonApiWrapper(
        new JsonApiData(id, HandleType.SOURCE_SYSTEM, mapper.valueToTree(sourceSystemRecord.sourceSystem())),
        new JsonApiLinks(path)
    );
  }

  public void deleteSourceSystem(String id) throws NotFoundException {
    var result = repository.getActiveSourceSystem(id);
    if (result.isPresent()){
      Instant deleted = Instant.now();
      repository.deleteSourceSystem(id, deleted);
    }
    else throw new NotFoundException("Requested source system"+id +" does not exist");
  }


  private JsonApiListWrapper wrapResponse(List<SourceSystemRecord> sourceSystemRecords, int pageNum,
      int pageSize, String path) {
    boolean hasNext = sourceSystemRecords.size() > pageSize;
    sourceSystemRecords = hasNext ? sourceSystemRecords.subList(0, pageSize) : sourceSystemRecords;
    var linksNode = new JsonApiLinks(pageSize, pageNum, hasNext, path);
    var dataNode = wrapData(sourceSystemRecords);
    return new JsonApiListWrapper(dataNode, linksNode);
  }

  List<JsonApiData> wrapData(List<SourceSystemRecord> sourceSystemRecords) {
    return sourceSystemRecords.stream()
        .map(r -> new JsonApiData(r.id(), HandleType.SOURCE_SYSTEM, mapper.valueToTree(r)))
        .toList();
  }


}
