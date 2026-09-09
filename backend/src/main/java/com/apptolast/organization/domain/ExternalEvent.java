package com.apptolast.organization.domain;

import java.time.Instant;

public record ExternalEvent(
    String uid, String summary, Instant startAt, Instant endAt, boolean allDay) {
  public ExternalEvent {
    if (uid == null || uid.isBlank() || summary == null || startAt == null || endAt == null)
      throw new IllegalArgumentException("External events require uid, summary and instants");
    if (!endAt.isAfter(startAt))
      throw new IllegalArgumentException("External events must end after they start");
  }
}
