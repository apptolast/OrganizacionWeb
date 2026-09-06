package com.apptolast.organization.domain;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public record BlockMoveRequest(
    LocalDateTime startLocal,
    LocalDateTime endLocal,
    String zoneId,
    ZoneOffset startOffset,
    ZoneOffset endOffset,
    boolean allowOverBudget) {
  public BlockMoveRequest {
    BlockRequest.validateDestination(startLocal, endLocal, zoneId);
  }

  public BlockRequest withObjective(String objective) {
    return new BlockRequest(
        objective, startLocal, endLocal, zoneId, startOffset, endOffset, allowOverBudget);
  }
}
