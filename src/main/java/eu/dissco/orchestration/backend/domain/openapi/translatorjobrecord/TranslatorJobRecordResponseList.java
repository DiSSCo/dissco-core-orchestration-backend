package eu.dissco.orchestration.backend.domain.openapi.translatorjobrecord;

import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public record TranslatorJobRecordResponseList(
    @Schema(description = "Links object") JsonApiLinks links,
    @Schema(description = "Translator Job Record") TranslatorJobRecordResponseData data
) {

}
