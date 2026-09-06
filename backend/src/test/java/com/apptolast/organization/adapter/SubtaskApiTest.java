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
class SubtaskApiTest {
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
    jdbc.execute(
        "TRUNCATE block_changes,block_projections,planned_blocks, task_status_history, tasks, outbox_events, projects");
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
    return "/api/v1/projects/" + project + "/tasks/" + parent + "/subtasks";
  }

  @Test
  void s1_commitsChildRelationshipAndOnlySubtaskEvent() throws Exception {
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
    assertThat(response.getHeader("Location"))
        .isEqualTo("/api/v1/projects/" + project + "/tasks/" + body.get("id").asText());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM tasks", Long.class)).isEqualTo(2);
    var event =
        json.readTree(jdbc.queryForObject("SELECT payload::text FROM outbox_events", String.class));
    assertThat(event.get("parentTaskId").asText()).isEqualTo(parent.toString());
    assertThat(event.get("type").asText()).isEqualTo("SubtaskCreated.v1");
    assertThat(
            jdbc.queryForObject(
                "SELECT parent_id FROM tasks WHERE id=?",
                UUID.class,
                UUID.fromString(body.get("id").asText())))
        .isEqualTo(parent);
    assertThat(event.size()).isEqualTo(9);
    assertThat(event.get("aggregateId").asText()).isEqualTo(project.toString());
    assertThat(event.get("taskId").asText()).isEqualTo(body.get("id").asText());
    assertThat(event.get("title").asText()).isEqualTo("Preparar portada");
    assertThat(event.get("ownerId").asText()).isEqualTo("persona-a");
    assertThat(event.get("occurredAt").asText()).isEqualTo(body.get("createdAt").asText());
    assertThat(
            jdbc.queryForObject(
                    "SELECT created_at FROM tasks WHERE id='" + body.get("id").asText() + "'",
                    java.sql.Timestamp.class)
                .toInstant())
        .isEqualTo(java.time.Instant.parse(body.get("createdAt").asText()));
  }

  @Test
  void s9_readsConfirmedRoot() throws Exception {
    mvc.perform(
            get("/api/v1/projects/" + project + "/tasks/" + parent + "/parent")
                .with(user("persona-a")))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"parent\":null}", true));
  }

  @Test
  void s11_readsOnlyDirectChildren() throws Exception {
    var child = UUID.randomUUID();
    var sibling = UUID.randomUUID();
    var grandchild = UUID.randomUUID();
    insertChild(child, parent, "Child");
    insertChild(sibling, parent, "Sibling");
    insertChild(grandchild, child, "Grandchild");
    var body =
        json.readTree(
            mvc.perform(get(path()).with(user("persona-a")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(body.size()).isEqualTo(2);
    assertThat(body.get("items")).hasSize(2);
    assertThat(body.get("nextCursor").isNull()).isTrue();
    assertThat(body.get("items").findValuesAsText("id"))
        .containsExactlyInAnyOrder(child.toString(), sibling.toString());
  }

  void insertChild(UUID id, UUID parentId, String title) {
    jdbc.update(
        "INSERT INTO tasks(id,project_id,parent_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,?,?,'','pending',now(),now())",
        id,
        project,
        parentId,
        title);
  }

  @Test
  void s12_s35_pagesStableDirectChildren() throws Exception {
    for (int n = 1; n <= 21; n++) {
      insertChild(new UUID(0, n), parent, "T" + n);
    }
    jdbc.update(
        "UPDATE tasks SET created_at='2026-01-01T00:00:00.123456Z',updated_at='2026-01-01T00:00:00.123456Z' WHERE parent_id=?",
        parent);
    var first =
        json.readTree(
            mvc.perform(get(path()).with(user("persona-a")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(first.get("items")).hasSize(20);
    assertThat(first.get("items").get(0).get("id").asText()).isEqualTo(new UUID(0, 21).toString());
    String cursor = first.get("nextCursor").textValue();
    assertThat(cursor).isNotBlank();
    var fields = json.readTree(java.util.Base64.getUrlDecoder().decode(cursor));
    assertThat(fields.size()).isEqualTo(4);
    assertThat(fields.get("projectId").asText()).isEqualTo(project.toString());
    assertThat(fields.get("parentTaskId").asText()).isEqualTo(parent.toString());
    assertThat(fields.get("id").asText()).isEqualTo(new UUID(0, 2).toString());
    assertThat(fields.get("createdAt").asText()).isEqualTo("2026-01-01T00:00:00.123456Z");
    insertChild(UUID.randomUUID(), parent, "Newer");
    var next =
        json.readTree(
            mvc.perform(get(path()).param("cursor", cursor).with(user("persona-a")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(next.get("items")).hasSize(1);
    assertThat(next.get("items").get(0).get("id").asText()).isEqualTo(new UUID(0, 1).toString());
    assertThat(next.get("nextCursor").isNull()).isTrue();
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
  void s4_inherited_s4_s6_validatesJsonTypes(String body, String field, String code)
      throws Exception {
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
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM tasks WHERE parent_id IS NOT NULL", Long.class))
        .isZero();
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
        "{\"title\":\"T\",\"status\":\"pending\"}|VALIDATION_ERROR",
        "{\"title\":\"T\",\"id\":\"x\"}|VALIDATION_ERROR",
        "{\"title\":\"T\",\"createdAt\":\"x\"}|VALIDATION_ERROR",
        "{\"title\":\"T\",\"unknown\":true}|VALIDATION_ERROR",
        "{\"title\":\"T\",\"parentId\":\"x\"}|VALIDATION_ERROR"
      })
  void s4_s5_inherited_s8_strictBody(String body, String code) throws Exception {
    mvc.perform(
            post(path())
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(code));
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM tasks WHERE parent_id IS NOT NULL", Long.class))
        .isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
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
        "trailing",
        "parent",
        "flat",
        "parentType",
        "projectType",
        "dateType",
        "idType",
        "padding",
        "offset"
      })
  void s13_s14_inherited_s22_rejectsInvalidCursorAndQuery(String defect) throws Exception {
    var position =
        json.createObjectNode()
            .put("projectId", project.toString())
            .put("parentTaskId", parent.toString())
            .put("createdAt", "2026-01-01T00:00:00Z")
            .put("id", UUID.randomUUID().toString());
    switch (defect) {
      case "parent" -> position.put("parentTaskId", UUID.randomUUID().toString());
      case "flat" -> position.remove("parentTaskId");
      case "parentType" -> position.put("parentTaskId", 1);
      case "projectType" -> position.put("projectId", 1);
      case "dateType" -> position.put("createdAt", 1);
      case "idType" -> position.put("id", 1);
      case "offset" -> position.put("createdAt", "2026-01-01T00:00:00+00:00");
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
    if (defect.equals("padding")) cursor += "=";
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
  @org.junit.jupiter.params.provider.MethodSource("inheritedPositiveContent")
  void s4_inherited_s2_s3_s5_s7_positiveContent(
      String raw, String title, String criterion, Integer minutes) throws Exception {
    var body =
        json.readTree(
            mvc.perform(
                    post(path())
                        .with(user("persona-a"))
                        .with(csrf().asHeader())
                        .contentType("application/json")
                        .content(raw))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(body.size()).isEqualTo(8);
    assertThat(body.get("title").asText()).isEqualTo(title);
    assertThat(body.get("completionCriterion").asText()).isEqualTo(criterion);
    if (minutes == null) assertThat(body.get("estimatedMinutes").isNull()).isTrue();
    else assertThat(body.get("estimatedMinutes").intValue()).isEqualTo(minutes);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE event_type='SubtaskCreated.v1'",
                Long.class))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(1);
  }

  static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments>
      inheritedPositiveContent() throws Exception {
    var mapper = new ObjectMapper();
    var cases = new java.util.ArrayList<org.junit.jupiter.params.provider.Arguments>();
    for (var title :
        java.util.List.of(
            "a",
            "a".repeat(160),
            "\uD83D\uDE80".repeat(160),
            " \u00a0\u2003Mi  Ae\u0301 tarea\u2003\u00a0 "))
      cases.add(
          org.junit.jupiter.params.provider.Arguments.of(
              mapper.writeValueAsString(java.util.Map.of("title", title)),
              title.startsWith(" ") ? "Mi  Ae\u0301 tarea" : title,
              "",
              null));
    cases.add(
        org.junit.jupiter.params.provider.Arguments.of(
            mapper.createObjectNode().put("title", "T").toString(), "T", "", null));
    cases.add(
        org.junit.jupiter.params.provider.Arguments.of(
            mapper.createObjectNode().put("title", "T").putNull("completionCriterion").toString(),
            "T",
            "",
            null));
    for (var criterion : java.util.List.of("\uD83D\uDE80".repeat(2000), "  text\nmore  "))
      cases.add(
          org.junit.jupiter.params.provider.Arguments.of(
              mapper.writeValueAsString(
                  java.util.Map.of("title", "T", "completionCriterion", criterion)),
              "T",
              criterion,
              null));
    cases.add(
        org.junit.jupiter.params.provider.Arguments.of(
            mapper.createObjectNode().put("title", "T").putNull("estimatedMinutes").toString(),
            "T",
            "",
            null));
    for (int minutes : new int[] {1, 1440})
      cases.add(
          org.junit.jupiter.params.provider.Arguments.of(
              mapper.writeValueAsString(
                  java.util.Map.of("title", "T", "estimatedMinutes", minutes)),
              "T",
              "",
              minutes));
    return cases.stream();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"emptyTitle", "spaceTitle", "longTitle", "longCriterion", "largeInteger", "media"})
  void s4_inheritedRemainingContentBoundaries(String mode) throws Exception {
    var value = json.createObjectNode().put("title", "T");
    String field = "title", code = "REQUIRED";
    switch (mode) {
      case "emptyTitle" -> value.put("title", "");
      case "spaceTitle" -> value.put("title", " \u00a0\u2003\u0085\t\n");
      case "longTitle" -> {
        value.put("title", "\uD83D\uDE80".repeat(161));
        code = "TOO_LONG";
      }
      case "longCriterion" -> {
        value.put("completionCriterion", "\uD83D\uDE80".repeat(2001));
        field = "completionCriterion";
        code = "TOO_LONG";
      }
      case "largeInteger" -> {
        value.put("estimatedMinutes", Long.MAX_VALUE);
        field = "estimatedMinutes";
        code = "OUT_OF_RANGE";
      }
      default -> {}
    }
    var result =
        mvc.perform(
            post(path())
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType(mode.equals("media") ? "text/plain" : "application/json")
                .content(value.toString()));
    if (mode.equals("media"))
      result
          .andExpect(status().isUnsupportedMediaType())
          .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    else
      result
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.errors[0].field").value(field))
          .andExpect(jsonPath("$.errors[0].code").value(code));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM tasks", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"idea", "active", "paused"})
  void s2_s3_s9_s10_s34_preservesAncestorsAndHistoricalRoots(String state) throws Exception {
    jdbc.update("UPDATE projects SET status=? WHERE id=?", state, project);
    jdbc.update(
        "UPDATE tasks SET completion_criterion='Parent criterion',estimated_minutes=1440 WHERE id=?",
        parent);
    var projectBefore = jdbc.queryForMap("SELECT * FROM projects WHERE id=?", project);
    var parentBefore = jdbc.queryForMap("SELECT * FROM tasks WHERE id=?", parent);
    String etag =
        mvc.perform(get("/api/v1/projects/" + project).with(user("persona-a")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getHeader("ETag");
    var child = createAt(path(), "Child");
    String childLocation = "/api/v1/projects/" + project + "/tasks/" + child.get("id").asText();
    var grandchild = createAt(childLocation + "/subtasks", "Grandchild");
    var relation =
        json.readTree(
            mvc.perform(get(childLocation + "/parent").with(user("persona-a")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    var parentDto =
        json.readTree(
            mvc.perform(
                    get("/api/v1/projects/" + project + "/tasks/" + parent).with(user("persona-a")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(relation.size()).isEqualTo(1);
    assertThat(relation.get("parent")).isEqualTo(parentDto);
    assertThat(parentDto.size()).isEqualTo(8);
    assertThat(
            jdbc.queryForObject(
                "SELECT parent_id FROM tasks WHERE id=?",
                UUID.class,
                UUID.fromString(grandchild.get("id").asText())))
        .isEqualTo(UUID.fromString(child.get("id").asText()));
    var root = createAt("/api/v1/projects/" + project + "/tasks", "Root");
    assertThat(
            jdbc.queryForObject(
                "SELECT parent_id FROM tasks WHERE id=?",
                UUID.class,
                UUID.fromString(root.get("id").asText())))
        .isNull();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE event_type='TaskCreated.v1'", Long.class))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE event_type='SubtaskCreated.v1'",
                Long.class))
        .isEqualTo(2);
    var all =
        json.readTree(
            mvc.perform(get("/api/v1/projects/" + project + "/tasks").with(user("persona-a")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(all.get("items")).hasSize(4);
    all.get("items").forEach(item -> assertThat(item.size()).isEqualTo(8));
    assertThat(jdbc.queryForMap("SELECT * FROM projects WHERE id=?", project))
        .isEqualTo(projectBefore);
    assertThat(jdbc.queryForMap("SELECT * FROM tasks WHERE id=?", parent)).isEqualTo(parentBefore);
    mvc.perform(get("/api/v1/projects/" + project).with(user("persona-a")))
        .andExpect(header().string("ETag", etag));
  }

  com.fasterxml.jackson.databind.JsonNode createAt(String target, String title) throws Exception {
    var response =
        mvc.perform(
                post(target)
                    .with(user("persona-a"))
                    .with(csrf().asHeader())
                    .contentType("application/json")
                    .content(
                        json.createObjectNode()
                            .put("title", title)
                            .put("completionCriterion", "Private child")
                            .put("estimatedMinutes", 1)
                            .toString()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();
    var body = json.readTree(response.getContentAsString());
    assertThat(body.size()).isEqualTo(8);
    var detail =
        json.readTree(
            mvc.perform(get(response.getHeader("Location")).with(user("persona-a")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(detail).isEqualTo(body);
    return body;
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"missingProject", "foreignProject", "missingTask", "wrongProject", "foreignTask"})
  void s8_comparesCompletePrivacyProblemsForAllThreeResources(String defect) throws Exception {
    var missing = UUID.randomUUID();
    String base = "/api/v1/projects/" + project + "/tasks/";
    var reference =
        json.readTree(
            mvc.perform(get(base + missing + "/parent").with(user("persona-a")))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse()
                .getContentAsString());
    UUID requestedProject = project, requestedTask = parent;
    if (defect.equals("missingProject")) requestedProject = UUID.randomUUID();
    if (defect.equals("foreignProject") || defect.equals("foreignTask"))
      jdbc.update("UPDATE projects SET owner_id='other' WHERE id=?", project);
    if (defect.equals("missingTask")) requestedTask = missing;
    if (defect.equals("wrongProject")) {
      requestedProject = UUID.randomUUID();
      jdbc.update(
          "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'persona-a','Other','','idea',now(),now())",
          requestedProject);
    }
    String target = "/api/v1/projects/" + requestedProject + "/tasks/" + requestedTask;
    for (var request :
        java.util.List.of(
            get(target + "/parent"),
            get(target + "/subtasks"),
            post(target + "/subtasks")
                .contentType("application/json")
                .content(json.createObjectNode().put("title", "T").toString()))) {
      var actual =
          json.readTree(
              mvc.perform(request.with(user("persona-a")).with(csrf().asHeader()))
                  .andExpect(status().isNotFound())
                  .andExpect(
                      header()
                          .string(
                              "Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                  .andReturn()
                  .getResponse()
                  .getContentAsString());
      assertThat(actual).isEqualTo(reference);
    }
    assertThat(jdbc.queryForObject("SELECT count(*) FROM tasks", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "tasks,skip",
    "outbox_events,skip",
    "tasks,fail",
    "outbox_events,fail"
  })
  void s18_rollsBackSuppressedOrFailedWrites(String table, String mode) throws Exception {
    var parentBefore = jdbc.queryForMap("SELECT * FROM tasks WHERE id=?", parent);
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
      assertThat(
              jdbc.queryForObject(
                  "SELECT count(*) FROM tasks WHERE parent_id IS NOT NULL", Long.class))
          .isZero();
      assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
      assertThat(jdbc.queryForMap("SELECT * FROM projects WHERE id=?", project))
          .isEqualTo(previous);
      assertThat(jdbc.queryForMap("SELECT * FROM tasks WHERE id=?", parent))
          .isEqualTo(parentBefore);
    } finally {
      jdbc.execute("DROP TRIGGER reject_task_write ON " + table);
      jdbc.execute("DROP FUNCTION reject_task_write()");
    }
  }

  @Autowired com.apptolast.organization.application.SubtaskCommit taskCommit;
  @Autowired com.apptolast.organization.application.ProjectStatusEditing statusEditing;

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
  void s17_serializesCreationAndCompletionOnSameProject(boolean taskFirst) throws Exception {
    jdbc.update("UPDATE projects SET status='active' WHERE id=?", project);
    var locked = new java.util.concurrent.CountDownLatch(1);
    var release = new java.util.concurrent.CountDownLatch(1);
    java.time.Clock clock = java.time.Clock.systemUTC();
    com.apptolast.organization.application.SubtaskCommit creationPort =
        (owner, id, parentId, operation) ->
            taskCommit.save(
                owner,
                id,
                parentId,
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
    var create = new com.apptolast.organization.application.CreateSubtask(creationPort, clock);
    var finish =
        new com.apptolast.organization.application.ChangeProjectStatus(transitionPort, clock, 3);
    java.util.concurrent.Callable<Object> createCall =
        () -> create.execute("persona-a", project, parent, "T", null, null);
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
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM tasks WHERE status='pending' AND parent_id IS NOT NULL",
                Long.class))
        .isEqualTo(taskFirst ? 1 : 0);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE event_type='SubtaskCreated.v1'",
                Long.class))
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

  @Autowired org.springframework.session.SessionRepository sessions;

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "create,false",
    "children,false",
    "parent,false",
    "create,true",
    "children,true",
    "parent,true"
  })
  void s6_requiresSessionIncludingPersistedExpiration(String operation, boolean expired)
      throws Exception {
    var request =
        operation.equals("create")
            ? post(path())
                .contentType("application/json")
                .content(json.createObjectNode().put("title", "T").toString())
            : get(
                operation.equals("children")
                    ? path()
                    : "/api/v1/projects/" + project + "/tasks/" + parent + "/parent");
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
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
        .andExpect(header().doesNotExist("WWW-Authenticate"))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM tasks", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"missing", "invalid", "origin"})
  void s7_writeProtection(String defect) throws Exception {
    var request =
        post(path())
            .with(user("persona-a"))
            .header(
                "Origin",
                defect.equals("origin")
                    ? "https://foreign.example"
                    : "https://organization.example")
            .contentType("application/json")
            .content(json.createObjectNode().put("title", "T").toString());
    if (defect.equals("invalid")) request.with(csrf().useInvalidToken().asHeader());
    if (defect.equals("origin")) request.with(csrf().asHeader());
    mvc.perform(request)
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.code")
                .value(defect.equals("origin") ? "UNTRUSTED_ORIGIN" : "CSRF_INVALID"));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM tasks", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "children,projectId",
    "children,parentId",
    "create,parentId",
    "parent,id"
  })
  void s15_validatesPathBeforeCursor(String operation, String field) throws Exception {
    String projectValue = field.equals("projectId") ? "1-1-1-1-1" : project.toString();
    String taskValue = field.equals("projectId") ? parent.toString() : "1-1-1-1-1";
    String target =
        "/api/v1/projects/"
            + projectValue
            + "/tasks/"
            + taskValue
            + (operation.equals("parent") ? "/parent" : "/subtasks");
    var cursor =
        json.createObjectNode()
            .put("projectId", project.toString())
            .put("parentTaskId", parent.toString())
            .put("createdAt", "2026-01-01T00:00:00Z")
            .put("id", UUID.randomUUID().toString());
    var request =
        operation.equals("create")
            ? post(target)
                .contentType("application/json")
                .content(json.createObjectNode().put("title", "T").toString())
            : get(target)
                .param(
                    "cursor",
                    java.util.Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(json.writeValueAsBytes(cursor)));
    mvc.perform(request.with(user("persona-a")).with(csrf().asHeader()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value(field))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_FORMAT"))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "children,200",
    "parent,200",
    "children,400",
    "parent,400",
    "children,401",
    "parent,401",
    "children,404",
    "parent,404",
    "children,503",
    "parent,503"
  })
  void s19_s20_doesNotCacheOrInventAnEmptyRelation(String operation, int expected)
      throws Exception {
    String id =
        expected == 400
            ? "bad"
            : expected == 404 ? UUID.randomUUID().toString() : parent.toString();
    String target =
        "/api/v1/projects/"
            + project
            + "/tasks/"
            + id
            + (operation.equals("parent") ? "/parent" : "/subtasks");
    if (expected == 503) jdbc.execute("ALTER TABLE tasks RENAME TO unavailable_subtask_fixture");
    try {
      var request = get(target);
      if (expected != 401) request.with(user("persona-a"));
      var response =
          mvc.perform(request)
              .andExpect(status().is(expected))
              .andExpect(
                  header()
                      .string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
              .andReturn()
              .getResponse();
      var body = json.readTree(response.getContentAsString());
      if (expected == 503) {
        assertThat(body.get("code").asText()).isEqualTo("STORAGE_UNAVAILABLE");
        assertThat(body.has("items")).isFalse();
        assertThat(body.has("parent")).isFalse();
      }
      if (expected == 200 && operation.equals("children")) {
        assertThat(body.size()).isEqualTo(2);
        assertThat(body.get("items")).isEmpty();
        assertThat(body.get("nextCursor").isNull()).isTrue();
      }
    } finally {
      if (expected == 503) jdbc.execute("ALTER TABLE unavailable_subtask_fixture RENAME TO tasks");
    }
  }

  @Test
  void s16_s36_requiresDeliberateReopeningBeforeAnotherCreation() throws Exception {
    jdbc.update("UPDATE projects SET status='completed' WHERE id=?", project);
    mvc.perform(
            post(path())
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(json.createObjectNode().put("title", "T").toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("PROJECT_COMPLETED"));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM tasks", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
    mvc.perform(get(path()).with(user("persona-a"))).andExpect(status().isOk());
    mvc.perform(
            get("/api/v1/projects/" + project + "/tasks/" + parent + "/parent")
                .with(user("persona-a")))
        .andExpect(status().isOk());
    mvc.perform(
            put("/api/v1/projects/" + project + "/status")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .header("If-Match", "\"" + project + ":0\"")
                .contentType("application/json")
                .content(json.createObjectNode().put("status", "paused").toString()))
        .andExpect(status().isOk());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM tasks", Long.class)).isEqualTo(1);
    createAt(path(), "Chosen after reopening");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE event_type='SubtaskCreated.v1'",
                Long.class))
        .isEqualTo(1);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"foreignProject", "self", "missing"})
  void s1_databaseEnforcesParentIntegrity(String defect) {
    UUID id = UUID.randomUUID(), parentId = parent, target = project;
    if (defect.equals("foreignProject")) {
      target = UUID.randomUUID();
      jdbc.update(
          "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'persona-a','Other','','idea',now(),now())",
          target);
    }
    if (defect.equals("self")) parentId = id;
    if (defect.equals("missing")) parentId = UUID.randomUUID();
    UUID finalTarget = target, finalParent = parentId;
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO tasks(id,project_id,parent_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,?,'Invalid','','pending',now(),now())",
                    id,
                    finalTarget,
                    finalParent))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM tasks", Long.class)).isEqualTo(1);
  }
}
