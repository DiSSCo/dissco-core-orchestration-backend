package eu.dissco.orchestration.backend.domain.openapi.mas;

import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.schema.MachineAnnotationServiceRequest;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public record MasRequestSchema(
    @Schema MasRequestData data
) {

  @Schema
  public record MasRequestData (
      @Schema(description = "Type of request. For MASs, must be \"ods:MachineAnnotationService\"", example = "ods:MachineAnnotationService") ObjectType type,
      @Schema(description = "Desired MAS") MachineAnnotationServiceRequest attributes
  ){
  }

}
