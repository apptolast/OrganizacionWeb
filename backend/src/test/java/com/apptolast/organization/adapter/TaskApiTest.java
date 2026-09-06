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
class TaskApiTest {
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

  @BeforeEach
  void setup() {
    jdbc.execute("TRUNCATE outbox_events,projects CASCADE");
    project = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'persona-a','P','','idea',now(),now())",
        project);
  }

  String path() {
    return "/api/v1/projects/" + project + "/tasks";
  }

  @Test
  void s1_commitsTaskAndMinimalEvent() throws Exception {
    var response =
        mvc.perform(
                post(path())
                    .with(user("persona-a"))
                    .with(csrf().asHeader())
                    .contentType("application/json")
                    .content("{\"title\":\" Preparar portada \"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.title").value("Preparar portada"))
            .andExpect(jsonPath("$.status").value("pending"))
            .andReturn()
            .getResponse();
    var body = json.readTree(response.getContentAsString());
    assertThat(body.size()).isEqualTo(8);
    assertThat(response.getHeader("Location")).isEqualTo(path() + "/" + body.get("id").asText());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM tasks", Long.class)).isEqualTo(1);
    var event =
        json.readTree(jdbc.queryForObject("SELECT payload::text FROM outbox_events", String.class));
    assertThat(event.size()).isEqualTo(8);
    assertThat(event.get("aggregateId").asText()).isEqualTo(project.toString());
    assertThat(event.get("taskId").asText()).isEqualTo(body.get("id").asText());
    assertThat(event.get("title").asText()).isEqualTo("Preparar portada");
    assertThat(event.get("ownerId").asText()).isEqualTo("persona-a");
    assertThat(event.get("occurredAt").asText()).isEqualTo(body.get("createdAt").asText());
    assertThat(
            jdbc.queryForObject("SELECT created_at FROM tasks", java.sql.Timestamp.class)
                .toInstant())
        .isEqualTo(java.time.Instant.parse(body.get("createdAt").asText()));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource(
      delimiter = '|',
      value = {
        "{}|title|REQUIRED",
        "{\"title\":null}|title|REQUIRED",
        "{\"title\":42}|title|INVALID_TYPE",
        "{\"title\":[]}|title|INVALID_TYPE",
        "{\"title\":\"T\",\"completionCriterion\":42}|completionCriterion|INVALID_TYPE",
        "{\"title\":\"T\",\"estimatedMinutes\":\"2\"}|estimatedMinutes|INVALID_TYPE",
        "{\"title\":\"T\",\"estimatedMinutes\":true}|estimatedMinutes|INVALID_TYPE",
        "{\"title\":\"T\",\"estimatedMinutes\":1.5}|estimatedMinutes|INVALID_TYPE",
        "{\"title\":\"T\",\"estimatedMinutes\":0}|estimatedMinutes|OUT_OF_RANGE",
        "{\"title\":\"T\",\"estimatedMinutes\":1441}|estimatedMinutes|OUT_OF_RANGE"
      })
  void s4_s6_validatesJsonTypes(String body, String field, String code) throws Exception {
    mvc.perform(
            post(path())
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value(field))
        .andExpect(jsonPath("$.errors[0].code").value(code));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM tasks", Long.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource(
      delimiter = '|',
      value = {
        "{|MALFORMED_JSON",
        "{}{}|MALFORMED_JSON",
        "[]|VALIDATION_ERROR",
        "null|VALIDATION_ERROR",
        "{\"title\":\"A\",\"title\":\"B\"}|MALFORMED_JSON",
        "{\"title\":\"T\",\"ownerId\":\"b\"}|VALIDATION_ERROR",
        "{\"title\":\"T\",\"status\":\"pending\"}|VALIDATION_ERROR"
      })
  void s8_strictBody(String body, String code) throws Exception {
    mvc.perform(
            post(path())
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(code));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM tasks", Long.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @Test
  void s19_s23_readsOnlyConfirmedOwnTasks() throws Exception {
    mvc.perform(get(path()).with(user("persona-a")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isEmpty())
        .andExpect(jsonPath("$.nextCursor").isEmpty());
    var created =
        mvc.perform(
                post(path())
                    .with(user("persona-a"))
                    .with(csrf().asHeader())
                    .contentType("application/json")
                    .content(
                        "{\"title\":\"🚀 tarea\",\"completionCriterion\":\"  conservar\\ntexto\",\"estimatedMinutes\":1440}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();
    var detail =
        mvc.perform(get(created.getHeader("Location")).with(user("persona-a")))
            .andExpect(status().isOk())
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(detail.getContentAsString()))
        .isEqualTo(json.readTree(created.getContentAsString()));
    mvc.perform(get(path()).with(user("persona-a")))
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].estimatedMinutes").value(1440));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "missing,404,RESOURCE_NOT_FOUND",
    "foreign,404,RESOURCE_NOT_FOUND",
    "completed,409,PROJECT_COMPLETED"
  })
  void s11_s13_hidesUnavailableProjects(String mode, int expected, String code) throws Exception {
    if (mode.equals("missing")) jdbc.update("DELETE FROM projects WHERE id=?", project);
    if (mode.equals("foreign"))
      jdbc.update("UPDATE projects SET owner_id='other' WHERE id=?", project);
    if (mode.equals("completed"))
      jdbc.update("UPDATE projects SET status='completed' WHERE id=?", project);
    mvc.perform(
            post(path())
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"title\":\"T\"}"))
        .andExpect(status().is(expected))
        .andExpect(jsonPath("$.code").value(code));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM tasks", Long.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @Test
  void s20_s21_stablePaginationWithTiesAndNewerInsert() throws Exception {
    for (int n = 1; n <= 21; n++)
      jdbc.update(
          "INSERT INTO tasks VALUES (?,?,?,'',null,'pending','2026-01-01T00:00:00.123456Z','2026-01-01T00:00:00.123456Z')",
          new UUID(0, n),
          project,
          "T" + n);
    var first =
        json.readTree(
            mvc.perform(get(path()).with(user("persona-a")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(first.get("items").size()).isEqualTo(20);
    assertThat(first.get("items").get(0).get("title").asText()).isEqualTo("T21");
    assertThat(first.get("items").get(19).get("title").asText()).isEqualTo("T2");
    jdbc.update(
        "INSERT INTO tasks VALUES (?,?,?,'',null,'pending','2026-02-01Z','2026-02-01Z')",
        UUID.randomUUID(),
        project,
        "newer");
    mvc.perform(
            get(path()).param("cursor", first.get("nextCursor").asText()).with(user("persona-a")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].title").value("T1"))
        .andExpect(jsonPath("$.nextCursor").isEmpty());
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "empty",
        "garbage",
        "missing",
        "extra",
        "foreign",
        "timestamp",
        "range",
        "precision",
        "id",
        "duplicate",
        "limit",
        "unknown",
        "repeated",
        "trailing"
      })
  void s22_rejectsInvalidCursorAndQuery(String defect) throws Exception {
    var position =
        json.createObjectNode()
            .put("projectId", project.toString())
            .put("createdAt", "2026-01-01T00:00:00Z")
            .put("id", UUID.randomUUID().toString());
    switch (defect) {
      case "missing" -> position.remove("id");
      case "extra" -> position.put("extra", true);
      case "foreign" -> position.put("projectId", UUID.randomUUID().toString());
      case "timestamp" -> position.put("createdAt", "not-time");
      case "range" -> position.put("createdAt", "+1000000000-12-31T23:59:59Z");
      case "precision" -> position.put("createdAt", "2026-01-01T00:00:00.123456789Z");
      case "id" -> position.put("id", "1-1-1-1-1");
      default -> {}
    }
    String raw = position.toString();
    if (defect.equals("duplicate")) raw = raw.replace("{", "{\"id\":\"duplicate\",");
    if (defect.equals("trailing")) raw += "{}";
    String cursor =
        java.util.Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    if (defect.equals("empty")) cursor = "";
    if (defect.equals("garbage")) cursor = "!";
    var request = get(path()).with(user("persona-a"));
    String field = "cursor";
    if (defect.equals("limit") || defect.equals("unknown")) {
      request.param(defect, "1");
      field = "query";
    } else if (defect.equals("repeated")) request.param("cursor", cursor, cursor);
    else request.param("cursor", cursor);
    mvc.perform(request)
        .andExpect(status().isBadRequest())
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value(field));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"create", "list", "detailProject", "detailTask"})
  void s12_rejectsPartialIdentifiers(String operation) throws Exception {
    String target =
        operation.equals("detailTask")
            ? path() + "/1-1-1-1-1"
            : "/api/v1/projects/1-1-1-1-1/tasks"
                + (operation.equals("detailProject") ? "/" + UUID.randomUUID() : "");
    var request =
        operation.equals("create")
            ? post(target).contentType("application/json").content("{\"title\":\"T\"}")
            : get(target);
    mvc.perform(request.with(user("persona-a")).with(csrf().asHeader()))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.errors[0].field")
                .value(operation.equals("detailTask") ? "taskId" : "projectId"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_FORMAT"));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "tasks,skip",
    "outbox_events,skip",
    "tasks,fail",
    "outbox_events,fail"
  })
  void s15_rollsBackSuppressedOrFailedWrites(String table, String mode) throws Exception {
    var previous = jdbc.queryForMap("SELECT * FROM projects WHERE id=?", project);
    jdbc.execute(
        "CREATE FUNCTION reject_task_write() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN "
            + (mode.equals("skip") ? "RETURN NULL;" : "RAISE EXCEPTION 'synthetic failure';")
            + " END $$");
    jdbc.execute(
        "CREATE TRIGGER reject_task_write BEFORE INSERT ON "
            + table
            + " FOR EACH ROW EXECUTE FUNCTION reject_task_write()");
    try {
      mvc.perform(
              post(path())
                  .with(user("persona-a"))
                  .with(csrf().asHeader())
                  .contentType("application/json")
                  .content("{\"title\":\"T\"}"))
          .andExpect(status().isServiceUnavailable())
          .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"))
          .andExpect(header().doesNotExist("Location"));
      assertThat(jdbc.queryForObject("SELECT count(*) FROM tasks", Long.class)).isZero();
      assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
      assertThat(jdbc.queryForMap("SELECT * FROM projects WHERE id=?", project))
          .isEqualTo(previous);
    } finally {
      jdbc.execute("DROP TRIGGER reject_task_write ON " + table);
      jdbc.execute("DROP FUNCTION reject_task_write()");
    }
  }

  @Autowired com.apptolast.organization.application.TaskCommit taskCommit;
  @Autowired com.apptolast.organization.application.ProjectStatusEditing statusEditing;

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
  void s14_serializesCreationAndCompletionOnSameProject(boolean taskFirst) throws Exception {
    jdbc.update("UPDATE projects SET status='active' WHERE id=?", project);
    var locked = new java.util.concurrent.CountDownLatch(1);
    var release = new java.util.concurrent.CountDownLatch(1);
    java.time.Clock clock = java.time.Clock.systemUTC();
    com.apptolast.organization.application.TaskCommit creationPort =
        (owner, id, operation) ->
            taskCommit.save(
                owner,
                id,
                state -> {
                  if (taskFirst) {
                    locked.countDown();
                    await(release);
                  }
                  return operation.apply(state);
                });
    com.apptolast.organization.application.ProjectStatusEditing transitionPort =
        (owner, id, operation) ->
            statusEditing.update(
                owner,
                id,
                (snapshot, count) -> {
                  if (!taskFirst) {
                    locked.countDown();
                    await(release);
                  }
                  return operation.apply(snapshot, count);
                });
    var create = new com.apptolast.organization.application.CreateTask(creationPort, clock);
    var finish =
        new com.apptolast.organization.application.ChangeProjectStatus(transitionPort, clock, 3);
    java.util.concurrent.Callable<Object> createCall =
        () -> create.execute("persona-a", project, "T", null, null);
    java.util.concurrent.Callable<Object> finishCall =
        () ->
            finish.execute(
                "persona-a",
                project,
                new com.apptolast.organization.domain.ProjectRevision(project, 0),
                "completed");
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var first = executor.submit(taskFirst ? createCall : finishCall);
      try {
        assertThat(locked.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        var second = executor.submit(taskFirst ? finishCall : createCall);
        assertThatThrownBy(() -> second.get(200, java.util.concurrent.TimeUnit.MILLISECONDS))
            .isInstanceOf(java.util.concurrent.TimeoutException.class);
        release.countDown();
        first.get(5, java.util.concurrent.TimeUnit.SECONDS);
        if (taskFirst) second.get(5, java.util.concurrent.TimeUnit.SECONDS);
        else
          assertThatThrownBy(() -> second.get(5, java.util.concurrent.TimeUnit.SECONDS))
              .hasCauseInstanceOf(
                  com.apptolast.organization.application.ProjectCompletedException.class);
      } finally {
        release.countDown();
      }
    }
    assertThat(jdbc.queryForObject("SELECT status FROM projects WHERE id=?", String.class, project))
        .isEqualTo("completed");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM tasks WHERE status='pending'", Long.class))
        .isEqualTo(taskFirst ? 1 : 0);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE event_type='TaskCreated.v1'", Long.class))
        .isEqualTo(taskFirst ? 1 : 0);
  }

  private static void await(java.util.concurrent.CountDownLatch latch) {
    try {
      if (!latch.await(5, java.util.concurrent.TimeUnit.SECONDS))
        throw new AssertionError("Timed out releasing owned test transaction");
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
      throw new AssertionError(error);
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"missingProject", "foreignProject", "missingTask", "foreignTask", "wrongProject"})
  void s11_privateReadsUseOneNotFoundProblem(String defect) throws Exception {
    UUID task = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO tasks VALUES (?,?,?,'',null,'pending',now(),now())",
        task,
        project,
        "private-title");
    String target = path() + "/" + task;
    if (defect.equals("missingProject"))
      target = "/api/v1/projects/" + UUID.randomUUID() + "/tasks";
    if (defect.equals("foreignProject")) {
      jdbc.update("UPDATE projects SET owner_id='other' WHERE id=?", project);
      target = path();
    }
    if (defect.equals("missingTask")) target = path() + "/" + UUID.randomUUID();
    if (defect.equals("foreignTask"))
      jdbc.update("UPDATE projects SET owner_id='other' WHERE id=?", project);
    if (defect.equals("wrongProject")) {
      UUID other = UUID.randomUUID();
      jdbc.update(
          "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'persona-a','Other','','idea',now(),now())",
          other);
      target = "/api/v1/projects/" + other + "/tasks/" + task;
    }
    mvc.perform(get(target).with(user("persona-a")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
        .andExpect(jsonPath("$.title").value("No se ha encontrado el recurso."))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("private-title"))));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "anonymousCreate,401,UNAUTHENTICATED",
    "anonymousList,401,UNAUTHENTICATED",
    "anonymousDetail,401,UNAUTHENTICATED",
    "csrf,403,CSRF_INVALID",
    "invalidCsrf,403,CSRF_INVALID",
    "origin,403,UNTRUSTED_ORIGIN",
    "media,415,UNSUPPORTED_MEDIA_TYPE"
  })
  void s8_s9_s10_s24_protectsTaskResources(String defect, int expected, String code)
      throws Exception {
    var request =
        defect.equals("anonymousList")
            ? get(path())
            : defect.equals("anonymousDetail")
                ? get(path() + "/" + UUID.randomUUID())
                : post(path())
                    .contentType(defect.equals("media") ? "text/plain" : "application/json")
                    .content("{\"title\":\"T\"}");
    if (!defect.startsWith("anonymous")) request.with(user("persona-a"));
    if (defect.equals("origin"))
      request.header("Origin", "https://foreign.example").with(csrf().asHeader());
    if (defect.equals("media")) request.with(csrf().asHeader());
    if (defect.equals("invalidCsrf")) request.with(csrf().useInvalidToken().asHeader());
    mvc.perform(request)
        .andExpect(status().is(expected))
        .andExpect(jsonPath("$.code").value(code))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM tasks", Long.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @Test
  void s12_pathErrorPrecedesCursorParsing() throws Exception {
    String cursor =
        java.util.Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                json.writeValueAsBytes(
                    java.util.Map.of(
                        "projectId",
                        project.toString(),
                        "createdAt",
                        "2026-01-01T00:00:00Z",
                        "id",
                        UUID.randomUUID().toString())));
    mvc.perform(
            get("/api/v1/projects/1-1-1-1-1/tasks").param("cursor", cursor).with(user("persona-a")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("projectId"));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"list", "detail"})
  void s24_s25_storageFailureNeverBecomesEmpty(String operation) throws Exception {
    jdbc.execute("ALTER TABLE tasks RENAME TO unavailable_tasks_fixture");
    try {
      mvc.perform(
              get(path() + (operation.equals("detail") ? "/" + UUID.randomUUID() : ""))
                  .with(user("persona-a")))
          .andExpect(status().isServiceUnavailable())
          .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"))
          .andExpect(jsonPath("$.items").doesNotExist())
          .andExpect(
              header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    } finally {
      jdbc.execute("ALTER TABLE unavailable_tasks_fixture RENAME TO tasks");
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"idea", "active", "paused"})
  void s13_doesNotAlterParentRepresentationOrRevision(String state) throws Exception {
    jdbc.update("UPDATE projects SET status=? WHERE id=?", state, project);
    var before = jdbc.queryForMap("SELECT * FROM projects WHERE id=?", project);
    mvc.perform(
            post(path())
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"title\":\"T\"}"))
        .andExpect(status().isCreated());
    assertThat(jdbc.queryForMap("SELECT * FROM projects WHERE id=?", project)).isEqualTo(before);
    mvc.perform(get("/api/v1/projects/" + project).with(user("persona-a")))
        .andExpect(header().string("ETag", "\"" + project + ":0\""));
  }
}
