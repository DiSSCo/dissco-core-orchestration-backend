package eu.dissco.orchestration.backend.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public final class MachineAnnotationService {

  private final @NotBlank String name;
  private final @NotBlank String containerImage;
  private final @NotBlank String containerTag;
  private final JsonNode targetDigitalObjectFilters;
  private final String serviceDescription;
  private final String serviceState;
  private final String sourceCodeRepository;
  private final String serviceAvailability;
  private final String codeMaintainer;
  private final String codeLicense;
  private final List<String> dependencies;
  private final String supportContact;
  private final String slaDocumentation;
  private String topicName;
  private int maxReplicas;
  private MasInput masInput;
}
