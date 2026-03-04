package ro.cabinet.backend.v2.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class SyncedPatientV2Id implements Serializable {
  private UUID clinicId;
  private String patientId;

  public SyncedPatientV2Id() {
  }

  public SyncedPatientV2Id(UUID clinicId, String patientId) {
    this.clinicId = clinicId;
    this.patientId = patientId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SyncedPatientV2Id that)) {
      return false;
    }
    return Objects.equals(clinicId, that.clinicId) && Objects.equals(patientId, that.patientId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clinicId, patientId);
  }
}
