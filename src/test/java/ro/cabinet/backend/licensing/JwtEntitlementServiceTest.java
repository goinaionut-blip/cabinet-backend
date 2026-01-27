package ro.cabinet.backend.licensing;

import ro.cabinet.backend.licensing.config.LicensingProperties;
import ro.cabinet.backend.licensing.service.EntitlementStatus;
import ro.cabinet.backend.licensing.service.JwtEntitlementService;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JwtEntitlementServiceTest {
  @Test
  void tokenContainsExpectedClaims() throws Exception {
    KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair keyPair = generator.generateKeyPair();

    LicensingProperties properties = new LicensingProperties();
    properties.setJwtIssuer("licensing-test");
    properties.setJwtPrivateKeyPem(toPem("PRIVATE KEY", keyPair.getPrivate().getEncoded()));
    properties.setJwtPublicKeyPem(toPem("PUBLIC KEY", keyPair.getPublic().getEncoded()));
    properties.setEntitlementTtlHours(24);

    Instant fixedNow = Instant.parse("2026-01-27T08:10:00Z");
    Clock clock = Clock.fixed(fixedNow, ZoneOffset.UTC);

    JwtEntitlementService service = new JwtEntitlementService(properties, clock);

    String token = service.issueToken(
        "install-1",
        "DENTRX",
        EntitlementStatus.TRIAL_ACTIVE,
        fixedNow,
        fixedNow.plusSeconds(3600),
        72,
        Optional.empty());

    Claims claims = Jwts.parserBuilder()
        .setSigningKey(service.getPublicKey())
        .build()
        .parseClaimsJws(token)
        .getBody();

    assertEquals("licensing-test", claims.getIssuer());
    assertEquals("install-1", claims.getSubject());
    assertEquals("DENTRX", claims.get("product", String.class));
    assertEquals("TRIAL_ACTIVE", claims.get("status", String.class));
    assertEquals("72", claims.get("offlineGraceHours").toString());
    assertNotNull(claims.getExpiration());
    assertEquals(Date.from(fixedNow), claims.getIssuedAt());
  }

  private String toPem(String type, byte[] encoded) {
    String base64 = Base64.getEncoder().encodeToString(encoded);
    StringBuilder builder = new StringBuilder();
    builder.append("-----BEGIN ").append(type).append("-----\n");
    for (int i = 0; i < base64.length(); i += 64) {
      int end = Math.min(base64.length(), i + 64);
      builder.append(base64, i, end).append("\n");
    }
    builder.append("-----END ").append(type).append("-----\n");
    return builder.toString();
  }
}
