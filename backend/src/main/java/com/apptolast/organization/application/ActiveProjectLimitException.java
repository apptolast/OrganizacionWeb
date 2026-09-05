package com.apptolast.organization.application;

public final class ActiveProjectLimitException extends RuntimeException {
  private final long activeCount;
  private final int limit;

  public ActiveProjectLimitException(long activeCount, int limit) {
    super("No hay plazas activas disponibles. Pausa o termina otro proyecto.");
    this.activeCount = activeCount;
    this.limit = limit;
  }

  public long activeCount() {
    return activeCount;
  }

  public int limit() {
    return limit;
  }
}
