package eu.dissco.orchestration.backend.client;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

public interface HandleClient {

  @PostExchange("/batch")
  List<Map<String, Map<String, ?>>> postHandle(@RequestBody Map<String, ?> handleRequest);

  @PutExchange("/{pid}")
  Map<String, ?> tombstoneHandle(@PathVariable String pid, @RequestBody Map<String, ?> handleRequest);

  @DeleteExchange("/{pid}")
  void rollbackHandle(@RequestBody Map<String, ?> handleRequest);
}
