package ro.cabinet.backend.notifications.dto;

import ro.cabinet.backend.notifications.NotificationPreference;
import ro.cabinet.backend.notifications.WahaSessionStatus;

import java.util.UUID;

public class ClinicNotificationSettingsResponse {
  private UUID clinicId;
  private boolean whatsappEnabled;
  private String wahaSessionName;
  private boolean smsFallbackEnabled;
  private boolean whatsappReplyProcessingEnabled;
  private int replyWindowHours;
  private boolean saveReplyPreview;
  private NotificationPreference defaultPreference;
  private WahaSessionStatus wahaSessionStatus;
  private boolean qrAvailable;

  public UUID getClinicId() {
    return clinicId;
  }

  public void setClinicId(UUID clinicId) {
    this.clinicId = clinicId;
  }

  public boolean isWhatsappEnabled() {
    return whatsappEnabled;
  }

  public void setWhatsappEnabled(boolean whatsappEnabled) {
    this.whatsappEnabled = whatsappEnabled;
  }

  public String getWahaSessionName() {
    return wahaSessionName;
  }

  public void setWahaSessionName(String wahaSessionName) {
    this.wahaSessionName = wahaSessionName;
  }

  public boolean isSmsFallbackEnabled() {
    return smsFallbackEnabled;
  }

  public void setSmsFallbackEnabled(boolean smsFallbackEnabled) {
    this.smsFallbackEnabled = smsFallbackEnabled;
  }

  public boolean isWhatsappReplyProcessingEnabled() {
    return whatsappReplyProcessingEnabled;
  }

  public void setWhatsappReplyProcessingEnabled(boolean whatsappReplyProcessingEnabled) {
    this.whatsappReplyProcessingEnabled = whatsappReplyProcessingEnabled;
  }

  public int getReplyWindowHours() {
    return replyWindowHours;
  }

  public void setReplyWindowHours(int replyWindowHours) {
    this.replyWindowHours = replyWindowHours;
  }

  public boolean isSaveReplyPreview() {
    return saveReplyPreview;
  }

  public void setSaveReplyPreview(boolean saveReplyPreview) {
    this.saveReplyPreview = saveReplyPreview;
  }

  public NotificationPreference getDefaultPreference() {
    return defaultPreference;
  }

  public void setDefaultPreference(NotificationPreference defaultPreference) {
    this.defaultPreference = defaultPreference;
  }

  public WahaSessionStatus getWahaSessionStatus() {
    return wahaSessionStatus;
  }

  public void setWahaSessionStatus(WahaSessionStatus wahaSessionStatus) {
    this.wahaSessionStatus = wahaSessionStatus;
  }

  public boolean isQrAvailable() {
    return qrAvailable;
  }

  public void setQrAvailable(boolean qrAvailable) {
    this.qrAvailable = qrAvailable;
  }
}
