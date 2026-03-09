package ro.cabinet.backend.v2.repo;

import ro.cabinet.backend.v2.entity.ClinicUser;
import ro.cabinet.backend.v2.entity.ClinicUserId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClinicUserRepository extends JpaRepository<ClinicUser, ClinicUserId> {
  boolean existsByClinicIdAndUserId(UUID clinicId, UUID userId);

  Optional<ClinicUser> findByClinicIdAndUserId(UUID clinicId, UUID userId);

  @Query("""
      select cu from ClinicUser cu
      where cu.userId = :userId
      order by cu.createdAt asc
      """)
  List<ClinicUser> findAllByUserId(@Param("userId") UUID userId);

  List<ClinicUser> findAllByClinicIdOrderByCreatedAtAsc(UUID clinicId);
}
