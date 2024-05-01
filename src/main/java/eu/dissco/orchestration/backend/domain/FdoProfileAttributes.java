package eu.dissco.orchestration.backend.domain;

import lombok.Getter;

@Getter
public enum FdoProfileAttributes {
  // Issued for agent should be DiSSCo PID; currently it's set as Naturalis's ROR
  ISSUED_FOR_AGENT("issuedForAgent"),
  // Mapping
  SOURCE_DATA_STANDARD("sourceDataStandard"),
  // Source System
  SOURCE_SYSTEM_NAME("sourceSystemName"),
  MAS_NAME("machineAnnotationServiceName");

  private final String attribute;

  FdoProfileAttributes(String attribute) {
    this.attribute = attribute;}
}
