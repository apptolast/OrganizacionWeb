package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.ChangeTaskStatusUseCase;
import com.apptolast.organization.application.ReadTaskStatusUseCase;
import com.apptolast.organization.domain.FieldError;
import com.apptolast.organization.domain.TaskRevision;
import com.apptolast.organization.domain.TaskSnapshot;
import com.apptolast.organization.domain.ValidationException;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public final class TaskStatusController {
  private final ReadTaskStatusUseCase read;
  private final ChangeTaskStatusUseCase change;
  private final com.fasterxml.jackson.databind.ObjectMapper json;

  public TaskStatusController(
      ReadTaskStatusUseCase read,
      ChangeTaskStatusUseCase change,
      com.fasterxml.jackson.databind.ObjectMapper json) {
    this.read = read;
    this.change = change;
    this.json = json;
  }

  public record StatusResponse(String status, Instant completedAt, Instant updatedAt) {}

  @GetMapping("/api/v1/projects/{projectId}/tasks/{id}/status")
  public ResponseEntity<StatusResponse> status(
      @PathVariable String projectId, @PathVariable String id, Principal principal) {
    return response(
        read.status(principal.getName(), identifier(projectId, "projectId"), identifier(id, "id")));
  }

  private static ResponseEntity<StatusResponse> response(TaskSnapshot snapshot) {
    return ResponseEntity.ok()
        .eTag("\"task:" + snapshot.task().id() + ":" + snapshot.version() + "\"")
        .body(
            new StatusResponse(
                snapshot.task().status(), snapshot.completedAt(), snapshot.task().updatedAt()));
  }

  @PutMapping(
      value = "/api/v1/projects/{projectId}/tasks/{id}/status",
      consumes = "application/json")
  public ResponseEntity<?> change(
      @PathVariable String projectId,
      @PathVariable String id,
      @RequestHeader(value = "If-Match", required = false) List<String> matches,
      @RequestBody(required = false) String raw,
      Principal principal)
      throws com.fasterxml.jackson.core.JsonProcessingException {
    var project = identifier(projectId, "projectId");
    var task = identifier(id, "id");
    if (matches == null)
      return ResponseEntity.status(428)
          .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
          .body(
              ApiErrors.problem(
                  428, "PRECONDITION_REQUIRED", "Envía la versión actual de la tarea."));
    var expected = precondition(matches, task);
    return response(change.execute(principal.getName(), project, task, expected, target(raw)));
  }

  private String target(String raw) throws com.fasterxml.jackson.core.JsonProcessingException {
    if (raw == null || raw.isBlank()) throw new MalformedBody();
    com.fasterxml.jackson.databind.JsonNode body =
        json.reader()
            .with(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .readTree(raw);
    if (body == null || !body.isObject()) throw invalid("body", "INVALID_TYPE");
    var extras = new java.util.TreeSet<String>();
    body.fieldNames()
        .forEachRemaining(
            field -> {
              if (!field.equals("status")) extras.add(field);
            });
    if (!extras.isEmpty()) throw invalid(extras.first(), "UNKNOWN_FIELD");
    var status = body.get("status");
    if (status == null || status.isNull()) throw invalid("status", "REQUIRED");
    if (!status.isTextual()) throw invalid("status", "INVALID_TYPE");
    if (!Set.of("pending", "completed").contains(status.textValue()))
      throw invalid("status", "INVALID_VALUE");
    return status.textValue();
  }

  private static final class MalformedBody extends RuntimeException {}

  @ExceptionHandler({MalformedBody.class, com.fasterxml.jackson.core.JsonProcessingException.class})
  ResponseEntity<Map<String, Object>> malformed() {
    return ResponseEntity.badRequest()
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(ApiErrors.problem(400, "MALFORMED_JSON", "No se puede leer el JSON enviado."));
  }

  private static TaskRevision precondition(List<String> matches, UUID task) {
    if (matches.size() != 1) throw invalid("If-Match", "INVALID_VALUE");
    var raw = matches.getFirst();
    if (!raw.matches(
        "\"task:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}:(0|[1-9][0-9]*)\""))
      throw invalid("If-Match", "INVALID_VALUE");
    var parts = raw.substring(1, raw.length() - 1).split(":");
    try {
      var id = UUID.fromString(parts[1]);
      if (!id.equals(task)) throw invalid("If-Match", "INVALID_VALUE");
      return new TaskRevision(id, Long.parseLong(parts[2]));
    } catch (NumberFormatException error) {
      throw invalid("If-Match", "INVALID_VALUE");
    }
  }

  private static UUID identifier(String raw, String field) {
    if (!raw.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
      throw invalid(field, "INVALID_FORMAT");
    return UUID.fromString(raw);
  }

  private static ValidationException invalid(String field, String code) {
    return new ValidationException(
        List.of(new FieldError(field, code, "Revisa el valor de este campo.")));
  }
}
