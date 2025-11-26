package eu.dissco.orchestration.backend.domain.openapi.sourcesystem;

import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.schema.SourceSystem;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public record SourceSystemResponseData(
    @Schema(description = "Handle of the source system") String id,
    @Schema(description = "Type of the object, in this case \"ods:SourceSystem\"", example = "ods:SourceSystem") ObjectType type,
    @Schema(description = "Source System") SourceSystem attributes

) {

}
