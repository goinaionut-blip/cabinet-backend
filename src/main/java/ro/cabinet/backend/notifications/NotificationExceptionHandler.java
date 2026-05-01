package ro.cabinet.backend.notifications;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "ro.cabinet.backend.notifications")
public class NotificationExceptionHandler {
  @ExceptionHandler(NotificationValidationException.class)
  public ResponseEntity<Map<String, String>> badRequest(NotificationValidationException ex) {
    return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException ex) {
    return ResponseEntity.badRequest().body(Map.of("error", "Date invalide."));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String, String>> forbidden(AccessDeniedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Forbidden"));
  }
}
