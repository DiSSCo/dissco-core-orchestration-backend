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
  private String sourceSystemType = "https://hdl.handle.net/21.T11148/417a4f472f60f7974c12";
  @NotBlank
  private String mappingType = "https://hdl.handle.net/21.T11148/ce794a6f4df42eb7e77e";
  @NotBlank
  private String masType = "https://hdl.handle.net/21.T11148/22e71a0015cbcfba8ffa";

  @NotBlank
  private String issuedForAgent = "https://ror.org/0566bfb96";


}
