package eu.dissco.orchestration.backend.domain.openapi.datamapping;

import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public record DataMappingResponseSingle (
    @Schema(description = "Links object, self-referencing")JsonApiLinks links,
    @Schema(description = "Data mapping") DataMappingResponseData data
    ){

}
