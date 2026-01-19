package ro.cabinet.backend.sign;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class SignPublicController {
  private final SignService signService;

  public SignPublicController(SignService signService) {
    this.signService = signService;
  }

  @GetMapping("/sign")
  public ResponseEntity<Resource> downloadOriginal(@RequestParam("t") String token) {
    SignSession session = signService.getSessionByToken(token);
    Resource resource = signService.loadIncoming(session);
    String filename = session.getOriginalFilename();
    if (filename == null || filename.isBlank()) {
      filename = "document.pdf";
    }
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
        .body(resource);
  }

  @GetMapping(value = "/upload", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<byte[]> uploadPage(@RequestParam("t") String token) {
    signService.getSessionByToken(token);
    String html = """
        <!doctype html>
        <html lang=\"ro\">
        <head>
          <meta charset=\"utf-8\">
          <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">
          <title>Incarcare PDF semnat</title>
        </head>
        <body>
          <h2>Incarcare PDF semnat</h2>
          <p>Alege fisierul PDF semnat si apasa Incarca.</p>
          <form action=\"/upload?t=%s\" method=\"post\" enctype=\"multipart/form-data\">
            <input type=\"file\" name=\"file\" accept=\"application/pdf\" required />
            <button type=\"submit\">Incarca</button>
          </form>
        </body>
        </html>
        """.formatted(token);
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_HTML)
        .body(html.getBytes(StandardCharsets.UTF_8));
  }

  @PostMapping("/upload")
  public ResponseEntity<Void> uploadSigned(@RequestParam("t") String token,
                                           @RequestParam("file") MultipartFile file) {
    SignSession session = signService.getSessionByToken(token);
    signService.saveSigned(session, file);
    return ResponseEntity.status(302)
        .header(HttpHeaders.LOCATION, "/done?t=" + token)
        .build();
  }

  @GetMapping(value = "/done", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<byte[]> donePage(@RequestParam("t") String token) {
    signService.getSessionByToken(token);
    String html = """
        <!doctype html>
        <html lang=\"ro\">
        <head>
          <meta charset=\"utf-8\">
          <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">
          <title>Multumim</title>
        </head>
        <body>
          <h2>Multumim!</h2>
          <p>Documentul semnat a fost incarcat.</p>
        </body>
        </html>
        """;
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_HTML)
        .body(html.getBytes(StandardCharsets.UTF_8));
  }
}
