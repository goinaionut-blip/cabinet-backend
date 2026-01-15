package ro.cabinet.backend.appointments;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class SyncedPatientService {
  private final SyncedPatientRepository repository;

  public SyncedPatientService(SyncedPatientRepository repository) {
    this.repository = repository;
  }

  public SyncedPatient upsert(Long patientId, String patientName) {
    SyncedPatient patient = new SyncedPatient();
    patient.setPatientId(patientId);
    patient.setPatientName(patientName);
    return repository.save(patient);
  }

  public List<SyncedPatient> findAll() {
    return repository.findAll();
  }

  public void delete(Long patientId) {
    repository.deleteById(patientId);
  }
}
