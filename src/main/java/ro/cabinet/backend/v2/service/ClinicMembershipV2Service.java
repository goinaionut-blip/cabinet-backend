package ro.cabinet.backend.v2.service;

import ro.cabinet.backend.v2.entity.ClinicUser;
import ro.cabinet.backend.v2.entity.DbUser;
import ro.cabinet.backend.v2.exception.V2NotFoundException;
import ro.cabinet.backend.v2.exception.V2ValidationException;
import ro.cabinet.backend.v2.repo.ClinicRepository;
import ro.cabinet.backend.v2.repo.ClinicUserRepository;
import ro.cabinet.backend.v2.repo.DbUserRepository;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClinicMembershipV2Service {
  private static final Set<String> OWNER_ONLY = Set.of("OWNER");
  private static final Set<String> OWNER_ASSIGNABLE = Set.of("ADMIN", "MEMBER");
  private static final Set<String> SUPERADMIN_ASSIGNABLE = Set.of("SUPERADMIN", "OWNER", "ADMIN", "MEMBER");

  private final ClinicRepository clinicRepository;
  private final DbUserRepository dbUserRepository;
  private final ClinicUserRepository clinicUserRepository;
  private final ClinicAccessService clinicAccessService;

  public ClinicMembershipV2Service(ClinicRepository clinicRepository,
                                   DbUserRepository dbUserRepository,
                                   ClinicUserRepository clinicUserRepository,
                                   ClinicAccessService clinicAccessService) {
    this.clinicRepository = clinicRepository;
    this.dbUserRepository = dbUserRepository;
    this.clinicUserRepository = clinicUserRepository;
    this.clinicAccessService = clinicAccessService;
  }

  @Transactional
  public ClinicUser associateUser(UUID clinicId, UUID actorUserId, UUID targetUserId, String role) {
    if (!clinicRepository.existsById(clinicId)) {
      throw new V2NotFoundException("Clinic not found");
    }
    ActorRole actorRole = resolveActorRole(actorUserId, clinicId);
    if (actorRole == ActorRole.NONE) {
      throw new org.springframework.security.access.AccessDeniedException("Forbidden");
    }

    if (!dbUserRepository.existsById(targetUserId)) {
      throw new V2NotFoundException("User not found");
    }

    String normalizedRole = normalizeRole(role);
    if (actorRole == ActorRole.OWNER && !OWNER_ASSIGNABLE.contains(normalizedRole)) {
      throw new V2ValidationException("OWNER can assign only ADMIN or STANDARD");
    }
    if (actorRole == ActorRole.SUPERADMIN && !SUPERADMIN_ASSIGNABLE.contains(normalizedRole)) {
      throw new V2ValidationException("Role must be one of: SUPERADMIN, OWNER, ADMIN, STANDARD");
    }

    ClinicUser membership = clinicUserRepository.findByClinicIdAndUserId(clinicId, targetUserId)
        .orElseGet(() -> {
          ClinicUser created = new ClinicUser();
          created.setClinicId(clinicId);
          created.setUserId(targetUserId);
          return created;
        });
    membership.setRole(normalizedRole);
    return clinicUserRepository.save(membership);
  }

  @Transactional(readOnly = true)
  public java.util.List<ClinicMemberView> listMembers(UUID clinicId, UUID actorUserId) {
    if (!clinicRepository.existsById(clinicId)) {
      throw new V2NotFoundException("Clinic not found");
    }
    boolean isSuperAdmin = clinicUserRepository.findAllByUserId(actorUserId).stream()
        .map(ClinicUser::getRole)
        .map(this::normalizeRole)
        .anyMatch("SUPERADMIN"::equals);
    if (!isSuperAdmin) {
      clinicAccessService.requireClinicMembership(clinicId, actorUserId);
    }

    java.util.List<ClinicUser> memberships = clinicUserRepository.findAllByClinicIdOrderByCreatedAtAsc(clinicId);
    Map<UUID, DbUser> usersById = dbUserRepository.findAllById(
        memberships.stream().map(ClinicUser::getUserId).toList())
        .stream()
        .collect(java.util.stream.Collectors.toMap(DbUser::getId, u -> u));

    return memberships.stream()
        .map(membership -> {
          DbUser user = usersById.get(membership.getUserId());
          String email = user == null ? null : user.getEmail();
          String displayName = user == null ? null : user.getDisplayName();
          Boolean active = user == null ? null : user.isActive();
          return new ClinicMemberView(
              membership.getClinicId(),
              membership.getUserId(),
              membership.getRole(),
              email,
              displayName,
              active,
              membership.getCreatedAt());
        })
        .toList();
  }

  private String normalizeRole(String value) {
    if (value == null) {
      return "";
    }
    String normalized = value.trim().toUpperCase(Locale.ROOT);
    if ("STANDARD".equals(normalized)) {
      return "MEMBER";
    }
    return normalized;
  }

  private ActorRole resolveActorRole(UUID actorUserId, UUID clinicId) {
    boolean isSuperAdmin = clinicUserRepository.findAllByUserId(actorUserId).stream()
        .map(ClinicUser::getRole)
        .map(this::normalizeRole)
        .anyMatch("SUPERADMIN"::equals);
    if (isSuperAdmin) {
      return ActorRole.SUPERADMIN;
    }
    try {
      clinicAccessService.requireRole(clinicId, actorUserId, OWNER_ONLY);
      return ActorRole.OWNER;
    } catch (org.springframework.security.access.AccessDeniedException ignored) {
      return ActorRole.NONE;
    }
  }

  public record ClinicMemberView(UUID clinicId,
                                 UUID userId,
                                 String role,
                                 String email,
                                 String displayName,
                                 Boolean active,
                                 java.time.OffsetDateTime joinedAt) {
  }

  private enum ActorRole {
    SUPERADMIN,
    OWNER,
    NONE
  }
}
