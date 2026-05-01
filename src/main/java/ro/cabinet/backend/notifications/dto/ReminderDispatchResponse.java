package ro.cabinet.backend.notifications.dto;

import ro.cabinet.backend.notifications.NotificationChannel;
import ro.cabinet.backend.notifications.NotificationStatus;

import java.util.UUID;

public class ReminderDispatchResponse {
  private UUID notificationId;
  private NotificationStatus status;
  private NotificationChannel channelUsed;
  private boolean fallbackUsed;
  private String providerMessageId;
  private String errorMessage;

  public UUID getNotificationId() {
    return notificationId;
  }

  public void setNotificationId(UUID notificationId) {
    this.notificationId = notificationId;
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

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
