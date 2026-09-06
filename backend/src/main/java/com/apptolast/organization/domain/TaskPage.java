package com.apptolast.organization.domain;

import java.util.List;

public record TaskPage(List<Task> items, TaskPosition next) {
  public TaskPage {
    items = List.copyOf(items);
  }
}
