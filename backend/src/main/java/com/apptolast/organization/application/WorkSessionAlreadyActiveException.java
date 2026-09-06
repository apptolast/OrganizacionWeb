package com.apptolast.organization.application;

import java.util.UUID;

public final class WorkSessionAlreadyActiveException extends RuntimeException {
  private final UUID sessionId;

  public WorkSessionAlreadyActiveException(UUID sessionId) {
    this.sessionId = sessionId;
  }

  public UUID sessionId() {
    return sessionId;
  }
}
