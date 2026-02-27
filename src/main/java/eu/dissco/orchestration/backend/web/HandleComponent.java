package eu.dissco.orchestration.backend.web;


import eu.dissco.orchestration.backend.client.HandleClient;
import eu.dissco.orchestration.backend.exception.PidException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class HandleComponent {

  private final HandleClient handleClient;
  private final JsonMapper mapper;

  public String postHandle(JsonNode request) throws PidException {
    try {
      var result = handleClient.postHandle(List.of(request));
      var resultNode = mapper.valueToTree(result);
      return parseResponse(resultNode);
    } catch (RuntimeException e){
      log.error("An error has occurred while posting the Handle", e);
      throw new PidException("Unable to create a PID for requested resource");
    }
  }

  public void tombstoneHandle(JsonNode request, String id) throws  PidException {
    try {
      handleClient.tombstoneHandle(id, mapper.convertValue(request, Map.class));
    } catch (RuntimeException e){
      log.error("An error has occurred while tombstoning handle {} ", id, e);
      throw new PidException("Unable to create a PID for requested resource");
    }
  }

  public void rollbackHandleCreation(JsonNode request) throws PidException {
    try {
      handleClient.rollbackHandle(mapper.convertValue(request, Map.class));
    } catch (RuntimeException e) {
      log.error("An error has occurred while rolling back PID. Manually remove pid {}", request.get("data").get("id").asString());
      throw new PidException("Unable to rollback PID");
    }
  }


  private String parseResponse(JsonNode apiResponse) throws PidException {
    try {
      return apiResponse.get("data").get(0).get("id").asString();
    } catch (NullPointerException e) {
      log.error(
          "Unable to parse response from handle server. Received response does not contain  \"id\" field");
      throw new PidException("Unable to parse response from handle server");
    }
  }
}
