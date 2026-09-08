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
  void s32_everyOperationDocumentsBearerErrorsAndAllReferencesResolve() throws Exception {
    var response =
        mvc.perform(get("/api/v1/integration-openapi.json").with(user("owner")))
            .andReturn()
            .getResponse();
    var doc =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(response.getContentAsByteArray());
    for (var path : doc.path("paths"))
      for (var operation : path) {
        for (String status : java.util.List.of("401", "403", "429", "503"))
          org.junit.jupiter.api.Assertions.assertTrue(
              operation.path("responses").has(status), status);
        String success = operation.path("responses").has("201") ? "201" : "200";
        org.junit.jupiter.api.Assertions.assertTrue(
            operation
                .path("responses")
                .path(success)
                .path("content")
                .path("application/json")
                .path("schema")
                .has("$ref"));
      }
    org.junit.jupiter.api.Assertions.assertEquals(
        "Bearer",
        doc.path("components")
            .path("responses")
            .path("Unauthenticated")
            .path("headers")
            .path("WWW-Authenticate")
            .path("schema")
            .path("const")
            .asText());
    org.junit.jupiter.api.Assertions.assertEquals(
        1,
        doc.path("components")
            .path("responses")
            .path("RateLimited")
            .path("headers")
            .path("Retry-After")
            .path("schema")
            .path("minimum")
            .asInt());
    for (var reference : doc.findValues("$ref")) {
      org.junit.jupiter.api.Assertions.assertTrue(reference.textValue().startsWith("#/"));
      org.junit.jupiter.api.Assertions.assertFalse(
          doc.at(reference.textValue().substring(1)).isMissingNode(), reference.textValue());
    }
  }

  @Test
  void s32_historyPreservesSevenConcreteDetailShapesAndReceiptCounters() throws Exception {
    var response =
        mvc.perform(get("/api/v1/integration-openapi.json").with(user("owner")))
            .andReturn()
            .getResponse();
    var doc =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(response.getContentAsByteArray());
    var schemas = doc.path("components").path("schemas");
    org.junit.jupiter.api.Assertions.assertEquals(
        7, schemas.path("HistoryEntry").path("properties").path("details").path("oneOf").size());
    org.junit.jupiter.api.Assertions.assertEquals(
        7, schemas.path("SessionStart").path("properties").size());
    org.junit.jupiter.api.Assertions.assertEquals(
        "string",
        schemas.path("SessionState").path("properties").path("revision").path("type").asText());
    org.junit.jupiter.api.Assertions.assertEquals(
        "string",
        schemas
            .path("SessionState")
            .path("properties")
            .path("workedMicroseconds")
            .path("type")
            .asText());
    org.junit.jupiter.api.Assertions.assertTrue(
        schemas.path("SessionCloseReceipt").path("properties").has("closure"));
    org.junit.jupiter.api.Assertions.assertTrue(
        schemas.path("SessionExtensionReceipt").path("properties").has("extension"));
    org.junit.jupiter.api.Assertions.assertTrue(
        schemas.path("BlockChangeReceipt").path("properties").path("after").has("anyOf"));
    org.junit.jupiter.api.Assertions.assertEquals(
        java.util.List.of("planned", "cancelled"),
        new com.fasterxml.jackson.databind.ObjectMapper()
            .convertValue(
                schemas.path("BlockState").path("properties").path("status").path("enum"),
                java.util.List.class));
  }

  @Test
  void s32_agendaAndWeeklyDescribeNullableBudgetsAndTextualMicroseconds() throws Exception {
    var response =
        mvc.perform(get("/api/v1/integration-openapi.json").with(user("owner")))
            .andReturn()
            .getResponse();
    var doc =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(response.getContentAsByteArray());
    var schemas = doc.path("components").path("schemas");
    org.junit.jupiter.api.Assertions.assertEquals(
        9, schemas.path("Block").path("properties").size());
    org.junit.jupiter.api.Assertions.assertFalse(
        schemas.path("Block").path("properties").has("status"));
    org.junit.jupiter.api.Assertions.assertEquals(
        "integer",
        schemas.path("Today").path("properties").path("plannedSeconds").path("type").asText());
    org.junit.jupiter.api.Assertions.assertTrue(
        schemas.path("Today").path("properties").path("budgetMinutes").has("anyOf"));
    org.junit.jupiter.api.Assertions.assertEquals(
        "string",
        schemas
            .path("WeeklyTotals")
            .path("properties")
            .path("workedMicroseconds")
            .path("type")
            .asText());
    org.junit.jupiter.api.Assertions.assertTrue(
        schemas.path("WeeklyTotals").path("properties").path("capacityMicroseconds").has("anyOf"));
    org.junit.jupiter.api.Assertions.assertEquals(
        "string",
        schemas
            .path("WeeklyReview")
            .path("properties")
            .path("unquantifiedSessionCount")
            .path("type")
            .asText());
    org.junit.jupiter.api.Assertions.assertEquals(
        java.util.List.of("pending", "completed"),
        new com.fasterxml.jackson.databind.ObjectMapper()
            .convertValue(
                schemas.path("Task").path("properties").path("status").path("enum"),
                java.util.List.class));
  }

  @Test
  void s32_projectAndTaskResponsesDescribeTheirActualWireShapes() throws Exception {
    var response =
        mvc.perform(get("/api/v1/integration-openapi.json").with(user("owner")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    var doc =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(response.getContentAsByteArray());
    var schemas = doc.path("components").path("schemas");
    org.junit.jupiter.api.Assertions.assertEquals(
        "#/components/schemas/ProjectPage",
        doc.path("paths")
            .path("/api/v1/projects")
            .path("get")
            .path("responses")
            .path("200")
            .path("content")
            .path("application/json")
            .path("schema")
            .path("$ref")
            .asText());
    org.junit.jupiter.api.Assertions.assertEquals(
        "#/components/schemas/ProjectSummary",
        schemas
            .path("ProjectPage")
            .path("properties")
            .path("items")
            .path("items")
            .path("$ref")
            .asText());
    org.junit.jupiter.api.Assertions.assertEquals(
        5, schemas.path("ProjectSummary").path("properties").size());
    org.junit.jupiter.api.Assertions.assertFalse(
        schemas.path("ProjectSummary").path("properties").has("ownerId"));
    org.junit.jupiter.api.Assertions.assertTrue(
        schemas.path("Project").path("properties").has("ownerId"));
    org.junit.jupiter.api.Assertions.assertEquals(
        8, schemas.path("Task").path("properties").size());
    org.junit.jupiter.api.Assertions.assertEquals(
        "#/components/schemas/Task",
        schemas
            .path("TaskPage")
            .path("properties")
            .path("items")
            .path("items")
            .path("$ref")
            .asText());
    org.junit.jupiter.api.Assertions.assertTrue(
        schemas.path("TaskParent").path("properties").path("parent").has("anyOf"));
  }

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
        schemas
            .path("ProjectCreate")
            .path("properties")
            .path("name")
            .path("x-normalizedMaxLength")
            .asInt());
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
    for (String name : java.util.List.of("ProjectCreate", "ProjectEdit", "TaskCreate")) {
      var field =
          schemas.path(name).path("properties").path(name.equals("TaskCreate") ? "title" : "name");
      org.junit.jupiter.api.Assertions.assertFalse(
          field.has("maxLength"), "El límite aplica tras recortar Unicode White_Space");
      org.junit.jupiter.api.Assertions.assertEquals(
          name.equals("TaskCreate") ? 160 : 120, field.path("x-normalizedMaxLength").asInt());
      org.junit.jupiter.api.Assertions.assertTrue(
          field.path("description").asText().contains("White_Space"));
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
