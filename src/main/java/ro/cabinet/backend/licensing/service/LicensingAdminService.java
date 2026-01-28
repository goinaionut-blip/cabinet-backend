package ro.cabinet.backend.licensing.service;

import ro.cabinet.backend.licensing.dto.AdminActivationResponse;
import ro.cabinet.backend.licensing.dto.AdminCreateLicenseRequest;
import ro.cabinet.backend.licensing.dto.AdminCreateLicenseResponse;
import ro.cabinet.backend.licensing.dto.AdminLicenseResponse;
import ro.cabinet.backend.licensing.entity.LicenseStatus;
import ro.cabinet.backend.licensing.entity.LicensingActivation;
import ro.cabinet.backend.licensing.entity.LicensingLicense;
import ro.cabinet.backend.licensing.entity.LicensingProduct;
import ro.cabinet.backend.licensing.exception.LicensingBadRequestException;
import ro.cabinet.backend.licensing.exception.LicensingConflictException;
import ro.cabinet.backend.licensing.exception.LicensingNotFoundException;
import ro.cabinet.backend.licensing.repo.LicensingActivationRepository;
import ro.cabinet.backend.licensing.repo.LicensingLicenseRepository;
import ro.cabinet.backend.licensing.repo.LicensingProductRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LicensingAdminService {
  private static final int MAX_GENERATE_ATTEMPTS = 5;

  private final LicensingProductRepository productRepository;
  private final LicensingLicenseRepository licenseRepository;
  private final LicensingActivationRepository activationRepository;
  private final LicenseKeyService licenseKeyService;
  private final Clock clock;

  public LicensingAdminService(LicensingProductRepository productRepository,
                               LicensingLicenseRepository licenseRepository,
                               LicensingActivationRepository activationRepository,
                               LicenseKeyService licenseKeyService,
                               Clock clock) {
    this.productRepository = productRepository;
    this.licenseRepository = licenseRepository;
    this.activationRepository = activationRepository;
    this.licenseKeyService = licenseKeyService;
    this.clock = clock;
  }

  @Transactional
  public AdminCreateLicenseResponse createLicense(AdminCreateLicenseRequest request) {
    String productCode = normalizeProductCode(request.getProductCode());
    LicensingProduct product = productRepository.findById(productCode)
        .orElseThrow(() -> new LicensingNotFoundException("Product not found."));

    for (int attempt = 0; attempt < MAX_GENERATE_ATTEMPTS; attempt++) {
      String licenseKey = licenseKeyService.generateKey(product.getProductCode());
      String hash = licenseKeyService.hashKey(licenseKey);

      LicensingLicense license = new LicensingLicense();
      license.setId(UUID.randomUUID());
      license.setProductCode(product.getProductCode());
      license.setLicenseKeyHash(hash);
      license.setPlan(request.getPlan() == null ? "STANDARD" : request.getPlan());
      license.setStatus(LicenseStatus.ACTIVE);
      license.setMaxSeats(request.getMaxSeats());
      license.setValidUntil(request.getValidUntil());
      license.setCreatedAt(Instant.now(clock));
      license.setNotes(request.getNotes());

      try {
        licenseRepository.save(license);
        return new AdminCreateLicenseResponse(license.getId(), licenseKey);
      } catch (DataIntegrityViolationException ex) {
        // retry key generation in case of hash collision
      }
    }
    throw new LicensingConflictException("Failed to generate unique license key.");
  }

  @Transactional
  public void revokeLicense(UUID licenseId) {
    LicensingLicense license = licenseRepository.findById(licenseId)
        .orElseThrow(() -> new LicensingNotFoundException("License not found."));
    if (license.getStatus() != LicenseStatus.REVOKED) {
      license.setStatus(LicenseStatus.REVOKED);
      licenseRepository.save(license);
    }
    activationRepository.revokeByLicenseId(licenseId, Instant.now(clock));
  }

  @Transactional(readOnly = true)
  public List<AdminLicenseResponse> listLicenses(String productCode) {
    List<LicensingLicense> licenses;
    if (productCode == null || productCode.isBlank()) {
      licenses = licenseRepository.findAll();
    } else {
      licenses = licenseRepository.findByProductCode(normalizeProductCode(productCode));
    }
    List<AdminLicenseResponse> response = new ArrayList<>();
    for (LicensingLicense license : licenses) {
      AdminLicenseResponse item = new AdminLicenseResponse();
      item.setLicenseId(license.getId());
      item.setProductCode(license.getProductCode());
      item.setStatus(license.getStatus().name());
      item.setPlan(license.getPlan());
      item.setMaxSeats(license.getMaxSeats());
      item.setValidUntil(license.getValidUntil());
      item.setCreatedAt(license.getCreatedAt());
      item.setNotes(license.getNotes());
      int activeCount = activationRepository.countByLicenseIdAndRevokedAtIsNull(license.getId());
      item.setActiveActivations(activeCount);
      response.add(item);
    }
    return response;
  }

  @Transactional(readOnly = true)
  public List<AdminActivationResponse> listActivations(String productCode, UUID installId) {
    if (productCode == null || productCode.isBlank()) {
      throw new LicensingBadRequestException("productCode is required.");
    }
    String normalizedProduct = normalizeProductCode(productCode);
    List<LicensingActivation> activations;
    if (installId == null) {
      activations = activationRepository.findByProductCode(normalizedProduct);
    } else {
      activations = activationRepository.findByProductCodeAndInstallId(normalizedProduct, installId);
    }

    List<AdminActivationResponse> response = new ArrayList<>();
    for (LicensingActivation activation : activations) {
      AdminActivationResponse item = new AdminActivationResponse();
      item.setActivationId(activation.getId());
      item.setLicenseId(activation.getLicenseId());
      item.setInstallId(activation.getInstallId());
      item.setProductCode(activation.getProductCode());
      item.setActivatedAt(activation.getActivatedAt());
      item.setRevokedAt(activation.getRevokedAt());
      response.add(item);
    }
    return response;
  }

  private String normalizeProductCode(String productCode) {
    if (productCode == null || productCode.isBlank()) {
      throw new LicensingBadRequestException("Product code is required.");
    }
    return productCode.trim().toUpperCase();
  }
}
