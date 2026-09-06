package com.apptolast.organization.application;

import com.apptolast.organization.domain.PlannedBlock;

public final class BlockOverlapException extends RuntimeException {
  private final PlannedBlock conflict;

  public BlockOverlapException(PlannedBlock conflict) {
    this.conflict = conflict;
  }

  public PlannedBlock conflict() {
    return conflict;
  }
}
