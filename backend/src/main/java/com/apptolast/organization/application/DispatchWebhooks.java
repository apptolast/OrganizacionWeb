package com.apptolast.organization.application;

import com.apptolast.organization.domain.WebhookDelivery;
import java.time.Clock;

/**
 * One bounded pass of the delivery queue: claim, send outside the transaction, settle. The endpoint
 * is only touched when the attempt exhausts its delivery.
 */
public final class DispatchWebhooks implements DispatchWebhooksUseCase {
  private static final int MAX_DELIVERIES_PER_CYCLE = 20;

  private final WebhookWork work;
  private final WebhookSender sender;
  private final Clock clock;

  public DispatchWebhooks(WebhookWork work, WebhookSender sender, Clock clock) {
    this.work = work;
    this.sender = sender;
    this.clock = clock;
  }

  @Override
  public void runCycle() {
    for (var sent = 0; sent < MAX_DELIVERIES_PER_CYCLE; sent++) {
      var claimed = work.claimNext(CustomizationTime.capture(clock));
      if (claimed.isEmpty()) return;
      attempt(claimed.get());
    }
  }

  private void attempt(ClaimedDelivery claimed) {
    var outcome =
        sender.send(
            claimed.endpoint().url(),
            claimed.secret(),
            claimed.delivery().eventId().toString(),
            claimed.body());
    var now = CustomizationTime.capture(clock);
    var result = claimed.delivery().recorded(outcome, now);
    work.record(claimed, result, exhaustedEndpoint(claimed, result, now));
  }

  /** An exhausted delivery drags its endpoint into DELIVERY_EXHAUSTED in the same transaction. */
  private static com.apptolast.organization.domain.WebhookEndpoint exhaustedEndpoint(
      ClaimedDelivery claimed, WebhookDelivery result, java.time.Instant now) {
    return WebhookDelivery.EXHAUSTED.equals(result.status())
        ? claimed.endpoint().disabledByExhaustion(now)
        : null;
  }
}
