package com.apptolast.organization.adapter.http;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS;

import com.apptolast.organization.application.ReadTaskHistoryUseCase;
import com.apptolast.organization.domain.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.time.Instant;
import java.util.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
public final class TaskHistoryController {
  private final ReadTaskHistoryUseCase read;
  private final ObjectMapper json;

  public TaskHistoryController(ReadTaskHistoryUseCase read, ObjectMapper json) {
    this.read = read;
    this.json = json;
  }

  public record HistoryEntryResponse(
      UUID id, String fromStatus, String toStatus, Instant occurredAt) {}

  public record HistoryResponse(List<HistoryEntryResponse> items, String nextCursor) {}

  @GetMapping("/api/v1/projects/{projectId}/tasks/{id}/history")
  public HistoryResponse history(
      @PathVariable String projectId,
      @PathVariable String id,
      @RequestParam MultiValueMap<String, String> parameters,
      Principal principal)
      throws JsonProcessingException {
    var project = identifier(projectId, "projectId");
    var task = identifier(id, "id");
    if (parameters.keySet().stream().anyMatch(key -> !key.equals("cursor")))
      throw invalid("query", "INVALID_VALUE");
    TaskHistoryPosition after = null;
    if (parameters.containsKey("cursor")) {
      if (parameters.get("cursor").size() != 1) throw invalid("cursor", "INVALID_VALUE");
      after = decode(parameters.getFirst("cursor"), project, task);
    }
    var page = read.list(principal.getName(), project, task, after);
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
                            "taskId",
                            task.toString(),
                            "taskVersion",
                            page.next().taskVersion())));
    return new HistoryResponse(
        page.items().stream()
            .map(
                item ->
                    new HistoryEntryResponse(
                        item.id(), item.fromStatus(), item.toStatus(), item.occurredAt()))
            .toList(),
        next);
  }

  private TaskHistoryPosition decode(String cursor, UUID project, UUID task) {
    try {
      if (cursor == null || !cursor.matches("[A-Za-z0-9_-]+"))
        throw invalid("cursor", "INVALID_VALUE");
      var bytes = Base64.getUrlDecoder().decode(cursor);
      if (!Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(cursor))
        throw invalid("cursor", "INVALID_VALUE");
      var body =
          json.reader()
              .with(FAIL_ON_READING_DUP_TREE_KEY)
              .with(FAIL_ON_TRAILING_TOKENS)
              .readTree(bytes);
      if (body == null
          || !body.isObject()
          || body.size() != 3
          || !body.has("projectId")
          || !body.has("taskId")
          || !body.has("taskVersion")
          || !body.get("projectId").isTextual()
          || !body.get("taskId").isTextual()
          || !body.get("taskVersion").isIntegralNumber()
          || !body.get("taskVersion").canConvertToLong()) throw invalid("cursor", "INVALID_VALUE");
      if (!project.toString().equals(body.get("projectId").textValue())
          || !task.toString().equals(body.get("taskId").textValue())
          || body.get("taskVersion").longValue() <= 0) throw invalid("cursor", "INVALID_VALUE");
      return new TaskHistoryPosition(body.get("taskVersion").longValue());
    } catch (Exception error) {
      throw invalid("cursor", "INVALID_VALUE");
    }
  }

  private static UUID identifier(String value, String field) {
    if (!value.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
      throw invalid(field, "INVALID_FORMAT");
    return UUID.fromString(value);
  }

  private static ValidationException invalid(String field, String code) {
    return new ValidationException(
        List.of(new FieldError(field, code, "Revisa el valor de este campo.")));
  }
}
