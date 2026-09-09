package com.apptolast.organization.application;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Dobles en memoria de los puertos propios del conector de GitLab. */
final class GitlabFakes {
  final FakeGitlabConnections connections = new FakeGitlabConnections();
  final FakeGitlabProjects projects = new FakeGitlabProjects();
  final ConnectorFakes.FakeCipher cipher = new ConnectorFakes.FakeCipher();

  static final class FakeGitlabConnections implements GitlabConnectionStore {
    private final Map<String, GitlabConnection> rows = new HashMap<>();

    @Override
    public Optional<GitlabConnection> find(String ownerId) {
      return Optional.ofNullable(rows.get(ownerId));
    }

    @Override
    public void save(String ownerId, GitlabConnection connection) {
      rows.put(ownerId, connection);
    }

    @Override
    public boolean delete(String ownerId) {
      return rows.remove(ownerId) != null;
    }

    void put(String ownerId, GitlabConnection connection) {
      rows.put(ownerId, connection);
    }
  }

  /** Anota lo que se le preguntó, para poder afirmar que nada más salió hacia el proveedor. */
  static final class FakeGitlabProjects implements GitlabProjectDirectory {
    private final Map<String, GitlabProject> known = new HashMap<>();
    private String verifiedPath;
    private String verifiedToken;
    private IssueSourceException failure;

    void accept(String canonicalPath, long projectId) {
      known.put(canonicalPath.toLowerCase(java.util.Locale.ROOT), new GitlabProject(projectId, canonicalPath));
    }

    void reject(IssueSourceException error) {
      failure = error;
    }

    String verifiedPath() {
      return verifiedPath;
    }

    String verifiedToken() {
      return verifiedToken;
    }

    @Override
    public GitlabProject verify(String projectPath, String token) {
      verifiedPath = projectPath;
      verifiedToken = token;
      if (failure != null) throw failure;
      var project = known.get(projectPath.toLowerCase(java.util.Locale.ROOT));
      if (project == null) throw IssueSourceException.repositoryUnavailable();
      return project;
    }
  }
}
