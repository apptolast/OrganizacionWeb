package com.apptolast.organization.application;

import com.apptolast.organization.domain.WebhookCursor;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Walks the outbox for each ready endpoint and turns the first eligible event into one pending
 * delivery. The outbox is the single source: no row is ever created or modified here.
 *
 * <p>Only one outbox delivery per endpoint is in flight at a time, which is what keeps the
 * receiver's order equal to the outbox order.
 */
public final class EnqueueWebhookDeliveries implements EnqueueWebhookDeliveriesUseCase {
  /**
   * Events younger than this are left alone: a row can still be committed with an earlier
   * occurredAt, and skipping it would strand it behind the cursor forever.
   */
  private static final Duration GRACE = Duration.ofSeconds(5);

  private static final String UNSUPPORTED = "UNSUPPORTED_EVENT";
  private static final String INVALID = "INVALID_EVENT";

  private final WebhookOutbox outbox;
  private final WebhookAudit audit;
  private final Clock clock;

  public EnqueueWebhookDeliveries(WebhookOutbox outbox, WebhookAudit audit, Clock clock) {
    this.outbox = outbox;
    this.audit = audit;
    this.clock = clock;
  }

  @Override
  public void runCycle() {
    var now = CustomizationTime.capture(clock);
    var horizon = now.minus(GRACE);
    for (var ready : outbox.readyEndpoints()) enqueueFirstEligible(ready, horizon, now);
  }

  private void enqueueFirstEligible(ReadyEndpoint ready, Instant horizon, Instant now) {
    for (var candidate : outbox.after(ready.ownerId(), ready.cursor(), horizon)) {
      if (!ready.endpoint().eventTypes().contains(candidate.eventType())) continue;
      var endpointId = ready.endpoint().id();
      var reached = new WebhookCursor(candidate.occurredAt(), candidate.eventId());
      var rejection = rejectionOf(candidate);
      if (rejection != null || candidate.isBlocked()) {
        outbox.skip(endpointId, reached, rejection);
        // A blocked row is a deliberate skip, not a failure worth auditing.
        if (rejection != null) audit.discarded(endpointId, candidate.eventId(), rejection);
        continue;
      }
      outbox.enqueue(endpointId, candidate, UUID.randomUUID(), now);
      return;
    }
  }

  /** Null when the row is deliverable; otherwise the audit code that explains the discard. */
  private static String rejectionOf(OutboxCandidate candidate) {
    if (candidate.isBlocked()) return null;
    if (!candidate.isSupported()) return UNSUPPORTED;
    return candidate.payloadValid() ? null : INVALID;
  }
}
