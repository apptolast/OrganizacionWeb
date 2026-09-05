package com.apptolast.organization.application;

import com.apptolast.organization.domain.PublicationAttempt;
import java.time.Clock;
import java.util.Set;

public final class PublishOutbox implements PublishOutboxUseCase {
  private final OutboxWork work;
  private final BrokerPublisher broker;
  private final PublicationAudit audit;
  private final Clock clock;

  public PublishOutbox(
      OutboxWork work, BrokerPublisher broker, PublicationAudit audit, Clock clock) {
    this.work = work;
    this.broker = broker;
    this.audit = audit;
    this.clock = clock;
  }

  @Override
  public void runCycle() {
    try {
      publishBatch();
    } catch (StorageUnavailableException error) {
      audit.workerError("STORAGE_UNAVAILABLE");
    } catch (TopologyMismatchException error) {
      audit.workerError("TOPOLOGY_MISMATCH");
    }
  }

  private void publishBatch() {
    Set<java.util.UUID> processed = new java.util.HashSet<>();
    for (int index = 0; index < 20 && !Thread.currentThread().isInterrupted(); index++) {
      var completed =
          work.processNext(
              clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS),
              processed,
              message -> {
                String invalid = message.validationCode();
                if (invalid != null)
                  return new PublicationAttempt(
                      message.eventId(),
                      "blocked",
                      message.attempts(),
                      clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS),
                      null,
                      invalid);
                DeliveryOutcome outcome = broker.publish(message);
                java.time.Instant completedAt =
                    clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
                if (outcome != DeliveryOutcome.ACCEPTED) {
                  return new PublicationAttempt(
                      message.eventId(),
                      "retry",
                      message.attempts() + 1,
                      completedAt,
                      completedAt.plusSeconds(
                          message.attempts() >= 6 ? 60 : 1L << message.attempts()),
                      outcome.name());
                }
                return new PublicationAttempt(
                    message.eventId(),
                    "published",
                    message.attempts() + 1,
                    completedAt,
                    null,
                    null);
              });
      if (completed.isEmpty()) return;
      processed.add(completed.get().eventId());
      audit.event(completed.get());
    }
  }
}
