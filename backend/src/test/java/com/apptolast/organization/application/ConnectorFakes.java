package com.apptolast.organization.application;

import com.apptolast.organization.domain.ExternalIssue;
import com.apptolast.organization.domain.IssueImportReceipt;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Dobles en memoria de los puertos del conector. Sustituyen a PostgreSQL y a GitHub en las pruebas
 * de los casos de uso; la fidelidad de los adaptadores reales se comprueba en sus propias pruebas.
 */
final class ConnectorFakes {
  final FakeConnections connections = new FakeConnections();
  final FakeReceipts receipts = new FakeReceipts();
  final FakeIssueSource source = new FakeIssueSource();
  final FakeCipher cipher = new FakeCipher();
  final FakeImportedTaskCommit tasks = new FakeImportedTaskCommit();
  final FakeProjects projects = new FakeProjects();
  final FakeAudit audit = new FakeAudit();

  /** Bitácora en memoria: guarda las líneas para poder afirmar qué se registró y qué no. */
  static final class FakeAudit implements ConnectorAudit {
    private final List<String> lines = new ArrayList<>();

    List<String> lines() {
      return List.copyOf(lines);
    }

    @Override
    public void connected(String ownerId, String repository, String login) {
      lines.add("connected " + ownerId + " " + repository + " " + login);
    }

    @Override
    public void connectionRefused(
        String ownerId, String repository, String errorCode, int githubStatus) {
      lines.add("refused " + ownerId + " " + repository + " " + errorCode + " " + githubStatus);
    }

    @Override
    public void importFinished(
        String ownerId,
        String repository,
        UUID importId,
        int created,
        int skipped,
        int failed,
        boolean truncated) {
      lines.add(
          "finished "
              + ownerId
              + " "
              + repository
              + " "
              + created
              + " "
              + skipped
              + " "
              + failed
              + " "
              + truncated);
    }

    @Override
    public void importFailed(
        String ownerId,
        String repository,
        UUID importId,
        String errorCode,
        int created,
        int githubStatus) {
      lines.add(
          "import-failed "
              + ownerId
              + " "
              + repository
              + " "
              + errorCode
              + " "
              + created
              + " "
              + githubStatus);
    }
  }

  /** Cifrado reversible con nonce por escritura y propietario como dato autenticado. */
  static final class FakeCipher implements SecretCipher {
    private final AtomicInteger nonce = new AtomicInteger();
    private boolean enabled = true;

    void disable() {
      enabled = false;
    }

    @Override
    public boolean enabled() {
      return enabled;
    }

    @Override
    public byte[] encrypt(String ownerId, String plaintext) {
      if (!enabled) throw new ConnectorsDisabledException();
      return (nonce.incrementAndGet() + "|" + ownerId + "|" + reversed(plaintext))
          .getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public java.util.Optional<String> decrypt(String ownerId, byte[] ciphertext) {
      if (!enabled) throw new ConnectorsDisabledException();
      var parts = new String(ciphertext, StandardCharsets.UTF_8).split("\\|", 3);
      if (parts.length != 3 || !parts[1].equals(ownerId)) return java.util.Optional.empty();
      return java.util.Optional.of(reversed(parts[2]));
    }

    private static String reversed(String text) {
      return new StringBuilder(text).reverse().toString();
    }
  }

  static final class FakeConnections implements ConnectorConnectionStore {
    private final Map<String, StoredConnection> rows = new HashMap<>();

    @Override
    public Optional<StoredConnection> find(String ownerId) {
      return Optional.ofNullable(rows.get(ownerId));
    }

    @Override
    public void save(String ownerId, StoredConnection connection) {
      rows.put(ownerId, connection);
    }

    @Override
    public boolean delete(String ownerId) {
      return rows.remove(ownerId) != null;
    }

    @Override
    public void invalidate(String ownerId) {
      var row = rows.get(ownerId);
      if (row != null) rows.put(ownerId, row.asInvalid());
    }

    int size() {
      return rows.size();
    }
  }

  static final class FakeReceipts implements IssueImportReceiptStore {
    private final Map<UUID, IssueImportReceipt> rows = new LinkedHashMap<>();
    private final Map<UUID, String> owners = new HashMap<>();
    private final List<Integer> progressCreated = new ArrayList<>();

    List<Integer> progressCreated() {
      return List.copyOf(progressCreated);
    }

    @Override
    public IssueImportReceipt begin(
        String ownerId, UUID projectId, String repository, Instant startedAt, Instant staleBefore) {
      interruptStale(ownerId, staleBefore);
      if (running(ownerId).isPresent()) throw new IssueImportInProgressException();
      var receipt =
          new IssueImportReceipt(
              UUID.randomUUID(),
              projectId,
              repository,
              "running",
              0,
              0,
              0,
              false,
              null,
              startedAt,
              null);
      owners.put(receipt.id(), ownerId);
      rows.put(receipt.id(), receipt);
      return receipt;
    }

    @Override
    public void progress(String ownerId, UUID importId, int created, int skipped, int failed) {
      progressCreated.add(created);
      rows.computeIfPresent(importId, (id, row) -> row.withCounters(created, skipped, failed));
    }

    @Override
    public IssueImportReceipt finish(
        String ownerId,
        UUID importId,
        String status,
        String errorCode,
        boolean truncated,
        Instant finishedAt) {
      var closed = rows.get(importId).close(status, errorCode, truncated, finishedAt);
      rows.put(importId, closed);
      return closed;
    }

    @Override
    public Optional<IssueImportReceipt> find(String ownerId, UUID importId) {
      return Optional.ofNullable(rows.get(importId))
          .filter(row -> ownerId.equals(owners.get(row.id())));
    }

    @Override
    public Optional<IssueImportReceipt> latest(String ownerId) {
      return rows.values().stream()
          .filter(row -> ownerId.equals(owners.get(row.id())))
          .max(java.util.Comparator.comparing(IssueImportReceipt::startedAt));
    }

    private Optional<IssueImportReceipt> running(String ownerId) {
      return rows.values().stream()
          .filter(row -> ownerId.equals(owners.get(row.id())) && "running".equals(row.status()))
          .findFirst();
    }

    private void interruptStale(String ownerId, Instant staleBefore) {
      running(ownerId)
          .filter(row -> row.startedAt().isBefore(staleBefore))
          .ifPresent(
              row -> rows.put(row.id(), row.close("failed", "INTERRUPTED", false, staleBefore)));
    }

    IssueImportReceipt seedCompleted(String ownerId) {
      var receipt =
          new IssueImportReceipt(
              UUID.randomUUID(),
              UUID.randomUUID(),
              "octocat/Hello-World",
              "completed",
              2,
              0,
              0,
              false,
              null,
              Instant.parse("2026-09-01T08:00:00Z"),
              Instant.parse("2026-09-01T08:00:05Z"));
      owners.put(receipt.id(), ownerId);
      rows.put(receipt.id(), receipt);
      return receipt;
    }

    IssueImportReceipt seedRunning(String ownerId, Instant startedAt) {
      var receipt =
          new IssueImportReceipt(
              UUID.randomUUID(),
              UUID.randomUUID(),
              "octocat/Hello-World",
              "running",
              0,
              0,
              0,
              false,
              null,
              startedAt,
              null);
      owners.put(receipt.id(), ownerId);
      rows.put(receipt.id(), receipt);
      return receipt;
    }

    IssueImportReceipt current(UUID importId) {
      return rows.get(importId);
    }

    int size() {
      return rows.size();
    }
  }

  static final class FakeIssueSource implements IssueSource {
    private final List<String> calls = new ArrayList<>();
    private final Map<Integer, IssuePage> pages = new HashMap<>();
    private final Map<Integer, IssueSourceException> pageFailures = new HashMap<>();
    private RepositoryIdentity identity = new RepositoryIdentity("octocat/Hello-World", "octocat");
    private IssueSourceException failure;
    private String lastToken;

    void identify(String fullName, String login) {
      identity = new RepositoryIdentity(fullName, login);
      failure = null;
    }

    void fail(IssueSourceException error) {
      failure = error;
    }

    void failOnPage(int number, IssueSourceException error) {
      pageFailures.put(number, error);
    }

    void page(int number, IssuePage page) {
      pages.put(number, page);
    }

    List<String> calls() {
      return List.copyOf(calls);
    }

    String lastToken() {
      return lastToken;
    }

    @Override
    public RepositoryIdentity verify(String repository, String token) {
      calls.add("verify " + repository + " " + token);
      lastToken = token;
      if (failure != null) throw failure;
      return identity;
    }

    @Override
    public IssuePage list(String repository, String token, int page) {
      calls.add("list " + repository + " page=" + page);
      lastToken = token;
      if (failure != null) throw failure;
      var pageFailure = pageFailures.get(page);
      if (pageFailure != null) throw pageFailure;
      return pages.getOrDefault(page, new IssuePage(List.of(), 0, false));
    }
  }

  /** Proyectos del propietario: sólo lo que el conector necesita saber de ellos. */
  static final class FakeProjects implements ProjectQueries {
    private final Map<UUID, com.apptolast.organization.domain.Project> rows = new LinkedHashMap<>();

    UUID seed(String ownerId, String status) {
      var id = UUID.randomUUID();
      rows.put(id, project(id, ownerId, status));
      return id;
    }

    void status(UUID id, String status) {
      var row = rows.get(id);
      rows.put(id, project(id, row.ownerId(), status));
    }

    private static com.apptolast.organization.domain.Project project(
        UUID id, String ownerId, String status) {
      var now = Instant.parse("2026-09-01T08:00:00Z");
      return new com.apptolast.organization.domain.Project(
          id, ownerId, "Proyecto", "", status, now, now);
    }

    @Override
    public List<com.apptolast.organization.domain.ProjectSummary> list(
        String ownerId, com.apptolast.organization.domain.ProjectPosition after, int limit) {
      throw new UnsupportedOperationException("El conector no lista proyectos");
    }

    @Override
    public Optional<com.apptolast.organization.domain.ProjectSnapshot> find(
        String ownerId, UUID id) {
      return Optional.ofNullable(rows.get(id))
          .filter(row -> row.ownerId().equals(ownerId))
          .map(row -> new com.apptolast.organization.domain.ProjectSnapshot(row, 1));
    }
  }

  /** Confirma tarea, evento y enlace juntos, o revierte los tres. */
  static final class FakeImportedTaskCommit implements ImportedTaskCommit {
    private final Map<String, UUID> links = new LinkedHashMap<>();
    private final List<com.apptolast.organization.domain.Task> tasks = new ArrayList<>();
    private final List<UUID> events = new ArrayList<>();
    private String projectStatus = "idea";
    private String storageFailureOn;
    private String completedOn;

    void failStorageOn(String externalId) {
      storageFailureOn = externalId;
    }

    void completeProjectOn(String externalId) {
      completedOn = externalId;
    }

    void seedLink(String ownerId, String externalId) {
      links.put(ownerId + "|github|" + externalId, UUID.randomUUID());
    }

    int tasks() {
      return tasks.size();
    }

    com.apptolast.organization.domain.Task lastTask() {
      return tasks.getLast();
    }

    int events() {
      return events.size();
    }

    int links() {
      return links.size();
    }

    @Override
    public boolean save(
        String ownerId,
        UUID projectId,
        ExternalIssue issue,
        Function<String, TaskCreation> operation) {
      if (issue.externalId().equals(storageFailureOn))
        throw new StorageUnavailableException(new IllegalStateException("link insert failed"));
      if (issue.externalId().equals(completedOn)) projectStatus = "completed";
      var key = ownerId + "|github|" + issue.externalId();
      if (links.containsKey(key)) return false;
      var creation = operation.apply(projectStatus);
      tasks.add(creation.task());
      events.add(creation.event().eventId());
      links.put(key, creation.task().id());
      return true;
    }
  }
}
