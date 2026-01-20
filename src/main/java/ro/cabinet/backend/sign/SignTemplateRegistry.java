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
          new SignaturePlacement(1, 110, 380, 200, 80),
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
              new FormField("undefined", "undefined", FieldType.TEXT),
              new FormField("fill_5", "fill_5", FieldType.TEXT),
              new FormField("2 Reprezentant legal al", "2 Reprezentant legal al", FieldType.TEXT),
              new FormField("fill_7", "fill_7", FieldType.TEXT),
              new FormField("fill_9", "fill_9", FieldType.TEXT),
              new FormField("al pacientului ei", "al pacientului ei", FieldType.TEXT),
              new FormField("fill_10", "fill_10", FieldType.TEXT),
              new FormField("nu_2", "nu_2", FieldType.CHECKBOX),
              new FormField("fill_11", "fill_11", FieldType.TEXT),
              new FormField("da_3", "da_3", FieldType.CHECKBOX),
              new FormField("nu_3", "nu_3", FieldType.CHECKBOX),
              new FormField("fill_12", "fill_12", FieldType.TEXT),
              new FormField("fill_13", "fill_13", FieldType.TEXT),
              new FormField("fill_14", "fill_14", FieldType.TEXT),
              new FormField("da_4", "da_4", FieldType.CHECKBOX),
              new FormField("nu_4", "nu_4", FieldType.CHECKBOX),
              new FormField("fill_15", "fill_15", FieldType.TEXT),
              new FormField("fill_16", "fill_16", FieldType.TEXT),
              new FormField("fill_17", "fill_17", FieldType.TEXT),
              new FormField("fill_18", "fill_18", FieldType.TEXT),
              new FormField("fill_19", "fill_19", FieldType.TEXT),
              new FormField("fill_20", "fill_20", FieldType.TEXT),
              new FormField("fill_21", "fill_21", FieldType.TEXT),
              new FormField("fill_23", "fill_23", FieldType.TEXT),
              new FormField("undefined_2", "undefined_2", FieldType.TEXT),
              new FormField("da_8", "da_8", FieldType.CHECKBOX),
              new FormField("nu_8", "nu_8", FieldType.CHECKBOX),
              new FormField("fill_24", "fill_24", FieldType.TEXT),
              new FormField("boli congenitale", "boli congenitale", FieldType.CHECKBOX),
              new FormField("boli profesionale", "boli profesionale", FieldType.CHECKBOX),
              new FormField("undefined_3", "undefined_3", FieldType.TEXT),
              new FormField("toggle_21", "toggle_21", FieldType.CHECKBOX),
              new FormField("toggle_22", "toggle_22", FieldType.CHECKBOX),
              new FormField("undefined_4", "undefined_4", FieldType.TEXT),
              new FormField("blocuri", "blocuri", FieldType.CHECKBOX),
              new FormField("toggle_25", "toggle_25", FieldType.CHECKBOX),
              new FormField("undefined_5", "undefined_5", FieldType.TEXT),
              new FormField("toggle_23", "toggle_23", FieldType.CHECKBOX),
              new FormField("toggle_26", "toggle_26", FieldType.CHECKBOX),
              new FormField("toggle_28", "toggle_28", FieldType.CHECKBOX),
              new FormField("undefined_6", "undefined_6", FieldType.TEXT),
              new FormField("undefined_7", "undefined_7", FieldType.TEXT),
              new FormField("toggle_27", "toggle_27", FieldType.CHECKBOX),
              new FormField("altele", "altele", FieldType.CHECKBOX),
              new FormField("undefined_8", "undefined_8", FieldType.TEXT),
              new FormField("fill_32", "fill_32", FieldType.TEXT),
              new FormField("boli vasculare", "boli vasculare", FieldType.CHECKBOX),
              new FormField("boli ale aparatului respirator", "boli ale aparatului respirator", FieldType.CHECKBOX),
              new FormField("toggle_1", "toggle_1", FieldType.CHECKBOX),
              new FormField("toggle_2", "toggle_2", FieldType.CHECKBOX),
              new FormField("toggle_3", "toggle_3", FieldType.CHECKBOX),
              new FormField("fill_1_2", "fill_1_2", FieldType.TEXT),
              new FormField("fill_2", "fill_2", FieldType.TEXT),
              new FormField("undefined_9", "undefined_9", FieldType.CHECKBOX),
              new FormField("altele_2", "altele_2", FieldType.TEXT),
              new FormField("toggle_7", "toggle_7", FieldType.CHECKBOX),
              new FormField("emfizem", "emfizem", FieldType.CHECKBOX),
              new FormField("toggle_9", "toggle_9", FieldType.CHECKBOX),
              new FormField("toggle_10", "toggle_10", FieldType.CHECKBOX),
              new FormField("fill_4", "fill_4", FieldType.TEXT),
              new FormField("undefined_10", "undefined_10", FieldType.CHECKBOX),
              new FormField("altele_3", "altele_3", FieldType.TEXT),
              new FormField("gastriteulcer gastroduodenal", "gastriteulcer gastroduodenal", FieldType.CHECKBOX),
              new FormField("undefined_11", "undefined_11", FieldType.CHECKBOX),
              new FormField("altele_4", "altele_4", FieldType.TEXT),
              new FormField("toggle_14", "toggle_14", FieldType.CHECKBOX),
              new FormField("toggle_15_2", "toggle_15_2", FieldType.CHECKBOX),
              new FormField("toggle_16_2", "toggle_16_2", FieldType.CHECKBOX),
              new FormField("undefined_12", "undefined_12", FieldType.CHECKBOX),
              new FormField("altele_5", "altele_5", FieldType.TEXT),
              new FormField("boli renale", "boli renale", FieldType.CHECKBOX),
              new FormField("fill_8", "fill_8", FieldType.TEXT),
              new FormField("toggle_19", "toggle_19", FieldType.CHECKBOX),
              new FormField("tratament cu antidiabetice orale", "tratament cu antidiabetice orale", FieldType.CHECKBOX),
              new FormField("hipotiroidie", "hipotiroidie", FieldType.CHECKBOX),
              new FormField("hipertiroidie", "hipertiroidie", FieldType.CHECKBOX),
              new FormField("undefined_13", "undefined_13", FieldType.CHECKBOX),
              new FormField("altele_6", "altele_6", FieldType.TEXT),
              new FormField("toggle_24", "toggle_24", FieldType.CHECKBOX),
              new FormField("colagenoze", "colagenoze", FieldType.CHECKBOX),
              new FormField("undefined_14", "undefined_14", FieldType.CHECKBOX),
              new FormField("altele_7", "altele_7", FieldType.TEXT),
              new FormField("toggle_27_2", "toggle_27_2", FieldType.CHECKBOX),
              new FormField("undefined_15", "undefined_15", FieldType.CHECKBOX),
              new FormField("altele_8", "altele_8", FieldType.TEXT),
              new FormField("epilepsie", "epilepsie", FieldType.CHECKBOX),
              new FormField("undefined_16", "undefined_16", FieldType.CHECKBOX),
              new FormField("altele_9", "altele_9", FieldType.TEXT),
              new FormField("depresie", "depresie", FieldType.CHECKBOX),
              new FormField("schizofrenie", "schizofrenie", FieldType.CHECKBOX),
              new FormField("undefined_17", "undefined_17", FieldType.CHECKBOX),
              new FormField("altele_10", "altele_10", FieldType.TEXT),
              new FormField("toggle_34", "toggle_34", FieldType.CHECKBOX),
              new FormField("anemie", "anemie", FieldType.CHECKBOX),
              new FormField("thalasemie", "thalasemie", FieldType.CHECKBOX),
              new FormField("toggle_37", "toggle_37", FieldType.CHECKBOX),
              new FormField("toggle_38", "toggle_38", FieldType.CHECKBOX),
              new FormField("hemofilie", "hemofilie", FieldType.CHECKBOX),
              new FormField("trombocitopenie", "trombocitopenie", FieldType.CHECKBOX),
              new FormField("boala von Willebrand", "boala von Willebrand", FieldType.CHECKBOX),
              new FormField("undefined_18", "undefined_18", FieldType.CHECKBOX),
              new FormField("altele_11", "altele_11", FieldType.TEXT),
              new FormField("toggle_43", "toggle_43", FieldType.CHECKBOX),
              new FormField("B", "B", FieldType.CHECKBOX),
              new FormField("C", "C", FieldType.CHECKBOX),
              new FormField("D", "D", FieldType.CHECKBOX),
              new FormField("HIV", "HIV", FieldType.CHECKBOX),
              new FormField("undefined_19", "undefined_19", FieldType.CHECKBOX),
              new FormField("altele_12", "altele_12", FieldType.TEXT),
              new FormField("neoplasme", "neoplasme", FieldType.TEXT),
              new FormField("alte boli", "alte boli", FieldType.TEXT),
              new FormField("da_9", "da_9", FieldType.CHECKBOX),
              new FormField("nu_9", "nu_9", FieldType.CHECKBOX),
              new FormField("fill_18_2", "fill_18_2", FieldType.TEXT),
              new FormField("fill_19_2", "fill_19_2", FieldType.TEXT),
              new FormField("toggle_51", "toggle_51", FieldType.CHECKBOX),
              new FormField("sedare", "sedare", FieldType.CHECKBOX),
              new FormField("altul", "altul", FieldType.TEXT),
              new FormField("toggle_53", "toggle_53", FieldType.CHECKBOX),
              new FormField("toggle_54", "toggle_54", FieldType.CHECKBOX),
              new FormField("nu_10", "nu_10", FieldType.CHECKBOX),
              new FormField("undefined_20", "undefined_20", FieldType.CHECKBOX),
              new FormField("fill_21_2", "fill_21_2", FieldType.TEXT),
              new FormField("fill_22", "fill_22", FieldType.TEXT),
              new FormField("da_10", "da_10", FieldType.CHECKBOX),
              new FormField("nu_11", "nu_11", FieldType.CHECKBOX),
              new FormField("da_11", "da_11", FieldType.CHECKBOX),
              new FormField("nu_12", "nu_12", FieldType.CHECKBOX),
              new FormField("toggle_62", "toggle_62", FieldType.CHECKBOX),
              new FormField("toggle_61", "toggle_61", FieldType.CHECKBOX),
              new FormField("toggle_64", "toggle_64", FieldType.CHECKBOX),
              new FormField("toggle_63", "toggle_63", FieldType.CHECKBOX),
              new FormField("toggle_65", "toggle_65", FieldType.CHECKBOX),
              new FormField("toggle_71", "toggle_71", FieldType.CHECKBOX),
              new FormField("toggle_68", "toggle_68", FieldType.CHECKBOX),
              new FormField("toggle_69", "toggle_69", FieldType.CHECKBOX),
              new FormField("alergii", "alergii", FieldType.CHECKBOX),
              new FormField("da_12", "da_12", FieldType.CHECKBOX),
              new FormField("nu_13", "nu_13", FieldType.CHECKBOX),
              new FormField("altele_13", "altele_13", FieldType.TEXT),
              new FormField("toggle_73", "toggle_73", FieldType.CHECKBOX),
              new FormField("nu_14", "nu_14", FieldType.CHECKBOX),
              new FormField("fill_24_2", "fill_24_2", FieldType.TEXT),
              new FormField("nu_15", "nu_15", FieldType.CHECKBOX),
              new FormField("undefined_21", "undefined_21", FieldType.CHECKBOX),
              new FormField("fill_25", "fill_25", FieldType.TEXT),
              new FormField("alcool", "alcool", FieldType.TEXT),
              new FormField("droguri", "droguri", FieldType.CHECKBOX),
              new FormField("fill_27", "fill_27", FieldType.TEXT),
              new FormField("fill_28", "fill_28", FieldType.TEXT),
              new FormField("nu_16", "nu_16", FieldType.CHECKBOX),
              new FormField("toggle_78", "toggle_78", FieldType.CHECKBOX),
              new FormField("Address", "Address", FieldType.TEXT),
              new FormField("CNP", "CNP", FieldType.TEXT),
              new FormField("da_2", "da_2", FieldType.CHECKBOX),
              new FormField("nu", "nu", FieldType.CHECKBOX),
              new FormField("da", "da", FieldType.CHECKBOX),
              new FormField("toggle_16", "toggle_16", FieldType.CHECKBOX),
              new FormField("toggle_15", "toggle_15", FieldType.CHECKBOX),
              new FormField("nu_7", "nu_7", FieldType.CHECKBOX),
              new FormField("da_7", "da_7", FieldType.CHECKBOX),
              new FormField("nu_6", "nu_6", FieldType.CHECKBOX),
              new FormField("da_6", "da_6", FieldType.CHECKBOX),
              new FormField("Check Box1", "Check Box1", FieldType.CHECKBOX),
              new FormField("Check Box2", "Check Box2", FieldType.CHECKBOX),
              new FormField("Name", "Name", FieldType.TEXT),
              new FormField("Data", "Data", FieldType.TEXT)
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
