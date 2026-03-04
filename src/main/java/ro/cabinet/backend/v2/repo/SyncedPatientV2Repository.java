package ro.cabinet.backend.v2.repo;

import ro.cabinet.backend.v2.entity.SyncedPatientV2;
import ro.cabinet.backend.v2.entity.SyncedPatientV2Id;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncedPatientV2Repository extends JpaRepository<SyncedPatientV2, SyncedPatientV2Id> {
  List<SyncedPatientV2> findByClinicIdOrderByPatientNameAsc(UUID clinicId);
}
