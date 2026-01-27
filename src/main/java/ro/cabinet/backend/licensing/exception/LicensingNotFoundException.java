package ro.cabinet.backend.licensing.exception;

public class LicensingNotFoundException extends RuntimeException {
  public LicensingNotFoundException(String message) {
    super(message);
  }
}
