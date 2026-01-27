package ro.cabinet.backend.licensing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "licensing")
public class LicensingProperties {
  private String adminToken;
  private String keyHashSecret;
  private String jwtIssuer;
  private String jwtPrivateKeyPem;
  private String jwtPublicKeyPem;
  private int entitlementTtlHours = 24;
  private RateLimit rateLimit = new RateLimit();

  public String getAdminToken() {
    return adminToken;
  }

  public void setAdminToken(String adminToken) {
    this.adminToken = adminToken;
  }

  public String getKeyHashSecret() {
    return keyHashSecret;
  }

  public void setKeyHashSecret(String keyHashSecret) {
    this.keyHashSecret = keyHashSecret;
  }

  public String getJwtIssuer() {
    return jwtIssuer;
  }

  public void setJwtIssuer(String jwtIssuer) {
    this.jwtIssuer = jwtIssuer;
  }

  public String getJwtPrivateKeyPem() {
    return jwtPrivateKeyPem;
  }

  public void setJwtPrivateKeyPem(String jwtPrivateKeyPem) {
    this.jwtPrivateKeyPem = jwtPrivateKeyPem;
  }

  public String getJwtPublicKeyPem() {
    return jwtPublicKeyPem;
  }

  public void setJwtPublicKeyPem(String jwtPublicKeyPem) {
    this.jwtPublicKeyPem = jwtPublicKeyPem;
  }

  public int getEntitlementTtlHours() {
    return entitlementTtlHours;
  }

  public void setEntitlementTtlHours(int entitlementTtlHours) {
    this.entitlementTtlHours = entitlementTtlHours;
  }

  public RateLimit getRateLimit() {
    return rateLimit;
  }

  public void setRateLimit(RateLimit rateLimit) {
    this.rateLimit = rateLimit;
  }

  public static class RateLimit {
    private int windowSeconds = 60;
    private int maxRequests = 60;

    public int getWindowSeconds() {
      return windowSeconds;
    }

    public void setWindowSeconds(int windowSeconds) {
      this.windowSeconds = windowSeconds;
    }

    public int getMaxRequests() {
      return maxRequests;
    }

    public void setMaxRequests(int maxRequests) {
      this.maxRequests = maxRequests;
    }
  }
}
