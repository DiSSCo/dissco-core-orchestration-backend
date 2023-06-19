package eu.dissco.orchestration.backend.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum HandleType {
  @JsonProperty("mapping") MAPPING("mapping"),
  @JsonProperty("sourceSystem") SOURCE_SYSTEM("sourceSystem"),
  @JsonProperty("machineAnnotationService") MACHINE_ANNOTATION_SERVICE("machineAnnotationService");

  private final String type;

  HandleType(String type) {
    this.type = type;
  }

  @Override
  public String toString() {
    return type;
  }

}
