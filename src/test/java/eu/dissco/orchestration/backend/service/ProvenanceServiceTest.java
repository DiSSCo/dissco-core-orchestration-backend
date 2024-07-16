package eu.dissco.orchestration.backend.service;

import static eu.dissco.orchestration.backend.testutils.TestUtils.APP_HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.APP_NAME;
import static eu.dissco.orchestration.backend.testutils.TestUtils.HANDLE;
import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.OBJECT_CREATOR;
import static eu.dissco.orchestration.backend.testutils.TestUtils.TTL;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.dissco.orchestration.backend.properties.ApplicationProperties;
import eu.dissco.orchestration.backend.schema.Agent;
import eu.dissco.orchestration.backend.schema.Agent.Type;
import eu.dissco.orchestration.backend.schema.OdsChangeValue;
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
    assertThat(event.getProvActivity().getOdsChangeValue()).isEqualTo(givenChangeValue());
    assertThat(event.getProvEntity().getProvValue()).isNotNull();
    assertThat(event.getOdsHasProvAgent()).isEqualTo(givenExpectedAgents());
  }

  List<OdsChangeValue> givenChangeValue() {
    return List.of(new OdsChangeValue()
            .withAdditionalProperty("op", "replace")
            .withAdditionalProperty("path", "/schema:version")
            .withAdditionalProperty("value", 2),
        new OdsChangeValue()
            .withAdditionalProperty("op", "replace")
            .withAdditionalProperty("path", "/schema:name")
            .withAdditionalProperty("value", "A Machine Annotation Service")
    );
  }
}
