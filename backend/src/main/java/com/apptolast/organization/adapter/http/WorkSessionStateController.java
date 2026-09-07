package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.*;
import java.net.URI;
import java.security.Principal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public final class WorkSessionStateController {
  @ExceptionHandler(WorkSessionIdempotencyConflictException.class)
  ResponseEntity<?> idempotency() {
    return ResponseEntity.status(409)
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(
                409,
                "IDEMPOTENCY_CONFLICT",
                "La clave corresponde a otra intención de cambio de sesión."));
  }

  @ExceptionHandler(WorkSessionTransitionException.class)
  ResponseEntity<?> transition(WorkSessionTransitionException error) {
    String title =
        switch (error.code()) {
          case "PRECONDITION_FAILED" -> "La sesión ha cambiado. Consulta su estado actual.";
          case "WORK_SESSION_STATE_CONFLICT" -> "El estado de la sesión no permite esta acción.";
          case "WORK_SESSION_REVISION_EXHAUSTED" -> "La sesión no admite más revisiones.";
          case "WORK_SESSION_TIME_OUT_OF_RANGE" ->
              "No se puede registrar la transición en ese instante.";
          default -> throw error;
        };
    int status = error.code().equals("PRECONDITION_FAILED") ? 412 : 409;
    return ResponseEntity.status(status)
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(ApiErrors.problem(status, error.code(), title));
  }

  @ExceptionHandler(WorkSessionChangeNotFoundException.class)
  ResponseEntity<?> missingChange() {
    return ResponseEntity.status(404)
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(
                404, "WORK_SESSION_CHANGE_NOT_FOUND", "No se ha encontrado el cambio de sesión."));
  }

  @ExceptionHandler(WorkSessionNotFoundException.class)
  ResponseEntity<?> missingSession() {
    return ResponseEntity.status(404)
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(
                404, "WORK_SESSION_NOT_FOUND", "No se ha encontrado la sesión de trabajo."));
  }

  private final ChangeWorkSessionUseCase change;
  private final ReadWorkSessionStateUseCase states;
  private final ReadWorkSessionChangesUseCase receipts;
  private final com.fasterxml.jackson.databind.ObjectMapper json;

  public WorkSessionStateController(
      ChangeWorkSessionUseCase change,
      ReadWorkSessionStateUseCase states,
      ReadWorkSessionChangesUseCase receipts,
      com.fasterxml.jackson.databind.ObjectMapper json) {
    this.change = change;
    this.states = states;
    this.receipts = receipts;
    this.json = json;
  }

  @GetMapping("/api/v1/work-session-changes/{id}")
  public ReceiptResponse receipt(
      Principal principal,
      @PathVariable String id,
      @RequestParam org.springframework.util.MultiValueMap<String, String> parameters) {
    if (!parameters.isEmpty()) throw BlockController.invalid("query", "INVALID_VALUE");
    return ReceiptResponse.from(
        receipts.detail(principal.getName(), BlockController.identifier(id, "id")));
  }

  @GetMapping("/api/v1/work-session-changes/by-request/{key}")
  public ReceiptResponse byRequest(
      Principal principal,
      @PathVariable String key,
      @RequestParam org.springframework.util.MultiValueMap<String, String> parameters) {
    if (!parameters.isEmpty()) throw BlockController.invalid("query", "INVALID_VALUE");
    return ReceiptResponse.from(
        receipts.byRequest(principal.getName(), BlockController.identifier(key, "requestKey")));
  }

  @GetMapping("/api/v1/work-sessions/{id}/state")
  public ResponseEntity<?> state(
      Principal principal,
      @PathVariable String id,
      @RequestParam org.springframework.util.MultiValueMap<String, String> parameters) {
    if (!parameters.isEmpty()) throw BlockController.invalid("query", "INVALID_VALUE");
    var snapshot = states.read(principal.getName(), BlockController.identifier(id, "id"));
    var state = snapshot.state();
    return ResponseEntity.ok()
        .header(
            "Work-Session-Revision",
            "work-session-" + state.session().id() + "-" + state.revision())
        .body(
            new SnapshotResponse(
                StateResponse.from(state),
                snapshot.serverNow(),
                Long.toString(snapshot.netMicroseconds())));
  }

  public record SnapshotResponse(StateResponse state, Instant serverNow, String netMicroseconds) {}

  @PostMapping(
      value = "/api/v1/work-sessions/{id}/{action:pause|resume}",
      consumes = "application/json")
  public ResponseEntity<?> change(
      Principal principal,
      @PathVariable String id,
      @PathVariable String action,
      @RequestBody(required = false) String raw,
      @RequestHeader org.springframework.http.HttpHeaders headers,
      @RequestHeader(value = "Work-Session-Revision", required = false) String token,
      @RequestParam org.springframework.util.MultiValueMap<String, String> parameters)
      throws com.fasterxml.jackson.core.JsonProcessingException {
    if (!parameters.isEmpty()) throw BlockController.invalid("query", "INVALID_VALUE");
    var sessionId = BlockController.identifier(id, "id");
    var keys = headers.get("Idempotency-Key");
    if (keys == null) throw BlockController.invalid("Idempotency-Key", "REQUIRED");
    if (keys.size() != 1) throw BlockController.invalid("Idempotency-Key", "INVALID_VALUE");
    var key = keys.getFirst();
    var requestKey = BlockController.identifier(key, "Idempotency-Key");
    if (token == null)
      return ResponseEntity.status(428)
          .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
          .body(
              ApiErrors.problem(
                  428, "PRECONDITION_REQUIRED", "Envía la revisión actual requerida."));
    if (!token.matches(
        "work-session-[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}-[1-9][0-9]*"))
      throw BlockController.invalid("Work-Session-Revision", "INVALID_VALUE");
    WorkSessionRevision expected;
    try {
      expected =
          new WorkSessionRevision(
              UUID.fromString(token.substring(13, 49)), Long.parseLong(token.substring(50)));
    } catch (NumberFormatException invalid) {
      throw BlockController.invalid("Work-Session-Revision", "INVALID_VALUE");
    }
    if (raw == null || raw.isBlank())
      throw new com.fasterxml.jackson.core.JsonParseException(null, "Body required");
    var body =
        json.reader()
            .with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .with(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .readTree(raw);
    if (body == null || !body.isObject()) throw BlockController.invalid("body", "INVALID_TYPE");
    var fields = new java.util.TreeSet<String>();
    body.fieldNames().forEachRemaining(fields::add);
    if (!fields.isEmpty()) throw BlockController.invalid(fields.first(), "UNKNOWN_FIELD");
    var confirmed =
        action.equals("pause")
            ? change.pause(principal.getName(), sessionId, requestKey, expected)
            : change.resume(principal.getName(), sessionId, requestKey, expected);
    return ResponseEntity.status(confirmed.replayed() ? 200 : 201)
        .location(URI.create("/api/v1/work-session-changes/" + confirmed.receipt().id()))
        .body(ReceiptResponse.from(confirmed.receipt()));
  }

  public record StateResponse(
      SessionStart session,
      String status,
      String revision,
      Instant changedAt,
      String workedMicroseconds,
      Instant runningSince) {
    static StateResponse from(WorkSessionState state) {
      return new StateResponse(
          state.session(),
          state.status(),
          Long.toString(state.revision()),
          state.changedAt(),
          Long.toString(state.workedMicroseconds()),
          state.runningSince());
    }
  }

  public record ReceiptResponse(
      UUID id,
      UUID sessionId,
      String action,
      Instant occurredAt,
      StateResponse before,
      StateResponse after) {
    static ReceiptResponse from(WorkSessionTransitionReceipt receipt) {
      return new ReceiptResponse(
          receipt.id(),
          receipt.sessionId(),
          receipt.action(),
          receipt.occurredAt(),
          StateResponse.from(receipt.before()),
          StateResponse.from(receipt.after()));
    }
  }
}
