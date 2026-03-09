package ro.cabinet.backend.v2.service;

import ro.cabinet.backend.v2.entity.Doctor;
import ro.cabinet.backend.v2.entity.DbUser;
import ro.cabinet.backend.v2.exception.V2NotFoundException;
import ro.cabinet.backend.v2.exception.V2ValidationException;
import ro.cabinet.backend.v2.repo.ClinicUserRepository;
import ro.cabinet.backend.v2.repo.DbUserRepository;
import ro.cabinet.backend.v2.repo.DoctorRepository;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DoctorV2Service {
  private static final Set<String> OWNER_ADMIN_SUPERADMIN = Set.of("OWNER", "ADMIN", "SUPERADMIN");

  private final DoctorRepository doctorRepository;
  private final ClinicAccessService clinicAccessService;
  private final DbUserRepository dbUserRepository;
  private final ClinicUserRepository clinicUserRepository;

  public DoctorV2Service(DoctorRepository doctorRepository,
                         ClinicAccessService clinicAccessService,
                         DbUserRepository dbUserRepository,
                         ClinicUserRepository clinicUserRepository) {
    this.doctorRepository = doctorRepository;
    this.clinicAccessService = clinicAccessService;
    this.dbUserRepository = dbUserRepository;
    this.clinicUserRepository = clinicUserRepository;
  }

  @Transactional(readOnly = true)
  public List<Doctor> list(UUID clinicId, UUID userId) {
    clinicAccessService.requireClinicMembership(clinicId, userId);
    List<Doctor> doctors = doctorRepository.findByClinicIdAndActiveTrueOrderByDisplayNameAsc(clinicId);
    return enrichAssignedUsers(doctors);
  }

  @Transactional
  public Doctor create(UUID clinicId, UUID userId, String displayName, String externalCode, UUID assignedUserId) {
    clinicAccessService.requireRole(clinicId, userId, OWNER_ADMIN_SUPERADMIN);
    if (displayName == null || displayName.isBlank()) {
      throw new V2ValidationException("displayName is required");
    }
    validateAssignedUser(clinicId, assignedUserId);

    Doctor doctor = new Doctor();
    doctor.setId(UUID.randomUUID());
    doctor.setClinicId(clinicId);
    doctor.setDisplayName(displayName.trim());
    doctor.setExternalCode(externalCode);
    doctor.setUserId(assignedUserId);
    doctor.setActive(true);
    Doctor saved = doctorRepository.save(doctor);
    return enrichAssignedUsers(List.of(saved)).get(0);
  }

  @Transactional
  public Doctor update(UUID clinicId, UUID doctorId, UUID userId,
                       String displayName, String externalCode, UUID assignedUserId) {
    clinicAccessService.requireRole(clinicId, userId, OWNER_ADMIN_SUPERADMIN);
    Doctor doctor = doctorRepository.findByIdAndClinicId(doctorId, clinicId)
        .orElseThrow(() -> new V2NotFoundException("Doctor not found"));

    if (displayName == null || displayName.isBlank()) {
      throw new V2ValidationException("displayName is required");
    }
    validateAssignedUser(clinicId, assignedUserId);
    doctor.setDisplayName(displayName.trim());
    doctor.setExternalCode(externalCode);
    doctor.setUserId(assignedUserId);
    Doctor saved = doctorRepository.save(doctor);
    return enrichAssignedUsers(List.of(saved)).get(0);
  }

  @Transactional
  public void softDelete(UUID clinicId, UUID doctorId, UUID userId) {
    clinicAccessService.requireRole(clinicId, userId, OWNER_ADMIN_SUPERADMIN);
    Doctor doctor = doctorRepository.findByIdAndClinicId(doctorId, clinicId)
        .orElseThrow(() -> new V2NotFoundException("Doctor not found"));
    doctor.setActive(false);
    doctorRepository.save(doctor);
  }

  private void validateAssignedUser(UUID clinicId, UUID assignedUserId) {
    if (assignedUserId == null) {
      return;
    }
    if (!dbUserRepository.existsById(assignedUserId)) {
      throw new V2ValidationException("Assigned user not found");
    }
    if (!clinicUserRepository.existsByClinicIdAndUserId(clinicId, assignedUserId)) {
      throw new V2ValidationException("Assigned user must be a clinic member");
    }
  }

  private List<Doctor> enrichAssignedUsers(List<Doctor> doctors) {
    if (doctors == null || doctors.isEmpty()) {
      return doctors;
    }
    Set<UUID> userIds = new LinkedHashSet<>();
    for (Doctor doctor : doctors) {
      if (doctor != null && doctor.getUserId() != null) {
        userIds.add(doctor.getUserId());
      }
    }
    Map<UUID, DbUser> usersById = new LinkedHashMap<>();
    if (!userIds.isEmpty()) {
      for (DbUser user : dbUserRepository.findAllById(userIds)) {
        usersById.put(user.getId(), user);
      }
    }
    for (Doctor doctor : doctors) {
      if (doctor == null || doctor.getUserId() == null) {
        continue;
      }
      DbUser user = usersById.get(doctor.getUserId());
      if (user == null) {
        continue;
      }
      doctor.setUserEmail(user.getEmail());
      doctor.setUserDisplayName(user.getDisplayName());
    }
    return doctors;
  }
}
