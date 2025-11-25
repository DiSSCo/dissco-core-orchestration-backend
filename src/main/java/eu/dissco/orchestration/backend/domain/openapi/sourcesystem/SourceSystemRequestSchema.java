package eu.dissco.orchestration.backend.domain.openapi.sourcesystem;

import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.schema.SourceSystemRequest;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public record SourceSystemRequestSchema(
    @Schema SourceSystemRequestData data
) {

  @Schema
  public record SourceSystemRequestData (
      @Schema(description = "Type of request. For source systems, must be \"ods:SourceSystem\"", example = "ods:SourceSystem") ObjectType type,
      @Schema(description = "Desired source system") SourceSystemRequest attributes
  ){
  }

}
