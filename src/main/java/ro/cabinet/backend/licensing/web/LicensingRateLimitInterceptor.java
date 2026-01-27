package ro.cabinet.backend.licensing.web;

import ro.cabinet.backend.licensing.config.LicensingProperties;
import ro.cabinet.backend.licensing.exception.LicensingRateLimitException;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LicensingRateLimitInterceptor implements HandlerInterceptor {
  private final LicensingProperties properties;
  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

  public LicensingRateLimitInterceptor(LicensingProperties properties) {
    this.properties = properties;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    int windowSeconds = properties.getRateLimit().getWindowSeconds();
    int maxRequests = properties.getRateLimit().getMaxRequests();
    if (windowSeconds <= 0 || maxRequests <= 0) {
      return true;
    }
    String key = clientKey(request);
    long nowMillis = Instant.now().toEpochMilli();
    Window updated = windows.compute(key, (k, window) -> {
      if (window == null || nowMillis - window.windowStartMillis >= windowSeconds * 1000L) {
        return new Window(nowMillis, 1);
      }
      window.count++;
      return window;
    });
    if (updated.count > maxRequests) {
      throw new LicensingRateLimitException("Rate limit exceeded.");
    }
    return true;
  }

  private String clientKey(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      int comma = forwarded.indexOf(',');
      return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
    }
    return request.getRemoteAddr();
  }

  private static class Window {
    private final long windowStartMillis;
    private int count;

    private Window(long windowStartMillis, int count) {
      this.windowStartMillis = windowStartMillis;
      this.count = count;
    }
  }
}
