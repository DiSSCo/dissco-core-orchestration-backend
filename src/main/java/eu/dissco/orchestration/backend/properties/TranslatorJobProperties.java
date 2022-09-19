package eu.dissco.orchestration.backend.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@Component
@ConfigurationProperties("translator-job")
public class TranslatorJobProperties {

  private String image = "public.ecr.aws/dissco/dissco-core-translator:latest";

  private String kafkaHost = "kafka.kafka.svc.cluster.local:9092";

  private String kafkaTopic = "digital-specimen";

}
