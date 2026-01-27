package ro.cabinet.backend.licensing.service;

import ro.cabinet.backend.licensing.config.LicensingProperties;
import ro.cabinet.backend.licensing.exception.LicensingBadRequestException;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

@Service
public class LicenseKeyService {
  private static final String HMAC_ALGO = "HmacSHA256";
  private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
  private final SecureRandom random = new SecureRandom();
  private final LicensingProperties properties;

  public LicenseKeyService(LicensingProperties properties) {
    this.properties = properties;
  }

  public String generateKey(String productCode) {
    StringBuilder builder = new StringBuilder();
    builder.append(productCode.toUpperCase());
    for (int group = 0; group < 3; group++) {
      builder.append('-');
      for (int i = 0; i < 4; i++) {
        builder.append(ALPHABET[random.nextInt(ALPHABET.length)]);
      }
    }
    return builder.toString();
  }

  public String hashKey(String licenseKey) {
    String secret = properties.getKeyHashSecret();
    if (secret == null || secret.isBlank()) {
      throw new LicensingBadRequestException("Licensing key hash secret is not configured.");
    }
    try {
      Mac mac = Mac.getInstance(HMAC_ALGO);
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
      byte[] hashed = mac.doFinal(licenseKey.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(hashed);
    } catch (Exception ex) {
      throw new LicensingBadRequestException("Failed to hash license key.");
    }
  }
}
