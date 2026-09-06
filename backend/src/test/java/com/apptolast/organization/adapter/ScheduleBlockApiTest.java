package com.apptolast.organization.adapter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(
    properties = {
      "app.auth.username=persona-a",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@AutoConfigureMockMvc
class ScheduleBlockApiTest {
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  static {
    postgres.start();
  }

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    r.add("spring.datasource.username", postgres::getUsername);
    r.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  @MockitoBean Clock clock;
  @MockitoBean com.apptolast.organization.application.ZoneCatalog catalog;
  UUID project, task, preference;

  String base() {
    return "/api/v1/projects/" + project + "/tasks/" + task + "/blocks";
  }

  @BeforeEach
  void reset() {
    when(catalog.zones()).thenReturn(ZoneId.getAvailableZoneIds());
    when(clock.instant()).thenReturn(Instant.parse("2030-01-01T00:00:00.123456789Z"));
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    jdbc.execute(
        "TRUNCATE planned_blocks,availability_preferences,task_status_history,tasks,outbox_events,projects");
    project = UUID.randomUUID();
    task = UUID.randomUUID();
    preference = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'persona-a','Proyecto','','active',now(),now())",
        project);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,'Tarea','','pending',now(),now())",
        task,
        project);
    jdbc.update(
        "INSERT INTO availability_preferences(id,owner_id,zone_id,monday_minutes,tuesday_minutes,wednesday_minutes,thursday_minutes,friday_minutes,saturday_minutes,sunday_minutes,version,created_at,updated_at) VALUES (?,'persona-a','UTC',120,120,120,120,120,120,120,0,now(),now())",
        preference);
  }

  String body() {
    return "{\"objective\":\" Meta \",\"startLocal\":\"2030-01-07T10:00\",\"endLocal\":\"2030-01-07T11:00\",\"zoneId\":\"UTC\",\"startOffset\":null,\"endOffset\":null}";
  }

  @Test
  void s1_previewReturnsExactSnapshotWithoutWriting() throws Exception {
    var response =
        mvc.perform(
                post(base() + "/preview")
                    .with(user("persona-a"))
                    .with(csrf().asHeader())
                    .contentType("application/json")
                    .content(body()))
            .andExpect(status().isOk())
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse();
    var result = json.readTree(response.getContentAsString());
    assertThat(result.size()).isEqualTo(10);
    assertThat(result.get("objective").asText()).isEqualTo("Meta");
    assertThat(result.get("zoneId").asText()).isEqualTo("UTC");
    assertThat(result.get("startAt").asText()).isEqualTo("2030-01-07T10:00:00Z");
    assertThat(result.get("endAt").asText()).isEqualTo("2030-01-07T11:00:00Z");
    assertThat(result.get("startOffset").asText()).isEqualTo("Z");
    assertThat(result.get("endOffset").asText()).isEqualTo("Z");
    assertThat(result.get("durationMinutes").asInt()).isEqualTo(60);
    assertThat(result.get("availabilityEtag").asText())
        .isEqualTo("\"availability:" + preference + ":0\"");
    assertThat(result.get("budgetZoneId").asText()).isEqualTo("UTC");
    assertThat(result.get("days").size()).isEqualTo(1);
    var day = result.get("days").get(0);
    assertThat(day.size()).isEqualTo(5);
    assertThat(day.get("date").asText()).isEqualTo("2030-01-07");
    assertThat(day.get("budgetMinutes").asInt()).isEqualTo(120);
    assertThat(day.get("plannedSeconds").asLong()).isZero();
    assertThat(day.get("requestedSeconds").asLong()).isEqualTo(3600);
    assertThat(day.get("excessSeconds").asLong()).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "objective,missing,REQUIRED",
    "objective,null,REQUIRED",
    "objective,7,INVALID_TYPE",
    "objective,[],INVALID_TYPE",
    "objective,{},INVALID_TYPE",
    "startLocal,missing,REQUIRED",
    "startLocal,null,REQUIRED",
    "startLocal,7,INVALID_TYPE",
    "endLocal,missing,REQUIRED",
    "endLocal,null,REQUIRED",
    "endLocal,7,INVALID_TYPE",
    "zoneId,missing,REQUIRED",
    "zoneId,null,REQUIRED",
    "zoneId,7,INVALID_TYPE",
    "startOffset,missing,REQUIRED",
    "startOffset,7,INVALID_TYPE",
    "endOffset,missing,REQUIRED",
    "endOffset,7,INVALID_TYPE",
    "extra,7,UNKNOWN_FIELD"
  })
  void s4_invalidStructure(String field, String value, String code) throws Exception {
    var data = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(body());
    if (value.equals("missing")) data.remove(field);
    else data.set(field, json.readTree(value));
    mvc.perform(
            post(base() + "/preview")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(data.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value(field))
        .andExpect(jsonPath("$.errors[0].code").value(code));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "2030-1-07T10:00",
        "2030-01-07 11:00",
        "2030-02-30T10:00",
        "2030-01-07T24:00",
        "2030-01-07T10:00:00",
        "2030-01-07T11:00Z",
        "0000-01-01T10:00",
        "10000-01-01T11:00"
      })
  void s5_strictLocal(String value) throws Exception {
    var data = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(body());
    data.put("startLocal", value);
    mvc.perform(
            post(base() + "/preview")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(data.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("startLocal"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_FORMAT"));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"+00:00", "+1:00", "foo", ""})
  void s9_canonicalOffset(String value) throws Exception {
    var data = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(body());
    data.put("startOffset", value);
    mvc.perform(
            post(base() + "/preview")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(data.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("startOffset"));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"{", "{\"objective\":\"a\",\"objective\":\"b\"}", "{} {}", ""})
  void s62_unreadableJson(String value) throws Exception {
    mvc.perform(
            post(base() + "/preview")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(value))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_JSON"))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"null", "[]", "7", "true", "\"text\""})
  void s4_rootObject(String value) throws Exception {
    mvc.perform(
            post(base() + "/preview")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(value))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("body"));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({"extra,query", "project,projectId", "task,taskId"})
  void s19_queryThenIdsBeforeBody(String defect, String field) throws Exception {
    String url = base() + "/preview";
    if (defect.equals("extra")) url = url.replace(project.toString(), "1-1-1-1-1") + "?extra=1";
    if (defect.equals("project")) url = url.replace(project.toString(), "1-1-1-1-1");
    if (defect.equals("task")) url = url.replace(task.toString(), "invalid");
    mvc.perform(
            post(url)
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value(field));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"other-owner", "missing-project", "missing-task", "wrong-project"})
  void s19_ownershipBeforeAvailability(String defect) throws Exception {
    jdbc.update("DELETE FROM availability_preferences");
    if (defect.equals("other-owner"))
      jdbc.update("UPDATE projects SET owner_id='persona-b' WHERE id=?", project);
    if (defect.equals("missing-project")) project = UUID.randomUUID();
    if (defect.equals("missing-task")) task = UUID.randomUUID();
    if (defect.equals("wrong-project")) {
      var other = UUID.randomUUID();
      jdbc.update(
          "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'persona-a','Otro','','active',now(),now())",
          other);
      project = other;
    }
    mvc.perform(
            post(base() + "/preview")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(body()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  String createBody() {
    return body().replace("null", "\"Z\"").replace("}", ",\"allowOverBudget\":false}");
  }

  String revision() {
    return "\"availability:" + preference + ":0\"";
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"idea", "active", "paused"})
  void s2_createAtomic(String state) throws Exception {
    jdbc.update("UPDATE projects SET status=? WHERE id=?", state, project);
    var beforeProject = jdbc.queryForMap("SELECT * FROM projects WHERE id=?", project);
    var beforeTask = jdbc.queryForMap("SELECT * FROM tasks WHERE id=?", task);
    var beforeAvailability =
        jdbc.queryForMap("SELECT * FROM availability_preferences WHERE id=?", preference);
    var key = UUID.randomUUID();
    var response =
        mvc.perform(
                post(base())
                    .with(user("persona-a"))
                    .with(csrf().asHeader())
                    .header("Availability-Revision", revision())
                    .header("Idempotency-Key", key)
                    .contentType("application/json")
                    .content(createBody()))
            .andExpect(status().isCreated())
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse();
    var block = json.readTree(response.getContentAsString());
    assertThat(block.size()).isEqualTo(9);
    assertThat(block.get("projectId").asText()).isEqualTo(project.toString());
    assertThat(block.get("taskId").asText()).isEqualTo(task.toString());
    assertThat(block.get("objective").asText()).isEqualTo("Meta");
    assertThat(block.get("startAt").asText()).isEqualTo("2030-01-07T10:00:00Z");
    assertThat(block.get("endAt").asText()).isEqualTo("2030-01-07T11:00:00Z");
    assertThat(block.get("zoneId").asText()).isEqualTo("UTC");
    assertThat(block.get("durationMinutes").asInt()).isEqualTo(60);
    assertThat(block.get("createdAt").asText()).isEqualTo("2030-01-01T00:00:00.123456Z");
    assertThat(UUID.fromString(block.get("id").asText())).isNotNull();
    assertThat(response.getHeader("Location")).isEqualTo(base() + "/" + block.get("id").asText());
    var saved = jdbc.queryForMap("SELECT * FROM planned_blocks");
    assertThat(saved.get("request_key")).isEqualTo(key);
    assertThat(saved.get("objective")).isEqualTo("Meta");
    var event =
        json.readTree(jdbc.queryForObject("SELECT payload::text FROM outbox_events", String.class));
    assertThat(event.size()).isEqualTo(12);
    assertThat(event.get("type").asText()).isEqualTo("BlockPlanned.v1");
    assertThat(event.get("blockId").asText()).isEqualTo(block.get("id").asText());
    assertThat(event.get("aggregateId").asText()).isEqualTo(project.toString());
    assertThat(UUID.fromString(event.get("eventId").asText())).isNotNull();
    assertThat(event.get("ownerId").asText()).isEqualTo("persona-a");
    assertThat(event.get("occurredAt").asText()).isEqualTo(block.get("createdAt").asText());
    assertThat(event.get("schemaVersion").asInt()).isEqualTo(1);
    assertThat(event.get("taskId").asText()).isEqualTo(task.toString());
    for (var field : List.of("startAt", "endAt", "zoneId", "durationMinutes"))
      assertThat(event.get(field)).isEqualTo(block.get(field));
    assertThat(jdbc.queryForMap("SELECT * FROM projects WHERE id=?", project))
        .isEqualTo(beforeProject);
    assertThat(jdbc.queryForMap("SELECT * FROM tasks WHERE id=?", task)).isEqualTo(beforeTask);
    assertThat(jdbc.queryForMap("SELECT * FROM availability_preferences WHERE id=?", preference))
        .isEqualTo(beforeAvailability);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "missing,missing,428,PRECONDITION_REQUIRED,none",
    "bad,missing,400,VALIDATION_ERROR,Availability-Revision",
    "valid,missing,400,VALIDATION_ERROR,Idempotency-Key",
    "valid,bad,400,VALIDATION_ERROR,Idempotency-Key",
    "unconfigured,valid,400,VALIDATION_ERROR,Availability-Revision",
    "duplicate,valid,400,VALIDATION_ERROR,Availability-Revision",
    "valid,duplicate,400,VALIDATION_ERROR,Idempotency-Key",
    "overflow,valid,400,VALIDATION_ERROR,Availability-Revision",
    "different,valid,412,AVAILABILITY_CONFLICT,none"
  })
  void s18_headers(String revisionMode, String keyMode, int statusCode, String code, String field)
      throws Exception {
    var request =
        post(base())
            .with(user("persona-a"))
            .with(csrf().asHeader())
            .contentType("application/json")
            .content(revisionMode.equals("different") ? createBody() : "{");
    if (!revisionMode.equals("missing"))
      request.header(
          "Availability-Revision",
          switch (revisionMode) {
            case "bad" -> "bad";
            case "unconfigured" -> "\"availability:unconfigured\"";
            case "overflow" -> "\"availability:" + preference + ":9223372036854775808\"";
            case "different" -> "\"availability:" + preference + ":1\"";
            default -> revision();
          });
    if (revisionMode.equals("duplicate")) request.header("Availability-Revision", revision());
    if (!keyMode.equals("missing"))
      request.header("Idempotency-Key", keyMode.equals("bad") ? "1-1-1-1-1" : UUID.randomUUID());
    if (keyMode.equals("duplicate")) request.header("Idempotency-Key", UUID.randomUUID());
    var result =
        mvc.perform(request)
            .andExpect(status().is(statusCode))
            .andExpect(jsonPath("$.code").value(code));
    if (!field.equals("none")) result.andExpect(jsonPath("$.errors[0].field").value(field));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "allowOverBudget,missing,REQUIRED",
    "allowOverBudget,null,REQUIRED",
    "allowOverBudget,1,INVALID_TYPE",
    "allowOverBudget,\"true\",INVALID_TYPE",
    "allowOverBudget,[],INVALID_TYPE",
    "allowOverBudget,{},INVALID_TYPE",
    "startOffset,null,REQUIRED",
    "endOffset,null,REQUIRED",
    "extra,7,UNKNOWN_FIELD"
  })
  void s4_creationStructure(String field, String value, String code) throws Exception {
    var data = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(createBody());
    if (value.equals("missing")) data.remove(field);
    else data.set(field, json.readTree(value));
    mvc.perform(
            post(base())
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .header("Availability-Revision", revision())
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType("application/json")
                .content(data.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value(field))
        .andExpect(jsonPath("$.errors[0].code").value(code));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  org.springframework.mock.web.MockHttpServletResponse create(UUID key, String content)
      throws Exception {
    return mvc.perform(
            post(base())
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .header("Availability-Revision", revision())
                .header("Idempotency-Key", key)
                .contentType("application/json")
                .content(content))
        .andReturn()
        .getResponse();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "past",
        "completed",
        "availability",
        "revision",
        "catalog",
        "unresolvable",
        "unconfigured",
        "normalized"
      })
  void s21_s22_replayConfirmedIntention(String change) throws Exception {
    UUID key = UUID.randomUUID();
    String content = createBody();
    var original = create(key, content);
    assertThat(original.getStatus()).isEqualTo(201);
    if (change.equals("past"))
      when(clock.instant()).thenReturn(Instant.parse("2040-01-01T00:00:00Z"));
    if (change.equals("completed")) {
      jdbc.update("UPDATE projects SET status='completed'");
      jdbc.update("UPDATE tasks SET status='completed',completed_at=updated_at");
    }
    if (change.equals("availability"))
      jdbc.update(
          "UPDATE availability_preferences SET zone_id='Europe/Madrid',monday_minutes=0,version=1");
    if (change.equals("revision")) jdbc.update("UPDATE availability_preferences SET version=1");
    if (change.equals("catalog")) when(catalog.zones()).thenReturn(Set.of());
    if (change.equals("unresolvable"))
      jdbc.update("UPDATE availability_preferences SET zone_id='Retired/Zone'");
    if (change.equals("unconfigured")) jdbc.update("DELETE FROM availability_preferences");
    if (change.equals("normalized")) content = content.replace(" Meta ", "\u00a0Meta\u00a0");
    org.mockito.Mockito.clearInvocations(catalog);
    var replay = create(key, content);
    assertThat(replay.getStatus()).isEqualTo(200);
    assertThat(json.readTree(replay.getContentAsString()))
        .isEqualTo(json.readTree(original.getContentAsString()));
    assertThat(replay.getHeader("Location")).isEqualTo(original.getHeader("Location"));
    org.mockito.Mockito.verifyNoInteractions(catalog);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(1);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "objective,Diferente",
    "startLocal,2030-01-07T09:59",
    "endLocal,2030-01-07T11:01",
    "zoneId,Europe/Madrid",
    "startOffset,+01:00",
    "endOffset,+01:00",
    "allowOverBudget,true"
  })
  void s22_keyBindsExactIntention(String field, String value) throws Exception {
    var key = UUID.randomUUID();
    var original = create(key, createBody());
    assertThat(original.getStatus()).isEqualTo(201);
    var data = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(createBody());
    if (field.equals("allowOverBudget")) data.put(field, true);
    else data.put(field, value);
    var conflict = create(key, data.toString());
    assertThat(conflict.getStatus()).isEqualTo(409);
    var error = json.readTree(conflict.getContentAsString());
    assertThat(error.size()).isEqualTo(4);
    assertThat(error.get("code").asText()).isEqualTo("IDEMPOTENCY_CONFLICT");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(1);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "preview,query,400,VALIDATION_ERROR,query",
    "create,query,400,VALIDATION_ERROR,query",
    "create,id,400,VALIDATION_ERROR,projectId",
    "create,none,428,PRECONDITION_REQUIRED,none",
    "create,headers,400,MALFORMED_JSON,none",
    "preview,none,400,MALFORMED_JSON,none"
  })
  void s19_emptyBodyPreservesPrecedence(
      String operation, String defect, int statusCode, String code, String field) throws Exception {
    var url = base() + (operation.equals("preview") ? "/preview" : "");
    if (defect.equals("query")) url += "?extra=1";
    if (defect.equals("id")) url = url.replace(project.toString(), "1-1-1-1-1");
    var request =
        post(url)
            .with(user("persona-a"))
            .with(csrf().asHeader())
            .contentType("application/json")
            .content("");
    if (defect.equals("headers"))
      request
          .header("Availability-Revision", revision())
          .header("Idempotency-Key", UUID.randomUUID());
    var result =
        mvc.perform(request)
            .andExpect(status().is(statusCode))
            .andExpect(jsonPath("$.code").value(code));
    if (!field.equals("none")) result.andExpect(jsonPath("$.errors[0].field").value(field));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "by-request,own,200,ok",
    "by-request,absent,404,BLOCK_NOT_FOUND",
    "by-request,foreign,404,RESOURCE_NOT_FOUND",
    "by-request,missing,404,RESOURCE_NOT_FOUND",
    "by-request,other-task,404,BLOCK_NOT_FOUND",
    "by-request,completed,200,ok",
    "by-request,zone,200,ok",
    "detail,own,200,ok",
    "detail,absent,404,BLOCK_NOT_FOUND",
    "detail,foreign,404,RESOURCE_NOT_FOUND",
    "detail,missing,404,RESOURCE_NOT_FOUND",
    "detail,other-task,404,BLOCK_NOT_FOUND",
    "detail,completed,200,ok",
    "detail,zone,200,ok"
  })
  void s24_s26_readSavedBlock(String operation, String mode, int statusCode, String code)
      throws Exception {
    var key = UUID.randomUUID();
    var original = create(key, createBody());
    assertThat(original.getStatus()).isEqualTo(201);
    var expected = json.readTree(original.getContentAsString());
    String id = operation.equals("by-request") ? key.toString() : expected.get("id").asText();
    if (mode.equals("absent")) id = UUID.randomUUID().toString();
    if (mode.equals("foreign")) jdbc.update("UPDATE projects SET owner_id='persona-b'");
    if (mode.equals("missing")) project = UUID.randomUUID();
    if (mode.equals("other-task")) {
      task = UUID.randomUUID();
      jdbc.update(
          "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,'Otra','','pending',now(),now())",
          task,
          project);
    }
    if (mode.equals("completed")) {
      jdbc.update("UPDATE projects SET status='completed'");
      jdbc.update("UPDATE tasks SET status='completed',completed_at=updated_at");
    }
    if (mode.equals("zone"))
      jdbc.update("UPDATE availability_preferences SET zone_id='Retired/Zone'");
    org.mockito.Mockito.clearInvocations(catalog);
    var result =
        mvc.perform(
                get(base() + (operation.equals("by-request") ? "/by-request/" : "/") + id)
                    .with(user("persona-a")))
            .andExpect(status().is(statusCode))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse();
    if (statusCode == 200)
      assertThat(json.readTree(result.getContentAsString())).isEqualTo(expected);
    else
      assertThat(json.readTree(result.getContentAsString()).get("code").asText()).isEqualTo(code);
    org.mockito.Mockito.verifyNoInteractions(catalog);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(1);
  }

  @Test
  void s25_s26_listUsesStableCursor() throws Exception {
    var empty =
        mvc.perform(get(base()).with(user("persona-a")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    assertThat(json.readTree(empty.getContentAsString()).get("items").size()).isZero();
    assertThat(json.readTree(empty.getContentAsString()).get("nextCursor").isNull()).isTrue();
    var ids = new ArrayList<String>();
    for (int day = 7; day < 28; day++) {
      var response =
          create(
              UUID.randomUUID(),
              createBody().replace("2030-01-07", LocalDate.of(2030, 1, day).toString()));
      assertThat(response.getStatus()).isEqualTo(201);
      ids.add(json.readTree(response.getContentAsString()).get("id").asText());
    }
    ids.sort(Comparator.reverseOrder());
    var first =
        json.readTree(
            mvc.perform(get(base()).with(user("persona-a")))
                .andExpect(status().isOk())
                .andExpect(
                    header()
                        .string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(first.size()).isEqualTo(2);
    assertThat(first.get("items").size()).isEqualTo(20);
    assertThat(first.get("nextCursor").isTextual()).isTrue();
    var second =
        json.readTree(
            mvc.perform(
                    get(base())
                        .param("cursor", first.get("nextCursor").asText())
                        .with(user("persona-a")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(second.size()).isEqualTo(2);
    assertThat(second.get("items").size()).isEqualTo(1);
    assertThat(second.get("nextCursor").isNull()).isTrue();
    var actual = new ArrayList<String>();
    for (var page : List.of(first, second))
      for (var item : page.get("items")) {
        assertThat(item.size()).isEqualTo(9);
        actual.add(item.get("id").asText());
      }
    assertThat(actual).containsExactlyElementsOf(ids);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class))
        .isEqualTo(21);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(21);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "garbage",
        "padding",
        "duplicate",
        "tasks",
        "subtasks",
        "wrong-collection",
        "other-project",
        "other-task",
        "extra",
        "missing",
        "wrong-type",
        "invalid-date",
        "nanos",
        "year-zero",
        "year-large",
        "invalid-id",
        "duplicate-json",
        "trailing-json",
        "list-query",
        "detail-query",
        "key-query"
      })
  void s19_s27_strictReadQueryAndCursor(String defect) throws Exception {
    var data =
        json.createObjectNode()
            .put("collection", "blocks")
            .put("projectId", project.toString())
            .put("taskId", task.toString())
            .put("createdAt", "2030-01-01T00:00:00.123456Z")
            .put("id", UUID.randomUUID().toString());
    switch (defect) {
      case "tasks" -> {
        data.remove("collection");
        data.remove("taskId");
      }
      case "subtasks" -> {
        data.remove("collection");
        data.put("parentTaskId", task.toString());
        data.remove("taskId");
      }
      case "wrong-collection" -> data.put("collection", "tasks");
      case "other-project" -> data.put("projectId", UUID.randomUUID().toString());
      case "other-task" -> data.put("taskId", UUID.randomUUID().toString());
      case "extra" -> data.put("extra", true);
      case "missing" -> data.remove("id");
      case "wrong-type" -> data.put("createdAt", 7);
      case "invalid-date" -> data.put("createdAt", "yesterday");
      case "nanos" -> data.put("createdAt", "2030-01-01T00:00:00.123456789Z");
      case "year-zero" -> data.put("createdAt", "0000-01-01T00:00:00Z");
      case "year-large" -> data.put("createdAt", "+10000-01-01T00:00:00Z");
      case "invalid-id" -> data.put("id", "1-1-1-1-1");
    }
    String raw = data.toString();
    if (defect.equals("duplicate-json")) raw = raw.replace("{", "{\"collection\":\"blocks\",");
    if (defect.equals("trailing-json")) raw += " {}";
    String cursor =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    if (defect.equals("garbage")) cursor = "%%%";
    if (defect.equals("padding")) cursor += "=";
    var url = base();
    if (defect.equals("detail-query")) url += "/" + UUID.randomUUID();
    if (defect.equals("key-query")) url += "/by-request/" + UUID.randomUUID();
    var request = get(url).with(user("persona-a"));
    if (defect.endsWith("query")) request.param("extra", "1");
    else request.param("cursor", cursor);
    if (defect.equals("duplicate")) request.param("cursor", cursor);
    mvc.perform(request)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(
            jsonPath("$.errors[0].field").value(defect.endsWith("query") ? "query" : "cursor"));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "preview,absent,409,AVAILABILITY_REQUIRED",
    "create,absent,409,AVAILABILITY_REQUIRED",
    "preview,completed,409,PROJECT_COMPLETED",
    "create,task-completed,409,TASK_COMPLETED",
    "create,project-zone,409,PROJECT_COMPLETED",
    "preview,zone,409,AVAILABILITY_ZONE_UNAVAILABLE",
    "create,zone,409,AVAILABILITY_ZONE_UNAVAILABLE",
    "create,revision-completed,412,AVAILABILITY_CONFLICT"
  })
  void s17_s18_businessPrecedence(String operation, String mode, int statusCode, String code)
      throws Exception {
    if (mode.equals("absent")) {
      jdbc.update("DELETE FROM availability_preferences");
      jdbc.update("UPDATE projects SET status='completed'");
    }
    if (mode.equals("completed")) {
      jdbc.update("UPDATE projects SET status='completed'");
      jdbc.update("UPDATE tasks SET status='completed',completed_at=updated_at");
    }
    if (mode.equals("task-completed"))
      jdbc.update("UPDATE tasks SET status='completed',completed_at=updated_at");
    if (mode.equals("zone") || mode.equals("project-zone"))
      jdbc.update("UPDATE availability_preferences SET zone_id='Retired/Zone'");
    if (mode.equals("project-zone") || mode.equals("revision-completed"))
      jdbc.update("UPDATE projects SET status='completed'");
    if (mode.equals("revision-completed"))
      jdbc.update("UPDATE availability_preferences SET version=1");
    var request =
        post(base() + (operation.equals("preview") ? "/preview" : ""))
            .with(user("persona-a"))
            .with(csrf().asHeader())
            .contentType("application/json")
            .content(operation.equals("preview") ? body() : createBody());
    if (operation.equals("create"))
      request
          .header("Availability-Revision", revision())
          .header("Idempotency-Key", UUID.randomUUID());
    var response =
        mvc.perform(request)
            .andExpect(status().is(statusCode))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(content().contentType("application/problem+json"))
            .andReturn()
            .getResponse();
    var error = json.readTree(response.getContentAsString());
    assertThat(error.size()).isEqualTo(4);
    assertThat(error.get("code").asText()).isEqualTo(code);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "2026-10-25T02:15,2026-10-25T02:45,null,null,startOffset,AMBIGUOUS_OFFSET,+02:00;+01:00",
    "2026-10-25T01:30,2026-10-25T02:45,null,null,endOffset,AMBIGUOUS_OFFSET,+02:00;+01:00",
    "2030-01-07T10:00,2030-01-07T11:00,+02:00,+01:00,startOffset,INVALID_OFFSET,+01:00",
    "2030-01-07T10:00,2030-01-07T11:00,+01:00,+02:00,endOffset,INVALID_OFFSET,+01:00"
  })
  void s8_closedOffsetOptions(
      String start,
      String end,
      String startOffset,
      String endOffset,
      String field,
      String code,
      String options)
      throws Exception {
    when(clock.instant()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
    var data = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(body());
    data.put("zoneId", "Europe/Madrid").put("startLocal", start).put("endLocal", end);
    if (!startOffset.equals("null")) data.put("startOffset", startOffset);
    if (!endOffset.equals("null")) data.put("endOffset", endOffset);
    var response =
        mvc.perform(
                post(base() + "/preview")
                    .with(user("persona-a"))
                    .with(csrf().asHeader())
                    .contentType("application/json")
                    .content(data.toString()))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType("application/problem+json"))
            .andReturn()
            .getResponse();
    var error = json.readTree(response.getContentAsString());
    assertThat(error.size()).isEqualTo(6);
    assertThat(error.get("code").asText()).isEqualTo("VALIDATION_ERROR");
    assertThat(error.get("errors").size()).isEqualTo(1);
    var entry = error.get("errors").get(0);
    assertThat(entry.size()).isEqualTo(3);
    assertThat(entry.get("field").asText()).isEqualTo(field);
    assertThat(entry.get("code").asText()).isEqualTo(code);
    assertThat(entry.get("message").asText()).isNotBlank();
    assertThat(error.get("validOffsets").size()).isEqualTo(1);
    var actual = new ArrayList<String>();
    error.get("validOffsets").get(field).forEach(value -> actual.add(value.asText()));
    assertThat(actual).containsExactly(options.split(";"));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(ints = {0, 30})
  void s15_budgetErrorHasCurrentDays(int budget) throws Exception {
    jdbc.update("UPDATE availability_preferences SET monday_minutes=?", budget);
    var response = create(UUID.randomUUID(), createBody());
    assertThat(response.getStatus()).isEqualTo(409);
    var error = json.readTree(response.getContentAsString());
    assertThat(error.size()).isEqualTo(6);
    assertThat(error.get("code").asText()).isEqualTo("BUDGET_EXCEEDED");
    assertThat(error.get("budgetZoneId").asText()).isEqualTo("UTC");
    assertThat(error.get("days").size()).isEqualTo(1);
    var day = error.get("days").get(0);
    assertThat(day.size()).isEqualTo(5);
    assertThat(day.get("date").asText()).isEqualTo("2030-01-07");
    assertThat(day.get("budgetMinutes").asInt()).isEqualTo(budget);
    assertThat(day.get("plannedSeconds").asLong()).isZero();
    assertThat(day.get("requestedSeconds").asLong()).isEqualTo(3600);
    assertThat(day.get("excessSeconds").asLong()).isEqualTo(3600 - budget * 60);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"preview", "create"})
  void s13_s14_s16_overlapHasOnlyOwnIdentity(String operation) throws Exception {
    var original = create(UUID.randomUUID(), createBody());
    assertThat(original.getStatus()).isEqualTo(201);
    var existing = json.readTree(original.getContentAsString());
    var response =
        operation.equals("create")
            ? create(UUID.randomUUID(), createBody().replace("false", "true"))
            : mvc.perform(
                    post(base() + "/preview")
                        .with(user("persona-a"))
                        .with(csrf().asHeader())
                        .contentType("application/json")
                        .content(body()))
                .andReturn()
                .getResponse();
    assertThat(response.getStatus()).isEqualTo(409);
    var error = json.readTree(response.getContentAsString());
    assertThat(error.size()).isEqualTo(5);
    assertThat(error.get("code").asText()).isEqualTo("BLOCK_OVERLAP");
    var conflict = error.get("conflict");
    assertThat(conflict.size()).isEqualTo(3);
    assertThat(conflict.get("id")).isEqualTo(existing.get("id"));
    assertThat(conflict.get("projectId").asText()).isEqualTo(project.toString());
    assertThat(conflict.get("taskId").asText()).isEqualTo(task.toString());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(1);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"null", "[]", "7", "true", "\"text\""})
  void s4_creationRootMustBeObject(String body) throws Exception {
    var response = create(UUID.randomUUID(), body);
    assertThat(response.getStatus()).isEqualTo(400);
    var error = json.readTree(response.getContentAsString());
    assertThat(error.get("code").asText()).isEqualTo("VALIDATION_ERROR");
    assertThat(error.get("errors").get(0).get("field").asText()).isEqualTo("body");
    assertThat(error.get("errors").get(0).get("code").asText()).isEqualTo("INVALID_TYPE");
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "list,anonymous,401,UNAUTHENTICATED",
    "detail,anonymous,401,UNAUTHENTICATED",
    "key,anonymous,401,UNAUTHENTICATED",
    "preview,anonymous,401,UNAUTHENTICATED",
    "create,anonymous,401,UNAUTHENTICATED",
    "preview,origin,403,UNTRUSTED_ORIGIN",
    "create,csrf,403,CSRF_INVALID"
  })
  void s19_allRoutesKeepSecurity(String operation, String defect, int statusCode, String code)
      throws Exception {
    var url =
        base()
            + switch (operation) {
              case "detail" -> "/" + UUID.randomUUID();
              case "key" -> "/by-request/" + UUID.randomUUID();
              case "preview" -> "/preview";
              default -> "";
            };
    var request =
        (operation.equals("preview") || operation.equals("create"))
            ? post(url)
                .contentType("application/json")
                .content(operation.equals("preview") ? body() : createBody())
            : get(url);
    if (!defect.equals("anonymous")) request.with(user("persona-a"));
    if (defect.equals("origin"))
      request.with(csrf().asHeader()).header("Origin", "https://evil.example");
    mvc.perform(request)
        .andExpect(status().is(statusCode))
        .andExpect(jsonPath("$.code").value(code))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "revision,428,PRECONDITION_REQUIRED",
    "key,400,VALIDATION_ERROR",
    "body,400,VALIDATION_ERROR",
    "owner,404,RESOURCE_NOT_FOUND",
    "session,401,UNAUTHENTICATED",
    "csrf,403,CSRF_INVALID"
  })
  void s23_replayStillRequiresSecurityAndStructure(String defect, int statusCode, String code)
      throws Exception {
    var key = UUID.randomUUID();
    assertThat(create(key, createBody()).getStatus()).isEqualTo(201);
    if (defect.equals("owner")) jdbc.update("UPDATE projects SET owner_id='persona-b'");
    var request =
        post(base())
            .contentType("application/json")
            .content(defect.equals("body") ? "{}" : createBody());
    if (!defect.equals("revision")) request.header("Availability-Revision", revision());
    request.header("Idempotency-Key", defect.equals("key") ? "invalid" : key.toString());
    if (!defect.equals("session")) request.with(user("persona-a"));
    if (!defect.equals("csrf")) request.with(csrf().asHeader());
    mvc.perform(request)
        .andExpect(status().is(statusCode))
        .andExpect(jsonPath("$.code").value(code));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(1);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"{", "{\"objective\":\"a\",\"objective\":\"b\"}", "{} {}", ""})
  void s62_creationUnreadableJson(String value) throws Exception {
    var response = create(UUID.randomUUID(), value);
    assertThat(response.getStatus()).isEqualTo(400);
    var error = json.readTree(response.getContentAsString());
    assertThat(error.size()).isEqualTo(4);
    assertThat(error.get("code").asText()).isEqualTo("MALFORMED_JSON");
    assertThat(response.getHeader("Cache-Control")).contains("no-store");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @Test
  void s22_sameKeyIsScopedToTask() throws Exception {
    var key = UUID.randomUUID();
    var first = create(key, createBody());
    assertThat(first.getStatus()).isEqualTo(201);
    var firstBody = json.readTree(first.getContentAsString());
    task = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,'Otra','','pending',now(),now())",
        task,
        project);
    var second = create(key, createBody().replace("2030-01-07", "2030-01-08"));
    assertThat(second.getStatus()).isEqualTo(201);
    var secondBody = json.readTree(second.getContentAsString());
    assertThat(secondBody.get("taskId").asText())
        .isEqualTo(task.toString())
        .isNotEqualTo(firstBody.get("taskId").asText());
    var restored =
        mvc.perform(get(base() + "/by-request/" + key).with(user("persona-a")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    assertThat(json.readTree(restored.getContentAsString())).isEqualTo(secondBody);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class)).isEqualTo(2);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(2);
  }

  @Test
  void s25_createdAtPrecedesUuidOrder() throws Exception {
    var newer = UUID.fromString("00000000-0000-0000-0000-000000000001");
    var older = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
    for (int i = 0; i < 2; i++) {
      var local = LocalDateTime.of(2030, 1, 7 + i, 10, 0);
      jdbc.update(
          "INSERT INTO planned_blocks(id,project_id,task_id,request_key,objective,start_local,end_local,zone_id,start_offset,end_offset,allow_over_budget,start_at,end_at,duration_minutes,created_at) VALUES (?,?,?,?,'Meta',?,?,'UTC','Z','Z',false,?,?,60,?)",
          i == 0 ? older : newer,
          project,
          task,
          UUID.randomUUID(),
          local,
          local.plusHours(1),
          java.sql.Timestamp.from(local.toInstant(ZoneOffset.UTC)),
          java.sql.Timestamp.from(local.plusHours(1).toInstant(ZoneOffset.UTC)),
          java.sql.Timestamp.from(
              Instant.parse(i == 0 ? "2030-01-01T00:00:00Z" : "2030-01-02T00:00:00Z")));
    }
    var data =
        json.readTree(
            mvc.perform(get(base()).with(user("persona-a")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(data.get("items").get(0).get("id").asText()).isEqualTo(newer.toString());
    assertThat(data.get("items").get(1).get("id").asText()).isEqualTo(older.toString());
    assertThat(data.get("nextCursor").isNull()).isTrue();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"list", "detail", "key", "preview"})
  void s26_readStorageFailureIsSafe503(String operation) throws Exception {
    jdbc.execute("ALTER TABLE planned_blocks RENAME TO unavailable_blocks");
    try {
      var url =
          base()
              + switch (operation) {
                case "detail" -> "/" + UUID.randomUUID();
                case "key" -> "/by-request/" + UUID.randomUUID();
                case "preview" -> "/preview";
                default -> "";
              };
      var request =
          operation.equals("preview")
              ? post(url).with(csrf().asHeader()).contentType("application/json").content(body())
              : get(url);
      var response =
          mvc.perform(request.with(user("persona-a")))
              .andExpect(status().isServiceUnavailable())
              .andExpect(content().contentType("application/problem+json"))
              .andExpect(
                  header()
                      .string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
              .andReturn()
              .getResponse();
      var error = json.readTree(response.getContentAsString());
      assertThat(error.size()).isEqualTo(4);
      assertThat(error.get("code").asText()).isEqualTo("STORAGE_UNAVAILABLE");
      assertThat(response.getContentAsString())
          .doesNotContain("SELECT", "unavailable_blocks", "PSQLException", "stackTrace");
      assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
    } finally {
      jdbc.execute("ALTER TABLE unavailable_blocks RENAME TO planned_blocks");
    }
  }
}
