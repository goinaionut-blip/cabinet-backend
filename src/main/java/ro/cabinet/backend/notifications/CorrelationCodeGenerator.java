package ro.cabinet.backend.notifications;

import java.security.SecureRandom;
import java.util.Locale;

import org.springframework.stereotype.Component;

@Component
public class CorrelationCodeGenerator {
  private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private final SecureRandom random = new SecureRandom();

  public String generate(String reminderTypeCode) {
    String prefix = buildPrefix(reminderTypeCode);
    return prefix + randomToken(4);
  }

  private String buildPrefix(String reminderTypeCode) {
    if (reminderTypeCode == null || reminderTypeCode.isBlank()) {
      return "R";
    }
    String normalized = reminderTypeCode.replaceAll("[^A-Za-z]", "").toUpperCase(Locale.ROOT);
    if (normalized.isBlank()) {
      return "R";
    }
    if (normalized.length() >= 3) {
      return normalized.substring(0, 3);
    }
    return normalized;
  }

  private String randomToken(int length) {
    StringBuilder builder = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      builder.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
    }
    return builder.toString();
  }
}
