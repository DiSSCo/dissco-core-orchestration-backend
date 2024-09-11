package eu.dissco.orchestration.backend.service;

import static eu.dissco.orchestration.backend.domain.FdoProfileAttributes.ISSUED_FOR_AGENT;
import static eu.dissco.orchestration.backend.domain.FdoProfileAttributes.MAS_NAME;
import static eu.dissco.orchestration.backend.domain.FdoProfileAttributes.SOURCE_DATA_STANDARD;
import static eu.dissco.orchestration.backend.domain.FdoProfileAttributes.SOURCE_SYSTEM_NAME;
import static eu.dissco.orchestration.backend.utils.HandleUtils.removeProxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.properties.FdoProperties;
import eu.dissco.orchestration.backend.schema.DataMappingRequest;
import eu.dissco.orchestration.backend.schema.MachineAnnotationServiceRequest;
import eu.dissco.orchestration.backend.schema.SourceSystemRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FdoRecordService {

  private final ObjectMapper mapper;
  private final FdoProperties fdoProperties;

  public JsonNode buildCreateRequest(Object object, ObjectType type) {
    var request = mapper.createObjectNode();
    var data = mapper.createObjectNode();
    data.put("type", getFdoType(type));
    var attributes = buildRequestAttributes(object, type);
    data.set("attributes", attributes);
    request.set("data", data);
    return request;
  }

  public JsonNode buildTombstoneRequest(ObjectType type, String handle) {
    return mapper.createObjectNode()
        .set("data", mapper.createObjectNode()
            .put("type", getFdoType(type))
            .put("id", handle)
            .set("attributes", mapper.createObjectNode()
                .put("tombstoneText", type.getFullName() + " tombstoned by user through the orchestration backend")));
  }

  public JsonNode buildRollbackCreateRequest(String handle) {
    var dataNode = List.of(mapper.createObjectNode().put("id", removeProxy(handle)));
    ArrayNode dataArrayNode = mapper.valueToTree(dataNode);
    return mapper.createObjectNode().set("data", dataArrayNode);
  }

  private JsonNode buildRequestAttributes(Object object, ObjectType type) {
    switch (type) {
      case DATA_MAPPING -> {
        return buildMappingAttributes(((DataMappingRequest) object));
      }
      case MAS -> {
        return buildMasAttributes((MachineAnnotationServiceRequest) object);
      }
      case SOURCE_SYSTEM -> {
        return buildSourceSystemAttributes((SourceSystemRequest) object);
      }
    }
    throw new IllegalStateException();
  }

  private JsonNode buildMappingAttributes(DataMappingRequest mapping) {
    return buildGeneralAttributes()
        .put(SOURCE_DATA_STANDARD.getAttribute(), mapping.getOdsMappingDataStandard().toString());
  }

  private JsonNode buildMasAttributes(MachineAnnotationServiceRequest mas) {
    return buildGeneralAttributes()
        .put(MAS_NAME.getAttribute(), mas.getSchemaName());
  }

  private JsonNode buildSourceSystemAttributes(SourceSystemRequest sourceSystemRequest) {
    return buildGeneralAttributes()
        .put(SOURCE_SYSTEM_NAME.getAttribute(), sourceSystemRequest.getSchemaName());
  }

  private String getFdoType(ObjectType type) {
    switch (type) {
      case MAS -> {
        return fdoProperties.getMasType();
      }
      case DATA_MAPPING -> {
        return fdoProperties.getDataMappingType();
      }
      case SOURCE_SYSTEM -> {
        return fdoProperties.getSourceSystemType();
      }
      default -> throw new IllegalStateException();
    }
  }

  private ObjectNode buildGeneralAttributes() {
    return mapper.createObjectNode()
        .put(ISSUED_FOR_AGENT.getAttribute(), fdoProperties.getIssuedForAgent());
  }

}
