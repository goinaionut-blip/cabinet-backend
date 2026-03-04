package ro.cabinet.backend.v2.service;

import ro.cabinet.backend.v2.entity.Doctor;
import ro.cabinet.backend.v2.exception.V2NotFoundException;
import ro.cabinet.backend.v2.exception.V2ValidationException;
import ro.cabinet.backend.v2.repo.DoctorRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoctorV2Service {
  private static final Set<String> OWNER_ADMIN = Set.of("OWNER", "ADMIN");

  private final DoctorRepository doctorRepository;
  private final ClinicAccessService clinicAccessService;

  public DoctorV2Service(DoctorRepository doctorRepository, ClinicAccessService clinicAccessService) {
    this.doctorRepository = doctorRepository;
    this.clinicAccessService = clinicAccessService;
  }

  @Transactional(readOnly = true)
  public List<Doctor> list(UUID clinicId, UUID userId) {
    clinicAccessService.requireClinicMembership(clinicId, userId);
    return doctorRepository.findByClinicIdAndActiveTrueOrderByDisplayNameAsc(clinicId);
  }

  @Transactional
  public Doctor create(UUID clinicId, UUID userId, String displayName, String externalCode) {
    clinicAccessService.requireRole(clinicId, userId, OWNER_ADMIN);
    if (displayName == null || displayName.isBlank()) {
      throw new V2ValidationException("displayName is required");
    }

    Doctor doctor = new Doctor();
    doctor.setId(UUID.randomUUID());
    doctor.setClinicId(clinicId);
    doctor.setDisplayName(displayName.trim());
    doctor.setExternalCode(externalCode);
    doctor.setActive(true);
    return doctorRepository.save(doctor);
  }

  @Transactional
  public Doctor update(UUID clinicId, UUID doctorId, UUID userId, String displayName, String externalCode) {
    clinicAccessService.requireRole(clinicId, userId, OWNER_ADMIN);
    Doctor doctor = doctorRepository.findByIdAndClinicId(doctorId, clinicId)
        .orElseThrow(() -> new V2NotFoundException("Doctor not found"));

    if (displayName == null || displayName.isBlank()) {
      throw new V2ValidationException("displayName is required");
    }
    doctor.setDisplayName(displayName.trim());
    doctor.setExternalCode(externalCode);
    return doctorRepository.save(doctor);
  }

  @Transactional
  public void softDelete(UUID clinicId, UUID doctorId, UUID userId) {
    clinicAccessService.requireRole(clinicId, userId, OWNER_ADMIN);
    Doctor doctor = doctorRepository.findByIdAndClinicId(doctorId, clinicId)
        .orElseThrow(() -> new V2NotFoundException("Doctor not found"));
    doctor.setActive(false);
    doctorRepository.save(doctor);
  }
}
