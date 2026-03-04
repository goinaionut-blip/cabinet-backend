package ro.cabinet.backend.v2.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class ClinicUserId implements Serializable {
  private UUID clinicId;
  private UUID userId;

  public ClinicUserId() {
  }

  public ClinicUserId(UUID clinicId, UUID userId) {
    this.clinicId = clinicId;
    this.userId = userId;
  }

  public UUID getClinicId() {
    return clinicId;
  }

  public UUID getUserId() {
    return userId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ClinicUserId that)) {
      return false;
    }
    return Objects.equals(clinicId, that.clinicId) && Objects.equals(userId, that.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clinicId, userId);
  }
}
