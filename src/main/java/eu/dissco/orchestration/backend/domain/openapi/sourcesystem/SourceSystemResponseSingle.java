package eu.dissco.orchestration.backend.domain.openapi.sourcesystem;

import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public record SourceSystemResponseSingle(
    @Schema(description = "Links object") JsonApiLinks links,
    @Schema(description = "Source system") SourceSystemResponseData data

) {

}
