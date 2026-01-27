package ro.cabinet.backend.licensing.exception;

public class LicensingRateLimitException extends RuntimeException {
  public LicensingRateLimitException(String message) {
    super(message);
  }
}
