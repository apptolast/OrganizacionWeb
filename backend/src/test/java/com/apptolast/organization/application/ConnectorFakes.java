package com.apptolast.organization.application;

import com.apptolast.organization.domain.ExternalIssue;
import com.apptolast.organization.domain.ImportReceipt;
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
    public String decrypt(String ownerId, byte[] ciphertext) {
      if (!enabled) throw new ConnectorsDisabledException();
      var parts = new String(ciphertext, StandardCharsets.UTF_8).split("\\|", 3);
      if (parts.length != 3 || !parts[1].equals(ownerId)) throw new SecretUndecipherableException();
      return reversed(parts[2]);
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

  static final class FakeReceipts implements ImportReceiptStore {
    private final Map<UUID, ImportReceipt> rows = new LinkedHashMap<>();
    private final Map<UUID, String> owners = new HashMap<>();

    @Override
    public ImportReceipt begin(
        String ownerId, UUID projectId, String repository, Instant startedAt, Instant staleBefore) {
      interruptStale(ownerId, staleBefore);
      if (running(ownerId).isPresent()) throw new ImportInProgressException();
      var receipt =
          new ImportReceipt(
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
      rows.computeIfPresent(importId, (id, row) -> row.withCounters(created, skipped, failed));
    }

    @Override
    public ImportReceipt finish(
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
    public Optional<ImportReceipt> find(String ownerId, UUID importId) {
      return Optional.ofNullable(rows.get(importId))
          .filter(row -> ownerId.equals(owners.get(row.id())));
    }

    @Override
    public Optional<ImportReceipt> latest(String ownerId) {
      return rows.values().stream()
          .filter(row -> ownerId.equals(owners.get(row.id())))
          .max(java.util.Comparator.comparing(ImportReceipt::startedAt));
    }

    private Optional<ImportReceipt> running(String ownerId) {
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

    ImportReceipt seedCompleted(String ownerId) {
      var receipt =
          new ImportReceipt(
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

    ImportReceipt seedRunning(String ownerId, Instant startedAt) {
      var receipt =
          new ImportReceipt(
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

    ImportReceipt current(UUID importId) {
      return rows.get(importId);
    }

    int size() {
      return rows.size();
    }
  }

  static final class FakeIssueSource implements IssueSource {
    private final List<String> calls = new ArrayList<>();
    private final Map<Integer, IssuePage> pages = new HashMap<>();
    private RepositoryIdentity identity = new RepositoryIdentity("octocat/Hello-World", "octocat");
    private IssueSourceException failure;

    void identify(String fullName, String login) {
      identity = new RepositoryIdentity(fullName, login);
      failure = null;
    }

    void fail(IssueSourceException error) {
      failure = error;
    }

    void page(int number, IssuePage page) {
      pages.put(number, page);
    }

    List<String> calls() {
      return List.copyOf(calls);
    }

    @Override
    public RepositoryIdentity verify(String repository, String token) {
      calls.add("verify " + repository + " " + token);
      if (failure != null) throw failure;
      return identity;
    }

    @Override
    public IssuePage list(String repository, String token, int page) {
      calls.add("list " + repository + " page=" + page);
      if (failure != null) throw failure;
      return pages.getOrDefault(page, new IssuePage(List.of(), 0, false));
    }
  }

  /** Confirma tarea, evento y enlace juntos, o revierte los tres. */
  static final class FakeImportedTaskCommit implements ImportedTaskCommit {
    private final Map<String, UUID> links = new LinkedHashMap<>();
    private final List<UUID> tasks = new ArrayList<>();
    private final List<UUID> events = new ArrayList<>();
    private String projectStatus = "idea";
    private String storageFailureOn;
    private String completedOn;

    void projectStatus(String status) {
      projectStatus = status;
    }

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

    int events() {
      return events.size();
    }

    int links() {
      return links.size();
    }

    @Override
    public boolean save(
        String ownerId, UUID projectId, ExternalIssue issue, Function<String, TaskCreation> operation) {
      if (issue.externalId().equals(storageFailureOn))
        throw new StorageUnavailableException(new IllegalStateException("link insert failed"));
      if (issue.externalId().equals(completedOn)) projectStatus = "completed";
      var key = ownerId + "|github|" + issue.externalId();
      if (links.containsKey(key)) return false;
      var creation = operation.apply(projectStatus);
      tasks.add(creation.task().id());
      events.add(creation.event().eventId());
      links.put(key, creation.task().id());
      return true;
    }
  }
}
