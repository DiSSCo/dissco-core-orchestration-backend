package eu.dissco.orchestration.backend.service;

import eu.dissco.orchestration.backend.domain.SourceSystem;
import eu.dissco.orchestration.backend.domain.SourceSystemRecord;
import eu.dissco.orchestration.backend.repository.SourceSystemRepository;
import java.time.Instant;
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

  public SourceSystemRecord createSourceSystem(SourceSystem sourceSystem) throws TransformerException {
    var handle = handleService.createNewHandle();
    var sourceSystemRecord = new SourceSystemRecord(handle, Instant.now(), sourceSystem);
    repository.createSourceSystem(sourceSystemRecord);
    return sourceSystemRecord;
  }

  public SourceSystemRecord updateSourceSystem(String id, SourceSystem sourceSystem) {
    var sourceSystemRecord = new SourceSystemRecord(id, Instant.now(), sourceSystem);
    repository.updateSourceSystem(sourceSystemRecord);
    return sourceSystemRecord;
  }
}
