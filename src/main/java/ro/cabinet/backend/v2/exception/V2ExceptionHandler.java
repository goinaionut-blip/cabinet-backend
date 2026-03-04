package ro.cabinet.backend.v2.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "ro.cabinet.backend.v2")
public class V2ExceptionHandler {
  @ExceptionHandler(V2NotFoundException.class)
  public ResponseEntity<Map<String, String>> notFound(V2NotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
  }

  @ExceptionHandler(V2ValidationException.class)
  public ResponseEntity<Map<String, String>> badRequest(V2ValidationException ex) {
    return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> methodValidation(MethodArgumentNotValidException ex) {
    return ResponseEntity.badRequest().body(Map.of("error", "Date invalide."));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String, String>> forbidden(AccessDeniedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Forbidden"));
  }
}
