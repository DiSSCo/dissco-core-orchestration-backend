package eu.dissco.orchestration.backend.controller;


import static org.assertj.core.api.Assertions.assertThat;

import eu.dissco.orchestration.backend.exception.ForbiddenException;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class RestResponseEntityEntityExceptionHandlerTest {

  private RestResponseEntityExceptionHandler exceptionHandler;

  @BeforeEach
  void setup() {
    exceptionHandler = new RestResponseEntityExceptionHandler();
  }

  @Test
  void testNotFoundException() {
    // When
    var result = exceptionHandler.notFoundException(new NotFoundException(""));

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void testIllegalArgumentException() {
    // When
    var result = exceptionHandler.illegalArgumentException(new IllegalArgumentException(""));

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void testForbiddenException() {
    // When
    var result = exceptionHandler.forbiddenException(new ForbiddenException(""));

    // Then
    assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
  }

}
