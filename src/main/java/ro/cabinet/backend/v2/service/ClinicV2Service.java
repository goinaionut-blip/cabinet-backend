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

  private V2Dtos.ClinicResponse toResponse(Clinic clinic, String role) {
    return new V2Dtos.ClinicResponse(
        clinic.getId(), clinic.getName(), clinic.getSlug(), clinic.getCreatedAt(), clinic.getUpdatedAt(), role);
  }
}
