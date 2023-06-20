package eu.dissco.orchestration.backend.exception;

public class ProcessingFailedException extends RuntimeException {

  public ProcessingFailedException(String message, Throwable e) {
    super(message, e);
  }
}
