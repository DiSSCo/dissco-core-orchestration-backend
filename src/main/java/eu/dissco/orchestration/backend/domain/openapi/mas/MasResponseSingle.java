package eu.dissco.orchestration.backend.domain.openapi.mas;

import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public record MasResponseSingle(
    @Schema(description = "Links object") JsonApiLinks links,
    @Schema(description = "MAS") MasResponseData data

) {

}
