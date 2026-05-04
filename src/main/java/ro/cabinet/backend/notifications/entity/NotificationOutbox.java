package ro.cabinet.backend.notifications.entity;

import ro.cabinet.backend.notifications.NotificationChannel;
import ro.cabinet.backend.notifications.NotificationPreference;
import ro.cabinet.backend.notifications.NotificationReplyStatus;
import ro.cabinet.backend.notifications.NotificationStatus;

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
@Table(name = "notification_outbox")
public class NotificationOutbox {
  @Id
  private UUID id;

  @Column(name = "clinic_id", nullable = false)
  private UUID clinicId;

  @Column(name = "doctor_id")
  private UUID doctorId;

  @Column(name = "patient_id")
  private String patientId;

  @Column(name = "patient_name", nullable = false)
  private String patientName;

  @Column(name = "phone_e164", nullable = false)
  private String phoneE164;

  @Column(name = "appointment_external_id")
  private String appointmentExternalId;

  @Column(name = "appointment_date_time")
  private OffsetDateTime appointmentDateTime;

  @Column(name = "message_text", nullable = false, columnDefinition = "text")
  private String messageText;

  @Column(name = "reminder_type_code")
  private String reminderTypeCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "preference", nullable = false)
  private NotificationPreference preference;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private NotificationStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "channel_used")
  private NotificationChannel channelUsed;

  @Column(name = "fallback_used", nullable = false)
  private boolean fallbackUsed;

  @Column(name = "provider_message_id")
  private String providerMessageId;

  @Column(name = "correlation_code")
  private String correlationCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "reply_status", nullable = false)
  private NotificationReplyStatus replyStatus;

  @Column(name = "reply_received_at")
  private OffsetDateTime replyReceivedAt;

  @Column(name = "reply_text_preview", length = 200)
  private String replyTextPreview;

  @Column(name = "reply_processed_at")
  private OffsetDateTime replyProcessedAt;

  @Column(name = "last_error", columnDefinition = "text")
  private String lastError;

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
    if (replyStatus == null) {
      replyStatus = NotificationReplyStatus.NONE;
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

  public UUID getDoctorId() {
    return doctorId;
  }

  public void setDoctorId(UUID doctorId) {
    this.doctorId = doctorId;
  }

  public String getPatientId() {
    return patientId;
  }

  public void setPatientId(String patientId) {
    this.patientId = patientId;
  }

  public String getPatientName() {
    return patientName;
  }

  public void setPatientName(String patientName) {
    this.patientName = patientName;
  }

  public String getPhoneE164() {
    return phoneE164;
  }

  public void setPhoneE164(String phoneE164) {
    this.phoneE164 = phoneE164;
  }

  public String getAppointmentExternalId() {
    return appointmentExternalId;
  }

  public void setAppointmentExternalId(String appointmentExternalId) {
    this.appointmentExternalId = appointmentExternalId;
  }

  public OffsetDateTime getAppointmentDateTime() {
    return appointmentDateTime;
  }

  public void setAppointmentDateTime(OffsetDateTime appointmentDateTime) {
    this.appointmentDateTime = appointmentDateTime;
  }

  public String getMessageText() {
    return messageText;
  }

  public void setMessageText(String messageText) {
    this.messageText = messageText;
  }

  public String getReminderTypeCode() {
    return reminderTypeCode;
  }

  public void setReminderTypeCode(String reminderTypeCode) {
    this.reminderTypeCode = reminderTypeCode;
  }

  public NotificationPreference getPreference() {
    return preference;
  }

  public void setPreference(NotificationPreference preference) {
    this.preference = preference;
  }

  public NotificationStatus getStatus() {
    return status;
  }

  public void setStatus(NotificationStatus status) {
    this.status = status;
  }

  public NotificationChannel getChannelUsed() {
    return channelUsed;
  }

  public void setChannelUsed(NotificationChannel channelUsed) {
    this.channelUsed = channelUsed;
  }

  public boolean isFallbackUsed() {
    return fallbackUsed;
  }

  public void setFallbackUsed(boolean fallbackUsed) {
    this.fallbackUsed = fallbackUsed;
  }

  public String getProviderMessageId() {
    return providerMessageId;
  }

  public void setProviderMessageId(String providerMessageId) {
    this.providerMessageId = providerMessageId;
  }

  public String getCorrelationCode() {
    return correlationCode;
  }

  public void setCorrelationCode(String correlationCode) {
    this.correlationCode = correlationCode;
  }

  public NotificationReplyStatus getReplyStatus() {
    return replyStatus;
  }

  public void setReplyStatus(NotificationReplyStatus replyStatus) {
    this.replyStatus = replyStatus;
  }

  public OffsetDateTime getReplyReceivedAt() {
    return replyReceivedAt;
  }

  public void setReplyReceivedAt(OffsetDateTime replyReceivedAt) {
    this.replyReceivedAt = replyReceivedAt;
  }

  public String getReplyTextPreview() {
    return replyTextPreview;
  }

  public void setReplyTextPreview(String replyTextPreview) {
    this.replyTextPreview = replyTextPreview;
  }

  public OffsetDateTime getReplyProcessedAt() {
    return replyProcessedAt;
  }

  public void setReplyProcessedAt(OffsetDateTime replyProcessedAt) {
    this.replyProcessedAt = replyProcessedAt;
  }

  public String getLastError() {
    return lastError;
  }

  public void setLastError(String lastError) {
    this.lastError = lastError;
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
