package eu.dissco.orchestration.backend.controller;


import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class RestResponseEntityEntityExceptionHandler {

  private RestResponseEntityExceptionHandler exceptionHandler;

  @BeforeEach
  void setup() {
    exceptionHandler = new RestResponseEntityExceptionHandler();
  }

  @Test
  void testNotFoundException() {
    // When
    var result = exceptionHandler.handleException(new NotFoundException(""));

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

}
