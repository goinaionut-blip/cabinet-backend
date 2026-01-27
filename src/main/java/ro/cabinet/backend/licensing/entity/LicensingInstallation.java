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
@Table(name = "licensing_installations")
public class LicensingInstallation {
  @Id
  @Column(nullable = false)
  private UUID id;

  @Column(name = "product_code", nullable = false, length = 64)
  private String productCode;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "last_seen_at")
  private Instant lastSeenAt;

  @Column(name = "trial_started_at", nullable = false)
  private Instant trialStartedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private InstallationStatus status;

  @Column(name = "blocked_reason")
  private String blockedReason;

  @Column(name = "app_version")
  private String appVersion;

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

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getLastSeenAt() {
    return lastSeenAt;
  }

  public void setLastSeenAt(Instant lastSeenAt) {
    this.lastSeenAt = lastSeenAt;
  }

  public Instant getTrialStartedAt() {
    return trialStartedAt;
  }

  public void setTrialStartedAt(Instant trialStartedAt) {
    this.trialStartedAt = trialStartedAt;
  }

  public InstallationStatus getStatus() {
    return status;
  }

  public void setStatus(InstallationStatus status) {
    this.status = status;
  }

  public String getBlockedReason() {
    return blockedReason;
  }

  public void setBlockedReason(String blockedReason) {
    this.blockedReason = blockedReason;
  }

  public String getAppVersion() {
    return appVersion;
  }

  public void setAppVersion(String appVersion) {
    this.appVersion = appVersion;
  }
}
