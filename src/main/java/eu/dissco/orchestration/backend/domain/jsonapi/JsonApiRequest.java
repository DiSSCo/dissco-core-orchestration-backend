package eu.dissco.orchestration.backend.domain.jsonapi;

import com.fasterxml.jackson.databind.JsonNode;
import eu.dissco.orchestration.backend.domain.ObjectType;

public record JsonApiRequest(ObjectType type, JsonNode attributes) {

}
