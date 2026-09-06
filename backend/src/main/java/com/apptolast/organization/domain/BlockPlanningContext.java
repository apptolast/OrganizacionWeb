package com.apptolast.organization.domain;

import java.util.*;

public record BlockPlanningContext(
    String projectStatus,
    String taskStatus,
    Optional<Availability> availability,
    List<PlannedBlock> blocks) {
  public BlockPlanningContext {
    blocks = List.copyOf(blocks);
  }
}
