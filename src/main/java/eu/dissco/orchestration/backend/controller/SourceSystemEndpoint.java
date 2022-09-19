package eu.dissco.orchestration.backend.controller;

import eu.dissco.orchestration.backend.domain.SourceSystem;
import eu.dissco.orchestration.backend.domain.SourceSystemRecord;
import eu.dissco.orchestration.backend.service.SourceSystemService;
import javax.xml.transform.TransformerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/source-system")
@RequiredArgsConstructor
public class SourceSystemEndpoint {

  private final SourceSystemService service;

  @PreAuthorize("isAuthenticated()")
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SourceSystemRecord> createSourceSystem(Authentication authentication,
      @RequestBody SourceSystem request) throws TransformerException {
    log.info("Received create request for request: {}", request);
    var result = service.createSourceSystem(request);
    return ResponseEntity.ok(result);
  }

  @PreAuthorize("isAuthenticated()")
  @PatchMapping(value = "/{prefix}/{suffix}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<SourceSystemRecord> updateSourceSystem(Authentication authentication,
      @PathVariable("prefix") String prefix, @PathVariable("suffix") String suffix,
      @RequestBody SourceSystem sourceSystem) {
    var id = prefix + '/' + suffix;
    log.info("Received update request for source system: {}", id);
    var result = service.updateSourceSystem(id, sourceSystem);
    return ResponseEntity.ok(result);
  }

}
