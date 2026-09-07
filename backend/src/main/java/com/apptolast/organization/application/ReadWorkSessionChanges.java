package com.apptolast.organization.application;

import java.util.UUID;

public final class ReadWorkSessionChanges implements ReadWorkSessionChangesUseCase {
  private final WorkSessionTransitionQueries queries;

  public ReadWorkSessionChanges(WorkSessionTransitionQueries queries) {
    this.queries = queries;
  }

  public WorkSessionTransitionReceipt detail(String owner, UUID id) {
    return queries.changeDetail(owner, id).orElseThrow(WorkSessionChangeNotFoundException::new);
  }

  public WorkSessionTransitionReceipt byRequest(String owner, UUID key) {
    return queries.changeByRequest(owner, key).orElseThrow(WorkSessionChangeNotFoundException::new);
  }
}
