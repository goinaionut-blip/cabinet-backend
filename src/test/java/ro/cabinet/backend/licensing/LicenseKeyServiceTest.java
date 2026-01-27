package ro.cabinet.backend.licensing;

import ro.cabinet.backend.licensing.config.LicensingProperties;
import ro.cabinet.backend.licensing.service.LicenseKeyService;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LicenseKeyServiceTest {
  @Test
  void hashIsStable() {
    LicensingProperties properties = new LicensingProperties();
    properties.setKeyHashSecret("test-secret");
    LicenseKeyService service = new LicenseKeyService(properties);

    String hash1 = service.hashKey("DENTRX-ABCD-EFGH-IJKL");
    String hash2 = service.hashKey("DENTRX-ABCD-EFGH-IJKL");

    assertEquals(hash1, hash2);
  }

  @Test
  void generateFormatIncludesProductCode() {
    LicensingProperties properties = new LicensingProperties();
    properties.setKeyHashSecret("test-secret");
    LicenseKeyService service = new LicenseKeyService(properties);

    String key = service.generateKey("DENTRX");
    assertTrue(key.startsWith("DENTRX-"));
  }
}
