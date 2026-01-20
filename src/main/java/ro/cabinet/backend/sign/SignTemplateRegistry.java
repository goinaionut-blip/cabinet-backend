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
          new SignaturePlacement(1, 130, 360, 200, 80),
          List.of(
              new FormField("Name", "Nume", FieldType.TEXT),
              new FormField("Address", "Adresa", FieldType.TEXT)
          ),
          "4. Au fost furnizate pacientului următoarele informații în legătura cu actul medical:",
          List.of(
              new YesNoField("Check Box8", "Check Box9",
                  "4.1. Date despre starea de sănătate"),
              new YesNoField("Check Box11", "Check Box12",
                  "4.2. Diagnostic"),
              new YesNoField("Check Box13", "Check Box14",
                  "4.3. Prognostic"),
              new YesNoField("Check Box15", "Check Box16",
                  "4.4. Natura și scopul actului medical propus"),
              new YesNoField("Check Box17", "Check Box18",
                  "4.5. Intervențiile și strategia terapeutică propuse"),
              new YesNoField("Check Box19", "Check Box20",
                  "4.6. Beneficiile și consecințele actului medical, insistându-se asupra următoarelor:"),
              new YesNoField("Check Box21", "Check Box22",
                  "4.7. Riscurile potențiale ale actului medical, insistându-se asupra următoarelor:"),
              new YesNoField("Check Box23", "Check Box24",
                  "4.8. Alternative viabile de tratament și riscurile acestora, insistându-se asupra următoarelor:"),
              new YesNoField("Check Box25", "Check Box26",
                  "4.9. Riscurile neefectuării tratamentului"),
              new YesNoField("Check Box27", "Check Box28",
                  "4.10. Riscurile nerespectării recomandărilor medicale"),
              new YesNoField("Check Box29", "Check Box30",
                  "5.1. Pacientul este de acord cu recoltarea, păstrarea și folosirea produselor biologice"),
              new YesNoField("Check Box31", "Check Box32",
                  "6.1. Informații despre serviciile medicale disponibile"),
              new YesNoField("Check Box33", "Check Box34",
                  "6.2. Informații despre identitatea și statutul profesional al personalului care îl va trata*"),
              new YesNoField("Check Box35", "Check Box36",
                  "6.3. Informații despre regulile/practicile din unitatea medicală, pe care trebuie să le respecte"),
              new YesNoField("Check Box37", "Check Box38",
                  "6.4. Pacientul a fost încunoștințat că are dreptul la o a doua opinie medicală"),
              new YesNoField("Check Box39", "Check Box40",
                  "7. Pacientul dorește să fie informat în continuare despre starea sa de sănătate")
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
          ),
          null,
          List.of()
      ),
      SignTemplateId.GDPR,
      new TemplateDefinition(
          SignTemplateId.GDPR,
          "sign-templates/gdpr.pdf",
          new SignaturePlacement(6, 380, 180, 200, 80),
          List.of(
              new FormField("Name", "Nume", FieldType.TEXT),
              new FormField("Data", "Data", FieldType.TEXT)
          ),
          null,
          List.of()
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
                                   List<FormField> formFields,
                                   String questionsHeader,
                                   List<YesNoField> yesNoFields) {
  }

  public record SignaturePlacement(int pageIndex, float x, float y, float width, float height) {
  }

  public record FormField(String name, String label, FieldType type) {
  }

  public record YesNoField(String yesField, String noField, String label) {
  }

  public enum FieldType {
    TEXT,
    CHECKBOX
  }
}
