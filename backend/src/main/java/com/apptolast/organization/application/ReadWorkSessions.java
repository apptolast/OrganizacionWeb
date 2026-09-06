package com.apptolast.organization.application;

import com.apptolast.organization.domain.SessionStart;
import java.util.Optional;

public final class ReadWorkSessions implements ReadWorkSessionsUseCase {
  private final WorkSessionQueries queries;

  public ReadWorkSessions(WorkSessionQueries queries) {
    this.queries = queries;
  }

  public Optional<SessionStart> active(String owner) {
    return queries.active(owner);
  }

  public SessionStart detail(String owner, java.util.UUID id) {
    return queries.detail(owner, id).orElseThrow(WorkSessionNotFoundException::new);
  }

  public SessionStart byRequest(String owner, java.util.UUID key) {
    return queries.byRequest(owner, key).orElseThrow(WorkSessionNotFoundException::new);
  }
}
