package ro.cabinet.backend.notifications.dto;

import ro.cabinet.backend.notifications.NotificationPreference;

import java.util.UUID;

public class ClinicNotificationSettingsRequest {
  private UUID clinicId;
  private Boolean whatsappEnabled;
  private String wahaSessionName;
  private Boolean smsFallbackEnabled;
  private NotificationPreference defaultPreference;

  public UUID getClinicId() {
    return clinicId;
  }

  public void setClinicId(UUID clinicId) {
    this.clinicId = clinicId;
  }

  public Boolean getWhatsappEnabled() {
    return whatsappEnabled;
  }

  public void setWhatsappEnabled(Boolean whatsappEnabled) {
    this.whatsappEnabled = whatsappEnabled;
  }

  public String getWahaSessionName() {
    return wahaSessionName;
  }

  public void setWahaSessionName(String wahaSessionName) {
    this.wahaSessionName = wahaSessionName;
  }

  public Boolean getSmsFallbackEnabled() {
    return smsFallbackEnabled;
  }

  public void setSmsFallbackEnabled(Boolean smsFallbackEnabled) {
    this.smsFallbackEnabled = smsFallbackEnabled;
  }

  public NotificationPreference getDefaultPreference() {
    return defaultPreference;
  }

  public void setDefaultPreference(NotificationPreference defaultPreference) {
    this.defaultPreference = defaultPreference;
  }
}
