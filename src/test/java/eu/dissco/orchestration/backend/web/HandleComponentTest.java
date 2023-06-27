package eu.dissco.orchestration.backend.web;

import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMasHandleRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import eu.dissco.orchestration.backend.exception.PidAuthenticationException;
import eu.dissco.orchestration.backend.exception.PidCreationException;
import java.io.IOException;
import java.util.List;
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
import org.springframework.web.reactive.function.client.WebClient;

@ExtendWith(MockitoExtension.class)
class HandleComponentTest {

  @Mock
  private TokenAuthenticator tokenAuthenticator;
  private HandleComponent handleComponent;

  private static MockWebServer mockHandleServer;

  @BeforeAll
  static void init() throws IOException {
    mockHandleServer = new MockWebServer();
    mockHandleServer.start();
  }

  @BeforeEach
  void setup() {
    WebClient webClient = WebClient.create(
        String.format("http://%s:%s", mockHandleServer.getHostName(), mockHandleServer.getPort()));
    handleComponent = new HandleComponent(webClient, tokenAuthenticator);

  }

  @AfterAll
  static void destroy() throws IOException {
    mockHandleServer.shutdown();
  }

  @Test
  void testPostHandle() throws Exception {
    // Given
    var requestBody = givenMasHandleRequest();
    var responseBody = givenHandleApiResponse();
    mockHandleServer.enqueue(new MockResponse()
        .setResponseCode(HttpStatus.CREATED.value())
        .setBody(MAPPER.writeValueAsString(responseBody))
        .addHeader("Content-Type", "application/json"));

    // When
    var response = handleComponent.postHandle(requestBody);

    // Then
    assertThat(response).isEqualTo(HANDLE);
  }

  @Test
  void testUnauthorized() throws Exception {
    // Given
    var requestBody = givenMasHandleRequest();

    mockHandleServer.enqueue(new MockResponse()
        .setResponseCode(HttpStatus.UNAUTHORIZED.value())
        .addHeader("Content-Type", "application/json"));

    // Then
    assertThrows(PidAuthenticationException.class, () -> handleComponent.postHandle(requestBody));
  }

  @Test
  void testBadRequest() throws Exception {
    // Given
    var requestBody = MAPPER.createObjectNode();

    mockHandleServer.enqueue(new MockResponse()
        .setResponseCode(HttpStatus.BAD_REQUEST.value())
        .addHeader("Content-Type", "application/json"));

    // Then
    assertThrows(PidCreationException.class, () -> handleComponent.postHandle(requestBody));
  }

  @Test
  void testRetriesSuccess() throws Exception {
    // Given
    var requestBody = givenMasHandleRequest();
    var responseBody = givenHandleApiResponse();
    int requestCount = mockHandleServer.getRequestCount();

    mockHandleServer.enqueue(new MockResponse().setResponseCode(501));
    mockHandleServer.enqueue(new MockResponse()
        .setResponseCode(HttpStatus.CREATED.value())
        .setBody(MAPPER.writeValueAsString(responseBody))
        .addHeader("Content-Type", "application/json"));

    // When
    var response = handleComponent.postHandle(requestBody);

    // Then
    assertThat(response).isEqualTo(HANDLE);
    assertThat(mockHandleServer.getRequestCount() - requestCount).isEqualTo(2);
  }

  @Test
  void testInterruptedException() throws Exception {
    // Given
    var requestBody = givenMasHandleRequest();
    var responseBody = givenHandleApiResponse();

    mockHandleServer.enqueue(new MockResponse()
        .setResponseCode(HttpStatus.OK.value())
        .setBody(MAPPER.writeValueAsString(responseBody))
        .addHeader("Content-Type", "application/json"));

    Thread.currentThread().interrupt();

    // When
    var response = assertThrows(PidCreationException.class,
        () -> handleComponent.postHandle(requestBody));

    // Then
    assertThat(response).hasMessage(
        "Interrupted execution: A connection error has occurred in creating a handle.");
  }

  @Test
  void testRetriesFail() throws Exception {
    // Given
    var requestBody = givenMasHandleRequest();
    int requestCount = mockHandleServer.getRequestCount();

    mockHandleServer.enqueue(new MockResponse().setResponseCode(501));
    mockHandleServer.enqueue(new MockResponse().setResponseCode(501));
    mockHandleServer.enqueue(new MockResponse().setResponseCode(501));
    mockHandleServer.enqueue(new MockResponse().setResponseCode(501));

    // Then
    assertThrows(PidCreationException.class, () -> handleComponent.postHandle(requestBody));
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

    mockHandleServer.enqueue(new MockResponse()
        .setResponseCode(HttpStatus.CREATED.value())
        .setBody(MAPPER.writeValueAsString(responseBody))
        .addHeader("Content-Type", "application/json"));
    // Then
    assertThrows(PidCreationException.class, () -> handleComponent.postHandle(requestBody));
  }

  @Test
  void testEmptyResponse() throws Exception {
    // Given
    var requestBody = givenMasHandleRequest();
    var responseBody = MAPPER.createObjectNode();

    mockHandleServer.enqueue(new MockResponse()
        .setResponseCode(HttpStatus.CREATED.value())
        .setBody(MAPPER.writeValueAsString(responseBody))
        .addHeader("Content-Type", "application/json"));
    // Then
    assertThrows(PidCreationException.class, () -> handleComponent.postHandle(requestBody));
  }

  private JsonNode givenHandleApiResponse() throws Exception {
    return MAPPER.readTree("""
        {
          "data": {
            "type": "machineAnnotationService",
            "id":"20.5000.1025/GW0-POP-XSL",
            "attributes":{
            }
          }
        }""");

  }
}
