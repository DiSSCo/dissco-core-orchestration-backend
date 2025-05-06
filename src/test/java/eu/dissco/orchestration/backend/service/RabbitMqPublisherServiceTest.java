package eu.dissco.orchestration.backend.service;

import static eu.dissco.orchestration.backend.testutils.TestUtils.MAPPER;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenAgent;
import static eu.dissco.orchestration.backend.testutils.TestUtils.givenMas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.dissco.orchestration.backend.schema.CreateUpdateTombstoneEvent;
import eu.dissco.orchestration.backend.testutils.TestUtils;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;
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
    declareRabbitResources("create-update-tombstone-exchange", "create-update-tombstone-queue",
        "create-update-tombstone");

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
        "type=direct", "durable=true");
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
        provenanceService);
    ReflectionTestUtils.setField(rabbitMqPublisherService, "exchange",
        "create-update-tombstone-exchange");
    ReflectionTestUtils.setField(rabbitMqPublisherService, "routingKey", "create-update-tombstone");
  }

  @Test
  void testPublishCreateEvent() throws JsonProcessingException {
    // Given
    given(provenanceService.generateCreateEvent(MAPPER.valueToTree(givenMas()), givenAgent()))
        .willReturn(new CreateUpdateTombstoneEvent().withId(ID));

    // When
    rabbitMqPublisherService.publishCreateEvent(MAPPER.valueToTree(givenMas()), givenAgent());

    // Then
    var result = rabbitTemplate.receive("create-update-tombstone-queue");
    assertThat(MAPPER.readValue(new String(result.getBody()),
        CreateUpdateTombstoneEvent.class).getId()).isEqualTo(ID);
  }

  @Test
  void testPublishUpdateEvent() throws JsonProcessingException {
    // Given
    var annotation = MAPPER.valueToTree(givenMas());
    var unequalAnnotation = MAPPER.createObjectNode();
    given(provenanceService.generateUpdateEvent(annotation, unequalAnnotation, givenAgent()))
        .willReturn(new CreateUpdateTombstoneEvent().withId(ID));

    // When
    rabbitMqPublisherService.publishUpdateEvent(annotation, unequalAnnotation, givenAgent());

    // Then
    var result = rabbitTemplate.receive("create-update-tombstone-queue");
    assertThat(MAPPER.readValue(new String(result.getBody()),
        CreateUpdateTombstoneEvent.class).getId()).isEqualTo(ID);
  }

  @Test
  void testPublishTombstoneEvent() throws Exception {
    // Given
    var tombstoneMas = MAPPER.valueToTree(TestUtils.givenTombstoneMas());
    var mas = MAPPER.valueToTree(givenMas());
    given(provenanceService.generateTombstoneEvent(tombstoneMas, mas, givenAgent())).willReturn(new
        CreateUpdateTombstoneEvent().withId(ID));

    // When
    rabbitMqPublisherService.publishTombstoneEvent(tombstoneMas, mas, givenAgent());

    // Then
    var result = rabbitTemplate.receive("create-update-tombstone-queue");
    assertThat(MAPPER.readValue(new String(result.getBody()),
        CreateUpdateTombstoneEvent.class).getId()).isEqualTo(ID);
  }
}
