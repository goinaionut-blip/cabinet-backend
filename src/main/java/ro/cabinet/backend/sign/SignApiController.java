package ro.cabinet.backend.sign;

import java.time.OffsetDateTime;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/sign")
public class SignApiController {
  private final SignService signService;

  public SignApiController(SignService signService) {
    this.signService = signService;
  }

  @PostMapping("/sessions")
  public CreateSignSessionResponse createSession(@RequestBody CreateSignSessionRequest requestBody,
                                                 HttpServletRequest request) {
    if (requestBody == null) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, "Request body lipsa");
    }
    SignTemplateId templateId = SignTemplateId.fromString(requestBody.templateId());
    if (requestBody.templateId() != null && templateId == null) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, "Template invalid");
    }
    SignSession session = signService.createSession(
        requestBody.documentId(),
        requestBody.patientId(),
        requestBody.ttlMinutes(),
        templateId
    );
    String signUrl = signService.buildPublicUrl(request, "/sign?token=" + session.getToken());
    String signWebUrl = signService.buildPublicUrl(request, "/sign-web?token=" + session.getToken());
    String uploadUrl = signService.buildPublicUrl(request, "/upload?token=" + session.getToken());
    return new CreateSignSessionResponse(
        session.getToken(),
        session.getStatus(),
        session.getExpiresAt(),
        signUrl,
        signWebUrl,
        uploadUrl
    );
  }

  @PostMapping(value = "/sessions/{token}/original", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public SignSessionStatusResponse uploadOriginal(@RequestParam("file") MultipartFile file,
                                                  @PathVariable("token") String token) {
    SignSession session = signService.getSessionByToken(token);
    signService.saveOriginal(session, file);
    return toStatusResponse(session);
  }

  @GetMapping("/sessions/{token}/status")
  public SignSessionStatusResponse getStatus(@PathVariable("token") String token) {
    SignSession session = signService.refreshStatus(signService.getSessionByToken(token));
    return toStatusResponse(session);
  }

  @GetMapping("/sessions/{token}/signed")
  public ResponseEntity<Resource> downloadSigned(@PathVariable("token") String token) {
    SignSession session = signService.refreshStatus(signService.getSessionByToken(token));
    if (session.getStatus() != SignSessionStatus.SIGNED_UPLOADED) {
      return ResponseEntity.status(409).build();
    }
    Resource resource = signService.loadSigned(session);
    String filename = session.getOriginalFilename();
    filename = buildSignedFilename(filename);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
        .header(HttpHeaders.PRAGMA, "no-cache")
        .header("X-Content-Type-Options", "nosniff")
        .header(HttpHeaders.CONTENT_DISPOSITION, SignPublicController.contentDispositionAttachment(filename))
        .body(resource);
  }

  @PostMapping("/sessions/{token}/ack-download")
  public SignSessionStatusResponse acknowledgeDownload(@PathVariable("token") String token) {
    SignSession session = signService.refreshStatus(signService.getSessionByToken(token));
    signService.markDone(session);
    return toStatusResponse(session);
  }

  @DeleteMapping("/sessions/{token}/files")
  public ResponseEntity<Void> deleteFiles(@PathVariable("token") String token) {
    SignSession session = signService.refreshStatus(signService.getSessionByToken(token));
    signService.deleteSession(session);
    return ResponseEntity.noContent().build();
  }

  private SignSessionStatusResponse toStatusResponse(SignSession session) {
    boolean signedAvailable = session.getSignedPath() != null;
    return new SignSessionStatusResponse(
        session.getToken(),
        session.getStatus(),
        session.getExpiresAt(),
        signedAvailable,
        session.getOriginalFilename()
    );
  }

  public record CreateSignSessionRequest(String documentId, String patientId, Integer ttlMinutes,
                                         String templateId, Map<String, Object> formData) {
  }

  public record CreateSignSessionResponse(String token, SignSessionStatus status,
                                          OffsetDateTime expiresAt, String signUrl,
                                          String signWebUrl,
                                          String uploadUrl) {
  }

  public record SignSessionStatusResponse(String token, SignSessionStatus status,
                                          OffsetDateTime expiresAt, boolean signedAvailable,
                                          String originalFilename) {
  }

  private String buildSignedFilename(String originalFilename) {
    if (originalFilename == null || originalFilename.isBlank()) {
      return "document_signed.pdf";
    }
    String cleaned = originalFilename.replace("\"", "").trim();
    if (cleaned.isBlank()) {
      return "document_signed.pdf";
    }
    String withoutExt = cleaned.replaceAll("(?i)\\.pdf$", "");
    if (withoutExt.isBlank()) {
      return "document_signed.pdf";
    }
    return withoutExt + "_signed.pdf";
  }
}
