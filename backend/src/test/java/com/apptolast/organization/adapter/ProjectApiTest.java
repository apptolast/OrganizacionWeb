package com.apptolast.organization.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class ProjectApiTest {
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
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;

  @BeforeEach
  void clear() {
    jdbc.execute("TRUNCATE task_status_history, tasks, outbox_events, projects");
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "name,42",
    "name,true",
    "name,[]",
    "name,{}",
    "description,42",
    "description,true",
    "description,[]",
    "description,{}"
  })
  void s5_s10_rejectsNonStringTypes(String field, String value) throws Exception {
    String body =
        field.equals("name")
            ? "{\"name\":" + value + "}"
            : "{\"name\":\"Idea\",\"description\":" + value + "}";
    mvc.perform(
            post("/api/v1/projects")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value(field))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_TYPE"));
    assertEmpty();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"ownerId", "status", "extra"})
  void s12_rejectsUnknownFields(String field) throws Exception {
    mvc.perform(
            post("/api/v1/projects")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"name\":\"Idea\",\"" + field + "\":\"persona-b\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value(field))
        .andExpect(jsonPath("$.errors[0].code").value("UNKNOWN_FIELD"));
    assertEmpty();
  }

  @Test
  void s14_rejectsTrailingJsonDocumentWithoutWrites() throws Exception {
    mvc.perform(
            post("/api/v1/projects")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"name\":\"Idea\"}{\"extra\":true}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    assertEmpty();
  }

  @Test
  void s14_malformedJsonProblem() throws Exception {
    mvc.perform(
            post("/api/v1/projects")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"name\":"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    assertEmpty();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"projects", "outbox_events"})
  void s17_rollsBackEitherFailedWrite(String table) throws Exception {
    jdbc.execute(
        "CREATE OR REPLACE FUNCTION test_fail_write() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'private storage details' USING ERRCODE = '08006'; END; $$");
    jdbc.execute(
        "CREATE TRIGGER test_failure BEFORE INSERT ON "
            + table
            + " FOR EACH ROW EXECUTE FUNCTION test_fail_write()");
    try {
      mvc.perform(
              post("/api/v1/projects")
                  .with(user("persona-a"))
                  .with(csrf().asHeader())
                  .contentType("application/json")
                  .content("{\"name\":\"Idea\"}"))
          .andExpect(status().isServiceUnavailable())
          .andExpect(content().contentType("application/problem+json"))
          .andExpect(jsonPath("$.status").value(503))
          .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"));
      assertEmpty();
    } finally {
      jdbc.execute("DROP TRIGGER test_failure ON " + table);
      jdbc.execute("DROP FUNCTION test_fail_write()");
    }
  }

  @Test
  void s18_internalFailureRollsBackAndHidesDetails() throws Exception {
    jdbc.execute(
        "CREATE OR REPLACE FUNCTION test_fail_internal() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'password=private-secret'; END; $$");
    jdbc.execute(
        "CREATE TRIGGER test_failure BEFORE INSERT ON outbox_events FOR EACH ROW EXECUTE FUNCTION test_fail_internal()");
    try {
      var response =
          mvc.perform(
                  post("/api/v1/projects")
                      .with(user("persona-a"))
                      .with(csrf().asHeader())
                      .contentType("application/json")
                      .content("{\"name\":\"Idea\"}"))
              .andExpect(status().isInternalServerError())
              .andExpect(content().contentType("application/problem+json"))
              .andExpect(jsonPath("$.status").value(500))
              .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
              .andExpect(jsonPath("$.correlationId").isString())
              .andReturn()
              .getResponse();
      assertThat(response.getContentAsString())
          .doesNotContain("private-secret", "SQLException", "at com.", "INSERT");
      assertEmpty();
    } finally {
      jdbc.execute("DROP TRIGGER test_failure ON outbox_events");
      jdbc.execute("DROP FUNCTION test_fail_internal()");
    }
  }

  @Test
  void s15_rejectsUnsupportedContentType() throws Exception {
    mvc.perform(
            post("/api/v1/projects")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("text/plain")
                .content("Idea"))
        .andExpect(status().isUnsupportedMediaType());
    assertEmpty();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void s13_requiresVerifiedIdentity(boolean invalid) throws Exception {
    var request =
        post("/api/v1/projects")
            .header("X-Owner-Id", "persona-a")
            .contentType("application/json")
            .content("{\"name\":\"Idea\"}");
    if (invalid) request.with(httpBasic("persona-a", "incorrect"));
    mvc.perform(request)
        .andExpect(status().isUnauthorized())
        .andExpect(header().doesNotExist("WWW-Authenticate"))
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    assertEmpty();
  }

  @Test
  void sessionReportsIdentityWithoutBasicChallenge() throws Exception {
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/session"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authenticated").value(false))
        .andExpect(header().doesNotExist("WWW-Authenticate"));
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/session")
                .with(user("persona-a")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authenticated").value(true))
        .andExpect(jsonPath("$.username").value("persona-a"));
    assertEmpty();
  }

  @Test
  void rejectsCrossOriginAuthenticatedWrites() throws Exception {
    mvc.perform(
            post("/api/v1/projects")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .header("Origin", "https://hostile.example")
                .contentType("application/json")
                .content("{\"name\":\"Idea\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("UNTRUSTED_ORIGIN"));
    assertEmpty();
  }

  @Test
  void s11_duplicateNamesCreateDistinctUnchangedProjects() throws Exception {
    var first = create("{\"name\":\"Idea\"}");
    var snapshot = jdbc.queryForMap("SELECT * FROM projects");
    var second = create("{\"name\":\"Idea\"}");
    assertThat(first.get("id").asText()).isNotEqualTo(second.get("id").asText());
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM projects WHERE owner_id='persona-a' AND name='Idea'",
                Long.class))
        .isEqualTo(2L);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(2L);
    assertThat(
            jdbc.queryForMap(
                "SELECT * FROM projects WHERE id=?",
                java.util.UUID.fromString(first.get("id").asText())))
        .isEqualTo(snapshot);
  }

  private com.fasterxml.jackson.databind.JsonNode create(String body) throws Exception {
    var response =
        mvc.perform(
                post("/api/v1/projects")
                    .with(user("persona-a"))
                    .with(csrf().asHeader())
                    .header("Origin", "https://organization.example")
                    .contentType("application/json")
                    .content(body))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();
    return json.readTree(response.getContentAsString());
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"{}", "{\"name\":null}", "{\"name\":\" \u00a0\u2003\"}"})
  void s5_missingNullAndUnicodeBlankAreRequired(String body) throws Exception {
    mvc.perform(
            post("/api/v1/projects")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].code").value("REQUIRED"));
    assertEmpty();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "{\"name\":\"Idea\"}",
        "{\"name\":\"Idea\",\"description\":null}",
        "{\"name\":\"Idea\",\"description\":\"\"}"
      })
  void s7_optionalDescriptionNormalizesInResponseAndStorage(String body) throws Exception {
    assertThat(create(body).get("description").asText()).isEmpty();
    assertThat(jdbc.queryForObject("SELECT description FROM projects", String.class)).isEmpty();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "name,a,1",
    "name,a,120",
    "name,🚀,1",
    "name,🚀,120",
    "description,a,4000",
    "description,🚀,4000"
  })
  void s2_s8_persistsInclusiveUnicodeLimits(String field, String character, int count)
      throws Exception {
    String value = character.repeat(count);
    var body = json.createObjectNode().put("name", "Idea").put(field, value);
    assertThat(create(body.toString()).get(field).asText()).isEqualTo(value);
    assertThat(jdbc.queryForObject("SELECT " + field + " FROM projects", String.class))
        .isEqualTo(value);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "name,a,121",
    "name,🚀,121",
    "description,a,4001",
    "description,🚀,4001"
  })
  void s6_s10_rejectsOverLimitWithoutWrites(String field, String character, int count)
      throws Exception {
    String value = character.repeat(count);
    var body =
        json.createObjectNode()
            .put("name", "Idea")
            .put(field, field.equals("name") ? " " + value + " " : value);
    mvc.perform(
            post("/api/v1/projects")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value(field))
        .andExpect(jsonPath("$.errors[0].code").value("TOO_LONG"));
    assertEmpty();
  }

  @Test
  void s3_s4_s9_preservesTextInStorage() throws Exception {
    String name = " \u00a0\u2003Mi  Proyecto Ae\u0301\u2003\u00a0 ";
    String description = "  Primera\nSegunda  ";
    var response =
        create(
            json.createObjectNode().put("name", name).put("description", description).toString());
    assertThat(response.get("name").asText()).isEqualTo("Mi  Proyecto Ae\u0301");
    assertThat(jdbc.queryForObject("SELECT name FROM projects", String.class))
        .isEqualTo("Mi  Proyecto Ae\u0301");
    assertThat(jdbc.queryForObject("SELECT description FROM projects", String.class))
        .isEqualTo(description);
  }

  @Test
  void s19_brokerAbsentDoesNotPreventPendingDurableEvent() throws Exception {
    // No broker is started or configured: this slice only writes the outbox.
    create("{\"name\":\"Idea\"}");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM projects", Long.class)).isEqualTo(1L);
    assertThat(jdbc.queryForObject("SELECT status FROM outbox_events", String.class))
        .isEqualTo("pending");
  }

  @Test
  void s5_s21_validationUsesSpanishProblemDetails() throws Exception {
    mvc.perform(
            post("/api/v1/projects")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"name\":\"\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(jsonPath("$.type").isString())
        .andExpect(jsonPath("$.title").value("Revisa los campos indicados."))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("name"))
        .andExpect(jsonPath("$.errors[0].code").value("REQUIRED"));
    assertEmpty();
  }

  private void assertEmpty() {
    assertThat(jdbc.queryForObject("SELECT count(*) FROM projects", Long.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @Test
  void s1_s16_commitsBeforeReturningCreated() throws Exception {
    var response =
        mvc.perform(
                post("/api/v1/projects")
                    .with(user("persona-a"))
                    .with(csrf().asHeader())
                    .contentType("application/json")
                    .content("{\"name\":\"  Idea  \",\"description\":\"Contenido privado\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Idea"))
            .andExpect(jsonPath("$.ownerId").value("persona-a"))
            .andExpect(jsonPath("$.description").value("Contenido privado"))
            .andExpect(jsonPath("$.status").value("idea"))
            .andExpect(jsonPath("$.createdAt").value("2026-09-05T12:00:00Z"))
            .andExpect(jsonPath("$.updatedAt").value("2026-09-05T12:00:00Z"))
            .andReturn()
            .getResponse();
    String id = json.readTree(response.getContentAsString()).get("id").asText();
    assertThat(response.getHeader("Location")).isEqualTo("/api/v1/projects/" + id);
    var project = jdbc.queryForMap("SELECT * FROM projects");
    assertThat(project.get("id").toString()).isEqualTo(id);
    assertThat(project.get("owner_id")).isEqualTo("persona-a");
    assertThat(project.get("name")).isEqualTo("Idea");
    assertThat(project.get("description")).isEqualTo("Contenido privado");
    var event = jdbc.queryForMap("SELECT * FROM outbox_events");
    assertThat(event.get("aggregate_id").toString()).isEqualTo(id);
    assertThat(event.get("event_id")).isNotNull().isNotEqualTo(project.get("id"));
    assertThat(event.get("event_type")).isEqualTo("ProjectCreated.v1");
    assertThat(event.get("schema_version")).isEqualTo(1);
    assertThat(event.get("occurred_at")).isEqualTo(project.get("created_at"));
    assertThat(event.get("status")).isEqualTo("pending");
    var payload = json.readTree(event.get("payload").toString());
    assertThat(payload.get("name").asText()).isEqualTo("Idea");
    assertThat(payload.has("description")).isFalse();
  }
}
