package ro.cabinet.backend.sign;

import java.util.Locale;

public enum SignTemplateId {
  INFORMED_CONSENT,
  HEALTH_QUESTIONNAIRE,
  GDPR;

  public static SignTemplateId fromString(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return SignTemplateId.valueOf(value.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
