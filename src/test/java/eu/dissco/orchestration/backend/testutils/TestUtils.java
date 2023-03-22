package eu.dissco.orchestration.backend.testutils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.dissco.orchestration.backend.domain.HandleType;
import eu.dissco.orchestration.backend.domain.Mapping;
import eu.dissco.orchestration.backend.domain.MappingRecord;
import eu.dissco.orchestration.backend.domain.SourceSystem;
import eu.dissco.orchestration.backend.domain.SourceSystemRecord;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiData;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiListWrapper;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiRequest;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiRequestWrapper;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TestUtils {
  public static final String OBJECT_NAME = "Naturalis Tunicate DWCA endpoint";
  public static final String OBJECT_CREATOR = "e2befba6-9324-4bb4-9f41-d7dfae4a44b0";
  public static final String PREFIX = "20.5000.1025";
  public static final String SUFFIX = "GW0-POP-XSL";
  public static final String HANDLE = PREFIX + "/" + SUFFIX;
  public static final String HANDLE_ALT = PREFIX + "/EMK-X81-1QZ";
  public static final String SS_ENDPOINT = "https://api.biodiversitydata.nl/v2/specimen/dwca/getDataSet/tunicata";
  public static final String OBJECT_DESCRIPTION = "Source system for the DWCA of the Tunicate specimen";
  public static final Instant CREATED = Instant.parse("2022-11-01T09:59:24.00Z");
  public static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
  public static final String SANDBOX_URI = "https://sandbox.dissco.tech/orchestrator";
  public static final String SYSTEM_URI = "/source-system";
  public static final String SYSTEM_PATH = SANDBOX_URI + SYSTEM_URI;
  public static final String MAPPING_URI = "/mapping";
  public static final String MAPPING_PATH = SANDBOX_URI + MAPPING_URI;


  private TestUtils() {
    throw new IllegalStateException("Utility class");
  }

  public static JsonApiWrapper givenSourceSystemSingleJsonApiWrapper(){
    var sourceSystemRecord = givenSourceSystemRecord();
    return new JsonApiWrapper(new JsonApiData(
        sourceSystemRecord.id(),
        HandleType.SOURCE_SYSTEM,
        flattenSourceSystemRecord(sourceSystemRecord)
    ), new JsonApiLinks(SYSTEM_PATH));
  }

  public static JsonApiWrapper givenMappingSingleJsonApiWrapper(){
    var mappingRecord = givenMappingRecord(HANDLE, 1);
    return new JsonApiWrapper(new JsonApiData(
        mappingRecord.id(),
        HandleType.MAPPING,
      flattenMappingRecord(mappingRecord)
    ), new JsonApiLinks(MAPPING_PATH));
  }

  public static JsonApiRequestWrapper givenSourceSystemRequest(){
    return new JsonApiRequestWrapper(
        new JsonApiRequest(
            HandleType.SOURCE_SYSTEM,
            MAPPER.valueToTree(givenSourceSystem())
        )
    );
  }

  public static JsonApiRequestWrapper givenMappingRequest(){
    return new JsonApiRequestWrapper(
        new JsonApiRequest(
            HandleType.MAPPING,
            MAPPER.valueToTree(givenMapping())
        )
    );
  }

  public static SourceSystemRecord givenSourceSystemRecord(){
    return new SourceSystemRecord(
        HANDLE,
        CREATED,
        null, givenSourceSystem()
    );
  }

  public static SourceSystem givenSourceSystem(){
    return new SourceSystem(
        OBJECT_NAME,
        SS_ENDPOINT,
        OBJECT_DESCRIPTION,
        HANDLE_ALT
    );
  }

  public static JsonApiListWrapper givenSourceSystemRecordResponse(List<SourceSystemRecord> ssRecords, JsonApiLinks linksNode){
    List<JsonApiData> dataNode = new ArrayList<>();
    ssRecords.forEach(ss -> dataNode.add(new JsonApiData(ss.id(), HandleType.SOURCE_SYSTEM, flattenSourceSystemRecord(ss))));
    return new JsonApiListWrapper(dataNode, linksNode);
  }


  public static JsonNode flattenSourceSystemRecord(SourceSystemRecord sourceSystemRecord){
    var sourceSystemNode =  (ObjectNode) MAPPER.valueToTree(sourceSystemRecord.sourceSystem());
    sourceSystemNode.put("created", sourceSystemRecord.created().toString());
    if (sourceSystemRecord.deleted() != null){
      sourceSystemNode.put("deleted", sourceSystemRecord.deleted().toString());
    }
    return sourceSystemNode;
  }


  public static MappingRecord givenMappingRecord(String id, int version){
    return new MappingRecord(
        id,
        version,
        CREATED,
        null, OBJECT_CREATOR,
        givenMapping()
    );
  }

  public static Mapping givenMapping(){
    return new Mapping(
        OBJECT_NAME,
        OBJECT_DESCRIPTION,
        MAPPER.createObjectNode(),
        "dwc");
  }

  public static JsonApiListWrapper givenMappingRecordResponse(List<MappingRecord> mappingRecords, JsonApiLinks linksNode){
    List<JsonApiData> dataNode = new ArrayList<>();
    mappingRecords.forEach(m -> dataNode.add(new JsonApiData(m.id(), HandleType.MAPPING, flattenMappingRecord(m))));
    return new JsonApiListWrapper(dataNode, linksNode);
  }

  public static JsonNode flattenMappingRecord(MappingRecord mappingRecord){
    var mappingNode = (ObjectNode) MAPPER.valueToTree(mappingRecord.mapping());
    mappingNode.put("version", mappingRecord.version());
    mappingNode.put("created", mappingRecord.created().toString());
    if (mappingRecord.deleted() != null){
      mappingNode.put("deleted", mappingRecord.deleted().toString());
    }
    return mappingNode;
  }


}
