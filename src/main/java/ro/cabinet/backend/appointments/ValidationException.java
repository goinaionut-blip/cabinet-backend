package ro.cabinet.backend.appointments;

public class ValidationException extends RuntimeException {
  public ValidationException(String message) {
    super(message);
  }
}
