package eu.dissco.orchestration.backend.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
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
  private String kafkaHost = "kafka.kafka.svc.cluster.local:9092";

  @NotBlank
  private String kafkaTopic = "digital-specimen";

  @NotBlank
  private String namespace = "default";

}
