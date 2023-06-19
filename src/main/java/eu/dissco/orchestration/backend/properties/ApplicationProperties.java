package eu.dissco.orchestration.backend.properties;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Component
@ConfigurationProperties("application")
public class ApplicationProperties {

  @NotBlank
  private String baseUrl;
}
