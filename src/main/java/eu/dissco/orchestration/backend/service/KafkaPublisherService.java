package eu.dissco.orchestration.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaPublisherService {

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper mapper;
  private final ProvenanceService provenanceService;

  public void publishCreateEvent(JsonNode object)
      throws JsonProcessingException {
    var event = provenanceService.generateCreateEvent(object);
    log.info("Publishing new create message to queue: {}", event);
    kafkaTemplate.send("createUpdateDeleteTopic", mapper.writeValueAsString(event));
  }

  public void publishUpdateEvent(JsonNode object, JsonNode currentObject)
      throws JsonProcessingException {
    var event = provenanceService.generateUpdateEvent(object, currentObject);
    log.info("Publishing new update message to queue: {}", event);
    kafkaTemplate.send("createUpdateDeleteTopic", mapper.writeValueAsString(event));
  }
}
