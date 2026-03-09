package ro.cabinet.backend.v2.service;

import ro.cabinet.backend.config.JwtProperties;
import ro.cabinet.backend.auth.JwtService;
import ro.cabinet.backend.v2.dto.V2Dtos;
import ro.cabinet.backend.v2.entity.Clinic;
import ro.cabinet.backend.v2.entity.ClinicUser;
import ro.cabinet.backend.v2.entity.DbUser;
import ro.cabinet.backend.v2.exception.V2NotFoundException;
import ro.cabinet.backend.v2.repo.ClinicRepository;
import ro.cabinet.backend.v2.repo.ClinicUserRepository;
import ro.cabinet.backend.v2.repo.DbUserRepository;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class V2AuthService {
  private final DbUserRepository dbUserRepository;
  private final ClinicUserRepository clinicUserRepository;
  private final ClinicRepository clinicRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final JwtProperties jwtProperties;

  public V2AuthService(DbUserRepository dbUserRepository,
                       ClinicUserRepository clinicUserRepository,
                       ClinicRepository clinicRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       JwtProperties jwtProperties) {
    this.dbUserRepository = dbUserRepository;
    this.clinicUserRepository = clinicUserRepository;
    this.clinicRepository = clinicRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.jwtProperties = jwtProperties;
  }

  public V2Dtos.AuthLoginResponse login(String email, String password) {
    DbUser user = dbUserRepository.findByEmailIgnoreCase(email)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
    if (!user.isActive()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User inactive");
    }
    if (!passwordEncoder.matches(password, user.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    String token = jwtService.generateTokenV2(user.getEmail(), user.getId(), Map.of());
    OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(jwtProperties.getExpirationMinutes());
    V2Dtos.UserSummary userSummary = new V2Dtos.UserSummary(user.getId(), user.getEmail(), user.getDisplayName());
    return new V2Dtos.AuthLoginResponse(token, expiresAt, userSummary);
  }

  @Transactional(readOnly = true)
  public V2Dtos.AuthMeResponse me(UUID userId) {
    DbUser user = dbUserRepository.findById(userId)
        .orElseThrow(() -> new V2NotFoundException("User not found"));

    List<ClinicUser> memberships = clinicUserRepository.findAllByUserId(userId);
    boolean superAdmin = memberships.stream()
        .map(ClinicUser::getRole)
        .map(this::normalizeRole)
        .anyMatch("SUPERADMIN"::equals);
    Map<UUID, Clinic> clinicsById = new HashMap<>();
    if (superAdmin) {
      for (Clinic clinic : clinicRepository.findAll()) {
        clinicsById.put(clinic.getId(), clinic);
      }
    } else {
      Set<UUID> clinicIds = memberships.stream().map(ClinicUser::getClinicId)
          .collect(java.util.stream.Collectors.toSet());
      for (Clinic clinic : clinicRepository.findAllById(clinicIds)) {
        clinicsById.put(clinic.getId(), clinic);
      }
    }

    List<V2Dtos.ClinicMembershipSummary> clinics;
    if (superAdmin) {
      clinics = clinicsById.values().stream()
          .map(clinic -> new V2Dtos.ClinicMembershipSummary(
              clinic.getId(), clinic.getName(), clinic.getSlug(), "SUPERADMIN"))
          .toList();
    } else {
      clinics = memberships.stream()
          .map(membership -> {
            Clinic clinic = clinicsById.get(membership.getClinicId());
            if (clinic == null) {
              return null;
            }
            return new V2Dtos.ClinicMembershipSummary(
                clinic.getId(), clinic.getName(), clinic.getSlug(), membership.getRole());
          })
          .filter(java.util.Objects::nonNull)
          .toList();
    }

    V2Dtos.UserSummary userSummary = new V2Dtos.UserSummary(user.getId(), user.getEmail(), user.getDisplayName());
    return new V2Dtos.AuthMeResponse(userSummary, clinics);
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
}
