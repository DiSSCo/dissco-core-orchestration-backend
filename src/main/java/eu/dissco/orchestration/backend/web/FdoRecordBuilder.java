package eu.dissco.orchestration.backend.web;

import static eu.dissco.orchestration.backend.domain.FdoProfileAttributes.DIGITAL_OBJECT_TYPE;
import static eu.dissco.orchestration.backend.domain.FdoProfileAttributes.FDO_PROFILE;
import static eu.dissco.orchestration.backend.domain.FdoProfileAttributes.ISSUED_FOR_AGENT;
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
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FdoRecordBuilder {

  private final ObjectMapper mapper;

  public JsonNode buildCreateRequest(Object object, ObjectType type) {
    var request = mapper.createObjectNode();
    var data = mapper.createObjectNode();
    data.put("type", type.getObjectType());
    var attributes = buildRequestAttributes(object, type);
    data.set("attributes", attributes);
    request.set("data", data);
    return request;
  }

  public JsonNode buildRollbackUpdateRequest(Object object, ObjectType type, String handle) {
    var subRequest = (ObjectNode) buildCreateRequest(object, type);
    ((ObjectNode) subRequest.get("data")).put("id", handle);
    return subRequest;
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
    throw new IllegalStateException("Invalid Object type " + type.getObjectType());
  }

  private JsonNode buildMappingAttributes(Mapping mapping) {
    var attributes = buildGeneralAttributes(ObjectType.MAPPING);
    attributes.put(SOURCE_DATA_STANDARD.getAttribute(), mapping.sourceDataStandard());
    return attributes;
  }

  private JsonNode buildMasAttributes(MachineAnnotationService mas) {
    return buildGeneralAttributes(ObjectType.MAS);
  }

  private JsonNode buildSourceSystemAttributes(SourceSystem sourceSystem) {
    var attributes = buildGeneralAttributes(ObjectType.SOURCE_SYSTEM);
    attributes.put(SOURCE_SYSTEM_NAME.getAttribute(), sourceSystem.name());
    return attributes;
  }

  private ObjectNode buildGeneralAttributes(ObjectType type) {
    var attributes = mapper.createObjectNode();
    attributes.put(FDO_PROFILE.getAttribute(), FDO_PROFILE.getDefaultValue());
    attributes.put(ISSUED_FOR_AGENT.getAttribute(), ISSUED_FOR_AGENT.getDefaultValue());
    attributes.put(DIGITAL_OBJECT_TYPE.getAttribute(), type.getFdoProfile());
    return attributes;
  }


}
