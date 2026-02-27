package eu.dissco.orchestration.backend.client;

import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;
import tools.jackson.databind.JsonNode;

public interface HandleClient {

  @PostExchange("batch")
  JsonNode postHandle(@RequestBody List<JsonNode> handleRequest);

  @PutExchange("{pid}")
  void tombstoneHandle(@PathVariable String pid, @RequestBody JsonNode handleRequest);

  @DeleteExchange("rollback/create")
  void rollbackHandle(@RequestBody JsonNode handleRequest);
}
