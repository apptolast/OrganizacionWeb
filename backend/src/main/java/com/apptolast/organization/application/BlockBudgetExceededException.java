package com.apptolast.organization.application;

import com.apptolast.organization.domain.BudgetDay;
import java.util.List;

public final class BlockBudgetExceededException extends RuntimeException {
  private final String budgetZoneId;
  private final List<BudgetDay> days;

  public BlockBudgetExceededException(String zone, List<BudgetDay> days) {
    this.budgetZoneId = zone;
    this.days = List.copyOf(days);
  }

  public String budgetZoneId() {
    return budgetZoneId;
  }

  public List<BudgetDay> days() {
    return days;
  }
}
