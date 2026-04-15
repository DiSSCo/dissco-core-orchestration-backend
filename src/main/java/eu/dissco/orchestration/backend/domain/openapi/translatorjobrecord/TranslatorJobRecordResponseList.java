package eu.dissco.orchestration.backend.domain.openapi.translatorjobrecord;

import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema
public record TranslatorJobRecordResponseList(
    @Schema(description = "Links object") JsonApiLinks links,
    @Schema(description = "Translator Job Records") List<TranslatorJobRecordResponseData> data
) {

}
