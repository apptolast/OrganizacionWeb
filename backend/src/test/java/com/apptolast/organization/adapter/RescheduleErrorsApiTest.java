package com.apptolast.organization.adapter;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = {
      "app.auth.username=persona-a",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@AutoConfigureMockMvc
@Testcontainers
class RescheduleErrorsApiTest {
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  // Match the API suites: retain the JDBC endpoint while Spring caches its context.
  // Testcontainers Ryuk owns cleanup when the test JVM exits.
  static {
    postgres.start();
  }

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  @MockitoBean Clock clock;
  UUID project, task, block, preference;

  @BeforeEach
  void seed() {
    org.mockito.Mockito.when(clock.instant()).thenReturn(Instant.parse("2030-01-07T09:00:00Z"));
    jdbc.execute(
        "TRUNCATE"
            + " block_changes,block_projections,planned_blocks,availability_preferences,task_status_history,tasks,outbox_events,projects");
    project = UUID.randomUUID();
    task = UUID.randomUUID();
    block = UUID.randomUUID();
    preference = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES"
            + " (?,'persona-a','Proyecto','','active',now(),now())",
        project);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at)"
            + " VALUES (?,?,'Tarea','','pending',now(),now())",
        task,
        project);
    jdbc.update(
        "INSERT INTO"
            + " availability_preferences(id,owner_id,zone_id,monday_minutes,tuesday_minutes,wednesday_minutes,thursday_minutes,friday_minutes,saturday_minutes,sunday_minutes,version,created_at,updated_at)"
            + " VALUES (?,'persona-a','UTC',120,120,120,120,120,120,120,0,now(),now())",
        preference);
    jdbc.update(
        "INSERT INTO"
            + " planned_blocks(id,project_id,task_id,request_key,objective,start_local,end_local,zone_id,start_offset,end_offset,allow_over_budget,start_at,end_at,duration_minutes,created_at)"
            + " VALUES (?,?,?,?,'Preparar borrador',TIMESTAMP '2030-01-07 10:00',TIMESTAMP"
            + " '2030-01-07 11:00','UTC','Z','Z',false,TIMESTAMPTZ '2030-01-07 10:00Z',TIMESTAMPTZ"
            + " '2030-01-07 11:00Z',60,TIMESTAMPTZ '2030-01-06 10:00Z')",
        block,
        project,
        task,
        UUID.randomUUID());
  }

  String base() {
    return "/api/v1/projects/" + project + "/tasks/" + task + "/blocks/" + block + "/reschedule";
  }

  Map<String, Object> body(boolean commit) {
    var value = new LinkedHashMap<String, Object>();
    value.put("startLocal", "2030-01-07T12:00");
    value.put("endLocal", "2030-01-07T13:00");
    value.put("zoneId", "UTC");
    value.put("startOffset", "Z");
    value.put("endOffset", "Z");
    if (commit) value.put("allowOverBudget", false);
    return value;
  }

  Map<String, List<Map<String, Object>>> rows() {
    var result = new LinkedHashMap<String, List<Map<String, Object>>>();
    for (var table :
        List.of(
            "planned_blocks",
            "block_projections",
            "block_changes",
            "outbox_events",
            "availability_preferences"))
      result.put(table, jdbc.queryForList("SELECT * FROM " + table));
    return result;
  }

  MockHttpServletResponse request(boolean commit, Map<String, Object> body) throws Exception {
    return request(commit, body, "persona-a");
  }

  MockHttpServletResponse request(boolean commit, Map<String, Object> body, String owner)
      throws Exception {
    var before = rows();
    var request =
        post(base() + (commit ? "" : "/preview"))
            .with(user(owner))
            .with(csrf().asHeader())
            .header("If-Match", "\"block:" + block + ":1\"")
            .contentType("application/json")
            .content(json.writeValueAsString(body));
    if (commit)
      request
          .header("Availability-Revision", "\"availability:" + preference + ":0\"")
          .header("Idempotency-Key", UUID.randomUUID());
    var response = mvc.perform(request).andReturn().getResponse();
    assertThat(rows()).isEqualTo(before);
    return response;
  }

  JsonNode problem(
      MockHttpServletResponse response, int status, String code, String title, String... fields)
      throws Exception {
    assertThat(response.getStatus()).isEqualTo(status);
    assertThat(response.getContentType()).isEqualTo("application/problem+json");
    assertThat(response.getHeader("Cache-Control")).contains("no-store");
    var value = json.readTree(response.getContentAsString());
    var expected = new ArrayList<>(List.of("type", "title", "status", "code"));
    expected.addAll(List.of(fields));
    var names = new ArrayList<String>();
    value.fieldNames().forEachRemaining(names::add);
    assertThat(names).containsExactlyInAnyOrderElementsOf(expected);
    assertThat(value.get("type").asText())
        .isEqualTo("urn:organization:problem:" + code.toLowerCase(Locale.ROOT));
    assertThat(value.get("status").asInt()).isEqualTo(status);
    assertThat(value.get("code").asText()).isEqualTo(code);
    assertThat(value.get("title").asText()).isEqualTo(title);
    return value;
  }

  @Test
  void moveRejectsBudgetExcessWithCurrentDays() throws Exception {
    jdbc.update("UPDATE availability_preferences SET monday_minutes=30 WHERE id=?", preference);
    var error =
        problem(
            request(true, body(true)),
            409,
            "BUDGET_EXCEEDED",
            "Revisa el presupuesto y confirma si quieres superar su límite.",
            "budgetZoneId",
            "days");
    assertThat(error.get("budgetZoneId").asText()).isEqualTo("UTC");
    assertThat(error.get("days"))
        .isEqualTo(
            json.readTree(
                "[{\"date\":\"2030-01-07\",\"budgetMinutes\":30,\"plannedSeconds\":0,\"requestedSeconds\":3600,\"excessSeconds\":1800}]"));
  }

  @Test
  void moveRejectsOverlapEvenWithBudgetConsent() throws Exception {
    var conflict = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO planned_blocks SELECT ?,project_id,task_id,?,objective,start_local+INTERVAL '2"
            + " hours',end_local+INTERVAL '2"
            + " hours',zone_id,start_offset,end_offset,allow_over_budget,start_at+INTERVAL '2"
            + " hours',end_at+INTERVAL '2 hours',duration_minutes,created_at FROM planned_blocks"
            + " WHERE id=?",
        conflict,
        UUID.randomUUID(),
        block);
    var body = body(true);
    body.put("allowOverBudget", true);
    var error =
        problem(
            request(true, body),
            409,
            "BLOCK_OVERLAP",
            "El intervalo coincide con otro bloque planificado.",
            "conflict");
    assertThat(error.get("conflict"))
        .isEqualTo(json.valueToTree(Map.of("id", conflict, "projectId", project, "taskId", task)));
  }

  @Test
  void moveRequiresAvailabilityBeforeCompletedProject() throws Exception {
    jdbc.update("DELETE FROM availability_preferences WHERE id=?", preference);
    jdbc.update("UPDATE projects SET status='completed' WHERE id=?", project);
    problem(
        request(true, body(true)),
        409,
        "AVAILABILITY_REQUIRED",
        "Configura tu disponibilidad antes de planificar.");
  }

  @Test
  void previewRejectsUnavailablePreferenceZone() throws Exception {
    jdbc.update(
        "UPDATE availability_preferences SET zone_id='Retired/Zone' WHERE id=?", preference);
    problem(
        request(false, body(false)),
        409,
        "AVAILABILITY_ZONE_UNAVAILABLE",
        "Actualiza la zona de tu disponibilidad antes de planificar.");
  }

  @Test
  void moveRejectsCompletedTaskWithValidPreference() throws Exception {
    jdbc.update("UPDATE tasks SET status='completed',completed_at=updated_at WHERE id=?", task);
    problem(
        request(true, body(true)),
        409,
        "TASK_COMPLETED",
        "Reabre la tarea antes de planificar un bloque.");
  }

  @Test
  void previewReportsBothOccurrencesOfAmbiguousStart() throws Exception {
    var body = body(false);
    body.put("startLocal", "2030-10-27T02:30");
    body.put("endLocal", "2030-10-27T03:30");
    body.put("zoneId", "Europe/Madrid");
    body.put("startOffset", null);
    body.put("endOffset", null);
    var error =
        problem(
            request(false, body),
            400,
            "VALIDATION_ERROR",
            "Revisa la ocurrencia de la hora indicada.",
            "errors",
            "validOffsets");
    assertThat(error.get("errors"))
        .isEqualTo(
            json.valueToTree(
                List.of(
                    Map.of(
                        "field",
                        "startOffset",
                        "code",
                        "AMBIGUOUS_OFFSET",
                        "message",
                        "Elige una de las ocurrencias de esta hora."))));
    assertThat(error.get("validOffsets"))
        .isEqualTo(json.valueToTree(Map.of("startOffset", List.of("+02:00", "+01:00"))));
  }

  @Test
  void moveReportsValidOffsetForAnIncorrectEndOccurrence() throws Exception {
    var body = body(true);
    body.put("startLocal", "2030-10-27T02:30");
    body.put("endLocal", "2030-10-27T03:30");
    body.put("zoneId", "Europe/Madrid");
    body.put("startOffset", "+02:00");
    body.put("endOffset", "+02:00");
    var error =
        problem(
            request(true, body),
            400,
            "VALIDATION_ERROR",
            "Revisa la ocurrencia de la hora indicada.",
            "errors",
            "validOffsets");
    assertThat(error.get("errors"))
        .isEqualTo(
            json.valueToTree(
                List.of(
                    Map.of(
                        "field",
                        "endOffset",
                        "code",
                        "INVALID_OFFSET",
                        "message",
                        "El desplazamiento no corresponde a esta hora y zona."))));
    assertThat(error.get("validOffsets"))
        .isEqualTo(json.valueToTree(Map.of("endOffset", List.of("+01:00"))));
  }

  @Test
  void previewPreservesValidationErrorForNonexistentLocalTime() throws Exception {
    var body = body(false);
    body.put("startLocal", "2030-03-31T02:30");
    body.put("endLocal", "2030-03-31T03:30");
    body.put("zoneId", "Europe/Madrid");
    body.put("startOffset", null);
    body.put("endOffset", null);
    var error =
        problem(
            request(false, body),
            400,
            "VALIDATION_ERROR",
            "Revisa los campos indicados.",
            "errors");
    assertThat(error.get("errors"))
        .isEqualTo(
            json.valueToTree(
                List.of(
                    Map.of(
                        "field",
                        "startLocal",
                        "code",
                        "NONEXISTENT_LOCAL_TIME",
                        "message",
                        "Esta hora no existe en la zona elegida."))));
  }

  @Test
  void anotherOwnerCannotObserveBudgetOrBlockDetails() throws Exception {
    jdbc.update("UPDATE availability_preferences SET monday_minutes=0 WHERE id=?", preference);
    problem(
        request(true, body(true), "persona-b"),
        404,
        "RESOURCE_NOT_FOUND",
        "No se ha encontrado el recurso.");
  }
}
