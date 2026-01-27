package ro.cabinet.backend.licensing.exception;

public class LicensingUnauthorizedException extends RuntimeException {
  public LicensingUnauthorizedException(String message) {
    super(message);
  }
}
