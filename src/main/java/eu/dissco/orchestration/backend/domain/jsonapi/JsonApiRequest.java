package eu.dissco.orchestration.backend.domain.jsonapi;

import eu.dissco.orchestration.backend.domain.ObjectType;
import tools.jackson.databind.JsonNode;

public record JsonApiRequest(ObjectType type, JsonNode attributes) {

}
