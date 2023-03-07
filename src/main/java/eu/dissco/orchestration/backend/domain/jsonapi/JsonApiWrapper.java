package eu.dissco.orchestration.backend.domain.jsonapi;

import java.util.List;

public record JsonApiWrapper(
    List<JsonApiData> data,
    JsonApiLinks links
) {

}
