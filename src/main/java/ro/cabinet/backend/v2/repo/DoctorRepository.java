package ro.cabinet.backend.v2.repo;

import ro.cabinet.backend.v2.entity.Doctor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, UUID> {
  List<Doctor> findByClinicIdAndActiveTrueOrderByDisplayNameAsc(UUID clinicId);

  Optional<Doctor> findByIdAndClinicId(UUID id, UUID clinicId);

  boolean existsByIdAndClinicIdAndActiveTrue(UUID id, UUID clinicId);
}
