package eu.dissco.orchestration.backend.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

public record Mapping(
    @NotBlank
    String name,
    String description,
    @JsonProperty(value = "fieldMapping")
    JsonNode mapping,
    @NotBlank
    String sourceDataStandard) {

}
