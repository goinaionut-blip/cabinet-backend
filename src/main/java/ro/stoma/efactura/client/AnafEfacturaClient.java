package ro.stoma.efactura.client;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import ro.stoma.efactura.config.EfacturaProperties;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class AnafEfacturaClient {
  private final EfacturaProperties properties;
  private final RestTemplate restTemplate;

  public AnafEfacturaClient(EfacturaProperties properties, RestTemplateBuilder builder) {
    this.properties = properties;
    this.restTemplate = builder
        .setConnectTimeout(properties.getApi().getTimeout())
        .setReadTimeout(properties.getApi().getTimeout())
        .build();
    this.restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
      @Override
      public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
        return false;
      }
    });
  }

  public String uploadInvoice(byte[] xml, String accessToken) {
    Assert.notNull(xml, "xml is required");
    ResponseEntity<byte[]> response = executeWithRetry(() -> {
      String url = buildUrl(properties.getApi().getUploadPath(), Map.of("cif", properties.getCif()));
      HttpHeaders headers = bearerHeaders(accessToken);
      // ANAF specific: upload expects UBL XML payload.
      headers.setContentType(MediaType.APPLICATION_XML);
      ResponseEntity<byte[]> entity = restTemplate.exchange(
          url, HttpMethod.POST, new HttpEntity<>(xml, headers), byte[].class);
      return entity;
    }, "upload");
    return bodyAsString(response);
  }

  public String getStatus(String indexIncarcare, String accessToken) {
    ResponseEntity<byte[]> response = executeWithRetry(() -> {
      String url = buildUrl(properties.getApi().getStatusPath(), Map.of("index", indexIncarcare));
      HttpHeaders headers = bearerHeaders(accessToken);
      ResponseEntity<byte[]> entity = restTemplate.exchange(
          url, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
      return entity;
    }, "status");
    return bodyAsString(response);
  }

  public String listMessages(String accessToken, Integer days) {
    ResponseEntity<byte[]> response = executeWithRetry(() -> {
      int listDays = days == null ? properties.getApi().getListDays() : days;
      String url = UriComponentsBuilder.fromHttpUrl(properties.resolveApiBase())
          .path(properties.getApi().getListPath())
          // ANAF specific: listaMesaje supports cif; include it so we list invoices for this CIF.
          .queryParam("cif", properties.getCif())
          .queryParam("zile", listDays)
          .build()
          .toUriString();
      HttpHeaders headers = bearerHeaders(accessToken);
      ResponseEntity<byte[]> entity = restTemplate.exchange(
          url, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
      return entity;
    }, "list");
    return bodyAsString(response);
  }

  public byte[] download(String id, String accessToken) {
    ResponseEntity<byte[]> response = executeWithRetry(() -> {
      String url = buildUrl(properties.getApi().getDownloadPath(), Map.of("id", id));
      HttpHeaders headers = bearerHeaders(accessToken);
      ResponseEntity<byte[]> entity = restTemplate.exchange(
          url, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
      return entity;
    }, "download");
    return response.getBody();
  }

  private ResponseEntity<byte[]> executeWithRetry(RequestSupplier supplier, String operation) {
    int attempts = Math.max(0, properties.getApi().getMaxRetries()) + 1;
    Duration delay = properties.getApi().getRetryDelay();
    for (int i = 0; i < attempts; i++) {
      ResponseEntity<byte[]> response = supplier.get();
      HttpStatusCode status = response.getStatusCode();
      if (status.is2xxSuccessful()) {
        return response;
      }
      if (!shouldRetry(status) || i == attempts - 1) {
        throw new AnafApiException(operation, status.value(), bodyAsString(response));
      }
      sleep(delay);
    }
    throw new AnafApiException(operation, 500, "Unexpected retry loop exit");
  }

  private boolean shouldRetry(HttpStatusCode status) {
    return status.value() == 429 || status.is5xxServerError();
  }

  private void sleep(Duration delay) {
    if (delay == null || delay.isZero() || delay.isNegative()) {
      return;
    }
    try {
      Thread.sleep(delay.toMillis());
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  private HttpHeaders bearerHeaders(String accessToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    return headers;
  }

  private String buildUrl(String path, Map<String, ?> params) {
    String baseUrl = properties.resolveApiBase();
    // ANAF specific: these paths include query params; keep them here so API changes are isolated.
    return UriComponentsBuilder.fromHttpUrl(baseUrl)
        .path(path)
        .buildAndExpand(params)
        .toUriString();
  }

  private String bodyAsString(ResponseEntity<byte[]> response) {
    byte[] body = response.getBody();
    if (body == null) {
      return "";
    }
    return new String(body, StandardCharsets.UTF_8);
  }

  private interface RequestSupplier {
    ResponseEntity<byte[]> get();
  }
}
