package eu.dissco.orchestration.backend.domain.openapi.datamapping;

import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.schema.DataMapping;
import io.swagger.v3.oas.annotations.media.Schema;

public record DataMappingResponseData(
    @Schema(description = "The type of object") ObjectType type,
    @Schema(description = "Attributes to post") DataMapping attributes) {

}
