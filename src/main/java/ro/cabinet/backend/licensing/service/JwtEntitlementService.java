package ro.cabinet.backend.licensing.service;

import ro.cabinet.backend.licensing.config.LicensingProperties;
import ro.cabinet.backend.licensing.exception.LicensingBadRequestException;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Service;

@Service
public class JwtEntitlementService {
  private final LicensingProperties properties;
  private final Clock clock;
  private volatile PrivateKey privateKey;
  private volatile PublicKey publicKey;

  public JwtEntitlementService(LicensingProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
  }

  public String issueToken(String installId, String productCode, EntitlementStatus status,
                           Instant trialStart, Instant trialEnd, int offlineGraceHours,
                           Optional<String> licenseId) {
    PrivateKey key = loadPrivateKey();
    SignatureAlgorithm algorithm = resolveAlgorithm(key);
    Instant now = Instant.now(clock);
    Instant exp = now.plus(properties.getEntitlementTtlHours(), ChronoUnit.HOURS);

    io.jsonwebtoken.JwtBuilder builder = Jwts.builder()
        .setIssuer(required(properties.getJwtIssuer(), "Licensing JWT issuer is not configured."))
        .setSubject(installId)
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(exp))
        .claim("product", productCode)
        .claim("status", status.name())
        .claim("trialStart", trialStart.toString())
        .claim("trialEnd", trialEnd.toString())
        .claim("offlineGraceHours", offlineGraceHours);

    licenseId.ifPresent(id -> builder.claim("licenseId", id));

    return builder.signWith(key, algorithm).compact();
  }

  public PublicKey getPublicKey() {
    return loadPublicKey();
  }

  private PrivateKey loadPrivateKey() {
    if (privateKey == null) {
      synchronized (this) {
        if (privateKey == null) {
          String pem = required(properties.getJwtPrivateKeyPem(),
              "Licensing JWT private key is not configured.");
          privateKey = parsePrivateKey(pem);
        }
      }
    }
    return privateKey;
  }

  private PublicKey loadPublicKey() {
    if (publicKey == null) {
      synchronized (this) {
        if (publicKey == null) {
          String pem = required(properties.getJwtPublicKeyPem(),
              "Licensing JWT public key is not configured.");
          publicKey = parsePublicKey(pem);
        }
      }
    }
    return publicKey;
  }

  private SignatureAlgorithm resolveAlgorithm(PrivateKey key) {
    if (key instanceof RSAPrivateKey) {
      return SignatureAlgorithm.RS256;
    }
    if (key instanceof ECPrivateKey) {
      return SignatureAlgorithm.ES256;
    }
    throw new LicensingBadRequestException("Unsupported JWT private key type.");
  }

  private PrivateKey parsePrivateKey(String pem) {
    try {
      byte[] decoded = decodePem(pem);
      PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
      try {
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
      } catch (Exception ignored) {
        return KeyFactory.getInstance("EC").generatePrivate(spec);
      }
    } catch (Exception ex) {
      throw new LicensingBadRequestException("Invalid JWT private key PEM.");
    }
  }

  private PublicKey parsePublicKey(String pem) {
    try {
      byte[] decoded = decodePem(pem);
      X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
      try {
        return KeyFactory.getInstance("RSA").generatePublic(spec);
      } catch (Exception ignored) {
        return KeyFactory.getInstance("EC").generatePublic(spec);
      }
    } catch (Exception ex) {
      throw new LicensingBadRequestException("Invalid JWT public key PEM.");
    }
  }

  private byte[] decodePem(String pem) {
    String cleaned = pem.replace("-----BEGIN PRIVATE KEY-----", "")
        .replace("-----END PRIVATE KEY-----", "")
        .replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")
        .replaceAll("\\s", "");
    return Base64.getDecoder().decode(cleaned);
  }

  private String required(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new LicensingBadRequestException(message);
    }
    return value;
  }
}
