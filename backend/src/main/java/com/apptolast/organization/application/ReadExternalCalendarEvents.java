package com.apptolast.organization.application;

import com.apptolast.organization.domain.ExternalEventsRange;

/** Lee la instantánea almacenada dentro del rango pedido. Nunca descarga ni sincroniza. */
public final class ReadExternalCalendarEvents implements ExternalCalendarUseCases.ReadEvents {
  private final ExternalCalendarStore store;

  public ReadExternalCalendarEvents(ExternalCalendarStore store) {
    this.store = store;
  }

  @Override
  public ExternalEventsView execute(String ownerId, ExternalEventsRange range) {
    return store
        .find(ownerId)
        .map(
            stored ->
                new ExternalEventsView(
                    true,
                    stored.subscription().lastSyncAt(),
                    stored.subscription().lastStatus(),
                    store.events(ownerId, range.from(), range.to())))
        .orElseGet(ExternalEventsView::unconfigured);
  }
}
