package ro.cabinet.backend.licensing.exception;

public class LicensingBadRequestException extends RuntimeException {
  public LicensingBadRequestException(String message) {
    super(message);
  }
}
