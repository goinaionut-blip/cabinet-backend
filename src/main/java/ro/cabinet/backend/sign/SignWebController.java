package ro.cabinet.backend.sign;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
  private final ObjectMapper objectMapper;

  public SignWebController(SignService signService, SignTemplateRegistry templateRegistry,
                           ObjectMapper objectMapper) {
    this.signService = signService;
    this.templateRegistry = templateRegistry;
    this.objectMapper = objectMapper;
  }

  @GetMapping(value = "/sign-web", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<byte[]> signWebPage(@RequestParam("token") String token) {
    SignSession session = signService.getSessionByToken(token);
    SignTemplateId templateId = session.getTemplateId();
    SignTemplateRegistry.TemplateDefinition template = templateRegistry.getTemplate(templateId);
    Map<String, Object> prefill = filterPrefill(templateId, parseFormData(session.getFormData()));
    String html = buildPageHtml(token, template, prefill);
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
    Map<String, Object> empty = new LinkedHashMap<>();
    return buildPageHtml(token, template, empty);
  }

  private String buildPageHtml(String token, SignTemplateRegistry.TemplateDefinition template,
                               Map<String, Object> prefill) {
    StringBuilder fieldsHtml = new StringBuilder();
    if (template.id() == SignTemplateId.HEALTH_QUESTIONNAIRE) {
      Set<String> prefillFields = Set.of("Name", "CNP", "Address", "Data");
      fieldsHtml.append("""
          <section class="section">
            <div class="section-header">
              <h3>Date pacient</h3>
              <p>Aceste câmpuri sunt precompletate.</p>
            </div>
            <div class="grid two">
          """);
      for (SignTemplateRegistry.FormField field : template.formFields()) {
        if (!prefillFields.contains(field.name())) {
          continue;
        }
        fieldsHtml.append(renderField(field, prefill));
      }
      fieldsHtml.append("""
            </div>
          </section>
          <section class="section">
            <div class="section-header">
              <h3>Chestionar medical</h3>
              <p>Completați toate câmpurile aplicabile.</p>
            </div>
            <div class="grid two">
          """);
      for (SignTemplateRegistry.FormField field : template.formFields()) {
        if (prefillFields.contains(field.name())) {
          continue;
        }
        fieldsHtml.append(renderField(field, prefill));
      }
      fieldsHtml.append("""
            </div>
          </section>
          """);
    } else {
      for (SignTemplateRegistry.FormField field : template.formFields()) {
        fieldsHtml.append(renderField(field, prefill));
      }
    }

    StringBuilder questionsHtml = new StringBuilder();
    String header = template.questionsHeader();
    if (header != null && !header.isBlank()) {
      questionsHtml.append("""
          <div class="questions-header">%s</div>
          """.formatted(escapeHtml(header)));
    }
    int index = 0;
    for (SignTemplateRegistry.YesNoField field : template.yesNoFields()) {
      String group = "q" + index;
      String yesName = escapeHtml(field.yesField());
      String noName = escapeHtml(field.noField());
      Object yesValue = prefill.get(field.yesField());
      Object noValue = prefill.get(field.noField());
      boolean yesChecked = toBoolean(yesValue);
      boolean noChecked = toBoolean(noValue);
      if (yesValue == null && noValue == null) {
        yesChecked = true;
        noChecked = false;
      }
      String yesAttr = yesChecked ? " checked" : "";
      String noAttr = noChecked ? " checked" : "";
      questionsHtml.append("""
          <div class="question">
            <span class="question-label">%s</span>
            <label class="yesno">
              <input type="checkbox" data-field="%s" data-group="%s"%s />
              <span>Da</span>
            </label>
            <label class="yesno">
              <input type="checkbox" data-field="%s" data-group="%s"%s />
              <span>Nu</span>
            </label>
          </div>
          """.formatted(escapeHtml(field.label()), yesName, group, yesAttr, noName, group, noAttr));
      index++;
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
              font-family: "Palatino Linotype", "Book Antiqua", Palatino, serif;
              margin: 0;
              padding: 24px;
              background: radial-gradient(circle at top, #fdf3e6, #f7f1e1 40%%, #edf2f8 100%%);
              color: #14202b;
            }
            .card {
              max-width: 980px;
              margin: 0 auto;
              background: #fffdf8;
              border-radius: 20px;
              padding: 28px;
              box-shadow: 0 18px 40px rgba(27, 39, 51, 0.12);
              border: 1px solid rgba(20, 32, 43, 0.08);
            }
            h2 {
              margin: 0 0 18px;
              font-family: "Trebuchet MS", "Lucida Sans Unicode", "Lucida Grande", sans-serif;
              letter-spacing: 0.3px;
            }
            .section {
              margin-bottom: 24px;
              padding: 16px;
              border-radius: 16px;
              background: #ffffff;
              border: 1px solid rgba(20, 32, 43, 0.08);
            }
            .section-header {
              display: flex;
              flex-direction: column;
              gap: 4px;
              margin-bottom: 12px;
            }
            .section-header h3 {
              margin: 0;
              font-family: "Trebuchet MS", "Lucida Sans Unicode", "Lucida Grande", sans-serif;
            }
            .section-header p {
              margin: 0;
              font-size: 13px;
              color: #4b5563;
            }
            .grid {
              display: grid;
              grid-template-columns: 1fr;
              gap: 12px;
            }
            .grid.two {
              grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
            }
            .field { display: grid; gap: 6px; font-size: 14px; }
            .field input[type="text"] {
              padding: 10px 12px;
              border-radius: 10px;
              border: 1px solid #ccd6e3;
              background: #fff;
              font-family: inherit;
            }
            .checkbox {
              display: flex;
              align-items: center;
              gap: 10px;
              font-size: 14px;
              padding: 6px 8px;
              border-radius: 10px;
              background: #f7f7fb;
              border: 1px solid rgba(20, 32, 43, 0.08);
            }
            .question {
              display: grid;
              grid-template-columns: 1fr auto auto;
              gap: 12px;
              align-items: center;
              padding: 8px 0;
              border-bottom: 1px solid #eef0f4;
            }
            .question-label { font-size: 14px; }
            .questions-header {
              margin-top: 12px;
              font-size: 14px;
              font-weight: 600;
              color: #1f2937;
            }
            .yesno { display: inline-flex; align-items: center; gap: 6px; }
            .signature {
              margin-top: 16px;
              padding: 16px;
              border-radius: 16px;
              background: #fff;
              border: 1px solid rgba(20, 32, 43, 0.08);
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
              flex-wrap: wrap;
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
            @media (max-width: 720px) {
              body { padding: 16px; }
              .card { padding: 20px; }
            }
          </style>
        </head>
        <body>
          <div class="card">
            <h2>Semnare document</h2>
            <div class="grid">
              %s
            </div>
            %s
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

            document.querySelectorAll('[data-group]').forEach((el) => {
              el.addEventListener('change', () => {
                if (!el.checked) return;
                const group = el.getAttribute('data-group');
                document.querySelectorAll('[data-group=\"' + group + '\"]').forEach((other) => {
                  if (other !== el) other.checked = false;
                });
              });
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
        """.formatted(fieldsHtml, questionsHtml, token);
  }

  private String renderField(SignTemplateRegistry.FormField field, Map<String, Object> prefill) {
    String safeName = escapeHtml(field.name());
    String safeLabel = escapeHtml(field.label());
    if (field.type() == SignTemplateRegistry.FieldType.CHECKBOX) {
      boolean checked = toBoolean(prefill.get(field.name()));
      String checkedAttr = checked ? " checked" : "";
      return """
          <label class="field checkbox">
            <input type="checkbox" data-field="%s"%s />
            <span>%s</span>
          </label>
          """.formatted(safeName, checkedAttr, safeLabel);
    }
    String value = escapeHtml(toText(prefill.get(field.name())));
    return """
        <label class="field">
          <span>%s</span>
          <input type="text" data-field="%s" value="%s" />
        </label>
        """.formatted(safeLabel, safeName, value);
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

  private String toText(Object value) {
    return value == null ? "" : String.valueOf(value);
  }

  private Map<String, Object> parseFormData(String json) {
    if (json == null || json.isBlank()) {
      return new LinkedHashMap<>();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    } catch (Exception ex) {
      return new LinkedHashMap<>();
    }
  }

  private Map<String, Object> filterPrefill(SignTemplateId templateId, Map<String, Object> prefill) {
    if (prefill == null || prefill.isEmpty() || templateId != SignTemplateId.HEALTH_QUESTIONNAIRE) {
      return prefill == null ? new LinkedHashMap<>() : prefill;
    }
    Set<String> allowed = Set.of("Name", "CNP", "Address", "Data");
    Map<String, Object> filtered = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : prefill.entrySet()) {
      if (allowed.contains(entry.getKey())) {
        filtered.put(entry.getKey(), entry.getValue());
      }
    }
    return filtered;
  }

  public record SignWebSubmitRequest(Map<String, Object> formData, String signaturePngBase64) {
  }
}
