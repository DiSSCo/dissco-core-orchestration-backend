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
  private String sourceSystemType = "https://doi.org/21.T11148/23a63913d0c800609a50";
  @NotBlank
  private String dataMappingType = "https://doi.org/21.T11148/ce794a6f4df42eb7e77e";
  @NotBlank
  private String masType = "https://doi.org/21.T11148/a369e128df5ef31044d4";

}
