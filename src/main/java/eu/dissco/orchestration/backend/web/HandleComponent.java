package eu.dissco.orchestration.backend.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.orchestration.backend.client.HandleClient;
import eu.dissco.orchestration.backend.exception.PidException;
import eu.dissco.orchestration.backend.exception.ProcessingFailedException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
@RequiredArgsConstructor
@Slf4j
public class HandleComponent {

  private final WebClient webClient;
  private final HandleClient handleClient;
  private final ObjectMapper mapper;

  public String postHandle2(JsonNode request) throws PidException {
    var result = handleClient.postHandle(mapper.convertValue(request, Map.class));
    var resultNode = mapper.valueToTree(result);
    return parseResponse(resultNode);
  }


  public String postHandle(JsonNode request)
      throws PidException {
    var requestBody = BodyInserters.fromValue(List.of(request));
    log.debug("Sending call to Handle API: {}", request.toPrettyString());
    var response = sendRequest(HttpMethod.POST, requestBody, "batch");
    var responseJson = validateResponse(response);
    log.debug("Received response from Handle API: {}", responseJson.toPrettyString());
    return parseResponse(responseJson);
  }

  public void tombstoneHandle(JsonNode request, String id)
      throws PidException {
    var requestBody = BodyInserters.fromValue(request);
    var response = sendRequest(HttpMethod.PUT, requestBody, id);
    validateResponse(response);
  }

  public void rollbackHandleCreation(JsonNode request)
      throws PidException {
    var requestBody = BodyInserters.fromValue(request);
    var response = sendRequest(HttpMethod.DELETE, requestBody, "rollback/create");
    validateResponse(response);
  }

  private <T> Mono<JsonNode> sendRequest(HttpMethod httpMethod,
      BodyInserter<T, ReactiveHttpOutputMessage> requestBody, String endpoint) {
    return webClient
        .method(httpMethod)
        .uri(uriBuilder -> uriBuilder.path(endpoint).build())
        .body(requestBody)
        .acceptCharset(StandardCharsets.UTF_8)
        .retrieve()
        .onStatus(HttpStatus.UNAUTHORIZED::equals,
            r -> Mono.error(
                new PidException("Unable to authenticate with Handle Service.")))
        .onStatus(HttpStatusCode::is4xxClientError, r -> Mono.error(new PidException(
            "Unable to create PID. Response from Handle API: " + r.statusCode())))
        .bodyToMono(JsonNode.class).retryWhen(
            Retry.fixedDelay(3, Duration.ofSeconds(2)).filter(WebClientUtils::is5xxServerError)
                .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> new PidException(
                    "External Service failed to process after max retries")));
  }

  private JsonNode validateResponse(Mono<JsonNode> response)
      throws PidException {
    try {
      return response.toFuture().get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Interrupted exception has occurred.");
      throw new PidException(
          "Interrupted execution: A connection error has occurred in creating a handle.");
    } catch (ExecutionException e) {
      throw new PidException(e.getCause().getMessage());
    }
  }

  private String parseResponse(JsonNode apiResponse) throws PidException {
    try {
      return apiResponse.get("data").get(0).get("id").asText();
    } catch (NullPointerException e) {
      log.error(
          "Unable to parse response from handle server. Received response does not contain  \"id\" field");
      throw new PidException("Unable to parse response from handle server");
    }
  }
}
