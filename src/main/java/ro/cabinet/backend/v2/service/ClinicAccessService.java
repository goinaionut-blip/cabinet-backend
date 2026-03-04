package ro.cabinet.backend.v2.service;

import ro.cabinet.backend.v2.entity.ClinicUser;
import ro.cabinet.backend.v2.repo.ClinicUserRepository;

import java.util.Set;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class ClinicAccessService {
  private final ClinicUserRepository clinicUserRepository;

  public ClinicAccessService(ClinicUserRepository clinicUserRepository) {
    this.clinicUserRepository = clinicUserRepository;
  }

  public void requireClinicMembership(UUID clinicId, UUID userId) {
    if (!clinicUserRepository.existsByClinicIdAndUserId(clinicId, userId)) {
      throw new AccessDeniedException("Forbidden");
    }
  }

  public void requireRole(UUID clinicId, UUID userId, Set<String> allowedRoles) {
    ClinicUser membership = clinicUserRepository.findByClinicIdAndUserId(clinicId, userId)
        .orElseThrow(() -> new AccessDeniedException("Forbidden"));
    String role = membership.getRole() == null ? "" : membership.getRole().trim().toUpperCase();
    if (!allowedRoles.contains(role)) {
      throw new AccessDeniedException("Forbidden");
    }
  }
}
