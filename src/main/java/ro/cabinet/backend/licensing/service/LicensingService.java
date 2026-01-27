package ro.cabinet.backend.licensing.service;

import ro.cabinet.backend.licensing.dto.ActivateRequest;
import ro.cabinet.backend.licensing.dto.EntitlementResponse;
import ro.cabinet.backend.licensing.dto.TouchRequest;
import ro.cabinet.backend.licensing.entity.InstallationStatus;
import ro.cabinet.backend.licensing.entity.LicenseStatus;
import ro.cabinet.backend.licensing.entity.LicensingActivation;
import ro.cabinet.backend.licensing.entity.LicensingInstallation;
import ro.cabinet.backend.licensing.entity.LicensingLicense;
import ro.cabinet.backend.licensing.entity.LicensingProduct;
import ro.cabinet.backend.licensing.exception.LicensingBadRequestException;
import ro.cabinet.backend.licensing.exception.LicensingConflictException;
import ro.cabinet.backend.licensing.exception.LicensingNotFoundException;
import ro.cabinet.backend.licensing.repo.LicensingActivationRepository;
import ro.cabinet.backend.licensing.repo.LicensingInstallationRepository;
import ro.cabinet.backend.licensing.repo.LicensingLicenseRepository;
import ro.cabinet.backend.licensing.repo.LicensingProductRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LicensingService {
  private final LicensingProductRepository productRepository;
  private final LicensingInstallationRepository installationRepository;
  private final LicensingLicenseRepository licenseRepository;
  private final LicensingActivationRepository activationRepository;
  private final LicenseKeyService licenseKeyService;
  private final JwtEntitlementService jwtEntitlementService;
  private final Clock clock;

  public LicensingService(LicensingProductRepository productRepository,
                          LicensingInstallationRepository installationRepository,
                          LicensingLicenseRepository licenseRepository,
                          LicensingActivationRepository activationRepository,
                          LicenseKeyService licenseKeyService,
                          JwtEntitlementService jwtEntitlementService,
                          Clock clock) {
    this.productRepository = productRepository;
    this.installationRepository = installationRepository;
    this.licenseRepository = licenseRepository;
    this.activationRepository = activationRepository;
    this.licenseKeyService = licenseKeyService;
    this.jwtEntitlementService = jwtEntitlementService;
    this.clock = clock;
  }

  @Transactional
  public EntitlementResponse touch(TouchRequest request) {
    String productCode = normalizeProductCode(request.getProductCode());
    UUID installId = parseInstallId(request.getInstallId());
    LicensingProduct product = productRepository.findById(productCode)
        .orElseThrow(() -> new LicensingNotFoundException("Product not found."));

    Instant now = Instant.now(clock);
    LicensingInstallation installation = installationRepository.findById(installId)
        .orElse(null);
    if (installation == null) {
      installation = new LicensingInstallation();
      installation.setId(installId);
      installation.setProductCode(productCode);
      installation.setCreatedAt(now);
      installation.setTrialStartedAt(now);
      installation.setStatus(InstallationStatus.TRIAL);
    } else if (!productCode.equalsIgnoreCase(installation.getProductCode())) {
      throw new LicensingConflictException("Install ID belongs to another product.");
    }

    installation.setLastSeenAt(now);
    installation.setAppVersion(request.getAppVersion());

    Optional<LicensingActivation> activeActivation = Optional.empty();
    if (installation.getStatus() != InstallationStatus.BLOCKED) {
      activeActivation = activationRepository.findActiveActivation(installId, productCode, now);
    }

    EntitlementStatus entitlementStatus;
    Optional<String> licenseId = Optional.empty();
    Instant trialEnd = installation.getTrialStartedAt()
        .plus(product.getTrialDays(), ChronoUnit.DAYS);

    if (installation.getStatus() == InstallationStatus.BLOCKED) {
      entitlementStatus = EntitlementStatus.BLOCKED;
    } else if (activeActivation.isPresent()) {
      entitlementStatus = EntitlementStatus.PAID;
      licenseId = Optional.of(activeActivation.get().getLicenseId().toString());
      if (installation.getStatus() != InstallationStatus.PAID) {
        installation.setStatus(InstallationStatus.PAID);
      }
    } else if (now.isBefore(trialEnd)) {
      entitlementStatus = EntitlementStatus.TRIAL_ACTIVE;
    } else {
      entitlementStatus = EntitlementStatus.TRIAL_EXPIRED;
      if (installation.getStatus() == InstallationStatus.TRIAL) {
        installation.setStatus(InstallationStatus.EXPIRED);
      }
    }

    installationRepository.save(installation);

    String token = jwtEntitlementService.issueToken(
        installId.toString(), productCode, entitlementStatus,
        installation.getTrialStartedAt(), trialEnd, product.getOfflineGraceHours(), licenseId);

    return new EntitlementResponse(now, entitlementStatus.name(), trialEnd,
        product.getOfflineGraceHours(), token);
  }

  @Transactional
  public EntitlementResponse activate(ActivateRequest request) {
    String productCode = normalizeProductCode(request.getProductCode());
    UUID installId = parseInstallId(request.getInstallId());
    String licenseKey = request.getLicenseKey().trim().toUpperCase();
    LicensingProduct product = productRepository.findById(productCode)
        .orElseThrow(() -> new LicensingNotFoundException("Product not found."));

    String licenseHash = licenseKeyService.hashKey(licenseKey);
    LicensingLicense license = licenseRepository
        .findByProductCodeAndLicenseKeyHashAndStatus(productCode, licenseHash, LicenseStatus.ACTIVE)
        .orElseThrow(() -> new LicensingNotFoundException("License not found."));

    Instant now = Instant.now(clock);
    if (license.getValidUntil() != null && !license.getValidUntil().isAfter(now)) {
      throw new LicensingConflictException("License expired.");
    }

    LicensingInstallation installation = installationRepository.findById(installId).orElse(null);
    if (installation == null) {
      installation = new LicensingInstallation();
      installation.setId(installId);
      installation.setProductCode(productCode);
      installation.setCreatedAt(now);
      installation.setTrialStartedAt(now);
      installation.setStatus(InstallationStatus.TRIAL);
    } else if (!productCode.equalsIgnoreCase(installation.getProductCode())) {
      throw new LicensingConflictException("Install ID belongs to another product.");
    }

    if (installation.getStatus() == InstallationStatus.BLOCKED) {
      throw new LicensingConflictException("Installation is blocked.");
    }

    Optional<LicensingActivation> existingActivation =
        activationRepository.findByLicenseIdAndInstallId(license.getId(), installId);
    if (existingActivation.isPresent()) {
      LicensingActivation activation = existingActivation.get();
      if (activation.getRevokedAt() == null) {
        return paidResponse(now, product, installation, activation.getLicenseId());
      }
      throw new LicensingConflictException("Activation revoked for this install.");
    }

    int activeSeats = activationRepository.countByLicenseIdAndRevokedAtIsNull(license.getId());
    if (activeSeats >= license.getMaxSeats()) {
      throw new LicensingConflictException("License seats exceeded.");
    }

    LicensingActivation activation = new LicensingActivation();
    activation.setId(UUID.randomUUID());
    activation.setLicenseId(license.getId());
    activation.setInstallId(installId);
    activation.setProductCode(productCode);
    activation.setActivatedAt(now);

    try {
      activationRepository.save(activation);
    } catch (DataIntegrityViolationException ex) {
      throw new LicensingConflictException("Activation already exists.");
    }

    installation.setStatus(InstallationStatus.PAID);
    installation.setLastSeenAt(now);
    installationRepository.save(installation);

    return paidResponse(now, product, installation, license.getId());
  }

  private EntitlementResponse paidResponse(Instant now, LicensingProduct product,
                                           LicensingInstallation installation, UUID licenseId) {
    Instant trialEnd = installation.getTrialStartedAt()
        .plus(product.getTrialDays(), ChronoUnit.DAYS);
    String token = jwtEntitlementService.issueToken(
        installation.getId().toString(), product.getProductCode(), EntitlementStatus.PAID,
        installation.getTrialStartedAt(), trialEnd, product.getOfflineGraceHours(),
        Optional.of(licenseId.toString()));
    return new EntitlementResponse(now, EntitlementStatus.PAID.name(), trialEnd,
        product.getOfflineGraceHours(), token);
  }

  private UUID parseInstallId(String installId) {
    try {
      return UUID.fromString(installId);
    } catch (IllegalArgumentException ex) {
      throw new LicensingBadRequestException("Invalid installId.");
    }
  }

  private String normalizeProductCode(String productCode) {
    if (productCode == null || productCode.isBlank()) {
      throw new LicensingBadRequestException("Product code is required.");
    }
    return productCode.trim().toUpperCase();
  }
}
