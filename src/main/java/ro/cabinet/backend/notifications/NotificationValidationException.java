package ro.cabinet.backend.notifications;

public class NotificationValidationException extends RuntimeException {
  public NotificationValidationException(String message) {
    super(message);
  }
}
