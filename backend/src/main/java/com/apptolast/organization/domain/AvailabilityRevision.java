package com.apptolast.organization.domain;

import java.util.UUID;

public record AvailabilityRevision(UUID id, long version) {
  public AvailabilityRevision {
    if (version < 0 || (id == null && version != 0))
      throw new IllegalArgumentException("Invalid availability revision");
  }
}
