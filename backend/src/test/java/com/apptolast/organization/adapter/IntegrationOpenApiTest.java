package com.apptolast.organization.adapter;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.apptolast.organization.adapter.config.SecurityConfiguration;
import com.apptolast.organization.adapter.http.IntegrationOpenApiController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = IntegrationOpenApiController.class,
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@Import(SecurityConfiguration.class)
class IntegrationOpenApiTest {
  @Autowired MockMvc mvc;

  @Test
  void s32_writesDescribeClosedBodiesAndExistingEditPrecondition() throws Exception {
    var response =
        mvc.perform(get("/api/v1/integration-openapi.json").with(user("owner")))
            .andReturn()
            .getResponse();
    var json =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(response.getContentAsByteArray());
    var schemas = json.path("components").path("schemas");
    org.junit.jupiter.api.Assertions.assertEquals(
        120,
        schemas.path("ProjectCreate").path("properties").path("name").path("maxLength").asInt());
    org.junit.jupiter.api.Assertions.assertEquals(
        1440,
        schemas
            .path("TaskCreate")
            .path("properties")
            .path("estimatedMinutes")
            .path("maximum")
            .asInt());
    for (String name : java.util.List.of("ProjectCreate", "ProjectEdit", "TaskCreate")) {
      org.junit.jupiter.api.Assertions.assertEquals(
          false, schemas.path(name).path("additionalProperties").booleanValue());
      org.junit.jupiter.api.Assertions.assertTrue(schemas.path(name).path("required").size() > 0);
    }
    var edit = json.path("paths").path("/api/v1/projects/{id}").path("put");
    org.junit.jupiter.api.Assertions.assertTrue(
        edit.path("parameters").findValuesAsText("name").contains("If-Match"));
    org.junit.jupiter.api.Assertions.assertTrue(edit.path("responses").has("412"));
    org.junit.jupiter.api.Assertions.assertTrue(edit.path("responses").has("428"));
    org.junit.jupiter.api.Assertions.assertTrue(
        edit.path("responses").path("200").path("headers").has("ETag"));
    org.junit.jupiter.api.Assertions.assertEquals(
        "#/components/schemas/ProjectEdit",
        edit.path("requestBody")
            .path("content")
            .path("application/json")
            .path("schema")
            .path("$ref")
            .asText());
  }

  @Test
  void s32_documentListsExactlyTheEighteenAllowedOperationsAndScopes() throws Exception {
    var response =
        mvc.perform(get("/api/v1/integration-openapi.json").with(user("owner")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    var document =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(response.getContentAsByteArray());
    var actual = new java.util.TreeSet<String>();
    document
        .get("paths")
        .fields()
        .forEachRemaining(
            path ->
                path.getValue()
                    .fields()
                    .forEachRemaining(
                        operation ->
                            actual.add(
                                operation.getKey().toUpperCase(java.util.Locale.ROOT)
                                    + " "
                                    + path.getKey()
                                    + " "
                                    + operation.getValue().path("x-required-scope").asText())));
    org.junit.jupiter.api.Assertions.assertEquals(
        new java.util.TreeSet<>(
            java.util.List.of(
                "GET /api/v1/projects projects:read",
                "GET /api/v1/projects/{id} projects:read",
                "POST /api/v1/projects projects:write",
                "PUT /api/v1/projects/{id} projects:write",
                "GET /api/v1/projects/{projectId}/tasks tasks:read",
                "GET /api/v1/projects/{projectId}/tasks/{taskId} tasks:read",
                "GET /api/v1/projects/{projectId}/tasks/{id}/status tasks:read",
                "GET /api/v1/projects/{projectId}/tasks/{id}/parent tasks:read",
                "GET /api/v1/projects/{projectId}/tasks/{parentId}/subtasks tasks:read",
                "POST /api/v1/projects/{projectId}/tasks tasks:write",
                "GET /api/v1/today agenda:read",
                "GET /api/v1/projects/{projectId}/tasks/{taskId}/blocks agenda:read",
                "GET /api/v1/projects/{projectId}/tasks/{taskId}/blocks/{blockId} agenda:read",
                "GET /api/v1/projects/{projectId}/tasks/{taskId}/blocks/{blockId}/state agenda:read",
                "GET /api/v1/projects/{projectId}/tasks/{taskId}/blocks/by-request/{requestKey} agenda:read",
                "GET /api/v1/history history:read",
                "GET /api/v1/weekly-review history:read",
                "GET /api/v1/projects/{projectId}/tasks/{id}/history history:read")),
        actual);
  }

  @Test
  void s32_humanSessionCanReadVersionedBearerDescription() throws Exception {
    mvc.perform(get("/api/v1/integration-openapi.json").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.openapi").value("3.1.0"))
        .andExpect(jsonPath("$.info.version").value("1.0.0"))
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
        .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
  }
}
