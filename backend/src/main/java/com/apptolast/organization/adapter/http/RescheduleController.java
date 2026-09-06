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

  public RescheduleController(
      ReadBlocksUseCase blocks, com.apptolast.organization.application.CancelBlockUseCase cancel) {
    this.blocks = blocks;
    this.cancel = cancel;
  }

  @PostMapping("/{blockId}/cancel")
  public ResponseEntity<?> cancel(
      Principal principal,
      @PathVariable String projectId,
      @PathVariable String taskId,
      @PathVariable String blockId,
      @RequestHeader org.springframework.http.HttpHeaders headers,
      @RequestParam org.springframework.util.MultiValueMap<String, String> parameters) {
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
      @PathVariable UUID projectId,
      @PathVariable UUID taskId,
      @PathVariable UUID blockId) {
    var state = blocks.state(principal.getName(), projectId, taskId, blockId);
    return ResponseEntity.ok()
        .eTag("\"block:" + blockId + ":" + state.version() + "\"")
        .body(
            new StateResponse(
                BlockController.BlockResponse.from(state.block()),
                state.status(),
                state.updatedAt()));
  }

  public record StateResponse(
      BlockController.BlockResponse block, String status, java.time.Instant updatedAt) {}
}
