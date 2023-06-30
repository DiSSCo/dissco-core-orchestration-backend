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

  @Bean(name = "keda-template")
  public Template kedaTemplate() throws IOException {
    return configuration.getTemplate("keda-template.ftl");
  }

  @Bean(name = "deployment-template")
  public Template deploymentTemplate() throws IOException {
    return configuration.getTemplate("mas-template.ftl");
  }


}
