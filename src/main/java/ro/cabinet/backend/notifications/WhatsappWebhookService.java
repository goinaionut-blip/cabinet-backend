package ro.cabinet.backend.notifications;

import com.fasterxml.jackson.databind.JsonNode;
import ro.cabinet.backend.notifications.dto.WhatsappWebhookResponse;
import ro.cabinet.backend.notifications.entity.ClinicNotificationSettings;
import ro.cabinet.backend.notifications.entity.NotificationOutbox;
import ro.cabinet.backend.notifications.repo.NotificationOutboxRepository;
import ro.cabinet.backend.v2.entity.AppointmentV2;
import ro.cabinet.backend.v2.repo.AppointmentV2Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WhatsappWebhookService {
  private static final Logger LOGGER = Logger.getLogger(WhatsappWebhookService.class.getName());
  private static final Pattern CORRELATION_CODE_PATTERN = Pattern.compile("\\b([A-Z]{1,4}[A-Z0-9]{3,8})\\b");
  private static final int PREVIEW_LIMIT = 200;
  private static final Collection<NotificationStatus> REPLY_ELIGIBLE_STATUSES = Set.of(NotificationStatus.SENT);

  private final NotificationOutboxRepository outboxRepository;
  private final ClinicNotificationSettingsService settingsService;
  private final WhatsappReplyClassifier classifier;
  private final AppointmentV2Repository appointmentRepository;

  public WhatsappWebhookService(NotificationOutboxRepository outboxRepository,
                                ClinicNotificationSettingsService settingsService,
                                WhatsappReplyClassifier classifier,
                                AppointmentV2Repository appointmentRepository) {
    this.outboxRepository = outboxRepository;
    this.settingsService = settingsService;
    this.classifier = classifier;
    this.appointmentRepository = appointmentRepository;
  }

  @Transactional
  public WhatsappWebhookResponse process(JsonNode payload) {
    String session = extractText(payload, "session");
    String from = extractText(payload, "from");
    String body = extractBody(payload);
    OffsetDateTime receivedAt = extractTimestamp(payload);
    if (extractBoolean(payload, "fromMe")) {
      return new WhatsappWebhookResponse("ignored");
    }

    if (body == null || body.isBlank()) {
      logIgnored(from, session, receivedAt, "empty_text");
      return new WhatsappWebhookResponse("ignored");
    }

    String normalizedPhone = normalizePhone(from);
    if (normalizedPhone == null) {
      logIgnored(from, session, receivedAt, "invalid_phone");
      return new WhatsappWebhookResponse("ignored");
    }

    NotificationOutbox outbox = findMatchingOutbox(session, normalizedPhone, body, receivedAt);
    if (outbox == null) {
      logIgnored(normalizedPhone, session, receivedAt, "no_recent_reminder");
      return new WhatsappWebhookResponse("ignored");
    }

    ClinicNotificationSettings settings = settingsService.getOrCreateSettings(outbox.getClinicId());
    if (!settings.isWhatsappReplyProcessingEnabled()) {
      logIgnored(normalizedPhone, session, receivedAt, "reply_processing_disabled");
      return new WhatsappWebhookResponse("ignored");
    }

    NotificationReplyStatus replyStatus = classifier.classify(body, isAppointmentReminder(outbox));
    outbox.setReplyStatus(replyStatus);
    outbox.setReplyReceivedAt(receivedAt);
    outbox.setReplyProcessedAt(OffsetDateTime.now());
    outbox.setReplyTextPreview(settings.isSaveReplyPreview() ? abbreviate(body, PREVIEW_LIMIT) : null);
    if (replyStatus == NotificationReplyStatus.CONFIRMED && isAppointmentReminder(outbox)) {
      markAppointmentConfirmed(outbox);
    }
    outboxRepository.save(outbox);
    return new WhatsappWebhookResponse("processed");
  }

  private NotificationOutbox findMatchingOutbox(String session, String phoneE164, String body, OffsetDateTime receivedAt) {
    OffsetDateTime cutoff = receivedAt.minusDays(30);
    String correlationCode = extractCorrelationCode(body);
    if (correlationCode != null) {
      List<NotificationOutbox> byCode = outboxRepository
          .findTop20ByCorrelationCodeAndChannelUsedAndStatusInAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
              correlationCode, NotificationChannel.WHATSAPP, REPLY_ELIGIBLE_STATUSES, cutoff);
      NotificationOutbox matched = filterBySession(byCode, session, receivedAt);
      if (matched != null) {
        return matched;
      }
    }

    List<NotificationOutbox> byPhone = outboxRepository
        .findTop20ByPhoneE164AndChannelUsedAndStatusInAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            phoneE164, NotificationChannel.WHATSAPP, REPLY_ELIGIBLE_STATUSES, cutoff);
    return filterBySession(byPhone, session, receivedAt);
  }

  private NotificationOutbox filterBySession(List<NotificationOutbox> candidates, String session, OffsetDateTime receivedAt) {
    if (candidates == null || candidates.isEmpty()) {
      return null;
    }
    for (NotificationOutbox candidate : candidates) {
      ClinicNotificationSettings settings = settingsService.getOrCreateSettings(candidate.getClinicId());
      int replyWindowHours = Math.max(1, settings.getReplyWindowHours());
      if (!settings.isWhatsappReplyProcessingEnabled()) {
        continue;
      }
      if (session != null && !session.isBlank()
          && !session.equalsIgnoreCase(settingsService.resolveWahaSessionName(candidate.getClinicId()))) {
        continue;
      }
      if (candidate.getCreatedAt() != null && candidate.getCreatedAt().isBefore(receivedAt.minusHours(replyWindowHours))) {
        continue;
      }
      return candidate;
    }
    return null;
  }

  private boolean isAppointmentReminder(NotificationOutbox outbox) {
    return outbox != null && outbox.getAppointmentExternalId() != null && !outbox.getAppointmentExternalId().isBlank();
  }

  private void markAppointmentConfirmed(NotificationOutbox outbox) {
    String appointmentId = outbox.getAppointmentExternalId();
    if (appointmentId == null || appointmentId.isBlank()) {
      return;
    }
    try {
      AppointmentV2 appointment = appointmentRepository.findById(UUID.fromString(appointmentId)).orElse(null);
      if (appointment != null) {
        appointment.setStatus("CONFIRMED");
        appointmentRepository.save(appointment);
      }
    } catch (IllegalArgumentException ignored) {
      LOGGER.fine("Appointment id is not UUID: " + appointmentId);
    }
  }

  private String extractCorrelationCode(String body) {
    if (body == null) {
      return null;
    }
    Matcher matcher = CORRELATION_CODE_PATTERN.matcher(body.toUpperCase(Locale.ROOT));
    return matcher.find() ? matcher.group(1) : null;
  }

  private static String extractText(JsonNode payload, String field) {
    if (payload == null || field == null) {
      return null;
    }
    JsonNode direct = payload.get(field);
    if (direct != null && direct.isTextual()) {
      return direct.asText();
    }
    for (String container : List.of("event", "payload", "message", "data")) {
      JsonNode nested = payload.path(container).get(field);
      if (nested != null && nested.isValueNode()) {
        String value = nested.asText();
        if (value != null && !value.isBlank()) {
          return value;
        }
      }
    }
    return null;
  }

  private static String extractBody(JsonNode payload) {
    for (String field : List.of("body", "text", "message")) {
      String value = extractText(payload, field);
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    JsonNode textNode = payload == null ? null : payload.path("text").path("body");
    if (textNode != null && textNode.isTextual()) {
      return textNode.asText().trim();
    }
    return null;
  }

  private static OffsetDateTime extractTimestamp(JsonNode payload) {
    JsonNode node = findValueNode(payload, "timestamp");
    if (node == null || node.isNull()) {
      return OffsetDateTime.now();
    }
    if (node.isNumber()) {
      long raw = node.asLong();
      Instant instant = raw > 9_999_999_999L ? Instant.ofEpochMilli(raw) : Instant.ofEpochSecond(raw);
      return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
    String text = node.asText();
    if (text == null || text.isBlank()) {
      return OffsetDateTime.now();
    }
    try {
      return OffsetDateTime.parse(text);
    } catch (DateTimeParseException ignored) {
      try {
        long raw = Long.parseLong(text.trim());
        Instant instant = raw > 9_999_999_999L ? Instant.ofEpochMilli(raw) : Instant.ofEpochSecond(raw);
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
      } catch (NumberFormatException ignoredToo) {
        return OffsetDateTime.now();
      }
    }
  }

  private static JsonNode findValueNode(JsonNode payload, String field) {
    if (payload == null) {
      return null;
    }
    JsonNode direct = payload.get(field);
    if (direct != null && direct.isValueNode()) {
      return direct;
    }
    for (String container : List.of("event", "payload", "message", "data")) {
      JsonNode nested = payload.path(container).get(field);
      if (nested != null && nested.isValueNode()) {
        return nested;
      }
    }
    return null;
  }

  private static boolean extractBoolean(JsonNode payload, String field) {
    JsonNode value = findValueNode(payload, field);
    return value != null && value.asBoolean(false);
  }

  private static String normalizePhone(String from) {
    if (from == null) {
      return null;
    }
    String digits = from.replaceAll("[^0-9]", "");
    if (digits.length() < 8 || digits.length() > 15) {
      return null;
    }
    if (digits.startsWith("00")) {
      digits = digits.substring(2);
    }
    return "+" + digits;
  }

  private void logIgnored(String phone, String session, OffsetDateTime timestamp, String reason) {
    LOGGER.info(() -> "WhatsApp webhook ignored: phone=" + maskPhone(phone)
        + ", reason=" + reason
        + ", session=" + safe(session)
        + ", timestamp=" + timestamp);
  }

  private static String maskPhone(String phone) {
    String normalized = normalizePhone(phone);
    if (normalized == null) {
      return "sha256:" + hash(phone);
    }
    String digits = normalized.substring(1);
    String suffix = digits.length() <= 4 ? digits : digits.substring(digits.length() - 4);
    return "***" + suffix;
  }

  private static String hash(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] bytes = digest.digest(String.valueOf(value).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(bytes, 0, 6);
    } catch (Exception ex) {
      return "na";
    }
  }

  private static String abbreviate(String value, int maxLength) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.length() <= maxLength) {
      return trimmed;
    }
    return trimmed.substring(0, maxLength);
  }

  private static String safe(String value) {
    return value == null ? "-" : value;
  }
}
