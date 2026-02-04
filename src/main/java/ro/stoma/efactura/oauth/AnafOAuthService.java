package ro.stoma.efactura.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

import ro.stoma.efactura.config.EfacturaProperties;
import ro.stoma.efactura.oauth.lock.AnafTokenRefreshLockService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

@Service
public class AnafOAuthService {
  private static final Logger log = LoggerFactory.getLogger(AnafOAuthService.class);

  private final EfacturaProperties properties;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private final Map<String, AnafToken> tokenByCif = new ConcurrentHashMap<>();
  private final AnafTokenRefreshLockService refreshLockService;
  private final Retry tokenRetry;
  private static final Duration REFRESH_LEAD_TIME = Duration.ofMinutes(5);

  public AnafOAuthService(EfacturaProperties properties,
                          RestTemplateBuilder restTemplateBuilder,
                          ObjectMapper objectMapper,
                          @Qualifier("efacturaClientHttpRequestFactory") ClientHttpRequestFactory requestFactory,
                          AnafTokenRefreshLockService refreshLockService) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.refreshLockService = refreshLockService;
    this.restTemplate = restTemplateBuilder
        .requestFactory(() -> requestFactory)
        .build();
    this.tokenRetry = buildTokenRetry(properties);
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
      token = refreshTokenSingleFlight(token, key);
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

  @Scheduled(fixedDelay = 60000)
  public void scheduledTokenRefresh() {
    Set<String> keys = new HashSet<>(tokenByCif.keySet());
    String defaultCif = normalizeCif(properties.getCif());
    if (defaultCif != null && !defaultCif.isBlank()) {
      keys.add(defaultCif);
    }
    for (String key : keys) {
      maybeRefreshSoonExpiringToken(key);
    }
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
    AtomicInteger attempt = new AtomicInteger(0);
    ResponseEntity<Map> response = Retry.decorateSupplier(tokenRetry, () -> {
      int attemptNumber = attempt.incrementAndGet();
      long startNs = System.nanoTime();
      try {
        return restTemplate.postForEntity(
            properties.getOauth().getTokenUrl(),
            new HttpEntity<>(form, headers),
            Map.class);
      } catch (Exception ex) {
        logTokenAttemptFailure(attemptNumber, startNs, ex);
        throw ex;
      }
    }).get();

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

  private Retry buildTokenRetry(EfacturaProperties properties) {
    long initialDelayMs = Math.max(100L, properties.getApi().getRetryDelay().toMillis());
    IntervalFunction intervalFunction = IntervalFunction.ofExponentialRandomBackoff(initialDelayMs, 2.0d, 0.5d);
    RetryConfig config = RetryConfig.custom()
        .maxAttempts(4)
        .intervalFunction(intervalFunction)
        .retryOnException(this::isRetryableTokenException)
        .build();
    return Retry.of("anafToken", config);
  }

  private boolean isRetryableTokenException(Throwable throwable) {
    if (!(throwable instanceof ResourceAccessException)) {
      return false;
    }
    Throwable cause = rootCause(throwable);
    return cause instanceof java.net.SocketTimeoutException
        || cause instanceof java.net.ConnectException
        || cause instanceof java.net.UnknownHostException;
  }

  private void logTokenAttemptFailure(int attemptNumber, long startNs, Exception ex) {
    long totalMs = Duration.ofNanos(System.nanoTime() - startNs).toMillis();
    long connectMs = isConnectFailure(ex) ? totalMs : -1L;
    String root = rootCause(ex).getClass().getSimpleName();
    String extra = "";
    if (ex instanceof HttpStatusCodeException status) {
      extra = " status=" + status.getStatusCode().value();
    }
    log.warn("ANAF token attempt={} failed totalMs={} connectMs={} rootCause={}{}",
        attemptNumber, totalMs, connectMs, root, extra);
  }

  private boolean isConnectFailure(Throwable throwable) {
    Throwable cause = rootCause(throwable);
    return cause instanceof java.net.SocketTimeoutException
        || cause instanceof java.net.ConnectException
        || cause instanceof java.net.UnknownHostException;
  }

  private Throwable rootCause(Throwable throwable) {
    Throwable current = throwable;
    while (current.getCause() != null && current.getCause() != current) {
      current = current.getCause();
    }
    return current;
  }

  private void saveToken(AnafToken token, String cif) {
    String key = normalizeCif(cif);
    if (key == null || key.isBlank()) {
      key = "default";
    }
    tokenByCif.put(key, token);
    persistTokenToFile(token, key);
  }

  private void maybeRefreshSoonExpiringToken(String key) {
    AnafToken token = tokenByCif.get(key);
    if (token == null) {
      token = loadTokenFromFile(key);
      if (token != null) {
        tokenByCif.put(key, token);
      }
    }
    if (token == null) {
      return;
    }
    if (token.getExpiresAt() == null) {
      return;
    }
    Instant now = Instant.now();
    if (token.getExpiresAt().isBefore(now.plus(REFRESH_LEAD_TIME))) {
      log.info("ANAF token near expiry; scheduled refresh for cif={}", key);
      AnafToken refreshed = refreshTokenSingleFlight(token, key);
      saveToken(refreshed, key);
    }
  }

  private AnafToken refreshTokenSingleFlight(AnafToken existing, String key) {
    if (!refreshLockService.tryAcquire(key)) {
      log.info("ANAF refresh already in progress for cif={}", key);
      AnafToken updated = loadTokenFromFile(key);
      if (updated != null && !updated.isExpired(Instant.now())) {
        tokenByCif.put(key, updated);
        return updated;
      }
      return existing;
    }
    try {
      AnafToken refreshed = refreshToken(existing);
      return refreshed;
    } finally {
      refreshLockService.release(key);
    }
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
