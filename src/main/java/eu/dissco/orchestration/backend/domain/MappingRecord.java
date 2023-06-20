package eu.dissco.orchestration.backend.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

public record MappingRecord(
    @NotBlank
    String id,
    @Positive
    int version,
    @NotNull
    Instant created,
    Instant deleted,
    @NotBlank
    String creator,
    Mapping mapping
) {

}
