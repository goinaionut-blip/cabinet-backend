package ro.cabinet.backend.licensing.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.cabinet.backend.licensing.entity.LicensingProduct;

public interface LicensingProductRepository extends JpaRepository<LicensingProduct, String> {
}
