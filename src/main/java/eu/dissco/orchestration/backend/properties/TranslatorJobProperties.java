package eu.dissco.orchestration.backend.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Component
@ConfigurationProperties("translator-job")
public class TranslatorJobProperties {

  @NotBlank
  private String image = "public.ecr.aws/dissco/dissco-core-translator:latest";

  @NotBlank
  private String namespace = "translator-services";

  @NotBlank
  @Value("${spring.datasource.url}")
  private String databaseUrl;

}
