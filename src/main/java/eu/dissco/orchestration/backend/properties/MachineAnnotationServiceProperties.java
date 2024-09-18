package eu.dissco.orchestration.backend.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties("mas")
public class MachineAnnotationServiceProperties {

  @NotBlank
  private String namespace = "machine-annotation-services";

  @NotBlank
  private String kafkaHost;

  @NotBlank
  private String masSecretStore = "mas-secrets";

  @NotBlank
  private String runningEndpoint;

}
