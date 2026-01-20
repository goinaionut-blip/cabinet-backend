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

  @GetMapping(value = "/sign", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<byte[]> signPage(@RequestParam("token") String token) {
    signService.getSessionByToken(token);
    String html = """
        <!doctype html>
        <html lang=\"ro\">
        <head>
          <meta charset=\"utf-8\">
          <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">
          <title>Semnare document</title>
          <style>
            :root { color-scheme: light; }
            body {
              font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Arial, sans-serif;
              margin: 0;
              padding: 24px;
              background: #f7f7f9;
              color: #111;
            }
            .card {
              max-width: 520px;
              margin: 0 auto;
              background: #fff;
              border-radius: 12px;
              padding: 20px;
              box-shadow: 0 2px 10px rgba(0,0,0,0.08);
            }
            .badge {
              display: inline-block;
              font-size: 12px;
              font-weight: 700;
              padding: 4px 8px;
              border-radius: 6px;
              background: #e3f2fd;
              color: #0d47a1;
              margin-left: 6px;
              vertical-align: middle;
            }
            .btn {
              display: block;
              width: 100%;
              text-align: center;
              padding: 16px 20px;
              font-size: 18px;
              border-radius: 10px;
              background: #1b5e20;
              color: #fff;
              text-decoration: none;
              margin: 12px 0 20px;
            }
            .btn.secondary {
              background: #1565c0;
            }
            .divider {
              border: none;
              height: 1px;
              background: #e0e0e0;
              margin: 16px 0;
            }
            p { line-height: 1.4; }
          </style>
        </head>
        <body>
          <div class=\"card\">
            <h2>Semnare document</h2>
            <p><strong>Pasul 1:</strong> descarca PDF-ul si semneaza-l in Adobe Acrobat Reader.</p>
            <a class=\"btn\" href=\"/sign/pdf?token=%s\">Descarca PDF <span class=\"badge\">PDF</span></a>
            <p style=\"font-size: 13px; color: #444; margin-top: -8px;\">Daca nu se deschide automat, alege „Open with Adobe Acrobat Reader”.</p>
            <hr class=\"divider\" />
            <p><strong>Pasul 2:</strong> incarca fisierul semnat aici:</p>
            <a class=\"btn secondary\" href=\"/upload?token=%s\">Incarca PDF semnat</a>
          </div>
        </body>
        </html>
        """.formatted(token, token);
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_HTML)
        .body(html.getBytes(StandardCharsets.UTF_8));
  }

  @GetMapping("/sign/pdf")
  public ResponseEntity<Resource> downloadOriginal(@RequestParam("token") String token) {
    SignSession session = signService.getSessionByToken(token);
    Resource resource = signService.loadIncoming(session);
    String filename = session.getOriginalFilename();
    if (filename == null || filename.isBlank()) {
      filename = "document.pdf";
    }
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .body(resource);
  }

  @GetMapping(value = "/upload", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<byte[]> uploadPage(@RequestParam("token") String token) {
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
          <form action=\"/upload?token=%s\" method=\"post\" enctype=\"multipart/form-data\">
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
  public ResponseEntity<Void> uploadSigned(@RequestParam("token") String token,
                                           @RequestParam("file") MultipartFile file) {
    SignSession session = signService.getSessionByToken(token);
    signService.saveSigned(session, file);
    return ResponseEntity.status(302)
        .header(HttpHeaders.LOCATION, "/done?token=" + token)
        .build();
  }

  @GetMapping(value = "/done", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<byte[]> donePage(@RequestParam("token") String token) {
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
