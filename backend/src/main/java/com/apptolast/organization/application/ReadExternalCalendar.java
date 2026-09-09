package com.apptolast.organization.application;

import com.apptolast.organization.domain.ExternalCalendarSubscription;
import java.util.Optional;

/** Lee la suscripción sin intentar descifrar la dirección: host y cola bastan para mostrarla. */
public final class ReadExternalCalendar {
  private final ExternalCalendarStore store;

  public ReadExternalCalendar(ExternalCalendarStore store) {
    this.store = store;
  }

  public Optional<ExternalCalendarSubscription> execute(String ownerId) {
    return store.find(ownerId).map(StoredSubscription::subscription);
  }
}
