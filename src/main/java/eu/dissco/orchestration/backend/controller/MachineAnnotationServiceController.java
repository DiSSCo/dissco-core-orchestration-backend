package eu.dissco.orchestration.backend.controller;

import static eu.dissco.orchestration.backend.domain.AgentRoleType.CREATOR;
import static eu.dissco.orchestration.backend.domain.AgentRoleType.TOMBSTONER;
import static eu.dissco.orchestration.backend.utils.ControllerUtils.getAgent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiListWrapper;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiRequestWrapper;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.exception.ForbiddenException;
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
@RequestMapping("/mas/v1")
public class MachineAnnotationServiceController {

  private final MachineAnnotationServiceService service;
  private final ObjectMapper mapper;
  private final ApplicationProperties appProperties;

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonApiWrapper> createMachineAnnotationService(
      Authentication authentication,
      @RequestBody JsonApiRequestWrapper requestBody, HttpServletRequest servletRequest)
      throws JsonProcessingException, ProcessingFailedException, ForbiddenException {
    var machineAnnotationService = getMachineAnnotation(requestBody);
    var agent = getAgent(authentication, CREATOR);
    log.info("Received create request for machine annotation service: {} from agent: {}",
        machineAnnotationService, agent.getId());
    String path = appProperties.getBaseUrl() + servletRequest.getRequestURI();
    var result = service.createMachineAnnotationService(machineAnnotationService, agent, path);
    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  }

  @PatchMapping(value = "/{prefix}/{suffix}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonApiWrapper> updateMachineAnnotationService(
      Authentication authentication,
      @PathVariable("prefix") String prefix, @PathVariable("suffix") String suffix,
      @RequestBody JsonApiRequestWrapper requestBody, HttpServletRequest servletRequest)
      throws JsonProcessingException, NotFoundException, ProcessingFailedException, ForbiddenException {
    var machineAnnotationService = getMachineAnnotation(requestBody);
    var agent = getAgent(authentication, CREATOR);
    var id = prefix + '/' + suffix;
    log.info("Received update request for machine annotation service: {} from agent: {}", id,
        agent.getId());
    String path = appProperties.getBaseUrl() + servletRequest.getRequestURI();
    var result = service.updateMachineAnnotationService(id, machineAnnotationService, agent, path);
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
      throws NotFoundException, ProcessingFailedException, ForbiddenException {
    String id = prefix + "/" + suffix;
    var agent = getAgent(authentication, TOMBSTONER);
    log.info("Received delete request for machine annotation service: {} from agent: {}", id,
        agent.getId());
    service.tombstoneMachineAnnotationService(id, agent);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @ResponseStatus(HttpStatus.OK)
  @GetMapping(value = "/{prefix}/{suffix}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonApiWrapper> getMachineAnnotationService(
      @PathVariable("prefix") String prefix, @PathVariable("suffix") String suffix,
      HttpServletRequest servletRequest) throws NotFoundException {
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
      log.warn("Incorrect type for this endpoint: {}", requestBody.data().type());
      throw new IllegalArgumentException();
    }
    var request = mapper.treeToValue(requestBody.data().attributes(),
        MachineAnnotationServiceRequest.class);
    if (request.getSchemaName() == null || request.getOdsContainerImage() == null || request.getOdsHasTargetDigitalObjectFilter() == null){
      log.warn("Missing required field for MAS creation");
      throw new IllegalArgumentException("Missing required field for MAS creation");
    }
    return request;
  }

}
