package ro.cabinet.backend.licensing.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "licensing_activations")
public class LicensingActivation {
  @Id
  @Column(nullable = false)
  private UUID id;

  @Column(name = "license_id", nullable = false)
  private UUID licenseId;

  @Column(name = "install_id", nullable = false)
  private UUID installId;

  @Column(name = "product_code", nullable = false, length = 64)
  private String productCode;

  @Column(name = "activated_at", nullable = false)
  private Instant activatedAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
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
