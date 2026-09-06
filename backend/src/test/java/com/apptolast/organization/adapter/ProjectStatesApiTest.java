package com.apptolast.organization.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
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
class ProjectStatesApiTest {
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  // Spring caches its context across PIT iterations; retain the same JDBC endpoint per JVM.
  // Testcontainers Ryuk still owns cleanup when this test JVM exits.
  static {
    postgres.start();
  }

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @TestConfiguration
  static class Time {
    @Bean
    @Primary
    Clock testClock() {
      return Clock.fixed(Instant.parse("2026-09-05T12:00:00Z"), ZoneOffset.UTC);
    }
  }

  @Autowired MockMvc mvc;

  @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
  com.apptolast.organization.adapter.persistence.PostgresProjectQueries queries;

  @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
  com.apptolast.organization.adapter.persistence.PostgresProjectStatusEditing editing;

  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;

  @BeforeEach
  void clear() {
    jdbc.execute("TRUNCATE task_status_history, tasks, outbox_events, projects");
  }

  java.util.UUID seed() {
    var id = java.util.UUID.randomUUID();
    var now = java.sql.Timestamp.from(java.time.Instant.parse("2026-09-05T11:00:00Z"));
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,?,?,?,'idea',?,?)",
        id,
        "persona-a",
        "Original",
        "Description",
        now,
        now);
    return id;
  }

  @Test
  void s1_s11_updatesStateAndPersistsOneExactEvent() throws Exception {
    var id = seed();
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                    "/api/v1/projects/" + id + "/status")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .header("If-Match", "\"" + id + ":0\"")
                .contentType("application/json")
                .content("{\"status\":\"active\"}"))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"" + id + ":1\""))
        .andExpect(jsonPath("$.status").value("active"))
        .andExpect(jsonPath("$.name").value("Original"))
        .andExpect(jsonPath("$.description").value("Description"))
        .andExpect(jsonPath("$.updatedAt").value("2026-09-05T12:00:00Z"));
    assertThat(jdbc.queryForMap("SELECT status,version FROM projects WHERE id=?", id))
        .containsEntry("status", "active")
        .containsEntry("version", 1L);
    var event = jdbc.queryForMap("SELECT * FROM outbox_events");
    assertThat(event.get("event_type")).isEqualTo("ProjectStatusChanged.v1");
    assertThat(event.get("status")).isEqualTo("pending");
    var payload = json.readTree(event.get("payload").toString());
    assertThat(payload.size()).isEqualTo(8);
    assertThat(payload.get("eventId").asText()).isEqualTo(event.get("event_id").toString());
    assertThat(payload.get("aggregateId").asText()).isEqualTo(id.toString());
    assertThat(payload.get("ownerId").asText()).isEqualTo("persona-a");
    assertThat(payload.get("occurredAt").asText()).isEqualTo("2026-09-05T12:00:00Z");
    assertThat(payload.get("schemaVersion").asInt()).isEqualTo(1);
    assertThat(payload.get("type").asText()).isEqualTo("ProjectStatusChanged.v1");
    assertThat(payload.get("fromStatus").asText()).isEqualTo("idea");
    assertThat(payload.get("toStatus").asText()).isEqualTo("active");
  }

  org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder transition(
      java.util.UUID id, long version, String state) {
    return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
            "/api/v1/projects/" + id + "/status")
        .with(user("persona-a"))
        .with(csrf().asHeader())
        .header("If-Match", "\"" + id + ":" + version + "\"")
        .contentType("application/json")
        .content("{\"status\":\"" + state + "\"}");
  }

  @Test
  void s2_invalidTransitionReturnsConflictWithoutWrites() throws Exception {
    var id = seed();
    var before = jdbc.queryForList("SELECT * FROM projects");
    mvc.perform(transition(id, 0, "paused"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVALID_PROJECT_TRANSITION"));
    assertThat(jdbc.queryForList("SELECT * FROM projects")).isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @Test
  void s4_onlyOneConcurrentActivationCanTakeLastSlot() throws Exception {
    for (int i = 0; i < 2; i++) {
      var active = seed();
      jdbc.update("UPDATE projects SET status='active' WHERE id=?", active);
    }
    var first = seed();
    var second = seed();
    var start = new java.util.concurrent.CountDownLatch(1);
    var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
    try {
      var futures =
          new java.util.ArrayList<
              java.util.concurrent.Future<org.springframework.mock.web.MockHttpServletResponse>>();
      for (var id : java.util.List.of(first, second))
        futures.add(
            pool.submit(
                () -> {
                  if (!start.await(5, java.util.concurrent.TimeUnit.SECONDS))
                    throw new AssertionError("start timeout");
                  return mvc.perform(transition(id, 0, "active")).andReturn().getResponse();
                }));
      start.countDown();
      var responses =
          new java.util.ArrayList<org.springframework.mock.web.MockHttpServletResponse>();
      for (var future : futures)
        responses.add(future.get(10, java.util.concurrent.TimeUnit.SECONDS));
      assertThat(responses)
          .extracting(org.springframework.mock.web.MockHttpServletResponse::getStatus)
          .containsExactlyInAnyOrder(200, 409);
      var rejected =
          responses.stream()
              .filter(response -> response.getStatus() == 409)
              .findFirst()
              .orElseThrow();
      var problem = json.readTree(rejected.getContentAsString());
      assertThat(problem.get("code").asText()).isEqualTo("ACTIVE_PROJECT_LIMIT");
      assertThat(problem.get("activeCount").asLong()).isEqualTo(3);
      assertThat(problem.get("limit").asInt()).isEqualTo(3);
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM projects WHERE owner_id='persona-a' AND status='active'",
                  Long.class))
          .isEqualTo(3);
      assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class))
          .isEqualTo(1);
    } finally {
      start.countDown();
      pool.shutdownNow();
      assertThat(pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "idea,active",
    "idea,completed",
    "active,paused",
    "active,completed",
    "paused,active",
    "paused,completed",
    "completed,paused"
  })
  void s1_s14_transitionsRemainReadableAndTextEditingPreservesState(String from, String to)
      throws Exception {
    var id = seed();
    jdbc.update("UPDATE projects SET status=? WHERE id=?", from, id);
    mvc.perform(transition(id, 0, to))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(to));
    for (String path : java.util.List.of("/api/v1/projects/" + id, "/api/v1/projects"))
      mvc.perform(
              org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path)
                  .with(user("persona-a"))
                  .with(csrf().asHeader()))
          .andExpect(status().isOk())
          .andExpect(
              jsonPath(path.endsWith(id.toString()) ? "$.status" : "$.items[0].status").value(to));
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                    "/api/v1/projects/" + id)
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .header("If-Match", "\"" + id + ":1\"")
                .contentType("application/json")
                .content("{\"name\":\"Edited\",\"description\":\"Text\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(to))
        .andExpect(header().string("ETag", "\"" + id + ":2\""));
    assertThat(jdbc.queryForObject("SELECT status FROM projects WHERE id=?", String.class, id))
        .isEqualTo(to);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"idea", "active", "paused", "completed"})
  void s3_sameStatePreservesDatabaseAndTag(String state) throws Exception {
    var id = seed();
    jdbc.update("UPDATE projects SET status=? WHERE id=?", state, id);
    var before = jdbc.queryForList("SELECT * FROM projects");
    mvc.perform(transition(id, 0, state))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"" + id + ":0\""));
    assertThat(jdbc.queryForList("SELECT * FROM projects")).isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "noauth,401,UNAUTHENTICATED",
    "foreign,404,PROJECT_NOT_FOUND",
    "absent,404,PROJECT_NOT_FOUND",
    "id,400,VALIDATION_ERROR",
    "missingTag,428,PRECONDITION_REQUIRED",
    "badTag,400,VALIDATION_ERROR",
    "repeat,400,VALIDATION_ERROR",
    "weak,400,VALIDATION_ERROR",
    "wildcard,400,VALIDATION_ERROR",
    "unknown,400,VALIDATION_ERROR",
    "null,400,VALIDATION_ERROR",
    "absentState,400,VALIDATION_ERROR",
    "extra,400,VALIDATION_ERROR",
    "duplicate,400,VALIDATION_ERROR",
    "trailing,400,VALIDATION_ERROR",
    "origin,403,UNTRUSTED_ORIGIN",
    "media,415,UNSUPPORTED_MEDIA_TYPE",
    "emptyBody,400,VALIDATION_ERROR",
    "malformed,400,VALIDATION_ERROR"
  })
  void s10_boundaryFailuresCannotWriteOrExposePrivateState(String defect, int expected, String code)
      throws Exception {
    var id = seed();
    if (defect.equals("foreign"))
      jdbc.update("UPDATE projects SET owner_id='other-owner',version=4 WHERE id=?", id);
    var before = jdbc.queryForList("SELECT * FROM projects");
    String requested =
        defect.equals("absent")
            ? java.util.UUID.randomUUID().toString()
            : defect.equals("id") ? "1-1-1-1-1" : id.toString();
    String tag = "\"" + id + ":0\"";
    if (defect.equals("weak")) tag = "W/" + tag;
    else if (defect.equals("wildcard")) tag = "*";
    else if (defect.equals("badTag")) tag = "wrong";
    String body =
        switch (defect) {
          case "unknown" -> "{\"status\":\"other\"}";
          case "null" -> "{\"status\":null}";
          case "absentState" -> "{}";
          case "extra" -> "{\"status\":\"active\",\"ownerId\":\"other\"}";
          case "duplicate" -> "{\"status\":\"active\",\"status\":\"completed\"}";
          case "trailing" -> "{\"status\":\"active\"}{}";
          case "emptyBody" -> "";
          case "malformed" -> "{";
          default -> "{\"status\":\"active\"}";
        };
    var request =
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                "/api/v1/projects/" + requested + "/status")
            .contentType(defect.equals("media") ? "text/plain" : "application/json")
            .content(body);
    if (!defect.equals("noauth")) request.with(user("persona-a")).with(csrf().asHeader());
    if (!defect.equals("missingTag")) request.header("If-Match", tag);
    if (defect.equals("repeat")) request.header("If-Match", tag);
    if (defect.equals("origin")) request.header("Origin", "https://evil.example");
    var response =
        mvc.perform(request)
            .andExpect(status().is(expected))
            .andExpect(jsonPath("$.code").value(code))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(response).doesNotContain("other-owner", "Original", "Description");
    assertThat(jdbc.queryForList("SELECT * FROM projects")).isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"project", "outbox", "projectSuppressed", "outboxSuppressed"})
  void s12_failedOrSuppressedWriteRollsBackAllState(String target) throws Exception {
    var id = seed();
    var before = jdbc.queryForList("SELECT * FROM projects");
    String table = target.startsWith("outbox") ? "outbox_events" : "projects",
        operation = target.startsWith("outbox") ? "INSERT" : "UPDATE";
    String action =
        target.endsWith("Suppressed")
            ? "RETURN NULL;"
            : "RAISE EXCEPTION 'synthetic write failure';";
    jdbc.execute(
        "CREATE FUNCTION reject_state() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN "
            + action
            + " END; $$");
    jdbc.execute(
        "CREATE TRIGGER reject_state BEFORE "
            + operation
            + " ON "
            + table
            + " FOR EACH ROW EXECUTE FUNCTION reject_state()");
    try {
      mvc.perform(transition(id, 0, "active"))
          .andExpect(status().isServiceUnavailable())
          .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"));
      assertThat(jdbc.queryForList("SELECT * FROM projects")).isEqualTo(before);
      assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
    } finally {
      jdbc.execute("DROP TRIGGER reject_state ON " + table);
      jdbc.execute("DROP FUNCTION reject_state()");
    }
  }

  @Test
  void s5_s6_pauseReleasesOnlyOwnersCapacityForAnotherActivation() throws Exception {
    var own = new java.util.ArrayList<java.util.UUID>();
    for (int i = 0; i < 3; i++) {
      var id = seed();
      own.add(id);
      jdbc.update("UPDATE projects SET status='active' WHERE id=?", id);
      var other = seed();
      jdbc.update("UPDATE projects SET owner_id='other-owner',status='active' WHERE id=?", other);
    }
    var otherBefore = jdbc.queryForList("SELECT * FROM projects WHERE owner_id='other-owner'");
    var candidate = seed();
    mvc.perform(transition(own.getFirst(), 0, "paused")).andExpect(status().isOk());
    mvc.perform(transition(candidate, 0, "active")).andExpect(status().isOk());
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM projects WHERE owner_id='persona-a' AND status='active'",
                Long.class))
        .isEqualTo(3);
    assertThat(jdbc.queryForList("SELECT * FROM projects WHERE owner_id='other-owner'"))
        .isEqualTo(otherBefore);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(2);
  }

  @Test
  void s8_lowerLimitDoesNotPauseExistingProjects() {
    for (int i = 0; i < 3; i++) {
      var id = seed();
      jdbc.update("UPDATE projects SET status='active' WHERE id=?", id);
    }
    var candidate = seed();
    var before = jdbc.queryForList("SELECT * FROM projects");
    var useCase =
        new com.apptolast.organization.application.ChangeProjectStatus(
            editing, Clock.fixed(Instant.parse("2026-09-05T12:00:00Z"), ZoneOffset.UTC), 2);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                useCase.execute(
                    "persona-a",
                    candidate,
                    new com.apptolast.organization.domain.ProjectRevision(candidate, 0),
                    "active"))
        .isInstanceOf(com.apptolast.organization.application.ActiveProjectLimitException.class)
        .satisfies(
            error -> {
              var limit =
                  (com.apptolast.organization.application.ActiveProjectLimitException) error;
              assertThat(limit.activeCount()).isEqualTo(3);
              assertThat(limit.limit()).isEqualTo(2);
            });
    assertThat(jdbc.queryForList("SELECT * FROM projects")).isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"textFirst", "stateFirst", "stateNoop"})
  void s9_textAndStateShareOneRevision(String order) throws Exception {
    var id = seed();
    var text =
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                "/api/v1/projects/" + id)
            .with(user("persona-a"))
            .with(csrf().asHeader())
            .header("If-Match", "\"" + id + ":0\"")
            .contentType("application/json")
            .content("{\"name\":\"Edited\",\"description\":\"Text\"}");
    mvc.perform(order.equals("textFirst") ? text : transition(id, 0, "active"))
        .andExpect(status().isOk());
    var before = jdbc.queryForList("SELECT * FROM projects");
    var events = jdbc.queryForList("SELECT * FROM outbox_events");
    mvc.perform(order.equals("stateFirst") ? text : transition(id, 0, "active"))
        .andExpect(status().isPreconditionFailed())
        .andExpect(jsonPath("$.code").value("PROJECT_CONFLICT"));
    assertThat(jdbc.queryForList("SELECT * FROM projects")).isEqualTo(before);
    assertThat(jdbc.queryForList("SELECT * FROM outbox_events")).isEqualTo(events);
  }

  @Test
  void s17_internalFailureIsSafeAndPrivate() throws Exception {
    var id = seed();
    var before = jdbc.queryForList("SELECT * FROM projects");
    org.mockito.Mockito.doThrow(new IllegalStateException("SQL secret-password other-owner"))
        .when(editing)
        .update(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
    var body =
        mvc.perform(transition(id, 0, "active"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
            .andExpect(jsonPath("$.correlationId").isString())
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(body)
        .doesNotContain("SQL", "secret-password", "other-owner", "IllegalStateException");
    assertThat(jdbc.queryForList("SELECT * FROM projects")).isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }
}
