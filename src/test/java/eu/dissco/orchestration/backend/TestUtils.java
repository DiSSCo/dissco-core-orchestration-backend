package eu.dissco.orchestration.backend;

import eu.dissco.orchestration.backend.domain.TranslatorRequest;
import eu.dissco.orchestration.backend.domain.TranslatorResponse;
import eu.dissco.orchestration.backend.domain.TranslatorType;
import java.time.Instant;

public class TestUtils {

  public static final String JOB_NAME = "job-test";
  public static final String CONTAINER_NAME = "container-test";
  public static final String CONTAINER_IMAGE = "dissco.eu/translator";

  public static final String KAFKA_HOST = "kafka-service";
  public static final String KAFKA_TOPIC = "kafka-topic";

  public static final String REQUEST_ENDPOINT = "http://endpoint.com";
  public static final String QUERY = "?*";

  public static TranslatorResponse givenResponse() {
    return TranslatorResponse.builder()
        .jobName(JOB_NAME)
        .jobStatus("Completed")
        .startTime(Instant.parse("2022-03-23T23:00:00.00Z"))
        .completedAt(Instant.parse("2022-03-24T23:00:00.00Z"))
        .build();
  }

  public static TranslatorResponse givenResponseActive() {
    return TranslatorResponse.builder()
        .jobName(JOB_NAME)
        .jobStatus("Active")
        .startTime(Instant.parse("2022-03-23T23:00:00.00Z"))
        .build();
  }

  public static TranslatorRequest givenRequest() {
    var request = new TranslatorRequest();
    request.setTranslatorType(TranslatorType.DWCA);
    request.setServiceName("Test");
    request.setEndPoint(REQUEST_ENDPOINT);
    request.setQuery(QUERY);
    return request;
  }

}
