package com.apptolast.organization.adapter.logging;

import com.apptolast.organization.application.ExternalCalendarAudit;
import com.apptolast.organization.domain.FeedError;
import com.apptolast.organization.domain.SyncStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Solo host, resultado, código y duración: la ruta de la dirección es el secreto y no se registra. */
public final class Slf4jExternalCalendarAudit implements ExternalCalendarAudit {
  private static final Logger LOG = LoggerFactory.getLogger("organization.external-calendar");

  @Override
  public void syncFinished(String urlHost, SyncStatus status, FeedError error, long millis) {
    LOG.info(
        "host={} status={} code={} durationMs={}",
        urlHost,
        status,
        error == null ? "NONE" : error.name(),
        millis);
  }
}
