package eu.dissco.orchestration.backend.service;

import static eu.dissco.orchestration.backend.domain.FdoProfileAttributes.ISSUED_FOR_AGENT;
import static eu.dissco.orchestration.backend.domain.FdoProfileAttributes.MAS_NAME;
import static eu.dissco.orchestration.backend.domain.FdoProfileAttributes.SOURCE_DATA_STANDARD;
import static eu.dissco.orchestration.backend.domain.FdoProfileAttributes.SOURCE_SYSTEM_NAME;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.dissco.orchestration.backend.domain.MachineAnnotationService;
import eu.dissco.orchestration.backend.domain.Mapping;
import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.domain.SourceSystem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FdoRecordService {

  private final ObjectMapper mapper;
  private static final String ISSUED_FOR_AGENT_ROR = "https://ror.org/0566bfb96";

  public JsonNode buildCreateRequest(Object object, ObjectType type) {
    var request = mapper.createObjectNode();
    var data = mapper.createObjectNode();
    data.put("type", type.getFdoProfile());
    var attributes = buildRequestAttributes(object, type);
    data.set("attributes", attributes);
    request.set("data", data);
    return request;
  }

  public JsonNode buildRollbackCreateRequest(String handle) {
    var dataNode = List.of(mapper.createObjectNode().put("id", handle));
    ArrayNode dataArrayNode= mapper.valueToTree(dataNode);
    return mapper.createObjectNode().set("data", dataArrayNode);
  }

  private JsonNode buildRequestAttributes(Object object, ObjectType type) {
    switch (type) {
      case MAPPING -> {
        return buildMappingAttributes(((Mapping) object));
      }
      case MAS -> {
        return buildMasAttributes((MachineAnnotationService) object);
      }
      case SOURCE_SYSTEM -> {
        return buildSourceSystemAttributes((SourceSystem) object);
      }
    }
    throw new IllegalStateException();
  }

  private JsonNode buildMappingAttributes(Mapping mapping) {
    return buildGeneralAttributes()
        .put(SOURCE_DATA_STANDARD.getAttribute(), mapping.sourceDataStandard());
  }

  private JsonNode buildMasAttributes(MachineAnnotationService mas) {
    return buildGeneralAttributes()
        .put(MAS_NAME.getAttribute(), mas.getName());
  }

  private JsonNode buildSourceSystemAttributes(SourceSystem sourceSystem) {
    return buildGeneralAttributes()
        .put(SOURCE_SYSTEM_NAME.getAttribute(), sourceSystem.name());
  }

  private ObjectNode buildGeneralAttributes() {
    return mapper.createObjectNode()
            .put(ISSUED_FOR_AGENT.getAttribute(), ISSUED_FOR_AGENT_ROR);
  }

}
