package com.apptolast.organization.application;

import com.apptolast.organization.domain.BlockChangePage;
import com.apptolast.organization.domain.BlockChangePosition;
import java.util.UUID;

public final class ReadBlockChanges implements ReadBlockChangesUseCase {
  private final BlockChangeQueries queries;

  public ReadBlockChanges(BlockChangeQueries queries) {
    this.queries = queries;
  }

  public BlockChangePage list(String owner, UUID project, UUID task, BlockChangePosition after) {
    var rows = queries.list(owner, project, task, after);
    var items = rows.stream().limit(20).toList();
    return new BlockChangePage(
        items,
        rows.size() > 20
            ? new BlockChangePosition(items.getLast().occurredAt(), items.getLast().id())
            : null);
  }

  public com.apptolast.organization.domain.BlockChangeReceipt detail(
      String owner, UUID project, UUID task, UUID id) {
    return queries.detail(owner, project, task, id);
  }

  public com.apptolast.organization.domain.BlockChangeReceipt byRequest(
      String owner, UUID project, UUID task, UUID key) {
    return queries.byRequest(owner, project, task, key);
  }
}
