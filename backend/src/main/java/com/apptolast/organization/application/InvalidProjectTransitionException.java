package com.apptolast.organization.application;

public final class InvalidProjectTransitionException extends RuntimeException {
  public InvalidProjectTransitionException() {
    super("La transición solicitada no está permitida.");
  }
}
