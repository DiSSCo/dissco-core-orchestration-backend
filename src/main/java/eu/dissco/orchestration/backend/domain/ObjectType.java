package eu.dissco.orchestration.backend.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum ObjectType {
  @JsonProperty("sourceSystem") SOURCE_SYSTEM ("https://hdl.handle.net/21.T11148/417a4f472f60f7974c12"),
  @JsonProperty("mapping") MAPPING ("https://hdl.handle.net/21.T11148/ce794a6f4df42eb7e77e"),
  @JsonProperty("machineAnnotationService") MAS( "https://hdl.handle.net/21.T11148/22e71a0015cbcfba8ffa");

  private final String fdoProfile;

  private ObjectType(String fdoProfile){
    this.fdoProfile = fdoProfile;
  }

}
