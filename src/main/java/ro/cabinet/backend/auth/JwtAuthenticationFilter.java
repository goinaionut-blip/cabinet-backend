package ro.cabinet.backend.auth;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;

  public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
    this.jwtService = jwtService;
    this.userDetailsService = userDetailsService;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ")) {
      log.info("JWT missing/invalid header path={} method={}", request.getRequestURI(), request.getMethod());
      filterChain.doFilter(request, response);
      return;
    }
    String token = header.substring(7);
    if (!jwtService.isValid(token)) {
      log.info("JWT invalid token path={} method={}", request.getRequestURI(), request.getMethod());
      filterChain.doFilter(request, response);
      return;
    }
    String username = jwtService.extractUsername(token);
    log.info("JWT ok user={} path={} method={}", username, request.getRequestURI(), request.getMethod());
    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
        userDetails, null, userDetails.getAuthorities()
    );
    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(auth);
    filterChain.doFilter(request, response);
  }
}
