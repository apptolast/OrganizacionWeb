package com.apptolast.organization.application;

import com.apptolast.organization.domain.ExternalCalendarSubscription;
import com.apptolast.organization.domain.SyncStatus;

/**
 * La fila del calendario externo (28). La suscripción ya trae su propio código de fallo cerrado
 * ({@link com.apptolast.organization.domain.FeedError}), así que la fila lo publica tal cual: no
 * hay traducción que pueda colar el texto del servidor ajeno.
 *
 * <p>Suscrito y todavía sin sincronizar es {@code connected} sin actividad: la suscripción existe,
 * que es lo que el catálogo cuenta, y nadie ha intentado leerla aún.
 */
public final class ExternalCalendarStatusSource implements ConnectorStatusSource {
  public static final String ID = "external_calendar";

  private final ExternalCalendarStore subscriptions;

  public ExternalCalendarStatusSource(ExternalCalendarStore subscriptions) {
    this.subscriptions = subscriptions;
  }

  @Override
  public String id() {
    return ID;
  }

  @Override
  public boolean encryptsSecrets() {
    return true;
  }

  @Override
  public ConnectorRow read(String ownerId) {
    return subscriptions
        .find(ownerId)
        .map(stored -> row(stored.subscription()))
        .orElseGet(() -> ConnectorRow.notConnected(ID));
  }

  private static ConnectorRow row(ExternalCalendarSubscription subscription) {
    var attemptAt = subscription.lastAttemptAt();
    if (subscription.lastStatus() != SyncStatus.FAILED)
      return ConnectorRow.connected(ID, attemptAt);
    return ConnectorRow.error(
        ID, attemptAt, new ConnectorError(subscription.lastError().name(), attemptAt));
  }
}
