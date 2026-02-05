package ro.cabinet.backend.licensing.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AdminCreateProductRequest {
  @NotBlank
  @Pattern(regexp = "^[A-Z0-9_-]{3,32}$")
  private String productCode;

  @NotBlank
  private String name;

  @Min(0)
  private Integer trialDays;

  @Min(0)
  private Integer offlineGraceHours;

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

  public Integer getTrialDays() {
    return trialDays;
  }

  public void setTrialDays(Integer trialDays) {
    this.trialDays = trialDays;
  }

  public Integer getOfflineGraceHours() {
    return offlineGraceHours;
  }

  public void setOfflineGraceHours(Integer offlineGraceHours) {
    this.offlineGraceHours = offlineGraceHours;
  }
}
