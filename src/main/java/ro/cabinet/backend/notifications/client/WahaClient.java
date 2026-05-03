package ro.cabinet.backend.notifications.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ro.cabinet.backend.notifications.WahaSessionStatus;
import ro.cabinet.backend.notifications.config.NotificationsProperties;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.logging.Logger;

import org.springframework.stereotype.Component;

@Component
public class WahaClient {
  private static final Logger LOGGER = Logger.getLogger(WahaClient.class.getName());

  private final NotificationsProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public WahaClient(NotificationsProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(Math.max(1, properties.getWaha().getTimeoutSeconds())))
        .build();
  }

  public ProviderSendResult sendText(String sessionName, String phoneE164, String text) {
    String resolvedSession = resolveSessionName(sessionName);
    if (!properties.getWaha().isEnabled()) {
      return ProviderSendResult.failure("WAHA is disabled.");
    }
    if (isBlank(text)) {
      return ProviderSendResult.failure("WhatsApp text is required.");
    }
    String chatId = formatChatId(phoneE164);
    if (chatId == null) {
      return ProviderSendResult.failure("Invalid WhatsApp phone number.");
    }
    if (isBlank(resolvedSession)) {
      return ProviderSendResult.failure("WAHA session name is required.");
    }

    try {
      ObjectNode payload = objectMapper.createObjectNode();
      payload.put("session", resolvedSession);
      payload.put("chatId", chatId);
      payload.put("text", text);
      HttpRequest request = authorizedJsonRequest(sendTextUrl(), "POST", objectMapper.writeValueAsString(payload));
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (!isSuccess(response.statusCode())) {
        return ProviderSendResult.failure(buildProviderError("WAHA sendText", response.statusCode(), response.body()));
      }
      return ProviderSendResult.success(extractWahaMessageId(response.body()));
    } catch (IOException ex) {
      LOGGER.fine("WAHA sendText IO error: " + ex.getMessage());
      return ProviderSendResult.failure("WAHA sendText IO error: " + ex.getMessage());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return ProviderSendResult.failure("WAHA sendText interrupted.");
    } catch (Exception ex) {
      return ProviderSendResult.failure("WAHA sendText failed: " + ex.getMessage());
    }
  }

  public WahaSessionInfo getSessionStatus(String sessionName) {
    String resolvedSession = resolveSessionName(sessionName);
    if (!properties.getWaha().isEnabled()) {
      return new WahaSessionInfo(resolvedSession, WahaSessionStatus.UNKNOWN, "WAHA is disabled.");
    }
    if (isBlank(resolvedSession)) {
      return new WahaSessionInfo(resolvedSession, WahaSessionStatus.UNKNOWN, "WAHA session name is required.");
    }

    try {
      HttpRequest request = authorizedJsonRequest(sessionStatusUrl(resolvedSession), "GET", null);
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (!isSuccess(response.statusCode())) {
        return new WahaSessionInfo(resolvedSession, WahaSessionStatus.UNKNOWN,
            buildProviderError("WAHA session status", response.statusCode(), response.body()));
      }
      JsonNode root = objectMapper.readTree(response.body());
      return new WahaSessionInfo(resolvedSession, parseSessionStatus(root), extractSessionMessage(root));
    } catch (IOException ex) {
      LOGGER.fine("WAHA status IO error: " + ex.getMessage());
      return new WahaSessionInfo(resolvedSession, WahaSessionStatus.UNKNOWN, ex.getMessage());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return new WahaSessionInfo(resolvedSession, WahaSessionStatus.UNKNOWN, "WAHA status interrupted.");
    } catch (Exception ex) {
      return new WahaSessionInfo(resolvedSession, WahaSessionStatus.FAILED, ex.getMessage());
    }
  }

  public WahaSessionInfo createOrStartSession(String sessionName) {
    String resolvedSession = resolveSessionName(sessionName);
    if (!properties.getWaha().isEnabled()) {
      return new WahaSessionInfo(resolvedSession, WahaSessionStatus.UNKNOWN, "WAHA is disabled.");
    }
    if (isBlank(resolvedSession)) {
      return new WahaSessionInfo(resolvedSession, WahaSessionStatus.UNKNOWN, "WAHA session name is required.");
    }

    try {
      ObjectNode payload = objectMapper.createObjectNode();
      payload.put("name", resolvedSession);

      HttpRequest createRequest = authorizedJsonRequest(sessionsCollectionUrl(), "POST",
          objectMapper.writeValueAsString(payload));
      HttpResponse<String> createResponse = httpClient.send(createRequest, HttpResponse.BodyHandlers.ofString());
      if (!isAcceptableCreateResponse(createResponse)) {
        return new WahaSessionInfo(resolvedSession, WahaSessionStatus.FAILED,
            buildProviderError("WAHA create session", createResponse.statusCode(), createResponse.body()));
      }

      HttpRequest startRequest = authorizedJsonRequest(sessionStartUrl(resolvedSession), "POST", "");
      HttpResponse<String> startResponse = httpClient.send(startRequest, HttpResponse.BodyHandlers.ofString());
      if (!isSuccess(startResponse.statusCode()) && startResponse.statusCode() != 409) {
        return new WahaSessionInfo(resolvedSession, WahaSessionStatus.FAILED,
            buildProviderError("WAHA start session", startResponse.statusCode(), startResponse.body()));
      }
      WahaSessionInfo status = getSessionStatus(resolvedSession);
      return new WahaSessionInfo(resolvedSession, status.getStatus(),
          isBlank(status.getMessage()) ? "WAHA session requested." : status.getMessage());
    } catch (IOException ex) {
      LOGGER.fine("WAHA create/start IO error: " + ex.getMessage());
      return new WahaSessionInfo(resolvedSession, WahaSessionStatus.UNKNOWN, ex.getMessage());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return new WahaSessionInfo(resolvedSession, WahaSessionStatus.UNKNOWN, "WAHA start interrupted.");
    } catch (Exception ex) {
      return new WahaSessionInfo(resolvedSession, WahaSessionStatus.FAILED, ex.getMessage());
    }
  }

  public WahaQrData getQr(String sessionName) {
    String resolvedSession = resolveSessionName(sessionName);
    WahaSessionInfo status = getSessionStatus(resolvedSession);
    if (!properties.getWaha().isEnabled()) {
      return new WahaQrData(resolvedSession, null, null, status.getStatus(), status.getMessage());
    }
    if (status.getStatus() != WahaSessionStatus.SCAN_QR && status.getStatus() != WahaSessionStatus.STARTING) {
      return new WahaQrData(resolvedSession, null, null, status.getStatus(),
          isBlank(status.getMessage()) ? "WAHA session is not waiting for QR." : status.getMessage());
    }

    try {
      HttpRequest jsonRequest = authorizedQrRequest(sessionQrJsonUrl(resolvedSession), "application/json");
      HttpResponse<String> jsonResponse = httpClient.send(jsonRequest, HttpResponse.BodyHandlers.ofString());
      if (isSuccess(jsonResponse.statusCode())) {
        WahaQrData jsonData = parseJsonQrResponse(resolvedSession, status, jsonResponse.body());
        if (jsonData != null) {
          return jsonData;
        }
      }

      HttpRequest rawRequest = authorizedQrRequest(sessionQrRawUrl(resolvedSession), "application/json");
      HttpResponse<String> rawResponse = httpClient.send(rawRequest, HttpResponse.BodyHandlers.ofString());
      if (isSuccess(rawResponse.statusCode())) {
        WahaQrData rawData = parseRawQrResponse(resolvedSession, status, rawResponse.body());
        if (rawData != null) {
          return rawData;
        }
      }

      HttpRequest imageRequest = authorizedQrRequest(sessionQrJsonUrl(resolvedSession), "image/png");
      HttpResponse<byte[]> imageResponse = httpClient.send(imageRequest, HttpResponse.BodyHandlers.ofByteArray());
      if (isSuccess(imageResponse.statusCode())) {
        byte[] bytes = imageResponse.body();
        if (bytes != null && bytes.length > 0) {
          return new WahaQrData(resolvedSession, null, Base64.getEncoder().encodeToString(bytes),
              status.getStatus(), status.getMessage());
        }
      }

      String errorMessage = firstQrError(jsonResponse, rawResponse, imageResponse);
      return new WahaQrData(resolvedSession, null, null, status.getStatus(), errorMessage);
    } catch (IOException ex) {
      LOGGER.fine("WAHA QR IO error: " + ex.getMessage());
      return new WahaQrData(resolvedSession, null, null, status.getStatus(), ex.getMessage());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return new WahaQrData(resolvedSession, null, null, status.getStatus(), "WAHA QR interrupted.");
    } catch (Exception ex) {
      return new WahaQrData(resolvedSession, null, null, WahaSessionStatus.FAILED, ex.getMessage());
    }
  }

  public static String formatChatId(String phoneE164) {
    if (phoneE164 == null) {
      return null;
    }
    String normalized = phoneE164.trim().replace(" ", "").replace("-", "").replace("(", "").replace(")", "");
    if (normalized.isEmpty()) {
      return null;
    }
    if (normalized.startsWith("00")) {
      normalized = normalized.substring(2);
    } else if (normalized.startsWith("+")) {
      normalized = normalized.substring(1);
    }
    if (!normalized.matches("\\d{8,15}")) {
      return null;
    }
    return normalized + "@c.us";
  }

  private HttpRequest authorizedJsonRequest(String url, String method, String body) {
    HttpRequest.Builder builder = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(Duration.ofSeconds(Math.max(1, properties.getWaha().getTimeoutSeconds())))
        .header("Accept", "application/json");
    addApiKey(builder);
    if ("GET".equals(method)) {
      return builder.GET().build();
    }
    builder.header("Content-Type", "application/json");
    return builder.method(method, HttpRequest.BodyPublishers.ofString(body == null ? "" : body)).build();
  }

  private HttpRequest authorizedBytesRequest(String url) {
    HttpRequest.Builder builder = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(Duration.ofSeconds(Math.max(1, properties.getWaha().getTimeoutSeconds())));
    addApiKey(builder);
    return builder.GET().build();
  }

  private HttpRequest authorizedQrRequest(String url, String acceptHeader) {
    HttpRequest.Builder builder = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(Duration.ofSeconds(Math.max(1, properties.getWaha().getTimeoutSeconds())));
    addApiKey(builder);
    if (!isBlank(acceptHeader)) {
      builder.header("Accept", acceptHeader);
    }
    return builder.GET().build();
  }

  private void addApiKey(HttpRequest.Builder builder) {
    if (!isBlank(properties.getWaha().getApiKey())) {
      builder.header("X-Api-Key", properties.getWaha().getApiKey());
    }
  }

  private String resolveSessionName(String sessionName) {
    if (!isBlank(sessionName)) {
      return sessionName.trim();
    }
    if (!isBlank(properties.getWaha().getDefaultSession())) {
      return properties.getWaha().getDefaultSession().trim();
    }
    return "default";
  }

  private String sendTextUrl() {
    return normalizeBaseUrl() + "/api/sendText";
  }

  private String sessionsCollectionUrl() {
    return normalizeBaseUrl() + "/api/sessions";
  }

  private String sessionStatusUrl(String sessionName) {
    return normalizeBaseUrl() + "/api/sessions/" + urlEncode(sessionName);
  }

  private String sessionStartUrl(String sessionName) {
    return normalizeBaseUrl() + "/api/sessions/" + urlEncode(sessionName) + "/start";
  }

  private String sessionQrUrl(String sessionName) {
    return normalizeBaseUrl() + "/api/" + urlEncode(sessionName) + "/auth/qr";
  }

  private String sessionQrJsonUrl(String sessionName) {
    return sessionQrUrl(sessionName);
  }

  private String sessionQrRawUrl(String sessionName) {
    return sessionQrUrl(sessionName) + "?format=raw";
  }

  private String normalizeBaseUrl() {
    String baseUrl = properties.getWaha().getBaseUrl();
    if (baseUrl == null) {
      return "";
    }
    if (baseUrl.endsWith("/")) {
      return baseUrl.substring(0, baseUrl.length() - 1);
    }
    return baseUrl;
  }

  private String extractWahaMessageId(String body) throws IOException {
    if (isBlank(body)) {
      return null;
    }
    JsonNode root = objectMapper.readTree(body);
    String id = text(root, "id");
    if (!isBlank(id)) {
      return id;
    }
    JsonNode key = root.path("key");
    id = text(key, "id");
    if (!isBlank(id)) {
      return id;
    }
    return text(root, "messageId");
  }

  private WahaSessionStatus parseSessionStatus(JsonNode root) {
    String raw = text(root, "status");
    if (isBlank(raw)) {
      return WahaSessionStatus.UNKNOWN;
    }
    String normalized = raw.trim().toUpperCase().replace('-', '_');
    if ("WORKING".equals(normalized) || "READY".equals(normalized) || "CONNECTED".equals(normalized)) {
      return WahaSessionStatus.WORKING;
    }
    if ("SCAN_QR".equals(normalized) || "SCAN_QR_CODE".equals(normalized)) {
      return WahaSessionStatus.SCAN_QR;
    }
    if ("STARTING".equals(normalized)) {
      return WahaSessionStatus.STARTING;
    }
    if ("STOPPED".equals(normalized)) {
      return WahaSessionStatus.STOPPED;
    }
    if ("FAILED".equals(normalized)) {
      return WahaSessionStatus.FAILED;
    }
    return WahaSessionStatus.UNKNOWN;
  }

  private String extractSessionMessage(JsonNode root) {
    String message = text(root, "message");
    if (!isBlank(message)) {
      return message;
    }
    JsonNode engine = root.path("engine");
    String state = text(engine, "state");
    if (!isBlank(state)) {
      return "engine.state=" + state;
    }
    return null;
  }

  private WahaQrData parseJsonQrResponse(String sessionName, WahaSessionInfo status, String body) throws IOException {
    if (isBlank(body)) {
      return null;
    }
    JsonNode root = objectMapper.readTree(body);
    String base64 = text(root, "data");
    if (!isBlank(base64)) {
      return new WahaQrData(sessionName, null, base64, status.getStatus(), status.getMessage());
    }
    return null;
  }

  private WahaQrData parseRawQrResponse(String sessionName, WahaSessionInfo status, String body) throws IOException {
    if (isBlank(body)) {
      return null;
    }
    JsonNode root = objectMapper.readTree(body);
    String qrValue = text(root, "value");
    if (!isBlank(qrValue)) {
      return new WahaQrData(sessionName, qrValue, null, status.getStatus(), status.getMessage());
    }
    return null;
  }

  private String firstQrError(HttpResponse<String> jsonResponse,
                              HttpResponse<String> rawResponse,
                              HttpResponse<byte[]> imageResponse) {
    if (jsonResponse != null && !isSuccess(jsonResponse.statusCode())) {
      return buildProviderError("WAHA QR", jsonResponse.statusCode(), jsonResponse.body());
    }
    if (rawResponse != null && !isSuccess(rawResponse.statusCode())) {
      return buildProviderError("WAHA QR raw", rawResponse.statusCode(), rawResponse.body());
    }
    if (imageResponse != null && !isSuccess(imageResponse.statusCode())) {
      return buildProviderError("WAHA QR image", imageResponse.statusCode(), toUtf8(imageResponse.body()));
    }
    return "WAHA QR is not available for this session.";
  }

  private static boolean isSuccess(int statusCode) {
    return statusCode >= 200 && statusCode < 300;
  }

  private boolean isAcceptableCreateResponse(HttpResponse<String> response) {
    if (response == null) {
      return false;
    }
    if (isSuccess(response.statusCode()) || response.statusCode() == 409) {
      return true;
    }
    if (response.statusCode() == 422) {
      String body = response.body();
      if (!isBlank(body)) {
        String normalized = body.trim().toLowerCase();
        if (normalized.contains("already exists")) {
          return true;
        }
      }
    }
    return false;
  }

  private static String buildProviderError(String operation, int statusCode, String body) {
    String trimmed = body == null ? "" : body.replace('\n', ' ').replace('\r', ' ').trim();
    if (trimmed.length() > 240) {
      trimmed = trimmed.substring(0, 240) + "...";
    }
    if (trimmed.isEmpty()) {
      return operation + " failed with HTTP " + statusCode;
    }
    return operation + " failed with HTTP " + statusCode + ": " + trimmed;
  }

  private static String text(JsonNode node, String field) {
    JsonNode value = node == null ? null : node.get(field);
    return value == null ? null : value.asText(null);
  }

  private static String header(HttpResponse<?> response, String name) {
    return response.headers().firstValue(name).orElse(null);
  }

  private static String toUtf8(byte[] bytes) {
    return new String(bytes == null ? new byte[0] : bytes, StandardCharsets.UTF_8);
  }

  private static String urlEncode(String value) {
    return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
