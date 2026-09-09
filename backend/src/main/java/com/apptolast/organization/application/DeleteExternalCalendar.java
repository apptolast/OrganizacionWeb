package com.apptolast.organization.application;

/** Borrar es idempotente y arrastra la instantánea: sin suscripción no hay eventos. */
public final class DeleteExternalCalendar implements ExternalCalendarUseCases.Delete {
  private final ExternalCalendarStore store;

  public DeleteExternalCalendar(ExternalCalendarStore store) {
    this.store = store;
  }

  @Override
  public void execute(String ownerId) {
    store.delete(ownerId);
  }
}
