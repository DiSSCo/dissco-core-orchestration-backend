package eu.dissco.orchestration.backend.domain.jsonapi;

import eu.dissco.orchestration.backend.domain.ObjectType;
import tools.jackson.databind.JsonNode;

public record JsonApiData(
    String id,
    ObjectType type,
    JsonNode attributes
) {

}
