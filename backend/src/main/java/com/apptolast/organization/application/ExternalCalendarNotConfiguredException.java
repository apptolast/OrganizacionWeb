package com.apptolast.organization.application;

/** No hay ninguna suscripción que sincronizar. */
public final class ExternalCalendarNotConfiguredException extends RuntimeException {
  public ExternalCalendarNotConfiguredException() {
    super("Configura antes un calendario externo.");
  }
}
