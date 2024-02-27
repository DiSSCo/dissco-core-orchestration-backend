package eu.dissco.orchestration.backend.exception;

public class ProcessingFailedException extends Exception {

  public ProcessingFailedException(String message, Throwable e) {
    super(message, e);
  }
}
