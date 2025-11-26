package eu.dissco.orchestration.backend.domain.openapi.datamapping;

import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.schema.DataMapping;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public record DataMappingResponseData(
    @Schema(description = "Handle of the data mapping") String id,
    @Schema(description = "Type of the object, in this case \"ods:DataMapping\"", example = "ods:DataMapping") ObjectType type,
    @Schema(description = "Data mapping") DataMapping attributes
) {

}
