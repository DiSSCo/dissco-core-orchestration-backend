package eu.dissco.orchestration.backend.domain;

import java.time.Instant;

public record MappingRecord(
    String id,
    int version,
    Instant created,
    Instant deleted,
    String creator,
    Mapping mapping
) {

}
