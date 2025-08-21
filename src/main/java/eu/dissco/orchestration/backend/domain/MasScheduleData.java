package eu.dissco.orchestration.backend.domain;

import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor
public class MasScheduleData {
  boolean forceMasSchedule;
  Set<String> specimenMass;
  Set<String> mediaMass;

  public MasScheduleData(){
    forceMasSchedule = false;
    specimenMass = Set.of();
    mediaMass = Set.of();
  }

}
