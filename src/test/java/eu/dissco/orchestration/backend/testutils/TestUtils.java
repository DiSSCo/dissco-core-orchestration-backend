package eu.dissco.orchestration.backend.testutils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.dissco.orchestration.backend.domain.HandleType;
import eu.dissco.orchestration.backend.domain.MachineAnnotationService;
import eu.dissco.orchestration.backend.domain.MachineAnnotationServiceRecord;
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
  public static final String MAS_URI = "/mas";
  public static final String MAS_PATH = SANDBOX_URI + MAS_URI;


  private TestUtils() {
    throw new IllegalStateException("Utility class");
  }

  public static JsonApiWrapper givenSourceSystemSingleJsonApiWrapper() {
    return givenSourceSystemSingleJsonApiWrapper(1);
  }

  public static JsonApiWrapper givenSourceSystemSingleJsonApiWrapper(int version) {
    var sourceSystemRecord = givenSourceSystemRecord(version);
    return new JsonApiWrapper(new JsonApiData(
        sourceSystemRecord.id(),
        HandleType.SOURCE_SYSTEM,
        flattenSourceSystemRecord(sourceSystemRecord)
    ), new JsonApiLinks(SYSTEM_PATH));
  }

  public static JsonApiWrapper givenMasSingleJsonApiWrapper() {
    return givenMasSingleJsonApiWrapper(1);
  }

  public static JsonApiWrapper givenMasSingleJsonApiWrapper(int version) {
    var masRecord = givenMasRecord(version);
    return new JsonApiWrapper(new JsonApiData(
        masRecord.id(),
        HandleType.MACHINE_ANNOTATION_SERVICE,
        flattenMasRecord(masRecord)
    ), new JsonApiLinks(MAS_PATH));
  }

  public static JsonApiWrapper givenMappingSingleJsonApiWrapper() {
    return givenMappingSingleJsonApiWrapper(1);
  }

  public static JsonApiWrapper givenMappingSingleJsonApiWrapper(int version) {
    var mappingRecord = givenMappingRecord(HANDLE, version);
    return new JsonApiWrapper(new JsonApiData(
        mappingRecord.id(),
        HandleType.MAPPING,
        flattenMappingRecord(mappingRecord)
    ), new JsonApiLinks(MAPPING_PATH));
  }

  public static JsonApiRequestWrapper givenSourceSystemRequest() {
    return new JsonApiRequestWrapper(
        new JsonApiRequest(
            HandleType.SOURCE_SYSTEM,
            MAPPER.valueToTree(givenSourceSystem())
        )
    );
  }

  public static JsonApiRequestWrapper givenMappingRequest() {
    return new JsonApiRequestWrapper(
        new JsonApiRequest(
            HandleType.MAPPING,
            MAPPER.valueToTree(givenMapping())
        )
    );
  }

  public static SourceSystemRecord givenSourceSystemRecord() {
    return givenSourceSystemRecord(1);
  }

  public static SourceSystemRecord givenSourceSystemRecord(int version) {
    return new SourceSystemRecord(
        HANDLE,
        version,
        OBJECT_CREATOR,
        CREATED,
        null, givenSourceSystem()
    );
  }

  public static SourceSystem givenSourceSystem() {
    return new SourceSystem(
        OBJECT_NAME,
        SS_ENDPOINT,
        OBJECT_DESCRIPTION,
        HANDLE_ALT
    );
  }

  public static JsonApiListWrapper givenSourceSystemRecordResponse(
      List<SourceSystemRecord> ssRecords, JsonApiLinks linksNode) {
    List<JsonApiData> dataNode = new ArrayList<>();
    ssRecords.forEach(ss -> dataNode.add(
        new JsonApiData(ss.id(), HandleType.SOURCE_SYSTEM, flattenSourceSystemRecord(ss))));
    return new JsonApiListWrapper(dataNode, linksNode);
  }

  public static JsonApiListWrapper givenMasRecordResponse(
      List<MachineAnnotationServiceRecord> masRecords, JsonApiLinks linksNode) {
    var dataNode = masRecords.stream().map(
        mas -> new JsonApiData(mas.id(), HandleType.MACHINE_ANNOTATION_SERVICE,
            flattenMasRecord(mas))).toList();
    return new JsonApiListWrapper(dataNode, linksNode);
  }

  public static JsonNode flattenSourceSystemRecord(SourceSystemRecord sourceSystemRecord) {
    var sourceSystemNode = (ObjectNode) MAPPER.valueToTree(sourceSystemRecord.sourceSystem());
    sourceSystemNode.put("created", sourceSystemRecord.created().toString());
    sourceSystemNode.put("version", sourceSystemRecord.version());
    if (sourceSystemRecord.deleted() != null) {
      sourceSystemNode.put("deleted", sourceSystemRecord.deleted().toString());
    }
    return sourceSystemNode;
  }

  public static JsonNode flattenMasRecord(MachineAnnotationServiceRecord masRecord) {
    var masNode = (ObjectNode) MAPPER.valueToTree(masRecord.mas());
    masNode.put("version", masRecord.version());
    masNode.put("created", masRecord.created().toString());
    if (masRecord.deleted() != null) {
      masNode.put("deleted", masRecord.deleted().toString());
    }
    return masNode;
  }


  public static MappingRecord givenMappingRecord(String id, int version) {
    return new MappingRecord(
        id,
        version,
        CREATED,
        null, OBJECT_CREATOR,
        givenMapping()
    );
  }

  public static Mapping givenMapping() {
    return new Mapping(
        OBJECT_NAME,
        OBJECT_DESCRIPTION,
        MAPPER.createObjectNode(),
        "dwc");
  }

  public static JsonApiListWrapper givenMappingRecordResponse(List<MappingRecord> mappingRecords,
      JsonApiLinks linksNode) {
    List<JsonApiData> dataNode = new ArrayList<>();
    mappingRecords.forEach(
        m -> dataNode.add(new JsonApiData(m.id(), HandleType.MAPPING, flattenMappingRecord(m))));
    return new JsonApiListWrapper(dataNode, linksNode);
  }

  public static JsonNode flattenMappingRecord(MappingRecord mappingRecord) {
    var mappingNode = (ObjectNode) MAPPER.valueToTree(mappingRecord.mapping());
    mappingNode.put("version", mappingRecord.version());
    mappingNode.put("created", mappingRecord.created().toString());
    if (mappingRecord.deleted() != null) {
      mappingNode.put("deleted", mappingRecord.deleted().toString());
    }
    return mappingNode;
  }

  public static JsonApiRequestWrapper givenMasRequest() {
    return new JsonApiRequestWrapper(new JsonApiRequest(
        HandleType.MACHINE_ANNOTATION_SERVICE,
        MAPPER.valueToTree(givenMas())
    ));
  }

  public static MachineAnnotationServiceRecord givenMasRecord() {
    return givenMasRecord(1);
  }

  public static MachineAnnotationServiceRecord givenMasRecord(int version) {
    return new MachineAnnotationServiceRecord(
        HANDLE,
        version,
        CREATED,
        OBJECT_CREATOR,
        givenMas(),
        null
    );
  }

  public static MachineAnnotationService givenMas() {
    return new MachineAnnotationService(
        "A Machine Annotation Service",
        "public.ecr.aws/dissco/fancy-mas",
        "sha-54289",
        MAPPER.createObjectNode(),
        "A fancy mas making all dreams come true",
        "Definitely production ready",
        "https://github.com/DiSSCo/fancy-mas",
        "public",
        "No one we know",
        "https://www.apache.org/licenses/LICENSE-2.0",
        List.of(),
        "dontmail@dissco.eu",
        "https://www.know.dissco.tech/no_sla",
        "fancy-topic-name",
        5,
        false
    );
  }

  public static JsonNode givenMasHandleRequest() throws Exception {
    return MAPPER.readTree("""
        {
          "data": {
            "type": "machineAnnotationService",
            "attributes": {
              "fdoProfile": "https://hdl.handle.net/21.T11148/64396cf36b976ad08267",
              "issuedForAgent": "https://ror.org/0566bfb96",
              "digitalObjectType": "https://hdl.handle.net/21.T11148/64396cf36b976ad08267",
              "machineAnnotationServiceName":"A Machine Annotation Service"
            }
          }
        }""");
  }

  public static JsonNode givenSourceSystemHandleRequest() throws Exception{
    return MAPPER.readTree("""
        {
          "data": {
            "type": "sourceSystem",
            "attributes": {
              "fdoProfile": "https://hdl.handle.net/21.T11148/64396cf36b976ad08267",
              "issuedForAgent": "https://ror.org/0566bfb96",
              "digitalObjectType": "https://hdl.handle.net/21.T11148/64396cf36b976ad08267",
              "sourceSystemName":"Naturalis Tunicate DWCA endpoint"
            }
          }
        }""");
  }

  public static JsonNode givenMappingHandleRequest() throws Exception {
    return MAPPER.readTree("""
        {
          "data": {
            "type": "mapping",
            "attributes": {
              "fdoProfile": "https://hdl.handle.net/21.T11148/64396cf36b976ad08267",
              "issuedForAgent": "https://ror.org/0566bfb96",
              "digitalObjectType": "https://hdl.handle.net/21.T11148/b3f1045d8524d863ccfb",
              "sourceDataStandard": "dwc"
            }
          }
        }""");
  }

  public  static JsonNode givenRollbackCreationRequest() throws Exception{
    return MAPPER.readTree("""
        {
          "data":
            [{"id":"20.5000.1025/GW0-POP-XSL"}]
        }""");
  }
}
