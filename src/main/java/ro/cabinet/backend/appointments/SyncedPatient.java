package ro.cabinet.backend.appointments;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "synced_patients")
public class SyncedPatient {
  @Id
  @Column(name = "patient_id", nullable = false)
  private Long patientId;

  @NotNull
  @Column(name = "patient_name", nullable = false)
  private String patientName;

  public Long getPatientId() {
    return patientId;
  }

  public void setPatientId(Long patientId) {
    this.patientId = patientId;
  }

  public String getPatientName() {
    return patientName;
  }

  public void setPatientName(String patientName) {
    this.patientName = patientName;
  }
}
