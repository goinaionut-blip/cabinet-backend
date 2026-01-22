package ro.stoma.efactura.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

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
  private final AtomicReference<AnafToken> tokenRef = new AtomicReference<>();

  public AnafOAuthService(EfacturaProperties properties,
                          RestTemplateBuilder restTemplateBuilder,
                          ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.restTemplate = restTemplateBuilder
        .setConnectTimeout(properties.getApi().getTimeout())
        .setReadTimeout(properties.getApi().getTimeout())
        .build();
    loadTokenFromFile();
  }

  public String buildLoginUrl() {
    // ANAF specific: authorization endpoint and required query params.
    String redirectUri = resolveBackendRedirectUri();
    return UriComponentsBuilder.fromHttpUrl(properties.getOauth().getAuthorizeUrl())
        .queryParam("response_type", "code")
        .queryParam("client_id", properties.getClientId())
        .queryParam("redirect_uri", redirectUri)
        .build()
        .toUriString();
  }

  public AnafToken exchangeCodeForToken(String code) {
    String redirectUri = resolveBackendRedirectUri();
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "authorization_code");
    form.add("code", code);
    form.add("redirect_uri", redirectUri);
    form.add("client_id", properties.getClientId());
    form.add("client_secret", properties.getClientSecret());

    AnafToken token = requestToken(form);
    saveToken(token);
    return token;
  }

  public String getValidAccessToken() {
    AnafToken token = tokenRef.get();
    if (token == null) {
      throw new IllegalStateException("ANAF token missing. Authenticate first.");
    }
    if (token.isExpired(Instant.now())) {
      token = refreshToken(token);
      saveToken(token);
    }
    return token.getAccessToken();
  }

  public boolean hasToken() {
    return tokenRef.get() != null;
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
    return token;
  }

  private void saveToken(AnafToken token) {
    tokenRef.set(token);
    persistTokenToFile(token);
  }

  private void loadTokenFromFile() {
    Path path = tokenFilePath();
    if (path == null || !Files.exists(path)) {
      return;
    }
    try {
      AnafToken token = objectMapper.readValue(path.toFile(), AnafToken.class);
      tokenRef.set(token);
    } catch (IOException ex) {
      log.warn("Failed to load ANAF token file.");
    }
  }

  private void persistTokenToFile(AnafToken token) {
    Path path = tokenFilePath();
    if (path == null) {
      return;
    }
    try {
      objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), token);
    } catch (IOException ex) {
      log.warn("Failed to persist ANAF token file.");
    }
  }

  private Path tokenFilePath() {
    String tokenFile = properties.getOauth().getTokenFile();
    if (tokenFile == null || tokenFile.isBlank()) {
      return null;
    }
    return Path.of(tokenFile);
  }

  private String resolveBackendRedirectUri() {
    String backend = properties.getRedirectUri().getBackend();
    if (backend != null && !backend.isBlank()) {
      return backend;
    }
    return properties.getRedirectUri().getSwing();
  }
}
