package ro.cabinet.backend.notifications.dto;

public class ReminderReplyLookupItem {
  private String patientId;
  private String appointmentExternalId;
  private String reminderTypeCode;

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
}
