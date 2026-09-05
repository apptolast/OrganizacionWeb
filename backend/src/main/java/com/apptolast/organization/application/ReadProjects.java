package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;

public final class ReadProjects implements ReadProjectsUseCase {
  private final ProjectQueries queries;

  public ReadProjects(ProjectQueries queries) {
    this.queries = queries;
  }

  public ProjectPage list(String ownerId, ProjectPosition after) {
    var rows = queries.list(ownerId, after, 21);
    var items = rows.subList(0, Math.min(20, rows.size()));
    var next =
        rows.size() > 20
            ? new ProjectPosition(items.getLast().createdAt(), items.getLast().id())
            : null;
    return new ProjectPage(items, next);
  }

  public ProjectSnapshot detail(String ownerId, java.util.UUID id) {
    return queries.find(ownerId, id).orElseThrow(ProjectNotFoundException::new);
  }
}
