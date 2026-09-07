package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.*;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/history")
public final class HistoryController {
  private final ReadHistoryUseCase history;

  private final com.fasterxml.jackson.databind.ObjectMapper json;

  public HistoryController(
      ReadHistoryUseCase history, com.fasterxml.jackson.databind.ObjectMapper json) {
    this.json = json;
    this.history = history;
  }

  public record PageResponse(List<?> items, String nextCursor) {}

  @GetMapping
  public PageResponse list(
      Principal principal,
      @RequestParam org.springframework.util.MultiValueMap<String, String> parameters)
      throws com.fasterxml.jackson.core.JsonProcessingException {
    if (parameters.keySet().stream()
        .anyMatch(
            key ->
                !List.of("category", "projectId", "taskId", "from", "to", "cursor").contains(key)))
      throw BlockController.invalid("query", "INVALID_VALUE");
    parameters.forEach(
        (field, values) -> {
          if (values.size() != 1) throw BlockController.invalid(field, "INVALID_VALUE");
        });
    if (parameters.containsKey("category")
        && !List.of("sessions", "task-status", "planning")
            .contains(parameters.getFirst("category")))
      throw BlockController.invalid("category", "INVALID_VALUE");
    var filters =
        new HistoryFilters(
            parameters.getFirst("category"),
            identifier(parameters, "projectId"),
            identifier(parameters, "taskId"),
            date(parameters, "from"),
            date(parameters, "to"));
    if (filters.taskId() != null && filters.projectId() == null)
      throw BlockController.invalid("taskId", "INVALID_VALUE");
    if (filters.from() != null && filters.to() != null && filters.from().isAfter(filters.to()))
      throw BlockController.invalid("to", "INVALID_VALUE");
    var page =
        history.list(
            principal.getName(),
            filters,
            parameters.containsKey("cursor")
                ? HistoryCursorCodec.decode(json, parameters.getFirst("cursor"))
                : null);
    return new PageResponse(
        page.items().stream().map(HistoryController::entry).toList(),
        HistoryCursorCodec.encode(json, page.next()));
  }

  private static HistoryEntry<?> entry(HistoryEntry<?> entry) {
    Object detail = entry.details();
    if (detail instanceof com.apptolast.organization.domain.TaskHistoryEntry task)
      detail =
          new TaskHistoryController.HistoryEntryResponse(
              task.id(), task.fromStatus(), task.toStatus(), task.occurredAt());
    if (detail instanceof com.apptolast.organization.domain.PlannedBlock block)
      detail = BlockController.BlockResponse.from(block);
    if (detail instanceof com.apptolast.organization.domain.BlockChangeReceipt receipt)
      detail = RescheduleController.ReceiptResponse.from(receipt);
    if (detail
        instanceof com.apptolast.organization.application.WorkSessionTransitionReceipt receipt)
      detail = WorkSessionStateController.ReceiptResponse.from(receipt);
    return new HistoryEntry<>(
        entry.id(),
        entry.type(),
        entry.occurredAt(),
        entry.projectId(),
        entry.projectName(),
        entry.taskId(),
        entry.taskTitle(),
        detail);
  }

  private static java.util.UUID identifier(
      org.springframework.util.MultiValueMap<String, String> parameters, String field) {
    return parameters.containsKey(field)
        ? BlockController.identifier(parameters.getFirst(field), field)
        : null;
  }

  private static java.time.LocalDate date(
      org.springframework.util.MultiValueMap<String, String> parameters, String field) {
    return parameters.containsKey(field) ? date(parameters.getFirst(field), field) : null;
  }

  static java.time.LocalDate date(String value, String field) {
    try {
      if (!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))
        throw BlockController.invalid(field, "INVALID_VALUE");
      var date = java.time.LocalDate.parse(value);
      if (date.getYear() < 1) throw BlockController.invalid(field, "INVALID_VALUE");
      return date;
    } catch (java.time.DateTimeException error) {
      throw BlockController.invalid(field, "INVALID_VALUE");
    }
  }
}
