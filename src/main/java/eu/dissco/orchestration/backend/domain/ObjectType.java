package eu.dissco.orchestration.backend.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum ObjectType {
  @JsonProperty("ods:SourceSystem") SOURCE_SYSTEM,
  @JsonProperty("ods:DataMapping") MAPPING,
  @JsonProperty("ods:MachineAnnotationService") MAS

}
