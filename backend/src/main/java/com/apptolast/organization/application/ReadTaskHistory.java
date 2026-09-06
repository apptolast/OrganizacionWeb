package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.util.UUID;

public final class ReadTaskHistory implements ReadTaskHistoryUseCase {
  private final TaskHistoryQueries queries;

  public ReadTaskHistory(TaskHistoryQueries queries) {
    this.queries = queries;
  }

  public TaskHistoryPage list(String owner, UUID project, UUID task, TaskHistoryPosition after) {
    var rows = queries.list(owner, project, task, after);
    var items = rows.subList(0, Math.min(rows.size(), 20));
    return new TaskHistoryPage(
        items, rows.size() > 20 ? new TaskHistoryPosition(items.getLast().taskVersion()) : null);
  }
}
