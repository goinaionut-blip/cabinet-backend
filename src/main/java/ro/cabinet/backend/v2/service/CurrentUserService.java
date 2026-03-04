package ro.cabinet.backend.v2.service;

import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class CurrentUserService {
  public static final String UID_REQUEST_ATTR = "jwt.uid";

  public UUID requireCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new AccessDeniedException("Forbidden");
    }

    String uid = extractUidFromDetails(authentication.getDetails());
    if (uid == null) {
      uid = extractUidFromRequest();
    }
    if (uid == null || uid.isBlank()) {
      throw new AccessDeniedException("Token v2 required");
    }

    try {
      return UUID.fromString(uid);
    } catch (IllegalArgumentException ex) {
      throw new AccessDeniedException("Invalid uid claim");
    }
  }

  private String extractUidFromDetails(Object details) {
    if (details instanceof Map<?, ?> map) {
      Object value = map.get("uid");
      if (value != null) {
        return value.toString();
      }
    }
    return null;
  }

  private String extractUidFromRequest() {
    if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
      return null;
    }
    HttpServletRequest request = attrs.getRequest();
    Object value = request.getAttribute(UID_REQUEST_ATTR);
    return value == null ? null : value.toString();
  }
}
