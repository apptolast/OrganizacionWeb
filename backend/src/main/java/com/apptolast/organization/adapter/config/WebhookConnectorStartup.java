package com.apptolast.organization.adapter.config;

import com.apptolast.organization.application.WebhookAudit;
import com.apptolast.organization.application.WebhookSecrets;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * Reports a missing connector key once, when the application comes up.
 *
 * <p>Amendment B5: an absent key degrades the connectors instead of stopping the boot, so the
 * operator needs one clear line at startup rather than a warning per worker cycle.
 */
public final class WebhookConnectorStartup {
  private final WebhookSecrets secrets;
  private final WebhookAudit audit;
  private final AtomicBoolean reported = new AtomicBoolean();

  public WebhookConnectorStartup(WebhookSecrets secrets, WebhookAudit audit) {
    this.secrets = secrets;
    this.audit = audit;
  }

  @EventListener(ContextRefreshedEvent.class)
  public void report() {
    if (secrets.available() || !reported.compareAndSet(false, true)) return;
    audit.workerError("CONFIGURATION_ERROR");
  }
}
