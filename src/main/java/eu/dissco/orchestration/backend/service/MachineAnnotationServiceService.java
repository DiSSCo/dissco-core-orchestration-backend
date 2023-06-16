package eu.dissco.orchestration.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.dissco.orchestration.backend.domain.HandleType;
import eu.dissco.orchestration.backend.domain.MachineAnnotationService;
import eu.dissco.orchestration.backend.domain.MachineAnnotationServiceRecord;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiData;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiListWrapper;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.repository.MachineAnnotationServiceRepository;
import java.time.Instant;
import java.util.List;
import javax.xml.transform.TransformerException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MachineAnnotationServiceService {

  private final HandleService handleService;
  private final MachineAnnotationServiceRepository repository;
  private final ObjectMapper mapper;

  public JsonApiWrapper createMachineAnnotationService(MachineAnnotationService mas, String userId,
      String path)
      throws TransformerException {
    var handle = handleService.createNewHandle(HandleType.MACHINE_ANNOTATION_SERVICE);
    var masRecord = new MachineAnnotationServiceRecord(handle, 1, Instant.now(), userId, mas, null);
    repository.createMachineAnnotationService(masRecord);
    return wrapSingleResponse(handle, masRecord, path);
  }

  public JsonApiWrapper updateMachineAnnotationService(String id,
      MachineAnnotationService mas, String userId, String path) throws NotFoundException {
    var currentMasOptional = repository.getActiveMachineAnnotationService(id);
    if (currentMasOptional.isPresent()) {
      var currentMasRecord = currentMasOptional.get();
      if (mas.equals(currentMasRecord.mas())) {
        return null;
      } else {
        var masRecord = new MachineAnnotationServiceRecord(currentMasRecord.pid(),
            currentMasRecord.version() + 1, Instant.now(), userId, mas, null);
        repository.updateMachineAnnotationService(masRecord);
        return wrapSingleResponse(masRecord.pid(), masRecord, path);
      }
    } else {
      throw new NotFoundException("Requested machine annotation system: " + id + "does not exist");
    }
  }

  public void deleteMachineAnnotationService(String id) throws NotFoundException {
    var currentMasOptional = repository.getActiveMachineAnnotationService(id);
    if (currentMasOptional.isPresent()) {
      var deleted = Instant.now();
      repository.deleteMachineAnnotationService(id, deleted);
    } else {
      throw new NotFoundException("Requested machine annotation system: " + id + "does not exist");
    }
  }

  public JsonApiWrapper getMachineAnnotationService(String id, String path) {
    var masRecord = repository.getMachineAnnotationService(id);
    return wrapSingleResponse(id, masRecord, path);
  }

  public JsonApiListWrapper getMachineAnnotationServices(int pageNum, int pageSize, String path) {
    var masRecords = repository.getMachineAnnotationServices(pageNum, pageSize);
    return wrapResponse(masRecords, pageNum, pageSize, path);
  }

  private JsonApiWrapper wrapSingleResponse(String id, MachineAnnotationServiceRecord masRecord,
      String path) {
    return new JsonApiWrapper(
        new JsonApiData(id, HandleType.MACHINE_ANNOTATION_SERVICE,
            flattenMasRecord(masRecord)),
        new JsonApiLinks(path)
    );
  }

  private JsonApiListWrapper wrapResponse(List<MachineAnnotationServiceRecord> masRecords,
      int pageNum,
      int pageSize, String path) {
    boolean hasNext = masRecords.size() > pageSize;
    masRecords = hasNext ? masRecords.subList(0, pageSize) : masRecords;
    var linksNode = new JsonApiLinks(pageSize, pageNum, hasNext, path);
    var dataNode = wrapData(masRecords);
    return new JsonApiListWrapper(dataNode, linksNode);
  }

  private List<JsonApiData> wrapData(List<MachineAnnotationServiceRecord> masRecords) {
    return masRecords.stream()
        .map(r -> new JsonApiData(r.pid(), HandleType.MACHINE_ANNOTATION_SERVICE,
            flattenMasRecord(r)))
        .toList();
  }

  private JsonNode flattenMasRecord(MachineAnnotationServiceRecord masRecord) {
    var mappingNode = (ObjectNode) mapper.valueToTree(masRecord.mas());
    mappingNode.put("version", masRecord.version());
    mappingNode.put("created", masRecord.created().toString());
    if (masRecord.deleted() != null) {
      mappingNode.put("deleted", masRecord.deleted().toString());
    }
    return mappingNode;
  }
}