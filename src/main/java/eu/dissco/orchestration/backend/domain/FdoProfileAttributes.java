package eu.dissco.orchestration.backend.domain;

public enum FdoProfileAttributes {
  FDO_PROFILE("fdoProfile", "http://hdl.handle.net/21.T11148/64396cf36b976ad08267"),
  DIGITAL_OBJECT_TYPE("digitalObjectType", null),
  // Issued for agent should be DiSSCo PID; currently it's set as Naturalis's ROR
  ISSUED_FOR_AGENT ("issuedForAgent","https://ror.org/0566bfb96"),
  // Mapping
  SOURCE_DATA_STANDARD("sourceDataStandard", null),
  // Source System
  SOURCE_SYSTEM_NAME("sourceSystemName", null);

  private final String attribute;
  private final String defaultValue;

  public String getAttribute(){
    return this.attribute;
  }
  public String getDefaultValue(){return this.defaultValue;}

  private FdoProfileAttributes(String attribute, String defaultValue){
    this.attribute = attribute;
    this.defaultValue = defaultValue;
  }
}
