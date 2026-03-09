package ro.cabinet.backend.v2.repo;

import ro.cabinet.backend.v2.entity.Clinic;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicRepository extends JpaRepository<Clinic, UUID> {
  boolean existsBySlugIgnoreCase(String slug);

  boolean existsBySlugIgnoreCaseAndIdNot(String slug, UUID id);
}
