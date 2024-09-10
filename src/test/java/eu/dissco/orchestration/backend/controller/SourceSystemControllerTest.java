package eu.dissco.orchestration.backend.controller;

import static eu.dissco.orchestration.backend.testutils.TestUtils.BARE_HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.OBJECT_CREATOR;
import static eu.dissco.orchestration.backend.testutils.TestUtils.PREFIX;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SANDBOX_URI;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SUFFIX;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SYSTEM_PATH;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SYSTEM_URI;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenClaims;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenDataMappingRequestJson;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystem;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemRequest;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemRequestJson;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemResponse;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemSingleJsonApiWrapper;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.properties.ApplicationProperties;
import eu.dissco.orchestration.backend.schema.SourceSystem;
import eu.dissco.orchestration.backend.service.SourceSystemService;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class SourceSystemControllerTest {


  MockHttpServletRequest mockRequest = new MockHttpServletRequest();
  @Mock
  private SourceSystemService service;
  @Mock
  private ApplicationProperties appProperties;
  private SourceSystemController controller;
  @Mock
  private Authentication authentication;

  @BeforeEach
  void setup() {
    controller = new SourceSystemController(service, MAPPER, appProperties);
    mockRequest.setRequestURI(SYSTEM_URI);
  }

  @Test
  void testCreateSourceSystem() throws Exception {
    // Given
    var sourceSystem = givenSourceSystemRequestJson();
    givenAuthentication();

    // When
    var result = controller.createSourceSystem(authentication, sourceSystem, mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  }

  @Test
  void testCreateSourceSystemBadType() {
    // Given
    var sourceSystem = givenDataMappingRequestJson();

    // When
    assertThrowsExactly(
        IllegalArgumentException.class,
        () -> controller.createSourceSystem(authentication, sourceSystem, mockRequest));
  }

  @Test
  void testUpdateSourceSystem() throws Exception {
    // Given
    var sourceSystem = givenSourceSystemRequestJson();
    givenAuthentication();
    given(service.updateSourceSystem(BARE_HANDLE, givenSourceSystemRequest(), OBJECT_CREATOR,
        "null/source-system", true)).willReturn(
        givenSourceSystemSingleJsonApiWrapper());

    // When
    var result = controller.updateSourceSystem(authentication, PREFIX, SUFFIX, true, sourceSystem,
        mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void testUpdateSourceSystemNoChanges() throws Exception {
    // Given
    var sourceSystem = givenSourceSystemRequestJson();
    givenAuthentication();

    // When
    var result = controller.updateSourceSystem(authentication, PREFIX, SUFFIX, false, sourceSystem,
        mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  @Test
  void testGetSourceSystemById() {
    // Given
    // When
    var result = controller.getSourceSystemById(PREFIX, SUFFIX, mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void testGetSourceSystems() {
    // Given
    int pageNum = 1;
    int pageSize = 10;
    List<SourceSystem> sourceSystems = Collections.nCopies(pageSize, givenSourceSystem());
    var linksNode = new JsonApiLinks(pageSize, pageNum, true, SYSTEM_PATH);
    var expected = givenSourceSystemResponse(sourceSystems, linksNode);
    given(service.getSourceSystems(pageNum, pageSize, SYSTEM_PATH)).willReturn(expected);
    given(appProperties.getBaseUrl()).willReturn(SANDBOX_URI);

    // When
    var result = controller.getSourceSystems(pageNum, pageSize, mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getBody()).isEqualTo(expected);
  }

  @Test
  void testGetSourceSystemsLastPage() {
    // Given
    int pageNum = 2;
    int pageSize = 10;
    List<SourceSystem> ssRecords = Collections.nCopies(pageSize, givenSourceSystem());
    var linksNode = new JsonApiLinks(pageSize, pageNum, false, SYSTEM_PATH);
    var expected = givenSourceSystemResponse(ssRecords, linksNode);
    given(service.getSourceSystems(pageNum, pageSize, SYSTEM_PATH)).willReturn(expected);
    given(appProperties.getBaseUrl()).willReturn(SANDBOX_URI);

    // When
    var result = controller.getSourceSystems(pageNum, pageSize, mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getBody()).isEqualTo(expected);
  }

  @Test
  void testTombstoneSourceSystem() throws Exception {
    // Given
    givenAuthentication();

    // When
    var result = controller.tombstoneSourceSystem(authentication, PREFIX, SUFFIX);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  private void givenAuthentication() {
    var principal = mock(Jwt.class);
    given(authentication.getPrincipal()).willReturn(principal);
    given(principal.getClaims()).willReturn(givenClaims());
  }

}
