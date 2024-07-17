package eu.dissco.orchestration.backend.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Setter
@Getter
@Validated
@ConfigurationProperties("k8s")
public class KubernetesProperties {

  @NotNull
  private Duration apiPingInterval = Duration.ofSeconds(15L);

  @NotNull
  private Duration apiWriteTimeout = Duration.ofSeconds(30L);

  @NotNull
  private Duration apiReadTimeout = Duration.ofMinutes(2L);

  @NotNull
  private Duration apiConnectTimeout = Duration.ofSeconds(15L);

  @NotBlank
  private String kedaGroup = "keda.sh";

  @NotBlank
  private String kedaVersion = "v1alpha1";

  @NotBlank
  private String kedaResource = "scaledobjects";

  @Positive
  private int kedaPatchWait = 500;

}
