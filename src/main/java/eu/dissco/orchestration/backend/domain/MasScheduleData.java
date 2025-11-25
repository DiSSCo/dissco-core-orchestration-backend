package eu.dissco.orchestration.backend.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

@Schema
public record MasScheduleData (
    @Schema(description = "Schedule MASs on all objects, not just new objects") boolean forceMasSchedule,
    @Schema(description = "Handles of MASs to run on specimens") Set<String> specimenMass,
    @Schema(description = "Handles of MASs to run on media") Set<String> mediaMass
) {

  public MasScheduleData(){
    this(false, Set.of(), Set.of());
  }

}
