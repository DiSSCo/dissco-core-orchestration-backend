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
import eu.dissco.orchestration.backend.domain.ExportType;
import eu.dissco.orchestration.backend.domain.MasScheduleData;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiListWrapper;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.domain.openapi.sourcesystem.SourceSystemRequestSchema;
import eu.dissco.orchestration.backend.domain.openapi.sourcesystem.SourceSystemResponseList;
import eu.dissco.orchestration.backend.domain.openapi.sourcesystem.SourceSystemResponseSingle;
import eu.dissco.orchestration.backend.exception.ForbiddenException;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.exception.ProcessingFailedException;
import eu.dissco.orchestration.backend.properties.ApplicationProperties;
import eu.dissco.orchestration.backend.service.SourceSystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URISyntaxException;
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

  @Operation(
      summary = "Create a source system",
      description = """
          Create a source system from which to ingest data.
          User must have orchestration admin rights.
          """
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Source system successfully created", content = {
          @Content(mediaType = "application/json", schema = @Schema(implementation = SourceSystemResponseSingle.class))
      })
  })
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonApiWrapper> createSourceSystem(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          description = "Data mapping request adhering to JSON:API standard",
          content = @Content(mediaType = "application/json",
              schema = @Schema(implementation = SourceSystemRequestSchema.class)))
      Authentication authentication,
      @RequestBody SourceSystemRequestSchema requestBody, HttpServletRequest servletRequest)
      throws NotFoundException, ProcessingFailedException, ForbiddenException {
    var sourceSystemRequest = requestBody.data().attributes();
    String path = appProperties.getBaseUrl() + servletRequest.getRequestURI();
    var agent = getAgent(authentication, CREATOR);
    log.info("Received create request for source system: {} from agent: {}", sourceSystemRequest,
        agent.getId());
    var result = service.createSourceSystem(sourceSystemRequest, agent, path);
    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  }

  @Operation(
      summary = "Update a source system",
      description = """
          Update an existing source system.
          User must have orchestration admin rights.
          """
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Source system successfully updated", content = {
          @Content(mediaType = "application/json", schema = @Schema(implementation = SourceSystemResponseSingle.class))
      })
  })
  @ResponseStatus(HttpStatus.OK)
  @PatchMapping(value = "/{prefix}/{suffix}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonApiWrapper> updateSourceSystem(Authentication authentication,
      @Parameter(description = PREFIX_OAS) @PathVariable("prefix") String prefix,
      @Parameter(description = SUFFIX_OAS) @PathVariable("suffix") String suffix,
      @Parameter(description = "Trigger ingestion immediately") @RequestParam(name = "trigger", defaultValue = "false") boolean trigger,
      @RequestBody SourceSystemRequestSchema requestBody, HttpServletRequest servletRequest)
      throws NotFoundException, ProcessingFailedException, ForbiddenException {
    var sourceSystemRequest = requestBody.data().attributes();
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

  @Operation(
      summary = "Tombstone a source system",
      description = """
          Tombstone an existing source system.
          Source system will no longer ingest new/update existing data. Existing data will remain unchanged. 
          User must have orchestration admin rights.
          """
  )
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping(value = "/{prefix}/{suffix}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Void> tombstoneSourceSystem(Authentication authentication,
      @Parameter(description = PREFIX_OAS) @PathVariable("prefix") String prefix,
      @Parameter(description = SUFFIX_OAS) @PathVariable("suffix") String suffix)
      throws NotFoundException, ProcessingFailedException, ForbiddenException {
    String id = prefix + "/" + suffix;
    var agent = getAgent(authentication, TOMBSTONER);
    log.info("Received delete request for mapping: {} from agent: {}", id, agent.getId());
    service.tombstoneSourceSystem(id, agent);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Operation(
      summary = "Retrieve a source system",
      description = """
          Retrieve an existing source system, based on ID.
          """
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Source system successfully retrieved", content = {
          @Content(mediaType = "application/json", schema = @Schema(implementation = SourceSystemResponseSingle.class))
      })
  })
  @ResponseStatus(HttpStatus.OK)
  @GetMapping(value = "/{prefix}/{suffix}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonApiWrapper> getSourceSystemById(
      @Parameter(description = PREFIX_OAS) @PathVariable("prefix") String prefix,
      @Parameter(description = SUFFIX_OAS) @PathVariable("suffix") String suffix,
      HttpServletRequest servletRequest)
      throws NotFoundException {
    var id = prefix + '/' + suffix;
    log.info("Received get request for source system with id: {}", id);
    String path = appProperties.getBaseUrl() + servletRequest.getRequestURI();
    var sourceSystem = service.getSourceSystemById(id, path);
    return ResponseEntity.ok(sourceSystem);
  }

  @Operation(
      summary = "Retrieve an export of data a given source system",
      description = """
          Retrieve data from a given source system. Exports are downloadable from an AWS S3 bucket.
          """
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Source system download successfully retrieved", content = {
      })
  })
  @ResponseStatus(HttpStatus.OK)
  @GetMapping(value = "/{prefix}/{suffix}/download/{export-type}")
  public ResponseEntity<Resource> getSourceSystemDownload(
      @Parameter(description = PREFIX_OAS) @PathVariable("prefix") String prefix,
      @Parameter(description = SUFFIX_OAS) @PathVariable("suffix") String suffix,
      @PathVariable("export-type") ExportType exportType)
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

  @Operation(
      summary = "Run a source system ingestion",
      description = """
          Run a source system ingestion. Data will be retrieved from source system endpoint.
          Optional: May choose to apply additional MASs on objects in source system.
          """
  )
  @ResponseStatus(HttpStatus.OK)
  @PostMapping(value = "/{prefix}/{suffix}/run", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Void> scheduleRunSourceSystemById(
      @Parameter(description = PREFIX_OAS) @PathVariable("prefix") String prefix,
      @Parameter(description = SUFFIX_OAS) @PathVariable("suffix") String suffix,
      @RequestBody Optional<MasScheduleData> masScheduleDataOptional)
      throws ProcessingFailedException, NotFoundException {
    var id = prefix + '/' + suffix;
    var masScheduleData = masScheduleDataOptional.orElse(new MasScheduleData());
    log.info("Received a request to start a new run for Source System: {}", id);
    service.runSourceSystemById(id, masScheduleData);
    return ResponseEntity.accepted().build();
  }

  @Operation(
      summary = "Retrieve source systems",
      description = """
          Retrieve a paginated list of source systems.
          """
  )
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Source systems successfully retrieved", content = {
          @Content(mediaType = "application/json", schema = @Schema(implementation = SourceSystemResponseList.class))
      })
  })
  @ResponseStatus(HttpStatus.OK)
  @GetMapping("")
  public ResponseEntity<JsonApiListWrapper> getSourceSystems(
      @Parameter(description = PAGE_NUM_OAS) @RequestParam(value = "pageNumber", defaultValue = DEFAULT_PAGE_NUM) int pageNum,
      @Parameter(description = PAGE_SIZE_OAS) @RequestParam(value = "pageSize", defaultValue = DEFAULT_PAGE_SIZE) int pageSize,
      HttpServletRequest servletRequest) {
    log.info("Received get request for source system with pageNumber: {} and pageSzie: {}: ",
        pageNum, pageSize);
    String path = appProperties.getBaseUrl() + servletRequest.getRequestURI();
    return ResponseEntity.status(HttpStatus.OK)
        .body(service.getSourceSystems(pageNum, pageSize, path));
  }

}
