package eu.dissco.orchestration.backend.domain;

import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TranslatorResponse {

  String jobName;
  String jobStatus;
  Instant startTime;
  Instant completedAt;
}
