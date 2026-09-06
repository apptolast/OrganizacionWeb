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
class TaskHistoryApiTest {
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
    jdbc.execute("TRUNCATE block_changes,block_projections,planned_blocks, task_status_history, tasks, outbox_events, projects");
    project = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'persona-a','P','','idea',now(),now())",
        project);
    parent = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?, ?, 'Parent', '', 'pending', now(), now())",
        parent,
        project);
  }

  String path() {
    return "/api/v1/projects/" + project + "/tasks/" + parent + "/history";
  }

  @Test
  void s10_readsConfirmedEmptyHistory() throws Exception {
    var response =
        mvc.perform(get(path()).with(user("persona-a")))
            .andExpect(status().isOk())
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString()))
        .isEqualTo(json.readTree("{\"items\":[],\"nextCursor\":null}"));
  }

  UUID transition(long version) {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO task_status_history(id,project_id,task_id,task_version,from_status,to_status,occurred_at) VALUES (?,?,?,?,'pending','completed','2026-09-06T00:00:00.123456Z')",
        id,
        project,
        parent,
        version);
    return id;
  }

  @Test
  void s10_readsExactFieldsOrderedByVersionWithEqualDatesAndHoles() throws Exception {
    var first = transition(1);
    var second = transition(3);
    var third = transition(5);
    jdbc.execute("DELETE FROM outbox_events");
    var result =
        json.readTree(
            mvc.perform(get(path()).with(user("persona-a")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    var items = result.get("items");
    assertThat(items.size()).isEqualTo(3);
    assertThat(items.get(0).get("id").asText()).isEqualTo(third.toString());
    assertThat(items.get(1).get("id").asText()).isEqualTo(second.toString());
    assertThat(items.get(2).get("id").asText()).isEqualTo(first.toString());
    for (var item : items) {
      assertThat(item.size()).isEqualTo(4);
      assertThat(item.get("fromStatus").asText()).isEqualTo("pending");
      assertThat(item.get("toStatus").asText()).isEqualTo("completed");
      assertThat(item.get("occurredAt").asText()).isEqualTo("2026-09-06T00:00:00.123456Z");
    }
    assertThat(result.get("nextCursor").isNull()).isTrue();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"foreign", "missing-task", "wrong-project", "missing-project"})
  void s14_hidesResources(String kind) throws Exception {
    transition(1);
    var baseline =
        json.readTree(
            mvc.perform(
                    get(path().replace(parent.toString(), UUID.randomUUID().toString()))
                        .with(user("persona-a")))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse()
                .getContentAsString());
    String target = path();
    if (kind.equals("foreign"))
      jdbc.update("UPDATE projects SET owner_id='other' WHERE id=?", project);
    if (kind.equals("missing-task"))
      target = path().replace(parent.toString(), UUID.randomUUID().toString());
    if (kind.equals("missing-project"))
      target = path().replace(project.toString(), UUID.randomUUID().toString());
    if (kind.equals("wrong-project")) {
      var own = UUID.randomUUID();
      jdbc.update(
          "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'persona-a','Other','','idea',now(),now())",
          own);
      target = path().replace(project.toString(), own.toString());
    }
    var response =
        mvc.perform(get(target).with(user("persona-a")))
            .andExpect(status().isNotFound())
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString())).isEqualTo(baseline);
  }

  @Test
  void s18_storageFailureIsUnavailableNeverEmpty() throws Exception {
    jdbc.execute("ALTER TABLE task_status_history RENAME TO history_unavailable");
    try {
      mvc.perform(get(path()).with(user("persona-a")))
          .andExpect(status().isServiceUnavailable())
          .andExpect(jsonPath("code").value("STORAGE_UNAVAILABLE"))
          .andExpect(
              header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    } finally {
      jdbc.execute("ALTER TABLE history_unavailable RENAME TO task_status_history");
    }
  }

  @Test
  void s12_s29_continuesTwentyRowsByVersionDespiteNewInsert() throws Exception {
    for (int n = 1; n <= 21; n++) transition(n * 2);
    var first =
        json.readTree(
            mvc.perform(get(path()).with(user("persona-a")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(first.get("items").size()).isEqualTo(20);
    var cursor = first.get("nextCursor").textValue();
    assertThat(cursor).isNotBlank();
    var decoded = json.readTree(java.util.Base64.getUrlDecoder().decode(cursor));
    assertThat(decoded.size()).isEqualTo(3);
    assertThat(decoded.get("projectId").asText()).isEqualTo(project.toString());
    assertThat(decoded.get("taskId").asText()).isEqualTo(parent.toString());
    assertThat(decoded.get("taskVersion").longValue()).isEqualTo(4);
    assertThat(cursor).doesNotContain("=");
    transition(44);
    var next =
        json.readTree(
            mvc.perform(get(path()).param("cursor", cursor).with(user("persona-a")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(next.get("items").size()).isEqualTo(1);
    assertThat(next.get("nextCursor").isNull()).isTrue();
    assertThat(next.get("items").get(0).get("id").asText())
        .isEqualTo(
            jdbc.queryForObject(
                "SELECT id::text FROM task_status_history WHERE task_version=2", String.class));
    var same =
        json.readTree(
            mvc.perform(
                    get(path()
                            .toUpperCase()
                            .replace("/API/V1/PROJECTS/", "/api/v1/projects/")
                            .replace("/TASKS/", "/tasks/")
                            .replace("/HISTORY", "/history"))
                        .param("cursor", cursor)
                        .with(user("persona-a")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(same).isEqualTo(next);
  }

  String encode(String raw) {
    return java.util.Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  String validCursorJson() {
    return "{\"projectId\":\"" + project + "\",\"taskId\":\"" + parent + "\",\"taskVersion\":1}";
  }

  static java.util.stream.Stream<String> cursorDefects() {
    var cases =
        new java.util.ArrayList<>(
            java.util.List.of(
                "other-task",
                "other-project",
                "flat",
                "subtasks",
                "empty",
                "base64",
                "padding",
                "noncanonical",
                "truncated",
                "trailing",
                "extra",
                "project-type",
                "task-type",
                "repeat",
                "unknown"));
    for (String key : java.util.List.of("projectId", "taskId", "taskVersion")) {
      cases.add("missing:" + key);
      cases.add("duplicate:" + key);
    }
    for (String root : java.util.List.of("null", "[]", "\"text\"", "1", "true"))
      cases.add("root:" + root);
    for (String version :
        java.util.List.of("0", "-1", "1.5", "9223372036854775808", "\"1\"", "true", "null"))
      cases.add("version:" + version);
    return cases.stream();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.MethodSource("cursorDefects")
  void s13_rejectsEachCursorBoundary(String defect) throws Exception {
    String raw = validCursorJson();
    var body = json.readTree(raw).deepCopy();
    if (defect.equals("other-task"))
      ((com.fasterxml.jackson.databind.node.ObjectNode) body)
          .put("taskId", UUID.randomUUID().toString());
    if (defect.equals("other-project"))
      ((com.fasterxml.jackson.databind.node.ObjectNode) body)
          .put("projectId", UUID.randomUUID().toString());
    if (defect.equals("project-type"))
      ((com.fasterxml.jackson.databind.node.ObjectNode) body).put("projectId", 1);
    if (defect.equals("task-type"))
      ((com.fasterxml.jackson.databind.node.ObjectNode) body).putNull("taskId");
    if (defect.equals("extra"))
      ((com.fasterxml.jackson.databind.node.ObjectNode) body).put("extra", 1);
    if (defect.startsWith("missing:"))
      ((com.fasterxml.jackson.databind.node.ObjectNode) body).remove(defect.substring(8));
    raw = body.toString();
    if (defect.startsWith("duplicate:")) {
      var key = defect.substring(10);
      raw = raw.substring(0, raw.length() - 1) + ",\"" + key + "\":" + body.get(key) + "}";
    }
    if (defect.startsWith("root:")) raw = defect.substring(5);
    if (defect.startsWith("version:"))
      raw = raw.replace("\"taskVersion\":1", "\"taskVersion\":" + defect.substring(8));
    if (defect.equals("flat"))
      raw =
          "{\"projectId\":\""
              + project
              + "\",\"createdAt\":\"2026-01-01T00:00:00Z\",\"id\":\""
              + parent
              + "\"}";
    if (defect.equals("subtasks"))
      raw =
          "{\"projectId\":\""
              + project
              + "\",\"parentTaskId\":\""
              + parent
              + "\",\"createdAt\":\"2026-01-01T00:00:00Z\",\"id\":\""
              + parent
              + "\"}";
    if (defect.equals("truncated")) raw = raw.substring(0, raw.length() - 1);
    if (defect.equals("trailing")) raw += " {}";
    String cursor = encode(raw);
    if (defect.equals("empty")) cursor = "";
    if (defect.equals("base64")) cursor = "!";
    if (defect.equals("padding")) cursor += "=";
    if (defect.equals("noncanonical")) cursor = "e31";
    var request = get(path()).param("cursor", cursor).with(user("persona-a"));
    if (defect.equals("repeat")) request.param("cursor", cursor);
    if (defect.equals("unknown")) request.param("limit", "1");
    mvc.perform(request)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("errors[0].field").value(defect.equals("unknown") ? "query" : "cursor"))
        .andExpect(jsonPath("errors[0].code").value("INVALID_VALUE"))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"projectId", "id"})
  void s30_pathValidationPrecedesCursor(String field) throws Exception {
    String target =
        path()
            .replace(
                field.equals("projectId") ? project.toString() : parent.toString(), "1-1-1-1-1");
    mvc.perform(get(target).param("cursor", "!").with(user("persona-a")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("errors[0].field").value(field))
        .andExpect(jsonPath("errors[0].code").value("INVALID_FORMAT"));
  }

  @Autowired org.springframework.session.SessionRepository sessions;

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void s14_s31_requiresSessionIncludingPersistedExpiry(boolean expired) throws Exception {
    var request = get(path());
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
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("code").value("UNAUTHENTICATED"))
        .andExpect(header().doesNotExist("WWW-Authenticate"))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(longs = {1, 9223372036854775807L})
  void s13_acceptsPositiveLongBoundaries(long version) throws Exception {
    transition(1);
    String cursor =
        encode(validCursorJson().replace("\"taskVersion\":1", "\"taskVersion\":" + version));
    var body =
        json.readTree(
            mvc.perform(get(path()).param("cursor", cursor).with(user("persona-a")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(body.get("items").size()).isEqualTo(version == 1 ? 0 : 1);
    assertThat(body.get("nextCursor").isNull()).isTrue();
  }

  @Test
  void s10_preservesReopeningDirectionAndFiltersAnotherTask() throws Exception {
    var own = transition(1);
    jdbc.update(
        "UPDATE task_status_history SET from_status='completed',to_status='pending' WHERE id=?",
        own);
    var other = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,'Other','','pending',now(),now())",
        other,
        project);
    jdbc.update(
        "INSERT INTO task_status_history(id,project_id,task_id,task_version,from_status,to_status,occurred_at) VALUES (?,?,?,2,'pending','completed',now())",
        UUID.randomUUID(),
        project,
        other);
    var body =
        json.readTree(
            mvc.perform(get(path()).with(user("persona-a")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(body.get("items").size()).isEqualTo(1);
    assertThat(body.get("items").get(0).get("id").asText()).isEqualTo(own.toString());
    assertThat(body.get("items").get(0).get("fromStatus").asText()).isEqualTo("completed");
    assertThat(body.get("items").get(0).get("toStatus").asText()).isEqualTo("pending");
  }
}
