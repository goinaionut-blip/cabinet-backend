package ro.cabinet.backend.auth;

import ro.cabinet.backend.config.JwtProperties;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final JwtProperties properties;

  public JwtService(JwtProperties properties) {
    this.properties = properties;
  }

  public String generateToken(String username) {
    Instant now = Instant.now();
    return Jwts.builder()
        .setSubject(username)
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(now.plus(properties.getExpirationMinutes(), ChronoUnit.MINUTES)))
        .signWith(getKey())
        .compact();
  }

  public String generateTokenV2(String email, UUID userId, Map<String, Object> optionalClaims) {
    Instant now = Instant.now();
    io.jsonwebtoken.JwtBuilder builder = Jwts.builder()
        .setSubject(email)
        .claim("uid", userId.toString())
        .claim("v", 2)
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(now.plus(properties.getExpirationMinutes(), ChronoUnit.MINUTES)));

    if (optionalClaims != null) {
      optionalClaims.forEach(builder::claim);
    }

    return builder.signWith(getKey()).compact();
  }

  public String extractUsername(String token) {
    return parseClaims(token).getSubject();
  }

  public String extractUid(String token) {
    Object uid = parseClaims(token).get("uid");
    return uid == null ? null : uid.toString();
  }

  public boolean isValid(String token) {
    try {
      parseClaims(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  private Claims parseClaims(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(getKey())
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  private SecretKey getKey() {
    byte[] key = properties.getSecret().getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(key);
  }
}
