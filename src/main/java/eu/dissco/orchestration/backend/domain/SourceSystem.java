package eu.dissco.orchestration.backend.domain;

import jakarta.validation.constraints.NotBlank;

public record SourceSystem(
    @NotBlank
    String name,
    @NotBlank
    String endpoint,
    String description,
    @NotBlank
    String mappingId
) {

}
