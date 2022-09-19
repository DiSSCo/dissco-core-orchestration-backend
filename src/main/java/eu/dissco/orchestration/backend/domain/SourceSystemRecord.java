package eu.dissco.orchestration.backend.domain;

import java.time.Instant;

public record SourceSystemRecord(
        String id,
        Instant created,
        SourceSystem sourceSystem
    ) {

}
