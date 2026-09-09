package com.apptolast.organization.application;

import com.apptolast.organization.domain.ExternalEvent;
import com.apptolast.organization.domain.SyncStatus;
import java.time.Instant;
import java.util.List;

/** Lo que se lee de un rango: sin suscripción, todo vacío y {@code configured} falso. */
public record ExternalEventsView(
    boolean configured, Instant lastSyncAt, SyncStatus lastStatus, List<ExternalEvent> items) {
  public static ExternalEventsView unconfigured() {
    return new ExternalEventsView(false, null, null, List.of());
  }
}
