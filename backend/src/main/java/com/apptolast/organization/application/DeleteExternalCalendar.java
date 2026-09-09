package com.apptolast.organization.application;

/** Borrar es idempotente y arrastra la instantánea: sin suscripción no hay eventos. */
public final class DeleteExternalCalendar {
  private final ExternalCalendarStore store;

  public DeleteExternalCalendar(ExternalCalendarStore store) {
    this.store = store;
  }

  public void execute(String ownerId) {
    store.delete(ownerId);
  }
}
