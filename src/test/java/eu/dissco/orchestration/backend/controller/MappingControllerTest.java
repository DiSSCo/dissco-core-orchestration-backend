package eu.dissco.orchestration.backend.controller;

import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.OBJECT_CREATOR;
import static eu.dissco.orchestration.backend.testutils.TestUtils.PREFIX;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SANDBOX_URI;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SUFFIX;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMapping;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMappingRecord;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMappingRecordResponse;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemRecord;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemRecordResponse;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;

import eu.dissco.orchestration.backend.domain.MappingRecord;
import eu.dissco.orchestration.backend.domain.SourceSystemRecord;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.service.MappingService;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class MappingControllerTest {

  @Mock
  private MappingService service;

  private MappingController controller;

  private Authentication authentication;
  @Mock
  private KeycloakPrincipal<KeycloakSecurityContext> principal;
  @Mock
  private KeycloakSecurityContext securityContext;
  @Mock
  private AccessToken accessToken;

  @BeforeEach
  void setup() {
    controller = new MappingController(service);
  }

  @Test
  void testCreateMapping() throws Exception {
    // Given
    givenAuthentication();
    var mapping = givenMapping();
    var expected = givenMappingRecord(HANDLE, 1);
    given(service.createMapping(mapping, OBJECT_CREATOR)).willReturn(expected);

    // When
    var result = controller.createMapping(authentication, mapping);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(result.getBody()).isEqualTo(expected);
  }

  @Test
  void testUpdateMapping() {
    // Given
    givenAuthentication();
    var mapping = givenMapping();
    var expected = givenMappingRecord(HANDLE, 2);
    given(service.updateMapping(HANDLE, mapping, OBJECT_CREATOR)).willReturn(expected);

    // Whan
    var result = controller.updateMapping(authentication, PREFIX, SUFFIX, mapping);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getBody()).isEqualTo(expected);
  }

  @Test
  void testGetMappingById(){
    // Given
    var id = HANDLE;
    var expected = givenMappingRecord(id, 1);
    given(service.getMappingById(id)).willReturn(expected);

    // When
    var result = controller.getMappingById(PREFIX, SUFFIX);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getBody()).isEqualTo(expected);
  }

  @Test
  void testDeleteSourceSystem() throws Exception {
    // When
    var result = controller.deleteMapping(PREFIX, SUFFIX);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  @Test
  void testGetMappings(){
    int pageNum = 1;
    int pageSize = 10;

    MockHttpServletRequest r = new MockHttpServletRequest();
    r.setRequestURI("/source-system");
    String path = SANDBOX_URI + "/source-system";

    List<MappingRecord> mappingRecords = Collections.nCopies(pageSize, givenMappingRecord(HANDLE, 1));
    var linksNode = new JsonApiLinks(pageSize, pageNum, true, path);
    var expected = givenMappingRecordResponse(mappingRecords, linksNode);
    given(service.getMappings(pageNum, pageSize, path)).willReturn(expected);

    // When
    var result = controller.getMappings(pageNum, pageSize, r);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getBody()).isEqualTo(expected);
  }

  private void givenAuthentication() {
    authentication = new TestingAuthenticationToken(principal, null);
    given(principal.getKeycloakSecurityContext()).willReturn(securityContext);
    given(securityContext.getToken()).willReturn(accessToken);
    given(accessToken.getSubject()).willReturn(OBJECT_CREATOR);
  }

}
