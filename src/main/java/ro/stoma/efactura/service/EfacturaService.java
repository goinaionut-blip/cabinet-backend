package ro.stoma.efactura.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

import ro.stoma.efactura.client.AnafEfacturaClient;
import ro.stoma.efactura.oauth.AnafOAuthService;

import org.springframework.stereotype.Service;

@Service
public class EfacturaService {
  private final AnafEfacturaClient client;
  private final AnafOAuthService oauthService;
  private final ObjectMapper objectMapper;

  public EfacturaService(AnafEfacturaClient client, AnafOAuthService oauthService, ObjectMapper objectMapper) {
    this.client = client;
    this.oauthService = oauthService;
    this.objectMapper = objectMapper;
  }

  public String uploadInvoice(byte[] xml) {
    String accessToken = oauthService.getValidAccessToken();
    String response = client.uploadInvoice(xml, accessToken);
    return extractIndexIncarcare(response);
  }

  public String getStatus(String indexIncarcare) {
    String accessToken = oauthService.getValidAccessToken();
    return client.getStatus(indexIncarcare, accessToken);
  }

  public String listMessages(Integer days) {
    String accessToken = oauthService.getValidAccessToken();
    return client.listMessages(accessToken, days);
  }

  public byte[] download(String id) {
    String accessToken = oauthService.getValidAccessToken();
    return client.download(id, accessToken);
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
    } catch (IOException ex) {
      // If ANAF changes format, return raw response for troubleshooting.
    }
    throw new IllegalStateException("ANAF upload response missing index_incarcare.");
  }
}
