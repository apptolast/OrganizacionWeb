package com.apptolast.organization.application;

import com.apptolast.organization.domain.WebhookCursor;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Reading the outbox for webhook purposes. Nothing here ever writes to the outbox itself. */
public interface WebhookOutbox {
  /** Active endpoints that keep at most one outbox delivery in flight, so none is ready twice. */
  List<ReadyEndpoint> readyEndpoints();

  /**
   * The owner's rows strictly after the cursor and no later than the horizon, in tuple order
   * (occurredAt, eventId).
   */
  List<OutboxCandidate> after(String ownerId, WebhookCursor cursor, Instant horizon);

  /** Inserts the delivery and moves the cursor onto the event, in one transaction. */
  void enqueue(UUID endpointId, OutboxCandidate candidate, UUID deliveryId, Instant now);

  /** Moves the cursor past a row that produced no delivery, auditing it when there is a reason. */
  void skip(UUID endpointId, WebhookCursor cursor, String auditCode);
}
