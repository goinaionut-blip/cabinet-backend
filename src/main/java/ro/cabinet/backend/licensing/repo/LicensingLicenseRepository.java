package ro.cabinet.backend.licensing.repo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.cabinet.backend.licensing.entity.LicenseStatus;
import ro.cabinet.backend.licensing.entity.LicensingLicense;

public interface LicensingLicenseRepository extends JpaRepository<LicensingLicense, UUID> {
  Optional<LicensingLicense> findByProductCodeAndLicenseKeyHashAndStatus(
      String productCode, String licenseKeyHash, LicenseStatus status);

  List<LicensingLicense> findByProductCode(String productCode);
}
