package com.apptolast.organization.application;

import com.apptolast.organization.domain.WebhookAttempt;
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
  private final WebhookAudit audit;
  private final WebhookSecrets secrets;
  private final Clock clock;

  public DispatchWebhooks(
      WebhookWork work,
      WebhookSender sender,
      WebhookAudit audit,
      WebhookSecrets secrets,
      Clock clock) {
    this.work = work;
    this.sender = sender;
    this.audit = audit;
    this.secrets = secrets;
    this.clock = clock;
  }

  @Override
  public void runCycle() {
    // Without a key no stored secret can be opened, so there is nothing to sign with and no
    // reason to claim a row. Startup already audited CONFIGURATION_ERROR once.
    if (!secrets.available()) return;
    for (var sent = 0; sent < MAX_DELIVERIES_PER_CYCLE; sent++) {
      var claimed = work.claimNext(CustomizationTime.capture(clock));
      if (claimed.isEmpty()) return;
      attempt(claimed.get());
    }
  }

  private void attempt(ClaimedDelivery claimed) {
    var outcome = claimed.secret() == null ? WebhookAttempt.unreadableSecret() : send(claimed);
    var now = CustomizationTime.capture(clock);
    var result = claimed.delivery().recorded(outcome, now);
    work.record(claimed, result, exhaustedEndpoint(claimed, result, now));
    audit.attempt(claimed.endpoint().id(), result.eventId(), result.status(), result.errorClass());
  }

  /**
   * A claim with no readable secret cannot be signed, so nothing is sent: it settles as one failed
   * attempt and enters the retry table like any other. The sixth one exhausts it and disables its
   * endpoint, which is the documented remedy for a rotated key -recreate the endpoint- instead of a
   * queue that stops for every owner in silence.
   */
  private WebhookAttempt send(ClaimedDelivery claimed) {
    return sender.send(
        claimed.endpoint().url(),
        claimed.secret(),
        claimed.delivery().eventId().toString(),
        claimed.body());
  }

  /**
   * A claim with no readable secret cannot be signed, so nothing is sent: it settles as one failed
   * attempt and enters the retry table like any other. The sixth one exhausts it and disables its
   * endpoint, which is the documented remedy for a rotated key -recreate the endpoint- instead of a
   * queue that stops for every owner in silence.
   */
  /** An exhausted delivery drags its endpoint into DELIVERY_EXHAUSTED in the same transaction. */
  private static com.apptolast.organization.domain.WebhookEndpoint exhaustedEndpoint(
      ClaimedDelivery claimed, WebhookDelivery result, java.time.Instant now) {
    return WebhookDelivery.EXHAUSTED.equals(result.status())
        ? claimed.endpoint().disabledByExhaustion(now)
        : null;
  }
}
