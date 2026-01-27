package ro.cabinet.backend.licensing.dto;

import java.util.UUID;

public class AdminCreateLicenseResponse {
  private UUID licenseId;
  private String licenseKey;

  public AdminCreateLicenseResponse() {
  }

  public AdminCreateLicenseResponse(UUID licenseId, String licenseKey) {
    this.licenseId = licenseId;
    this.licenseKey = licenseKey;
  }

  public UUID getLicenseId() {
    return licenseId;
  }

  public void setLicenseId(UUID licenseId) {
    this.licenseId = licenseId;
  }

  public String getLicenseKey() {
    return licenseKey;
  }

  public void setLicenseKey(String licenseKey) {
    this.licenseKey = licenseKey;
  }
}
