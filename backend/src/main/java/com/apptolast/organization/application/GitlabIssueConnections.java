package com.apptolast.organization.application;

import java.time.Instant;
import java.util.Optional;

/** La conexión de GitLab vista por el caso de uso compartido de importación. */
public final class GitlabIssueConnections implements IssueConnections {
  public static final String SOURCE = "gitlab";

  private final GitlabConnectionStore connections;

  public GitlabIssueConnections(GitlabConnectionStore connections) {
    this.connections = connections;
  }

  @Override
  public String source() {
    return SOURCE;
  }

  @Override
  public Optional<IssueConnection> find(String ownerId) {
    return connections
        .find(ownerId)
        .map(
            row ->
                new IssueConnection(
                    row.projectPath(),
                    String.valueOf(row.projectId()),
                    row.tokenCiphertext(),
                    row.isConnected()));
  }

  @Override
  public void invalidate(String ownerId, String errorCode, Instant at) {
    connections
        .find(ownerId)
        .ifPresent(row -> connections.save(ownerId, row.withError(errorCode, at)));
  }

  @Override
  public void recordFailure(String ownerId, String errorCode, Instant at) {
    connections
        .find(ownerId)
        .ifPresent(row -> connections.save(ownerId, row.withFailure(errorCode, at)));
  }
}
