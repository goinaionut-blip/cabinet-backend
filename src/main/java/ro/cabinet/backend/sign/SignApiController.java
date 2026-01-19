package ro.cabinet.backend.sign;

import java.time.OffsetDateTime;

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
    SignSession session = signService.createSession(
        requestBody.documentId(),
        requestBody.patientId(),
        requestBody.ttlMinutes()
    );
    String signUrl = signService.buildPublicUrl(request, "/sign?t=" + session.getToken());
    String uploadUrl = signService.buildPublicUrl(request, "/upload?t=" + session.getToken());
    return new CreateSignSessionResponse(
        session.getToken(),
        session.getStatus(),
        session.getExpiresAt(),
        signUrl,
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
    if (filename == null || filename.isBlank()) {
      filename = "document-signed.pdf";
    }
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
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

  public record CreateSignSessionRequest(String documentId, String patientId, Integer ttlMinutes) {
  }

  public record CreateSignSessionResponse(String token, SignSessionStatus status,
                                          OffsetDateTime expiresAt, String signUrl,
                                          String uploadUrl) {
  }

  public record SignSessionStatusResponse(String token, SignSessionStatus status,
                                          OffsetDateTime expiresAt, boolean signedAvailable,
                                          String originalFilename) {
  }
}
