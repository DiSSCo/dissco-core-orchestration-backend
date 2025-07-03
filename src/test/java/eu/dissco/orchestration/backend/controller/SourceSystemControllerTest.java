package eu.dissco.orchestration.backend.controller;

import static eu.dissco.orchestration.backend.testutils.TestUtils.BARE_HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.PREFIX;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SANDBOX_URI;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SUFFIX;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SYSTEM_PATH;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SYSTEM_URI;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenAgent;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenClaims;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenDataMappingRequestJson;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystem;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemRequest;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemRequestJson;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemResponse;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemSingleJsonApiWrapper;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiRequest;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiRequestWrapper;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.properties.ApplicationProperties;
import eu.dissco.orchestration.backend.schema.SourceSystem;
import eu.dissco.orchestration.backend.service.SourceSystemService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

  @ParameterizedTest
  @MethodSource("badSourceSystemRequest")
  void testCreateDataMappingMissingInfo(String fieldToRemove) {
    // Given
    var requestBody = (ObjectNode) givenSourceSystemRequestJson().data().attributes();
    requestBody.remove(fieldToRemove);
    var request = new JsonApiRequestWrapper(new JsonApiRequest(ObjectType.SOURCE_SYSTEM, requestBody));

    // When / Then
    assertThrows(IllegalArgumentException.class, () -> controller.createSourceSystem(authentication, request, mockRequest));
  }

  private static Stream<Arguments> badSourceSystemRequest() {

    return Stream.of(
        Arguments.of("schema:url"),
        Arguments.of("ods:translatorType"),
        Arguments.of("ods:dataMappingIDgivenSourceSystemRequestJson"),
        Arguments.of("schema:name"));
  }

  @Test
  void testUpdateSourceSystem() throws Exception {
    // Given
    var sourceSystem = givenSourceSystemRequestJson();
    givenAuthentication();
    given(service.updateSourceSystem(BARE_HANDLE, givenSourceSystemRequest(), givenAgent(),
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
  void testDownloadSourceSystemDwcDp() throws URISyntaxException, IOException, NotFoundException {
    // Given
    given(service.getSourceSystemDwcDp(BARE_HANDLE)).willReturn(
        new ByteArrayInputStream("test".getBytes()));

    // When
    var result = controller.getSourceSystemDwcDp(PREFIX, SUFFIX);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(new String(result.getBody().getContentAsByteArray())).isEqualTo("test");
    assertThat(result.getHeaders().containsValue(List.of("application/zip"))).isTrue();
    assertThat(result.getHeaders().containsValue(List.of("attachment; filename=\"20.5000.1025_gw0-pop-xsl_dwc-dp.zip\""))).isTrue();
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
