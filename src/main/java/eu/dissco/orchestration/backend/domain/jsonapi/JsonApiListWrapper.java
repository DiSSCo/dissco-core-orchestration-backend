package eu.dissco.orchestration.backend.domain.jsonapi;

import java.util.List;

public record JsonApiListWrapper(
    List<JsonApiData> data,
    JsonApiLinks links
) {

}
