package ro.cabinet.backend.licensing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class TouchRequest {
  @NotBlank
  @Pattern(regexp = "^[A-Z0-9_-]{3,32}$")
  private String productCode;

  @NotBlank
  @Pattern(regexp = "^[0-9a-fA-F-]{36}$")
  private String installId;

  private String appVersion;

  public String getProductCode() {
    return productCode;
  }

  public void setProductCode(String productCode) {
    this.productCode = productCode;
  }

  public String getInstallId() {
    return installId;
  }

  public void setInstallId(String installId) {
    this.installId = installId;
  }

  public String getAppVersion() {
    return appVersion;
  }

  public void setAppVersion(String appVersion) {
    this.appVersion = appVersion;
  }
}
