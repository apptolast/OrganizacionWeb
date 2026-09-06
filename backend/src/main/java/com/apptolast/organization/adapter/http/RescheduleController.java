package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.ReadBlocksUseCase;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/tasks/{taskId}/blocks")
public final class RescheduleController {
  private final ReadBlocksUseCase blocks;
  private final com.apptolast.organization.application.CancelBlockUseCase cancel;
  private final com.fasterxml.jackson.databind.ObjectMapper json;
  private final com.apptolast.organization.application.MoveBlockUseCase move;

  public RescheduleController(
      ReadBlocksUseCase blocks,
      com.apptolast.organization.application.CancelBlockUseCase cancel,
      com.fasterxml.jackson.databind.ObjectMapper json,
      com.apptolast.organization.application.MoveBlockUseCase move) {
    this.blocks = blocks;
    this.cancel = cancel;
    this.json = json;
    this.move = move;
  }

  @PostMapping(value = "/{blockId}/reschedule", consumes = "application/json")
  public ResponseEntity<?> move(
      Principal principal,
      @PathVariable String projectId,
      @PathVariable String taskId,
      @PathVariable String blockId,
      @RequestBody(required = false) String raw,
      @RequestHeader org.springframework.http.HttpHeaders headers,
      @RequestParam org.springframework.util.MultiValueMap<String, String> parameters)
      throws com.fasterxml.jackson.core.JsonProcessingException {
    if (!parameters.isEmpty()) throw BlockController.invalid("query", "INVALID_VALUE");
    var project = BlockController.identifier(projectId, "projectId");
    var task = BlockController.identifier(taskId, "taskId");
    var block = BlockController.identifier(blockId, "blockId");
    if (headers.get("If-Match") == null) return requiredRevision();
    long version = revision(headers.get("If-Match"), block);
    if (headers.get("Availability-Revision") == null) return requiredRevision();
    var availability = BlockController.revision(headers.get("Availability-Revision"));
    var keys = headers.get("Idempotency-Key");
    if (keys == null) throw BlockController.invalid("Idempotency-Key", "REQUIRED");
    if (keys.size() != 1) throw BlockController.invalid("Idempotency-Key", "INVALID_VALUE");
    var key = BlockController.identifier(keys.getFirst(), "Idempotency-Key");
    var request = moveRequest(raw, true);
    var result =
        move.move(principal.getName(), project, task, block, key, version, availability, request);
    return ResponseEntity.status(result.replayed() ? 200 : 201)
        .location(
            java.net.URI.create(
                "/api/v1/projects/"
                    + project
                    + "/tasks/"
                    + task
                    + "/blocks/changes/"
                    + result.receipt().id()))
        .body(ReceiptResponse.from(result.receipt()));
  }

  private ResponseEntity<?> requiredRevision() {
    return ResponseEntity.status(428)
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(428, "PRECONDITION_REQUIRED", "Envía la revisión actual requerida."));
  }

  @PostMapping(value = "/{blockId}/reschedule/preview", consumes = "application/json")
  public ResponseEntity<?> preview(
      Principal principal,
      @PathVariable String projectId,
      @PathVariable String taskId,
      @PathVariable String blockId,
      @RequestBody(required = false) String raw,
      @RequestHeader org.springframework.http.HttpHeaders headers,
      @RequestParam org.springframework.util.MultiValueMap<String, String> parameters)
      throws com.fasterxml.jackson.core.JsonProcessingException {
    if (!parameters.isEmpty()) throw BlockController.invalid("query", "INVALID_VALUE");
    var project = BlockController.identifier(projectId, "projectId");
    var task = BlockController.identifier(taskId, "taskId");
    var block = BlockController.identifier(blockId, "blockId");
    if (headers.get("If-Match") == null) return requiredRevision();
    long version = revision(headers.get("If-Match"), block);
    var preview =
        move.preview(principal.getName(), project, task, block, version, moveRequest(raw, false));
    var time = preview.time();
    var availability = preview.availabilityRevision();
    return ResponseEntity.ok()
        .eTag("\"block:" + block + ":" + version + "\"")
        .body(
            new BlockController.PreviewResponse(
                preview.request().objective(),
                preview.request().zoneId(),
                time.startAt(),
                time.endAt(),
                time.startOffset().getId(),
                time.endOffset().getId(),
                time.durationMinutes(),
                "\"availability:" + availability.id() + ":" + availability.version() + "\"",
                preview.budgetZoneId(),
                preview.days()));
  }

  private com.apptolast.organization.domain.BlockMoveRequest moveRequest(String raw, boolean commit)
      throws com.fasterxml.jackson.core.JsonProcessingException {
    if (raw == null || raw.isBlank())
      throw new com.fasterxml.jackson.core.JsonParseException(null, "Empty JSON body");
    var body =
        json.reader()
            .with(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .readTree(raw);
    if (body == null || !body.isObject()) throw BlockController.invalid("body", "INVALID_TYPE");
    body.fieldNames()
        .forEachRemaining(
            field -> {
              if (!java.util.Set.of("startLocal", "endLocal", "zoneId", "startOffset", "endOffset")
                      .contains(field)
                  && !(commit && field.equals("allowOverBudget")))
                throw BlockController.invalid(field, "UNKNOWN_FIELD");
            });
    boolean allow = false;
    if (commit) {
      var value = body.get("allowOverBudget");
      if (value == null || value.isNull())
        throw BlockController.invalid("allowOverBudget", "REQUIRED");
      if (!value.isBoolean()) throw BlockController.invalid("allowOverBudget", "INVALID_TYPE");
      allow = value.booleanValue();
    }
    return new com.apptolast.organization.domain.BlockMoveRequest(
        BlockController.local(body, "startLocal"), BlockController.local(body, "endLocal"),
        BlockController.string(body, "zoneId", false),
            BlockController.offset(body, "startOffset", !commit),
        BlockController.offset(body, "endOffset", !commit), allow);
  }

  @PostMapping("/{blockId}/cancel")
  public ResponseEntity<?> cancel(
      Principal principal,
      @PathVariable String projectId,
      @PathVariable String taskId,
      @PathVariable String blockId,
      @RequestBody(required = false) String raw,
      @RequestHeader org.springframework.http.HttpHeaders headers,
      @RequestParam org.springframework.util.MultiValueMap<String, String> parameters)
      throws com.fasterxml.jackson.core.JsonProcessingException {
    if (!parameters.isEmpty()) throw BlockController.invalid("query", "INVALID_VALUE");
    var project = BlockController.identifier(projectId, "projectId");
    var task = BlockController.identifier(taskId, "taskId");
    var block = BlockController.identifier(blockId, "blockId");
    var matches = headers.get("If-Match");
    if (matches == null)
      return ResponseEntity.status(428)
          .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
          .body(
              ApiErrors.problem(
                  428, "PRECONDITION_REQUIRED", "Envía la revisión actual del bloque."));
    long version = revision(matches, block);
    var keys = headers.get("Idempotency-Key");
    if (keys == null) throw BlockController.invalid("Idempotency-Key", "REQUIRED");
    if (keys.size() != 1) throw BlockController.invalid("Idempotency-Key", "INVALID_VALUE");
    var key = BlockController.identifier(keys.getFirst(), "Idempotency-Key");
    if (raw == null || raw.isBlank())
      throw new com.fasterxml.jackson.core.JsonParseException(null, "Empty JSON body");
    var body = json.readTree(raw);
    if (!body.isObject()) throw BlockController.invalid("body", "INVALID_TYPE");
    body.fieldNames()
        .forEachRemaining(
            field -> {
              throw BlockController.invalid(field, "UNKNOWN_FIELD");
            });
    var result = cancel.cancel(principal.getName(), project, task, block, key, version);
    var receipt = result.receipt();
    return ResponseEntity.status(result.replayed() ? 200 : 201)
        .location(
            java.net.URI.create(
                "/api/v1/projects/"
                    + projectId
                    + "/tasks/"
                    + taskId
                    + "/blocks/changes/"
                    + receipt.id()))
        .body(ReceiptResponse.from(receipt));
  }

  private static long revision(java.util.List<String> values, UUID block) {
    if (values.size() != 1
        || !values
            .getFirst()
            .matches(
                "\"block:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}:[1-9][0-9]*\""))
      throw BlockController.invalid("If-Match", "INVALID_VALUE");
    var parts = values.getFirst().substring(1, values.getFirst().length() - 1).split(":");
    if (!UUID.fromString(parts[1]).equals(block))
      throw BlockController.invalid("If-Match", "INVALID_VALUE");
    try {
      return Long.parseLong(parts[2]);
    } catch (NumberFormatException error) {
      throw BlockController.invalid("If-Match", "INVALID_VALUE");
    }
  }

  @ExceptionHandler(com.apptolast.organization.domain.BlockStateException.class)
  ResponseEntity<java.util.Map<String, Object>> blockStateConflict(
      com.apptolast.organization.domain.BlockStateException error) {
    int status = error.getMessage().equals("BLOCK_CONFLICT") ? 412 : 409;
    String title =
        status == 412
            ? "El bloque tiene una revisión más reciente."
            : "El bloque no admite este cambio.";
    return ResponseEntity.status(status)
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(ApiErrors.problem(status, error.getMessage(), title));
  }

  @ExceptionHandler(com.apptolast.organization.application.BlockIdempotencyConflictException.class)
  ResponseEntity<java.util.Map<String, Object>> idempotencyConflict() {
    return ResponseEntity.status(409)
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(
                409, "IDEMPOTENCY_CONFLICT", "La clave corresponde a otra intención de bloque."));
  }

  public record ReceiptResponse(
      UUID id,
      UUID blockId,
      String kind,
      String revision,
      java.time.Instant occurredAt,
      BlockController.BlockResponse before,
      BlockController.BlockResponse after) {
    static ReceiptResponse from(com.apptolast.organization.domain.BlockChangeReceipt receipt) {
      return new ReceiptResponse(
          receipt.id(),
          receipt.blockId(),
          receipt.kind(),
          "\"block:" + receipt.blockId() + ":" + receipt.version() + "\"",
          receipt.occurredAt(),
          BlockController.BlockResponse.from(receipt.before()),
          receipt.after() == null ? null : BlockController.BlockResponse.from(receipt.after()));
    }
  }

  @GetMapping("/{blockId}/state")
  public ResponseEntity<StateResponse> state(
      Principal principal,
      @PathVariable String projectId,
      @PathVariable String taskId,
      @PathVariable String blockId,
      @RequestParam org.springframework.util.MultiValueMap<String, String> parameters) {
    if (!parameters.isEmpty()) throw BlockController.invalid("query", "INVALID_VALUE");
    var project = BlockController.identifier(projectId, "projectId");
    var task = BlockController.identifier(taskId, "taskId");
    var block = BlockController.identifier(blockId, "blockId");
    var state = blocks.state(principal.getName(), project, task, block);
    return ResponseEntity.ok()
        .eTag("\"block:" + block + ":" + state.version() + "\"")
        .body(
            new StateResponse(
                BlockController.BlockResponse.from(state.block()),
                state.status(),
                state.updatedAt()));
  }

  public record StateResponse(
      BlockController.BlockResponse block, String status, java.time.Instant updatedAt) {}
}
