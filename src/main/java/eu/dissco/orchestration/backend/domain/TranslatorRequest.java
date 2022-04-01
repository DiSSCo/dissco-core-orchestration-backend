package eu.dissco.orchestration.backend.domain;

import lombok.Data;

@Data
public class TranslatorRequest {

  private String serviceName;
  private TranslatorType translatorType;
  private String endPoint;
  private String query;
  private Integer itemsPerRequest;

}
