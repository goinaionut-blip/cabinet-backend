package ro.cabinet.backend.v2.service;

import ro.cabinet.backend.v2.dto.V2Dtos;
import ro.cabinet.backend.v2.entity.Clinic;
import ro.cabinet.backend.v2.entity.ClinicUser;
import ro.cabinet.backend.v2.entity.DbUser;
import ro.cabinet.backend.v2.exception.V2NotFoundException;
import ro.cabinet.backend.v2.exception.V2ValidationException;
import ro.cabinet.backend.v2.repo.ClinicRepository;
import ro.cabinet.backend.v2.repo.ClinicUserRepository;
import ro.cabinet.backend.v2.repo.DbUserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClinicV2Service {
  private final ClinicRepository clinicRepository;
  private final ClinicUserRepository clinicUserRepository;
  private final DbUserRepository dbUserRepository;

  public ClinicV2Service(ClinicRepository clinicRepository,
                         ClinicUserRepository clinicUserRepository,
                         DbUserRepository dbUserRepository) {
    this.clinicRepository = clinicRepository;
    this.clinicUserRepository = clinicUserRepository;
    this.dbUserRepository = dbUserRepository;
  }

  @Transactional
  public V2Dtos.ClinicResponse createClinic(UUID userId, String name, String slug) {
    requireSuperAdmin(userId);
    return createClinicAsSuperAdmin(userId, name, slug);
  }

  @Transactional
  public V2Dtos.ClinicResponse updateClinic(UUID clinicId, UUID userId, String name, String slug) {
    requireSuperAdmin(userId);
    Clinic clinic = clinicRepository.findById(clinicId).orElseThrow(() -> new V2NotFoundException("Clinic not found"));
    if (name == null || name.isBlank()) {
      throw new V2ValidationException("Clinic name is required");
    }
    String normalizedSlug = normalizeSlug(slug);
    if (normalizedSlug != null && clinicRepository.existsBySlugIgnoreCaseAndIdNot(normalizedSlug, clinicId)) {
      throw new V2ValidationException("Slug already exists");
    }
    clinic.setName(name.trim());
    clinic.setSlug(normalizedSlug);
    Clinic saved = clinicRepository.save(clinic);
    String role = resolveRoleForClinic(userId, saved.getId());
    return toResponse(saved, role);
  }

  @Transactional
  public void deleteClinic(UUID clinicId, UUID userId) {
    requireSuperAdmin(userId);
    Clinic clinic = clinicRepository.findById(clinicId).orElseThrow(() -> new V2NotFoundException("Clinic not found"));
    clinicRepository.delete(clinic);
  }

  @Transactional
  private V2Dtos.ClinicResponse createClinicAsSuperAdmin(UUID userId, String name, String slug) {
    DbUser user = dbUserRepository.findById(userId).orElseThrow(() -> new V2NotFoundException("User not found"));
    if (!user.isActive()) {
      throw new V2ValidationException("User inactive");
    }
    if (name == null || name.isBlank()) {
      throw new V2ValidationException("Clinic name is required");
    }
    String normalizedSlug = normalizeSlug(slug);
    if (normalizedSlug != null && clinicRepository.existsBySlugIgnoreCase(normalizedSlug)) {
      throw new V2ValidationException("Slug already exists");
    }

    Clinic clinic = new Clinic();
    clinic.setId(UUID.randomUUID());
    clinic.setName(name.trim());
    clinic.setSlug(normalizedSlug);
    Clinic saved = clinicRepository.save(clinic);

    ClinicUser membership = new ClinicUser();
    membership.setClinicId(saved.getId());
    membership.setUserId(userId);
    membership.setRole("OWNER");
    clinicUserRepository.save(membership);

    return toResponse(saved, "OWNER");
  }

  @Transactional(readOnly = true)
  public List<V2Dtos.ClinicResponse> listClinics(UUID userId) {
    List<ClinicUser> memberships = clinicUserRepository.findAllByUserId(userId);
    Set<UUID> clinicIds = memberships.stream().map(ClinicUser::getClinicId).collect(java.util.stream.Collectors.toSet());
    Map<UUID, Clinic> clinicsById = new HashMap<>();
    for (Clinic clinic : clinicRepository.findAllById(clinicIds)) {
      clinicsById.put(clinic.getId(), clinic);
    }

    return memberships.stream()
        .map(membership -> {
          Clinic clinic = clinicsById.get(membership.getClinicId());
          if (clinic == null) {
            return null;
          }
          return toResponse(clinic, membership.getRole());
        })
        .filter(java.util.Objects::nonNull)
        .toList();
  }

  private String normalizeSlug(String slug) {
    if (slug == null) {
      return null;
    }
    String trimmed = slug.trim();
    return trimmed.isBlank() ? null : trimmed;
  }

  private void requireSuperAdmin(UUID userId) {
    boolean isSuperAdmin = clinicUserRepository.findAllByUserId(userId).stream()
        .map(ClinicUser::getRole)
        .map(this::normalizeRole)
        .anyMatch("SUPERADMIN"::equals);
    if (!isSuperAdmin) {
      throw new org.springframework.security.access.AccessDeniedException("Forbidden");
    }
  }

  private String resolveRoleForClinic(UUID userId, UUID clinicId) {
    return clinicUserRepository.findByClinicIdAndUserId(clinicId, userId)
        .map(ClinicUser::getRole)
        .map(this::normalizeRole)
        .orElse("SUPERADMIN");
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

  private V2Dtos.ClinicResponse toResponse(Clinic clinic, String role) {
    return new V2Dtos.ClinicResponse(
        clinic.getId(), clinic.getName(), clinic.getSlug(), clinic.getCreatedAt(), clinic.getUpdatedAt(), role);
  }
}
