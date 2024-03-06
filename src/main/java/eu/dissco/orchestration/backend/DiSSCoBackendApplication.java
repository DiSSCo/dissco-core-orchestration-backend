package eu.dissco.orchestration.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DiSSCoBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(DiSSCoBackendApplication.class, args);
  }

}
