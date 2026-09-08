package com.apptolast.organization.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example",
      "app.publisher.enabled=false"
    })
@AutoConfigureMockMvc
@Testcontainers
class CustomizationHttpPersistenceTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  Cookie session;

  @BeforeEach
  void authenticateAndResetOnlyPrivateMetadata() throws Exception {
    jdbc.update("DELETE FROM project_custom_field_values WHERE owner_id='owner'");
    jdbc.update("DELETE FROM task_custom_field_values WHERE owner_id='owner'");
    jdbc.update("DELETE FROM customization_preferences WHERE owner_id='owner'");
    var login =
        mvc.perform(
                post("/api/session")
                    .with(csrf().asHeader())
                    .header("Origin", "https://organization.example")
                    .param("username", "owner")
                    .param("password", "test-only-secret"))
            .andExpect(status().isNoContent())
            .andReturn()
            .getResponse();
    session = login.getCookie("SESSION");
    assertThat(session).isNotNull();
  }

  @Test
  void s3_s10_s11_projectDefinitionAndValuesRoundTripThroughRealBeansAndPostgres()
      throws Exception {
    var project = project("owner");
    var field = createField("PROJECT", "Nota", "TEXT");
    var path = "/api/v1/projects/" + project + "/custom-fields";
    var initial = read(path);
    assertThat(body(initial).get("configured").asBoolean()).isFalse();
    assertThat(body(initial).get("values").get(0).get("value").isNull()).isTrue();
    var saved =
        write(
            put(path),
            initial.getHeader("ETag"),
            "{\"values\":[{\"fieldId\":\"" + field + "\",\"value\":\"  privado  \"}]}");
    assertThat(body(saved).size()).isEqualTo(3);
    assertThat(body(saved).get("configured").asBoolean()).isTrue();
    assertThat(body(saved).get("values").get(0).size()).isEqualTo(4);
    assertThat(body(saved).get("values").get(0).get("fieldId").asText()).isEqualTo(field);
    assertThat(body(saved).get("values").get(0).get("value").asText()).isEqualTo("  privado  ");
    var reread = read(path);
    assertThat(body(reread)).isEqualTo(body(saved));
    assertThat(reread.getHeader("ETag")).isEqualTo(saved.getHeader("ETag"));
    assertThat(
            jdbc.queryForObject(
                "SELECT field_values ->> ? FROM project_custom_field_values WHERE owner_id='owner' AND project_id=?",
                String.class,
                field,
                project))
        .isEqualTo("  privado  ");
  }

  UUID project(String owner) {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,?,'P','','active',now(),now())",
        id,
        owner);
    return id;
  }

  @Test
  void s23_realSessionSecurityRejectsAnonymousReadAndUnprotectedWrite() throws Exception {
    var project = project("owner");
    var path = "/api/v1/projects/" + project + "/custom-fields";
    mvc.perform(get(path)).andExpect(status().isUnauthorized());
    var initial = read(path);
    mvc.perform(
            put(path)
                .cookie(session)
                .header("Origin", "https://organization.example")
                .header("If-Match", initial.getHeader("ETag"))
                .contentType("application/json")
                .content("{\"values\":[]}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            put(path)
                .cookie(session)
                .with(csrf().asHeader())
                .header("Origin", "https://foreign.example")
                .header("If-Match", initial.getHeader("ETag"))
                .contentType("application/json")
                .content("{\"values\":[]}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("UNTRUSTED_ORIGIN"));
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM project_custom_field_values WHERE project_id=?",
                Integer.class,
                project))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM customization_preferences WHERE owner_id='owner'",
                Integer.class))
        .isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"foreign-project", "foreign-task", "wrong-project"})
  void s17_realOwnershipAndTaskContextRejectReadAndWriteWithoutEffects(String context)
      throws Exception {
    var ownProject = project("owner");
    var otherProject = project("owner");
    var foreignProject = project("other");
    var task = UUID.randomUUID();
    var foreignTask = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,'Privado','','pending',now(),now())",
        task,
        ownProject);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,'Ajeno','','pending',now(),now())",
        foreignTask,
        foreignProject);
    var field = createField("TASK", "Secreto", "TEXT");
    var ownPath = "/api/v1/projects/" + ownProject + "/tasks/" + task + "/custom-fields";
    write(
        put(ownPath),
        read(ownPath).getHeader("ETag"),
        "{\"values\":[{\"fieldId\":\"" + field + "\",\"value\":\"privado\"}]}");
    var path =
        switch (context) {
          case "foreign-project" -> "/api/v1/projects/" + foreignProject + "/custom-fields";
          case "foreign-task" ->
              "/api/v1/projects/" + foreignProject + "/tasks/" + foreignTask + "/custom-fields";
          default -> "/api/v1/projects/" + otherProject + "/tasks/" + task + "/custom-fields";
        };
    var scope = context.equals("foreign-project") ? "PROJECT" : "TASK";
    var entity =
        context.equals("foreign-project")
            ? foreignProject
            : context.equals("foreign-task") ? foreignTask : task;
    var before = jdbc.queryForList("SELECT * FROM task_custom_field_values ORDER BY id");
    for (var request :
        java.util.List.of(
            get(path).cookie(session),
            put(path)
                .cookie(session)
                .with(csrf().asHeader())
                .header("Origin", "https://organization.example")
                .header(
                    "If-Match",
                    "\"custom-values:"
                        + scope
                        + ":"
                        + entity
                        + ":schema:unconfigured:values:unconfigured\"")
                .contentType("application/json")
                .content("{\"values\":[]}"))) {
      mvc.perform(request)
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
          .andExpect(jsonPath("$.values").doesNotExist())
          .andExpect(header().doesNotExist("ETag"));
    }
    assertThat(jdbc.queryForList("SELECT * FROM task_custom_field_values ORDER BY id"))
        .isEqualTo(before);
    assertThat(
            jdbc.queryForObject("SELECT count(*) FROM project_custom_field_values", Integer.class))
        .isZero();
  }

  @Test
  void s16_canonicalNumberNoOpPreservesExactBodyTagAndStoredRow() throws Exception {
    var project = project("owner");
    var field = createField("PROJECT", "Cantidad", "NUMBER");
    var path = "/api/v1/projects/" + project + "/custom-fields";
    var saved =
        write(
            put(path),
            read(path).getHeader("ETag"),
            "{\"values\":[{\"fieldId\":\"" + field + "\",\"value\":1.0}]}");
    assertThat(body(saved).get("values").get(0).get("value").intValue()).isEqualTo(1);
    var before =
        jdbc.queryForMap("SELECT * FROM project_custom_field_values WHERE project_id=?", project);
    var noop =
        write(
            put(path),
            saved.getHeader("ETag"),
            "{\"values\":[{\"fieldId\":\"" + field + "\",\"value\":1e0}]}");
    assertThat(noop.getContentAsString()).isEqualTo(saved.getContentAsString());
    assertThat(noop.getHeader("ETag")).isEqualTo(saved.getHeader("ETag"));
    assertThat(
            jdbc.queryForMap(
                "SELECT * FROM project_custom_field_values WHERE project_id=?", project))
        .isEqualTo(before);
  }

  @Test
  void s15_renamingTheSchemaRejectsAnOldCompositeTagWithoutChangingStoredValues() throws Exception {
    var project = project("owner");
    var field = createField("PROJECT", "Antes", "TEXT");
    var path = "/api/v1/projects/" + project + "/custom-fields";
    var initial = read(path);
    var saved =
        write(
            put(path),
            initial.getHeader("ETag"),
            "{\"values\":[{\"fieldId\":\"" + field + "\",\"value\":\"original\"}]}");
    var before =
        jdbc.queryForMap("SELECT * FROM project_custom_field_values WHERE project_id=?", project);
    var schema = read("/api/v1/me/customization/PROJECT");
    write(
        put("/api/v1/me/customization/PROJECT/fields/" + field),
        schema.getHeader("ETag"),
        "{\"label\":\"Después\",\"active\":true}");
    mvc.perform(
            put(path)
                .cookie(session)
                .with(csrf().asHeader())
                .header("Origin", "https://organization.example")
                .header("If-Match", saved.getHeader("ETag"))
                .contentType("application/json")
                .content(
                    "{\"values\":[{\"fieldId\":\""
                        + field
                        + "\",\"value\":\"no debe guardarse\"}]}"))
        .andExpect(status().isPreconditionFailed())
        .andExpect(jsonPath("$.code").value("CUSTOMIZATION_CONFLICT"))
        .andExpect(header().doesNotExist("ETag"));
    assertThat(
            jdbc.queryForMap(
                "SELECT * FROM project_custom_field_values WHERE project_id=?", project))
        .isEqualTo(before);
    var current = read(path);
    assertThat(current.getHeader("ETag")).isNotEqualTo(saved.getHeader("ETag"));
    assertThat(current.getHeader("ETag").split(":values:")[1])
        .isEqualTo(saved.getHeader("ETag").split(":values:")[1]);
    assertThat(body(current).get("values").get(0).get("label").asText()).isEqualTo("Después");
    assertThat(body(current).get("values").get(0).get("value").asText()).isEqualTo("original");
  }

  @Test
  void s14_completedTaskValuesKeepAllBusinessColumnsAndPersistFalse() throws Exception {
    var project = project("owner");
    var task = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,estimated_minutes,created_at,updated_at,completed_at) VALUES (?,?,'Terminado','Se conserva','completed',25,'2026-09-07T10:00:00Z','2026-09-07T11:00:00Z','2026-09-07T11:00:00Z')",
        task,
        project);
    var before = jdbc.queryForMap("SELECT * FROM tasks WHERE id=?", task);
    var field = createField("TASK", "Validado", "BOOLEAN");
    var path = "/api/v1/projects/" + project + "/tasks/" + task + "/custom-fields";
    var initial = read(path);
    var saved =
        write(
            put(path),
            initial.getHeader("ETag"),
            "{\"values\":[{\"fieldId\":\"" + field + "\",\"value\":false}]}");
    assertThat(body(saved).get("values").get(0).get("value").isBoolean()).isTrue();
    assertThat(body(saved).get("values").get(0).get("value").booleanValue()).isFalse();
    assertThat(body(read(path))).isEqualTo(body(saved));
    assertThat(jdbc.queryForMap("SELECT * FROM tasks WHERE id=?", task)).isEqualTo(before);
    assertThat(
            jdbc.queryForObject(
                "SELECT field_values ->> ? FROM task_custom_field_values WHERE task_id=?",
                String.class,
                field,
                task))
        .isEqualTo("false");
  }

  String createField(String scope, String label, String type) throws Exception {
    var path = "/api/v1/me/customization/" + scope;
    var initial = read(path);
    var created =
        write(
            post(path + "/fields"),
            initial.getHeader("ETag"),
            "{\"label\":\"" + label + "\",\"type\":\"" + type + "\"}");
    return body(created).get("customFields").get(0).get("id").asText();
  }

  MockHttpServletResponse read(String path) throws Exception {
    return mvc.perform(get(path).cookie(session))
        .andExpect(status().isOk())
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andReturn()
        .getResponse();
  }

  MockHttpServletResponse write(MockHttpServletRequestBuilder request, String tag, String body)
      throws Exception {
    return mvc.perform(
            request
                .cookie(session)
                .with(csrf().asHeader())
                .header("Origin", "https://organization.example")
                .header("If-Match", tag)
                .contentType("application/json")
                .content(body))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();
  }

  JsonNode body(MockHttpServletResponse response) throws Exception {
    return json.readTree(response.getContentAsString());
  }
}
