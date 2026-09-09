package com.apptolast.organization.application;

public final class CalendarTooLargeException extends RuntimeException {
  public CalendarTooLargeException() {
    super("El calendario supera el límite de eventos permitido.");
  }
}
