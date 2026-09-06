package com.apptolast.organization.adapter;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest(
    properties = {
      "app.auth.username=persona-a",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@AutoConfigureMockMvc
class TaskStatusApiTest {
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  static {
    // PIT repeats JUnit class lifecycles inside one JVM. Keep the database endpoint stable
    // for Spring's cached context; Ryuk removes this JVM-owned container on exit.
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
  UUID project;
  UUID parent;

  @BeforeEach
  void setup() {
    jdbc.execute("TRUNCATE planned_blocks, task_status_history, tasks, outbox_events, projects");
    project = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'persona-a','P','','idea',now(),now())",
        project);
    parent = UUID.fromString("abcdef01-2345-6789-abcd-0123456789ab");
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?, ?, 'Parent', '', 'pending', now(), now())",
        parent,
        project);
  }

  String path() {
    return "/api/v1/projects/" + project + "/tasks/" + parent + "/status";
  }

  @Test
  void s1_readsLegacyPendingSnapshotAndTaskEtag() throws Exception {
    var response =
        mvc.perform(get(path()).with(user("persona-a")))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(status().isOk())
            .andExpect(header().string("ETag", "\"task:" + parent + ":0\""))
            .andReturn()
            .getResponse();
    var body = json.readTree(response.getContentAsString());
    assertThat(body.size()).isEqualTo(3);
    assertThat(body.get("status").asText()).isEqualTo("pending");
    assertThat(body.get("completedAt").isNull()).isTrue();
    assertThat(java.time.Instant.parse(body.get("updatedAt").asText()))
        .isEqualTo(
            jdbc.queryForObject(
                    "SELECT updated_at FROM tasks WHERE id=?", java.sql.Timestamp.class, parent)
                .toInstant());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM task_status_history", Long.class))
        .isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @Test
  void s2_confirmsTaskHistoryAndEventWithOneSnapshot() throws Exception {
    var original = jdbc.queryForMap("SELECT * FROM projects WHERE id=?", project);
    var previous = jdbc.queryForMap("SELECT * FROM tasks WHERE id=?", parent);
    var response =
        mvc.perform(
                put(path())
                    .with(user("persona-a"))
                    .with(csrf().asHeader())
                    .header("If-Match", "\"task:" + parent + ":0\"")
                    .contentType("application/json")
                    .content(json.createObjectNode().put("status", "completed").toString()))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(status().isOk())
            .andExpect(header().string("ETag", "\"task:" + parent + ":1\""))
            .andReturn()
            .getResponse();
    var body = json.readTree(response.getContentAsString());
    assertThat(body.size()).isEqualTo(3);
    assertThat(body.get("status").asText()).isEqualTo("completed");
    assertThat(body.get("completedAt")).isEqualTo(body.get("updatedAt"));
    var row = jdbc.queryForMap("SELECT * FROM tasks WHERE id=?", parent);
    assertThat(row.get("version")).isEqualTo(1L);
    assertThat(row.get("completed_at")).isEqualTo(row.get("updated_at"));
    assertThat(((java.sql.Timestamp) row.get("updated_at")).toInstant())
        .isEqualTo(java.time.Instant.parse(body.get("updatedAt").asText()));
    for (String key :
        java.util.List.of(
            "title",
            "completion_criterion",
            "estimated_minutes",
            "parent_id",
            "created_at",
            "project_id")) assertThat(row.get(key)).isEqualTo(previous.get(key));
    assertThat(jdbc.queryForMap("SELECT * FROM projects WHERE id=?", project)).isEqualTo(original);
    var history = jdbc.queryForMap("SELECT * FROM task_status_history");
    assertThat(history.get("task_id")).isEqualTo(parent);
    assertThat(history.get("task_version")).isEqualTo(1L);
    assertThat(history.get("from_status")).isEqualTo("pending");
    assertThat(history.get("to_status")).isEqualTo("completed");
    assertThat(history.get("occurred_at")).isEqualTo(row.get("updated_at"));
    var event =
        json.readTree(jdbc.queryForObject("SELECT payload::text FROM outbox_events", String.class));
    assertThat(event.size()).isEqualTo(9);
    assertThat(event.get("eventId").asText()).isEqualTo(history.get("id").toString());
    assertThat(event.get("aggregateId").asText()).isEqualTo(project.toString());
    assertThat(event.get("taskId").asText()).isEqualTo(parent.toString());
    assertThat(event.get("ownerId").asText()).isEqualTo("persona-a");
    assertThat(event.get("type").asText()).isEqualTo("TaskStatusChanged.v1");
    assertThat(event.get("schemaVersion").intValue()).isEqualTo(1);
    assertThat(event.get("occurredAt")).isEqualTo(body.get("updatedAt"));
    assertThat(event.get("fromStatus").asText()).isEqualTo("pending");
    assertThat(event.get("toStatus").asText()).isEqualTo("completed");
    var read =
        json.readTree(
            mvc.perform(get(path()).with(user("persona-a")))
                .andExpect(header().string("ETag", response.getHeader("ETag")))
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(read).isEqualTo(body);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "GET,projectId",
    "GET,id",
    "PUT,projectId",
    "PUT,id"
  })
  void s30_validatesPathBeforePreconditionAndBody(String method, String field) throws Exception {
    String target =
        "/api/v1/projects/"
            + (field.equals("projectId") ? "1-1-1-1-1" : project)
            + "/tasks/"
            + (field.equals("id") ? "1-1-1-1-1" : parent)
            + "/status";
    var request =
        method.equals("PUT")
            ? put(target).contentType("application/json").content("{")
            : get(target);
    mvc.perform(request.with(user("persona-a")).with(csrf().asHeader()))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value(field))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_FORMAT"));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "missing",
        "malformed",
        "weak",
        "star",
        "list",
        "repeated",
        "project",
        "other",
        "uppercase",
        "zeros",
        "sign",
        "overflow"
      })
  void s6_rejectsAmbiguousPreconditionsBeforeJson(String defect) throws Exception {
    String tag = "\"task:" + parent + ":0\"";
    switch (defect) {
      case "malformed" -> tag = "invalid";
      case "weak" -> tag = "W/" + tag;
      case "star" -> tag = "*";
      case "list" -> tag = tag + "," + tag;
      case "project" -> tag = "\"" + project + ":0\"";
      case "other" -> tag = "\"task:" + UUID.randomUUID() + ":0\"";
      case "uppercase" ->
          tag = "\"task:" + parent.toString().toUpperCase(java.util.Locale.ROOT) + ":0\"";
      case "zeros" -> tag = "\"task:" + parent + ":00\"";
      case "sign" -> tag = "\"task:" + parent + ":+0\"";
      case "overflow" -> tag = "\"task:" + parent + ":9223372036854775808\"";
      default -> {}
    }
    var request =
        put(path())
            .with(user("persona-a"))
            .with(csrf().asHeader())
            .contentType("application/json")
            .content(json.createObjectNode().put("status", "completed").toString());
    if (!defect.equals("missing")) request.header("If-Match", tag);
    if (defect.equals("repeated")) request.header("If-Match", tag);
    var result =
        mvc.perform(request)
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(status().is(defect.equals("missing") ? 428 : 400))
            .andExpect(
                jsonPath("$.code")
                    .value(
                        defect.equals("missing") ? "PRECONDITION_REQUIRED" : "VALIDATION_ERROR"));
    if (!defect.equals("missing"))
      result
          .andExpect(jsonPath("$.errors[0].field").value("If-Match"))
          .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    assertThat(jdbc.queryForObject("SELECT version FROM tasks WHERE id=?", Long.class, parent))
        .isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM task_status_history", Long.class))
        .isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.MethodSource("invalidBodies")
  void s7_rejectsInvalidBody(String body, String code, String field, String error)
      throws Exception {
    var result =
        mvc.perform(
                put(path())
                    .with(user("persona-a"))
                    .with(csrf().asHeader())
                    .header("If-Match", "\"task:" + parent + ":0\"")
                    .contentType("application/json")
                    .content(body))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(code));
    if (field != null)
      result
          .andExpect(jsonPath("$.errors[0].field").value(field))
          .andExpect(jsonPath("$.errors[0].code").value(error));
    assertThat(jdbc.queryForObject("SELECT version FROM tasks WHERE id=?", Long.class, parent))
        .isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM task_status_history", Long.class))
        .isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> invalidBodies() {
    var rows = new java.util.ArrayList<org.junit.jupiter.params.provider.Arguments>();
    for (String raw :
        java.util.List.of(
            "{",
            "",
            "{\"status\":\"completed\",\"status\":\"pending\"}",
            "{\"status\":\"completed\"} {}"))
      rows.add(org.junit.jupiter.params.provider.Arguments.of(raw, "MALFORMED_JSON", null, null));
    for (String raw : java.util.List.of("[]", "null", "\"pending\"", "42"))
      rows.add(
          org.junit.jupiter.params.provider.Arguments.of(
              raw, "VALIDATION_ERROR", "body", "INVALID_TYPE"));
    for (String raw : java.util.List.of("{}", "{\"status\":null}"))
      rows.add(
          org.junit.jupiter.params.provider.Arguments.of(
              raw, "VALIDATION_ERROR", "status", "REQUIRED"));
    for (String raw : java.util.List.of("42", "[]", "{}", "true"))
      rows.add(
          org.junit.jupiter.params.provider.Arguments.of(
              "{\"status\":" + raw + "}", "VALIDATION_ERROR", "status", "INVALID_TYPE"));
    for (String value : java.util.List.of("active", "", " completed ", "COMPLETED"))
      rows.add(
          org.junit.jupiter.params.provider.Arguments.of(
              "{\"status\":\"" + value + "\"}", "VALIDATION_ERROR", "status", "INVALID_VALUE"));
    rows.add(
        org.junit.jupiter.params.provider.Arguments.of(
            "{\"status\":\"completed\",\"ownerId\":\"x\"}",
            "VALIDATION_ERROR",
            "ownerId",
            "UNKNOWN_FIELD"));
    rows.add(
        org.junit.jupiter.params.provider.Arguments.of(
            "{\"z\":true,\"a\":true,\"status\":null}", "VALIDATION_ERROR", "a", "UNKNOWN_FIELD"));
    return rows.stream();
  }

  @Test
  void s5_rejectsOldRevisionEvenWhenTargetAlreadySatisfied() throws Exception {
    mvc.perform(
            put(path())
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .header("If-Match", "\"task:" + parent + ":0\"")
                .contentType("application/json")
                .content("{\"status\":\"completed\"}"))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(status().isOk());
    var before = jdbc.queryForMap("SELECT * FROM tasks WHERE id=?", parent);
    mvc.perform(
            put(path())
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .header("If-Match", "\"task:" + parent + ":0\"")
                .contentType("application/json")
                .content("{\"status\":\"completed\"}"))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(status().isPreconditionFailed())
        .andExpect(jsonPath("$.code").value("TASK_CONFLICT"));
    assertThat(jdbc.queryForMap("SELECT * FROM tasks WHERE id=?", parent)).isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM task_status_history", Long.class))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(1);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"pending", "completed"})
  void s3_s4_reopenAndNoOpPreserveConfirmedSnapshot(String target) throws Exception {
    changeStatus("completed", 0)
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(status().isOk());
    if (target.equals("pending"))
      changeStatus("pending", 1)
          .andExpect(
              header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.completedAt").isEmpty());
    long version = target.equals("completed") ? 1 : 2;
    var before = jdbc.queryForMap("SELECT * FROM tasks WHERE id=?", parent);
    var history = jdbc.queryForList("SELECT * FROM task_status_history ORDER BY task_version");
    var events = jdbc.queryForList("SELECT * FROM outbox_events ORDER BY event_id");
    var previous = mvc.perform(get(path()).with(user("persona-a"))).andReturn().getResponse();
    var noOp =
        changeStatus(target, version)
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    assertThat(noOp.getContentAsString()).isEqualTo(previous.getContentAsString());
    assertThat(noOp.getHeader("ETag")).isEqualTo(previous.getHeader("ETag"));
    assertThat(jdbc.queryForMap("SELECT * FROM tasks WHERE id=?", parent)).isEqualTo(before);
    assertThat(jdbc.queryForList("SELECT * FROM task_status_history ORDER BY task_version"))
        .isEqualTo(history);
    assertThat(jdbc.queryForList("SELECT * FROM outbox_events ORDER BY event_id"))
        .isEqualTo(events);
  }

  org.springframework.test.web.servlet.ResultActions changeStatus(String target, long version)
      throws Exception {
    return mvc.perform(
        put(path())
            .with(user("persona-a"))
            .with(csrf().asHeader())
            .header("If-Match", "\"task:" + parent + ":" + version + "\"")
            .contentType("application/json")
            .content(json.createObjectNode().put("status", target).toString()));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "tasks,error",
    "tasks,skip",
    "task_status_history,error",
    "task_status_history,skip",
    "outbox_events,error",
    "outbox_events,skip"
  })
  void s17_rollsBackEveryWriteOnFailureOrSuppression(String table, String mode) throws Exception {
    var before = jdbc.queryForMap("SELECT * FROM tasks WHERE id=?", parent);
    jdbc.execute(
        "CREATE FUNCTION reject_status_write() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN "
            + (mode.equals("skip") ? "RETURN NULL;" : "RAISE EXCEPTION 'synthetic failure';")
            + " END $$");
    jdbc.execute(
        "CREATE TRIGGER reject_status_write BEFORE "
            + (table.equals("tasks") ? "UPDATE" : "INSERT")
            + " ON "
            + table
            + " FOR EACH ROW EXECUTE FUNCTION reject_status_write()");
    try {
      changeStatus("completed", 0)
          .andExpect(
              header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
          .andExpect(status().isServiceUnavailable())
          .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"));
      assertThat(jdbc.queryForMap("SELECT * FROM tasks WHERE id=?", parent)).isEqualTo(before);
      assertThat(jdbc.queryForObject("SELECT count(*) FROM task_status_history", Long.class))
          .isZero();
      assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
    } finally {
      jdbc.execute("DROP TRIGGER reject_status_write ON " + table);
      jdbc.execute("DROP FUNCTION reject_status_write()");
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "idea,completed",
    "active,completed",
    "paused,completed",
    "completed,completed",
    "idea,pending",
    "active,pending",
    "paused,pending",
    "completed,pending"
  })
  void s8_s9_changesOnlyTaskAcrossEveryProjectState(String projectStatus, String target)
      throws Exception {
    jdbc.update("UPDATE projects SET status=? WHERE id=?", projectStatus, project);
    var child = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO tasks(id,project_id,parent_id,title,completion_criterion,status,created_at,updated_at) VALUES (?, ?, ?, 'Child', '', 'pending',now(),now())",
        child,
        project,
        parent);
    var originalProject = jdbc.queryForMap("SELECT * FROM projects WHERE id=?", project);
    var originalChild = jdbc.queryForMap("SELECT * FROM tasks WHERE id=?", child);
    if (target.equals("pending"))
      changeStatus("completed", 0)
          .andExpect(
              header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
          .andExpect(status().isOk());
    changeStatus(target, target.equals("pending") ? 1 : 0)
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(target));
    assertThat(jdbc.queryForMap("SELECT * FROM projects WHERE id=?", project))
        .isEqualTo(originalProject);
    assertThat(jdbc.queryForMap("SELECT * FROM tasks WHERE id=?", child)).isEqualTo(originalChild);
    String base = "/api/v1/projects/" + project + "/tasks";
    for (String url :
        java.util.List.of(base, base + "/" + parent, base + "/" + child + "/parent")) {
      var body =
          json.readTree(
              mvc.perform(get(url).with(user("persona-a")))
                  .andExpect(
                      header()
                          .string(
                              "Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                  .andExpect(status().isOk())
                  .andReturn()
                  .getResponse()
                  .getContentAsString());
      var task =
          url.equals(base)
              ? body.get("items").get(1)
              : url.endsWith("/parent") ? body.get("parent") : body;
      assertThat(task.size()).isEqualTo(8);
      assertThat(task.get("status").asText()).isEqualTo(target);
    }
    var ancestor = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?, ?, 'Ancestor', '', 'pending',now(),now())",
        ancestor,
        project);
    jdbc.update("UPDATE tasks SET parent_id=? WHERE id=?", ancestor, parent);
    var children =
        json.readTree(
                mvc.perform(get(base + "/" + ancestor + "/subtasks").with(user("persona-a")))
                    .andExpect(
                        header()
                            .string(
                                "Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("items");
    assertThat(children.get(0).size()).isEqualTo(8);
    assertThat(children.get(0).get("status").asText()).isEqualTo(target);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "GET,missing",
    "GET,foreign",
    "GET,wrongProject",
    "PUT,missing",
    "PUT,foreign",
    "PUT,wrongProject"
  })
  void s14_hidesResourcesBeforeVersion(String method, String defect) throws Exception {
    var absent = UUID.randomUUID();
    String root = "/api/v1/projects/" + project + "/tasks/";
    var reference =
        mvc.perform(get(root + absent + "/status").with(user("persona-a")))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(status().isNotFound())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID requested = parent;
    if (defect.equals("missing")) requested = absent;
    if (defect.equals("foreign"))
      jdbc.update("UPDATE projects SET owner_id='other' WHERE id=?", project);
    if (defect.equals("wrongProject")) {
      var other = UUID.randomUUID();
      jdbc.update(
          "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'persona-a','Other','','idea',now(),now())",
          other);
      root = "/api/v1/projects/" + other + "/tasks/";
    }
    var request =
        method.equals("PUT")
            ? put(root + requested + "/status")
                .with(csrf().asHeader())
                .header("If-Match", "\"task:" + requested + ":99\"")
                .contentType("application/json")
                .content("{\"status\":\"completed\"}")
            : get(root + requested + "/status");
    mvc.perform(request.with(user("persona-a")))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(status().isNotFound())
        .andExpect(content().json(reference))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM task_status_history", Long.class))
        .isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @Autowired org.springframework.session.SessionRepository sessions;

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({"GET,false", "GET,true", "PUT,false", "PUT,true"})
  void s14_requiresSessionAndRejectsPersistedExpiration(String method, boolean expired)
      throws Exception {
    var request =
        method.equals("PUT")
            ? put(path())
                .header("If-Match", "\"task:" + parent + ":0\"")
                .contentType("application/json")
                .content("{\"status\":\"completed\"}")
            : get(path());
    if (expired) {
      var session = (org.springframework.session.Session) sessions.createSession();
      session.setAttribute(
          "SPRING_SECURITY_CONTEXT",
          new org.springframework.security.core.context.SecurityContextImpl(
              org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                  .authenticated(
                      "persona-a",
                      "",
                      java.util.List.of(
                          new org.springframework.security.core.authority.SimpleGrantedAuthority(
                              "ROLE_USER")))));
      sessions.save(session);
      jdbc.update(
          "UPDATE spring_session SET last_access_time=0,expiry_time=0 WHERE session_id=?",
          session.getId());
      request.cookie(
          new jakarta.servlet.http.Cookie(
              "SESSION",
              java.util.Base64.getEncoder()
                  .encodeToString(
                      session.getId().getBytes(java.nio.charset.StandardCharsets.UTF_8))));
    }
    mvc.perform(request)
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
        .andExpect(header().doesNotExist("WWW-Authenticate"))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM task_status_history", Long.class))
        .isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"missing", "invalid", "foreignOrigin"})
  void s15_rejectsCsrfAndForeignOrigin(String defect) throws Exception {
    var request =
        put(path())
            .with(user("persona-a"))
            .header("If-Match", "\"task:" + parent + ":0\"")
            .contentType("application/json")
            .content("{\"status\":\"completed\"}");
    if (defect.equals("invalid")) request.with(csrf().useInvalidToken().asHeader());
    if (defect.equals("foreignOrigin"))
      request.with(csrf().asHeader()).header("Origin", "https://other.example");
    mvc.perform(request)
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.code")
                .value(defect.equals("foreignOrigin") ? "UNTRUSTED_ORIGIN" : "CSRF_INVALID"))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    assertThat(jdbc.queryForObject("SELECT version FROM tasks WHERE id=?", Long.class, parent))
        .isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM task_status_history", Long.class))
        .isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"GET", "PUT"})
  void s18_reportsStorageFailureSafely(String method) throws Exception {
    jdbc.execute("ALTER TABLE tasks RENAME TO tasks_temporarily_unavailable");
    try {
      var request =
          method.equals("PUT")
              ? put(path())
                  .with(csrf().asHeader())
                  .header("If-Match", "\"task:" + parent + ":0\"")
                  .contentType("application/json")
                  .content("{\"status\":\"completed\"}")
              : get(path());
      mvc.perform(request.with(user("persona-a")))
          .andExpect(
              header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
          .andExpect(status().isServiceUnavailable())
          .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"))
          .andExpect(
              header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    } finally {
      jdbc.execute("ALTER TABLE tasks_temporarily_unavailable RENAME TO tasks");
    }
    assertThat(jdbc.queryForObject("SELECT count(*) FROM task_status_history", Long.class))
        .isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @Test
  void s16_serializesTwoHttpWritersUsingSameRevision() throws Exception {
    try (var connection = jdbc.getDataSource().getConnection();
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      connection.setAutoCommit(false);
      try (var lock =
          connection.prepareStatement("SELECT id FROM tasks WHERE id=? FOR NO KEY UPDATE")) {
        lock.setObject(1, parent);
        lock.executeQuery();
      }
      var started = new java.util.concurrent.CountDownLatch(2);
      java.util.concurrent.Callable<Integer> writer =
          () -> {
            started.countDown();
            return changeStatus("completed", 0).andReturn().getResponse().getStatus();
          };
      var first = executor.submit(writer);
      var second = executor.submit(writer);
      try {
        assertThat(started.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> first.get(150, java.util.concurrent.TimeUnit.MILLISECONDS))
            .isInstanceOf(java.util.concurrent.TimeoutException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> second.get(150, java.util.concurrent.TimeUnit.MILLISECONDS))
            .isInstanceOf(java.util.concurrent.TimeoutException.class);
      } finally {
        connection.commit();
      }
      assertThat(
              java.util.List.of(
                  first.get(5, java.util.concurrent.TimeUnit.SECONDS),
                  second.get(5, java.util.concurrent.TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(200, 412);
    }
    assertThat(jdbc.queryForObject("SELECT version FROM tasks WHERE id=?", Long.class, parent))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM task_status_history", Long.class))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(1);
  }

  @Autowired com.apptolast.organization.application.SubtaskCommit subtaskCommit;
  @Autowired com.apptolast.organization.application.TaskStatusEditing statusStore;

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
  void s28_childCreationAndParentTransitionCommitInBothLockOrders(boolean creationFirst)
      throws Exception {
    var firstLocked = new java.util.concurrent.CountDownLatch(1);
    var secondLocked = new java.util.concurrent.CountDownLatch(1);
    var release = new java.util.concurrent.CountDownLatch(1);
    com.apptolast.organization.application.SubtaskCommit creationPort =
        (owner, p, id, operation) ->
            subtaskCommit.save(
                owner,
                p,
                id,
                state -> {
                  if (creationFirst) {
                    firstLocked.countDown();
                    await(release);
                  } else secondLocked.countDown();
                  return operation.apply(state);
                });
    com.apptolast.organization.application.TaskStatusEditing transitionPort =
        (owner, p, id, operation) ->
            statusStore.update(
                owner,
                p,
                id,
                snapshot -> {
                  if (!creationFirst) {
                    firstLocked.countDown();
                    await(release);
                  } else secondLocked.countDown();
                  return operation.apply(snapshot);
                });
    var create =
        new com.apptolast.organization.application.CreateSubtask(
            creationPort, java.time.Clock.systemUTC());
    var change =
        new com.apptolast.organization.application.ChangeTaskStatus(
            transitionPort, java.time.Clock.systemUTC());
    java.util.concurrent.Callable<Object> creation =
        () -> create.execute("persona-a", project, parent, "Child", "", null);
    java.util.concurrent.Callable<Object> transition =
        () ->
            change.execute(
                "persona-a",
                project,
                parent,
                new com.apptolast.organization.domain.TaskRevision(parent, 0),
                "completed");
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var first = executor.submit(creationFirst ? creation : transition);
      try {
        assertThat(firstLocked.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        var second = executor.submit(creationFirst ? transition : creation);
        assertThat(secondLocked.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        release.countDown();
        first.get(5, java.util.concurrent.TimeUnit.SECONDS);
        second.get(5, java.util.concurrent.TimeUnit.SECONDS);
      } finally {
        release.countDown();
      }
    }
    assertThat(jdbc.queryForObject("SELECT status FROM tasks WHERE id=?", String.class, parent))
        .isEqualTo("completed");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM tasks WHERE parent_id=? AND status='pending'",
                Long.class,
                parent))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM task_status_history", Long.class))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(2);
    assertThat(jdbc.queryForObject("SELECT version FROM projects WHERE id=?", Long.class, project))
        .isZero();
  }

  private static void await(java.util.concurrent.CountDownLatch latch) {
    try {
      if (!latch.await(5, java.util.concurrent.TimeUnit.SECONDS))
        throw new AssertionError("Owned transaction release timed out");
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      throw new AssertionError(error);
    }
  }

  @Test
  void s27_createsPendingChildUnderCompletedTaskInOpenProject() throws Exception {
    changeStatus("completed", 0)
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(status().isOk());
    var previous = jdbc.queryForMap("SELECT * FROM tasks WHERE id=?", parent);
    mvc.perform(
            post("/api/v1/projects/" + project + "/tasks/" + parent + "/subtasks")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"title\":\"Child\"}"))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("pending"));
    assertThat(jdbc.queryForMap("SELECT * FROM tasks WHERE id=?", parent)).isEqualTo(previous);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM task_status_history", Long.class))
        .isEqualTo(1);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "completed,true",
    "completed,false",
    "pending,true",
    "pending,false"
  })
  void s33_persistsMonotonicDatesAndVersionForEqualOrBackwardClock(String target, boolean backwards)
      throws Exception {
    var prior = java.time.Instant.parse("2030-01-01T00:00:00.123456Z");
    jdbc.update(
        "UPDATE tasks SET status=?,created_at=?,updated_at=?,completed_at=? WHERE id=?",
        target.equals("completed") ? "pending" : "completed",
        java.sql.Timestamp.from(prior),
        java.sql.Timestamp.from(prior),
        target.equals("pending") ? java.sql.Timestamp.from(prior) : null,
        parent);
    var clock =
        java.time.Clock.fixed(
            backwards ? prior.minusSeconds(10) : prior.plusNanos(123), java.time.ZoneOffset.UTC);
    var changed =
        new com.apptolast.organization.application.ChangeTaskStatus(statusStore, clock)
            .execute(
                "persona-a",
                project,
                parent,
                new com.apptolast.organization.domain.TaskRevision(parent, 0),
                target);
    assertThat(changed.task().updatedAt()).isEqualTo(prior);
    assertThat(changed.version()).isEqualTo(1);
    assertThat(changed.completedAt()).isEqualTo(target.equals("completed") ? prior : null);
    var row = jdbc.queryForMap("SELECT * FROM tasks WHERE id=?", parent);
    assertThat(((java.sql.Timestamp) row.get("updated_at")).toInstant()).isEqualTo(prior);
    assertThat(row.get("completed_at"))
        .isEqualTo(target.equals("completed") ? java.sql.Timestamp.from(prior) : null);
    assertThat(
            jdbc.queryForObject(
                    "SELECT occurred_at FROM task_status_history", java.sql.Timestamp.class)
                .toInstant())
        .isEqualTo(prior);
    assertThat(
            jdbc.queryForObject("SELECT payload->>'occurredAt' FROM outbox_events", String.class))
        .isEqualTo(prior.toString());
  }

  @Test
  void s7_s31_rejectsUnsupportedMediaWithoutCaching() throws Exception {
    mvc.perform(
            put(path())
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .header("If-Match", "\"task:" + parent + ":0\"")
                .contentType("text/plain")
                .content("completed"))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM task_status_history", Long.class))
        .isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @Test
  void s11_historyRetainsThreeConfirmedTransitionsAfterOutboxCleanup() throws Exception {
    changeStatus("completed", 0).andExpect(status().isOk());
    changeStatus("pending", 1).andExpect(status().isOk());
    changeStatus("completed", 2).andExpect(status().isOk());
    String historyPath = "/api/v1/projects/" + project + "/tasks/" + parent + "/history";
    var previous =
        json.readTree(
            mvc.perform(get(historyPath).with(user("persona-a")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(previous.get("items").size()).isEqualTo(3);
    assertThat(previous.get("items").get(0).get("toStatus").asText()).isEqualTo("completed");
    assertThat(previous.get("items").get(1).get("toStatus").asText()).isEqualTo("pending");
    assertThat(previous.get("items").get(2).get("toStatus").asText()).isEqualTo("completed");
    var stored = jdbc.queryForList("SELECT * FROM task_status_history ORDER BY task_version DESC");
    for (int n = 0; n < 3; n++) {
      assertThat(previous.get("items").get(n).get("id").asText())
          .isEqualTo(stored.get(n).get("id").toString());
      assertThat(previous.get("items").get(n).size()).isEqualTo(4);
    }
    jdbc.execute("DELETE FROM outbox_events");
    assertThat(
            json.readTree(
                mvc.perform(get(historyPath).with(user("persona-a")))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString()))
        .isEqualTo(previous);
    assertThat(
            jdbc.queryForObject(
                "SELECT completed_at FROM tasks WHERE id=?", java.sql.Timestamp.class, parent))
        .isEqualTo(stored.getFirst().get("occurred_at"));
  }
}
