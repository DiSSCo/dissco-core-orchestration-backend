package eu.dissco.orchestration.backend.controller;

import static eu.dissco.orchestration.backend.domain.AgentRoleType.CREATOR;
import static eu.dissco.orchestration.backend.domain.AgentRoleType.TOMBSTONER;
import static eu.dissco.orchestration.backend.utils.ControllerUtils.getAgent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.orchestration.backend.domain.ExportType;
import eu.dissco.orchestration.backend.domain.MasScheduleData;
import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiListWrapper;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiRequestWrapper;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.exception.ForbiddenException;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.exception.ProcessingFailedException;
import eu.dissco.orchestration.backend.properties.ApplicationProperties;
import eu.dissco.orchestration.backend.schema.SourceSystemRequest;
import eu.dissco.orchestration.backend.service.SourceSystemService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
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
@RequestMapping("/source-system/v1")
@RequiredArgsConstructor
public class SourceSystemController {

  private final SourceSystemService service;
  private final ObjectMapper mapper;
  private final ApplicationProperties appProperties;

  private static String generateDownloadName(String id, ExportType exportType) {
    switch (exportType) {
      case DWC_DP -> {
        return id.toLowerCase().replace("/", "_") + "_dwc-dp.zip";
      }
      case DWCA -> {
        return id.toLowerCase().replace("/", "_") + "_dwca.zip";
      }
      default -> throw new UnsupportedOperationException(
          "This case should never happen, as the export type is validated before this method is called.");
    }
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonApiWrapper> createSourceSystem(Authentication authentication,
      @RequestBody JsonApiRequestWrapper requestBody, HttpServletRequest servletRequest)
      throws IOException, NotFoundException, ProcessingFailedException, ForbiddenException {
    var sourceSystemRequest = getSourceSystemFromRequest(requestBody);
    String path = appProperties.getBaseUrl() + servletRequest.getRequestURI();
    var agent = getAgent(authentication, CREATOR);
    log.info("Received create request for source system: {} from agent: {}", sourceSystemRequest,
        agent.getId());
    var result = service.createSourceSystem(sourceSystemRequest, agent, path);
    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  }

  @PatchMapping(value = "/{prefix}/{suffix}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonApiWrapper> updateSourceSystem(Authentication authentication,
      @PathVariable("prefix") String prefix, @PathVariable("suffix") String suffix,
      @RequestParam(name = "trigger", defaultValue = "false") boolean trigger,
      @RequestBody JsonApiRequestWrapper requestBody, HttpServletRequest servletRequest)
      throws IOException, NotFoundException, ProcessingFailedException, ForbiddenException {
    var sourceSystemRequest = getSourceSystemFromRequest(requestBody);
    var id = prefix + '/' + suffix;
    var agent = getAgent(authentication, CREATOR);
    log.info("Received update request for source system: {} from agent: {}", id, agent.getId());
    String path = appProperties.getBaseUrl() + servletRequest.getRequestURI();
    var result = service.updateSourceSystem(id, sourceSystemRequest, agent, path, trigger);
    if (result == null) {
      return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    } else {
      return ResponseEntity.ok(result);
    }
  }

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping(value = "/{prefix}/{suffix}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Void> tombstoneSourceSystem(Authentication authentication,
      @PathVariable("prefix") String prefix, @PathVariable("suffix") String suffix)
      throws NotFoundException, ProcessingFailedException, ForbiddenException {
    String id = prefix + "/" + suffix;
    var agent = getAgent(authentication, TOMBSTONER);
    log.info("Received delete request for mapping: {} from agent: {}", id, agent.getId());
    service.tombstoneSourceSystem(id, agent);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @ResponseStatus(HttpStatus.OK)
  @GetMapping(value = "/{prefix}/{suffix}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonApiWrapper> getSourceSystemById(@PathVariable("prefix") String prefix,
      @PathVariable("suffix") String suffix, HttpServletRequest servletRequest)
      throws NotFoundException {
    var id = prefix + '/' + suffix;
    log.info("Received get request for source system with id: {}", id);
    String path = appProperties.getBaseUrl() + servletRequest.getRequestURI();
    var sourceSystem = service.getSourceSystemById(id, path);
    return ResponseEntity.ok(sourceSystem);
  }

  @ResponseStatus(HttpStatus.OK)
  @GetMapping(value = "/{prefix}/{suffix}/download/{export-type}")
  public ResponseEntity<Resource> getSourceSystemDownload(@PathVariable("prefix") String prefix,
      @PathVariable("suffix") String suffix, @PathVariable("export-type") ExportType exportType)
      throws URISyntaxException, NotFoundException {
    var id = prefix + '/' + suffix;
    log.info("Received {} for source system with id: {}", exportType, id);
    var dwcDpInputStream = service.getSourceSystemDownload(id, exportType);
    var resource = new InputStreamResource(dwcDpInputStream);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("application/zip"))
        .header("Content-Disposition",
            "attachment; filename=\"" + generateDownloadName(id, exportType) + "\"")
        .body(resource);
  }

  @ResponseStatus(HttpStatus.OK)
  @PostMapping(value = "/{prefix}/{suffix}/run", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonApiWrapper> scheduleRunSourceSystemById(
      @PathVariable("prefix") String prefix, @PathVariable("suffix") String suffix,
      @RequestBody Optional<MasScheduleData> masScheduleDataOptional)
      throws ProcessingFailedException, NotFoundException {
    var id = prefix + '/' + suffix;
    MasScheduleData masScheduleData = masScheduleDataOptional.orElse(new MasScheduleData());
    log.info("Received a request to start a new run for Source System: {}", id);
    service.runSourceSystemById(id, masScheduleData);
    return ResponseEntity.accepted().build();
  }

  @GetMapping("")
  public ResponseEntity<JsonApiListWrapper> getSourceSystems(
      @RequestParam(value = "pageNumber", defaultValue = "1") int pageNum,
      @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
      HttpServletRequest servletRequest) {
    log.info("Received get request for source system with pageNumber: {} and pageSzie: {}: ",
        pageNum,
        pageSize);
    String path = appProperties.getBaseUrl() + servletRequest.getRequestURI();
    return ResponseEntity.status(HttpStatus.OK)
        .body(service.getSourceSystems(pageNum, pageSize, path));
  }

  private SourceSystemRequest getSourceSystemFromRequest(JsonApiRequestWrapper requestBody)
      throws JsonProcessingException, IllegalArgumentException {
    if (!requestBody.data().type().equals(ObjectType.SOURCE_SYSTEM)) {
      log.warn("Incorrect type for this endpoint: {}", requestBody.data().type());
      throw new IllegalArgumentException();
    }
    var request = mapper.treeToValue(requestBody.data().attributes(), SourceSystemRequest.class);
    if (request.getSchemaName() == null || request.getSchemaUrl() == null
        || request.getOdsTranslatorType() == null || request.getOdsDataMappingID() == null) {
      log.warn("Missing required field for source system creation");
      throw new IllegalArgumentException("Missing required field for source system creation");
    }
    return request;
  }
}
