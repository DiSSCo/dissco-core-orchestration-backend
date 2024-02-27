package eu.dissco.orchestration.backend.domain;

import eu.dissco.orchestration.backend.database.jooq.enums.TranslatorType;
import jakarta.validation.constraints.NotBlank;

public record SourceSystem(
    @NotBlank
    String name,
    @NotBlank
    String endpoint,
    String description,

    TranslatorType translatorType,
    @NotBlank
    String mappingId
) {

}
