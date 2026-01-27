package ro.cabinet.backend.licensing.repo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.cabinet.backend.licensing.entity.LicensingActivation;

public interface LicensingActivationRepository extends JpaRepository<LicensingActivation, UUID> {
  Optional<LicensingActivation> findByLicenseIdAndInstallId(UUID licenseId, UUID installId);

  int countByLicenseIdAndRevokedAtIsNull(UUID licenseId);

  List<LicensingActivation> findByProductCode(String productCode);

  List<LicensingActivation> findByProductCodeAndInstallId(String productCode, UUID installId);

  @Query("""
      select a from LicensingActivation a
      join LicensingLicense l on a.licenseId = l.id
      where a.installId = :installId
        and a.productCode = :productCode
        and a.revokedAt is null
        and l.status = 'ACTIVE'
        and (l.validUntil is null or l.validUntil > :now)
      """)
  Optional<LicensingActivation> findActiveActivation(@Param("installId") UUID installId,
                                                     @Param("productCode") String productCode,
                                                     @Param("now") Instant now);

  @Modifying
  @Query("""
      update LicensingActivation a
      set a.revokedAt = :revokedAt
      where a.licenseId = :licenseId and a.revokedAt is null
      """)
  int revokeByLicenseId(@Param("licenseId") UUID licenseId, @Param("revokedAt") Instant revokedAt);
}
