package eu.dissco.orchestration.backend.controller;

import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.PREFIX;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SANDBOX_URI;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SUFFIX;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SYSTEM_PATH;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SYSTEM_URI;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystem;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemRecord;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemRecordResponse;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;

import eu.dissco.orchestration.backend.domain.SourceSystemRecord;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.service.SourceSystemService;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class SourceSystemControllerTest {
  @Mock
  private SourceSystemService service;

  private SourceSystemController controller;

  private Authentication authentication;
  @Mock
  private KeycloakPrincipal<KeycloakSecurityContext> principal;

  MockHttpServletRequest mockRequest = new MockHttpServletRequest();

  @BeforeEach
  void setup(){controller = new SourceSystemController(service);
    mockRequest.setRequestURI(SYSTEM_URI);}

  @Test
  void testCreateSourceSystem() throws Exception{
    // Given
    var sourceSystem = givenSourceSystem();
    givenAuthentication();

    // When
    var result = controller.createSourceSystem(authentication, sourceSystem, mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  }

  @Test
  void testUpdateSourceSystem() {
    // Given
    var sourceSystem = givenSourceSystem();
    givenAuthentication();

    // When
    var result = controller.updateSourceSystem(authentication, PREFIX, SUFFIX, sourceSystem, mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void testGetSourceSystemById(){
    // Given
    // When
    var result = controller.getSourceSystemById(PREFIX, SUFFIX, mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void testGetSourceSystems(){
    // Given
    int pageNum = 1;
    int pageSize = 10;
    List<SourceSystemRecord> ssRecords = Collections.nCopies(pageSize, givenSourceSystemRecord());
    var linksNode = new JsonApiLinks(pageSize, pageNum, true, SYSTEM_PATH);
    var expected = givenSourceSystemRecordResponse(ssRecords, linksNode);
    given(service.getSourceSystemRecords(pageNum, pageSize, SYSTEM_PATH)).willReturn(expected);

    // When
    var result = controller.getSourceSystems(pageNum, pageSize, mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getBody()).isEqualTo(expected);
  }

  @Test
  void testGetSourceSystemsLastPage(){
    // Given
    int pageNum = 2;
    int pageSize = 10;
    List<SourceSystemRecord> ssRecords = Collections.nCopies(pageSize, givenSourceSystemRecord());
    var linksNode = new JsonApiLinks(pageSize, pageNum, false, SYSTEM_PATH);
    var expected = givenSourceSystemRecordResponse(ssRecords, linksNode);
    given(service.getSourceSystemRecords(pageNum, pageSize, SYSTEM_PATH)).willReturn(expected);

    // When
    var result = controller.getSourceSystems(pageNum, pageSize, mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getBody()).isEqualTo(expected);
  }

  private void givenAuthentication() {
    authentication = new TestingAuthenticationToken(principal, null);
  }

}
