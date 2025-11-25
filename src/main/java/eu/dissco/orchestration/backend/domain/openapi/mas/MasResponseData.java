package eu.dissco.orchestration.backend.domain.openapi.mas;

import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.schema.MachineAnnotationService;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public record MasResponseData(
    @Schema(description = "Handle of the source system") String id,
    @Schema(description = "Type of the object, in this case \"ods:MachineAnnotationService\"", example = "ods:MachineAnnotationService") ObjectType type,
    @Schema(description = "Source System") MachineAnnotationService attributes

) {

}
