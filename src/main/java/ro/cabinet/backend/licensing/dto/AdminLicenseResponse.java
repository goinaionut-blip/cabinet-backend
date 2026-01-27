package ro.cabinet.backend.licensing.dto;

import java.time.Instant;
import java.util.UUID;

public class AdminLicenseResponse {
  private UUID licenseId;
  private String productCode;
  private String status;
  private String plan;
  private int maxSeats;
  private Instant validUntil;
  private Instant createdAt;
  private int activeActivations;

  public UUID getLicenseId() {
    return licenseId;
  }

  public void setLicenseId(UUID licenseId) {
    this.licenseId = licenseId;
  }

  public String getProductCode() {
    return productCode;
  }

  public void setProductCode(String productCode) {
    this.productCode = productCode;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getPlan() {
    return plan;
  }

  public void setPlan(String plan) {
    this.plan = plan;
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

  public int getActiveActivations() {
    return activeActivations;
  }

  public void setActiveActivations(int activeActivations) {
    this.activeActivations = activeActivations;
  }
}
