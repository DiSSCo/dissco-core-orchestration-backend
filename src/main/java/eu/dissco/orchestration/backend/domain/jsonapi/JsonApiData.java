package eu.dissco.orchestration.backend.domain.jsonapi;

import com.fasterxml.jackson.databind.JsonNode;
import eu.dissco.orchestration.backend.domain.ObjectType;

public record JsonApiData(
    String id,
    ObjectType type,
    JsonNode attributes
) {

}
