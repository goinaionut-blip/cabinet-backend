package ro.cabinet.backend.sign;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Component
public class SignTemplateRegistry {
  private final Map<SignTemplateId, TemplateDefinition> templates = Map.of(
      SignTemplateId.INFORMED_CONSENT,
      new TemplateDefinition(
          SignTemplateId.INFORMED_CONSENT,
          "sign-templates/informed_consent.pdf",
          new SignaturePlacement(0, 360, 120, 180, 70),
          List.of(
              new FormField("patient_name", "Nume pacient", FieldType.TEXT),
              new FormField("patient_cnp", "CNP", FieldType.TEXT),
              new FormField("consent_checked", "Sunt de acord", FieldType.CHECKBOX)
          )
      ),
      SignTemplateId.HEALTH_QUESTIONNAIRE,
      new TemplateDefinition(
          SignTemplateId.HEALTH_QUESTIONNAIRE,
          "sign-templates/health_questionnaire.pdf",
          new SignaturePlacement(0, 360, 140, 180, 70),
          List.of(
              new FormField("patient_name", "Nume pacient", FieldType.TEXT),
              new FormField("phone_number", "Telefon", FieldType.TEXT),
              new FormField("health_ok", "Fara alergii cunoscute", FieldType.CHECKBOX)
          )
      ),
      SignTemplateId.GDPR,
      new TemplateDefinition(
          SignTemplateId.GDPR,
          "sign-templates/gdpr.pdf",
          new SignaturePlacement(0, 360, 100, 180, 70),
          List.of(
              new FormField("patient_name", "Nume pacient", FieldType.TEXT),
              new FormField("gdpr_accept", "Accept GDPR", FieldType.CHECKBOX)
          )
      )
  );

  public TemplateDefinition getTemplate(SignTemplateId templateId) {
    TemplateDefinition definition = templates.get(templateId);
    if (definition == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template invalid");
    }
    return definition;
  }

  public Resource getTemplateResource(SignTemplateId templateId) {
    TemplateDefinition definition = getTemplate(templateId);
    return new ClassPathResource(definition.resourcePath());
  }

  public InputStream openTemplate(SignTemplateId templateId) {
    try {
      return getTemplateResource(templateId).getInputStream();
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Nu pot incarca template-ul PDF");
    }
  }

  public record TemplateDefinition(SignTemplateId id, String resourcePath,
                                   SignaturePlacement signaturePlacement,
                                   List<FormField> formFields) {
  }

  public record SignaturePlacement(int pageIndex, float x, float y, float width, float height) {
  }

  public record FormField(String name, String label, FieldType type) {
  }

  public enum FieldType {
    TEXT,
    CHECKBOX
  }
}
