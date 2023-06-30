package eu.dissco.orchestration.backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.orchestration.backend.domain.HandleType;
import eu.dissco.orchestration.backend.domain.Mapping;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiListWrapper;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiRequestWrapper;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.properties.ApplicationProperties;
import eu.dissco.orchestration.backend.service.MappingService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
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
@RequestMapping("/mapping")
@RequiredArgsConstructor
public class MappingController {

  private final MappingService service;
  private final ObjectMapper mapper;
  private final ApplicationProperties appProperties;
  private static final ArrayList<String> SOURCE_DATA_SYSTEMS = new ArrayList<>(
      List.of("dwc", "abcd", "abcdefg"));

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonApiWrapper> createMapping(Authentication authentication,
      @RequestBody JsonApiRequestWrapper requestBody, HttpServletRequest servletRequest)
      throws JsonProcessingException {
    var mapping = getMappingFromRequest(requestBody);
    var userId = authentication.getName();
    log.info("Received create request for mapping: {} from user: {}", mapping, userId);
    String path = appProperties.getBaseUrl() + servletRequest.getRequestURI();
    var result = service.createMapping(mapping, userId, path);
    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  }

  @PatchMapping(value = "/{prefix}/{suffix}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonApiWrapper> updateMapping(Authentication authentication,
      @PathVariable("prefix") String prefix, @PathVariable("suffix") String suffix,
      @RequestBody JsonApiRequestWrapper requestBody, HttpServletRequest servletRequest)
      throws JsonProcessingException, NotFoundException {
    var mapping = getMappingFromRequest(requestBody);
    var id = prefix + '/' + suffix;
    var userId = authentication.getName();
    log.info("Received update request for mapping: {} from user: {}", mapping, userId);
    String path = appProperties.getBaseUrl() + servletRequest.getRequestURI();
    var result = service.updateMapping(id, mapping, userId, path);
    if (result == null) {
      return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    } else {
      return ResponseEntity.ok(result);
    }
  }

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping(value = "/{prefix}/{suffix}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Void> deleteMapping(Authentication authentication,
      @PathVariable("prefix") String prefix, @PathVariable("suffix") String suffix)
      throws NotFoundException {
    String id = prefix + "/" + suffix;
    log.info("Received delete request for mapping: {} from user: {}", id,
        authentication.getName());
    service.deleteMapping(id);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @ResponseStatus(HttpStatus.OK)
  @GetMapping(value = "/{prefix}/{suffix}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonApiWrapper> getMappingById(@PathVariable("prefix") String prefix,
      @PathVariable("suffix") String suffix, HttpServletRequest servletRequest) {
    var id = prefix + '/' + suffix;
    log.info("Received get request for mapping with id: {}", id);
    String path = appProperties.getBaseUrl() + servletRequest.getRequestURI();
    var mapping = service.getMappingById(id, path);
    return ResponseEntity.ok(mapping);
  }

  @GetMapping(value = "")
  public ResponseEntity<JsonApiListWrapper> getMappings(
      @RequestParam(value = "pageNumber", defaultValue = "1") int pageNum,
      @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
      HttpServletRequest servletRequest) {
    log.info("Received get request for mappings with pageNumber: {} and pageSzie: {}: ", pageNum,
        pageSize);
    String path = appProperties.getBaseUrl() + servletRequest.getRequestURI();
    return ResponseEntity.status(HttpStatus.OK).body(service.getMappings(pageNum, pageSize, path));
  }

  private Mapping getMappingFromRequest(JsonApiRequestWrapper requestBody)
      throws JsonProcessingException, IllegalArgumentException {
    if (!requestBody.data().type().equals(HandleType.MAPPING)) {
      throw new IllegalArgumentException();
    }
    var mapping = mapper.treeToValue(requestBody.data().attributes(), Mapping.class);
    checkSourceStandard(mapping);
    return mapping;
  }

  private void checkSourceStandard(Mapping mapping) {
    if (!SOURCE_DATA_SYSTEMS.contains(mapping.sourceDataStandard())) {
      throw new IllegalArgumentException(
          "Invalid source data standard" + mapping.sourceDataStandard());
    }
  }

}
