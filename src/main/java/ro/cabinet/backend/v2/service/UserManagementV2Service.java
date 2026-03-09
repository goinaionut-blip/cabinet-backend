package ro.cabinet.backend.v2.service;

import ro.cabinet.backend.v2.dto.V2Dtos;
import ro.cabinet.backend.v2.entity.ClinicUser;
import ro.cabinet.backend.v2.entity.DbUser;
import ro.cabinet.backend.v2.exception.V2NotFoundException;
import ro.cabinet.backend.v2.exception.V2ValidationException;
import ro.cabinet.backend.v2.repo.ClinicUserRepository;
import ro.cabinet.backend.v2.repo.DbUserRepository;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementV2Service {
  private static final Set<String> OWNER_OR_SUPERADMIN = Set.of("OWNER", "SUPERADMIN");
  private static final String ROLE_OWNER = "OWNER";
  private static final String ROLE_SUPERADMIN = "SUPERADMIN";

  private final DbUserRepository dbUserRepository;
  private final ClinicUserRepository clinicUserRepository;
  private final PasswordEncoder passwordEncoder;

  public UserManagementV2Service(DbUserRepository dbUserRepository,
                                 ClinicUserRepository clinicUserRepository,
                                 PasswordEncoder passwordEncoder) {
    this.dbUserRepository = dbUserRepository;
    this.clinicUserRepository = clinicUserRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public V2Dtos.UserSummary createUser(UUID actorUserId, String email, String password, String displayName, Boolean active) {
    requireRole(actorUserId, OWNER_OR_SUPERADMIN);

    String normalizedEmail = normalizeEmail(email);
    if (normalizedEmail == null) {
      throw new V2ValidationException("Email is required");
    }
    if (dbUserRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
      throw new V2ValidationException("Email already exists");
    }
    if (password == null || password.isBlank()) {
      throw new V2ValidationException("Password is required");
    }
    if (password.length() < 8) {
      throw new V2ValidationException("Password must have at least 8 characters");
    }

    DbUser user = new DbUser();
    user.setId(UUID.randomUUID());
    user.setEmail(normalizedEmail);
    user.setPasswordHash(passwordEncoder.encode(password));
    user.setDisplayName(trimToNull(displayName));
    user.setActive(active == null || active);

    DbUser saved = dbUserRepository.save(user);
    return new V2Dtos.UserSummary(saved.getId(), saved.getEmail(), saved.getDisplayName());
  }

  @Transactional
  public void deleteUser(UUID actorUserId, UUID targetUserId) {
    requireRole(actorUserId, Set.of("SUPERADMIN"));
    if (actorUserId.equals(targetUserId)) {
      throw new V2ValidationException("Cannot delete current user");
    }
    if (!dbUserRepository.existsById(targetUserId)) {
      throw new V2NotFoundException("User not found");
    }
    dbUserRepository.deleteById(targetUserId);
  }

  @Transactional
  public void changePassword(UUID actorUserId, UUID targetUserId, String newPassword) {
    if (targetUserId == null) {
      throw new V2ValidationException("User is required");
    }
    if (newPassword == null || newPassword.isBlank()) {
      throw new V2ValidationException("Password is required");
    }
    if (newPassword.length() < 8) {
      throw new V2ValidationException("Password must have at least 8 characters");
    }
    DbUser target = dbUserRepository.findById(targetUserId)
        .orElseThrow(() -> new V2NotFoundException("User not found"));
    requireCanManageTarget(actorUserId, targetUserId);
    target.setPasswordHash(passwordEncoder.encode(newPassword));
    dbUserRepository.save(target);
  }

  private void requireRole(UUID actorUserId, Set<String> allowedRoles) {
    boolean allowed = clinicUserRepository.findAllByUserId(actorUserId).stream()
        .map(ClinicUser::getRole)
        .map(this::normalizeRole)
        .anyMatch(allowedRoles::contains);
    if (!allowed) {
      throw new AccessDeniedException("Forbidden");
    }
  }

  private void requireCanManageTarget(UUID actorUserId, UUID targetUserId) {
    Set<UUID> ownerClinicIds = new LinkedHashSet<>();
    for (ClinicUser membership : clinicUserRepository.findAllByUserId(actorUserId)) {
      String role = normalizeRole(membership.getRole());
      if (ROLE_SUPERADMIN.equals(role)) {
        return;
      }
      if (ROLE_OWNER.equals(role)) {
        ownerClinicIds.add(membership.getClinicId());
      }
    }
    if (ownerClinicIds.isEmpty()) {
      throw new AccessDeniedException("Forbidden");
    }
    boolean hasSharedClinic = clinicUserRepository.findAllByUserId(targetUserId).stream()
        .anyMatch(membership -> ownerClinicIds.contains(membership.getClinicId()));
    if (!hasSharedClinic) {
      throw new AccessDeniedException("Forbidden");
    }
  }

  private String normalizeEmail(String value) {
    String trimmed = trimToNull(value);
    return trimmed == null ? null : trimmed.toLowerCase(Locale.ROOT);
  }

  private String normalizeRole(String value) {
    String trimmed = trimToNull(value);
    return trimmed == null ? "" : trimmed.toUpperCase(Locale.ROOT);
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
