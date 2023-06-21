package eu.dissco.orchestration.backend.exception;

public class KubernetesFailedException extends Exception {

  public KubernetesFailedException(String message) {
    super(message);
  }
}
