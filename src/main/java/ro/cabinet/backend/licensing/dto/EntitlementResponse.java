package ro.cabinet.backend.licensing.dto;

import java.time.Instant;

public class EntitlementResponse {
  private Instant serverTime;
  private String status;
  private Instant trialEndsAt;
  private int offlineGraceHours;
  private String entitlementToken;

  public EntitlementResponse() {
  }

  public EntitlementResponse(Instant serverTime, String status, Instant trialEndsAt,
                             int offlineGraceHours, String entitlementToken) {
    this.serverTime = serverTime;
    this.status = status;
    this.trialEndsAt = trialEndsAt;
    this.offlineGraceHours = offlineGraceHours;
    this.entitlementToken = entitlementToken;
  }

  public Instant getServerTime() {
    return serverTime;
  }

  public void setServerTime(Instant serverTime) {
    this.serverTime = serverTime;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Instant getTrialEndsAt() {
    return trialEndsAt;
  }

  public void setTrialEndsAt(Instant trialEndsAt) {
    this.trialEndsAt = trialEndsAt;
  }

  public int getOfflineGraceHours() {
    return offlineGraceHours;
  }

  public void setOfflineGraceHours(int offlineGraceHours) {
    this.offlineGraceHours = offlineGraceHours;
  }

  public String getEntitlementToken() {
    return entitlementToken;
  }

  public void setEntitlementToken(String entitlementToken) {
    this.entitlementToken = entitlementToken;
  }
}
