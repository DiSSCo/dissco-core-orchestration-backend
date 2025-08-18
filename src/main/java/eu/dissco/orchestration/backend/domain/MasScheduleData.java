package eu.dissco.orchestration.backend.domain;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor
public class MasScheduleData {
  boolean forceMasSchedule;
  List<String> additionalSpecimenMass;
  List<String> additionalMediaMass;

  public MasScheduleData(){
    forceMasSchedule = false;
    additionalSpecimenMass = List.of();
    additionalMediaMass = List.of();
  }

}
