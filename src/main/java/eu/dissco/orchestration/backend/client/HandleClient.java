package eu.dissco.orchestration.backend.client;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

public interface HandleClient {

  @PostExchange("batch")
  Map<String, Object> postHandle(@RequestBody List<Map<String, Object>> handleRequest);

  @PutExchange("{pid}")
  void tombstoneHandle(@PathVariable String pid, @RequestBody Map<String, Object> handleRequest);

  @DeleteExchange("rollback/create")
  void rollbackHandle(@RequestBody Map<String, Object> handleRequest);
}
