package com.apptolast.organization.adapter;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.apptolast.organization.adapter.config.SecurityConfiguration;
import com.apptolast.organization.adapter.http.AutomationController;
import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = AutomationController.class,
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@Import(SecurityConfiguration.class)
class AutomationsApiTest {
  private static final UUID PROJECT = UUID.fromString("11111111-1111-4111-8111-111111111111");
  private static final UUID RULE = UUID.fromString("22222222-2222-4222-8222-222222222222");
  private static final UUID ENDPOINT = UUID.fromString("33333333-3333-4333-8333-333333333333");
  private static final Instant NOW = Instant.parse("2026-09-08T10:15:30.123456Z");

  @Autowired MockMvc mvc;
  @MockitoBean CreateAutomationUseCase create;
  @MockitoBean ReadAutomationsUseCase read;
  @MockitoBean ReplaceAutomationUseCase replace;
  @MockitoBean DeleteAutomationUseCase delete;
  @MockitoBean SimulateAutomationUseCase simulate;
  @MockitoBean ReadAutomationRunsUseCase runs;

  private static String body(String name, String eventType, String condition, String action) {
    return """
        {"name":%s,"enabled":true,"trigger":{"eventType":%s},"condition":%s,"action":%s}"""
        .formatted(name, eventType, condition, action);
  }

  private static String createTask(String titleTemplate, String criterionTemplate, String minutes) {
    return """
        {"type":"CREATE_TASK","projectId":"%s","titleTemplate":%s,"criterionTemplate":%s,"estimatedMinutes":%s}"""
        .formatted(PROJECT, titleTemplate, criterionTemplate, minutes);
  }

  private static String valid() {
    return body(
        "\"  Seguimiento  \"",
        "\"TaskCreated.v1\"",
        "null",
        createTask("\"Revisar {{task.title}}\"", "null", "30"));
  }

  private static AutomationRule rule(long version, AutomationDraft draft) {
    return new AutomationRule(RULE, draft, version, NOW, NOW);
  }

  private static AutomationDraft draft() {
    return new AutomationDraft(
        "Seguimiento",
        true,
        "TaskCreated.v1",
        null,
        new CreateTaskAction(PROJECT, "Revisar {{task.title}}", null, 30));
  }

  @Test
  void s1_createsTheRuleAndAnswersWithTheClosedRepresentation() throws Exception {
    when(create.create(eq("owner"), any())).thenReturn(rule(1, draft()));
    mvc.perform(
            post("/api/v1/me/automations")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(valid()))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/me/automations/" + RULE))
        .andExpect(header().string("ETag", "\"1\""))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(
            content()
                .json(
                    """
                    {"id":"%s","name":"Seguimiento","enabled":true,
                     "trigger":{"eventType":"TaskCreated.v1"},"condition":null,
                     "action":{"type":"CREATE_TASK","projectId":"%s",
                       "titleTemplate":"Revisar {{task.title}}","criterionTemplate":null,
                       "estimatedMinutes":30},
                     "version":1,"createdAt":"2026-09-08T10:15:30.123456Z",
                     "updatedAt":"2026-09-08T10:15:30.123456Z"}"""
                        .formatted(RULE, PROJECT),
                    true));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ProjectCreated.v1",
        "ProjectUpdated.v1",
        "ProjectStatusChanged.v1",
        "TaskCreated.v1",
        "SubtaskCreated.v1",
        "TaskStatusChanged.v1",
        "BlockPlanned.v1",
        "BlockChanged.v1",
        "WorkSessionStarted.v1",
        "WorkSessionStateChanged.v1",
        "WorkSessionExtended.v1",
        "WorkSessionClosed.v1"
      })
  void s2_acceptsEveryPublishedTrigger(String eventType) throws Exception {
    when(create.create(eq("owner"), any())).thenReturn(rule(1, draft()));
    mvc.perform(
            post("/api/v1/me/automations")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(
                    body(
                        "\"Regla\"",
                        "\"" + eventType + "\"",
                        "null",
                        createTask("\"{{event.type}}\"", "null", "null"))))
        .andExpect(status().isCreated());
  }

  @ParameterizedTest
  @ValueSource(strings = {"TaskCreated.v2", "taskcreated.v1", "webhook.ping.v1", ""})
  void s2_rejectsAnyOtherTriggerWithoutWriting(String eventType) throws Exception {
    mvc.perform(
            post("/api/v1/me/automations")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(
                    body(
                        "\"Regla\"",
                        "\"" + eventType + "\"",
                        "null",
                        createTask("\"{{event.type}}\"", "null", "null"))))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("UNKNOWN_EVENT_TYPE"))
        .andExpect(jsonPath("$.errors[0].field").value("trigger.eventType"))
        .andExpect(jsonPath("$.errors.length()").value(1));
    verifyNoInteractions(create);
  }

  @Test
  void s3_acceptsAConditionOverAnOwnProject() throws Exception {
    var conditioned =
        new AutomationDraft(
            "Regla",
            true,
            "TaskCreated.v1",
            PROJECT,
            new CreateTaskAction(PROJECT, "Revisar", null, null));
    when(create.create(eq("owner"), any())).thenReturn(rule(1, conditioned));
    mvc.perform(
            post("/api/v1/me/automations")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(
                    body(
                        "\"Regla\"",
                        "\"TaskCreated.v1\"",
                        "{\"projectId\":\"" + PROJECT + "\"}",
                        createTask("\"Revisar\"", "null", "null"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.condition.length()").value(1))
        .andExpect(jsonPath("$.condition.projectId").value(PROJECT.toString()));
    // Sin esto la prueba sólo comprobaba el eco de su propio stub: el cuerpo de la respuesta
    // se construye a partir de `conditioned`, no de lo que el parser produjo. La condición es
    // una cláusula de alcance (@s3): si se pierde por el camino, una regla acotada a un
    // proyecto se guarda como global y se dispara en todos los del propietario.
    verify(create).create(eq("owner"), eq(conditioned));
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {"{\"projectId\":\"no-uuid\"}|condition.projectId", "{}|condition.projectId"})
  void s3_rejectsAMalformedCondition(String condition, String field) throws Exception {
    mvc.perform(
            post("/api/v1/me/automations")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(
                    body(
                        "\"Regla\"",
                        "\"TaskCreated.v1\"",
                        condition,
                        createTask("\"Revisar\"", "null", "null"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors.length()").value(1))
        .andExpect(jsonPath("$.errors[0].field").value(field));
    verifyNoInteractions(create);
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {"condition.projectId|422|TARGET_NOT_FOUND", "action.projectId|422|TARGET_NOT_FOUND"})
  void s4_aForeignOrMissingProjectIsUnprocessable(String field, int status, String code)
      throws Exception {
    when(create.create(eq("owner"), any())).thenThrow(new AutomationTargetNotFoundException(field));
    mvc.perform(
            post("/api/v1/me/automations")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(valid()))
        .andExpect(status().is(status))
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value(code))
        .andExpect(jsonPath("$.errors[0].field").value(field));
  }

  @Test
  void s5_aForeignInactiveOrMissingEndpointIsUnprocessable() throws Exception {
    when(create.create(eq("owner"), any())).thenThrow(new WebhookEndpointNotFoundException());
    mvc.perform(
            post("/api/v1/me/automations")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(
                    body(
                        "\"Regla\"",
                        "\"ProjectStatusChanged.v1\"",
                        "null",
                        "{\"type\":\"NOTIFY_WEBHOOK\",\"endpointId\":\"" + ENDPOINT + "\"}")))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("ENDPOINT_NOT_FOUND"))
        .andExpect(jsonPath("$.errors[0].field").value("action.endpointId"));
  }

  @Test
  void s5_acceptsAWebhookActionAndEchoesItClosed() throws Exception {
    var webhook =
        new AutomationDraft(
            "Regla", true, "ProjectStatusChanged.v1", null, new NotifyWebhookAction(ENDPOINT));
    when(create.create(eq("owner"), any())).thenReturn(rule(1, webhook));
    mvc.perform(
            post("/api/v1/me/automations")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(
                    body(
                        "\"Regla\"",
                        "\"ProjectStatusChanged.v1\"",
                        "null",
                        "{\"type\":\"NOTIFY_WEBHOOK\",\"endpointId\":\"" + ENDPOINT + "\"}")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.action.length()").value(2))
        .andExpect(jsonPath("$.action.type").value("NOTIFY_WEBHOOK"))
        .andExpect(jsonPath("$.action.endpointId").value(ENDPOINT.toString()));
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "TaskCreated.v1|\"Revisar {{task.name}}\"|null|action.titleTemplate|UNKNOWN_PLACEHOLDER",
        "TaskCreated.v1|\"Revisar {{event.type\"|null|action.titleTemplate|UNCLOSED_PLACEHOLDER",
        "TaskCreated.v1|\"{{ task.title }}\"|null|action.titleTemplate|UNKNOWN_PLACEHOLDER",
        "TaskCreated.v1|\"Ok\"|\"{{project}}\"|action.criterionTemplate|UNKNOWN_PLACEHOLDER",
        "ProjectCreated.v1|\"Revisar {{task.title}}\"|null|action.titleTemplate|PLACEHOLDER_NOT_AVAILABLE",
        "ProjectUpdated.v1|\"Ok\"|\"{{task.title}}\"|action.criterionTemplate|PLACEHOLDER_NOT_AVAILABLE"
      })
  void s7_anInvalidTemplateNamesItsField(
      String eventType, String title, String criterion, String field, String code)
      throws Exception {
    mvc.perform(
            post("/api/v1/me/automations")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(
                    body(
                        "\"Regla\"",
                        "\"" + eventType + "\"",
                        "null",
                        createTask(title, criterion, "null"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_TEMPLATE"))
        .andExpect(jsonPath("$.errors.length()").value(1))
        .andExpect(jsonPath("$.errors[0].field").value(field))
        .andExpect(jsonPath("$.errors[0].code").value(code));
    verifyNoInteractions(create);
  }

  @Test
  void s7_reportsOneDefectPerFieldAtOnce() throws Exception {
    mvc.perform(
            post("/api/v1/me/automations")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(
                    body(
                        "\"Regla\"",
                        "\"TaskCreated.v1\"",
                        "null",
                        createTask("\"{{task.title\"", "\"{{nada}}\"", "null"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_TEMPLATE"))
        .andExpect(jsonPath("$.errors.length()").value(2))
        .andExpect(jsonPath("$.errors[0].field").value("action.titleTemplate"))
        .andExpect(jsonPath("$.errors[0].code").value("UNCLOSED_PLACEHOLDER"))
        .andExpect(jsonPath("$.errors[1].field").value("action.criterionTemplate"))
        .andExpect(jsonPath("$.errors[1].code").value("UNKNOWN_PLACEHOLDER"));
  }

  @Test
  void s6_keepsTemplatesByteForByte() throws Exception {
    var kept =
        new AutomationDraft(
            "Regla",
            true,
            "TaskCreated.v1",
            null,
            new CreateTaskAction(
                PROJECT, "{{task.title}}", "Llave simple { y cierre } sin marcador", null));
    when(create.create(eq("owner"), any())).thenReturn(rule(1, kept));
    mvc.perform(
            post("/api/v1/me/automations")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(
                    body(
                        "\"Regla\"",
                        "\"TaskCreated.v1\"",
                        "null",
                        createTask(
                            "\"{{task.title}}\"",
                            "\"Llave simple { y cierre } sin marcador\"",
                            "null"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.action.titleTemplate").value("{{task.title}}"))
        .andExpect(
            jsonPath("$.action.criterionTemplate").value("Llave simple { y cierre } sin marcador"));
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "{\"name\":\" \",\"enabled\":true,\"trigger\":{\"eventType\":\"TaskCreated.v1\"},\"condition\":null,\"action\":ACTION}|name",
        "{\"enabled\":true,\"trigger\":{\"eventType\":\"TaskCreated.v1\"},\"condition\":null,\"action\":ACTION}|name",
        "{\"name\":\"R\",\"enabled\":\"si\",\"trigger\":{\"eventType\":\"TaskCreated.v1\"},\"condition\":null,\"action\":ACTION}|enabled",
        "{\"name\":\"R\",\"enabled\":true,\"ownerId\":\"x\",\"trigger\":{\"eventType\":\"TaskCreated.v1\"},\"condition\":null,\"action\":ACTION}|body",
        "{\"name\":\"R\",\"name\":\"S\",\"enabled\":true,\"trigger\":{\"eventType\":\"TaskCreated.v1\"},\"condition\":null,\"action\":ACTION}|body",
        "{\"name\":\"R\",\"enabled\":true,\"trigger\":{\"eventType\":\"TaskCreated.v1\"},\"condition\":null,\"action\":{\"type\":\"CREATE_TASK\"}}|action",
        "{\"name\":\"R\",\"enabled\":true,\"trigger\":{\"eventType\":\"TaskCreated.v1\"},\"condition\":null,\"action\":{\"type\":\"DELETE_TASK\"}}|action.type",
        // Cuenta de claves correcta y un nombre cambiado: la única fila que separa `exactly` de
        // su cuenta de tamaño. Sin ella sobrevivía «removed call to onlyKnown» y una acción con
        // una clave desconocida en lugar de la esperada pasaba la validación.
        "{\"name\":\"R\",\"enabled\":true,\"trigger\":{\"eventType\":\"TaskCreated.v1\"},\"condition\":null,\"action\":{\"type\":\"CREATE_TASK\",\"projectId\":\"11111111-1111-4111-8111-111111111111\",\"titleTemplate\":\"Revisar\",\"completionCriterion\":null,\"estimatedMinutes\":30}}|action"
      })
  void s8_rejectsAMalformedBodyNamingTheField(String raw, String field) throws Exception {
    var content = raw.replace("ACTION", createTask("\"Revisar\"", "null", "30"));
    mvc.perform(
            post("/api/v1/me/automations")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(content))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors.length()").value(1))
        .andExpect(jsonPath("$.errors[0].field").value(field));
    verifyNoInteractions(create);
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "\"Revisar\"|null|0|action.estimatedMinutes",
        "\"Revisar\"|null|1441|action.estimatedMinutes",
        "\"Revisar\"|null|30.5|action.estimatedMinutes",
        "\"\"|null|30|action.titleTemplate"
      })
  void s8_rejectsOutOfRangeActionFields(
      String title, String criterion, String minutes, String field) throws Exception {
    mvc.perform(
            post("/api/v1/me/automations")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(
                    body(
                        "\"Regla\"",
                        "\"TaskCreated.v1\"",
                        "null",
                        createTask(title, criterion, minutes))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value(field));
    verifyNoInteractions(create);
  }

  @Test
  void s9_theTwentyFirstRuleIsAConflict() throws Exception {
    when(create.create(eq("owner"), any())).thenThrow(new AutomationLimitException());
    mvc.perform(
            post("/api/v1/me/automations")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(valid()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("RULE_LIMIT"));
  }

  @Test
  void s11_listsOnlyTheItemsKey() throws Exception {
    when(read.list("owner")).thenReturn(List.of(rule(1, draft())));
    mvc.perform(get("/api/v1/me/automations").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].id").value(RULE.toString()));
  }

  @Test
  void s11_readsOneRuleWithItsVersionAsEtag() throws Exception {
    when(read.get("owner", RULE)).thenReturn(rule(3, draft()));
    mvc.perform(get("/api/v1/me/automations/" + RULE).with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"3\""))
        .andExpect(jsonPath("$.version").value(3));
  }

  /**
   * @s12 fila 1 —«enabled false → la lectura devuelve enabled false»
   *     (features/automations.feature:178)— en la capa que pinta el interruptor. Toda la clase
   *     montaba reglas con enabled true, así que {@code AutomationView$Rule::enabled -> true}
   *     sobrevivía: una regla pausada se serializaba como activa y nadie se enteraba. El único
   *     oráculo de la fila vivía sobre el draft (AutomationWiringTest), no sobre el JSON.
   */
  @Test
  void s12_aPausedRuleIsReadAsPaused() throws Exception {
    when(read.get("owner", RULE)).thenReturn(rule(3, paused()));
    mvc.perform(get("/api/v1/me/automations/" + RULE).with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.enabled").value(false));
  }

  private static AutomationDraft paused() {
    return new AutomationDraft(
        "Seguimiento",
        false,
        "TaskCreated.v1",
        null,
        new CreateTaskAction(PROJECT, "Revisar {{task.title}}", null, 30));
  }

  @Test
  void s11_aForeignOrUnknownRuleIsNotFound() throws Exception {
    when(read.get(eq("owner"), any())).thenThrow(new ResourceNotFoundException());
    mvc.perform(get("/api/v1/me/automations/" + RULE).with(user("owner")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void s12_replacingBumpsTheVersionAndTheEtag() throws Exception {
    when(replace.replace(eq("owner"), eq(RULE), eq(1L), any())).thenReturn(rule(2, draft()));
    mvc.perform(
            put("/api/v1/me/automations/" + RULE)
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"1\"")
                .contentType("application/json")
                .content(valid()))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"2\""))
        .andExpect(jsonPath("$.version").value(2));
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {"PUT|\"4x\"", "PUT|4", "DELETE|\"*\"", "DELETE|4"})
  void s13_aMalformedPreconditionIsAValidationError(String method, String ifMatch)
      throws Exception {
    var request =
        (method.equals("PUT")
                ? put("/api/v1/me/automations/" + RULE)
                    .contentType("application/json")
                    .content(valid())
                : delete("/api/v1/me/automations/" + RULE))
            .with(user("owner"))
            .with(csrf().asHeader())
            .header("If-Match", ifMatch);
    mvc.perform(request)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("If-Match"));
    verifyNoInteractions(replace, delete);
  }

  @ParameterizedTest
  @ValueSource(strings = {"PUT", "DELETE"})
  void s13_aMissingPreconditionIsRequired(String method) throws Exception {
    var request =
        (method.equals("PUT")
                ? put("/api/v1/me/automations/" + RULE)
                    .contentType("application/json")
                    .content(valid())
                : delete("/api/v1/me/automations/" + RULE))
            .with(user("owner"))
            .with(csrf().asHeader());
    mvc.perform(request)
        .andExpect(status().is(428))
        .andExpect(jsonPath("$.code").value("PRECONDITION_REQUIRED"));
    verifyNoInteractions(replace, delete);
  }

  @Test
  void s13_aStalePreconditionIsAnAutomationConflict() throws Exception {
    when(replace.replace(eq("owner"), eq(RULE), eq(3L), any()))
        .thenThrow(new AutomationConflictException());
    mvc.perform(
            put("/api/v1/me/automations/" + RULE)
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"3\"")
                .contentType("application/json")
                .content(valid()))
        .andExpect(status().isPreconditionFailed())
        .andExpect(jsonPath("$.code").value("AUTOMATION_CONFLICT"));
  }

  @Test
  void s14_deletingAnswersWithoutABody() throws Exception {
    mvc.perform(
            delete("/api/v1/me/automations/" + RULE)
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"2\""))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));
    verify(delete).delete("owner", RULE, 2L);
  }

  @Test
  void s36_privateRoutesRejectAnonymousCallers() throws Exception {
    mvc.perform(get("/api/v1/me/automations")).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/v1/me/automations").contentType("application/json").content(valid()))
        .andExpect(status().isUnauthorized());
    verifyNoInteractions(create, read);
  }

  @Test
  void s36_writesWithoutACsrfTokenAreForbidden() throws Exception {
    mvc.perform(
            post("/api/v1/me/automations")
                .with(user("owner"))
                .contentType("application/json")
                .content(valid()))
        .andExpect(status().isForbidden());
    verifyNoInteractions(create);
  }

  @Test
  void s36_aForeignOriginIsForbidden() throws Exception {
    mvc.perform(
            put("/api/v1/me/automations/" + RULE)
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("Origin", "https://evil.example")
                .header("If-Match", "\"1\"")
                .contentType("application/json")
                .content(valid()))
        .andExpect(status().isForbidden());
    verifyNoInteractions(replace);
  }

  @Test
  void s36_aNonJsonContentTypeIsUnsupported() throws Exception {
    mvc.perform(
            post("/api/v1/me/automations")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("text/plain")
                .content(valid()))
        .andExpect(status().isUnsupportedMediaType());
    verifyNoInteractions(create);
  }

  @Test
  void s36_patchIsNotAllowed() throws Exception {
    mvc.perform(
            patch("/api/v1/me/automations/" + RULE)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(valid()))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(header().string("Allow", "GET, PUT, DELETE"))
        .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    verifyNoInteractions(create, read, replace, delete);
  }

  @Test
  void s36_aStorageFailureIsServiceUnavailable() throws Exception {
    when(create.create(eq("owner"), any()))
        .thenThrow(new StorageUnavailableException(new IllegalStateException("private")));
    mvc.perform(
            post("/api/v1/me/automations")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(valid()))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"));
  }

  @Test
  void s34_pagesTheHistoryWithItemsAndNextCursor() throws Exception {
    var run =
        new AutomationRun(
            UUID.fromString("44444444-4444-4444-8444-444444444444"),
            RULE,
            "owner",
            UUID.fromString("55555555-5555-4555-8555-555555555555"),
            "TaskCreated.v1",
            NOW,
            1,
            "succeeded",
            PROJECT,
            null,
            null,
            NOW);
    when(runs.read(eq("owner"), eq(RULE), isNull()))
        .thenReturn(new AutomationRunPage(List.of(run), null));
    mvc.perform(get("/api/v1/me/automations/" + RULE + "/runs").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(
            content()
                .json(
                    """
                    {"items":[{"id":"44444444-4444-4444-8444-444444444444",
                      "eventId":"55555555-5555-4555-8555-555555555555",
                      "eventType":"TaskCreated.v1","occurredAt":"2026-09-08T10:15:30.123456Z",
                      "attempt":1,"status":"succeeded","createdTaskId":"%s","deliveryId":null,
                      "errorCode":null,"executedAt":"2026-09-08T10:15:30.123456Z"}],
                     "nextCursor":null}"""
                        .formatted(PROJECT),
                    true));
  }

  /**
   * @s34 —«cada item contiene exactamente id, eventId, ..., deliveryId, ... con null donde no
   *     aplica» (features/automations.feature:454)— para la mitad que sí aplica. El fixture de
   *     {@link #s34_pagesTheHistoryWithItemsAndNextCursor()} lleva {@code deliveryId} null y afirma
   *     null, así que ni {@code AutomationView$Run::deliveryId} ni {@code
   *     AutomationRun::deliveryId} podían caer: devolver null era exactamente lo esperado. Una
   *     ejecución de webhook sin su entrega deja al propietario sin el rastro que enlaza la regla
   *     con lo que se envió (@s26, :341).
   */
  @Test
  void s34_aWebhookRunNamesTheDeliveryItProduced() throws Exception {
    var delivery = UUID.fromString("66666666-6666-4666-8666-666666666666");
    var run =
        new AutomationRun(
            UUID.fromString("44444444-4444-4444-8444-444444444444"),
            RULE,
            "owner",
            UUID.fromString("55555555-5555-4555-8555-555555555555"),
            "ProjectStatusChanged.v1",
            NOW,
            1,
            "succeeded",
            null,
            delivery,
            null,
            NOW);
    when(runs.read(eq("owner"), eq(RULE), isNull()))
        .thenReturn(new AutomationRunPage(List.of(run), null));
    mvc.perform(get("/api/v1/me/automations/" + RULE + "/runs").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].deliveryId").value(delivery.toString()))
        .andExpect(jsonPath("$.items[0].createdTaskId").value(org.hamcrest.Matchers.nullValue()));
  }

  @Test
  void s35_aMalformedCursorIsAValidationError() throws Exception {
    mvc.perform(
            get("/api/v1/me/automations/" + RULE + "/runs")
                .param("cursor", "no-base64!")
                .with(user("owner")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("cursor"));
    verifyNoInteractions(runs);
  }

  @Test
  void s30_simulatesWithoutSavingAnything() throws Exception {
    var match =
        new AutomationMatch(
            UUID.fromString("55555555-5555-4555-8555-555555555555"),
            "TaskCreated.v1",
            NOW,
            // El criterio va con el valor que pide el contrato (:399), no vacío: con "" el
            // fixture coincidía con el mutante EmptyObjectReturnVals de
            // AutomationView$TaskPreview::completionCriterion y lo hacía indetectable.
            new ActionPreview.Task(
                PROJECT,
                "Revisar Redactar informe",
                "TaskCreated.v1 a las 2026-09-08T10:15:30.123456Z",
                30,
                null),
            false);
    when(simulate.simulate(eq("owner"), any()))
        .thenReturn(new AutomationSimulation(5, List.of(match)));
    mvc.perform(
            post("/api/v1/me/automations/simulate")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(valid()))
        .andExpect(status().isOk())
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(
            content()
                .json(
                    """
                    {"evaluatedEvents":5,
                     "matches":[{"eventId":"55555555-5555-4555-8555-555555555555",
                       "eventType":"TaskCreated.v1","occurredAt":"2026-09-08T10:15:30.123456Z",
                       "preview":{"type":"CREATE_TASK","projectId":"%s",
                         "title":"Revisar Redactar informe",
                         "completionCriterion":"TaskCreated.v1 a las 2026-09-08T10:15:30.123456Z",
                         "estimatedMinutes":30,"wouldFail":null},
                       "loopGuarded":false}]}"""
                        .formatted(PROJECT),
                    true));
    verifyNoInteractions(create);
  }

  @Test
  void s33_simulatingRejectsAnIdInTheBody() throws Exception {
    mvc.perform(
            post("/api/v1/me/automations/simulate")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"id\":\"" + RULE + "\"," + valid().substring(1)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("body"));
    verifyNoInteractions(simulate);
  }

  @Test
  void s33_simulatingReportsTheWebhookPreviewClosed() throws Exception {
    var match =
        new AutomationMatch(
            UUID.fromString("55555555-5555-4555-8555-555555555555"),
            "ProjectStatusChanged.v1",
            NOW,
            new ActionPreview.Webhook(
                ENDPOINT, UUID.fromString("55555555-5555-4555-8555-555555555555")),
            false);
    when(simulate.simulate(eq("owner"), any()))
        .thenReturn(new AutomationSimulation(1, List.of(match)));
    mvc.perform(
            post("/api/v1/me/automations/simulate")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(
                    body(
                        "\"Regla\"",
                        "\"ProjectStatusChanged.v1\"",
                        "null",
                        "{\"type\":\"NOTIFY_WEBHOOK\",\"endpointId\":\"" + ENDPOINT + "\"}")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.matches[0].preview.length()").value(3))
        .andExpect(jsonPath("$.matches[0].preview.type").value("NOTIFY_WEBHOOK"))
        .andExpect(jsonPath("$.matches[0].preview.endpointId").value(ENDPOINT.toString()))
        // La tercera clave de la fila 4 de @s32 (:428). Sin ella, `length()==3` seguía valiendo
        // con el eventId a null y AutomationView$WebhookPreview::eventId -> null sobrevivía:
        // la vista previa dejaría de decir sobre QUÉ evento se dispararía el envío.
        .andExpect(
            jsonPath("$.matches[0].preview.eventId").value("55555555-5555-4555-8555-555555555555"));
  }

  /**
   * @s32 fila 3 —«TaskCreated.v1 de una tarea creada por automatización → loopGuarded true y
   *     preview resuelta» (features/automations.feature:427)—. Ningún fixture de la clase traía una
   *     coincidencia guardada, así que {@code AutomationView$Match::loopGuarded -> false}
   *     sobrevivía y la pantalla mostraría como disparable lo que la guarda va a saltarse.
   */
  @Test
  void s32_aGuardedMatchSaysSoAndStillResolvesItsPreview() throws Exception {
    var match =
        new AutomationMatch(
            UUID.fromString("55555555-5555-4555-8555-555555555555"),
            "TaskCreated.v1",
            NOW,
            new ActionPreview.Task(PROJECT, "Revisar Redactar informe", null, 30, null),
            true);
    when(simulate.simulate(eq("owner"), any()))
        .thenReturn(new AutomationSimulation(1, List.of(match)));
    mvc.perform(
            post("/api/v1/me/automations/simulate")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(valid()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.matches[0].loopGuarded").value(true))
        .andExpect(jsonPath("$.matches[0].preview.title").value("Revisar Redactar informe"));
  }

  @Test
  void s33_simulatingSurfacesTheSameReferenceErrorsAsCreating() throws Exception {
    when(simulate.simulate(eq("owner"), any()))
        .thenThrow(new AutomationTargetNotFoundException("action.projectId"));
    mvc.perform(
            post("/api/v1/me/automations/simulate")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(valid()))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("TARGET_NOT_FOUND"));
  }

  @Test
  void s11_aMalformedRuleIdIsAValidationError() throws Exception {
    mvc.perform(get("/api/v1/me/automations/not-a-uuid").with(user("owner")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("id"));
    verifyNoInteractions(read);
  }

  @Test
  void s34_readsAPageBehindAnOpaqueCursor() throws Exception {
    var cursor =
        new AutomationRunCursor(RULE, NOW, UUID.fromString("44444444-4444-4444-8444-444444444444"));
    when(runs.read("owner", RULE, null)).thenReturn(new AutomationRunPage(List.of(), cursor));
    var encoded =
        com.jayway.jsonpath.JsonPath.read(
            mvc.perform(get("/api/v1/me/automations/" + RULE + "/runs").with(user("owner")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.nextCursor");
    when(runs.read(eq("owner"), eq(RULE), eq(cursor)))
        .thenReturn(new AutomationRunPage(List.of(), null));
    mvc.perform(
            get("/api/v1/me/automations/" + RULE + "/runs")
                .param("cursor", encoded.toString())
                .with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"items\":[],\"nextCursor\":null}", true));
  }

  @Test
  void s35_aCursorOfAnotherRuleIsRejectedBeforeReading() throws Exception {
    var other = new AutomationRunCursor(PROJECT, NOW, RULE);
    when(runs.read("owner", RULE, other))
        .thenThrow(
            new ValidationException(
                List.of(
                    new FieldError("cursor", "INVALID_VALUE", "Revisa el valor de este campo."))));
    mvc.perform(
            get("/api/v1/me/automations/" + RULE + "/runs")
                .param("cursor", encodedCursorOf(other))
                .with(user("owner")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("cursor"));
  }

  /** Round-trips a cursor through the API itself, so the test never encodes it by hand. */
  private String encodedCursorOf(AutomationRunCursor cursor) throws Exception {
    var probe = UUID.fromString("99999999-9999-4999-8999-999999999999");
    when(runs.read("owner", probe, null)).thenReturn(new AutomationRunPage(List.of(), cursor));
    return com.jayway.jsonpath.JsonPath.read(
            mvc.perform(get("/api/v1/me/automations/" + probe + "/runs").with(user("owner")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.nextCursor")
        .toString();
  }

  @Test
  void s35_anUnknownRuleHidesItsHistory() throws Exception {
    when(runs.read(eq("owner"), any(), isNull())).thenThrow(new ResourceNotFoundException());
    mvc.perform(get("/api/v1/me/automations/" + RULE + "/runs").with(user("owner")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void s11_aRuleWithACriterionEchoesItInstead() throws Exception {
    var withCriterion =
        new AutomationDraft(
            "Regla",
            true,
            "TaskCreated.v1",
            null,
            new CreateTaskAction(PROJECT, "Revisar", "{{event.type}}", null));
    when(read.get("owner", RULE)).thenReturn(rule(1, withCriterion));
    mvc.perform(get("/api/v1/me/automations/" + RULE).with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.action.criterionTemplate").value("{{event.type}}"))
        .andExpect(jsonPath("$.action.estimatedMinutes").doesNotExist());
  }

  @Test
  void s11_anEmptyListIsStillTheItemsEnvelope() throws Exception {
    when(read.list("owner")).thenReturn(List.of());
    mvc.perform(get("/api/v1/me/automations").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"items\":[]}", true));
  }

  @Test
  void s12_replacingValidatesTheBodyBeforeTouchingTheUseCase() throws Exception {
    mvc.perform(
            put("/api/v1/me/automations/" + RULE)
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"1\"")
                .contentType("application/json")
                .content(
                    body(
                        "\"Regla\"",
                        "\"TaskCreated.v9\"",
                        "null",
                        createTask("\"Revisar\"", "null", "null"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("UNKNOWN_EVENT_TYPE"));
    verifyNoInteractions(replace);
  }

  @Test
  void s11_readingIsNotAffectedByAnAbsentOptionalCursor() throws Exception {
    when(runs.read("owner", RULE, null)).thenReturn(new AutomationRunPage(List.of(), null));
    mvc.perform(get("/api/v1/me/automations/" + RULE + "/runs").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"items\":[]}", false));
    verify(runs).read("owner", RULE, null);
  }

  @Test
  void s8_aBodyThatIsNotAnObjectIsRejected() throws Exception {
    for (var raw : List.of("[]", "\"x\"", "null", "{")) {
      mvc.perform(
              post("/api/v1/me/automations")
                  .with(user("owner"))
                  .with(csrf().asHeader())
                  .contentType("application/json")
                  .content(raw))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].field").value("body"));
    }
    verifyNoInteractions(create);
  }

  @Test
  void s1_theOwnerNeverComesFromTheBody() throws Exception {
    when(create.create(eq("owner"), any())).thenReturn(rule(1, draft()));
    mvc.perform(
            post("/api/v1/me/automations")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(valid()))
        .andExpect(status().isCreated());
    verify(create).create(eq("owner"), eq(draft()));
    verifyNoMoreInteractions(create);
  }

  @Test
  void s8_aTooLongNameIsRejectedByCodePoints() throws Exception {
    var emoji = "😀".repeat(81);
    mvc.perform(
            post("/api/v1/me/automations")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(
                    body(
                        "\"" + emoji + "\"",
                        "\"TaskCreated.v1\"",
                        "null",
                        createTask("\"Revisar\"", "null", "30"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("name"));
    verifyNoInteractions(create);
  }

  @Test
  void s11_getIsNotAWrite() throws Exception {
    when(read.get("owner", RULE)).thenReturn(rule(1, draft()));
    mvc.perform(get("/api/v1/me/automations/" + RULE).with(user("owner")))
        .andExpect(status().isOk());
    verifyNoInteractions(create, replace, delete, simulate);
  }

  @Test
  void s13_aTriggerChangeStillNeedsThePrecondition() throws Exception {
    when(replace.replace(eq("owner"), eq(RULE), eq(4L), any())).thenReturn(rule(5, draft()));
    mvc.perform(
            put("/api/v1/me/automations/" + RULE)
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"4\"")
                .contentType("application/json")
                .content(
                    body(
                        "\"Regla\"",
                        "\"TaskStatusChanged.v1\"",
                        "null",
                        createTask("\"Revisar\"", "null", "null"))))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"5\""));
  }

  @Test
  void s14_deletingAnUnknownRuleIsNotFound() throws Exception {
    doThrow(new ResourceNotFoundException()).when(delete).delete("owner", RULE, 2L);
    mvc.perform(
            delete("/api/v1/me/automations/" + RULE)
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"2\""))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void s2_theTriggerObjectIsClosedToo() throws Exception {
    mvc.perform(
            post("/api/v1/me/automations")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(
                    """
                    {"name":"R","enabled":true,"trigger":{"eventType":"TaskCreated.v1","extra":1},"condition":null,"action":%s}"""
                        .formatted(createTask("\"Revisar\"", "null", "null"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("trigger"));
    verifyNoInteractions(create);
  }

  @Test
  void s5_theWebhookActionIsClosedToo() throws Exception {
    mvc.perform(
            post("/api/v1/me/automations")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(
                    body(
                        "\"R\"",
                        "\"ProjectStatusChanged.v1\"",
                        "null",
                        "{\"type\":\"NOTIFY_WEBHOOK\",\"endpointId\":\""
                            + ENDPOINT
                            + "\",\"extra\":1}")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("action"));
    verifyNoInteractions(create);
  }
}
