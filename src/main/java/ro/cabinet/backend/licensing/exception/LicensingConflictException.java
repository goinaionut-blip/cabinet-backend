package ro.cabinet.backend.licensing.exception;

public class LicensingConflictException extends RuntimeException {
  public LicensingConflictException(String message) {
    super(message);
  }
}
