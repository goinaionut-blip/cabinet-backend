package ro.cabinet.backend.notifications.entity;

import ro.cabinet.backend.notifications.NotificationChannel;
import ro.cabinet.backend.notifications.NotificationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_attempt")
public class NotificationAttempt {
  @Id
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "notification_outbox_id", nullable = false)
  private NotificationOutbox notificationOutbox;

  @Enumerated(EnumType.STRING)
  @Column(name = "channel", nullable = false)
  private NotificationChannel channel;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private NotificationStatus status;

  @Column(name = "provider_message_id")
  private String providerMessageId;

  @Column(name = "error_message", columnDefinition = "text")
  private String errorMessage;

  @Column(name = "attempted_at", nullable = false)
  private OffsetDateTime attemptedAt;

  @PrePersist
  public void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (attemptedAt == null) {
      attemptedAt = OffsetDateTime.now();
    }
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public NotificationOutbox getNotificationOutbox() {
    return notificationOutbox;
  }

  public void setNotificationOutbox(NotificationOutbox notificationOutbox) {
    this.notificationOutbox = notificationOutbox;
  }

  public NotificationChannel getChannel() {
    return channel;
  }

  public void setChannel(NotificationChannel channel) {
    this.channel = channel;
  }

  public NotificationStatus getStatus() {
    return status;
  }

  public void setStatus(NotificationStatus status) {
    this.status = status;
  }

  public String getProviderMessageId() {
    return providerMessageId;
  }

  public void setProviderMessageId(String providerMessageId) {
    this.providerMessageId = providerMessageId;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public OffsetDateTime getAttemptedAt() {
    return attemptedAt;
  }

  public void setAttemptedAt(OffsetDateTime attemptedAt) {
    this.attemptedAt = attemptedAt;
  }
}
