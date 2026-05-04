package ro.cabinet.backend.notifications;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class WhatsappReplyClassifier {
  private static final Set<String> POSITIVE = Set.of(
      "da", "ok", "confirm", "vin", "ajung", "este ok"
  );
  private static final Set<String> NEGATIVE = Set.of(
      "nu", "nu pot", "nu ajung", "anulez", "reprogramare", "alta data", "schimbare ora"
  );

  public NotificationReplyStatus classify(String text, boolean appointmentReminder) {
    String normalized = normalize(text);
    if (normalized.isBlank()) {
      return NotificationReplyStatus.IGNORED;
    }
    if (matches(normalized, POSITIVE)) {
      return appointmentReminder ? NotificationReplyStatus.CONFIRMED : NotificationReplyStatus.NEEDS_REVIEW;
    }
    if (matches(normalized, NEGATIVE)) {
      return appointmentReminder ? NotificationReplyStatus.NEEDS_REVIEW : NotificationReplyStatus.DECLINED;
    }
    return NotificationReplyStatus.NEEDS_REVIEW;
  }

  private boolean matches(String normalized, Set<String> options) {
    if (options.contains(normalized)) {
      return true;
    }
    for (String option : options) {
      if (normalized.equals(option)
          || normalized.startsWith(option + " ")
          || normalized.endsWith(" " + option)
          || normalized.contains(" " + option + " ")) {
        return true;
      }
    }
    return false;
  }

  static String normalize(String text) {
    if (text == null) {
      return "";
    }
    String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
        .replaceAll("\\p{M}+", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", " ")
        .trim();
    return normalized.replaceAll("\\s+", " ");
  }
}
