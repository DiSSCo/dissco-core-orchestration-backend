package eu.dissco.orchestration.backend.domain;

public enum ObjectType {
  SOURCE_SYSTEM ("sourceSystem", "https://hdl.handle.net/21.T11148/64396cf36b976ad08267"),
  MAPPING ("mapping", "https://hdl.handle.net/21.T11148/b3f1045d8524d863ccfb"),
  MAS("machineAnnotationService", "https://hdl.handle.net/21.T11148/64396cf36b976ad08267");

  private final String type;
  private final String fdoProfile;

  private ObjectType(String type, String fdoProfile){
    this.type = type;
    this.fdoProfile = fdoProfile;
  }

  public String getObjectType(){ return type;}

  public String getFdoProfile(){return fdoProfile;}

}
