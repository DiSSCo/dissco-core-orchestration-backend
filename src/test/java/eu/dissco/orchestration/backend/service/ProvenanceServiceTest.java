package eu.dissco.orchestration.backend.service;

import static eu.dissco.orchestration.backend.testutils.TestUtils.APP_HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.APP_NAME;
import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.OBJECT_CREATOR;
import static eu.dissco.orchestration.backend.testutils.TestUtils.TTL;
import static eu.dissco.orchestration.backend.testutils.TestUtils.UPDATED;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenDataMapping;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMas;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystem;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenTombstoneDataMapping;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenTombstoneMas;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenTombstoneMetadata;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenTombstoneSourceSystem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.properties.ApplicationProperties;
import eu.dissco.orchestration.backend.schema.Agent;
import eu.dissco.orchestration.backend.schema.Agent.Type;
import eu.dissco.orchestration.backend.schema.MachineAnnotationService.OdsStatus;
import eu.dissco.orchestration.backend.schema.OdsChangeValue;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProvenanceServiceTest {

  @Mock
  private ApplicationProperties properties;
  private ProvenanceService service;

  private static List<Agent> givenExpectedAgents() {
    return List.of(
        new Agent()
            .withId(OBJECT_CREATOR)
            .withType(Type.SCHEMA_PERSON),
        new Agent()
            .withId(APP_HANDLE)
            .withType(Type.AS_APPLICATION)
            .withSchemaName(APP_NAME)
    );
  }

  @BeforeEach
  void setup() {
    this.service = new ProvenanceService(MAPPER, properties);
  }

  @Test
  void testGenerateCreateEvent() throws JsonProcessingException {
    // Given
    given(properties.getName()).willReturn(APP_NAME);
    given(properties.getPid()).willReturn(APP_HANDLE);
    var machineAnnotationService = givenMas();

    // When
    var event = service.generateCreateEvent(MAPPER.valueToTree(machineAnnotationService));

    // Then
    assertThat(event.getOdsID()).isEqualTo(HANDLE + "/" + "1");
    assertThat(event.getProvActivity().getOdsChangeValue()).isNull();
    assertThat(event.getProvActivity().getRdfsComment()).isEqualTo("Object newly created");
    assertThat(event.getProvEntity().getProvValue()).isNotNull();
    assertThat(event.getOdsHasProvAgent()).isEqualTo(givenExpectedAgents());
  }

  @Test
  void testGenerateUpdateEvent() throws JsonProcessingException {
    // Given
    given(properties.getName()).willReturn(APP_NAME);
    given(properties.getPid()).willReturn(APP_HANDLE);
    var machineAnnotationService = givenMas(2);
    var prevMachineAnnotationService = givenMas(1, "The old name for the mas", TTL);

    // When
    var event = service.generateUpdateEvent(MAPPER.valueToTree(machineAnnotationService),
        MAPPER.valueToTree(prevMachineAnnotationService));

    // Then
    assertThat(event.getOdsID()).isEqualTo(HANDLE + "/" + "2");
    assertThat(event.getProvActivity().getRdfsComment()).isEqualTo("Object updated");
    assertThat(event.getProvActivity().getOdsChangeValue()).isEqualTo(givenChangeValueUpdate());
    assertThat(event.getProvEntity().getProvValue()).isNotNull();
    assertThat(event.getOdsHasProvAgent()).isEqualTo(givenExpectedAgents());
  }

  @Test
  void testGenerateTombstoneEventMas() throws Exception {
    // Given
    given(properties.getName()).willReturn(APP_NAME);
    given(properties.getPid()).willReturn(APP_HANDLE);
    var originalMas = MAPPER.valueToTree(givenMas());
    var tombstoneMas = MAPPER.valueToTree(givenTombstoneMas());

    // When
    var event = service.generateTombstoneEvent(tombstoneMas, originalMas);

    // Then
    assertThat(event.getOdsID()).isEqualTo(HANDLE + "/" + "2");
    assertThat(event.getProvActivity().getOdsChangeValue()).isEqualTo(
        givenChangeValueTombstone(ObjectType.MAS));
    assertThat(event.getProvEntity().getProvValue()).isNotNull();
    assertThat(event.getProvActivity().getRdfsComment()).isEqualTo("Object tombstoned");
    assertThat(event.getOdsHasProvAgent()).isEqualTo(givenExpectedAgents());
  }

  @Test
  void testGenerateTombstoneEventSourceSystem() throws Exception {
    // Given
    given(properties.getName()).willReturn(APP_NAME);
    given(properties.getPid()).willReturn(APP_HANDLE);
    var originalSourceSystem = MAPPER.valueToTree(givenSourceSystem());
    var tombstoneSourceSystem = MAPPER.valueToTree(givenTombstoneSourceSystem());

    // When
    var event = service.generateTombstoneEvent(tombstoneSourceSystem, originalSourceSystem);

    // Then
    assertThat(event.getOdsID()).isEqualTo(HANDLE + "/" + "2");
    assertThat(event.getProvActivity().getOdsChangeValue()).hasSameElementsAs(
        givenChangeValueTombstone(ObjectType.SOURCE_SYSTEM));
    assertThat(event.getProvEntity().getProvValue()).isNotNull();
    assertThat(event.getOdsHasProvAgent()).isEqualTo(givenExpectedAgents());
  }

  @Test
  void testGenerateTombstoneEventDataMapping() throws Exception {
    // Given
    given(properties.getName()).willReturn(APP_NAME);
    given(properties.getPid()).willReturn(APP_HANDLE);
    var originalDataMapping = MAPPER.valueToTree(givenDataMapping());
    var tombstoneDataMapping = MAPPER.valueToTree(givenTombstoneDataMapping());

    // When
    var event = service.generateTombstoneEvent(tombstoneDataMapping, originalDataMapping);

    // Then
    assertThat(event.getOdsID()).isEqualTo(HANDLE + "/" + "2");
    assertThat(event.getProvActivity().getOdsChangeValue()).hasSameElementsAs(
        givenChangeValueTombstone(ObjectType.DATA_MAPPING));
    assertThat(event.getProvEntity().getProvValue()).isNotNull();
    assertThat(event.getOdsHasProvAgent()).isEqualTo(givenExpectedAgents());
  }


  private static List<OdsChangeValue> givenChangeValueTombstone(ObjectType objectType) {
    return List.of(
        givenOdsChangeValue("add", "/ods:TombstoneMetadata", givenTombstoneMetadata(objectType)),
        givenOdsChangeValue("replace", "/ods:status", OdsStatus.ODS_TOMBSTONE),
        givenOdsChangeValue("replace", "/schema:version", 2),
        givenOdsChangeValue("replace", "/schema:dateModified", Date.from(UPDATED))
    );
  }

  private static List<OdsChangeValue> givenChangeValueUpdate() {
    return List.of(
        givenOdsChangeValue("replace", "/schema:version", 2),
        givenOdsChangeValue("replace", "/schema:name", "A Machine Annotation Service")
    );
  }

  private static OdsChangeValue givenOdsChangeValue(String op, String path, Object value) {
    return new OdsChangeValue()
        .withAdditionalProperty("op", op)
        .withAdditionalProperty("path", path)
        .withAdditionalProperty("value", MAPPER.convertValue(value, new TypeReference<>() {
        }));
  }
}
