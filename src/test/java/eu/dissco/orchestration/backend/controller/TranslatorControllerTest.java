package eu.dissco.orchestration.backend.controller;

import static eu.dissco.orchestration.backend.TestUtils.givenRequest;
import static eu.dissco.orchestration.backend.TestUtils.givenResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;

import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.service.TranslatorService;
import io.kubernetes.client.openapi.ApiException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class TranslatorControllerTest {

  @Mock
  private TranslatorService service;

  @Mock
  private KeycloakPrincipal<KeycloakSecurityContext> principal;

  @Mock
  private KeycloakSecurityContext securityContext;

  @Mock
  private AccessToken accessToken;

  private Authentication authentication;

  private TranslatorController controller;

  @BeforeEach
  void setup() {
    controller = new TranslatorController(service);
  }

  @Test
  void testGetAll() throws Exception {
    // Given
    given(service.getAll()).willReturn(List.of(givenResponse()));

    // When / Then
    var response = controller.getAll();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(List.of(givenResponse()));
  }

  @Test
  void testGetById() throws Exception {
    // Given
    given(service.get("job-test")).willReturn(Optional.ofNullable(givenResponse()));

    // When
    var response = controller.get("job-test");

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(givenResponse());
  }

  @Test
  void testGetByIdMissing() throws Exception {
    // Given
    given(service.get("job-test")).willReturn(Optional.empty());

    // When
    var response = controller.get("job-test");

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNull();
  }

  @Test
  void testPost() throws Exception {
    // Given
    given(service.createTranslator(givenRequest())).willReturn(givenResponse());
    givenToken();
    givenAuthentication();
    givenUserIdOnlyAuthentication();

    // When
    var response = controller.scheduleTranslator(authentication, givenRequest());

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isEqualTo(givenResponse());
  }

  @Test
  void testDelete() {
    // Given
    givenToken();
    givenAuthentication();
    givenUserIdOnlyAuthentication();

    // When
    var response = controller.deleteTranslator(authentication, "test-job");

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(response.getBody()).isNull();
  }

  @Test
  void testDeleteNotFound() throws NotFoundException, ApiException {
    // Given
    givenToken();
    givenAuthentication();
    givenUserIdOnlyAuthentication();
    doThrow(new NotFoundException("Not found")).when(service).deleteJob(anyString());

    // When
    var response = controller.deleteTranslator(authentication, "test-job");

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNull();
  }

  @Test
  void testDeleteAPIException() throws NotFoundException, ApiException {
    // Given
    givenToken();
    givenAuthentication();
    givenUserIdOnlyAuthentication();
    doThrow(new ApiException(403, "Not found")).when(service).deleteJob(anyString());

    // When
    var response = controller.deleteTranslator(authentication, "test-job");

    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody()).isNull();
  }



  private void givenAuthentication(String... auths) {
    authentication = new TestingAuthenticationToken(principal, null, auths);
  }

  private void givenToken() {
    given(principal.getKeycloakSecurityContext()).willReturn(securityContext);
    given(securityContext.getToken()).willReturn(accessToken);
  }

  private void givenUserIdOnlyAuthentication() {
    given(accessToken.getPreferredUsername()).willReturn("Test-user");
  }

}
