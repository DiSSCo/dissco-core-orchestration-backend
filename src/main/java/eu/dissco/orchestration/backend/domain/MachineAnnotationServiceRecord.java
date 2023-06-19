package eu.dissco.orchestration.backend.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

public record MachineAnnotationServiceRecord(
    @NotBlank
    String pid,
    @Positive
    int version,
    @NotNull
    Instant created,
    @NotBlank
    String administrator,
    @NotNull
    MachineAnnotationService mas,
    Instant deleted
) {

}
