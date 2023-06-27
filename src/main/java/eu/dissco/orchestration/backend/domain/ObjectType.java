package eu.dissco.orchestration.backend.domain;

public enum ObjectType {
  SOURCE_SYSTEM ("sourceSystem", "http://hdl.handle.net/21.T11148/64396cf36b976ad08267"),
  MAPPING ("mapping", "http://hdl.handle.net/21.T11148/64396cf36b976ad08267"),
  MAS("machineAnnotationService", "http://hdl.handle.net/21.T11148/64396cf36b976ad08267");

  private final String type;
  private final String fdoProfile;

  private ObjectType(String type, String fdoProfile){
    this.type = type;
    this.fdoProfile = fdoProfile;
  }

  public String getObjectType(){ return type;}

  public String getFdoProfile(){return fdoProfile;}

}
