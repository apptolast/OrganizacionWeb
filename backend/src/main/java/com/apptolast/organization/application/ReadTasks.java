package com.apptolast.organization.application;

import com.apptolast.organization.domain.Task;
import com.apptolast.organization.domain.TaskPage;
import com.apptolast.organization.domain.TaskPosition;
import java.util.UUID;

public final class ReadTasks implements ReadTasksUseCase {
  private final TaskQueries queries;

  public ReadTasks(TaskQueries queries) {
    this.queries = queries;
  }

  public TaskPage list(String owner, UUID project, TaskPosition after) {
    var found = queries.list(owner, project, after);
    return page(found);
  }

  static TaskPage page(java.util.List<Task> found) {
    var items = found.stream().limit(20).toList();
    var last = items.isEmpty() ? null : items.getLast();
    return new TaskPage(
        items, found.size() > 20 ? new TaskPosition(last.createdAt(), last.id()) : null);
  }

  public Task detail(String owner, UUID project, UUID id) {
    return queries.detail(owner, project, id);
  }
}
