package com.apptolast.organization.domain;

public final class UnknownEventTypeException extends RuntimeException {
  public UnknownEventTypeException() {
    super("El tipo de evento no está publicado.");
  }
}
