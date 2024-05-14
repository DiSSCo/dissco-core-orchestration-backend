package eu.dissco.orchestration.backend.controller;

import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPING_PATH;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPING_URI;
import static eu.dissco.orchestration.backend.testutils.TestUtils.OBJECT_CREATOR;
import static eu.dissco.orchestration.backend.testutils.TestUtils.PREFIX;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SUFFIX;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMapping;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMappingRecord;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMappingRecordResponse;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMappingRequest;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMappingSingleJsonApiWrapper;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemRequest;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.BDDMockito.given;

import eu.dissco.orchestration.backend.domain.MappingRecord;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.properties.ApplicationProperties;
import eu.dissco.orchestration.backend.service.MappingService;
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

@ExtendWith(MockitoExtension.class)
class MappingControllerTest {

  private final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
  @Mock
  private MappingService service;
  @Mock
  private ApplicationProperties appProperties;
  private MappingController controller;
  @Mock
  private Authentication authentication;

  @BeforeEach
  void setup() {
    controller = new MappingController(service, MAPPER, appProperties);
    mockRequest.setRequestURI(MAPPING_URI);
  }

  @Test
  void testCreateMapping() throws Exception {
    // Given
    givenAuthentication();
    var requestBody = givenMappingRequest();

    // When
    var result = controller.createMapping(authentication, requestBody, mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  }

  @Test
  void testCreateMappingBadType() {
    // Given
    var requestBody = givenSourceSystemRequest();

    // Then
    assertThrowsExactly(
        IllegalArgumentException.class,
        () -> controller.createMapping(authentication, requestBody, mockRequest));
  }

  @Test
  void testUpdateMapping() throws Exception {
    // Given
    givenAuthentication();
    var requestBody = givenMappingRequest();
    given(service.updateMapping(HANDLE, givenMapping(), OBJECT_CREATOR, "null/mapping")).willReturn(
        givenMappingSingleJsonApiWrapper());

    // When
    var result = controller.updateMapping(authentication, PREFIX, SUFFIX, requestBody, mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void testUpdateMappingNoChanges() throws Exception {
    // Given
    givenAuthentication();
    var requestBody = givenMappingRequest();

    // When
    var result = controller.updateMapping(authentication, PREFIX, SUFFIX, requestBody, mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  @Test
  void testGetMappingById() {
    // When
    var result = controller.getMappingById(PREFIX, SUFFIX, mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void testDeleteSourceSystem() throws Exception {
    // When
    var result = controller.deleteMapping(authentication, PREFIX, SUFFIX);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  @Test
  void testGetMappings() {
    int pageNum = 1;
    int pageSize = 10;

    List<MappingRecord> mappingRecords = Collections.nCopies(pageSize,
        givenMappingRecord(HANDLE, 1));
    var linksNode = new JsonApiLinks(pageSize, pageNum, true, MAPPING_PATH);
    var expected = givenMappingRecordResponse(mappingRecords, linksNode);
    given(service.getMappings(pageNum, pageSize, MAPPING_PATH)).willReturn(expected);
    given(appProperties.getBaseUrl()).willReturn("https://sandbox.dissco.tech/orchestrator");

    // When
    var result = controller.getMappings(pageNum, pageSize, mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(result.getBody()).isEqualTo(expected);
  }

  private void givenAuthentication() {
    given(authentication.getName()).willReturn(OBJECT_CREATOR);
  }

}
