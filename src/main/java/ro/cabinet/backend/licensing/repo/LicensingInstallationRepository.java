package ro.cabinet.backend.licensing.repo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.cabinet.backend.licensing.entity.LicensingInstallation;

public interface LicensingInstallationRepository extends JpaRepository<LicensingInstallation, UUID> {
  Optional<LicensingInstallation> findByIdAndProductCode(UUID id, String productCode);
}
