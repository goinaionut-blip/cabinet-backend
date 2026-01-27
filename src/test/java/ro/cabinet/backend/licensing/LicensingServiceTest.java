package ro.cabinet.backend.licensing;

import ro.cabinet.backend.licensing.dto.ActivateRequest;
import ro.cabinet.backend.licensing.dto.EntitlementResponse;
import ro.cabinet.backend.licensing.dto.TouchRequest;
import ro.cabinet.backend.licensing.entity.InstallationStatus;
import ro.cabinet.backend.licensing.entity.LicenseStatus;
import ro.cabinet.backend.licensing.entity.LicensingInstallation;
import ro.cabinet.backend.licensing.entity.LicensingLicense;
import ro.cabinet.backend.licensing.entity.LicensingProduct;
import ro.cabinet.backend.licensing.exception.LicensingConflictException;
import ro.cabinet.backend.licensing.repo.LicensingActivationRepository;
import ro.cabinet.backend.licensing.repo.LicensingInstallationRepository;
import ro.cabinet.backend.licensing.repo.LicensingLicenseRepository;
import ro.cabinet.backend.licensing.repo.LicensingProductRepository;
import ro.cabinet.backend.licensing.service.JwtEntitlementService;
import ro.cabinet.backend.licensing.service.LicenseKeyService;
import ro.cabinet.backend.licensing.service.LicensingService;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class LicensingServiceTest {
  @Test
  void trialExpiresOnBoundary() throws Exception {
    LicensingProductRepository productRepository = Mockito.mock(LicensingProductRepository.class);
    LicensingInstallationRepository installationRepository = Mockito.mock(LicensingInstallationRepository.class);
    LicensingLicenseRepository licenseRepository = Mockito.mock(LicensingLicenseRepository.class);
    LicensingActivationRepository activationRepository = Mockito.mock(LicensingActivationRepository.class);

    LicensingProduct product = new LicensingProduct();
    product.setProductCode("DENTRX");
    product.setTrialDays(14);
    product.setOfflineGraceHours(72);
    when(productRepository.findById("DENTRX")).thenReturn(Optional.of(product));

    UUID installId = UUID.randomUUID();
    Instant trialStart = Instant.parse("2026-01-13T08:10:00Z");
    Instant now = Instant.parse("2026-01-27T08:10:00Z");

    LicensingInstallation installation = new LicensingInstallation();
    installation.setId(installId);
    installation.setProductCode("DENTRX");
    installation.setTrialStartedAt(trialStart);
    installation.setStatus(InstallationStatus.TRIAL);

    when(installationRepository.findById(installId)).thenReturn(Optional.of(installation));
    when(activationRepository.findActiveActivation(installId, "DENTRX", now))
        .thenReturn(Optional.empty());

    Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    JwtEntitlementService jwtService = jwtService(clock);
    LicenseKeyService licenseKeyService = licenseKeyService();

    LicensingService service = new LicensingService(
        productRepository, installationRepository, licenseRepository,
        activationRepository, licenseKeyService, jwtService, clock);

    TouchRequest request = new TouchRequest();
    request.setProductCode("DENTRX");
    request.setInstallId(installId.toString());

    EntitlementResponse response = service.touch(request);

    assertEquals("TRIAL_EXPIRED", response.getStatus());
  }

  @Test
  void activationFailsWhenSeatsExceeded() throws Exception {
    LicensingProductRepository productRepository = Mockito.mock(LicensingProductRepository.class);
    LicensingInstallationRepository installationRepository = Mockito.mock(LicensingInstallationRepository.class);
    LicensingLicenseRepository licenseRepository = Mockito.mock(LicensingLicenseRepository.class);
    LicensingActivationRepository activationRepository = Mockito.mock(LicensingActivationRepository.class);

    LicensingProduct product = new LicensingProduct();
    product.setProductCode("DENTRX");
    product.setTrialDays(14);
    product.setOfflineGraceHours(72);
    when(productRepository.findById("DENTRX")).thenReturn(Optional.of(product));

    LicenseKeyService licenseKeyService = licenseKeyService();
    String licenseKey = "DENTRX-ABCD-EFGH-IJKL";
    String hash = licenseKeyService.hashKey(licenseKey);

    LicensingLicense license = new LicensingLicense();
    license.setId(UUID.randomUUID());
    license.setProductCode("DENTRX");
    license.setLicenseKeyHash(hash);
    license.setStatus(LicenseStatus.ACTIVE);
    license.setMaxSeats(1);

    when(licenseRepository.findByProductCodeAndLicenseKeyHashAndStatus("DENTRX", hash, LicenseStatus.ACTIVE))
        .thenReturn(Optional.of(license));
    when(activationRepository.countByLicenseIdAndRevokedAtIsNull(license.getId())).thenReturn(1);
    when(activationRepository.findByLicenseIdAndInstallId(any(), any())).thenReturn(Optional.empty());
    when(installationRepository.findById(any())).thenReturn(Optional.empty());

    Clock clock = Clock.fixed(Instant.parse("2026-01-27T08:10:00Z"), ZoneOffset.UTC);
    JwtEntitlementService jwtService = jwtService(clock);

    LicensingService service = new LicensingService(
        productRepository, installationRepository, licenseRepository,
        activationRepository, licenseKeyService, jwtService, clock);

    ActivateRequest request = new ActivateRequest();
    request.setProductCode("DENTRX");
    request.setInstallId(UUID.randomUUID().toString());
    request.setLicenseKey(licenseKey);

    assertThrows(LicensingConflictException.class, () -> service.activate(request));
  }

  private LicenseKeyService licenseKeyService() {
    ro.cabinet.backend.licensing.config.LicensingProperties properties =
        new ro.cabinet.backend.licensing.config.LicensingProperties();
    properties.setKeyHashSecret("test-secret");
    return new LicenseKeyService(properties);
  }

  private JwtEntitlementService jwtService(Clock clock) throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair keyPair = generator.generateKeyPair();

    ro.cabinet.backend.licensing.config.LicensingProperties properties =
        new ro.cabinet.backend.licensing.config.LicensingProperties();
    properties.setJwtIssuer("licensing-test");
    properties.setJwtPrivateKeyPem(toPem("PRIVATE KEY", keyPair.getPrivate().getEncoded()));
    properties.setJwtPublicKeyPem(toPem("PUBLIC KEY", keyPair.getPublic().getEncoded()));

    return new JwtEntitlementService(properties, clock);
  }

  private String toPem(String type, byte[] encoded) {
    String base64 = Base64.getEncoder().encodeToString(encoded);
    StringBuilder builder = new StringBuilder();
    builder.append("-----BEGIN ").append(type).append("-----\n");
    for (int i = 0; i < base64.length(); i += 64) {
      int end = Math.min(base64.length(), i + 64);
      builder.append(base64, i, end).append("\n");
    }
    builder.append("-----END ").append(type).append("-----\n");
    return builder.toString();
  }
}
