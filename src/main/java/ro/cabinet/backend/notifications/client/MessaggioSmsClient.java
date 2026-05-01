package ro.cabinet.backend.notifications.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ro.cabinet.backend.notifications.config.NotificationsProperties;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;

import org.springframework.stereotype.Component;

@Component
public class MessaggioSmsClient {
  private static final Logger LOGGER = Logger.getLogger(MessaggioSmsClient.class.getName());

  private final NotificationsProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;

  public MessaggioSmsClient(NotificationsProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(Math.max(1, properties.getWaha().getTimeoutSeconds())))
        .build();
  }

  public ProviderSendResult send(String phoneE164, String text, String externalId) {
    NotificationsProperties.MessaggioProperties sms = properties.getSms().getMessaggio();
    if (!sms.isEnabled()) {
      return ProviderSendResult.failure("Messaggio SMS is disabled.");
    }
    if (isBlank(sms.getLogin())) {
      return ProviderSendResult.failure("MESSAGGIO_LOGIN is missing.");
    }
    String normalizedPhone = normalizePhoneForApi(phoneE164);
    if (isBlank(normalizedPhone)) {
      return ProviderSendResult.failure("Invalid SMS phone number.");
    }
    if (isBlank(text)) {
      return ProviderSendResult.failure("SMS text is required.");
    }

    try {
      ObjectNode payload = objectMapper.createObjectNode();
      ArrayNode recipients = payload.putArray("recipients");
      ObjectNode recipient = objectMapper.createObjectNode();
      recipient.put("phone", normalizedPhone);
      recipients.add(recipient);
      ArrayNode channels = payload.putArray("channels");
      channels.add("sms");

      ObjectNode smsNode = payload.putObject("sms");
      smsNode.put("from", sms.getSenderId());
      ArrayNode content = smsNode.putArray("content");
      ObjectNode contentText = objectMapper.createObjectNode();
      contentText.put("type", "text");
      contentText.put("text", text);
      content.add(contentText);

      if (!isBlank(externalId)) {
        payload.putObject("options").put("external_id", externalId);
      }

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(normalizeBaseUrl() + "/send"))
          .timeout(Duration.ofSeconds(Math.max(1, properties.getWaha().getTimeoutSeconds())))
          .header("Accept", "application/json")
          .header("Content-Type", "application/json")
          .header("Messaggio-Login", sms.getLogin())
          .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        return ProviderSendResult.failure("Messaggio send failed: HTTP " + response.statusCode());
      }
      return ProviderSendResult.success(extractMessageId(response.body()));
    } catch (IOException ex) {
      LOGGER.fine("Messaggio IO error: " + ex.getMessage());
      return ProviderSendResult.failure("Messaggio IO error: " + ex.getMessage());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return ProviderSendResult.failure("Messaggio send interrupted.");
    } catch (Exception ex) {
      return ProviderSendResult.failure("Messaggio send failed: " + ex.getMessage());
    }
  }

  private String extractMessageId(String body) throws IOException {
    if (isBlank(body)) {
      return null;
    }
    JsonNode root = objectMapper.readTree(body);
    JsonNode messages = root.path("messages");
    if (messages.isArray() && !messages.isEmpty()) {
      JsonNode first = messages.get(0);
      String id = text(first, "message_id");
      if (!isBlank(id)) {
        return id;
      }
    }
    return text(root, "message_id");
  }

  private String normalizeBaseUrl() {
    String baseUrl = properties.getSms().getMessaggio().getBaseUrl();
    if (baseUrl == null) {
      return "";
    }
    if (baseUrl.endsWith("/")) {
      return baseUrl.substring(0, baseUrl.length() - 1);
    }
    return baseUrl;
  }

  private static String normalizePhoneForApi(String phoneE164) {
    if (phoneE164 == null) {
      return null;
    }
    String normalized = phoneE164.trim();
    if (normalized.startsWith("+")) {
      normalized = normalized.substring(1);
    }
    normalized = normalized.replaceAll("[^0-9]", "");
    if (!normalized.matches("\\d{8,15}")) {
      return null;
    }
    return normalized;
  }

  private static String text(JsonNode node, String field) {
    JsonNode value = node == null ? null : node.get(field);
    return value == null ? null : value.asText(null);
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
