package ro.stoma.efactura.oauth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class AnafToken {
  private String accessToken;
  private String refreshToken;
  private Instant expiresAt;

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public void setRefreshToken(String refreshToken) {
    this.refreshToken = refreshToken;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public boolean isExpired(Instant now) {
    if (expiresAt == null) {
      return true;
    }
    return expiresAt.isBefore(now.plus(60, ChronoUnit.SECONDS));
  }
}
