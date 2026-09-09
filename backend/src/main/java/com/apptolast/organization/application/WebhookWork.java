package com.apptolast.organization.application;

import com.apptolast.organization.domain.WebhookDelivery;
import com.apptolast.organization.domain.WebhookEndpoint;
import java.time.Instant;
import java.util.Optional;

/** Claiming and settling deliveries. The send itself happens between the two calls, unlocked. */
public interface WebhookWork {
  /**
   * Leases the next due delivery of an active endpoint, skipping rows another worker holds, so two
   * instances never send the same delivery at the same time.
   */
  Optional<ClaimedDelivery> claimNext(Instant now);

  /**
   * Writes the outcome in a short transaction. When {@code endpoint} is not null it is the same
   * transaction that disables it, so a delivery is never exhausted with its endpoint left active.
   */
  void record(ClaimedDelivery claimed, WebhookDelivery result, WebhookEndpoint endpoint);
}
