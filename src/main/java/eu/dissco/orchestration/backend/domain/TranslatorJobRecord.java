package eu.dissco.orchestration.backend.domain;

import eu.dissco.orchestration.backend.database.jooq.enums.JobState;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record TranslatorJobRecord(
    UUID jobId,
    Instant startTime,
    Instant endTime,
    JobState jobState,
    JsonNode report
) {

}
