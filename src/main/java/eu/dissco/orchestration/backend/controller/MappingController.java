package eu.dissco.orchestration.backend.controller;

import eu.dissco.orchestration.backend.domain.Mapping;
import eu.dissco.orchestration.backend.domain.MappingRecord;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.service.MappingService;
import javax.servlet.http.HttpServletRequest;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/mapping")
@RequiredArgsConstructor
public class MappingController {

  private final MappingService service;
  private static final String SANDBOX_URI = "https://sandbox.dissco.tech/";

  @PreAuthorize("isAuthenticated()")
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<MappingRecord> createMapping(Authentication authentication,
      @RequestBody Mapping mapping) throws TransformerException {
    log.info("Received create request for mapping: {}", mapping);
    var result = service.createMapping(mapping, getNameFromToken(authentication));
    return ResponseEntity.ok(result);
  }

  @PreAuthorize("isAuthenticated()")
  @PatchMapping(value = "/{prefix}/{suffix}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<MappingRecord> updateSourceSystem(Authentication authentication,
      @PathVariable("prefix") String prefix, @PathVariable("suffix") String suffix,
      @RequestBody Mapping mapping) {
    var id = prefix + '/' + suffix;
    log.info("Received update request for mapping: {}", id);
    var result = service.updateMapping(id, mapping, getNameFromToken(authentication));
    return ResponseEntity.ok(result);
  }

  @GetMapping(value = "")
  public ResponseEntity<JsonApiWrapper> getMappings(
      @RequestParam(value = "pageNum", defaultValue = "0") int pageNum,
      @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
      HttpServletRequest r
  ){
    String path = SANDBOX_URI + r.getRequestURI();
    return ResponseEntity.status(HttpStatus.OK).body(service.getMappings(pageNum, pageSize, path));
  }

  private String getNameFromToken(Authentication authentication) {
    KeycloakPrincipal<? extends KeycloakSecurityContext> principal =
        (KeycloakPrincipal<?>) authentication.getPrincipal();
    AccessToken token = principal.getKeycloakSecurityContext().getToken();
    return token.getSubject();
  }

}
