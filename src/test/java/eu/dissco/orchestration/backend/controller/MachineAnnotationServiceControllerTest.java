package eu.dissco.orchestration.backend.controller;

import static eu.dissco.orchestration.backend.testutils.TestUtils.BARE_HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAS_URI;
import static eu.dissco.orchestration.backend.testutils.TestUtils.OBJECT_CREATOR;
import static eu.dissco.orchestration.backend.testutils.TestUtils.PREFIX;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SUFFIX;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenAgent;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenClaims;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMasRequest;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMasRequestJson;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMasSingleJsonApiWrapper;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystemRequestJson;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiRequest;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiRequestWrapper;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.properties.ApplicationProperties;
import eu.dissco.orchestration.backend.service.MachineAnnotationServiceService;
import java.util.Map;
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
class MachineAnnotationServiceControllerTest {

  MockHttpServletRequest mockRequest = new MockHttpServletRequest();
  @Mock
  private Authentication authentication;
  @Mock
  private MachineAnnotationServiceService service;
  @Mock
  private ApplicationProperties appProperties;
  private MachineAnnotationServiceController controller;

  @BeforeEach
  void setup() {
    controller = new MachineAnnotationServiceController(service, MAPPER, appProperties);
    mockRequest.setRequestURI(MAS_URI);
  }

  @Test
  void testCreateMas() throws Exception {
    // Given
    givenAuthentication();
    var mas = givenMasRequestJson();

    // When
    var result = controller.createMachineAnnotationService(authentication, mas, mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  }

  @Test
  void testCreateMappingBadType() {
    // Given
    var requestBody = givenSourceSystemRequestJson();

    // Then
    assertThrowsExactly(
        IllegalArgumentException.class,
        () -> controller.createMachineAnnotationService(authentication, requestBody, mockRequest));
  }

  @ParameterizedTest
  @MethodSource("badMasRequest")
  void testCreateDataMappingMissingInfo(String fieldToRemove) {
    // Given
    var requestBody = (ObjectNode) givenMasRequestJson().data().attributes();
    requestBody.remove(fieldToRemove);
    var request = new JsonApiRequestWrapper(new JsonApiRequest(ObjectType.MAS, requestBody));

    // When / Then
    assertThrows(IllegalArgumentException.class, () -> controller.createMachineAnnotationService(authentication, request, mockRequest));
  }

  private static Stream<Arguments> badMasRequest() {
    return Stream.of(
        Arguments.of("ods:containerImage"),
        Arguments.of("ods:hasTargetDigitalObjectFilter"),
        Arguments.of("schema:name"));
  }

  @Test
  void testUpdateMas() throws Exception {
    // Given
    givenAuthentication();
    var mas = givenMasRequestJson();
    var masResponse = givenMasSingleJsonApiWrapper();
    given(service.updateMachineAnnotationService(BARE_HANDLE, givenMasRequest(), givenAgent(),
        "null/mas")).willReturn(
        masResponse);

    // When
    var result = controller.updateMachineAnnotationService(authentication, PREFIX, SUFFIX, mas,
        mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void testUpdateMasTokenHasName() throws Exception {
    // Given
    var principal = mock(Jwt.class);
    given(authentication.getPrincipal()).willReturn(principal);
    given(principal.getClaims()).willReturn(Map.of(
        "orcid", OBJECT_CREATOR,
        "given_name", "Carl"
    ));
    var expectedAgent = givenAgent()
        .withSchemaName("Carl");

    // When
    controller.updateMachineAnnotationService(authentication, PREFIX, SUFFIX,
        givenMasRequestJson(), mockRequest);

    // Then
    then(service).should()
        .updateMachineAnnotationService(anyString(), any(), eq(expectedAgent), anyString());
  }

  @Test
  void testUpdateMasNoChange() throws Exception {
    // Given
    givenAuthentication();
    var mas = givenMasRequestJson();
    given(service.updateMachineAnnotationService(BARE_HANDLE, givenMasRequest(), givenAgent(),
        "null/mas")).willReturn(
        null);

    // When
    var result = controller.updateMachineAnnotationService(authentication, PREFIX, SUFFIX, mas,
        mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  @Test
  void testGetMasById() throws NotFoundException {
    // When
    var result = controller.getMachineAnnotationService(PREFIX, SUFFIX, mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void testGetMass() {
    // Given

    // When
    var result = controller.getMachineAnnotationServices(1, 10, mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void testDeleteMas() throws Exception {
    // Given
    givenAuthentication();

    // When
    var result = controller.tombstoneMachineAnnotationService(authentication, PREFIX, SUFFIX);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  private void givenAuthentication() {
    var principal = mock(Jwt.class);
    given(authentication.getPrincipal()).willReturn(principal);
    given(principal.getClaims()).willReturn(givenClaims());
  }

}
