package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.StartWorkSessionUseCase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.security.Principal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public final class WorkSessionController {
  private final StartWorkSessionUseCase start;
  private final ObjectMapper json;
  private final com.apptolast.organization.application.ReadWorkSessionsUseCase read;

  public WorkSessionController(
      StartWorkSessionUseCase start,
      ObjectMapper json,
      com.apptolast.organization.application.ReadWorkSessionsUseCase read) {
    this.start = start;
    this.json = json;
    this.read = read;
  }

  public record ActiveResponse(com.apptolast.organization.domain.SessionStart session) {}

  @ExceptionHandler(com.apptolast.organization.application.WorkSessionNotFoundException.class)
  ResponseEntity<?> missing() {
    return ResponseEntity.status(404)
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(
                404, "WORK_SESSION_NOT_FOUND", "No se ha encontrado la sesión de trabajo."));
  }

  @ExceptionHandler(
      com.apptolast.organization.application.WorkSessionIdempotencyConflictException.class)
  ResponseEntity<?> idempotency() {
    return ResponseEntity.status(409)
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(
                409,
                "IDEMPOTENCY_CONFLICT",
                "La clave corresponde a otra intención de inicio de trabajo."));
  }

  @ExceptionHandler(com.apptolast.organization.application.WorkSessionTimeOutOfRangeException.class)
  ResponseEntity<?> timeOutOfRange() {
    return ResponseEntity.status(409)
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(
                409,
                "WORK_SESSION_TIME_OUT_OF_RANGE",
                "No se puede representar el inicio y el fin previsto de la sesión."));
  }

  @ExceptionHandler(com.apptolast.organization.application.ProjectCompletedException.class)
  ResponseEntity<?> completedProject() {
    return ResponseEntity.status(409)
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(
                409, "PROJECT_COMPLETED", "Reabre el proyecto antes de iniciar trabajo."));
  }

  @ExceptionHandler(com.apptolast.organization.application.TaskCompletedException.class)
  ResponseEntity<?> completedTask() {
    return ResponseEntity.status(409)
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(409, "TASK_COMPLETED", "Reabre la tarea antes de iniciar trabajo."));
  }

  @ExceptionHandler(com.apptolast.organization.application.WorkSessionAlreadyActiveException.class)
  ResponseEntity<?> alreadyActive(
      com.apptolast.organization.application.WorkSessionAlreadyActiveException error) {
    var body =
        ApiErrors.problem(
            409, "WORK_SESSION_ALREADY_ACTIVE", "Ya existe una sesión de trabajo activa.");
    body.put("sessionId", error.sessionId());
    return ResponseEntity.status(409)
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(body);
  }

  @GetMapping("/api/v1/work-sessions/active")
  public ActiveResponse active(
      Principal principal,
      @RequestParam org.springframework.util.MultiValueMap<String, String> parameters) {
    if (!parameters.isEmpty()) throw BlockController.invalid("query", "INVALID_VALUE");
    return new ActiveResponse(read.active(principal.getName()).orElse(null));
  }

  @GetMapping("/api/v1/work-sessions/{id}")
  public com.apptolast.organization.domain.SessionStart detail(
      Principal principal,
      @PathVariable String id,
      @RequestParam org.springframework.util.MultiValueMap<String, String> parameters) {
    if (!parameters.isEmpty()) throw BlockController.invalid("query", "INVALID_VALUE");
    return read.detail(principal.getName(), BlockController.identifier(id, "id"));
  }

  @GetMapping("/api/v1/work-sessions/by-request/{requestKey}")
  public com.apptolast.organization.domain.SessionStart byRequest(
      Principal principal,
      @PathVariable String requestKey,
      @RequestParam org.springframework.util.MultiValueMap<String, String> parameters) {
    if (!parameters.isEmpty()) throw BlockController.invalid("query", "INVALID_VALUE");
    return read.byRequest(
        principal.getName(), BlockController.identifier(requestKey, "requestKey"));
  }

  @PostMapping(
      value = "/api/v1/projects/{projectId}/tasks/{taskId}/work-sessions",
      consumes = "application/json")
  public ResponseEntity<?> start(
      Principal principal,
      @PathVariable String projectId,
      @PathVariable String taskId,
      @RequestHeader org.springframework.http.HttpHeaders headers,
      @RequestBody(required = false) String raw,
      @RequestParam org.springframework.util.MultiValueMap<String, String> parameters)
      throws JsonProcessingException {
    if (!parameters.isEmpty()) throw BlockController.invalid("query", "INVALID_VALUE");
    var project = BlockController.identifier(projectId, "projectId");
    var task = BlockController.identifier(taskId, "taskId");
    var keys = headers.get("Idempotency-Key");
    if (keys == null) throw BlockController.invalid("Idempotency-Key", "REQUIRED");
    if (keys.size() != 1) throw BlockController.invalid("Idempotency-Key", "INVALID_VALUE");
    var key = BlockController.identifier(keys.getFirst(), "Idempotency-Key");
    if (raw == null || raw.isBlank())
      throw new com.fasterxml.jackson.core.JsonParseException(null, "Body required");
    var body =
        json.reader()
            .with(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .readTree(raw);
    if (body == null || !body.isObject()) throw BlockController.invalid("body", "INVALID_TYPE");
    var fields = new java.util.TreeSet<String>();
    body.fieldNames().forEachRemaining(fields::add);
    fields.remove("plannedMinutes");
    if (!fields.isEmpty()) throw BlockController.invalid(fields.first(), "UNKNOWN_FIELD");
    var minutes = body.get("plannedMinutes");
    if (minutes == null || minutes.isNull())
      throw BlockController.invalid("plannedMinutes", "REQUIRED");
    if (!minutes.isIntegralNumber())
      throw BlockController.invalid("plannedMinutes", "INVALID_TYPE");
    if (!minutes.canConvertToInt() || minutes.intValue() < 1 || minutes.intValue() > 1440)
      throw BlockController.invalid("plannedMinutes", "OUT_OF_RANGE");
    var result = start.start(principal.getName(), project, task, key, minutes.intValue());
    return ResponseEntity.status(result.replayed() ? 200 : 201)
        .location(URI.create("/api/v1/work-sessions/" + result.session().id()))
        .body(result.session());
  }
}
