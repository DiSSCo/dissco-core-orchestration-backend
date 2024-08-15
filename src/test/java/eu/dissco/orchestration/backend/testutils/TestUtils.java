package eu.dissco.orchestration.backend.testutils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiData;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiLinks;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiListWrapper;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiRequest;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiRequestWrapper;
import eu.dissco.orchestration.backend.domain.jsonapi.JsonApiWrapper;
import eu.dissco.orchestration.backend.schema.Agent;
import eu.dissco.orchestration.backend.schema.Agent.Type;
import eu.dissco.orchestration.backend.schema.DataMapping;
import eu.dissco.orchestration.backend.schema.DataMappingRequest;
import eu.dissco.orchestration.backend.schema.DataMappingRequest.OdsMappingDataStandard;
import eu.dissco.orchestration.backend.schema.DefaultMapping;
import eu.dissco.orchestration.backend.schema.Environment;
import eu.dissco.orchestration.backend.schema.FieldMapping;
import eu.dissco.orchestration.backend.schema.MachineAnnotationService;
import eu.dissco.orchestration.backend.schema.MachineAnnotationServiceRequest;
import eu.dissco.orchestration.backend.schema.MachineAnnotationServiceRequestWrapper;
import eu.dissco.orchestration.backend.schema.OdsTargetDigitalObjectFilter;
import eu.dissco.orchestration.backend.schema.OdsTargetDigitalObjectFilter__1;
import eu.dissco.orchestration.backend.schema.SchemaContactPoint;
import eu.dissco.orchestration.backend.schema.SchemaContactPoint__1;
import eu.dissco.orchestration.backend.schema.Secret;
import eu.dissco.orchestration.backend.schema.SecretKeyRef;
import eu.dissco.orchestration.backend.schema.SourceSystem;
import eu.dissco.orchestration.backend.schema.SourceSystem.OdsStatus;
import eu.dissco.orchestration.backend.schema.SourceSystemRequest;
import eu.dissco.orchestration.backend.schema.SourceSystemRequest.OdsTranslatorType;
import eu.dissco.orchestration.backend.schema.TombstoneMetadata;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TestUtils {

  public static final String APP_NAME = "dissco-core-orchestration-backend";
  public static final String APP_HANDLE = "https://hdl.handle.net/TEST/123-123-123";
  public static final String OBJECT_NAME = "Naturalis Tunicate DWCA endpoint";
  public static final String MAS_NAME = "A Machine Annotation Service";
  public static final String OBJECT_CREATOR = "e2befba6-9324-4bb4-9f41-d7dfae4a44b0";
  public static final String PREFIX = "20.5000.1025";
  public static final String SUFFIX = "GW0-POP-XSL";
  public static final String HANDLE_PROXY = "https://hdl.handle.net/";
  public static final String HANDLE = HANDLE_PROXY + PREFIX + "/" + SUFFIX;
  public static final String BARE_HANDLE = PREFIX + "/" + SUFFIX;
  public static final String HANDLE_ALT = HANDLE_PROXY + PREFIX + "/EMK-X81-1QZ";
  public static final String SS_ENDPOINT = "https://api.biodiversitydata.nl/v2/specimen/dwca/getDataSet/tunicata";
  public static final String OBJECT_DESCRIPTION = "Source system for the DWCA of the Tunicate specimen";
  public static final Instant CREATED = Instant.parse("2022-11-01T09:59:24.00Z");
  public static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
  public static final String SANDBOX_URI = "https://sandbox.dissco.tech/orchestrator";
  public static final String SYSTEM_URI = "/source-system";
  public static final String SYSTEM_PATH = SANDBOX_URI + SYSTEM_URI;
  public static final String MAPPING_URI = "/data-mapping";
  public static final String MAPPING_PATH = SANDBOX_URI + MAPPING_URI;
  public static final String MAS_URI = "/mas";
  public static final String MAS_PATH = SANDBOX_URI + MAS_URI;
  public static final Integer TTL = 86400;
  public static final String SOURCE_SYSTEM_TYPE_DOI = "https://hdl.handle.net/21.T11148/417a4f472f60f7974c12";
  public static final String DATA_MAPPING_TYPE_DOI = "https://hdl.handle.net/21.T11148/ce794a6f4df42eb7e77e";
  public static final String MAS_TYPE_DOI = "https://hdl.handle.net/21.T11148/22e71a0015cbcfba8ffa";


  private TestUtils() {
    throw new IllegalStateException("Utility class");
  }

  public static JsonApiWrapper givenSourceSystemSingleJsonApiWrapper() {
    return givenSourceSystemSingleJsonApiWrapper(1);
  }

  public static JsonApiWrapper givenSourceSystemSingleJsonApiWrapper(int version) {
    var sourceSystem = givenSourceSystem(version);
    return new JsonApiWrapper(new JsonApiData(
        sourceSystem.getId(),
        ObjectType.SOURCE_SYSTEM,
        flattenSourceSystem(sourceSystem)
    ), new JsonApiLinks(SYSTEM_PATH));
  }

  public static JsonApiWrapper givenMasSingleJsonApiWrapper() {
    return givenMasSingleJsonApiWrapper(1);
  }

  public static JsonApiWrapper givenMasSingleJsonApiWrapper(int version) {
    var mas = givenMas(version);
    return new JsonApiWrapper(new JsonApiData(
        mas.getId(),
        ObjectType.MAS,
        flattenMas(mas)
    ), new JsonApiLinks(MAS_PATH));
  }

  public static JsonApiWrapper givenDataMappingSingleJsonApiWrapper() {
    return givenDataMappingSingleJsonApiWrapper(1);
  }

  public static JsonApiWrapper givenDataMappingSingleJsonApiWrapper(int version) {
    var dataMapping = givenDataMapping(HANDLE, version);
    return new JsonApiWrapper(new JsonApiData(
        dataMapping.getId(),
        ObjectType.DATA_MAPPING,
        flattenDataMapping(dataMapping)
    ), new JsonApiLinks(MAPPING_PATH));
  }

  public static JsonApiRequestWrapper givenSourceSystemRequestJson() {
    return new JsonApiRequestWrapper(
        new JsonApiRequest(
            ObjectType.SOURCE_SYSTEM,
            MAPPER.valueToTree(givenSourceSystemRequest())
        )
    );
  }

  public static JsonApiRequestWrapper givenDataMappingRequestJson() {
    return new JsonApiRequestWrapper(
        new JsonApiRequest(
            ObjectType.DATA_MAPPING,
            MAPPER.valueToTree(givenDataMappingRequest())
        )
    );
  }

  public static SourceSystem givenSourceSystem() {
    return givenSourceSystem(HANDLE, 1, SourceSystem.OdsTranslatorType.BIOCASE);
  }

  public static SourceSystem givenSourceSystem(int version) {
    return givenSourceSystem(HANDLE, version, SourceSystem.OdsTranslatorType.BIOCASE);
  }

  public static SourceSystem givenSourceSystem(SourceSystem.OdsTranslatorType translatorType) {
    return givenSourceSystem(HANDLE, 1, translatorType);
  }

  public static SourceSystem givenSourceSystem(String id, int version,
      SourceSystem.OdsTranslatorType translatorType) {
    return new SourceSystem()
        .withId(id)
        .withOdsID(id)
        .withType("ods:SourceSystem")
        .withOdsType(SOURCE_SYSTEM_TYPE_DOI)
        .withOdsStatus(OdsStatus.ODS_ACTIVE)
        .withSchemaVersion(version)
        .withSchemaDateCreated(Date.from(CREATED))
        .withSchemaDateModified(Date.from(CREATED))
        .withSchemaCreator(generateCreator())
        .withSchemaName(OBJECT_NAME)
        .withSchemaUrl(URI.create(SS_ENDPOINT))
        .withSchemaDescription(OBJECT_DESCRIPTION)
        .withOdsTranslatorType(translatorType)
        .withOdsDataMappingID(HANDLE_ALT);
  }

  private static Agent generateCreator() {
    return new Agent()
        .withId(OBJECT_CREATOR)
        .withType(Type.SCHEMA_PERSON);
  }

  public static SourceSystemRequest givenSourceSystemRequest() {
    return givenSourceSystemRequest(OdsTranslatorType.BIOCASE);
  }

  public static SourceSystemRequest givenSourceSystemRequest(OdsTranslatorType translatorType) {
    return new SourceSystemRequest()
        .withSchemaName(OBJECT_NAME)
        .withSchemaUrl(URI.create(SS_ENDPOINT))
        .withSchemaDescription(OBJECT_DESCRIPTION)
        .withOdsTranslatorType(translatorType)
        .withOdsDataMappingID(HANDLE_ALT);
  }

  public static JsonApiListWrapper givenSourceSystemResponse(List<SourceSystem> sourceSystems,
      JsonApiLinks linksNode) {
    List<JsonApiData> dataNode = new ArrayList<>();
    sourceSystems.forEach(ss -> dataNode.add(
        new JsonApiData(ss.getId(), ObjectType.SOURCE_SYSTEM, flattenSourceSystem(ss))));
    return new JsonApiListWrapper(dataNode, linksNode);
  }

  public static JsonApiListWrapper givenMasResponse(
      List<MachineAnnotationService> machineAnnotationServices, JsonApiLinks linksNode) {
    var dataNode = machineAnnotationServices.stream().map(
        mas -> new JsonApiData(mas.getId(), ObjectType.MAS,
            flattenMas(mas))).toList();
    return new JsonApiListWrapper(dataNode, linksNode);
  }

  public static JsonNode flattenSourceSystem(SourceSystem sourceSystem) {
    return MAPPER.valueToTree(sourceSystem);
  }

  public static JsonNode flattenMas(MachineAnnotationService mas) {
    return MAPPER.valueToTree(mas);
  }

  public static DataMapping givenDataMapping() {
    return givenDataMapping(HANDLE, 1, OBJECT_NAME);
  }

  public static DataMapping givenDataMapping(String id, int version) {
    return givenDataMapping(id, version, OBJECT_NAME);
  }

  public static DataMapping givenDataMapping(String id, int version, String name) {
    return new DataMapping()
        .withId(id)
        .withOdsID(id)
        .withType("ods:DataMapping")
        .withOdsType(DATA_MAPPING_TYPE_DOI)
        .withSchemaVersion(version)
        .withOdsStatus(DataMapping.OdsStatus.ODS_ACTIVE)
        .withSchemaName(name)
        .withSchemaDescription(OBJECT_DESCRIPTION)
        .withSchemaDateCreated(Date.from(CREATED))
        .withSchemaDateModified(Date.from(CREATED))
        .withOdsDefaultMapping(List.of(
            new DefaultMapping().withAdditionalProperty("ods:organisationID",
                "https://ror.org/05xg72x27")))
        .withOdsFieldMapping(List.of(
            new FieldMapping().withAdditionalProperty("ods:physicalSpecimenID",
                "dwc:catalogNumber")))
        .withSchemaCreator(new Agent().withType(Type.SCHEMA_PERSON).withId(OBJECT_CREATOR))
        .withOdsMappingDataStandard(DataMapping.OdsMappingDataStandard.DWC);
  }

  public static DataMappingRequest givenDataMappingRequest() {
    return new DataMappingRequest()
        .withSchemaName(OBJECT_NAME)
        .withSchemaDescription(OBJECT_DESCRIPTION)
        .withOdsDefaultMapping(List.of(
            new DefaultMapping().withAdditionalProperty("ods:organisationID",
                "https://ror.org/05xg72x27")))
        .withOdsFieldMapping(List.of(
            new FieldMapping().withAdditionalProperty("ods:physicalSpecimenID",
                "dwc:catalogNumber")))
        .withOdsMappingDataStandard(OdsMappingDataStandard.DWC);
  }

  public static JsonApiListWrapper givenMappingResponse(List<DataMapping> dataMappings,
      JsonApiLinks linksNode) {
    List<JsonApiData> dataNode = new ArrayList<>();
    dataMappings.forEach(
        m -> dataNode.add(
            new JsonApiData(m.getId(), ObjectType.DATA_MAPPING, flattenDataMapping(m))));
    return new JsonApiListWrapper(dataNode, linksNode);
  }

  public static JsonNode flattenDataMapping(DataMapping dataMapping) {
    return MAPPER.valueToTree(dataMapping);
  }

  public static JsonApiRequestWrapper givenMasRequestJson() {
    return new JsonApiRequestWrapper(new JsonApiRequest(
        ObjectType.MAS,
        MAPPER.valueToTree(givenMasRequestWrapper())
    ));
  }

  public static MachineAnnotationServiceRequest givenMasRequest() {
    return new MachineAnnotationServiceRequest()
        .withSchemaName(MAS_NAME)
        .withOdsContainerImage("public.ecr.aws/dissco/fancy-mas")
        .withOdsContainerTag("sha-54289")
        .withOdsTargetDigitalObjectFilter(
            new OdsTargetDigitalObjectFilter().withAdditionalProperty("ods:topicDiscipline",
                "botany"))
        .withSchemaDescription("A fancy mas making all dreams come true")
        .withSchemaCreativeWorkStatus("Definitely production ready")
        .withSchemaCodeRepository("https://github.com/DiSSCo/fancy-mas")
        .withSchemaProgrammingLanguage("Python")
        .withOdsServiceAvailability("99.99%")
        .withSchemaMaintainer(new Agent().withType(Type.SCHEMA_PERSON).withId(OBJECT_CREATOR))
        .withSchemaLicense("https://www.apache.org/licenses/LICENSE-2.0")
        .withOdsDependency(List.of())
        .withSchemaContactPoint(new SchemaContactPoint().withSchemaEmail("dontmail@dissco.eu"))
        .withOdsTopicName("fancy-topic-name")
        .withOdsMaxReplicas(5)
        .withOdsBatchingPermitted(false)
        .withOdsTimeToLive(TTL);

  }

  public static MachineAnnotationServiceRequestWrapper givenMasRequestWrapper() {
    return new MachineAnnotationServiceRequestWrapper()
        .withMas(givenMasRequest())
        .withEnvironment(givenMasEnvironment())
        .withSecrets(givenMasSecrets());
  }

  public static List<Environment> givenMasEnvironment() {
    return List.of(new Environment()
        .withName("server.port")
        .withValue(8080));
  }

  public static List<Secret> givenMasSecrets(){
    return List.of(new Secret()
        .withName("spring.datasource.password")
        .withSecretKeyRef(new SecretKeyRef()
            .withName("aws-secrets")
            .withKey("db-password")));
  }

  public static MachineAnnotationService givenMas() {
    return givenMas(1, MAS_NAME, TTL);
  }

  public static MachineAnnotationService givenMas(int version) {
    return givenMas(version, MAS_NAME, TTL);
  }

  public static MachineAnnotationService givenMas(int version, String name, Integer ttl) {
    return givenMas(HANDLE, version, name, ttl);
  }

  public static MachineAnnotationService givenMas(String id, int version, String name,
      Integer ttl) {
    return new MachineAnnotationService()
        .withId(id)
        .withOdsID(id)
        .withType("ods:MachineAnnotationService")
        .withOdsType("https://hdl.handle.net/21.T11148/22e71a0015cbcfba8ffa")
        .withSchemaVersion(version)
        .withOdsStatus(MachineAnnotationService.OdsStatus.ODS_ACTIVE)
        .withSchemaDateCreated(Date.from(CREATED))
        .withSchemaDateModified(Date.from(CREATED))
        .withSchemaCreator(new Agent().withType(Type.SCHEMA_PERSON).withId(OBJECT_CREATOR))
        .withSchemaName(name)
        .withOdsContainerImage("public.ecr.aws/dissco/fancy-mas")
        .withOdsContainerTag("sha-54289")
        .withOdsTargetDigitalObjectFilter(
            new OdsTargetDigitalObjectFilter__1().withAdditionalProperty("ods:topicDiscipline",
                "botany"))
        .withSchemaDescription("A fancy mas making all dreams come true")
        .withSchemaCreativeWorkStatus("Definitely production ready")
        .withSchemaCodeRepository("https://github.com/DiSSCo/fancy-mas")
        .withSchemaProgrammingLanguage("Python")
        .withOdsServiceAvailability("99.99%")
        .withSchemaMaintainer(new Agent().withType(Type.SCHEMA_PERSON).withId(OBJECT_CREATOR))
        .withSchemaLicense("https://www.apache.org/licenses/LICENSE-2.0")
        .withOdsDependency(List.of())
        .withSchemaContactPoint(new SchemaContactPoint__1().withSchemaEmail("dontmail@dissco.eu"))
        .withOdsTopicName("fancy-topic-name")
        .withOdsMaxReplicas(5)
        .withOdsBatchingPermitted(false)
        .withOdsTimeToLive(ttl);
  }

  public static JsonNode givenMasHandleRequest() throws Exception {
    return MAPPER.readTree("""
        {
          "data": {
            "type": "https://hdl.handle.net/21.T11148/22e71a0015cbcfba8ffa",
            "attributes": {
              "issuedForAgent": "https://ror.org/0566bfb96",
              "masName":"A Machine Annotation Service"
            }
          }
        }""");
  }

  public static JsonNode givenSourceSystemHandleRequest() throws Exception {
    return MAPPER.readTree("""
        {
          "data": {
            "type": "https://hdl.handle.net/21.T11148/417a4f472f60f7974c12",
            "attributes": {
              "issuedForAgent": "https://ror.org/0566bfb96",
              "sourceSystemName":"Naturalis Tunicate DWCA endpoint"
            }
          }
        }""");
  }

  public static JsonNode givenMappingHandleRequest() throws Exception {
    return MAPPER.readTree("""
        {
          "data": {
            "type": "https://hdl.handle.net/21.T11148/ce794a6f4df42eb7e77e",
            "attributes": {
              "issuedForAgent": "https://ror.org/0566bfb96",
              "sourceDataStandard": "dwc"
            }
          }
        }""");
  }

  public static JsonNode givenRollbackCreationRequest() throws Exception {
    return MAPPER.readTree("""
        {
          "data":
            [{"id":"20.5000.1025/GW0-POP-XSL"}]
        }""");
  }

  public static TombstoneMetadata givenTombstoneMetadata() {
    return new TombstoneMetadata()
        .withType("ods:TomstoneMetadata")
        .withOdsTombstonedByAgent(new Agent().withType(Type.SCHEMA_PERSON).withId(OBJECT_CREATOR))
        .withOdsTombstoneDate(Date.from(CREATED))
        .withOdsTombstoneText("Tombstoned");
  }
}
