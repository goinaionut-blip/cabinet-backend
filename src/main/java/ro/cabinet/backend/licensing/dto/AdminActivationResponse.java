package ro.cabinet.backend.licensing.dto;

import java.time.Instant;
import java.util.UUID;

public class AdminActivationResponse {
  private UUID activationId;
  private UUID licenseId;
  private UUID installId;
  private String productCode;
  private Instant activatedAt;
  private Instant revokedAt;

  public UUID getActivationId() {
    return activationId;
  }

  public void setActivationId(UUID activationId) {
    this.activationId = activationId;
  }

  public UUID getLicenseId() {
    return licenseId;
  }

  public void setLicenseId(UUID licenseId) {
    this.licenseId = licenseId;
  }

  public UUID getInstallId() {
    return installId;
  }

  public void setInstallId(UUID installId) {
    this.installId = installId;
  }

  public String getProductCode() {
    return productCode;
  }

  public void setProductCode(String productCode) {
    this.productCode = productCode;
  }

  public Instant getActivatedAt() {
    return activatedAt;
  }

  public void setActivatedAt(Instant activatedAt) {
    this.activatedAt = activatedAt;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  public void setRevokedAt(Instant revokedAt) {
    this.revokedAt = revokedAt;
  }
}
