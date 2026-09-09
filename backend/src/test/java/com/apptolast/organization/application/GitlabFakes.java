package com.apptolast.organization.application;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Dobles en memoria de los puertos propios del conector de GitLab. */
final class GitlabFakes {
  final FakeGitlabConnections connections = new FakeGitlabConnections();
  final ConnectorFakes.FakeCipher cipher = new ConnectorFakes.FakeCipher();

  static final class FakeGitlabConnections implements GitlabConnectionStore {
    private final Map<String, GitlabConnection> rows = new HashMap<>();

    @Override
    public Optional<GitlabConnection> find(String ownerId) {
      return Optional.ofNullable(rows.get(ownerId));
    }

    void put(String ownerId, GitlabConnection connection) {
      rows.put(ownerId, connection);
    }
  }
}
