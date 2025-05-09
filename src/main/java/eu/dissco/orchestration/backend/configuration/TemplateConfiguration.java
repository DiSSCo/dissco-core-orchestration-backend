package eu.dissco.orchestration.backend.configuration;

import freemarker.template.Template;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class TemplateConfiguration {

  private final freemarker.template.Configuration configuration;

  @Bean(name = "kedaTemplate")
  public Template kedaTemplate() throws IOException {
    return configuration.getTemplate("keda-template.ftl");
  }

  @Bean(name = "deploymentTemplate")
  public Template deploymentTemplate() throws IOException {
    return configuration.getTemplate("mas-template.ftl");
  }

  @Bean(name = "masRabbitBindingTemplate")
  public Template rabbitBindingTemplate() throws IOException {
    return configuration.getTemplate("mas-rabbitmq-binding.ftl");
  }

  @Bean(name = "masRabbitQueueTemplate")
  public Template rabbitQueueTemplate() throws IOException {
    return configuration.getTemplate("mas-rabbitmq-queue.ftl");
  }

}
