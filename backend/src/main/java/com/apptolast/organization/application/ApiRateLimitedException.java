package com.apptolast.organization.application;

public final class ApiRateLimitedException extends RuntimeException {
  private final int retryAfterSeconds;

  public ApiRateLimitedException(int retryAfterSeconds) {
    super("Se ha alcanzado el límite de solicitudes.");
    this.retryAfterSeconds = retryAfterSeconds;
  }

  public int retryAfterSeconds() {
    return retryAfterSeconds;
  }
}
