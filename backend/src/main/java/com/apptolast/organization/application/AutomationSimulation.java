package com.apptolast.organization.application;

import java.util.List;

/** The closed answer of a dry run: how many events were looked at and which ones would fire. */
public record AutomationSimulation(int evaluatedEvents, List<AutomationMatch> matches) {
  public AutomationSimulation {
    matches = List.copyOf(matches);
  }
}
