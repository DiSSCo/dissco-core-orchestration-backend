package eu.dissco.orchestration.backend.domain;

import java.time.Instant;

public record MappingRecord(
    String id,
    int version,
    Instant created,
    String creator,
    Mapping mapping
) {

}
