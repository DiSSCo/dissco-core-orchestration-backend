package eu.dissco.orchestration.backend.domain.openapi.datamapping;

import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.schema.DataMappingRequest;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public record DataMappingRequestSchema (
    DataMappingRequestData data
){

  @Schema
  public record DataMappingRequestData(
      @Schema(description = "The type of object") ObjectType type,
      @Schema(description = "Attributes to post") DataMappingRequest attributes
  ){

  }

}
