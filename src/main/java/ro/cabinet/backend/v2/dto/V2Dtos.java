package ro.cabinet.backend.v2.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class V2Dtos {
  public record UserSummary(UUID id, String email, String displayName) {
  }

  public record ClinicMembershipSummary(UUID clinicId, String clinicName, String clinicSlug, String role) {
  }

  public record AuthLoginResponse(String token, OffsetDateTime expiresAt, UserSummary user) {
  }

  public record AuthMeResponse(UserSummary user, List<ClinicMembershipSummary> clinics) {
  }

  public record ClinicResponse(UUID id, String name, String slug, OffsetDateTime createdAt,
                               OffsetDateTime updatedAt, String role) {
  }
}
