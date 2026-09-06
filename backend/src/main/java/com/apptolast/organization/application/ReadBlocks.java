package com.apptolast.organization.application;

import com.apptolast.organization.domain.PlannedBlock;
import java.util.UUID;

public final class ReadBlocks implements ReadBlocksUseCase {
  public com.apptolast.organization.domain.BlockPage list(
      String owner,
      UUID project,
      UUID task,
      com.apptolast.organization.domain.BlockPosition after) {
    var found = queries.list(owner, project, task, after);
    var items = found.stream().limit(20).toList();
    var next =
        found.size() > 20
            ? new com.apptolast.organization.domain.BlockPosition(
                items.getLast().createdAt(), items.getLast().id())
            : null;
    return new com.apptolast.organization.domain.BlockPage(items, next);
  }

  private final BlockQueries queries;

  public ReadBlocks(BlockQueries queries) {
    this.queries = queries;
  }

  public PlannedBlock detail(String owner, UUID project, UUID task, UUID block) {
    return queries.detail(owner, project, task, block);
  }

  public PlannedBlock byRequest(String owner, UUID project, UUID task, UUID key) {
    return queries.byRequest(owner, project, task, key);
  }
}
