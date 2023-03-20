package eu.dissco.orchestration.backend.domain.jsonapi;

import com.fasterxml.jackson.databind.JsonNode;
import eu.dissco.orchestration.backend.domain.HandleType;

public record JsonApiRequest(HandleType type, JsonNode attributes) {

}
