package eu.dissco.orchestration.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.orchestration.backend.exception.ProcessingFailedException;
import eu.dissco.orchestration.backend.schema.Agent;
import eu.dissco.orchestration.backend.schema.DataMapping;
import eu.dissco.orchestration.backend.schema.MachineAnnotationService;
import eu.dissco.orchestration.backend.schema.SourceSystem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitMqPublisherService {

  private final RabbitTemplate rabbitTemplate;
  private final ObjectMapper mapper;
  private final ProvenanceService provenanceService;
  @Value("${rabbitmq.create-update-tombstone.exchange-name:create-update-tombstone-exchange}")
  private String exchange;
  @Value("${rabbitmq.create-update-tombstone.routing-key:create-update-tombstone}")
  private String routingKey;

  public void publishCreateEvent(Object object, Agent agent)
      throws JsonProcessingException, ProcessingFailedException {
    var event = provenanceService.generateCreateEvent(mapper.valueToTree(object), agent);
    log.info("Publishing new create message to queue: {}", event);
    rabbitTemplate.convertAndSend(exchange, assembleRoutingKey(object), mapper.writeValueAsString(event));
  }

  public void publishUpdateEvent(Object object, Object currentObject, Agent agent)
      throws JsonProcessingException, ProcessingFailedException {
    var event = provenanceService.generateUpdateEvent(mapper.valueToTree(object), mapper.valueToTree(currentObject), agent);
    log.info("Publishing new update message to queue: {}", event);
    rabbitTemplate.convertAndSend(exchange, assembleRoutingKey(object), mapper.writeValueAsString(event));
  }

  public void publishTombstoneEvent(Object tombstoneObject, Object currentObject, Agent agent)
      throws JsonProcessingException, ProcessingFailedException {
    var event = provenanceService.generateTombstoneEvent(mapper.valueToTree(tombstoneObject), mapper.valueToTree(currentObject), agent);
    log.info("Publishing new tombstone message to queue: {}", event);
    rabbitTemplate.convertAndSend(exchange, assembleRoutingKey(tombstoneObject), mapper.writeValueAsString(event));
  }

  private String assembleRoutingKey(Object object) throws ProcessingFailedException {
    var stringBuilder = new StringBuilder(routingKey).append('.');
    switch (object) {
      case DataMapping dm -> stringBuilder.append("data-mapping");
      case SourceSystem ss -> stringBuilder.append("source-system");
      case MachineAnnotationService mas -> stringBuilder.append("machine-annotation-service");
      default -> throw new ProcessingFailedException("Unsupported object type for routing key assembly");
    }
    return stringBuilder.toString();
  }

}
