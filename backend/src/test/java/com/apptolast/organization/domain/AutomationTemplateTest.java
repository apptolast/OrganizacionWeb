package com.apptolast.organization.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AutomationTemplateTest {
  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "TaskCreated.v1|Revisar {{task.name}}|UNKNOWN_PLACEHOLDER",
        "TaskCreated.v1|Revisar {{event.type|UNCLOSED_PLACEHOLDER",
        "TaskCreated.v1|{{ task.title }}|UNKNOWN_PLACEHOLDER",
        "TaskCreated.v1|{{project}}|UNKNOWN_PLACEHOLDER",
        "ProjectCreated.v1|Revisar {{task.title}}|PLACEHOLDER_NOT_AVAILABLE",
        "ProjectUpdated.v1|{{task.title}}|PLACEHOLDER_NOT_AVAILABLE",
        "TaskCreated.v1|{{task.title|UNCLOSED_PLACEHOLDER",
        "TaskCreated.v1|{{nada}}|UNKNOWN_PLACEHOLDER",
        "TaskCreated.v1|Revisar {{task.title}} en {{project.name}}|",
        "TaskCreated.v1|{{event.type}} a las {{occurredAt}}|",
        "ProjectCreated.v1|Kickoff de {{project.name}}|",
        "TaskCreated.v1|Llave simple { y cierre } sin marcador|",
        "WorkSessionClosed.v1|Preparar {{task.title}}|",
        "TaskCreated.v1||"
      })
  void s7_reportsTheFirstTemplateDefect(String eventType, String template, String code) {
    assertThat(AutomationTemplate.validationCode(template == null ? "" : template, eventType))
        .isEqualTo(java.util.Optional.ofNullable(code));
  }

  @org.junit.jupiter.api.Test
  void s18_rendersPlaceholdersWithTheCurrentValues() {
    var values =
        new TemplateValues(
            "TaskCreated.v1",
            java.time.Instant.parse("2026-09-08T10:15:30.123456Z"),
            "Redactar informe",
            "Marketing");
    assertThat(AutomationTemplate.render("Revisar {{task.title}} en {{project.name}}", values))
        .isEqualTo("Revisar Redactar informe en Marketing");
    assertThat(AutomationTemplate.render("{{event.type}} a las {{occurredAt}}", values))
        .isEqualTo("TaskCreated.v1 a las 2026-09-08T10:15:30.123456Z");
    assertThat(AutomationTemplate.render("Llave { y } <b>{{task.title}}</b>", values))
        .isEqualTo("Llave { y } <b>Redactar informe</b>");
    assertThat(AutomationTemplate.render("Kickoff de {{project.name}}", values.withoutTask()))
        .isEqualTo("Kickoff de Marketing");
    // Dos marcadores pegados, sin nada entre el cierre de uno y la apertura del siguiente. Es una
    // plantilla legal —@s6 las guarda byte a byte y ningún defecto de @s7 la rechaza— y es la
    // única forma del texto que separa «busca el cierre DESPUÉS de la apertura» de «búscalo
    // antes»: en todas las demás plantillas del árbol sobra hueco y las dos búsquedas coinciden.
    // Con la resta, el cierre que encuentra es el del marcador anterior y el render revienta.
    assertThat(AutomationTemplate.render("{{task.title}}{{project.name}}", values))
        .isEqualTo("Redactar informeMarketing");
  }
}
