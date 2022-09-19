package eu.dissco.orchestration.backend.service;

import eu.dissco.orchestration.backend.domain.HandleType;
import eu.dissco.orchestration.backend.domain.Mapping;
import eu.dissco.orchestration.backend.domain.MappingRecord;
import eu.dissco.orchestration.backend.repository.MappingRepository;
import java.time.Instant;
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

  public MappingRecord createMapping(Mapping mapping, String userId) throws TransformerException {
    var handle = handleService.createNewHandle(HandleType.MAPPING);
    var mappingRecord = new MappingRecord(handle, 1, Instant.now(), userId, mapping);
    repository.createMapping(mappingRecord);
    return mappingRecord;
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
