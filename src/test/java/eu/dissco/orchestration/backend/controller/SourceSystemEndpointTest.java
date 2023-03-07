package eu.dissco.orchestration.backend.controller;

import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.PREFIX;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SANDBOX_URI;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SUFFIX;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystem;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemRecord;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSsRecordResponse;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;

import eu.dissco.orchestration.backend.domain.SourceSystemRecord;
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
class SourceSystemEndpointTest {
  @Mock
  private SourceSystemService service;

  private SourceSystemEndpoint controller;

  private Authentication authentication;
  @Mock
  private KeycloakPrincipal<KeycloakSecurityContext> principal;

  @BeforeEach
  void setup(){controller = new SourceSystemEndpoint(service);}

  @Test
  void testCreateSourceSystem() throws Exception{
    // Given
    var ssRecord = givenSourceSystemRecord();
    var ss = givenSourceSystem();
    givenAuthentication();
    given(service.createSourceSystem(ss)).willReturn(ssRecord);

    // When
    var result = controller.createSourceSystem(authentication, ss);

    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getBody()).isEqualTo(ssRecord);
  }

  @Test
  void testUpdateSourceSystem() {
    // Given
    var ssRecord = givenSourceSystemRecord();
    var ss = givenSourceSystem();
    givenAuthentication();
    given(service.updateSourceSystem(HANDLE, ss)).willReturn(ssRecord);

    // When
    var result = controller.updateSourceSystem(authentication, PREFIX, SUFFIX, ss);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getBody()).isEqualTo(ssRecord);
  }

  @Test
  void testGetSourceSystems(){
    // Given
    int pageNum = 0;
    int pageSize = 10;
    MockHttpServletRequest r = new MockHttpServletRequest();
    r.setRequestURI("/source-system");
    String path = SANDBOX_URI + "/source-system";
    List<SourceSystemRecord> ssRecords = Collections.nCopies(pageSize, givenSourceSystemRecord());
    var expected = givenSsRecordResponse(ssRecords, path);
    given(service.getSourceSystemRecords(pageNum, pageSize, path)).willReturn(expected);

    // When
    var result = controller.getSourceSystems(pageNum, pageSize, r);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getBody()).isEqualTo(expected);
  }

  private void givenAuthentication() {
    authentication = new TestingAuthenticationToken(principal, null);
  }

}
