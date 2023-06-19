package eu.dissco.orchestration.backend.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public record Mapping(
    String name,
    String description,
    @JsonProperty(value = "fieldMapping")
    JsonNode mapping,
    String sourceDataStandard) {

}
