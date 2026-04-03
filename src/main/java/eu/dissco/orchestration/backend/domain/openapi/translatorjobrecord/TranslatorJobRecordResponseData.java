package eu.dissco.orchestration.backend.domain.openapi.translatorjobrecord;

import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.domain.TranslatorJobRecord;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public record TranslatorJobRecordResponseData(
    @Schema(description = "UUID of translator job record") String id,
    @Schema(description = "Type of the object, in this case \"ods:TranslatorJobRecord\"",
        example = "ods:TranslatorJobRecord") ObjectType type,
    @Schema(description = "Translator Job Record") TranslatorJobRecord attributes
) {

}
