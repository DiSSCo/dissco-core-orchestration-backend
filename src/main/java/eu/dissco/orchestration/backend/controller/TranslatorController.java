package eu.dissco.orchestration.backend.controller;

import eu.dissco.orchestration.backend.domain.TranslatorRequest;
import eu.dissco.orchestration.backend.domain.TranslatorResponse;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.service.TranslatorService;
import freemarker.template.TemplateException;
import io.kubernetes.client.openapi.ApiException;
import java.io.IOException;
import java.util.List;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/translator")
@RequiredArgsConstructor
public class TranslatorController {

  private final TranslatorService service;

  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<TranslatorResponse>> getAll() {
    try {
      return ResponseEntity.ok(service.getAll());
    } catch (ApiException e) {
      log.error("Application threw error with message: {}", e.getResponseBody());
      return new ResponseEntity<>(HttpStatus.valueOf(e.getCode()));
    }
  }

  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<TranslatorResponse> get(@PathVariable String id) {
    try {
      var optionalResponse = service.get(id);
      return optionalResponse.map(ResponseEntity::ok)
          .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    } catch (ApiException e) {
      log.error("Application threw error with message: {}", e.getResponseBody());
      return new ResponseEntity<>(HttpStatus.valueOf(e.getCode()));
    }
  }

  @PreAuthorize("isAuthenticated()")
  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<TranslatorResponse> scheduleTranslator(Authentication authentication,
      @RequestBody TranslatorRequest request)
      throws TemplateException, IOException {
    try {
      log.info("Received new post request: {} by user: {}", request,
          getNameFromToken(authentication));
      var result = service.createTranslator(request);
      return new ResponseEntity<>(result, HttpStatus.CREATED);
    } catch (ApiException e) {
      return new ResponseEntity<>(HttpStatus.valueOf(e.getCode()));
    }
  }

  private String getNameFromToken(Authentication authentication) {
    KeycloakPrincipal<? extends KeycloakSecurityContext> principal =
        (KeycloakPrincipal<?>) authentication.getPrincipal();
    AccessToken token = principal.getKeycloakSecurityContext().getToken();
    return token.getPreferredUsername();
  }

  @PreAuthorize("isAuthenticated()")
  @DeleteMapping(value = "/{id}")
  public ResponseEntity<Void> deleteTranslator(Authentication authentication,
      @PathVariable String id) {
    log.info("Received delete request for job: {} from user: {}", id,
        getNameFromToken(authentication));
    try {
      service.deleteJob(id);
      return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    } catch (ApiException e) {
      log.error("Application threw error with message: {}", e.getResponseBody());
      return new ResponseEntity<>(HttpStatus.valueOf(e.getCode()));
    } catch (NotFoundException e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }
}
