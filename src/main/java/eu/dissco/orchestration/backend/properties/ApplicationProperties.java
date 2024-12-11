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
  private String name = "DiSSCo Orchestration Backend";

  @NotBlank
  private String pid = "https://doi.org/10.5281/zenodo.14383664";

  @NotBlank
  private String createUpdateTombstoneEventType = "https://doi.org/21.T11148/d7570227982f70256af3";
}
