package eu.dissco.orchestration.backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.orchestration.backend.domain.HandleType;
import eu.dissco.orchestration.backend.domain.Mapping;
import eu.dissco.orchestration.backend.domain.jsonapi.*;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiListWrapper;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiRequestWrapper;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.service.MappingService;
import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import javax.xml.transform.TransformerException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
  private static final String SANDBOX_URI = "https://sandbox.dissco.tech/orchestrator";
  private final ObjectMapper mapper;
  private static final ArrayList<String> sourceDataSystems = new ArrayList<>(List.of("dwc", "abcd", "abcdefg"));

  @PreAuthorize("isAuthenticated()")
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonApiWrapper> createMapping(Authentication authentication,
      @RequestBody JsonApiRequestWrapper requestBody, HttpServletRequest request)
      throws TransformerException, JsonProcessingException {
    var mapping = getMappingFromRequest(requestBody);
    log.info("Received create request for mapping: {}", mapping);
    String path = SANDBOX_URI + request.getRequestURI();
    var result = service.createMapping(mapping, getNameFromToken(authentication), path);
    return ResponseEntity.status(HttpStatus.CREATED).body(result);
  }

  @PatchMapping(value = "/{prefix}/{suffix}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonApiWrapper> updateMapping(Authentication authentication,
      @PathVariable("prefix") String prefix, @PathVariable("suffix") String suffix,
      @RequestBody JsonApiRequestWrapper requestBody, HttpServletRequest request)
      throws JsonProcessingException, NotFoundException {
    var mapping = getMappingFromRequest(requestBody);
    var id = prefix + '/' + suffix;
    log.info("Received update request for mapping: {}", id);
    String path = SANDBOX_URI + request.getRequestURI();
    var result = service.updateMapping(id, mapping, getNameFromToken(authentication), path);
    return ResponseEntity.ok(result);
  }

  @PreAuthorize("isAuthenticated()")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping(value = "/{prefix}/{postfix}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Void> deleteMapping(
      @PathVariable("prefix") String prefix, @PathVariable("postfix") String postfix) throws NotFoundException {
    String id = prefix + "/" + postfix;
    service.deleteMapping(id);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @ResponseStatus(HttpStatus.OK)
  @GetMapping(value = "/{prefix}/{postfix}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<JsonApiWrapper> getMappingById(@PathVariable("prefix") String prefix,
      @PathVariable("postfix") String postfix, HttpServletRequest request) {
    var id = prefix + '/' + postfix;
    log.info("Received get request for mapping with id: {}", id);
    String path = SANDBOX_URI + request.getRequestURI();
    var mapping = service.getMappingById(id, path);
    return ResponseEntity.ok(mapping);
  }

  @GetMapping(value = "")
  public ResponseEntity<JsonApiListWrapper> getMappings(
      @RequestParam(value = "pageNumber", defaultValue = "1") int pageNum,
      @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
      HttpServletRequest r
  ){
    String path = SANDBOX_URI + r.getRequestURI();
    return ResponseEntity.status(HttpStatus.OK).body(service.getMappings(pageNum, pageSize, path));
  }

  private Mapping getMappingFromRequest(JsonApiRequestWrapper requestBody)
      throws JsonProcessingException, IllegalArgumentException {
    if(!requestBody.data().type().equals(HandleType.MAPPING)){
      throw new IllegalArgumentException();
    }
    var mapping = mapper.treeToValue(requestBody.data().attributes(), Mapping.class);
    checkSourceStandard(mapping);
    return mapping;
  }

  private String getNameFromToken(Authentication authentication) {

    KeycloakPrincipal<? extends KeycloakSecurityContext> principal =
        (KeycloakPrincipal<?>) authentication.getPrincipal();
    AccessToken token = principal.getKeycloakSecurityContext().getToken();
    return token.getSubject();
  }

  private void checkSourceStandard(Mapping mapping){
    if (!sourceDataSystems.contains(mapping.sourceDataStandard())){
      throw new IllegalArgumentException("Invalid source data standard" + mapping.sourceDataStandard());
    }

  }

}
