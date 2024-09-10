package eu.dissco.orchestration.backend.controller;

import static eu.dissco.orchestration.backend.utils.ControllerUtils.getAgent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiListWrapper;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiRequestWrapper;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.exception.ProcessingFailedException;
import eu.dissco.orchestration.backend.properties.ApplicationProperties;
import eu.dissco.orchestration.backend.schema.MachineAnnotationServiceRequest;
import eu.dissco.orchestration.backend.service.MachineAnnotationServiceService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequiredArgsConstructor
@RequestMapping("/mas")
public class MachineAnnotationServiceController {

  private final MachineAnnotationServiceService service;
  private final ObjectMapper mapper;
  private final ApplicationProperties appProperties;

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonApiWrapper> createMachineAnnotationService(
      Authentication authentication,
      @RequestBody JsonApiRequestWrapper requestBody, HttpServletRequest servletRequest)
      throws JsonProcessingException, ProcessingFailedException {
    var machineAnnotationService = getMachineAnnotation(requestBody);
    var userId = getAgent(authentication).getId();
    log.info("Received create request for machine annotation service: {} from user: {}",
        machineAnnotationService, userId);
    String path = appProperties.getBaseUrl() + servletRequest.getRequestURI();
    var result = service.createMachineAnnotationService(machineAnnotationService, userId, path);
    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  }

  @PatchMapping(value = "/{prefix}/{suffix}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonApiWrapper> updateMachineAnnotationService(
      Authentication authentication,
      @PathVariable("prefix") String prefix, @PathVariable("suffix") String suffix,
      @RequestBody JsonApiRequestWrapper requestBody, HttpServletRequest servletRequest)
      throws JsonProcessingException, NotFoundException, ProcessingFailedException {
    var machineAnnotationService = getMachineAnnotation(requestBody);
    var userId = getAgent(authentication).getId();
    var id = prefix + '/' + suffix;
    log.info("Received update request for machine annotation service: {} from user: {}", id,
        userId);
    String path = appProperties.getBaseUrl() + servletRequest.getRequestURI();
    var result = service.updateMachineAnnotationService(id, machineAnnotationService, userId, path);
    if (result == null) {
      return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    } else {
      return ResponseEntity.ok(result);
    }
  }

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping(value = "/{prefix}/{suffix}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Void> tombstoneMachineAnnotationService(Authentication authentication,
      @PathVariable("prefix") String prefix, @PathVariable("suffix") String suffix)
      throws NotFoundException, ProcessingFailedException {
    String id = prefix + "/" + suffix;
    var agent = getAgent(authentication);
    log.info("Received delete request for machine annotation service: {} from user: {}", id,
        agent.getId());
    service.tombstoneMachineAnnotationService(id, agent);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @ResponseStatus(HttpStatus.OK)
  @GetMapping(value = "/{prefix}/{suffix}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonApiWrapper> getMachineAnnotationService(
      @PathVariable("prefix") String prefix, @PathVariable("suffix") String suffix,
      HttpServletRequest servletRequest) {
    var id = prefix + '/' + suffix;
    log.info("Received get request for machine annotation service with id: {}", id);
    String path = appProperties.getBaseUrl() + servletRequest.getRequestURI();
    var result = service.getMachineAnnotationService(id, path);
    return ResponseEntity.ok(result);
  }

  @GetMapping("")
  public ResponseEntity<JsonApiListWrapper> getMachineAnnotationServices(
      @RequestParam(value = "pageNumber", defaultValue = "1") int pageNum,
      @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
      HttpServletRequest servletRequest) {
    log.info("Received get request for machine annotation services");
    String path = appProperties.getBaseUrl() + servletRequest.getRequestURI();
    return ResponseEntity.status(HttpStatus.OK)
        .body(service.getMachineAnnotationServices(pageNum, pageSize, path));
  }

  private MachineAnnotationServiceRequest getMachineAnnotation(JsonApiRequestWrapper requestBody)
      throws JsonProcessingException, IllegalArgumentException {
    if (!requestBody.data().type().equals(ObjectType.MAS)) {
      log.error("Incorrect type for this endpoint: {}", requestBody.data().type());
      throw new IllegalArgumentException();
    }
    return mapper.treeToValue(requestBody.data().attributes(),
        MachineAnnotationServiceRequest.class);
  }

}
