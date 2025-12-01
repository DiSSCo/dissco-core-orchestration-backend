package eu.dissco.orchestration.backend.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "rabbitmq")
public class RabbitMqProperties {
  @NotBlank
  private String provenanceExchangeName = "provenance-exchange";

  @NotNull
  private String provenanceRoutingKeyPrefix = "provenance";
}
