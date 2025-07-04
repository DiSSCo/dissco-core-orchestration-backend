package eu.dissco.orchestration.backend.domain;

import lombok.Getter;

@Getter
public enum ExportType {
  DWC_DP("dwc-dp"),
  DWCA("dwca");

  private final String urlName;

  ExportType(String urlName) {
    this.urlName = urlName;
  }

  public static ExportType fromName(String name) {
    for (ExportType type : ExportType.values()) {
      if (type.getUrlName().equals(name)) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown export type: " + name);
  }
}
