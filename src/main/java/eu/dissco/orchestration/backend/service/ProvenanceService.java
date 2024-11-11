package eu.dissco.orchestration.backend.service;

import static eu.dissco.orchestration.backend.domain.AgentRoleType.CREATOR;
import static eu.dissco.orchestration.backend.domain.AgentRoleType.PROCESSING_SERVICE;
import static eu.dissco.orchestration.backend.schema.Agent.Type.PROV_PERSON;
import static eu.dissco.orchestration.backend.schema.Agent.Type.PROV_SOFTWARE_AGENT;
import static eu.dissco.orchestration.backend.utils.AgentUtils.createMachineAgent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.diff.JsonDiff;
import eu.dissco.orchestration.backend.properties.ApplicationProperties;
import eu.dissco.orchestration.backend.schema.Agent;
import eu.dissco.orchestration.backend.schema.CreateUpdateTombstoneEvent;
import eu.dissco.orchestration.backend.schema.Identifier.DctermsType;
import eu.dissco.orchestration.backend.schema.OdsChangeValue;
import eu.dissco.orchestration.backend.schema.ProvActivity;
import eu.dissco.orchestration.backend.schema.ProvEntity;
import eu.dissco.orchestration.backend.schema.ProvValue;
import eu.dissco.orchestration.backend.schema.ProvWasAssociatedWith;
import eu.dissco.orchestration.backend.schema.ProvWasAssociatedWith.ProvHadRole;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProvenanceService {

  private final ObjectMapper mapper;
  private final ApplicationProperties properties;

  private static String getRdfsComment(ProvActivity.Type activityType) {
    switch (activityType) {
      case ODS_CREATE -> {
        return "Object newly created";
      }
      case ODS_UPDATE -> {
        return "Object updated";
      }
      case ODS_TOMBSTONE -> {
        return "Object tombstoned";
      }
    }
    return null;
  }

  public CreateUpdateTombstoneEvent generateCreateEvent(JsonNode digitalObject, Agent agent) {
    return generateCreateUpdateTombStoneEvent(digitalObject, ProvActivity.Type.ODS_CREATE, null,
        agent);
  }

  public CreateUpdateTombstoneEvent generateTombstoneEvent(JsonNode tombstoneObject,
      JsonNode currentObject, Agent agent) {
    var patch = createJsonPatch(tombstoneObject, currentObject);
    return generateCreateUpdateTombStoneEvent(tombstoneObject, ProvActivity.Type.ODS_TOMBSTONE,
        patch, agent);
  }

  private CreateUpdateTombstoneEvent generateCreateUpdateTombStoneEvent(
      JsonNode digitalObject, ProvActivity.Type activityType, JsonNode jsonPatch, Agent agent) {
    var entityID =
        digitalObject.get("@id").asText() + "/" + digitalObject.get("schema:version").asText();
    var activityID = UUID.randomUUID().toString();
    return new CreateUpdateTombstoneEvent()
        .withId(entityID)
        .withType("ods:CreateUpdateTombstoneEvent")
        .withDctermsIdentifier(entityID)
        .withOdsFdoType(properties.getCreateUpdateTombstoneEventType())
        .withProvActivity(new ProvActivity()
            .withId(activityID)
            .withType(activityType)
            .withOdsChangeValue(mapJsonPatch(jsonPatch))
            .withProvEndedAtTime(Date.from(Instant.now()))
            .withProvWasAssociatedWith(List.of(
                new ProvWasAssociatedWith()
                    .withId(agent.getId())
                    .withProvHadRole(ProvHadRole.REQUESTOR),
                new ProvWasAssociatedWith()
                    .withId(agent.getId())
                    .withProvHadRole(ProvHadRole.APPROVER),
                new ProvWasAssociatedWith()
                    .withId(properties.getPid())
                    .withProvHadRole(ProvHadRole.GENERATOR)))
            .withProvUsed(entityID)
            .withRdfsComment(getRdfsComment(activityType)))
        .withProvEntity(new ProvEntity()
            .withId(entityID)
            .withType(digitalObject.get("@type").textValue())
            .withProvValue(mapEntityToProvValue(digitalObject))
            .withProvWasGeneratedBy(activityID))
        .withOdsHasAgents(
            List.of(createMachineAgent(agent.getSchemaName(), agent.getId(), CREATOR,
                    "orcid", PROV_PERSON),
                createMachineAgent(properties.getName(), properties.getPid(),
                    PROCESSING_SERVICE, DctermsType.DOI.value(), PROV_SOFTWARE_AGENT)));
  }

  private List<OdsChangeValue> mapJsonPatch(JsonNode jsonPatch) {
    if (jsonPatch == null) {
      return null;
    }
    return mapper.convertValue(jsonPatch, new TypeReference<>() {
    });
  }

  public CreateUpdateTombstoneEvent generateUpdateEvent(JsonNode digitalObject,
      JsonNode currentDigitalObject, Agent agent) {
    var jsonPatch = createJsonPatch(digitalObject, currentDigitalObject);
    return generateCreateUpdateTombStoneEvent(digitalObject, ProvActivity.Type.ODS_UPDATE,
        jsonPatch, agent);
  }

  private ProvValue mapEntityToProvValue(JsonNode jsonNode) {
    var provValue = new ProvValue();
    var node = mapper.convertValue(jsonNode, new TypeReference<Map<String, Object>>() {
    });
    for (var entry : node.entrySet()) {
      provValue.setAdditionalProperty(entry.getKey(), entry.getValue());
    }
    return provValue;
  }

  private JsonNode createJsonPatch(JsonNode digitalObject, JsonNode currentDigitalObject) {
    return JsonDiff.asJson(currentDigitalObject, digitalObject);
  }
}
