package com.apptolast.organization.adapter.config;

import com.apptolast.organization.application.DispatchWebhooksUseCase;
import com.apptolast.organization.application.EnqueueWebhookDeliveriesUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * One tick: turn new outbox events into deliveries, then send whatever is due. Enqueuing runs
 * first so an event that has just cleared the grace window can ship in this very tick.
 *
 * <p>Neither half may throw: a scheduled method that propagates stops the whole schedule.
 */
public final class WebhookSchedule {
  private static final Logger LOG = LoggerFactory.getLogger(WebhookSchedule.class);

  private final EnqueueWebhookDeliveriesUseCase enqueue;
  private final DispatchWebhooksUseCase dispatch;

  public WebhookSchedule(
      EnqueueWebhookDeliveriesUseCase enqueue, DispatchWebhooksUseCase dispatch) {
    this.enqueue = enqueue;
    this.dispatch = dispatch;
  }

  @Scheduled(fixedDelay = 1000, initialDelay = 1000)
  public void tick() {
    guarded("enqueue", enqueue::runCycle);
    guarded("dispatch", dispatch::runCycle);
  }

  private static void guarded(String phase, Runnable cycle) {
    try {
      cycle.run();
    } catch (RuntimeException failure) {
      // The class of the failure is enough: no URL, secret, signature nor body is ever logged.
      LOG.warn(
          "Webhook worker cycle failed; phase={} category={}",
          phase,
          failure.getClass().getSimpleName());
    }
  }
}
