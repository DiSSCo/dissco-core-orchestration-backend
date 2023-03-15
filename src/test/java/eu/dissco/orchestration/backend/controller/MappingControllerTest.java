package eu.dissco.orchestration.backend.controller;

import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPING_PATH;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPING_URI;
import static eu.dissco.orchestration.backend.testutils.TestUtils.OBJECT_CREATOR;
import static eu.dissco.orchestration.backend.testutils.TestUtils.PREFIX;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SANDBOX_URI;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SUFFIX;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMapping;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMappingRecord;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMappingRecordResponse;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;

import eu.dissco.orchestration.backend.domain.MappingRecord;
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
    private MockHttpServletRequest mockRequest = new MockHttpServletRequest();

  @BeforeEach
  void setup() {
    controller = new MappingController(service);
    mockRequest.setRequestURI(MAPPING_URI);
  }

  @Test
  void testCreateMapping() throws Exception {
    // Given
    givenAuthentication();
    var mapping = givenMapping();

    // When
    var result = controller.createMapping(authentication, mapping, mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  }

  @Test
  void testUpdateMapping() {
    // Given
    givenAuthentication();
    var mapping = givenMapping();

    // Whan
    var result = controller.updateMapping(authentication, PREFIX, SUFFIX, mapping, mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void testGetMappingById(){
    // Given
    var id = HANDLE;

    // When
    var result = controller.getMappingById(PREFIX, SUFFIX, mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void testGetMappings(){
    int pageNum = 1;
    int pageSize = 10;

    List<MappingRecord> mappingRecords = Collections.nCopies(pageSize, givenMappingRecord(HANDLE, 1));
    var linksNode = new JsonApiLinks(pageSize, pageNum, true, MAPPING_PATH);
    var expected = givenMappingRecordResponse(mappingRecords, linksNode);
    given(service.getMappings(pageNum, pageSize, MAPPING_PATH)).willReturn(expected);

    // When
    var result = controller.getMappings(pageNum, pageSize, mockRequest);

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
