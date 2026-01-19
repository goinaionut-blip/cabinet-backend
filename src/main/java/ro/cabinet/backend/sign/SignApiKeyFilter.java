package ro.cabinet.backend.sign;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SignApiKeyFilter extends OncePerRequestFilter {
  private final SignProperties properties;

  public SignApiKeyFilter(SignProperties properties) {
    this.properties = properties;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
    String path = request.getRequestURI();
    if (path == null || !path.startsWith("/api/sign")) {
      filterChain.doFilter(request, response);
      return;
    }
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      filterChain.doFilter(request, response);
      return;
    }
    String configuredKey = properties.getApiKey();
    if (configuredKey == null || configuredKey.isBlank()) {
      response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "SIGN_API_KEY not configured");
      return;
    }
    String headerKey = request.getHeader("X-API-KEY");
    if (headerKey == null || !headerKey.equals(configuredKey)) {
      response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid API key");
      return;
    }
    filterChain.doFilter(request, response);
  }
}
