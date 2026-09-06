package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.PlanBlockUseCase;
import com.apptolast.organization.domain.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.time.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/tasks/{taskId}/blocks")
public final class BlockController {
  private final PlanBlockUseCase plan;
  private final ObjectMapper json;
  private final com.apptolast.organization.application.ReadBlocksUseCase read;

  public record PageResponse(List<BlockResponse> items, String nextCursor) {}

  @GetMapping
  public PageResponse list(
      @PathVariable String projectId,
      @PathVariable String taskId,
      Principal principal,
      @RequestParam org.springframework.util.MultiValueMap<String, String> parameters)
      throws java.io.IOException {
    if (parameters.keySet().stream().anyMatch(key -> !key.equals("cursor")))
      throw invalid("query", "INVALID_VALUE");
    var project = identifier(projectId, "projectId");
    var task = identifier(taskId, "taskId");
    BlockPosition after = null;
    if (parameters.containsKey("cursor")) {
      if (parameters.get("cursor").size() != 1) throw invalid("cursor", "INVALID_VALUE");
      after = cursor(parameters.getFirst("cursor"), project, task);
    }
    var page = read.list(principal.getName(), project, task, after);
    String next =
        page.next() == null
            ? null
            : Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                    json.writeValueAsBytes(
                        Map.of(
                            "collection",
                            "blocks",
                            "projectId",
                            project.toString(),
                            "taskId",
                            task.toString(),
                            "createdAt",
                            page.next().createdAt().toString(),
                            "id",
                            page.next().id().toString())));
    return new PageResponse(page.items().stream().map(BlockResponse::from).toList(), next);
  }

  private BlockPosition cursor(String value, UUID project, UUID task) {
    var position = BlockCursor.decode(json, value, project, task, "blocks", "createdAt");
    return new BlockPosition(position.time(), position.id());
  }

  public BlockController(
      PlanBlockUseCase plan,
      ObjectMapper json,
      com.apptolast.organization.application.ReadBlocksUseCase read) {
    this.read = read;
    this.plan = plan;
    this.json = json;
  }

  @GetMapping("/{blockId}")
  public BlockResponse detail(
      @PathVariable String projectId,
      @PathVariable String taskId,
      @PathVariable String blockId,
      Principal principal,
      @RequestParam org.springframework.util.MultiValueMap<String, String> parameters) {
    if (!parameters.isEmpty()) throw invalid("query", "INVALID_VALUE");
    return BlockResponse.from(
        read.detail(
            principal.getName(),
            identifier(projectId, "projectId"),
            identifier(taskId, "taskId"),
            identifier(blockId, "blockId")));
  }

  @GetMapping("/by-request/{requestKey}")
  public BlockResponse byRequest(
      @PathVariable String projectId,
      @PathVariable String taskId,
      @PathVariable String requestKey,
      Principal principal,
      @RequestParam org.springframework.util.MultiValueMap<String, String> parameters) {
    if (!parameters.isEmpty()) throw invalid("query", "INVALID_VALUE");
    return BlockResponse.from(
        read.byRequest(
            principal.getName(),
            identifier(projectId, "projectId"),
            identifier(taskId, "taskId"),
            identifier(requestKey, "requestKey")));
  }

  @ExceptionHandler(com.apptolast.organization.application.BlockNotFoundException.class)
  org.springframework.http.ResponseEntity<Map<String, Object>> missingBlock() {
    return org.springframework.http.ResponseEntity.status(404)
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(ApiErrors.problem(404, "BLOCK_NOT_FOUND", "No se ha encontrado el bloque."));
  }

  public record PreviewResponse(
      String objective,
      String zoneId,
      Instant startAt,
      Instant endAt,
      String startOffset,
      String endOffset,
      int durationMinutes,
      String availabilityEtag,
      String budgetZoneId,
      List<BudgetDay> days) {}

  @PostMapping(value = "/preview", consumes = "application/json")
  public PreviewResponse preview(
      @PathVariable String projectId,
      @PathVariable String taskId,
      @RequestBody(required = false) String raw,
      Principal principal,
      @RequestParam org.springframework.util.MultiValueMap<String, String> parameters)
      throws JsonProcessingException {
    if (!parameters.isEmpty()) throw invalid("query", "INVALID_VALUE");
    var project = identifier(projectId, "projectId");
    var task = identifier(taskId, "taskId");
    var request = request(raw, false);
    var preview = plan.preview(principal.getName(), project, task, request);
    var time = preview.time();
    var revision = preview.availabilityRevision();
    return new PreviewResponse(
        request.objective(),
        request.zoneId(),
        time.startAt(),
        time.endAt(),
        time.startOffset().getId(),
        time.endOffset().getId(),
        time.durationMinutes(),
        "\"availability:" + revision.id() + ":" + revision.version() + "\"",
        preview.budgetZoneId(),
        preview.days());
  }

  public record BlockResponse(
      UUID id,
      UUID projectId,
      UUID taskId,
      String objective,
      Instant startAt,
      Instant endAt,
      String zoneId,
      int durationMinutes,
      Instant createdAt) {
    static BlockResponse from(PlannedBlock block) {
      return new BlockResponse(
          block.id(),
          block.projectId(),
          block.taskId(),
          block.request().objective(),
          block.time().startAt(),
          block.time().endAt(),
          block.request().zoneId(),
          block.time().durationMinutes(),
          block.createdAt());
    }
  }

  @PostMapping(consumes = "application/json")
  public org.springframework.http.ResponseEntity<?> create(
      @PathVariable String projectId,
      @PathVariable String taskId,
      @RequestBody(required = false) String raw,
      Principal principal,
      @RequestParam org.springframework.util.MultiValueMap<String, String> parameters,
      @RequestHeader(value = "Availability-Revision", required = false) List<String> revisions,
      @RequestHeader(value = "Idempotency-Key", required = false) List<String> keys)
      throws JsonProcessingException {
    if (!parameters.isEmpty()) throw invalid("query", "INVALID_VALUE");
    var project = identifier(projectId, "projectId");
    var task = identifier(taskId, "taskId");
    if (revisions == null)
      return org.springframework.http.ResponseEntity.status(428)
          .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
          .body(
              ApiErrors.problem(
                  428, "PRECONDITION_REQUIRED", "Envía la revisión actual de disponibilidad."));
    var expected = revision(revisions);
    if (keys == null
        || keys.size() != 1
        || !keys.getFirst().matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
      throw invalid("Idempotency-Key", "INVALID_VALUE");
    var key = UUID.fromString(keys.getFirst());
    var request = request(raw, true);
    var result = plan.create(principal.getName(), project, task, key, expected, request);
    var block = result.block();
    return org.springframework.http.ResponseEntity.status(result.replayed() ? 200 : 201)
        .location(
            java.net.URI.create(
                "/api/v1/projects/" + project + "/tasks/" + task + "/blocks/" + block.id()))
        .body(BlockResponse.from(block));
  }

  private BlockRequest request(String raw, boolean creation) throws JsonProcessingException {
    if (raw == null || raw.isBlank()) throw new MalformedBody();
    var body =
        json.reader()
            .with(
                com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
            .with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .readTree(raw);
    if (body == null || !body.isObject()) throw invalid("body", "INVALID_TYPE");
    body.fieldNames()
        .forEachRemaining(
            field -> {
              if (!Set.of(
                          "objective",
                          "startLocal",
                          "endLocal",
                          "zoneId",
                          "startOffset",
                          "endOffset")
                      .contains(field)
                  && !(creation && field.equals("allowOverBudget")))
                throw invalid(field, "UNKNOWN_FIELD");
            });
    boolean allowOverBudget = false;
    if (creation) {
      var allow = body.get("allowOverBudget");
      if (allow == null || allow.isNull()) throw invalid("allowOverBudget", "REQUIRED");
      if (!allow.isBoolean()) throw invalid("allowOverBudget", "INVALID_TYPE");
      allowOverBudget = allow.booleanValue();
    }
    return new BlockRequest(
        string(body, "objective", false),
        local(body, "startLocal"),
        local(body, "endLocal"),
        string(body, "zoneId", false),
        offset(body, "startOffset", !creation),
        offset(body, "endOffset", !creation),
        allowOverBudget);
  }

  static AvailabilityRevision revision(List<String> values) {
    if (values.size() != 1
        || !values
            .getFirst()
            .matches(
                "\"availability:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}:(0|[1-9][0-9]*)\""))
      throw invalid("Availability-Revision", "INVALID_VALUE");
    var value = values.getFirst();
    var parts = value.substring(1, value.length() - 1).split(":");
    try {
      return new AvailabilityRevision(UUID.fromString(parts[1]), Long.parseLong(parts[2]));
    } catch (NumberFormatException error) {
      throw invalid("Availability-Revision", "INVALID_VALUE");
    }
  }

  private static final class MalformedBody extends RuntimeException {}

  @ExceptionHandler({JsonProcessingException.class, MalformedBody.class})
  org.springframework.http.ResponseEntity<Map<String, Object>> malformed() {
    return org.springframework.http.ResponseEntity.badRequest()
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(ApiErrors.problem(400, "MALFORMED_JSON", "No se puede leer el JSON enviado."));
  }

  @ExceptionHandler(com.apptolast.organization.application.BlockIdempotencyConflictException.class)
  org.springframework.http.ResponseEntity<Map<String, Object>> idempotency() {
    return org.springframework.http.ResponseEntity.status(409)
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(
            ApiErrors.problem(
                409, "IDEMPOTENCY_CONFLICT", "La clave corresponde a otra intención de bloque."));
  }

  static UUID identifier(String value, String field) {
    if (!value.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
      throw invalid(field, "INVALID_FORMAT");
    return UUID.fromString(value);
  }

  static ZoneOffset offset(
      com.fasterxml.jackson.databind.JsonNode body, String field, boolean nullable) {
    var value = string(body, field, nullable);
    if (value == null) return null;
    try {
      var offset = ZoneOffset.of(value);
      if (!offset.getId().equals(value)) throw invalid(field, "INVALID_FORMAT");
      return offset;
    } catch (DateTimeException error) {
      throw invalid(field, "INVALID_FORMAT");
    }
  }

  static LocalDateTime local(com.fasterxml.jackson.databind.JsonNode body, String field) {
    var value = string(body, field, false);
    if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}"))
      throw invalid(field, "INVALID_FORMAT");
    try {
      return LocalDateTime.parse(value);
    } catch (DateTimeException error) {
      throw invalid(field, "INVALID_FORMAT");
    }
  }

  static String string(
      com.fasterxml.jackson.databind.JsonNode body, String field, boolean nullable) {
    var value = body.get(field);
    if (value == null || (value.isNull() && !nullable)) throw invalid(field, "REQUIRED");
    if (value.isNull()) return null;
    if (!value.isTextual()) throw invalid(field, "INVALID_TYPE");
    return value.textValue();
  }

  static ValidationException invalid(String field, String code) {
    return new ValidationException(
        List.of(new FieldError(field, code, "Revisa el valor de este campo.")));
  }
}
