package com.apptolast.organization.application;

/** El gestor externo agotó la cuota; {@code retryAfterSeconds} es siempre un entero positivo. */
public final class ConnectorRateLimitedException extends RuntimeException {
  private final int retryAfterSeconds;

  public ConnectorRateLimitedException(int retryAfterSeconds) {
    this.retryAfterSeconds = retryAfterSeconds;
  }

  public int retryAfterSeconds() {
    return retryAfterSeconds;
  }
}
