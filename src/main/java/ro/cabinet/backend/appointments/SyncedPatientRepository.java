package ro.cabinet.backend.appointments;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncedPatientRepository extends JpaRepository<SyncedPatient, Long> {
}
