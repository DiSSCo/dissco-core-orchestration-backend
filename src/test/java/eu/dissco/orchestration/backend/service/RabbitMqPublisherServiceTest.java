package eu.dissco.orchestration.backend.service;

import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenAgent;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenDataMapping;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMas;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenSourceSystem;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenTombstoneMas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import eu.dissco.orchestration.backend.exception.ProcessingFailedException;
import eu.dissco.orchestration.backend.properties.RabbitMqProperties;
import eu.dissco.orchestration.backend.schema.CreateUpdateTombstoneEvent;
import eu.dissco.orchestration.backend.schema.SourceSystem.OdsTranslatorType;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ExtendWith(MockitoExtension.class)
class RabbitMqPublisherServiceTest {

  private static final String ID = "123-123-123";

  private static RabbitMQContainer container;
  private static RabbitTemplate rabbitTemplate;
  private RabbitMqPublisherService rabbitMqPublisherService;
  @Mock
  private ProvenanceService provenanceService;

  @BeforeAll
  static void setupContainer() throws IOException, InterruptedException {
    container = new RabbitMQContainer("rabbitmq:4.0.8-management-alpine");
    container.start();

    // Declare create update tombstone exchange, queue and binding
    declareRabbitResources("provenance-exchange", "provenance-queue",
        "provenance.#");

    CachingConnectionFactory factory = new CachingConnectionFactory(container.getHost());
    factory.setPort(container.getAmqpPort());
    factory.setUsername(container.getAdminUsername());
    factory.setPassword(container.getAdminPassword());
    rabbitTemplate = new RabbitTemplate(factory);
    rabbitTemplate.setReceiveTimeout(100L);
  }


  private static void declareRabbitResources(String exchangeName, String queueName,
      String routingKey)
      throws IOException, InterruptedException {
    container.execInContainer("rabbitmqadmin", "declare", "exchange", "name=" + exchangeName,
        "type=topic", "durable=true");
    container.execInContainer("rabbitmqadmin", "declare", "queue", "name=" + queueName,
        "queue_type=quorum", "durable=true");
    container.execInContainer("rabbitmqadmin", "declare", "binding", "source=" + exchangeName,
        "destination_type=queue", "destination=" + queueName, "routing_key=" + routingKey);
  }

  @AfterAll
  static void shutdownContainer() {
    container.stop();
  }

  @BeforeEach
  void setup() {
    rabbitMqPublisherService = new RabbitMqPublisherService(rabbitTemplate, MAPPER,
        provenanceService, new RabbitMqProperties());
  }

  @ParameterizedTest
  @MethodSource("createEventProvider")
  void testPublishCreateEvent(Object createObject) throws Exception {
    // Given
    given(provenanceService.generateCreateEvent(MAPPER.valueToTree(createObject), givenAgent()))
        .willReturn(new CreateUpdateTombstoneEvent().withId(ID));

    // When
    rabbitMqPublisherService.publishCreateEvent(createObject, givenAgent());

    // Then
    var result = rabbitTemplate.receive("provenance-queue");
    assertThat(MAPPER.readValue(new String(result.getBody()),
        CreateUpdateTombstoneEvent.class).getId()).isEqualTo(ID);
  }

  @Test
  void testPublishCreateEventInvalidType() {
    // Given
    given(provenanceService.generateCreateEvent(MAPPER.createObjectNode(), givenAgent()))
        .willReturn(new CreateUpdateTombstoneEvent().withId(ID));

    // When / Then
    assertThrows(ProcessingFailedException.class,
        () -> rabbitMqPublisherService.publishCreateEvent(MAPPER.createObjectNode(), givenAgent()));
  }

  @Test
  void testPublishUpdateEvent() throws Exception {
    // Given
    var unequalSourceSystem = givenSourceSystem().withOdsTranslatorType(OdsTranslatorType.BIOCASE);
    given(provenanceService.generateUpdateEvent(MAPPER.valueToTree(givenSourceSystem()),
        MAPPER.valueToTree(unequalSourceSystem), givenAgent()))
        .willReturn(new CreateUpdateTombstoneEvent().withId(ID));

    // When
    rabbitMqPublisherService.publishUpdateEvent(givenSourceSystem(), unequalSourceSystem,
        givenAgent());

    // Then
    var result = rabbitTemplate.receive("provenance-queue");
    assertThat(MAPPER.readValue(new String(result.getBody()),
        CreateUpdateTombstoneEvent.class).getId()).isEqualTo(ID);
  }

  @Test
  void testPublishTombstoneEvent() throws Exception {
    // Given
    given(provenanceService.generateTombstoneEvent(MAPPER.valueToTree(givenTombstoneMas()),
        MAPPER.valueToTree(givenMas()), givenAgent())).willReturn(new
        CreateUpdateTombstoneEvent().withId(ID));

    // When
    rabbitMqPublisherService.publishTombstoneEvent(givenTombstoneMas(), givenMas(), givenAgent());

    // Then
    var result = rabbitTemplate.receive("provenance-queue");
    assertThat(MAPPER.readValue(new String(result.getBody()),
        CreateUpdateTombstoneEvent.class).getId()).isEqualTo(ID);
  }

  private static Stream<Object> createEventProvider() {
    return Stream.of(
        givenDataMapping(),
        givenSourceSystem(),
        givenMas()
    );
  }
}
