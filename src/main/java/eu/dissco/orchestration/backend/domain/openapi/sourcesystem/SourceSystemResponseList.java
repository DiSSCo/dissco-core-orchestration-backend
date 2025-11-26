package eu.dissco.orchestration.backend.domain.openapi.sourcesystem;

import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema
public record SourceSystemResponseList(
    List<SourceSystemResponseData> data,
    @Schema(description = "Links object") JsonApiLinks links
) {

}
