package eu.dissco.orchestration.backend.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum ObjectType {
  @JsonProperty("ods:SourceSystem") SOURCE_SYSTEM("ods:SourceSystem"),
  @JsonProperty("ods:DataMapping") DATA_MAPPING("ods:DataMapping"),
  @JsonProperty("ods:MachineAnnotationService") MAS("ods:MachineAnnotationService");

  private final String fullName;

  ObjectType(String fullName) {
    this.fullName = fullName;
  }
}
