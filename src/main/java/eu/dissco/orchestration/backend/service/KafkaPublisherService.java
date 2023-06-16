package eu.dissco.orchestration.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.dissco.orchestration.backend.domain.CreateUpdateDeleteEvent;
import java.time.Instant;
import java.util.UUID;
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

  public void publishCreateEvent(String id, JsonNode object, String subjectType)
      throws JsonProcessingException {
    var event = new CreateUpdateDeleteEvent(
        UUID.randomUUID(),
        "create",
        "orchestration-backend",
        id,
        subjectType,
        Instant.now(),
        object,
        null,
        "Object is newly created");
    log.info("Publishing new create message to queue: {}", event);
    kafkaTemplate.send("createUpdateDeleteTopic", mapper.writeValueAsString(event));
  }

  public void publishUpdateEvent(String id, JsonNode newRecord, JsonNode jsonPatch,
      String subjectType) throws JsonProcessingException {
    var event = new CreateUpdateDeleteEvent(
        UUID.randomUUID(),
        "update",
        "orchestration-backend",
        id,
        subjectType,
        Instant.now(),
        newRecord,
        jsonPatch,
        "Object has been updated");
    log.info("Publishing new update message to queue: {}", event);
    kafkaTemplate.send("createUpdateDeleteTopic", mapper.writeValueAsString(event));
  }
}
