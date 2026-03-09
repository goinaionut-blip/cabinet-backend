package ro.cabinet.backend.v2.controller;

import ro.cabinet.backend.v2.entity.ClinicUser;
import ro.cabinet.backend.v2.service.ClinicMembershipV2Service;
import ro.cabinet.backend.v2.service.CurrentUserService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v2/clinics/{clinicId}/users")
public class ClinicMembershipV2Controller {
  private final ClinicMembershipV2Service clinicMembershipV2Service;
  private final CurrentUserService currentUserService;

  public ClinicMembershipV2Controller(ClinicMembershipV2Service clinicMembershipV2Service,
                                      CurrentUserService currentUserService) {
    this.clinicMembershipV2Service = clinicMembershipV2Service;
    this.currentUserService = currentUserService;
  }

  @PostMapping
  public MembershipResponse associate(@PathVariable UUID clinicId,
                                      @Valid @RequestBody AssociateUserRequest request) {
    ClinicUser membership = clinicMembershipV2Service.associateUser(
        clinicId,
        currentUserService.requireCurrentUserId(),
        request.userId(),
        request.role());
    return new MembershipResponse(membership.getClinicId(), membership.getUserId(), membership.getRole());
  }

  @GetMapping
  public List<MembershipDetailsResponse> list(@PathVariable UUID clinicId) {
    return clinicMembershipV2Service.listMembers(
            clinicId,
            currentUserService.requireCurrentUserId())
        .stream()
        .map(member -> new MembershipDetailsResponse(
            member.clinicId(),
            member.userId(),
            member.role(),
            member.email(),
            member.displayName(),
            member.active(),
            member.joinedAt()))
        .toList();
  }

  public record AssociateUserRequest(@NotNull UUID userId, @NotBlank String role) {
  }

  public record MembershipResponse(UUID clinicId, UUID userId, String role) {
  }

  public record MembershipDetailsResponse(UUID clinicId,
                                          UUID userId,
                                          String role,
                                          String email,
                                          String displayName,
                                          Boolean active,
                                          OffsetDateTime joinedAt) {
  }
}
