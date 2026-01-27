package ro.cabinet.backend.licensing.service;

import ro.cabinet.backend.licensing.config.LicensingProperties;
import ro.cabinet.backend.licensing.exception.LicensingUnauthorizedException;

import org.springframework.stereotype.Component;

@Component
public class LicensingAdminGuard {
  private final LicensingProperties properties;

  public LicensingAdminGuard(LicensingProperties properties) {
    this.properties = properties;
  }

  public void requireAuthorized(String tokenHeader) {
    String adminToken = properties.getAdminToken();
    if (adminToken == null || adminToken.isBlank()) {
      return;
    }
    if (tokenHeader == null || !adminToken.equals(tokenHeader)) {
      throw new LicensingUnauthorizedException("Invalid admin token.");
    }
  }
}
