package com.apptolast.organization.adapter.logging;

import com.apptolast.organization.application.PublicationAudit;
import com.apptolast.organization.domain.PublicationAttempt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Slf4jPublicationAudit implements PublicationAudit {
  private static final Logger LOG = LoggerFactory.getLogger("organization.outbox");

  @Override
  public void event(PublicationAttempt attempt) {
    LOG.info(
        "eventId={} outcome={} attempt={} code={}",
        attempt.eventId(),
        attempt.outcome(),
        attempt.outcome().equals("blocked") ? 0 : attempt.attempt(),
        attempt.code());
  }

  @Override
  public void workerError(String code) {
    LOG.warn("outcome=worker_error code={}", code);
  }
}
