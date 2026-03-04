package ro.cabinet.backend.v2.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@IdClass(SyncedPatientV2Id.class)
@Table(name = "synced_patients_v2")
public class SyncedPatientV2 {
  @Id
  @Column(name = "clinic_id", nullable = false)
  private UUID clinicId;

  @Id
  @Column(name = "patient_id", nullable = false)
  private String patientId;

  @Column(name = "patient_name", nullable = false)
  private String patientName;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  @PrePersist
  public void onCreate() {
    if (updatedAt == null) {
      updatedAt = OffsetDateTime.now();
    }
  }

  @PreUpdate
  public void onUpdate() {
    updatedAt = OffsetDateTime.now();
  }

  public UUID getClinicId() {
    return clinicId;
  }

  public void setClinicId(UUID clinicId) {
    this.clinicId = clinicId;
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

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
