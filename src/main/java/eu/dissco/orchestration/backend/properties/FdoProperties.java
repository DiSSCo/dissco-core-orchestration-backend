package eu.dissco.orchestration.backend.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties("fdo")
public class FdoProperties {

  @NotBlank
  private String sourceSystemType = "https://doi.org/21.T11148/417a4f472f60f7974c12";
  @NotBlank
  private String dataMappingType = "https://doi.org/21.T11148/ce794a6f4df42eb7e77e";
  @NotBlank
  private String masType = "https://doi.org/21.T11148/22e71a0015cbcfba8ffa";

}
