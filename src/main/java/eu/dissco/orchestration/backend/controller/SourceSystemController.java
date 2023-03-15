package eu.dissco.orchestration.backend.controller;

import eu.dissco.orchestration.backend.domain.SourceSystem;
import eu.dissco.orchestration.backend.domain.SourceSystemRecord;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiListWrapper;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.service.SourceSystemService;
import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.TransformerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("orchestrator/source-system")
@RequiredArgsConstructor
public class SourceSystemController {
  private final SourceSystemService service;
  private static final String SANDBOX_URI = "https://sandbox.dissco.tech/orchestrator";

  @PreAuthorize("isAuthenticated()")
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonApiWrapper> createSourceSystem(Authentication authentication,
      @RequestBody SourceSystem request, HttpServletRequest servletRequest) throws TransformerException {
    String path = SANDBOX_URI + servletRequest.getRequestURI();
    log.info("Received create request for request: {}", request);
    var result = service.createSourceSystem(request, path);
    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  }

  @PreAuthorize("isAuthenticated()")
  @PatchMapping(value = "/{prefix}/{suffix}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonApiWrapper> updateSourceSystem(Authentication authentication,
      @PathVariable("prefix") String prefix, @PathVariable("suffix") String suffix,
      @RequestBody SourceSystem sourceSystem, HttpServletRequest servletRequest) {
    var id = prefix + '/' + suffix;
    log.info("Received update request for source system: {}", id);
    String path = SANDBOX_URI + servletRequest.getRequestURI();
    var result = service.updateSourceSystem(id, sourceSystem, path);
    return ResponseEntity.ok(result);
  }

  @ResponseStatus(HttpStatus.OK)
  @GetMapping(value = "/{prefix}/{postfix}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonApiWrapper> getSourceSystemById(
      @PathVariable("prefix") String prefix,
      @PathVariable("postfix") String postfix, HttpServletRequest servletRequest) {
    var id = prefix + '/' + postfix;
    log.info("Received get request for source system with id: {}", id);
    String path = SANDBOX_URI + servletRequest.getRequestURI();
    var sourceSystem = service.getSourceSystemById(id, path);
    return ResponseEntity.ok(sourceSystem);
  }

  @GetMapping("")
  public ResponseEntity<JsonApiListWrapper> getSourceSystems(
      @RequestParam(value = "pageNumber", defaultValue = "0") int pageNum,
      @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
      HttpServletRequest r
  ){
    String path = SANDBOX_URI + r.getRequestURI();
    return ResponseEntity.status(HttpStatus.OK).body(service.getSourceSystemRecords(pageNum, pageSize, path));
  }

}
