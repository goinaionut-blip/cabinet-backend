package ro.cabinet.backend.v2.service;

import ro.cabinet.backend.appointments.SyncedPatient;
import ro.cabinet.backend.appointments.SyncedPatientRepository;
import ro.cabinet.backend.v2.entity.ClinicUser;
import ro.cabinet.backend.v2.entity.LegacySyncedPatientCopy;
import ro.cabinet.backend.v2.entity.SyncedPatientV2;
import ro.cabinet.backend.v2.repo.ClinicUserRepository;
import ro.cabinet.backend.v2.repo.LegacySyncedPatientCopyRepository;
import ro.cabinet.backend.v2.repo.SyncedPatientV2Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LegacySyncedPatientCopyService {
  private static final Set<String> ADMIN_ROLES = Set.of("OWNER", "ADMIN", "SUPERADMIN");

  private final SyncedPatientRepository syncedPatientRepository;
  private final SyncedPatientV2Repository syncedPatientV2Repository;
  private final LegacySyncedPatientCopyRepository legacySyncedPatientCopyRepository;
  private final ClinicAccessService clinicAccessService;
  private final ClinicUserRepository clinicUserRepository;

  public LegacySyncedPatientCopyService(SyncedPatientRepository syncedPatientRepository,
                                        SyncedPatientV2Repository syncedPatientV2Repository,
                                        LegacySyncedPatientCopyRepository legacySyncedPatientCopyRepository,
                                        ClinicAccessService clinicAccessService,
                                        ClinicUserRepository clinicUserRepository) {
    this.syncedPatientRepository = syncedPatientRepository;
    this.syncedPatientV2Repository = syncedPatientV2Repository;
    this.legacySyncedPatientCopyRepository = legacySyncedPatientCopyRepository;
    this.clinicAccessService = clinicAccessService;
    this.clinicUserRepository = clinicUserRepository;
  }

  @Transactional(readOnly = true)
  public CopyStatus status(UUID clinicId, UUID userId) {
    requireClinicRole(clinicId, userId, ADMIN_ROLES);
    long legacyTotal = syncedPatientRepository.count();
    long copiedCount = legacySyncedPatientCopyRepository.countByClinicId(clinicId);
    long pendingCount = Math.max(0L, legacyTotal - copiedCount);
    OffsetDateTime lastCopiedAt = legacySyncedPatientCopyRepository.findFirstByClinicIdOrderByCopiedAtDesc(clinicId)
        .map(LegacySyncedPatientCopy::getCopiedAt)
        .orElse(null);
    return new CopyStatus(legacyTotal, copiedCount, pendingCount, copiedCount > 0, lastCopiedAt);
  }

  @Transactional
  public CopyResult copy(UUID clinicId, UUID userId) {
    requireClinicRole(clinicId, userId, ADMIN_ROLES);
    List<SyncedPatient> legacyPatients = syncedPatientRepository.findAll();
    int copiedNow = 0;
    int skippedAlreadyCopied = 0;
    for (SyncedPatient legacy : legacyPatients) {
      if (legacy == null || legacy.getPatientId() == null) {
        continue;
      }
      if (legacySyncedPatientCopyRepository.existsByClinicIdAndLegacyPatientId(clinicId, legacy.getPatientId())) {
        skippedAlreadyCopied++;
        continue;
      }

      SyncedPatientV2 target = new SyncedPatientV2();
      target.setClinicId(clinicId);
      target.setPatientId(String.valueOf(legacy.getPatientId()));
      target.setPatientName(legacy.getPatientName());
      syncedPatientV2Repository.save(target);

      LegacySyncedPatientCopy copy = new LegacySyncedPatientCopy();
      copy.setId(UUID.randomUUID());
      copy.setClinicId(clinicId);
      copy.setLegacyPatientId(legacy.getPatientId());
      legacySyncedPatientCopyRepository.save(copy);
      copiedNow++;
    }

    CopyStatus status = status(clinicId, userId);
    return new CopyResult(
        legacyPatients.size(),
        copiedNow,
        skippedAlreadyCopied,
        status.legacyTotal(),
        status.copiedCount(),
        status.pendingCount(),
        status.alreadyCopied(),
        status.lastCopiedAt()
    );
  }

  private void requireClinicRole(UUID clinicId, UUID userId, Set<String> allowedRoles) {
    if (isSuperAdmin(userId)) {
      return;
    }
    clinicAccessService.requireRole(clinicId, userId, allowedRoles);
  }

  private boolean isSuperAdmin(UUID userId) {
    return clinicUserRepository.findAllByUserId(userId).stream()
        .map(ClinicUser::getRole)
        .filter(Objects::nonNull)
        .map(String::trim)
        .map(String::toUpperCase)
        .anyMatch("SUPERADMIN"::equals);
  }

  public record CopyStatus(long legacyTotal,
                           long copiedCount,
                           long pendingCount,
                           boolean alreadyCopied,
                           OffsetDateTime lastCopiedAt) {
  }

  public record CopyResult(int scannedCount,
                           int copiedNow,
                           int skippedAlreadyCopied,
                           long legacyTotal,
                           long copiedCount,
                           long pendingCount,
                           boolean alreadyCopied,
                           OffsetDateTime lastCopiedAt) {
  }
}
