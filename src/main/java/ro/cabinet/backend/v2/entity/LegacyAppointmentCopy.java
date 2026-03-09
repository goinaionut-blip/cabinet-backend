package ro.cabinet.backend.v2.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "legacy_appointment_copies")
public class LegacyAppointmentCopy {
  @Id
  private UUID id;

  @Column(name = "clinic_id", nullable = false)
  private UUID clinicId;

  @Column(name = "doctor_id", nullable = false)
  private UUID doctorId;

  @Column(name = "legacy_appointment_id", nullable = false)
  private Long legacyAppointmentId;

  @Column(name = "appointment_v2_id", nullable = false)
  private UUID appointmentV2Id;

  @Column(name = "copied_at", nullable = false)
  private OffsetDateTime copiedAt;

  @PrePersist
  public void onCreate() {
    if (copiedAt == null) {
      copiedAt = OffsetDateTime.now();
    }
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

  public Long getLegacyAppointmentId() {
    return legacyAppointmentId;
  }

  public void setLegacyAppointmentId(Long legacyAppointmentId) {
    this.legacyAppointmentId = legacyAppointmentId;
  }

  public UUID getAppointmentV2Id() {
    return appointmentV2Id;
  }

  public void setAppointmentV2Id(UUID appointmentV2Id) {
    this.appointmentV2Id = appointmentV2Id;
  }

  public OffsetDateTime getCopiedAt() {
    return copiedAt;
  }

  public void setCopiedAt(OffsetDateTime copiedAt) {
    this.copiedAt = copiedAt;
  }
}
