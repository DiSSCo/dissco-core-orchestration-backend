package eu.dissco.orchestration.backend.web;

import eu.dissco.orchestration.backend.client.HandleClient;
import eu.dissco.orchestration.backend.exception.PidException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class HandleComponent {

	private final HandleClient handleClient;

	public String postHandle(JsonNode request) throws PidException {
		var result = handleClient.postHandle(List.of(request));
		return parseResponse(result);
	}

	public void tombstoneHandle(JsonNode request, String id) throws PidException {
		handleClient.tombstoneHandle(id, request);
	}

	public void rollbackHandleCreation(JsonNode request) {
		try {
			handleClient.rollbackHandle(request);
		}
		catch (PidException e) {
			log.error("Unable to rollback handle creation", e);
		}
	}

	private String parseResponse(JsonNode apiResponse) throws PidException {
		try {
			return apiResponse.get("data").get(0).get("id").asString();
		}
		catch (NullPointerException _) {
			log.error("Unable to parse response from handle server. Received response: {}", apiResponse);
			throw new PidException("Unable to parse response from handle server");
		}
	}

}
