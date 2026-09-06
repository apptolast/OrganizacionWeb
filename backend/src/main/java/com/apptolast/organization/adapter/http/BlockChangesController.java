package com.apptolast.organization.adapter.http;

import static com.apptolast.organization.adapter.http.BlockController.identifier;

import com.apptolast.organization.application.ReadBlockChangesUseCase;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/tasks/{taskId}/blocks/changes")
public final class BlockChangesController {
  private final ReadBlockChangesUseCase read;
  private final com.fasterxml.jackson.databind.ObjectMapper json;

  public BlockChangesController(
      ReadBlockChangesUseCase read, com.fasterxml.jackson.databind.ObjectMapper json) {
    this.read = read;
    this.json = json;
  }

  public record PageResponse(List<RescheduleController.ReceiptResponse> items, String nextCursor) {}

  @GetMapping
  public PageResponse list(
      @PathVariable String projectId,
      @PathVariable String taskId,
      Principal principal,
      @org.springframework.web.bind.annotation.RequestParam
          org.springframework.util.MultiValueMap<String, String> parameters)
      throws com.fasterxml.jackson.core.JsonProcessingException {
    if (parameters.keySet().stream().anyMatch(key -> !key.equals("cursor")))
      throw BlockController.invalid("query", "INVALID_VALUE");
    var project = identifier(projectId, "projectId");
    var task = identifier(taskId, "taskId");
    com.apptolast.organization.domain.BlockChangePosition after = null;
    if (parameters.containsKey("cursor")) {
      if (parameters.get("cursor").size() != 1)
        throw BlockController.invalid("cursor", "INVALID_VALUE");
      var decoded =
          BlockCursor.decode(
              json, parameters.getFirst("cursor"), project, task, "blockChanges", "occurredAt");
      after =
          new com.apptolast.organization.domain.BlockChangePosition(decoded.time(), decoded.id());
    }
    var page = read.list(principal.getName(), project, task, after);
    var next =
        page.next() == null
            ? null
            : java.util.Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                    json.writeValueAsBytes(
                        java.util.Map.of(
                            "collection",
                            "blockChanges",
                            "projectId",
                            project.toString(),
                            "taskId",
                            task.toString(),
                            "occurredAt",
                            page.next().occurredAt().toString(),
                            "id",
                            page.next().id().toString())));
    return new PageResponse(
        page.items().stream().map(RescheduleController.ReceiptResponse::from).toList(), next);
  }

  @GetMapping("/{changeId}")
  public RescheduleController.ReceiptResponse detail(
      @PathVariable String projectId,
      @PathVariable String taskId,
      @PathVariable String changeId,
      Principal principal,
      @org.springframework.web.bind.annotation.RequestParam
          org.springframework.util.MultiValueMap<String, String> parameters) {
    if (!parameters.isEmpty()) throw BlockController.invalid("query", "INVALID_VALUE");
    return RescheduleController.ReceiptResponse.from(
        read.detail(
            principal.getName(),
            identifier(projectId, "projectId"),
            identifier(taskId, "taskId"),
            identifier(changeId, "changeId")));
  }

  @GetMapping("/by-request/{requestKey}")
  public RescheduleController.ReceiptResponse byRequest(
      @PathVariable String projectId,
      @PathVariable String taskId,
      @PathVariable String requestKey,
      Principal principal,
      @org.springframework.web.bind.annotation.RequestParam
          org.springframework.util.MultiValueMap<String, String> parameters) {
    if (!parameters.isEmpty()) throw BlockController.invalid("query", "INVALID_VALUE");
    return RescheduleController.ReceiptResponse.from(
        read.byRequest(
            principal.getName(),
            identifier(projectId, "projectId"),
            identifier(taskId, "taskId"),
            identifier(requestKey, "requestKey")));
  }

  @org.springframework.web.bind.annotation.ExceptionHandler(
      com.apptolast.organization.application.BlockChangeNotFoundException.class)
  org.springframework.http.ResponseEntity<java.util.Map<String, Object>> missingReceipt() {
    return org.springframework.http.ResponseEntity.status(404)
        .contentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON)
        .body(ApiErrors.problem(404, "BLOCK_CHANGE_NOT_FOUND", "No se ha encontrado el cambio."));
  }
}
