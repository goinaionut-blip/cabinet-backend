package ro.cabinet.backend.v2.service;

import ro.cabinet.backend.appointments.Appointment;
import ro.cabinet.backend.appointments.AppointmentRepository;
import ro.cabinet.backend.v2.entity.AppointmentV2;
import ro.cabinet.backend.v2.entity.ClinicUser;
import ro.cabinet.backend.v2.entity.LegacyAppointmentCopy;
import ro.cabinet.backend.v2.exception.V2ValidationException;
import ro.cabinet.backend.v2.repo.AppointmentV2Repository;
import ro.cabinet.backend.v2.repo.ClinicUserRepository;
import ro.cabinet.backend.v2.repo.DoctorRepository;
import ro.cabinet.backend.v2.repo.LegacyAppointmentCopyRepository;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LegacyAppointmentCopyService {
  private static final ZoneId LEGACY_ZONE = ZoneId.of("Europe/Bucharest");
  private static final Set<String> ADMIN_ROLES = Set.of("OWNER", "ADMIN", "SUPERADMIN");

  private final AppointmentRepository appointmentRepository;
  private final AppointmentV2Repository appointmentV2Repository;
  private final LegacyAppointmentCopyRepository legacyAppointmentCopyRepository;
  private final DoctorRepository doctorRepository;
  private final ClinicAccessService clinicAccessService;
  private final ClinicUserRepository clinicUserRepository;

  public LegacyAppointmentCopyService(AppointmentRepository appointmentRepository,
                                      AppointmentV2Repository appointmentV2Repository,
                                      LegacyAppointmentCopyRepository legacyAppointmentCopyRepository,
                                      DoctorRepository doctorRepository,
                                      ClinicAccessService clinicAccessService,
                                      ClinicUserRepository clinicUserRepository) {
    this.appointmentRepository = appointmentRepository;
    this.appointmentV2Repository = appointmentV2Repository;
    this.legacyAppointmentCopyRepository = legacyAppointmentCopyRepository;
    this.doctorRepository = doctorRepository;
    this.clinicAccessService = clinicAccessService;
    this.clinicUserRepository = clinicUserRepository;
  }

  @Transactional(readOnly = true)
  public CopyStatus status(UUID clinicId, UUID doctorId, UUID userId) {
    requireClinicRole(clinicId, userId, ADMIN_ROLES);
    validateDoctor(clinicId, doctorId);

    long legacyTotal = appointmentRepository.count();
    long copiedCount = legacyAppointmentCopyRepository.countByClinicIdAndDoctorId(clinicId, doctorId);
    OffsetDateTime lastCopiedAt = legacyAppointmentCopyRepository
        .findFirstByClinicIdAndDoctorIdOrderByCopiedAtDesc(clinicId, doctorId)
        .map(LegacyAppointmentCopy::getCopiedAt)
        .orElse(null);
    long pendingCount = Math.max(0L, legacyTotal - copiedCount);
    return new CopyStatus(legacyTotal, copiedCount, pendingCount, copiedCount > 0, lastCopiedAt);
  }

  @Transactional
  public CopyResult copy(UUID clinicId, UUID doctorId, UUID userId) {
    requireClinicRole(clinicId, userId, ADMIN_ROLES);
    validateDoctor(clinicId, doctorId);

    List<Appointment> legacyAppointments = appointmentRepository.findAllByOrderByStartTimeAsc();
    int copiedNow = 0;
    int skippedAlreadyCopied = 0;
    for (Appointment legacy : legacyAppointments) {
      if (legacy == null || legacy.getId() == null) {
        continue;
      }
      if (legacyAppointmentCopyRepository.existsByClinicIdAndDoctorIdAndLegacyAppointmentId(
          clinicId, doctorId, legacy.getId())) {
        skippedAlreadyCopied++;
        continue;
      }

      AppointmentV2 target = new AppointmentV2();
      target.setId(UUID.randomUUID());
      target.setClinicId(clinicId);
      target.setDoctorId(doctorId);
      target.setPatientId(legacy.getPatientId() == null ? null : String.valueOf(legacy.getPatientId()));
      target.setPatientName(legacy.getPatientName());
      target.setStartTime(legacy.getStartTime().atZone(LEGACY_ZONE).toOffsetDateTime());
      target.setEndTime(legacy.getEndTime().atZone(LEGACY_ZONE).toOffsetDateTime());
      target.setNote(legacy.getNote());
      target.setStatus("SCHEDULED");
      appointmentV2Repository.save(target);

      LegacyAppointmentCopy copy = new LegacyAppointmentCopy();
      copy.setId(UUID.randomUUID());
      copy.setClinicId(clinicId);
      copy.setDoctorId(doctorId);
      copy.setLegacyAppointmentId(legacy.getId());
      copy.setAppointmentV2Id(target.getId());
      legacyAppointmentCopyRepository.save(copy);
      copiedNow++;
    }

    CopyStatus status = status(clinicId, doctorId, userId);
    return new CopyResult(
        legacyAppointments.size(),
        copiedNow,
        skippedAlreadyCopied,
        status.legacyTotal(),
        status.copiedCount(),
        status.pendingCount(),
        status.alreadyCopied(),
        status.lastCopiedAt()
    );
  }

  private void validateDoctor(UUID clinicId, UUID doctorId) {
    if (doctorId == null) {
      throw new V2ValidationException("doctorId is required");
    }
    if (!doctorRepository.existsByIdAndClinicIdAndActiveTrue(doctorId, clinicId)) {
      throw new V2ValidationException("Doctor not found in clinic");
    }
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
