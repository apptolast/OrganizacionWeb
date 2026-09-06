package com.apptolast.organization.adapter.http;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

import com.apptolast.organization.application.CreateSubtaskUseCase;
import com.apptolast.organization.application.CreateTaskUseCase;
import com.apptolast.organization.application.ReadSubtasksUseCase;
import com.apptolast.organization.application.ReadTasksUseCase;
import com.apptolast.organization.domain.FieldError;
import com.apptolast.organization.domain.Task;
import com.apptolast.organization.domain.TaskPosition;
import com.apptolast.organization.domain.ValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.security.Principal;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
public final class TaskController {
  private final ReadTasksUseCase read;
  private final CreateTaskUseCase create;
  private final ObjectMapper json;
  private final CreateSubtaskUseCase split;
  private final ReadSubtasksUseCase subtasks;

  public TaskController(
      CreateTaskUseCase create,
      ObjectMapper json,
      ReadTasksUseCase read,
      CreateSubtaskUseCase split,
      ReadSubtasksUseCase subtasks) {
    this.subtasks = subtasks;
    this.split = split;
    this.create = create;
    this.json = json;
    this.read = read;
  }

  @PostMapping(
      value = {
        "/api/v1/projects/{projectId}/tasks",
        "/api/v1/projects/{projectId}/tasks/{parentId}/subtasks"
      },
      consumes = "application/json")
  public ResponseEntity<Task> create(
      @PathVariable String projectId,
      @PathVariable(required = false) String parentId,
      @RequestBody String raw,
      Principal principal)
      throws JsonProcessingException {
    JsonNode body =
        json.reader()
            .with(FAIL_ON_READING_DUP_TREE_KEY)
            .with(FAIL_ON_TRAILING_TOKENS)
            .readTree(raw);
    if (body == null || !body.isObject()) throw invalid("body", "INVALID_TYPE");
    body.fieldNames()
        .forEachRemaining(
            field -> {
              if (!Set.of("title", "completionCriterion", "estimatedMinutes").contains(field))
                throw invalid(field, "UNKNOWN_FIELD");
            });
    var task =
        parentId == null
            ? create.execute(
                principal.getName(),
                identifier(projectId, "projectId"),
                string(body, "title"),
                string(body, "completionCriterion"),
                minutes(body))
            : split.execute(
                principal.getName(),
                identifier(projectId, "projectId"),
                identifier(parentId, "parentId"),
                string(body, "title"),
                string(body, "completionCriterion"),
                minutes(body));
    return ResponseEntity.created(
            URI.create("/api/v1/projects/" + task.projectId() + "/tasks/" + task.id()))
        .body(task);
  }

  private String string(JsonNode body, String field) {
    var value = body.get(field);
    if (value == null || value.isNull()) return null;
    if (!value.isTextual()) throw invalid(field, "INVALID_TYPE");
    return value.textValue();
  }

  private Integer minutes(JsonNode body) {
    var value = body.get("estimatedMinutes");
    if (value == null || value.isNull()) return null;
    if (!value.isIntegralNumber()) throw invalid("estimatedMinutes", "INVALID_TYPE");
    if (!value.canConvertToInt()) throw invalid("estimatedMinutes", "OUT_OF_RANGE");
    return value.intValue();
  }

  private static ValidationException invalid(String field, String code) {
    return new ValidationException(
        List.of(new FieldError(field, code, "Revisa el valor de este campo.")));
  }

  @ExceptionHandler(JsonProcessingException.class)
  ResponseEntity<Map<String, Object>> malformed() {
    return ResponseEntity.badRequest()
        .contentType(APPLICATION_PROBLEM_JSON)
        .body(ApiErrors.problem(400, "MALFORMED_JSON", "No se puede leer el JSON enviado."));
  }

  public record PageResponse(List<Task> items, String nextCursor) {}

  @GetMapping("/api/v1/projects/{projectId}/tasks")
  public PageResponse list(
      @PathVariable String projectId,
      @RequestParam MultiValueMap<String, String> parameters,
      Principal principal)
      throws JsonProcessingException {
    UUID project = identifier(projectId, "projectId");
    if (parameters.keySet().stream().anyMatch(key -> !key.equals("cursor")))
      throw invalid("query", "INVALID_VALUE");
    TaskPosition after = null;
    if (parameters.containsKey("cursor")) {
      if (parameters.get("cursor").size() != 1) throw invalid("cursor", "INVALID_VALUE");
      after = decode(parameters.getFirst("cursor"), project);
    }
    var page = read.list(principal.getName(), project, after);
    String next =
        page.next() == null
            ? null
            : Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                    json.writeValueAsBytes(
                        Map.of(
                            "projectId",
                            project.toString(),
                            "createdAt",
                            page.next().createdAt().toString(),
                            "id",
                            page.next().id().toString())));
    return new PageResponse(page.items(), next);
  }

  private TaskPosition decode(String cursor, UUID project) {
    return decode(cursor, project, null);
  }

  private TaskPosition decode(String cursor, UUID project, UUID parent) {
    try {
      if (cursor == null || !cursor.matches("[A-Za-z0-9_-]+"))
        throw invalid("cursor", "INVALID_VALUE");
      byte[] bytes = Base64.getUrlDecoder().decode(cursor);
      if (!Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(cursor))
        throw invalid("cursor", "INVALID_VALUE");
      var body =
          json.reader()
              .with(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
              .with(FAIL_ON_TRAILING_TOKENS)
              .readTree(bytes);
      if (body == null
          || !body.isObject()
          || body.size() != (parent == null ? 3 : 4)
          || !body.has("projectId")
          || !body.has("createdAt")
          || !body.has("id")
          || !body.get("projectId").isTextual()
          || !body.get("createdAt").isTextual()
          || !body.get("id").isTextual()) throw invalid("cursor", "INVALID_VALUE");
      if (!project.toString().equals(body.get("projectId").textValue()))
        throw invalid("cursor", "INVALID_VALUE");
      if (parent != null
          && (!body.has("parentTaskId")
              || !body.get("parentTaskId").isTextual()
              || !parent.toString().equals(body.get("parentTaskId").textValue())))
        throw invalid("cursor", "INVALID_VALUE");
      String timestamp = body.get("createdAt").textValue();
      var instant = Instant.parse(timestamp);
      if (!timestamp.endsWith("Z")
          || instant.getNano() % 1000 != 0
          || instant.isBefore(Instant.parse("-4713-11-24T00:00:00Z"))
          || !instant.isBefore(Instant.parse("+294277-01-01T00:00:00Z")))
        throw invalid("cursor", "INVALID_VALUE");
      String id = body.get("id").textValue();
      if (!id.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
        throw invalid("cursor", "INVALID_VALUE");
      return new TaskPosition(instant, UUID.fromString(id));
    } catch (Exception error) {
      throw invalid("cursor", "INVALID_VALUE");
    }
  }

  @GetMapping("/api/v1/projects/{projectId}/tasks/{taskId}")
  public Task detail(
      @PathVariable String projectId, @PathVariable String taskId, Principal principal) {
    return read.detail(
        principal.getName(), identifier(projectId, "projectId"), identifier(taskId, "taskId"));
  }

  private static UUID identifier(String value, String field) {
    if (!value.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
      throw invalid(field, "INVALID_FORMAT");
    return UUID.fromString(value);
  }

  public record ParentResponse(Task parent) {}

  @GetMapping("/api/v1/projects/{projectId}/tasks/{id}/parent")
  public ParentResponse parent(
      @PathVariable String projectId, @PathVariable String id, Principal principal) {
    return new ParentResponse(
        subtasks
            .parent(principal.getName(), identifier(projectId, "projectId"), identifier(id, "id"))
            .orElse(null));
  }

  @GetMapping("/api/v1/projects/{projectId}/tasks/{parentId}/subtasks")
  public PageResponse children(
      @PathVariable String projectId,
      @PathVariable String parentId,
      @RequestParam MultiValueMap<String, String> parameters,
      Principal principal)
      throws JsonProcessingException {
    var project = identifier(projectId, "projectId");
    var parent = identifier(parentId, "parentId");
    if (parameters.keySet().stream().anyMatch(key -> !key.equals("cursor")))
      throw invalid("query", "INVALID_VALUE");
    TaskPosition after = null;
    if (parameters.containsKey("cursor")) {
      if (parameters.get("cursor").size() != 1) throw invalid("cursor", "INVALID_VALUE");
      after = decode(parameters.getFirst("cursor"), project, parent);
    }
    var page = subtasks.list(principal.getName(), project, parent, after);
    var next =
        page.next() == null
            ? null
            : Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                    json.writeValueAsBytes(
                        Map.of(
                            "projectId",
                            project.toString(),
                            "parentTaskId",
                            parent.toString(),
                            "createdAt",
                            page.next().createdAt().toString(),
                            "id",
                            page.next().id().toString())));
    return new PageResponse(page.items(), next);
  }
}
