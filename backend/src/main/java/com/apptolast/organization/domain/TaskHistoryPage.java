package com.apptolast.organization.domain;

import java.util.List;

public record TaskHistoryPage(List<TaskHistoryEntry> items, TaskHistoryPosition next) {
  public TaskHistoryPage {
    items = List.copyOf(items);
  }
}
