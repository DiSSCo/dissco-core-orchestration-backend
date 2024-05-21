package eu.dissco.orchestration.backend.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public enum SourceDataStandard {
  @JsonProperty("dwc") DWC("dwc"),
  @JsonProperty("abcd") ABCD("abcd"),
  @JsonProperty("abcdefg") ABCDEFG("abcdefg");

  final String standard;

  SourceDataStandard(String s){
    standard = s;
  }
  @Override
  public String toString(){
    return standard;
  }
  
  public static SourceDataStandard fromString(String standard){
    switch (standard) {
      case "dwc" -> {
        return DWC;
      }
      case "abcd" -> {
        return ABCD;
      }
      case "abcdefg" -> {
        return ABCDEFG;
      }
      default -> {
        log.error("Invalid source data standard: {}", standard);
        throw new IllegalStateException();
      }
    }
    
  }
}
