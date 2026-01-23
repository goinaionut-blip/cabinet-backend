package ro.stoma.efactura.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ro.stoma.efactura.client.AnafEfacturaClient;
import ro.stoma.efactura.config.EfacturaProperties;
import ro.stoma.efactura.oauth.AnafOAuthService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EfacturaService {
  private static final Logger log = LoggerFactory.getLogger(EfacturaService.class);
  private static final Pattern INDEX_PATTERN =
      Pattern.compile("(?:index_incarcare|indexIncarcare)\\s*[:=]\\s*\"?([\\w-]+)\"?",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern INDEX_XML_PATTERN =
      Pattern.compile("<(?:index_incarcare|indexIncarcare)>([^<]+)</", Pattern.CASE_INSENSITIVE);
  private final AnafEfacturaClient client;
  private final AnafOAuthService oauthService;
  private final ObjectMapper objectMapper;
  private final EfacturaProperties properties;

  public EfacturaService(AnafEfacturaClient client,
                         AnafOAuthService oauthService,
                         ObjectMapper objectMapper,
                         EfacturaProperties properties) {
    this.client = client;
    this.oauthService = oauthService;
    this.objectMapper = objectMapper;
    this.properties = properties;
  }

  public String uploadInvoice(byte[] xml, String cif) {
    String resolvedCif = resolveCif(cif);
    String accessToken = oauthService.getValidAccessToken(resolvedCif);
    String response = client.uploadInvoice(xml, accessToken, resolvedCif);
    return extractIndexIncarcare(response);
  }

  public String getStatus(String indexIncarcare, String cif) {
    String accessToken = oauthService.getValidAccessToken(resolveCif(cif));
    return client.getStatus(indexIncarcare, accessToken);
  }

  public String listMessages(Integer days, String cif) {
    String resolvedCif = resolveCif(cif);
    log.info("ANAF env={} cif={}", properties.getEnvironment(), resolvedCif);
    String accessToken = oauthService.getValidAccessToken(resolvedCif);
    return client.listMessages(accessToken, days, resolvedCif);
  }

  public byte[] download(String id, String cif) {
    String accessToken = oauthService.getValidAccessToken(resolveCif(cif));
    return client.download(id, accessToken);
  }

  private String resolveCif(String cif) {
    if (cif != null && !cif.isBlank()) {
      return cif.trim();
    }
    String fallback = properties.getCif();
    if (fallback == null || fallback.isBlank()) {
      throw new IllegalArgumentException(
          "CIF lipseste. Trimite headerul X-EFACTURA-CIF sau parametrul 'cif'.");
    }
    return fallback;
  }

  private String extractIndexIncarcare(String response) {
    if (response == null || response.isBlank()) {
      throw new IllegalStateException("ANAF upload response was empty.");
    }
    try {
      JsonNode node = objectMapper.readTree(response);
      JsonNode value = node.get("index_incarcare");
      if (value == null) {
        value = node.get("indexIncarcare");
      }
      if (value != null && !value.isNull()) {
        return value.asText();
      }
      JsonNode error = node.get("eroare");
      if (error == null) {
        error = node.get("error");
      }
      if (error == null) {
        error = node.get("message");
      }
      if (error != null && !error.isNull()) {
        throw new IllegalStateException("ANAF upload error: " + error.asText());
      }
    } catch (IOException ex) {
      // If ANAF changes format, return raw response for troubleshooting.
    }
    String trimmed = response.trim();
    Matcher jsonMatcher = INDEX_PATTERN.matcher(trimmed);
    if (jsonMatcher.find()) {
      return jsonMatcher.group(1);
    }
    Matcher xmlMatcher = INDEX_XML_PATTERN.matcher(trimmed);
    if (xmlMatcher.find()) {
      return xmlMatcher.group(1).trim();
    }
    String snippet = trimmed.length() > 500 ? trimmed.substring(0, 500) + "..." : trimmed;
    throw new IllegalStateException("ANAF upload response missing index_incarcare. Response: " + snippet);
  }
}
