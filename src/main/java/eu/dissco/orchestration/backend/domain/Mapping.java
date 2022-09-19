package eu.dissco.orchestration.backend.domain;

import com.fasterxml.jackson.databind.JsonNode;

public record Mapping(
    String name,
    String description,
    JsonNode mapping
) {

}
