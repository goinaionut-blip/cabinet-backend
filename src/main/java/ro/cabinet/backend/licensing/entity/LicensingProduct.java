package ro.cabinet.backend.licensing.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "licensing_products")
public class LicensingProduct {
  @Id
  @Column(name = "product_code", nullable = false, length = 64)
  private String productCode;

  @Column(nullable = false)
  private String name;

  @Column(name = "trial_days", nullable = false)
  private int trialDays;

  @Column(name = "offline_grace_hours", nullable = false)
  private int offlineGraceHours;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public String getProductCode() {
    return productCode;
  }

  public void setProductCode(String productCode) {
    this.productCode = productCode;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getTrialDays() {
    return trialDays;
  }

  public void setTrialDays(int trialDays) {
    this.trialDays = trialDays;
  }

  public int getOfflineGraceHours() {
    return offlineGraceHours;
  }

  public void setOfflineGraceHours(int offlineGraceHours) {
    this.offlineGraceHours = offlineGraceHours;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
