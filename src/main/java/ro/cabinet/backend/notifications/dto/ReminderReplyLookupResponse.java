package ro.cabinet.backend.notifications.dto;

import ro.cabinet.backend.notifications.NotificationReplyStatus;

import java.time.OffsetDateTime;

public class ReminderReplyLookupResponse {
  private String patientId;
  private String appointmentExternalId;
  private String reminderTypeCode;
  private NotificationReplyStatus replyStatus;
  private OffsetDateTime replyReceivedAt;

  public String getPatientId() {
    return patientId;
  }

  public void setPatientId(String patientId) {
    this.patientId = patientId;
  }

  public String getAppointmentExternalId() {
    return appointmentExternalId;
  }

  public void setAppointmentExternalId(String appointmentExternalId) {
    this.appointmentExternalId = appointmentExternalId;
  }

  public String getReminderTypeCode() {
    return reminderTypeCode;
  }

  public void setReminderTypeCode(String reminderTypeCode) {
    this.reminderTypeCode = reminderTypeCode;
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
}
