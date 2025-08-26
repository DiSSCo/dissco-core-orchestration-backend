package eu.dissco.orchestration.backend.domain;

import java.util.Set;

public record MasScheduleData (
    boolean forceMasSchedule,
    Set<String> specimenMass,
    Set<String> mediaMass
) {

  public MasScheduleData(){
    this(false, Set.of(), Set.of());
  }

}
