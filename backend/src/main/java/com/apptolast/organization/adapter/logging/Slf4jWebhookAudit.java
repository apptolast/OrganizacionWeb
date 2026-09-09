package com.apptolast.organization.adapter.logging;

import com.apptolast.organization.application.WebhookAudit;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Writes the webhook audit trail to the {@code organization.webhooks} logger. */
public final class Slf4jWebhookAudit implements WebhookAudit {
  private static final Logger LOG = LoggerFactory.getLogger("organization.webhooks");

  @Override
  public void attempt(UUID endpointId, UUID eventId, String status, String errorClass) {
    LOG.info(
        "endpointId={} eventId={} status={} errorClass={}",
        endpointId,
        eventId,
        status,
        errorClass);
  }

  @Override
  public void discarded(UUID endpointId, UUID eventId, String code) {
    LOG.info("endpointId={} eventId={} outcome=discarded code={}", endpointId, eventId, code);
  }

  @Override
  public void workerError(String code) {
    LOG.warn("outcome=worker_error code={}", code);
  }
}
