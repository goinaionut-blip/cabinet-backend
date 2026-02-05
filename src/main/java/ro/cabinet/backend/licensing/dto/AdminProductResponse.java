package ro.cabinet.backend.licensing.dto;

import java.time.Instant;

public class AdminProductResponse {
  private String productCode;
  private String name;
  private int trialDays;
  private int offlineGraceHours;
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
