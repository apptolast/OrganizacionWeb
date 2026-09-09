package com.apptolast.organization.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** An outbox row as the rules see it: identity, type, instant and the payload that names things. */
public record AutomationEvent(
    UUID eventId,
    String ownerId,
    String eventType,
    UUID aggregateId,
    Instant occurredAt,
    Map<String, Object> payload) {
  public AutomationEvent {
    payload = Map.copyOf(payload);
  }

  public EventProject projectSource() {
    return switch (eventType) {
      case "BlockPlanned.v1", "BlockChanged.v1" -> new EventProject.OfTask(uuid("taskId"));
      case "WorkSessionStarted.v1" -> new EventProject.Known(uuid("projectId"));
      case "WorkSessionStateChanged.v1", "WorkSessionExtended.v1", "WorkSessionClosed.v1" ->
          new EventProject.OfWorkSession(aggregateId);
      default -> new EventProject.Known(aggregateId);
    };
  }

  /** Present only for the two creation events; the guard is never consulted for the others. */
  public Optional<UUID> loopGuardTaskId() {
    return switch (eventType) {
      case "TaskCreated.v1", "SubtaskCreated.v1" -> Optional.of(uuid("taskId"));
      default -> Optional.empty();
    };
  }

  private UUID uuid(String field) {
    return UUID.fromString(String.valueOf(payload.get(field)));
  }
}
