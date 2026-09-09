package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.ExternalIssue;
import com.apptolast.organization.domain.Task;
import com.apptolast.organization.support.TestDatabase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @s1 la fila de conexión sólo guarda texto cifrado, @s9 @s11 reconectar y desconectar conservan
 *     enlaces y recibos, @s12 @s17 @s19 tarea, evento y enlace confirman o revierten
 *     juntos, @s25 @s26 un solo recibo en curso por propietario y @s27 lo confirmado sobrevive.
 */
@Testcontainers
class GithubConnectorPersistenceTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  private static final String OWNER = "owner-a";
  private static final String OTHER = "owner-b";
  private static final String REPOSITORY = "octocat/Hello-World";
  private static final Instant NOW = Instant.parse("2026-09-09T12:00:00Z");

  static JdbcTemplate jdbc;
  static DataSourceTransactionManager manager;

  private PostgresConnectorConnectionStore connections;
  private PostgresIssueImportReceiptStore receipts;
  private PostgresImportedTaskCommit commit;
  private UUID projectId;

  @BeforeAll
  static void database() {
    var source =
        new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    Flyway.configure().dataSource(source).load().migrate();
    jdbc = new JdbcTemplate(source);
    manager = new DataSourceTransactionManager(source);
  }

  @BeforeEach
  void setUp() {
    // Se vacía descubriendo las tablas, no enumerándolas: fue la V25 de este carril, con su
    // task_external_links apuntando a tasks, la que tumbó las listas escritas a mano.
    TestDatabase.empty(jdbc);
    var transaction = new TransactionTemplate(manager);
    connections = new PostgresConnectorConnectionStore(jdbc, transaction);
    receipts = new PostgresIssueImportReceiptStore(jdbc, transaction);
    commit = new PostgresImportedTaskCommit(jdbc, transaction, eventJson());
    projectId = seedProject(OWNER, "idea");
  }

  /** El mismo Jackson que configura la aplicación: los instantes viajan en ISO-8601. */
  private static ObjectMapper eventJson() {
    return new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  private UUID seedProject(String ownerId, String status) {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,?,?,?,?,?,?)",
        id,
        ownerId,
        "Proyecto",
        "",
        status,
        Timestamp.from(NOW.minusSeconds(3600)),
        Timestamp.from(NOW.minusSeconds(3600)));
    return id;
  }

  private static StoredConnection connection(String repository, String status, byte[] token) {
    return new StoredConnection(repository, "octocat", status, token, NOW);
  }

  private static byte[] ciphertext(String marker) {
    var bytes = new byte[43];
    bytes[0] = (byte) marker.charAt(0);
    return bytes;
  }

  private static ExternalIssue issue(String id) {
    return new ExternalIssue(id, "Issue " + id, null, "https://github.com/octocat/x/issues/" + id);
  }

  private boolean save(String ownerId, UUID project, ExternalIssue external) {
    return commit.save(
        ownerId, project, "github", external, status -> creation(ownerId, project, external));
  }

  private static TaskCreation creation(String ownerId, UUID project, ExternalIssue external) {
    var task =
        Task.create(
            UUID.randomUUID(),
            project,
            external.taskTitle(),
            external.taskCompletionCriterion(),
            null,
            NOW);
    return new TaskCreation(
        task,
        new TaskCreated(
            UUID.randomUUID(),
            project,
            ownerId,
            task.createdAt(),
            1,
            "TaskCreated.v1",
            task.id(),
            task.title()));
  }

  private int count(String table) {
    return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
  }

  // ------------------------------------------------------------------------- @s1 conexión

  @Test
  void s1_theRowKeepsTheCipheredTokenAndNothingElseAboutIt() {
    connections.save(OWNER, connection(REPOSITORY, "valid", ciphertext("A")));

    var stored = connections.find(OWNER).orElseThrow();
    assertThat(stored.repository()).isEqualTo(REPOSITORY);
    assertThat(stored.login()).isEqualTo("octocat");
    assertThat(stored.status()).isEqualTo("valid");
    assertThat(stored.connectedAt()).isEqualTo(NOW);
    assertThat(stored.tokenCiphertext()).hasSize(43);
    assertThat(count("connector_connections")).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT octet_length(token_ciphertext) FROM connector_connections WHERE owner_id=?",
                Integer.class,
                OWNER))
        .isEqualTo(43);
  }

  @Test
  void s1_theRowIsPrivateToItsOwner() {
    connections.save(OWNER, connection(REPOSITORY, "valid", ciphertext("A")));

    assertThat(connections.find(OTHER)).isEmpty();
  }

  @Test
  void s9_savingAgainReplacesTheSingleRowOfTheOwner() {
    connections.save(OWNER, connection(REPOSITORY, "valid", ciphertext("A")));
    connections.save(OWNER, connection("octocat/Segundo", "valid", ciphertext("B")));

    var stored = connections.find(OWNER).orElseThrow();
    assertThat(stored.repository()).isEqualTo("octocat/Segundo");
    assertThat(stored.tokenCiphertext()[0]).isEqualTo((byte) 'B');
    assertThat(count("connector_connections")).isEqualTo(1);
  }

  @Test
  void s22_invalidatingOnlyChangesTheStatus() {
    connections.save(OWNER, connection(REPOSITORY, "valid", ciphertext("A")));

    connections.invalidate(OWNER);

    var stored = connections.find(OWNER).orElseThrow();
    assertThat(stored.status()).isEqualTo("invalid");
    assertThat(stored.repository()).isEqualTo(REPOSITORY);
    assertThat(stored.tokenCiphertext()[0]).isEqualTo((byte) 'A');
  }

  @Test
  void s11_deletingIsIdempotentAndLeavesNoCopyOfTheCiphertext() {
    connections.save(OWNER, connection(REPOSITORY, "valid", ciphertext("A")));

    assertThat(connections.delete(OWNER)).isTrue();
    assertThat(connections.delete(OWNER)).isFalse();
    assertThat(count("connector_connections")).isZero();
  }

  @Test
  void s1_theSchemaRefusesAnUnknownStatusAndAnImplausibleCiphertext() {
    assertThatThrownBy(
            () -> connections.save(OWNER, connection(REPOSITORY, "caducada", ciphertext("A"))))
        .isInstanceOf(StorageUnavailableException.class);
    assertThatThrownBy(() -> connections.save(OWNER, connection(REPOSITORY, "valid", new byte[8])))
        .isInstanceOf(StorageUnavailableException.class);
    assertThat(count("connector_connections")).isZero();
  }

  // --------------------------------------------------------- @s12 @s17 @s19 tarea y enlace

  @Test
  void s12_oneIssueCommitsTaskEventAndLinkTogether() {
    assertThat(save(OWNER, projectId, issue("101"))).isTrue();

    assertThat(count("tasks")).isEqualTo(1);
    assertThat(count("outbox_events")).isEqualTo(1);
    assertThat(count("task_external_links")).isEqualTo(1);
    var link =
        jdbc.queryForMap(
            "SELECT source, external_id, url, task_id FROM task_external_links WHERE owner_id=?",
            OWNER);
    assertThat(link.get("source")).isEqualTo("github");
    assertThat(link.get("external_id")).isEqualTo("101");
    assertThat(link.get("url")).isEqualTo("https://github.com/octocat/x/issues/101");
    assertThat(link.get("task_id"))
        .isEqualTo(jdbc.queryForObject("SELECT id FROM tasks", UUID.class));
    assertThat(jdbc.queryForObject("SELECT event_type FROM outbox_events", String.class))
        .isEqualTo("TaskCreated.v1");
  }

  @Test
  void s17_repeatingTheSameIssueSkipsWithoutCreatingAnything() {
    save(OWNER, projectId, issue("101"));

    assertThat(save(OWNER, projectId, issue("101"))).isFalse();

    assertThat(count("tasks")).isEqualTo(1);
    assertThat(count("outbox_events")).isEqualTo(1);
    assertThat(count("task_external_links")).isEqualTo(1);
  }

  @Test
  void s33_twoOwnersMayLinkTheSameExternalIdentifier() {
    var otherProject = seedProject(OTHER, "idea");

    assertThat(save(OWNER, projectId, issue("101"))).isTrue();
    assertThat(save(OTHER, otherProject, issue("101"))).isTrue();

    assertThat(count("task_external_links")).isEqualTo(2);
  }

  @Test
  void s17_theSchemaForbidsTwoLinksForTheSameOwnerSourceAndExternalIdentifier() {
    save(OWNER, projectId, issue("101"));
    var taskId = jdbc.queryForObject("SELECT id FROM tasks", UUID.class);

    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO task_external_links(owner_id,source,external_id,task_id,url,linked_at) VALUES (?,?,?,?,?,?)",
                    OWNER,
                    "github",
                    "101",
                    taskId,
                    "https://x",
                    Timestamp.from(NOW)))
        .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
  }

  @Test
  void s19_aFailureWhileCommittingLeavesNeitherTaskNorEventNorLink() {
    assertThatThrownBy(
            () ->
                commit.save(
                    OWNER,
                    projectId,
                    "github",
                    issue("101"),
                    status -> {
                      throw new StorageUnavailableException(new IllegalStateException("caída"));
                    }))
        .isInstanceOf(StorageUnavailableException.class);

    assertThat(count("tasks")).isZero();
    assertThat(count("outbox_events")).isZero();
    assertThat(count("task_external_links")).isZero();
  }

  @Test
  void s23_committingToAnUnknownOrForeignProjectIsAMissingResource() {
    var foreign = seedProject(OTHER, "idea");

    assertThatThrownBy(() -> save(OWNER, foreign, issue("101")))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> save(OWNER, UUID.randomUUID(), issue("101")))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThat(count("tasks")).isZero();
  }

  @Test
  void s24_theCallbackSeesTheCurrentProjectStatus() {
    jdbc.update("UPDATE projects SET status='completed' WHERE id=?", projectId);

    assertThatThrownBy(
            () ->
                commit.save(
                    OWNER,
                    projectId,
                    "github",
                    issue("101"),
                    status -> {
                      if ("completed".equals(status)) throw new ProjectCompletedException();
                      return creation(OWNER, projectId, issue("101"));
                    }))
        .isInstanceOf(ProjectCompletedException.class);

    assertThat(count("tasks")).isZero();
    assertThat(count("task_external_links")).isZero();
  }

  // ------------------------------------------------------------ @s25 @s26 @s27 @s30 recibos

  @Test
  void s12_beginningARunningReceiptRecordsItWithZeroedCounters() {
    var receipt =
        receipts.begin(OWNER, projectId, "github", REPOSITORY, NOW, NOW.minus(Duration.ofMinutes(15)));

    assertThat(receipt.status()).isEqualTo("running");
    assertThat(receipt.created()).isZero();
    assertThat(receipt.projectId()).isEqualTo(projectId);
    assertThat(receipt.projectPath()).isEqualTo(REPOSITORY);
    assertThat(receipt.source()).isEqualTo("github");
    assertThat(receipt.startedAt()).isEqualTo(NOW);
    assertThat(receipt.finishedAt()).isNull();
    assertThat(receipts.find(OWNER, receipt.id())).contains(receipt);
  }

  @Test
  void s25_asecondRunningReceiptOfTheSameOwnerIsRefused() {
    receipts.begin(OWNER, projectId, "github", REPOSITORY, NOW, NOW.minus(Duration.ofMinutes(15)));

    assertThatThrownBy(
            () ->
                receipts.begin(
                    OWNER,
                    projectId,
                    "github",
                    REPOSITORY,
                    NOW,
                    NOW.minus(Duration.ofMinutes(15))))
        .isInstanceOf(IssueImportInProgressException.class);
    assertThat(count("issue_import_receipts")).isEqualTo(1);
  }

  @Test
  void s25_twoRealSimultaneousImportsLeaveExactlyOneReceiptRunning() throws Exception {
    int attempts = 8;
    var start = new java.util.concurrent.CyclicBarrier(attempts);
    var pool = java.util.concurrent.Executors.newFixedThreadPool(attempts);
    var began = new java.util.concurrent.atomic.AtomicInteger();
    var refused = new java.util.concurrent.atomic.AtomicInteger();
    try {
      var results =
          pool.invokeAll(
              java.util.stream.IntStream.range(0, attempts)
                  .<java.util.concurrent.Callable<Void>>mapToObj(
                      n ->
                          () -> {
                            start.await();
                            try {
                              receipts.begin(
                                  OWNER,
                                  projectId,
                                  "github",
                                  REPOSITORY,
                                  NOW,
                                  NOW.minus(Duration.ofMinutes(15)));
                              began.incrementAndGet();
                            } catch (IssueImportInProgressException expected) {
                              refused.incrementAndGet();
                            }
                            return null;
                          })
                  .toList());
      for (var result : results) result.get();
    } finally {
      pool.shutdownNow();
    }

    assertThat(began.get()).isEqualTo(1);
    assertThat(refused.get()).isEqualTo(attempts - 1);
    assertThat(count("issue_import_receipts")).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM issue_import_receipts WHERE owner_id=? AND status='running'",
                Integer.class,
                OWNER))
        .isEqualTo(1);
  }

  @Test
  void s25_anotherOwnerMayImportAtTheSameTime() {
    var otherProject = seedProject(OTHER, "idea");
    receipts.begin(OWNER, projectId, "github", REPOSITORY, NOW, NOW.minus(Duration.ofMinutes(15)));

    assertThat(
            receipts.begin(
                OTHER,
                otherProject,
                "github",
                "otra/Cosa",
                NOW,
                NOW.minus(Duration.ofMinutes(15))))
        .isNotNull();
    assertThat(count("issue_import_receipts")).isEqualTo(2);
  }

  @Test
  void s26_anAbandonedRunningReceiptIsInterruptedAndYieldsItsTurn() {
    var stale =
        receipts.begin(
            OWNER,
            projectId,
            "github",
            REPOSITORY,
            NOW.minusSeconds(1000),
            NOW.minus(Duration.ofMinutes(15)));
    receipts.progress(OWNER, stale.id(), 2, 1, 0);

    var fresh =
        receipts.begin(OWNER, projectId, "github", REPOSITORY, NOW, NOW.minus(Duration.ofMinutes(15)));

    assertThat(fresh.status()).isEqualTo("running");
    var interrupted = receipts.find(OWNER, stale.id()).orElseThrow();
    assertThat(interrupted.status()).isEqualTo("failed");
    assertThat(interrupted.errorCode()).isEqualTo("INTERRUPTED");
    assertThat(interrupted.finishedAt()).isNotNull();
    assertThat(interrupted.created()).isEqualTo(2);
    assertThat(interrupted.skipped()).isEqualTo(1);
  }

  @Test
  void s27_progressIsVisibleWhileTheReceiptIsStillRunning() {
    var receipt =
        receipts.begin(OWNER, projectId, "github", REPOSITORY, NOW, NOW.minus(Duration.ofMinutes(15)));

    receipts.progress(OWNER, receipt.id(), 2, 0, 0);

    var reread = receipts.find(OWNER, receipt.id()).orElseThrow();
    assertThat(reread.status()).isEqualTo("running");
    assertThat(reread.created()).isEqualTo(2);
    assertThat(reread.errorCode()).isNull();
    assertThat(reread.finishedAt()).isNull();
  }

  @Test
  void s12_finishingClosesTheReceiptWithItsCountersAndInstant() {
    var receipt =
        receipts.begin(OWNER, projectId, "github", REPOSITORY, NOW, NOW.minus(Duration.ofMinutes(15)));
    receipts.progress(OWNER, receipt.id(), 3, 1, 1);

    var closed = receipts.finish(OWNER, receipt.id(), "completed", null, true, NOW.plusSeconds(4));

    assertThat(closed.status()).isEqualTo("completed");
    assertThat(closed.created()).isEqualTo(3);
    assertThat(closed.skipped()).isEqualTo(1);
    assertThat(closed.failed()).isEqualTo(1);
    assertThat(closed.truncated()).isTrue();
    assertThat(closed.errorCode()).isNull();
    assertThat(closed.finishedAt()).isEqualTo(NOW.plusSeconds(4));
    assertThat(receipts.find(OWNER, receipt.id())).contains(closed);
  }

  @Test
  void s20_afailedReceiptKeepsItsErrorCode() {
    var receipt =
        receipts.begin(OWNER, projectId, "github", REPOSITORY, NOW, NOW.minus(Duration.ofMinutes(15)));

    var closed =
        receipts.finish(OWNER, receipt.id(), "failed", "RATE_LIMITED", false, NOW.plusSeconds(1));

    assertThat(closed.status()).isEqualTo("failed");
    assertThat(closed.errorCode()).isEqualTo("RATE_LIMITED");
  }

  @Test
  void s10_theLatestReceiptIsTheOneWithTheGreatestStartInstant() {
    var older =
        receipts.begin(OWNER, projectId, "github", REPOSITORY, NOW.minusSeconds(100), NOW.minusSeconds(9000));
    receipts.finish(OWNER, older.id(), "completed", null, false, NOW.minusSeconds(99));
    var newer =
        receipts.begin(OWNER, projectId, "github", REPOSITORY, NOW, NOW.minus(Duration.ofMinutes(15)));

    assertThat(receipts.latest(OWNER, "github").orElseThrow().id()).isEqualTo(newer.id());
  }

  @Test
  void s33_receiptsNeverCrossOwners() {
    var otherProject = seedProject(OTHER, "idea");
    var theirs =
        receipts.begin(OTHER, otherProject, "github", "otra/Cosa", NOW, NOW.minus(Duration.ofMinutes(15)));

    assertThat(receipts.find(OWNER, theirs.id())).isEmpty();
    assertThat(receipts.latest(OWNER, "github")).isEmpty();
  }

  @Test
  void s11_receiptsAndLinksSurviveDisconnecting() {
    connections.save(OWNER, connection(REPOSITORY, "valid", ciphertext("A")));
    save(OWNER, projectId, issue("101"));
    var receipt =
        receipts.begin(OWNER, projectId, "github", REPOSITORY, NOW, NOW.minus(Duration.ofMinutes(15)));
    receipts.finish(OWNER, receipt.id(), "completed", null, false, NOW.plusSeconds(1));

    connections.delete(OWNER);

    assertThat(count("task_external_links")).isEqualTo(1);
    assertThat(count("tasks")).isEqualTo(1);
    assertThat(receipts.find(OWNER, receipt.id())).isPresent();
  }

  @Test
  void s26_theSchemaRefusesAReceiptWhoseStateAndInstantsDisagree() {
    var id = UUID.randomUUID();
    assertThatThrownBy(() -> insertReceipt(id, "running", null, NOW.plusSeconds(1)))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    assertThatThrownBy(() -> insertReceipt(id, "completed", null, null))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    assertThatThrownBy(() -> insertReceipt(id, "running", "RATE_LIMITED", null))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    assertThatThrownBy(() -> insertReceipt(id, "cancelado", null, NOW))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    assertThat(count("issue_import_receipts")).isZero();
  }

  private void insertReceipt(UUID id, String status, String errorCode, Instant finishedAt) {
    jdbc.update(
        "INSERT INTO issue_import_receipts(id,owner_id,source,project_id,project_path,status,created,skipped,failed,truncated,error_code,started_at,finished_at)"
            + " VALUES (?,?,?,?,?,0,0,0,false,?,?,?)",
        id,
        OWNER,
        projectId,
        REPOSITORY,
        status,
        errorCode,
        Timestamp.from(NOW),
        finishedAt == null ? null : Timestamp.from(finishedAt));
  }

  // ------------------------------------------------- el conjunto completo, extremo a extremo

  @Test
  void s19_anImportThatBreaksHalfwayLeavesExactlyWhatWasConfirmed() {
    var receipt =
        receipts.begin(OWNER, projectId, "github", REPOSITORY, NOW, NOW.minus(Duration.ofMinutes(15)));
    int created = 0;
    for (var external : List.of(issue("1"), issue("2"))) {
      if (save(OWNER, projectId, external)) created++;
      receipts.progress(OWNER, receipt.id(), created, 0, 0);
    }
    receipts.finish(
        OWNER, receipt.id(), "failed", "STORAGE_UNAVAILABLE", false, NOW.plusSeconds(2));

    var closed = receipts.find(OWNER, receipt.id()).orElseThrow();
    assertThat(closed.created()).isEqualTo(2);
    assertThat(closed.errorCode()).isEqualTo("STORAGE_UNAVAILABLE");
    assertThat(count("tasks")).isEqualTo(2);
    assertThat(count("outbox_events")).isEqualTo(2);
    assertThat(count("task_external_links")).isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM task_external_links l JOIN tasks t ON t.id = l.task_id",
                Integer.class))
        .isEqualTo(2);
  }
}
