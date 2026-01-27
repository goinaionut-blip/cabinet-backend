package ro.cabinet.backend.licensing.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "licensing_licenses")
public class LicensingLicense {
  @Id
  @Column(nullable = false)
  private UUID id;

  @Column(name = "product_code", nullable = false, length = 64)
  private String productCode;

  @Column(name = "license_key_hash", nullable = false, unique = true)
  private String licenseKeyHash;

  @Column(nullable = false)
  private String plan;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private LicenseStatus status;

  @Column(name = "max_seats", nullable = false)
  private int maxSeats;

  @Column(name = "valid_until")
  private Instant validUntil;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column
  private String notes;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getProductCode() {
    return productCode;
  }

  public void setProductCode(String productCode) {
    this.productCode = productCode;
  }

  public String getLicenseKeyHash() {
    return licenseKeyHash;
  }

  public void setLicenseKeyHash(String licenseKeyHash) {
    this.licenseKeyHash = licenseKeyHash;
  }

  public String getPlan() {
    return plan;
  }

  public void setPlan(String plan) {
    this.plan = plan;
  }

  public LicenseStatus getStatus() {
    return status;
  }

  public void setStatus(LicenseStatus status) {
    this.status = status;
  }

  public int getMaxSeats() {
    return maxSeats;
  }

  public void setMaxSeats(int maxSeats) {
    this.maxSeats = maxSeats;
  }

  public Instant getValidUntil() {
    return validUntil;
  }

  public void setValidUntil(Instant validUntil) {
    this.validUntil = validUntil;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }
}
