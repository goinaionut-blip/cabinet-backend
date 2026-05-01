package ro.cabinet.backend.notifications.dto;

import ro.cabinet.backend.notifications.NotificationPreference;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ReminderDispatchRequest {
  @NotNull
  private UUID clinicId;
  private UUID doctorId;
  private String patientId;
  @NotBlank
  private String patientName;
  @NotBlank
  private String phoneE164;
  private String appointmentExternalId;
  private OffsetDateTime appointmentDateTime;
  @NotBlank
  private String messageText;
  private NotificationPreference preference;
  private Boolean fallbackToSms;

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

  public NotificationPreference getPreference() {
    return preference;
  }

  public void setPreference(NotificationPreference preference) {
    this.preference = preference;
  }

  public Boolean getFallbackToSms() {
    return fallbackToSms;
  }

  public void setFallbackToSms(Boolean fallbackToSms) {
    this.fallbackToSms = fallbackToSms;
  }
}
