package com.apptolast.organization.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
class ReadProjectsApiTest {
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

  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;

  @BeforeEach
  void clear() {
    jdbc.execute("TRUNCATE outbox_events, projects");
  }

  @Test
  void s2_emptyListIsPrivateAndHasNoCursor() throws Exception {
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/v1/projects")
                .with(user("persona-a"))
                .with(csrf().asHeader()))
        .andExpect(status().isOk())
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(jsonPath("$.items").isEmpty())
        .andExpect(jsonPath("$.nextCursor").value(org.hamcrest.Matchers.nullValue()));
  }

  java.util.UUID seed(String owner, String name) {
    var id = java.util.UUID.randomUUID();
    var now = java.sql.Timestamp.from(java.time.Instant.parse("2026-09-05T12:00:00Z"));
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,?,?,?,'idea',?,?)",
        id,
        owner,
        name,
        "Description <script> & 😀",
        now,
        now);
    return id;
  }

  @Test
  void s1_readsOnlyOwnerAndOnlySummaryFields() throws Exception {
    var id = seed("persona-a", "Own 😀");
    seed("persona-b", "Foreign private");
    var response =
        mvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/api/v1/projects")
                    .with(user("persona-a"))
                    .with(csrf().asHeader()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    var body = json.readTree(response.getContentAsString());
    assertThat(body.get("items").size()).isEqualTo(1);
    var item = body.get("items").get(0);
    assertThat(item.size()).isEqualTo(5);
    assertThat(item.get("id").asText()).isEqualTo(id.toString());
    assertThat(item.get("name").asText()).isEqualTo("Own 😀");
    assertThat(item.get("status").asText()).isEqualTo("idea");
    assertThat(item.get("createdAt").asText()).isEqualTo("2026-09-05T12:00:00Z");
    assertThat(item.get("updatedAt").asText()).isEqualTo("2026-09-05T12:00:00Z");
  }

  @Test
  void s9_detailPreservesOriginalOwnedValues() throws Exception {
    var id = seed("persona-a", "Own 😀");
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/v1/projects/" + id)
                .with(user("persona-a"))
                .with(csrf().asHeader()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.ownerId").value("persona-a"))
        .andExpect(jsonPath("$.description").value("Description <script> & 😀"))
        .andExpect(jsonPath("$.name").value("Own 😀"))
        .andExpect(jsonPath("$.status").value("idea"))
        .andExpect(jsonPath("$.createdAt").value("2026-09-05T12:00:00Z"))
        .andExpect(jsonPath("$.updatedAt").value("2026-09-05T12:00:00Z"))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void s10_foreignAndMissingDetailsAreIndistinguishable(boolean foreign) throws Exception {
    var id = foreign ? seed("persona-b", "Private") : java.util.UUID.randomUUID();
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/v1/projects/" + id)
                .with(user("persona-a"))
                .with(csrf().asHeader()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("PROJECT_NOT_FOUND"))
        .andExpect(jsonPath("$.title").value("Proyecto no encontrado"))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
  }

  @Test
  void s4_ordersAndReturnsTwentyWithCursor() throws Exception {
    for (int i = 0; i < 21; i++) seed("persona-a", "Idea " + i);
    var expected =
        jdbc.queryForList(
            "SELECT id::text FROM projects ORDER BY created_at DESC,id DESC", String.class);
    var body =
        json.readTree(
            mvc.perform(
                    org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                            "/api/v1/projects")
                        .with(user("persona-a"))
                        .with(csrf().asHeader()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    var actual = new java.util.ArrayList<String>();
    body.get("items").forEach(item -> actual.add(item.get("id").asText()));
    assertThat(actual).containsExactlyElementsOf(expected.subList(0, 20));
    assertThat(body.get("nextCursor").isTextual()).isTrue();
    var cursor =
        json.readTree(java.util.Base64.getUrlDecoder().decode(body.get("nextCursor").asText()));
    assertThat(cursor.get("id").asText()).isEqualTo(expected.get(19));
    assertThat(cursor.get("createdAt").asText()).isEqualTo("2026-09-05T12:00:00Z");
    assertThat(cursor.size()).isEqualTo(2);
  }

  @Test
  void s5_s6_continuesAfterCursorWithoutRepeatingOrInsertingNewerRows() throws Exception {
    for (int i = 0; i < 21; i++) seed("persona-a", "Idea " + i);
    var expected =
        jdbc.queryForList(
            "SELECT id::text FROM projects ORDER BY created_at DESC,id DESC", String.class);
    String first =
        mvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/api/v1/projects")
                    .with(user("persona-a"))
                    .with(csrf().asHeader()))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String cursor = json.readTree(first).get("nextCursor").asText();
    var newer = seed("persona-a", "Newer");
    jdbc.update(
        "UPDATE projects SET created_at=created_at+interval '1 second', updated_at=updated_at+interval '1 second' WHERE id=?",
        newer);
    var body =
        json.readTree(
            mvc.perform(
                    org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                            "/api/v1/projects")
                        .param("cursor", cursor)
                        .with(user("persona-a"))
                        .with(csrf().asHeader()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(body.get("items").size()).isEqualTo(1);
    assertThat(body.get("items").get(0).get("id").asText()).isEqualTo(expected.get(20));
    assertThat(body.get("nextCursor").isNull()).isTrue();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "encoding",
        "missing",
        "date",
        "id",
        "empty",
        "repeated",
        "query",
        "type",
        "extra",
        "decimalTimestamp",
        "padding",
        "duplicateJson"
      })
  void s7_invalidPaginationReturnsFieldError(String defect) throws Exception {
    String data =
        "{\"createdAt\":\"2026-09-05T12:00:00Z\",\"id\":\"00000000-0000-0000-0000-000000000001\"}";
    switch (defect) {
      case "missing" -> data = "{}";
      case "date" -> data = data.replace("2026-09-05T12:00:00Z", "invalid");
      case "id" -> data = data.replace("00000000-0000-0000-0000-000000000001", "bad");
      case "type" -> data = data.replace("\"2026-09-05T12:00:00Z\"", "42");
      case "extra" -> data = data.replace("}", ",\"ownerId\":\"other\"}");
      case "decimalTimestamp" -> data = data.replace("12:00:00Z", "12:00:00.000000001Z");
      case "duplicateJson" ->
          data = data.replace("}", ",\"id\":\"00000000-0000-0000-0000-000000000001\"}");
    }
    String cursor =
        java.util.Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    if (defect.equals("encoding")) cursor = "!invalid";
    if (defect.equals("empty")) cursor = "";
    if (defect.equals("padding")) cursor += "=";
    var request =
        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/projects")
            .param("cursor", cursor)
            .with(user("persona-a"))
            .with(csrf().asHeader());
    if (defect.equals("repeated")) request.param("cursor", cursor);
    if (defect.equals("query")) request.param("limit", "10");
    mvc.perform(request)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(
            jsonPath("$.errors[0].field").value(defect.equals("query") ? "query" : "cursor"));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"invalid", "1-1-1-1-1"})
  void s11_invalidDetailIdentifierHasValidationError(String id) throws Exception {
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/v1/projects/" + id)
                .with(user("persona-a"))
                .with(csrf().asHeader()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("id"));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void s12_storageReadFailureIsUnavailableNotEmpty(boolean detail) throws Exception {
    var id = seed("persona-a", "Own");
    jdbc.execute("ALTER TABLE projects RENAME TO projects_unavailable");
    try {
      mvc.perform(
              org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                      "/api/v1/projects" + (detail ? "/" + id : ""))
                  .with(user("persona-a"))
                  .with(csrf().asHeader()))
          .andExpect(status().isServiceUnavailable())
          .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"))
          .andExpect(jsonPath("$.items").doesNotExist())
          .andExpect(
              header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    } finally {
      jdbc.execute("ALTER TABLE projects_unavailable RENAME TO projects");
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void s3_unauthenticatedReadsArePrivate(boolean detail) throws Exception {
    var id = seed("persona-a", "Private");
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                "/api/v1/projects" + (detail ? "/" + id : "")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Private"))));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void s28_unexpectedReadFailureHasOnlySafeProblem(boolean detail) throws Exception {
    var id = seed("persona-a", "Private");
    if (detail)
      org.mockito.Mockito.doThrow(new IllegalStateException("secret SQL password"))
          .when(queries)
          .find("persona-a", id);
    else
      org.mockito.Mockito.doThrow(new IllegalStateException("secret SQL password"))
          .when(queries)
          .list(
              org.mockito.ArgumentMatchers.eq("persona-a"),
              org.mockito.ArgumentMatchers.isNull(),
              org.mockito.ArgumentMatchers.eq(21));
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/v1/projects" + (detail ? "/" + id : ""))
                .with(user("persona-a"))
                .with(csrf().asHeader()))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
        .andExpect(jsonPath("$.correlationId").isString())
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("secret SQL password"))));
  }

  @Test
  void s8_foreignCursorOnlyPositionsOwnCollection() throws Exception {
    var own = seed("persona-a", "Own");
    var foreign = seed("persona-b", "Private");
    String cursor =
        java.util.Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                ("{\"createdAt\":\"2026-09-05T12:00:01Z\",\"id\":\"" + foreign + "\"}")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/v1/projects")
                .param("cursor", cursor)
                .with(user("persona-a"))
                .with(csrf().asHeader()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].id").value(own.toString()))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Private"))));
  }

  @Test
  void s13_readsLeaveProjectAndOutboxRowsUnchanged() throws Exception {
    var response =
        mvc.perform(
                post("/api/v1/projects")
                    .contentType("application/json")
                    .content("{\"name\":\"Immutable read\"}")
                    .with(user("persona-a"))
                    .with(csrf().asHeader()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();
    String id = json.readTree(response.getContentAsString()).get("id").asText();
    var projects = jdbc.queryForList("SELECT * FROM projects");
    var outbox = jdbc.queryForList("SELECT * FROM outbox_events");
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/v1/projects")
                .with(user("persona-a"))
                .with(csrf().asHeader()))
        .andExpect(status().isOk());
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/v1/projects/" + id)
                .with(user("persona-a"))
                .with(csrf().asHeader()))
        .andExpect(status().isOk());
    assertThat(jdbc.queryForList("SELECT * FROM projects")).isEqualTo(projects);
    assertThat(jdbc.queryForList("SELECT * FROM outbox_events")).isEqualTo(outbox);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "+1000000000-12-31T23:59:59Z",
        "+400000-01-01T00:00:00Z",
        "-10000-01-01T00:00:00Z",
        "-4713-11-23T23:59:59.999999Z",
        "+294277-01-01T00:00:00Z"
      })
  void s7_rejectsCursorDateOutsidePostgresStorageRange(String timestamp) throws Exception {
    String cursor =
        java.util.Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                ("{\"createdAt\":\""
                        + timestamp
                        + "\",\"id\":\"00000000-0000-0000-0000-000000000001\"}")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/v1/projects")
                .param("cursor", cursor)
                .with(user("persona-a"))
                .with(csrf().asHeader()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("cursor"));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"-4713-11-24T00:00:00Z", "+294276-12-31T23:59:59.999999Z"})
  void s7_acceptsFinitePostgresCursorBoundaries(String timestamp) throws Exception {
    String cursor =
        java.util.Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                ("{\"createdAt\":\""
                        + timestamp
                        + "\",\"id\":\"00000000-0000-0000-0000-000000000001\"}")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                    "/api/v1/projects")
                .param("cursor", cursor)
                .with(user("persona-a"))
                .with(csrf().asHeader()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isEmpty());
  }
}
