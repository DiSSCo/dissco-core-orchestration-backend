package eu.dissco.orchestration.backend.controller;

import static eu.dissco.orchestration.backend.domain.AgentRoleType.CREATOR;
import static eu.dissco.orchestration.backend.domain.AgentRoleType.TOMBSTONER;
import static eu.dissco.orchestration.backend.utils.ControllerUtils.DEFAULT_PAGE_NUM;
import static eu.dissco.orchestration.backend.utils.ControllerUtils.DEFAULT_PAGE_SIZE;
import static eu.dissco.orchestration.backend.utils.ControllerUtils.PAGE_NUM_OAS;
import static eu.dissco.orchestration.backend.utils.ControllerUtils.PAGE_SIZE_OAS;
import static eu.dissco.orchestration.backend.utils.ControllerUtils.PREFIX_OAS;
import static eu.dissco.orchestration.backend.utils.ControllerUtils.SUFFIX_OAS;
import static eu.dissco.orchestration.backend.utils.ControllerUtils.getAgent;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiListWrapper;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.domain.openapi.datamapping.DataMappingResponseSingle;
import eu.dissco.orchestration.backend.domain.openapi.mas.MasRequestSchema;
import eu.dissco.orchestration.backend.domain.openapi.mas.MasResponseList;
import eu.dissco.orchestration.backend.domain.openapi.mas.MasResponseSingle;
import eu.dissco.orchestration.backend.exception.ForbiddenException;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.exception.ProcessingFailedException;
import eu.dissco.orchestration.backend.properties.ApplicationProperties;
import eu.dissco.orchestration.backend.service.MachineAnnotationServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

  @Operation(
      summary = "Create a MAS",
      description = """
          Create a Machine Annotation Service (MAS).
          User must have orchestration admin rights.
          """
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "MAS successfully created", content = {
          @Content(mediaType = "application/json", schema = @Schema(implementation = MasResponseSingle.class))
      })
  })
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonApiWrapper> createMachineAnnotationService(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "MAS request adhering to JSON:API standard",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = MasRequestSchema.class)))
      Authentication authentication,
      @RequestBody MasRequestSchema requestBody, HttpServletRequest servletRequest)
      throws ProcessingFailedException, ForbiddenException {
    var machineAnnotationService = requestBody.data().attributes();
    var agent = getAgent(authentication, CREATOR);
    log.info("Received create request for machine annotation service: {} from agent: {}",
        machineAnnotationService, agent.getId());
    String path = appProperties.getBaseUrl() + servletRequest.getRequestURI();
    var result = service.createMachineAnnotationService(machineAnnotationService, agent, path);
    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  }

  @Operation(
      summary = "Update a MAS",
      description = """
          Update an existing MAS.
          User must have orchestration admin rights.
          """
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "MAS successfully updated", content = {
          @Content(mediaType = "application/json", schema = @Schema(implementation = MasResponseSingle.class))
      })
  })
  @ResponseStatus(HttpStatus.OK)
  @PatchMapping(value = "/{prefix}/{suffix}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonApiWrapper> updateMachineAnnotationService(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "Data mapping request adhering to JSON:API standard",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = MasRequestSchema.class)))
      Authentication authentication,
      @Parameter(description = PREFIX_OAS) @PathVariable("prefix") String prefix,
      @Parameter(description = SUFFIX_OAS) @PathVariable("suffix") String suffix,
      @RequestBody MasRequestSchema requestBody, HttpServletRequest servletRequest)
      throws NotFoundException, ProcessingFailedException, ForbiddenException {
    var machineAnnotationService = requestBody.data().attributes();
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

  @Operation(
      summary = "Tombstone a MAS",
      description = """
          Tombstone an existing machine annotation service (MAS).
          MAS will no longer be able to be scheduled.
          User must have orchestration admin rights.
          """
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Source system successfully tombstoned", content = {
          @Content(mediaType = "application/json", schema = @Schema(implementation = DataMappingResponseSingle.class))
      })
  })
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping(value = "/{prefix}/{suffix}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Void> tombstoneMachineAnnotationService(Authentication authentication,
      @Parameter(description = PREFIX_OAS) @PathVariable("prefix") String prefix,
      @Parameter(description = SUFFIX_OAS) @PathVariable("suffix") String suffix)
      throws NotFoundException, ProcessingFailedException, ForbiddenException {
    String id = prefix + "/" + suffix;
    var agent = getAgent(authentication, TOMBSTONER);
    log.info("Received delete request for machine annotation service: {} from agent: {}", id,
        agent.getId());
    service.tombstoneMachineAnnotationService(id, agent);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Operation(
      summary = "Retrieve a MAS",
      description = """
          Retrieve an existing Machine Annotation Service, based on ID.
          """
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "MAS successfully retrieved", content = {
          @Content(mediaType = "application/json", schema = @Schema(implementation = MasResponseSingle.class))
      })
  })
  @ResponseStatus(HttpStatus.OK)
  @GetMapping(value = "/{prefix}/{suffix}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonApiWrapper> getMachineAnnotationService(
      @Parameter(description = PREFIX_OAS) @PathVariable("prefix") String prefix,
      @Parameter(description = SUFFIX_OAS) @PathVariable("suffix") String suffix,
      HttpServletRequest servletRequest) throws NotFoundException {
    var id = prefix + '/' + suffix;
    log.info("Received get request for machine annotation service with id: {}", id);
    String path = appProperties.getBaseUrl() + servletRequest.getRequestURI();
    var result = service.getMachineAnnotationService(id, path);
    return ResponseEntity.ok(result);
  }

  @Operation(
      summary = "Retrieve MASs",
      description = """
          Retrieve a paginated list of Machine Annotation Services.
          """
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "MASs successfully retrieved", content = {
          @Content(mediaType = "application/json", schema = @Schema(implementation = MasResponseList.class))
      })
  })
  @ResponseStatus(HttpStatus.OK)
  @GetMapping("")
  public ResponseEntity<JsonApiListWrapper> getMachineAnnotationServices(
      @Parameter(description = PAGE_NUM_OAS) @RequestParam(value = "pageNumber", defaultValue = DEFAULT_PAGE_NUM) int pageNum,
      @Parameter(description = PAGE_SIZE_OAS) @RequestParam(value = "pageSize", defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
      HttpServletRequest servletRequest) {
    log.info("Received get request for machine annotation services");
    String path = appProperties.getBaseUrl() + servletRequest.getRequestURI();
    return ResponseEntity.status(HttpStatus.OK)
        .body(service.getMachineAnnotationServices(pageNum, pageSize, path));
  }

}
