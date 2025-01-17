package eu.dissco.orchestration.backend.service;

import static eu.dissco.orchestration.backend.domain.AgentRoleType.CREATOR;
import static eu.dissco.orchestration.backend.domain.AgentRoleType.PROCESSING_SERVICE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.APP_HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.APP_NAME;
import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.OBJECT_CREATOR;
import static eu.dissco.orchestration.backend.testutils.TestUtils.TTL;
import static eu.dissco.orchestration.backend.testutils.TestUtils.UPDATED;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenAgent;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenDataMapping;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMas;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystem;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenTombstoneDataMapping;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenTombstoneMas;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenTombstoneMetadata;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenTombstoneSourceSystem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.core.type.TypeReference;
import eu.dissco.orchestration.backend.domain.ObjectType;
import eu.dissco.orchestration.backend.properties.ApplicationProperties;
import eu.dissco.orchestration.backend.schema.Agent;
import eu.dissco.orchestration.backend.schema.Agent.Type;
import eu.dissco.orchestration.backend.schema.Identifier.DctermsType;
import eu.dissco.orchestration.backend.schema.MachineAnnotationService.OdsStatus;
import eu.dissco.orchestration.backend.schema.OdsChangeValue;
import eu.dissco.orchestration.backend.utils.AgentUtils;
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
        AgentUtils.createAgent(null, OBJECT_CREATOR, CREATOR,
            "orcid", Type.PROV_PERSON),
        AgentUtils.createAgent(APP_NAME, APP_HANDLE, PROCESSING_SERVICE,
            DctermsType.DOI.value(), Type.PROV_SOFTWARE_AGENT)
    );
  }

  private static List<OdsChangeValue> givenChangeValueTombstone(ObjectType objectType) {
    return List.of(
        givenOdsChangeValue("add", "/ods:hasTombstoneMetadata", givenTombstoneMetadata(objectType)),
        givenOdsChangeValue("replace", "/ods:status", OdsStatus.TOMBSTONE),
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

  @BeforeEach
  void setup() {
    this.service = new ProvenanceService(MAPPER, properties);
  }

  @Test
  void testGenerateCreateEvent() {
    // Given
    given(properties.getName()).willReturn(APP_NAME);
    given(properties.getPid()).willReturn(APP_HANDLE);
    var machineAnnotationService = givenMas();

    // When
    var event = service.generateCreateEvent(MAPPER.valueToTree(machineAnnotationService),
        givenAgent());

    // Then
    assertThat(event.getDctermsIdentifier()).isEqualTo(HANDLE + "/" + "1");
    assertThat(event.getProvActivity().getOdsChangeValue()).isNull();
    assertThat(event.getProvActivity().getRdfsComment()).isEqualTo("Object newly created");
    assertThat(event.getProvEntity().getProvValue()).isNotNull();
    assertThat(event.getOdsHasAgents()).isEqualTo(givenExpectedAgents());
  }

  @Test
  void testGenerateUpdateEvent() {
    // Given
    given(properties.getName()).willReturn(APP_NAME);
    given(properties.getPid()).willReturn(APP_HANDLE);
    var machineAnnotationService = givenMas(2);
    var prevMachineAnnotationService = givenMas(1, "The old name for the mas", TTL);

    // When
    var event = service.generateUpdateEvent(MAPPER.valueToTree(machineAnnotationService),
        MAPPER.valueToTree(prevMachineAnnotationService), givenAgent());

    // Then
    assertThat(event.getDctermsIdentifier()).isEqualTo(HANDLE + "/" + "2");
    assertThat(event.getProvActivity().getRdfsComment()).isEqualTo("Object updated");
    assertThat(event.getProvActivity().getOdsChangeValue()).isEqualTo(givenChangeValueUpdate());
    assertThat(event.getProvEntity().getProvValue()).isNotNull();
    assertThat(event.getOdsHasAgents()).isEqualTo(givenExpectedAgents());
  }

  @Test
  void testGenerateTombstoneEventMas() {
    // Given
    given(properties.getName()).willReturn(APP_NAME);
    given(properties.getPid()).willReturn(APP_HANDLE);
    var originalMas = MAPPER.valueToTree(givenMas());
    var tombstoneMas = MAPPER.valueToTree(givenTombstoneMas());

    // When
    var event = service.generateTombstoneEvent(tombstoneMas, originalMas, givenAgent());

    // Then
    assertThat(event.getDctermsIdentifier()).isEqualTo(HANDLE + "/" + "2");
    assertThat(event.getProvActivity().getOdsChangeValue()).isEqualTo(
        givenChangeValueTombstone(ObjectType.MAS));
    assertThat(event.getProvEntity().getProvValue()).isNotNull();
    assertThat(event.getProvActivity().getRdfsComment()).isEqualTo("Object tombstoned");
    assertThat(event.getOdsHasAgents()).isEqualTo(givenExpectedAgents());
  }

  @Test
  void testGenerateTombstoneEventSourceSystem() {
    // Given
    given(properties.getName()).willReturn(APP_NAME);
    given(properties.getPid()).willReturn(APP_HANDLE);
    var originalSourceSystem = MAPPER.valueToTree(givenSourceSystem());
    var tombstoneSourceSystem = MAPPER.valueToTree(givenTombstoneSourceSystem());

    // When
    var event = service.generateTombstoneEvent(tombstoneSourceSystem, originalSourceSystem,
        givenAgent());

    // Then
    assertThat(event.getDctermsIdentifier()).isEqualTo(HANDLE + "/" + "2");
    assertThat(event.getProvActivity().getOdsChangeValue()).hasSameElementsAs(
        givenChangeValueTombstone(ObjectType.SOURCE_SYSTEM));
    assertThat(event.getProvEntity().getProvValue()).isNotNull();
    assertThat(event.getOdsHasAgents()).isEqualTo(givenExpectedAgents());
  }

  @Test
  void testGenerateTombstoneEventDataMapping() {
    // Given
    given(properties.getName()).willReturn(APP_NAME);
    given(properties.getPid()).willReturn(APP_HANDLE);
    var originalDataMapping = MAPPER.valueToTree(givenDataMapping());
    var tombstoneDataMapping = MAPPER.valueToTree(givenTombstoneDataMapping());

    // When
    var event = service.generateTombstoneEvent(tombstoneDataMapping, originalDataMapping,
        givenAgent());

    // Then
    assertThat(event.getDctermsIdentifier()).isEqualTo(HANDLE + "/" + "2");
    assertThat(event.getProvActivity().getOdsChangeValue()).hasSameElementsAs(
        givenChangeValueTombstone(ObjectType.DATA_MAPPING));
    assertThat(event.getProvEntity().getProvValue()).isNotNull();
    assertThat(event.getOdsHasAgents()).isEqualTo(givenExpectedAgents());
  }
}
