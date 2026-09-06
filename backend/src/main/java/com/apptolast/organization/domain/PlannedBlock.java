package com.apptolast.organization.domain;

import java.time.Instant;
import java.util.UUID;

public record PlannedBlock(
    UUID id,
    UUID projectId,
    UUID taskId,
    BlockRequest request,
    ResolvedBlockTime time,
    Instant createdAt) {
  public PlannedBlock {
    if (id == null
        || projectId == null
        || taskId == null
        || request == null
        || time == null
        || createdAt == null
        || createdAt.getNano() % 1000 != 0
        || !time.startOffset().equals(request.startOffset())
        || !time.endOffset().equals(request.endOffset())
        || !request.startLocal().toInstant(time.startOffset()).equals(time.startAt())
        || !request.endLocal().toInstant(time.endOffset()).equals(time.endAt()))
      throw new IllegalArgumentException("Invalid stored block identity or intention");
  }
}
