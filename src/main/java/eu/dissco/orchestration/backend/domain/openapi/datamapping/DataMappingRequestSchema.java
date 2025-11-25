package eu.dissco.orchestration.backend.domain.openapi.datamapping;

import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.schema.DataMappingRequest;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public record DataMappingRequestSchema(
    @Schema DataMappingRequestData data
) {

  @Schema
  public record DataMappingRequestData (
      @Schema(description = "Type of request. For data mapping, must be \"ods:DataMapping\"", example = "ods:DataMapping") ObjectType type,
      @Schema(description = "Desired data mapping") DataMappingRequest attributes
  ){
  }

}
