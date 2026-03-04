package ro.cabinet.backend.v2.exception;

public class V2ValidationException extends RuntimeException {
  public V2ValidationException(String message) {
    super(message);
  }
}
