package ro.cabinet.backend.sign;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SignService {
  private final SignSessionRepository repository;
  private final SignProperties properties;
  private final StorageService storageService;
  private final SignTemplateRegistry templateRegistry;
  private final ObjectMapper objectMapper;

  public SignService(SignSessionRepository repository, SignProperties properties,
                     StorageService storageService, SignTemplateRegistry templateRegistry,
                     ObjectMapper objectMapper) {
    this.repository = repository;
    this.properties = properties;
    this.storageService = storageService;
    this.templateRegistry = templateRegistry;
    this.objectMapper = objectMapper;
  }

  public SignSession createSession(String documentId, String patientId, Integer ttlMinutes) {
    return createSession(documentId, patientId, ttlMinutes, null);
  }

  public SignSession createSession(String documentId, String patientId, Integer ttlMinutes, SignTemplateId templateId) {
    return createSession(documentId, patientId, ttlMinutes, templateId, null);
  }

  public SignSession createSession(String documentId, String patientId, Integer ttlMinutes,
                                   SignTemplateId templateId, Map<String, Object> formData) {
    if (documentId == null || documentId.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DocumentId lipsa");
    }
    UUID id = UUID.randomUUID();
    String token = UUID.randomUUID().toString().replace("-", "");
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    int ttlValue = ttlMinutes != null ? ttlMinutes : properties.getTokenTtlMinutes();
    if (ttlValue <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ttlMinutes invalid");
    }
    OffsetDateTime expiresAt = now.plusMinutes(ttlValue);

    SignSession session = new SignSession();
    session.setId(id);
    session.setToken(token);
    session.setStatus(SignSessionStatus.CREATED);
    session.setDocumentId(documentId);
    session.setPatientId(patientId);
    session.setTemplateId(templateId);
    session.setFormData(serializeFormData(formData));
    session.setCreatedAt(now);
    session.setExpiresAt(expiresAt);
    return repository.save(session);
  }

  public SignSession getSession(UUID id) {
    return repository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sesiune inexistenta"));
  }

  public SignSession getSessionByToken(String token) {
    SignSession session = repository.findByToken(token)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Token invalid"));
    validateActive(session);
    return session;
  }

  public void validateActive(SignSession session) {
    if (session.getStatus() == SignSessionStatus.EXPIRED) {
      throw new ResponseStatusException(HttpStatus.GONE, "Token expirat");
    }
    if (OffsetDateTime.now(ZoneOffset.UTC).isAfter(session.getExpiresAt())) {
      session.setStatus(SignSessionStatus.EXPIRED);
      repository.save(session);
      throw new ResponseStatusException(HttpStatus.GONE, "Token expirat");
    }
  }

  public SignSession refreshStatus(SignSession session) {
    if (OffsetDateTime.now(ZoneOffset.UTC).isAfter(session.getExpiresAt())) {
      if (session.getStatus() != SignSessionStatus.EXPIRED) {
        session.setStatus(SignSessionStatus.EXPIRED);
        repository.save(session);
      }
    }
    return session;
  }

  public Resource loadIncoming(SignSession session) {
    return storageService.loadIncoming(session.getOriginalPath());
  }

  public Resource loadSigned(SignSession session) {
    return storageService.loadSigned(session.getSignedPath());
  }

  public void saveSigned(SignSession session, MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fisier PDF lipsa");
    }
    String signedFilename = Optional.ofNullable(file.getOriginalFilename()).orElse("document-signed.pdf");
    signedFilename = sanitizeFilename(signedFilename);
    String signedSha256;
    String signedPath;
    try {
      StorageService.StorageResult result = storageService.saveSigned(
          session.getDocumentId(),
          signedFilename,
          file.getInputStream()
      );
      signedSha256 = result.sha256();
      signedPath = result.path();
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Nu pot salva fisierul semnat");
    }
    session.setSignedPath(signedPath);
    session.setSignedFilename(signedFilename);
    session.setSignedContentType(file.getContentType());
    session.setSignedSha256(signedSha256);
    session.setStatus(SignSessionStatus.SIGNED_UPLOADED);
    repository.save(session);
  }

  public void saveSignedFromWeb(SignSession session, Map<String, Object> formData,
                                String signaturePngBase64) {
    if (session.getTemplateId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template lipsa");
    }
    byte[] signatureBytes = decodePng(signaturePngBase64);
    SignTemplateRegistry.TemplateDefinition template = templateRegistry.getTemplate(session.getTemplateId());
    byte[] pdfBytes = generateSignedPdf(template, formData, signatureBytes);
    String signedFilename = buildSignedFilename(session.getOriginalFilename(), session.getDocumentId());
    try (InputStream inputStream = new ByteArrayInputStream(pdfBytes)) {
      StorageService.StorageResult result = storageService.saveSigned(
          session.getDocumentId(),
          signedFilename,
          inputStream
      );
      session.setSignedPath(result.path());
      session.setSignedFilename(signedFilename);
      session.setSignedContentType("application/pdf");
      session.setSignedSha256(result.sha256());
      session.setStatus(SignSessionStatus.SIGNED_UPLOADED);
      repository.save(session);
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Nu pot salva PDF-ul semnat");
    }
  }

  public void saveOriginal(SignSession session, MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fisier PDF lipsa");
    }
    String originalFilename = Optional.ofNullable(file.getOriginalFilename()).orElse("document.pdf");
    originalFilename = sanitizeFilename(originalFilename);
    String originalSha256;
    String originalPath;
    try {
      StorageService.StorageResult result = storageService.saveIncoming(
          session.getDocumentId(),
          originalFilename,
          file.getInputStream()
      );
      originalSha256 = result.sha256();
      originalPath = result.path();
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Nu pot salva fisierul original");
    }
    session.setOriginalPath(originalPath);
    session.setOriginalFilename(originalFilename);
    session.setOriginalContentType(file.getContentType());
    session.setOriginalSha256(originalSha256);
    session.setStatus(SignSessionStatus.ORIGINAL_UPLOADED);
    repository.save(session);
  }

  public void markDone(SignSession session) {
    if (session.getSignedPath() == null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Fisierul semnat nu este incarcat");
    }
    session.setDownloadedAt(OffsetDateTime.now(ZoneOffset.UTC));
    session.setStatus(SignSessionStatus.DOWNLOADED);
    repository.save(session);
  }

  public void deleteSession(SignSession session) {
    deleteFileIfExists(session.getOriginalPath());
    deleteFileIfExists(session.getSignedPath());
    session.setStatus(SignSessionStatus.EXPIRED);
    repository.save(session);
  }

  private void deleteFileIfExists(String pathValue) {
    if (pathValue == null || pathValue.isBlank()) {
      return;
    }
    try {
      java.nio.file.Files.deleteIfExists(java.nio.file.Path.of(pathValue));
    } catch (java.io.IOException ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Nu pot sterge fisierul");
    }
  }

  private String sanitizeFilename(String value) {
    return value.replaceAll("[\\r\\n]", "_");
  }

  private byte[] generateSignedPdf(SignTemplateRegistry.TemplateDefinition template,
                                   Map<String, Object> formData,
                                   byte[] signatureBytes) {
    try (InputStream inputStream = templateRegistry.openTemplate(template.id());
         PDDocument document = PDDocument.load(inputStream);
         ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      PDAcroForm form = document.getDocumentCatalog().getAcroForm();
      if (form != null && formData != null) {
        form.setNeedAppearances(true);
        for (Map.Entry<String, Object> entry : formData.entrySet()) {
          if (entry.getKey() == null || entry.getKey().isBlank()) {
            continue;
          }
          PDField field = form.getField(entry.getKey());
          if (field == null) {
            continue;
          }
          Object value = entry.getValue();
          if (field instanceof PDCheckBox checkbox) {
            boolean checked = toBoolean(value);
            if (checked) {
              checkbox.check();
            } else {
              checkbox.unCheck();
            }
          } else if (value != null) {
            field.setValue(String.valueOf(value));
          }
        }
      }

      SignTemplateRegistry.SignaturePlacement placement = template.signaturePlacement();
      PDPage page = document.getPage(placement.pageIndex());
      PDImageXObject signatureImage = PDImageXObject.createFromByteArray(document, signatureBytes, "signature");
      try (PDPageContentStream contentStream = new PDPageContentStream(
          document, page, AppendMode.APPEND, true, true)) {
        contentStream.drawImage(signatureImage, placement.x(), placement.y(),
            placement.width(), placement.height());
      }

      if (form != null) {
        form.flatten();
      }
      document.save(outputStream);
      return outputStream.toByteArray();
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Nu pot genera PDF-ul semnat");
    }
  }

  private byte[] decodePng(String signaturePngBase64) {
    if (signaturePngBase64 == null || signaturePngBase64.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Semnatura lipsa");
    }
    String trimmed = signaturePngBase64.trim();
    String prefix = "base64,";
    int idx = trimmed.indexOf(prefix);
    String encoded = idx >= 0 ? trimmed.substring(idx + prefix.length()) : trimmed;
    try {
      return Base64.getDecoder().decode(encoded);
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Semnatura invalida");
    }
  }

  private boolean toBoolean(Object value) {
    if (value == null) {
      return false;
    }
    if (value instanceof Boolean bool) {
      return bool;
    }
    String text = String.valueOf(value).trim();
    return "true".equalsIgnoreCase(text) || "1".equals(text) || "yes".equalsIgnoreCase(text);
  }

  private String buildSignedFilename(String originalFilename, String documentId) {
    if (originalFilename != null && !originalFilename.isBlank()) {
      String cleaned = originalFilename.replace("\"", "").trim();
      if (!cleaned.isBlank()) {
        String withoutExt = cleaned.replaceAll("(?i)\\.pdf$", "");
        if (!withoutExt.isBlank()) {
          return withoutExt + "_signed.pdf";
        }
      }
    }
    String fallback = (documentId == null || documentId.isBlank()) ? "document_signed" : documentId + "_signed";
    return fallback + ".pdf";
  }

  private String serializeFormData(Map<String, Object> formData) {
    if (formData == null || formData.isEmpty()) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(formData);
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Form data invalid");
    }
  }

  @Scheduled(fixedDelay = 300000)
  public void cleanupExpiredSessions() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    OffsetDateTime cutoff = now.minusHours(properties.getCleanupHours());

    List<SignSession> expiredByStatus = repository.findByStatus(SignSessionStatus.EXPIRED);
    List<SignSession> expiredByTime = repository.findByExpiresAtBefore(now);
    List<SignSession> staleByTime = repository.findByUpdatedAtBefore(cutoff);

    LinkedHashMap<UUID, SignSession> candidates = new LinkedHashMap<>();
    expiredByStatus.forEach(session -> candidates.put(session.getId(), session));
    expiredByTime.forEach(session -> candidates.put(session.getId(), session));
    staleByTime.forEach(session -> candidates.put(session.getId(), session));

    for (SignSession session : candidates.values()) {
      if (session.getStatus() != SignSessionStatus.EXPIRED) {
        session.setStatus(SignSessionStatus.EXPIRED);
      }
      deleteFileIfExists(session.getOriginalPath());
      deleteFileIfExists(session.getSignedPath());
      repository.save(session);
    }
  }

  public String buildPublicUrl(HttpServletRequest request, String path) {
    String baseUrl = properties.getBaseUrl();
    if (baseUrl == null || baseUrl.isBlank()) {
      baseUrl = properties.getPublicBaseUrl();
    }
    if (baseUrl == null || baseUrl.isBlank()) {
      baseUrl = resolveBaseUrl(request);
    }
    if (baseUrl.endsWith("/")) {
      baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
    }
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    return baseUrl + path;
  }

  private String resolveBaseUrl(HttpServletRequest request) {
    String forwardedProto = request.getHeader("X-Forwarded-Proto");
    String forwardedHost = request.getHeader("X-Forwarded-Host");
    String forwardedPort = request.getHeader("X-Forwarded-Port");

    String scheme = (forwardedProto != null && !forwardedProto.isBlank())
        ? forwardedProto
        : request.getScheme();

    String host = (forwardedHost != null && !forwardedHost.isBlank())
        ? forwardedHost
        : request.getServerName();

    boolean hostHasPort = host.contains(":");
    String portValue = (forwardedPort != null && !forwardedPort.isBlank())
        ? forwardedPort
        : String.valueOf(request.getServerPort());

    boolean isStandardPort = ("http".equalsIgnoreCase(scheme) && "80".equals(portValue))
        || ("https".equalsIgnoreCase(scheme) && "443".equals(portValue));

    if (!hostHasPort && !isStandardPort) {
      host = host + ":" + portValue;
    }

    return scheme + "://" + host;
  }
}
