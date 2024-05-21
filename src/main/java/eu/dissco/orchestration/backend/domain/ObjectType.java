package eu.dissco.orchestration.backend.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum ObjectType {
  @JsonProperty("sourceSystem") SOURCE_SYSTEM,
  @JsonProperty("mapping") MAPPING,
  @JsonProperty("machineAnnotationService") MAS;

}
