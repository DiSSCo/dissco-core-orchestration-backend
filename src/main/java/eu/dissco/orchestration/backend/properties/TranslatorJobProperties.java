package eu.dissco.orchestration.backend.properties;

import jakarta.validation.Valid;
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

  @Valid
  private Export export = new Export();

  private RabbitMq rabbitMq = new RabbitMq();

  @Data
  @Validated
  public static class Export {

    @NotBlank
    private String exportImage;
    @NotBlank
    private String keycloak;
    @NotBlank
    private String disscoDomain;
    @NotBlank
    private String namespace = "data-export-job";
  }

  @Data
  public static class RabbitMq{
    private String exchangeName;

    private String routingKeyName;
  }
}
