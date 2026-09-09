package com.apptolast.organization.application;

/** Carries no detail: every unresolvable candidate must produce one indistinguishable answer. */
public final class CalendarNotFoundException extends RuntimeException {
  public CalendarNotFoundException() {
    super("No existe ningún calendario para esa dirección.");
  }
}
