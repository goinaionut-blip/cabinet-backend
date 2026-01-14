package ro.cabinet.backend.appointments;

public class OverlapException extends RuntimeException {
  public OverlapException(String message) {
    super(message);
  }
}
