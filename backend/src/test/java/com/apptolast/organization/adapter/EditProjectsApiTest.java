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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = {
      "app.auth.username=persona-a",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@AutoConfigureMockMvc
@Testcontainers
class EditProjectsApiTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

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
  com.apptolast.organization.adapter.persistence.PostgresProjectEditing editing;

  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;

  @BeforeEach
  void clear() {
    jdbc.execute("TRUNCATE outbox_events, projects CASCADE");
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
  void s1_detailIncludesStrongVersionTagFromSameSnapshot() throws Exception {
    var id = seed();
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/v1/projects/" + id)
                .with(user("persona-a"))
                .with(csrf().asHeader()))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"" + id + ":0\""))
        .andExpect(jsonPath("$.name").value("Original"))
        .andExpect(jsonPath("$.version").doesNotExist());
  }

  @Test
  void s1_s12_putPersistsProjectVersionAndSingleEvent() throws Exception {
    var id = seed();
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                    "/api/v1/projects/" + id)
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .header("If-Match", "\"" + id + ":0\"")
                .contentType("application/json")
                .content("{\"name\":\"  Changed 😀  \",\"description\":\"<b>literal</b>\"}"))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"" + id + ":1\""))
        .andExpect(jsonPath("$.name").value("Changed 😀"))
        .andExpect(jsonPath("$.description").value("<b>literal</b>"))
        .andExpect(jsonPath("$.createdAt").value("2026-09-05T11:00:00Z"))
        .andExpect(jsonPath("$.updatedAt").value("2026-09-05T12:00:00Z"));
    var row = jdbc.queryForMap("SELECT * FROM projects WHERE id=?", id);
    assertThat(row.get("version")).isEqualTo(1L);
    assertThat(row.get("name")).isEqualTo("Changed 😀");
    var events = jdbc.queryForList("SELECT * FROM outbox_events");
    assertThat(events).hasSize(1);
    var event = events.getFirst();
    assertThat(event.get("event_type")).isEqualTo("ProjectUpdated.v1");
    assertThat(event.get("status")).isEqualTo("pending");
    var payload = json.readTree(event.get("payload").toString());
    assertThat(payload.size()).isEqualTo(7);
    assertThat(payload.get("eventId").asText()).isEqualTo(event.get("event_id").toString());
    assertThat(payload.get("aggregateId").asText()).isEqualTo(id.toString());
    assertThat(payload.get("ownerId").asText()).isEqualTo("persona-a");
    assertThat(payload.get("name").asText()).isEqualTo("Changed 😀");
    assertThat(payload.get("occurredAt").asText()).isEqualTo("2026-09-05T12:00:00Z");
    assertThat(payload.get("schemaVersion").asInt()).isEqualTo(1);
    assertThat(payload.get("type").asText()).isEqualTo("ProjectUpdated.v1");
  }

  org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder change(
      java.util.UUID id, String tag, String body) {
    return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
            "/api/v1/projects/" + id)
        .with(user("persona-a"))
        .with(csrf().asHeader())
        .header("If-Match", tag)
        .contentType("application/json")
        .content(body);
  }

  @Test
  void s2_oldVersionCannotOverwriteOrPassAsNoop() throws Exception {
    var id = seed();
    String tag = "\"" + id + ":0\"";
    String body = "{\"name\":\"New\",\"description\":\"New text\"}";
    mvc.perform(change(id, tag, body)).andExpect(status().isOk());
    var before = jdbc.queryForMap("SELECT * FROM projects WHERE id=?", id);
    var events = jdbc.queryForList("SELECT * FROM outbox_events");
    mvc.perform(change(id, tag, body))
        .andExpect(status().isPreconditionFailed())
        .andExpect(jsonPath("$.code").value("PROJECT_CONFLICT"));
    assertThat(jdbc.queryForMap("SELECT * FROM projects WHERE id=?", id)).isEqualTo(before);
    assertThat(jdbc.queryForList("SELECT * FROM outbox_events")).isEqualTo(events);
  }

  @Test
  void s4_missingPreconditionNeverWritesEvenNoop() throws Exception {
    var id = seed();
    var before = jdbc.queryForMap("SELECT * FROM projects WHERE id=?", id);
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                    "/api/v1/projects/" + id)
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"name\":\"Original\",\"description\":\"Description\"}"))
        .andExpect(status().is(428))
        .andExpect(jsonPath("$.code").value("PRECONDITION_REQUIRED"));
    assertThat(jdbc.queryForMap("SELECT * FROM projects WHERE id=?", id)).isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "malformed",
        "weak",
        "wildcard",
        "repeated",
        "negative",
        "overflow",
        "list",
        "upper",
        "zeros"
      })
  void s5_invalidPreconditionNeverWrites(String defect) throws Exception {
    var id = seed();
    String valid = "\"" + id + ":0\"";
    String tag =
        switch (defect) {
          case "upper" -> valid.toUpperCase(java.util.Locale.ROOT);
          case "zeros" -> valid.replace(":0", ":00");
          case "weak" -> "W/" + valid;
          case "wildcard" -> "*";
          case "negative" -> "\"" + id + ":-1\"";
          case "overflow" -> "\"" + id + ":9223372036854775808\"";
          case "list" -> valid + "," + valid;
          case "repeated" -> valid;
          default -> "invalid";
        };
    var request = change(id, tag, "{\"name\":\"Changed\",\"description\":\"Text\"}");
    if (defect.equals("repeated")) request.header("If-Match", valid);
    var before = jdbc.queryForMap("SELECT * FROM projects WHERE id=?", id);
    mvc.perform(request)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    assertThat(jdbc.queryForMap("SELECT * FROM projects WHERE id=?", id)).isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "empty,name",
    "longName,name",
    "longDescription,description",
    "missingName,name",
    "missingDescription,description",
    "nullDescription,description",
    "extra,body",
    "trailing,body",
    "duplicate,body",
    "nameType,name",
    "descriptionType,description",
    "array,body",
    "malformed,body"
  })
  void s6_invalidBodyNeverWrites(String defect, String field) throws Exception {
    var id = seed();
    String valid = "{\"name\":\"Changed\",\"description\":\"Text\"}";
    String body =
        switch (defect) {
          case "empty" -> valid.replace("Changed", "");
          case "longName" -> valid.replace("Changed", "😀".repeat(121));
          case "longDescription" -> valid.replace("Text", "😀".repeat(4001));
          case "missingName" -> "{\"description\":\"Text\"}";
          case "missingDescription" -> "{\"name\":\"Changed\"}";
          case "nullDescription" -> valid.replace("\"Text\"", "null");
          case "extra" -> valid.replace("}", ",\"ownerId\":\"other\"}");
          case "trailing" -> valid + "{}";
          case "duplicate" -> valid.replace("}", ",\"name\":\"Other\"}");
          case "nameType" -> valid.replace("\"Changed\"", "42");
          case "descriptionType" -> valid.replace("\"Text\"", "false");
          case "array" -> "[]";
          default -> "{";
        };
    var before = jdbc.queryForMap("SELECT * FROM projects WHERE id=?", id);
    mvc.perform(change(id, "\"" + id + ":0\"", body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value(field));
    assertThat(jdbc.queryForMap("SELECT * FROM projects WHERE id=?", id)).isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"project", "outbox", "suppressed", "outboxSuppressed"})
  void s13_anyFailedWriteRollsBackProjectVersionAndEvent(String target) throws Exception {
    var id = seed();
    var before = jdbc.queryForMap("SELECT * FROM projects WHERE id=?", id);
    String table = target.startsWith("outbox") ? "outbox_events" : "projects";
    String operation = target.startsWith("outbox") ? "INSERT" : "UPDATE";
    String action =
        (target.equals("suppressed") || target.equals("outboxSuppressed"))
            ? "RETURN NULL;"
            : "RAISE EXCEPTION 'fixture write failed';";
    jdbc.execute(
        "CREATE FUNCTION reject_edit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN "
            + action
            + " END; $$");
    jdbc.execute(
        "CREATE TRIGGER reject_edit BEFORE "
            + operation
            + " ON "
            + table
            + " FOR EACH ROW EXECUTE FUNCTION reject_edit()");
    try {
      mvc.perform(change(id, "\"" + id + ":0\"", "{\"name\":\"Changed\",\"description\":\"Text\"}"))
          .andExpect(status().isServiceUnavailable())
          .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"));
      assertThat(jdbc.queryForMap("SELECT * FROM projects WHERE id=?", id)).isEqualTo(before);
      assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
    } finally {
      jdbc.execute("DROP TRIGGER reject_edit ON " + table);
      jdbc.execute("DROP FUNCTION reject_edit()");
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"invalid", "1-1-1-1-1"})
  void s10_invalidIdentityIs400WithoutWrites(String id) throws Exception {
    seed();
    var before = jdbc.queryForList("SELECT * FROM projects");
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                    "/api/v1/projects/" + id)
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .header("If-Match", "\"00000000-0000-0000-0000-000000000000:0\"")
                .contentType("application/json")
                .content("{\"name\":\"Changed\",\"description\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("id"));
    assertThat(jdbc.queryForList("SELECT * FROM projects")).isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @Test
  void s3_noopPreservesEntireStoredSnapshotAndTag() throws Exception {
    var id = seed();
    var before = jdbc.queryForMap("SELECT * FROM projects WHERE id=?", id);
    mvc.perform(
            change(
                id,
                "\"" + id + ":0\"",
                "{\"name\":\"  Original  \",\"description\":\"Description\"}"))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"" + id + ":0\""))
        .andExpect(jsonPath("$.updatedAt").value("2026-09-05T11:00:00Z"))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    assertThat(jdbc.queryForMap("SELECT * FROM projects WHERE id=?", id)).isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void s8_foreignAndAbsentProjectsAreIndistinguishableBeforeVersionCheck(boolean foreign)
      throws Exception {
    var id = seed();
    if (foreign) jdbc.update("UPDATE projects SET owner_id='persona-b' WHERE id=?", id);
    var before = jdbc.queryForList("SELECT * FROM projects");
    var requested = foreign ? id : java.util.UUID.randomUUID();
    mvc.perform(
            change(
                requested,
                "\"" + requested + ":999\"",
                "{\"name\":\"Changed\",\"description\":\"\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    assertThat(jdbc.queryForList("SELECT * FROM projects")).isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "missing,401",
    "wrong,401",
    "origin,403",
    "media,415"
  })
  void s9_s11_boundaryRejectsUntrustedRequestsWithoutWrites(String defect, int expected)
      throws Exception {
    var id = seed();
    var before = jdbc.queryForList("SELECT * FROM projects");
    var request =
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                "/api/v1/projects/" + id)
            .header("If-Match", "\"" + id + ":0\"")
            .contentType(defect.equals("media") ? "text/plain" : "application/json")
            .content("{\"name\":\"Changed\",\"description\":\"\"}");
    if (!defect.equals("missing") && !defect.equals("wrong"))
      request.with(user("persona-a")).with(csrf().asHeader());
    if (defect.equals("origin")) request.header("Origin", "https://evil.example");
    mvc.perform(request)
        .andExpect(status().is(expected))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    assertThat(jdbc.queryForList("SELECT * FROM projects")).isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @Test
  void s2_noopWaitsForConcurrentWriterAndRejectsItsObsoleteTag() throws Exception {
    var id = seed();
    var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
    try (var connection = jdbc.getDataSource().getConnection()) {
      connection.setAutoCommit(false);
      try (var statement =
          connection.prepareStatement(
              "UPDATE projects SET name='Concurrent',version=1 WHERE id=?")) {
        statement.setObject(1, id);
        assertThat(statement.executeUpdate()).isEqualTo(1);
      }
      var started = new java.util.concurrent.CountDownLatch(1);
      var response =
          executor.submit(
              () -> {
                started.countDown();
                return mvc.perform(
                        change(
                            id,
                            "\"" + id + ":0\"",
                            "{\"name\":\"Original\",\"description\":\"Description\"}"))
                    .andReturn()
                    .getResponse();
              });
      assertThat(started.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () -> response.get(200, java.util.concurrent.TimeUnit.MILLISECONDS))
          .isInstanceOf(java.util.concurrent.TimeoutException.class);
      connection.commit();
      assertThat(response.get(10, java.util.concurrent.TimeUnit.SECONDS).getStatus())
          .isEqualTo(412);
      assertThat(jdbc.queryForMap("SELECT name,version FROM projects WHERE id=?", id))
          .containsEntry("name", "Concurrent")
          .containsEntry("version", 1L);
      assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
    } finally {
      executor.shutdownNow();
      assertThat(executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
    }
  }

  @Test
  void s24_internalFailureHasSafeProblemAndCorrelationId() throws Exception {
    var id = seed();
    var before = jdbc.queryForList("SELECT * FROM projects");
    org.mockito.Mockito.doThrow(new IllegalStateException("SQL secret-password other-owner"))
        .when(editing)
        .update(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());
    var response =
        mvc.perform(change(id, "\"" + id + ":0\"", "{\"name\":\"Changed\",\"description\":\"\"}"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
            .andExpect(jsonPath("$.correlationId").isString())
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(response)
        .doesNotContain("SQL", "secret-password", "other-owner", "IllegalStateException");
    assertThat(jdbc.queryForList("SELECT * FROM projects")).isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @Test
  void s1_detailTagCannotReadLaterVersionThanItsBody() throws Exception {
    var id = seed();
    org.mockito.Mockito.doAnswer(
            invocation -> {
              var snapshot = invocation.callRealMethod();
              jdbc.update("UPDATE projects SET name='Later',version=1 WHERE id=?", id);
              return snapshot;
            })
        .when(queries)
        .find("persona-a", id);
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/v1/projects/" + id)
                .with(user("persona-a"))
                .with(csrf().asHeader()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Original"))
        .andExpect(header().string("ETag", "\"" + id + ":0\""));
    assertThat(jdbc.queryForMap("SELECT name,version FROM projects WHERE id=?", id))
        .containsEntry("name", "Later")
        .containsEntry("version", 1L);
  }
}
