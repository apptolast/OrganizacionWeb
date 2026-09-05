package com.apptolast.organization.domain;

import java.util.List;

public record ProjectPage(List<ProjectSummary> items, ProjectPosition next) {
  public ProjectPage {
    items = List.copyOf(items);
  }
}
