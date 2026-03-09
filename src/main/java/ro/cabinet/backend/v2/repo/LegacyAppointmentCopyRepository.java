package ro.cabinet.backend.v2.repo;

import ro.cabinet.backend.v2.entity.LegacyAppointmentCopy;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LegacyAppointmentCopyRepository extends JpaRepository<LegacyAppointmentCopy, UUID> {
  boolean existsByClinicIdAndDoctorIdAndLegacyAppointmentId(UUID clinicId, UUID doctorId, Long legacyAppointmentId);

  long countByClinicIdAndDoctorId(UUID clinicId, UUID doctorId);

  Optional<LegacyAppointmentCopy> findFirstByClinicIdAndDoctorIdOrderByCopiedAtDesc(UUID clinicId, UUID doctorId);
}
