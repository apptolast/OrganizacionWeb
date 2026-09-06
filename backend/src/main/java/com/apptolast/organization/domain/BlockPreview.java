package com.apptolast.organization.domain;

import java.util.List;

public record BlockPreview(
    BlockRequest request,
    ResolvedBlockTime time,
    AvailabilityRevision availabilityRevision,
    String budgetZoneId,
    List<BudgetDay> days) {
  public BlockPreview {
    days = List.copyOf(days);
  }
}
