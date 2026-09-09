package com.apptolast.organization.application;

import com.apptolast.organization.domain.AutomationRun;
import java.util.List;

/** One page of history; a null cursor means there is nothing older left. */
public record AutomationRunPage(List<AutomationRun> items, AutomationRunCursor nextCursor) {
  public AutomationRunPage {
    items = List.copyOf(items);
  }
}
