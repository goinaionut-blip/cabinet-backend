package ro.stoma.efactura.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Objects;
import java.util.stream.Collectors;

import ro.stoma.efactura.config.EfacturaProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class AnafOAuthService {
  private static final Logger log = LoggerFactory.getLogger(AnafOAuthService.class);

  private final EfacturaProperties properties;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private final Map<String, AnafToken> tokenByCif = new ConcurrentHashMap<>();

  public AnafOAuthService(EfacturaProperties properties,
                          RestTemplateBuilder restTemplateBuilder,
                          ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.restTemplate = restTemplateBuilder
        .setConnectTimeout(properties.getApi().getTimeout())
        .setReadTimeout(properties.getApi().getTimeout())
        .build();
    loadTokenFromFile(properties.getCif());
    logNetworkSettings();
  }

  public String buildLoginUrl(String cif) {
    // ANAF specific: authorization endpoint and required query params.
    String redirectUri = resolveBackendRedirectUri();
    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(properties.getOauth().getAuthorizeUrl())
        .queryParam("response_type", "code")
        .queryParam("client_id", properties.getClientId())
        .queryParam("redirect_uri", redirectUri)
        .queryParam("token_content_type", "jwt");
    String resolvedCif = normalizeCif(cif);
    if (resolvedCif != null && !resolvedCif.isBlank()) {
      builder.queryParam("state", resolvedCif);
    }
    return builder.build().toUriString();
  }

  public AnafToken exchangeCodeForToken(String code, String cif) {
    String redirectUri = resolveBackendRedirectUri();
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "authorization_code");
    form.add("code", code);
    form.add("redirect_uri", redirectUri);
    form.add("client_id", properties.getClientId());
    form.add("client_secret", properties.getClientSecret());
    form.add("token_content_type", "jwt");

    AnafToken token = requestToken(form);
    saveToken(token, cif);
    return token;
  }

  public String getValidAccessToken(String cif) {
    String key = normalizeCif(cif);
    if (key == null || key.isBlank()) {
      key = "default";
    }
    AnafToken token = tokenByCif.get(key);
    if (token == null) {
      token = loadTokenFromFile(key);
      if (token != null) {
        tokenByCif.put(key, token);
      }
    }
    if (token == null) {
      log.warn("ANAF token missing; authentication required");
      throw new IllegalStateException("ANAF token missing. Authenticate first.");
    }
    log.info("ANAF token expiresAt={}", token.getExpiresAt());
    if (token.isExpired(Instant.now())) {
      log.info("ANAF token expired; refreshing");
      token = refreshToken(token);
      saveToken(token, key);
    }
    return token.getAccessToken();
  }

  public boolean hasToken(String cif) {
    String key = normalizeCif(cif);
    if (key == null || key.isBlank()) {
      key = "default";
    }
    AnafToken token = tokenByCif.get(key);
    if (token != null) {
      return true;
    }
    token = loadTokenFromFile(key);
    if (token != null) {
      tokenByCif.put(key, token);
      return true;
    }
    return false;
  }

  public String tokenInfo(String cif) {
    String key = normalizeCif(cif);
    if (key == null || key.isBlank()) {
      key = "default";
    }
    AnafToken token = tokenByCif.get(key);
    if (token == null) {
      token = loadTokenFromFile(key);
      if (token != null) {
        tokenByCif.put(key, token);
      }
    }
    if (token == null) {
      return "NO_TOKEN";
    }
    return "access=" + token.maskedAccessToken()
        + " refresh=" + token.maskedRefreshToken()
        + " expiresAt=" + token.getExpiresAt();
  }

  public String pingAuthorizeEndpoint() {
    try {
      ResponseEntity<String> response = restTemplate.getForEntity(
          properties.getOauth().getAuthorizeUrl(), String.class);
      return "OK " + response.getStatusCode().value();
    } catch (Exception ex) {
      return "ERROR " + ex.getClass().getSimpleName() + ": " + ex.getMessage();
    }
  }

  public String resolveAnafHost() {
    try {
      InetAddress[] addresses = InetAddress.getAllByName("logincert.anaf.ro");
      return "OK " + java.util.Arrays.stream(addresses)
          .map(addr -> addr.getHostAddress())
          .collect(Collectors.joining(", "));
    } catch (UnknownHostException ex) {
      return "ERROR UnknownHostException: " + ex.getMessage();
    }
  }

  public String pingTokenEndpoint() {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "authorization_code");
    form.add("code", "invalid");
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    int attempts = Math.max(1, properties.getApi().getMaxRetries() + 1);
    Duration delay = properties.getApi().getRetryDelay();
    Exception last = null;
    for (int i = 1; i <= attempts; i++) {
      try {
        ResponseEntity<String> response = restTemplate.postForEntity(
            properties.getOauth().getTokenUrl(),
            new HttpEntity<>(form, headers),
            String.class);
        return "OK " + response.getStatusCode().value() + " " + safeBody(response.getBody());
      } catch (Exception ex) {
        last = ex;
        if (i < attempts) {
          sleepQuietly(delay);
        }
      }
    }
    return "ERROR " + last.getClass().getSimpleName() + ": " + last.getMessage();
  }

  private String safeBody(String body) {
    if (body == null) {
      return "";
    }
    String trimmed = body.trim();
    if (trimmed.length() > 200) {
      return trimmed.substring(0, 200) + "...";
    }
    return trimmed;
  }

  private void sleepQuietly(Duration delay) {
    if (delay == null || delay.isZero() || delay.isNegative()) {
      return;
    }
    try {
      Thread.sleep(delay.toMillis());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  private void logNetworkSettings() {
    String preferV4 = System.getProperty("java.net.preferIPv4Stack");
    String preferV6 = System.getProperty("java.net.preferIPv6Addresses");
    log.info("Network prefs preferIPv4Stack={} preferIPv6Addresses={}", preferV4, preferV6);
    log.info("ANAF resolve {}", resolveAnafHost());
  }

  private AnafToken refreshToken(AnafToken existing) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "refresh_token");
    form.add("refresh_token", existing.getRefreshToken());
    form.add("client_id", properties.getClientId());
    form.add("client_secret", properties.getClientSecret());
    return requestToken(form);
  }

  private AnafToken requestToken(MultiValueMap<String, String> form) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    ResponseEntity<Map> response = restTemplate.postForEntity(
        properties.getOauth().getTokenUrl(),
        new HttpEntity<>(form, headers),
        Map.class);

    Map body = response.getBody();
    if (body == null) {
      throw new IllegalStateException("ANAF token response was empty.");
    }
    Object accessToken = body.get("access_token");
    Object refreshToken = body.get("refresh_token");
    Object expiresIn = body.get("expires_in");
    if (accessToken == null || refreshToken == null || expiresIn == null) {
      throw new IllegalStateException("ANAF token response missing required fields.");
    }

    AnafToken token = new AnafToken();
    token.setAccessToken(Objects.toString(accessToken, null));
    token.setRefreshToken(Objects.toString(refreshToken, null));
    long seconds = Long.parseLong(Objects.toString(expiresIn, "0"));
    token.setExpiresAt(Instant.now().plusSeconds(seconds));
    log.info("ANAF token received expiresInSeconds={}", seconds);
    return token;
  }

  private void saveToken(AnafToken token, String cif) {
    String key = normalizeCif(cif);
    if (key == null || key.isBlank()) {
      key = "default";
    }
    tokenByCif.put(key, token);
    persistTokenToFile(token, key);
  }

  private AnafToken loadTokenFromFile(String cif) {
    Path path = tokenFilePath(cif);
    if (path == null || !Files.exists(path)) {
      return null;
    }
    try {
      return objectMapper.readValue(path.toFile(), AnafToken.class);
    } catch (IOException ex) {
      log.warn("Failed to load ANAF token file.");
      return null;
    }
  }

  private void persistTokenToFile(AnafToken token, String cif) {
    Path path = tokenFilePath(cif);
    if (path == null) {
      return;
    }
    try {
      objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), token);
    } catch (IOException ex) {
      log.warn("Failed to persist ANAF token file.");
    }
  }

  private Path tokenFilePath(String cif) {
    String tokenFile = properties.getOauth().getTokenFile();
    if (tokenFile == null || tokenFile.isBlank()) {
      return null;
    }
    String resolvedCif = normalizeCif(cif);
    if (resolvedCif != null && !resolvedCif.isBlank()) {
      if (tokenFile.contains("{cif}")) {
        tokenFile = tokenFile.replace("{cif}", resolvedCif);
      } else {
        int dot = tokenFile.lastIndexOf('.');
        if (dot > 0) {
          tokenFile = tokenFile.substring(0, dot) + "-" + resolvedCif + tokenFile.substring(dot);
        } else {
          tokenFile = tokenFile + "-" + resolvedCif;
        }
      }
    }
    return Path.of(tokenFile);
  }

  private String normalizeCif(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim().toUpperCase();
    if (trimmed.startsWith("RO")) {
      trimmed = trimmed.substring(2);
    }
    StringBuilder digits = new StringBuilder();
    for (int i = 0; i < trimmed.length(); i++) {
      char ch = trimmed.charAt(i);
      if (Character.isDigit(ch)) {
        digits.append(ch);
      }
    }
    return digits.toString();
  }

  private String resolveBackendRedirectUri() {
    String backend = properties.getRedirectUri().getBackend();
    if (backend != null && !backend.isBlank()) {
      return backend;
    }
    return properties.getRedirectUri().getSwing();
  }
}
