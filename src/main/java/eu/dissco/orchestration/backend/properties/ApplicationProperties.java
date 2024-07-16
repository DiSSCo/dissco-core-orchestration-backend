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

  @NotBlank
  private String name = "dissco-core-orchestration-backend";

  @NotBlank
  private String pid = "https://hdl.handle.net/TEST/123-123-123";

  @NotBlank
  private String createUpdateTombstoneEventType = "https://hdl.handle.net/TEST/123-123-123";
}
