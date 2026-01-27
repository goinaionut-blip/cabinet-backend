package ro.cabinet.backend.licensing.dto;

import java.time.Instant;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AdminCreateLicenseRequest {
  @NotBlank
  @Pattern(regexp = "^[A-Z0-9_-]{3,32}$")
  private String productCode;

  @Min(1)
  private int maxSeats = 1;

  private Instant validUntil;

  private String plan = "STANDARD";

  private String notes;

  public String getProductCode() {
    return productCode;
  }

  public void setProductCode(String productCode) {
    this.productCode = productCode;
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

  public String getPlan() {
    return plan;
  }

  public void setPlan(String plan) {
    this.plan = plan;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }
}
