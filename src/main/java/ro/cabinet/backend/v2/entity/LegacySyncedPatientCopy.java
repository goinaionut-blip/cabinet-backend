package ro.cabinet.backend.v2.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "legacy_synced_patient_copies")
public class LegacySyncedPatientCopy {
  @Id
  private UUID id;

  @Column(name = "clinic_id", nullable = false)
  private UUID clinicId;

  @Column(name = "legacy_patient_id", nullable = false)
  private Long legacyPatientId;

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

  public Long getLegacyPatientId() {
    return legacyPatientId;
  }

  public void setLegacyPatientId(Long legacyPatientId) {
    this.legacyPatientId = legacyPatientId;
  }

  public OffsetDateTime getCopiedAt() {
    return copiedAt;
  }

  public void setCopiedAt(OffsetDateTime copiedAt) {
    this.copiedAt = copiedAt;
  }
}
