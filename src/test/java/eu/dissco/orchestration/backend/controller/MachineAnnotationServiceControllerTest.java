package eu.dissco.orchestration.backend.controller;

import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAS_URI;
import static eu.dissco.orchestration.backend.testutils.TestUtils.PREFIX;
import static eu.dissco.orchestration.backend.testutils.TestUtils.SUFFIX;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMas;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMasRequest;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMasSingleJsonApiWrapper;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;

import eu.dissco.orchestration.backend.exception.NotFoundException;
import eu.dissco.orchestration.backend.properties.ApplicationProperties;
import eu.dissco.orchestration.backend.service.MachineAnnotationServiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class MachineAnnotationServiceControllerTest {

  private final Authentication authentication = new TestingAuthenticationToken(null, null);
  MockHttpServletRequest mockRequest = new MockHttpServletRequest();
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
    var mas = givenMasRequest();

    // When
    var result = controller.createMachineAnnotationService(authentication, mas, mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
  }

  @Test
  void testUpdateMas() throws Exception {
    // Given
    var mas = givenMasRequest();
    var masResponse = givenMasSingleJsonApiWrapper();
    given(service.updateMachineAnnotationService(HANDLE, givenMas(), "", "null/mas")).willReturn(
        masResponse);

    // When
    var result = controller.updateMachineAnnotationService(authentication, PREFIX, SUFFIX, mas,
        mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  void testUpdateMasNoChange() throws Exception {
    // Given
    var mas = givenMasRequest();
    given(service.updateMachineAnnotationService(HANDLE, givenMas(), "", "null/mas")).willReturn(
        null);

    // When
    var result = controller.updateMachineAnnotationService(authentication, PREFIX, SUFFIX, mas,
        mockRequest);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  @Test
  void testGetMasById() {
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
  void testDeleteMas() throws NotFoundException {
    // When
    var result = controller.deleteMachineAnnotationService(authentication, PREFIX, SUFFIX);

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

}
