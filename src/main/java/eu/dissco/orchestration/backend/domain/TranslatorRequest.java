package eu.dissco.orchestration.backend.domain;

import java.util.List;

public record TranslatorRequest(
    String sourceSystemId,
    TranslatorType translatorType,
    String query,
    Integer itemsPerRequest,
    List<Enrichment> enrichmentList
) {

}
