package eu.dissco.orchestration.backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.dissco.orchestration.backend.exception.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

  @ResponseStatus(HttpStatus.NOT_FOUND)
  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<String> handleException(NotFoundException e){
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
  }

  @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<String> handleException(JsonProcessingException e){
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(e.getMessage());
  }


}
