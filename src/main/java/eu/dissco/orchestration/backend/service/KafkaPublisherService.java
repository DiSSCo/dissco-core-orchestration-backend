package eu.dissco.orchestration.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.orchestration.backend.schema.Agent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaPublisherService {

  private static final String TOPIC = "createUpdateDeleteTopic";
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper mapper;
  private final ProvenanceService provenanceService;

  public void publishCreateEvent(JsonNode object, Agent agent)
      throws JsonProcessingException {
    var event = provenanceService.generateCreateEvent(object, agent);
    log.info("Publishing new create message to queue: {}", event);
    kafkaTemplate.send(TOPIC, mapper.writeValueAsString(event));
  }

  public void publishUpdateEvent(JsonNode object, JsonNode currentObject, Agent agent)
      throws JsonProcessingException {
    var event = provenanceService.generateUpdateEvent(object, currentObject, agent);
    log.info("Publishing new update message to queue: {}", event);
    kafkaTemplate.send(TOPIC, mapper.writeValueAsString(event));
  }

  public void publishTombstoneEvent(JsonNode tombstoneObject, JsonNode currentObject, Agent agent)
      throws JsonProcessingException {
    var event = provenanceService.generateTombstoneEvent(tombstoneObject, currentObject, agent);
    log.info("Publishing new tombstone message to queue: {}", event);
    kafkaTemplate.send(TOPIC, mapper.writeValueAsString(event));
  }

}
