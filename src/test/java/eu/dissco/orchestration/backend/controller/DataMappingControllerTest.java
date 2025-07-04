package eu.dissco.orchestration.backend.controller;

import static eu.dissco.orchestration.backend.testutils.TestUtils.BARE_HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPING_PATH;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPING_URI;
import static eu.dissco.orchestration.backend.testutils.TestUtils.PREFIX;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SUFFIX;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenAgent;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenClaims;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenDataMapping;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenDataMappingRequest;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenDataMappingRequestJson;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenDataMappingSingleJsonApiWrapper;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMappingResponse;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemRequestJson;
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
import eu.dissco.orchestration.backend.schema.DataMapping;
import eu.dissco.orchestration.backend.service.DataMappingService;
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
class DataMappingControllerTest {

  private final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
  @Mock
  private DataMappingService service;
  @Mock
  private ApplicationProperties appProperties;
  private DataMappingController controller;
  @Mock
  private Authentication authentication;

  @BeforeEach
  void setup() {
    controller = new DataMappingController(service, MAPPER, appProperties);
    mockRequest.setRequestURI(MAPPING_URI);
  }

  @Test
  void testCreateDataMapping() throws Exception {
    // Given
    givenAuthentication();
    var requestBody = givenDataMappingRequestJson();

    // When
    var result = controller.createDataMapping(authentication, requestBody, mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  }

  @Test
  void testCreateDataMappingBadType() {
    // Given
    var requestBody = givenSourceSystemRequestJson();

    // Then
    assertThrowsExactly(
        IllegalArgumentException.class,
        () -> controller.createDataMapping(authentication, requestBody, mockRequest));
  }

  @ParameterizedTest
  @MethodSource("badDataMappingRequest")
  void testCreateDataMappingMissingInfo(String fieldToRemove) {
    // Given
    var requestBody = (ObjectNode) givenDataMappingRequestJson().data().attributes();
    requestBody.remove(fieldToRemove);
    var request = new JsonApiRequestWrapper(new JsonApiRequest(ObjectType.DATA_MAPPING, requestBody));

    // When / Then
    assertThrows(IllegalArgumentException.class, () -> controller.createDataMapping(authentication, request, mockRequest));
  }

  private static Stream<Arguments> badDataMappingRequest() {

    return Stream.of(
        Arguments.of("ods:mappingDataStandard"),
        Arguments.of("schema:name"));
  }

  @Test
  void testUpdateDataMapping() throws Exception {
    // Given
    givenAuthentication();
    var requestBody = givenDataMappingRequestJson();
    given(service.updateDataMapping(BARE_HANDLE, givenDataMappingRequest(), givenAgent(),
        "null/data-mapping")).willReturn(
        givenDataMappingSingleJsonApiWrapper());

    // When
    var result = controller.updateDataMapping(authentication, PREFIX, SUFFIX, requestBody,
        mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void testUpdateDataMappingNoChanges() throws Exception {
    // Given
    givenAuthentication();
    var requestBody = givenDataMappingRequestJson();

    // When
    var result = controller.updateDataMapping(authentication, PREFIX, SUFFIX, requestBody,
        mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  @Test
  void testGetDataMappingById() throws NotFoundException {
    // When
    var result = controller.getDataMappingById(PREFIX, SUFFIX, mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void testDeleteSourceSystem() throws Exception {
    // When
    givenAuthentication();
    var result = controller.tombstoneDataMapping(authentication, PREFIX, SUFFIX);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  @Test
  void testGetDataMappings() {
    int pageNum = 1;
    int pageSize = 10;

    List<DataMapping> dataMappings = Collections.nCopies(pageSize,
        givenDataMapping(HANDLE, 1));
    var linksNode = new JsonApiLinks(pageSize, pageNum, true, MAPPING_PATH);
    var expected = givenMappingResponse(dataMappings, linksNode);
    given(service.getDataMappings(pageNum, pageSize, MAPPING_PATH)).willReturn(expected);
    given(appProperties.getBaseUrl()).willReturn("https://sandbox.dissco.tech/orchestrator");

    // When
    var result = controller.getDataMappings(pageNum, pageSize, mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getBody()).isEqualTo(expected);
  }

  private void givenAuthentication() {
    var principal = mock(Jwt.class);
    given(authentication.getPrincipal()).willReturn(principal);
    given(principal.getClaims()).willReturn(givenClaims());
  }
}
