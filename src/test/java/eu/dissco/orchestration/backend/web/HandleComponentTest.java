package eu.dissco.orchestration.backend.web;

import static eu.dissco.orchestration.backend.testutils.TestUtils.BARE_HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMasHandleRequest;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMasHandleRequestMap;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenRollbackCreationRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import eu.dissco.orchestration.backend.client.HandleClient;
import eu.dissco.orchestration.backend.exception.PidException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;

@ExtendWith(MockitoExtension.class)
class HandleComponentTest {

  private static MockWebServer mockHandleServer;
  private HandleComponent handleComponent;
  @Mock
  HandleClient handleClient;

  @BeforeAll
  static void init() throws IOException {
    mockHandleServer = new MockWebServer();
    mockHandleServer.start();
  }

  @AfterAll
  static void destroy() throws IOException {
    mockHandleServer.shutdown();
  }

  @BeforeEach
  void setup() {
    handleComponent = new HandleComponent(handleClient, MAPPER);

  }

  @Test
  void testPostHandle1() throws Exception {
    // Given
    var requestBody = givenMasHandleRequest();
    var responseBody = givenHandleApiResponse();
    mockHandleServer.enqueue(new MockResponse().setResponseCode(HttpStatus.CREATED.value())
        .setBody(MAPPER.writeValueAsString(responseBody))
        .addHeader("Content-Type", "application/json"));

    // When
    var response = handleComponent.postHandle(requestBody);

    // Then
    assertThat(response).isEqualTo(BARE_HANDLE);
  }

  @Test
  void testPostHandle() throws PidException {
    // Given
    var requestBody = List.of(givenMasHandleRequestMap());
    given(handleClient.postHandle(List.of(givenMasHandleRequest()))).willReturn(givenHandleApiResponseMap());

    // When
    var result = handleComponent.postHandle(givenMasHandleRequest());

    // Then
    assertThat(result).isEqualTo(HANDLE);
  }

  @Test
  void testRollbackHandleCreation() {
    // Given
    var requestbody = givenRollbackCreationRequest();

    mockHandleServer.enqueue(
        new MockResponse().setResponseCode(200).addHeader("Content-Type", "application/json"));

    // Then
    assertDoesNotThrow(() -> handleComponent.rollbackHandleCreation(requestbody));
  }

  @Test
  void testUnauthorized() {
    // Given
    var requestBody = givenMasHandleRequest();

    mockHandleServer.enqueue(new MockResponse().setResponseCode(HttpStatus.UNAUTHORIZED.value())
        .addHeader("Content-Type", "application/json"));

    // Then
    assertThrows(PidException.class, () -> handleComponent.postHandle(requestBody));
  }

  @Test
  void testBadRequest() {
    // Given
    var requestBody = MAPPER.createObjectNode();

    mockHandleServer.enqueue(new MockResponse().setResponseCode(HttpStatus.BAD_REQUEST.value())
        .addHeader("Content-Type", "application/json"));

    // Then
    assertThrows(PidException.class, () -> handleComponent.postHandle(requestBody));
  }

  @Test
  void testRetriesSuccess() throws Exception {
    // Given
    var requestBody = givenMasHandleRequest();
    var responseBody = givenHandleApiResponse();
    int requestCount = mockHandleServer.getRequestCount();

    mockHandleServer.enqueue(new MockResponse().setResponseCode(501));
    mockHandleServer.enqueue(new MockResponse().setResponseCode(HttpStatus.CREATED.value())
        .setBody(MAPPER.writeValueAsString(responseBody))
        .addHeader("Content-Type", "application/json"));

    // When
    var response = handleComponent.postHandle(requestBody);

    // Then
    assertThat(response).isEqualTo(BARE_HANDLE);
    assertThat(mockHandleServer.getRequestCount() - requestCount).isEqualTo(2);
  }

  @Test
  void testInterruptedException() throws Exception {
    // Given
    var requestBody = givenMasHandleRequest();
    var responseBody = givenHandleApiResponse();

    mockHandleServer.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value())
        .setBody(MAPPER.writeValueAsString(responseBody))
        .addHeader("Content-Type", "application/json"));

    Thread.currentThread().interrupt();

    // When
    var response = assertThrows(PidException.class,
        () -> handleComponent.postHandle(requestBody));

    // Then
    assertThat(response).hasMessage(
        "Interrupted execution: A connection error has occurred in creating a handle.");
  }

  @Test
  void testRetriesFail() {
    // Given
    var requestBody = givenMasHandleRequest();
    int requestCount = mockHandleServer.getRequestCount();

    mockHandleServer.enqueue(new MockResponse().setResponseCode(501));
    mockHandleServer.enqueue(new MockResponse().setResponseCode(501));
    mockHandleServer.enqueue(new MockResponse().setResponseCode(501));
    mockHandleServer.enqueue(new MockResponse().setResponseCode(501));

    // Then
    assertThrows(PidException.class, () -> handleComponent.postHandle(requestBody));
    assertThat(mockHandleServer.getRequestCount() - requestCount).isEqualTo(4);
  }

  @Test
  void testDataMissingId() throws Exception {
    // Given
    var requestBody = givenMasHandleRequest();
    var responseBody = MAPPER.readTree("""
        {
                  "data": {
                    "type": "machineAnnotationService",
                    "attributes":{
                    }
                  }
                }""
                
        """);

    mockHandleServer.enqueue(new MockResponse().setResponseCode(HttpStatus.CREATED.value())
        .setBody(MAPPER.writeValueAsString(responseBody))
        .addHeader("Content-Type", "application/json"));
    // Then
    assertThrows(PidException.class, () -> handleComponent.postHandle(requestBody));
  }

  @Test
  void testEmptyResponse() throws Exception {
    // Given
    var requestBody = givenMasHandleRequest();
    var responseBody = MAPPER.createObjectNode();

    mockHandleServer.enqueue(new MockResponse().setResponseCode(HttpStatus.CREATED.value())
        .setBody(MAPPER.writeValueAsString(responseBody))
        .addHeader("Content-Type", "application/json"));
    // Then
    assertThrows(PidException.class, () -> handleComponent.postHandle(requestBody));
  }

  private JsonNode givenHandleApiResponse() {
    return MAPPER.readTree("""
        {
          "data":
          [
            {
              "type": "machineAnnotationService",
              "id":"20.5000.1025/GW0-POP-XSL",
              "attributes":{
              }
            }
          ]
        }""");
  }

  private Map<String, Object> givenHandleApiResponseMap(){
    return Map.of(
        "data", List.of(
            Map.of(
                "type", "machineAnnotationService",
                "id", HANDLE,
                "attributes", Map.of()
            )
        )
    );

  }

}
