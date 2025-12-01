package eu.dissco.orchestration.backend.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "rabbitmq")
public class RabbitMqProperties {

  @NotBlank
  private String provenanceExchangeName = "provenance-exchange";

  @NotBlank
  private String provenanceRoutingKeyPrefix = "provenance";
}
