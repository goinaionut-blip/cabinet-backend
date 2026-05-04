package ro.cabinet.backend.notifications.entity;

import ro.cabinet.backend.notifications.NotificationPreference;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "clinic_notification_settings")
public class ClinicNotificationSettings {
  @Id
  private UUID id;

  @Column(name = "clinic_id", nullable = false, unique = true)
  private UUID clinicId;

  @Column(name = "whatsapp_enabled", nullable = false)
  private boolean whatsappEnabled;

  @Column(name = "waha_session_name", nullable = false)
  private String wahaSessionName;

  @Column(name = "sms_fallback_enabled", nullable = false)
  private boolean smsFallbackEnabled;

  @Column(name = "whatsapp_reply_processing_enabled", nullable = false)
  private boolean whatsappReplyProcessingEnabled;

  @Column(name = "reply_window_hours", nullable = false)
  private int replyWindowHours;

  @Column(name = "save_reply_preview", nullable = false)
  private boolean saveReplyPreview;

  @Enumerated(EnumType.STRING)
  @Column(name = "default_preference", nullable = false)
  private NotificationPreference defaultPreference;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @PrePersist
  public void onCreate() {
    OffsetDateTime now = OffsetDateTime.now();
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (createdAt == null) {
      createdAt = now;
    }
    updatedAt = now;
  }

  @PreUpdate
  public void onUpdate() {
    updatedAt = OffsetDateTime.now();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

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

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
