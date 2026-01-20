package ro.cabinet.backend.sign;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SignWebController {
  private final SignService signService;
  private final SignTemplateRegistry templateRegistry;

  public SignWebController(SignService signService, SignTemplateRegistry templateRegistry) {
    this.signService = signService;
    this.templateRegistry = templateRegistry;
  }

  @GetMapping(value = "/sign-web", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<byte[]> signWebPage(@RequestParam("token") String token) {
    SignSession session = signService.getSessionByToken(token);
    SignTemplateId templateId = session.getTemplateId();
    SignTemplateRegistry.TemplateDefinition template = templateRegistry.getTemplate(templateId);
    String html = buildPageHtml(token, template);
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_HTML)
        .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
        .header(HttpHeaders.PRAGMA, "no-cache")
        .body(html.getBytes(StandardCharsets.UTF_8));
  }

  @PostMapping(value = "/sign-web/submit", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<byte[]> submitSignWeb(@RequestParam("token") String token,
                                              @RequestBody SignWebSubmitRequest request) {
    SignSession session = signService.getSessionByToken(token);
    Map<String, Object> formData = request != null && request.formData() != null
        ? request.formData()
        : new LinkedHashMap<>();
    String signature = request != null ? request.signaturePngBase64() : null;
    signService.saveSignedFromWeb(session, formData, signature);
    String html = """
        <!doctype html>
        <html lang="ro">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <title>Multumim</title>
          <style>
            body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Arial, sans-serif; padding: 24px; }
            .card { max-width: 520px; margin: 0 auto; background: #fff; border-radius: 12px;
                    padding: 20px; box-shadow: 0 2px 10px rgba(0,0,0,0.08); }
          </style>
        </head>
        <body>
          <div class="card">
            <h2>Multumim!</h2>
            <p>Documentul a fost semnat si trimis.</p>
          </div>
        </body>
        </html>
        """;
    return ResponseEntity.ok()
        .contentType(MediaType.TEXT_HTML)
        .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0")
        .header(HttpHeaders.PRAGMA, "no-cache")
        .body(html.getBytes(StandardCharsets.UTF_8));
  }

  private String buildPageHtml(String token, SignTemplateRegistry.TemplateDefinition template) {
    StringBuilder fieldsHtml = new StringBuilder();
    for (SignTemplateRegistry.FormField field : template.formFields()) {
      String safeName = escapeHtml(field.name());
      String safeLabel = escapeHtml(field.label());
      if (field.type() == SignTemplateRegistry.FieldType.CHECKBOX) {
        fieldsHtml.append("""
            <label class="field checkbox">
              <input type="checkbox" data-field="%s" />
              <span>%s</span>
            </label>
            """.formatted(safeName, safeLabel));
      } else {
        fieldsHtml.append("""
            <label class="field">
              <span>%s</span>
              <input type="text" data-field="%s" />
            </label>
            """.formatted(safeLabel, safeName));
      }
    }

    return """
        <!doctype html>
        <html lang="ro">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <title>Semnare in web</title>
          <style>
            :root { color-scheme: light; }
            body {
              font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Arial, sans-serif;
              margin: 0;
              padding: 20px;
              background: #f6f7fb;
              color: #111;
            }
            .card {
              max-width: 720px;
              margin: 0 auto;
              background: #fff;
              border-radius: 16px;
              padding: 20px;
              box-shadow: 0 2px 12px rgba(0,0,0,0.08);
            }
            h2 { margin-top: 0; }
            .grid {
              display: grid;
              grid-template-columns: 1fr;
              gap: 12px;
            }
            .field { display: grid; gap: 6px; font-size: 14px; }
            .field input[type="text"] {
              padding: 10px 12px;
              border-radius: 8px;
              border: 1px solid #d0d7de;
            }
            .checkbox {
              display: flex;
              align-items: center;
              gap: 10px;
              font-size: 14px;
            }
            .signature {
              margin-top: 16px;
            }
            canvas {
              width: 100%%;
              height: 180px;
              border: 1px solid #d0d7de;
              border-radius: 10px;
              touch-action: none;
            }
            .actions {
              display: flex;
              gap: 12px;
              margin-top: 16px;
            }
            button {
              padding: 12px 16px;
              border-radius: 10px;
              border: none;
              font-size: 16px;
            }
            .primary { background: #1b5e20; color: #fff; }
            .secondary { background: #e3f2fd; color: #0d47a1; }
            .status { margin-top: 12px; font-size: 14px; color: #555; }
          </style>
        </head>
        <body>
          <div class="card">
            <h2>Semnare document</h2>
            <div class="grid">
              %s
            </div>
            <div class="signature">
              <label class="field">
                <span>Semnatura</span>
                <canvas id="signature" width="600" height="180"></canvas>
              </label>
              <div class="actions">
                <button type="button" class="secondary" id="clearBtn">Sterge</button>
                <button type="button" class="primary" id="submitBtn">Finalizeaza</button>
              </div>
              <div class="status" id="status"></div>
            </div>
          </div>
          <script>
            const canvas = document.getElementById('signature');
            const ctx = canvas.getContext('2d');
            ctx.lineWidth = 2;
            ctx.lineCap = 'round';
            let drawing = false;
            let last = null;

            function getPos(e) {
              const rect = canvas.getBoundingClientRect();
              const clientX = e.touches ? e.touches[0].clientX : e.clientX;
              const clientY = e.touches ? e.touches[0].clientY : e.clientY;
              const x = (clientX - rect.left) * (canvas.width / rect.width);
              const y = (clientY - rect.top) * (canvas.height / rect.height);
              return { x, y };
            }

            function start(e) {
              drawing = true;
              last = getPos(e);
            }

            function move(e) {
              if (!drawing) return;
              const pos = getPos(e);
              ctx.beginPath();
              ctx.moveTo(last.x, last.y);
              ctx.lineTo(pos.x, pos.y);
              ctx.stroke();
              last = pos;
            }

            function stop() {
              drawing = false;
              last = null;
            }

            canvas.addEventListener('mousedown', start);
            canvas.addEventListener('mousemove', move);
            window.addEventListener('mouseup', stop);
            canvas.addEventListener('touchstart', (e) => { e.preventDefault(); start(e); });
            canvas.addEventListener('touchmove', (e) => { e.preventDefault(); move(e); });
            canvas.addEventListener('touchend', (e) => { e.preventDefault(); stop(); });

            document.getElementById('clearBtn').addEventListener('click', () => {
              ctx.clearRect(0, 0, canvas.width, canvas.height);
            });

            document.getElementById('submitBtn').addEventListener('click', async () => {
              const status = document.getElementById('status');
              status.textContent = 'Se trimite...';
              const fields = {};
              document.querySelectorAll('[data-field]').forEach((el) => {
                const key = el.getAttribute('data-field');
                if (el.type === 'checkbox') {
                  fields[key] = el.checked;
                } else {
                  fields[key] = el.value || '';
                }
              });
              const signature = canvas.toDataURL('image/png');
              const payload = { formData: fields, signaturePngBase64: signature };
              try {
                const response = await fetch('/sign-web/submit?token=%s', {
                  method: 'POST',
                  headers: { 'Content-Type': 'application/json' },
                  body: JSON.stringify(payload)
                });
                if (!response.ok) {
                  status.textContent = 'Eroare la trimitere.';
                  return;
                }
                const html = await response.text();
                document.open();
                document.write(html);
                document.close();
              } catch (err) {
                status.textContent = 'Eroare la trimitere.';
              }
            });
          </script>
        </body>
        </html>
        """.formatted(fieldsHtml, token);
  }

  private String escapeHtml(String value) {
    if (value == null) {
      return "";
    }
    return value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  public record SignWebSubmitRequest(Map<String, Object> formData, String signaturePngBase64) {
  }
}
