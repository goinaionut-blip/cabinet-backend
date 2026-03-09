package ro.cabinet.backend.v2.repo;

import ro.cabinet.backend.v2.entity.LegacySyncedPatientCopy;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LegacySyncedPatientCopyRepository extends JpaRepository<LegacySyncedPatientCopy, UUID> {
  boolean existsByClinicIdAndLegacyPatientId(UUID clinicId, Long legacyPatientId);

  long countByClinicId(UUID clinicId);

  Optional<LegacySyncedPatientCopy> findFirstByClinicIdOrderByCopiedAtDesc(UUID clinicId);
}
