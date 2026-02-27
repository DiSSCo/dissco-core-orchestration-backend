package eu.dissco.orchestration.backend.client;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;
import tools.jackson.databind.JsonNode;

public interface HandleClient {

  @PostExchange("batch")
  Map<String, Object> postHandle(@RequestBody List<JsonNode> handleRequest);

  @PutExchange("{pid}")
  void tombstoneHandle(@PathVariable String pid, @RequestBody Map<String, Object> handleRequest);

  @DeleteExchange("rollback/create")
  void rollbackHandle(@RequestBody Map<String, Object> handleRequest);
}
