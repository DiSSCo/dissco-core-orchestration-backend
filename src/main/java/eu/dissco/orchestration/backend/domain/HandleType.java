package eu.dissco.orchestration.backend.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum HandleType {
  @JsonProperty("mapping")MAPPING("mapping"), @JsonProperty("sourceSystem")SOURCE_SYSTEM("sourceSystem");

  private String type;

  private HandleType(String type){
    this.type = type;
  }
  @Override
  public String toString(){
    return type;
  }

}
