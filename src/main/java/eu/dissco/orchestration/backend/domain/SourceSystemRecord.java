package eu.dissco.orchestration.backend.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

public record SourceSystemRecord(
    @NotNull
    String id,
    @Positive
    int version,
    @NotNull
    String creator,
    @NotNull
    Instant created,
    Instant deleted,
    SourceSystem sourceSystem
) {

}
