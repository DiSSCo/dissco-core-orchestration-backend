package eu.dissco.orchestration.backend.domain;

public record SourceSystem(
    String name,
    String endpoint,
    String description,
    String mappingId
) {

}
