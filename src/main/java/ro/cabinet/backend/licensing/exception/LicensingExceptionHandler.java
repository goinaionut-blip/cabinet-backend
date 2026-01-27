package ro.cabinet.backend.licensing.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class LicensingExceptionHandler {
  @ExceptionHandler(LicensingNotFoundException.class)
  public ResponseEntity<Map<String, String>> notFound(LicensingNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
  }

  @ExceptionHandler(LicensingConflictException.class)
  public ResponseEntity<Map<String, String>> conflict(LicensingConflictException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
  }

  @ExceptionHandler(LicensingBadRequestException.class)
  public ResponseEntity<Map<String, String>> badRequest(LicensingBadRequestException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
  }

  @ExceptionHandler(LicensingUnauthorizedException.class)
  public ResponseEntity<Map<String, String>> unauthorized(LicensingUnauthorizedException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
  }

  @ExceptionHandler(LicensingRateLimitException.class)
  public ResponseEntity<Map<String, String>> rateLimit(LicensingRateLimitException ex) {
    return ResponseEntity.status(429).body(Map.of("error", ex.getMessage()));
  }
}
