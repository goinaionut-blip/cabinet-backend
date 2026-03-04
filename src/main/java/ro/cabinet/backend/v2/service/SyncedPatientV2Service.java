package ro.cabinet.backend.v2.service;

import ro.cabinet.backend.v2.entity.SyncedPatientV2;
import ro.cabinet.backend.v2.entity.SyncedPatientV2Id;
import ro.cabinet.backend.v2.exception.V2ValidationException;
import ro.cabinet.backend.v2.repo.SyncedPatientV2Repository;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SyncedPatientV2Service {
  private final SyncedPatientV2Repository repository;
  private final ClinicAccessService clinicAccessService;

  public SyncedPatientV2Service(SyncedPatientV2Repository repository,
                                ClinicAccessService clinicAccessService) {
    this.repository = repository;
    this.clinicAccessService = clinicAccessService;
  }

  @Transactional(readOnly = true)
  public List<SyncedPatientV2> list(UUID clinicId, UUID userId) {
    clinicAccessService.requireClinicMembership(clinicId, userId);
    return repository.findByClinicIdOrderByPatientNameAsc(clinicId);
  }

  @Transactional
  public SyncedPatientV2 upsert(UUID clinicId, String patientId, String patientName, UUID userId) {
    clinicAccessService.requireClinicMembership(clinicId, userId);
    if (patientName == null || patientName.isBlank()) {
      throw new V2ValidationException("patientName is required");
    }

    SyncedPatientV2 patient = repository.findById(new SyncedPatientV2Id(clinicId, patientId))
        .orElseGet(() -> {
          SyncedPatientV2 created = new SyncedPatientV2();
          created.setClinicId(clinicId);
          created.setPatientId(patientId);
          return created;
        });
    patient.setPatientName(patientName.trim());
    return repository.save(patient);
  }

  @Transactional
  public void delete(UUID clinicId, String patientId, UUID userId) {
    clinicAccessService.requireClinicMembership(clinicId, userId);
    repository.deleteById(new SyncedPatientV2Id(clinicId, patientId));
  }
}
