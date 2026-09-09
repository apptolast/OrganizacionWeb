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
 * @s9 la fila de conexión sólo guarda texto cifrado y el esquema lo comprueba, @s13 @s14 sustituir
 *     y soltar la conexión conservan tareas, enlaces y recibos, @s19 la unicidad del enlace es por
 *     origen y @s27 el guardián de importación es por propietario y no por proveedor.
 */
@Testcontainers
class GitlabConnectorPersistenceTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  private static final String OWNER = "owner-a";
  private static final String OTHER = "owner-b";
  private static final String PROJECT_PATH = "grupo/proyecto";
  private static final Instant NOW = Instant.parse("2026-09-09T12:00:00Z");
  private static final Duration FIFTEEN_MINUTES = Duration.ofMinutes(15);

  static JdbcTemplate jdbc;
  static DataSourceTransactionManager manager;

  private PostgresGitlabConnectionStore connections;
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
    TestDatabase.empty(jdbc);
    var transaction = new TransactionTemplate(manager);
    connections = new PostgresGitlabConnectionStore(jdbc, transaction);
    receipts = new PostgresIssueImportReceiptStore(jdbc, transaction);
    commit = new PostgresImportedTaskCommit(jdbc, transaction, eventJson());
    projectId = seedProject(OWNER, "idea");
  }

  private static ObjectMapper eventJson() {
    return new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  private UUID seedProject(String ownerId, String status) {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at)"
            + " VALUES (?,?,?,?,?,?,?)",
        id,
        ownerId,
        "Proyecto",
        "",
        status,
        Timestamp.from(NOW.minusSeconds(3600)),
        Timestamp.from(NOW.minusSeconds(3600)));
    return id;
  }

  private static GitlabConnection connection(String status, byte[] token, long version) {
    return new GitlabConnection(
        PROJECT_PATH, 4821L, "WXYZ", status, token, NOW, null, null, version);
  }

  /** Un texto cifrado del tamaño que produce el formato de 27: versión, nonce, texto y etiqueta. */
  private static byte[] ciphertext(String marker) {
    var bytes = new byte[43];
    bytes[0] = (byte) marker.charAt(0);
    return bytes;
  }

  private static ExternalIssue issue(String id) {
    return new ExternalIssue(
        id, "Issue " + id, null, "https://gitlab.example.com/grupo/proyecto/-/issues/" + id);
  }

  private boolean link(String ownerId, String source, String externalId) {
    var external = issue(externalId);
    return commit.save(ownerId, projectId, source, external, status -> creation(ownerId, external));
  }

  private TaskCreation creation(String ownerId, ExternalIssue external) {
    var task =
        Task.create(UUID.randomUUID(), projectId, external.title(), external.url(), null, NOW);
    return new TaskCreation(
        task,
        new TaskCreated(
            UUID.randomUUID(),
            projectId,
            ownerId,
            NOW,
            1,
            "TaskCreated.v1",
            task.id(),
            task.title()));
  }

  private int count(String table) {
    return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
  }

  // ------------------------------------------------------------------- @s9 la fila cifrada

  @Test
  void s9_theStoredRowKeepsCiphertextAndNothingThatLooksLikeAToken() {
    connections.save(OWNER, connection(GitlabConnection.CONNECTED, ciphertext("A"), 1L));

    var stored = connections.find(OWNER).orElseThrow();

    assertThat(stored.projectPath()).isEqualTo(PROJECT_PATH);
    assertThat(stored.projectId()).isEqualTo(4821L);
    assertThat(stored.tokenHint()).isEqualTo("WXYZ");
    assertThat(stored.status()).isEqualTo("connected");
    assertThat(stored.version()).isEqualTo(1L);
    assertThat(stored.lastError()).isNull();
    assertThat(jdbc.queryForList("SELECT * FROM gitlab_connections").getFirst().keySet())
        .doesNotContain("token", "token_plain");
  }

  @Test
  void s9_theSchemaRefusesAnythingTooShortToBeSealedWithNonceAndTag() {
    assertThatThrownBy(
            () -> connections.save(OWNER, connection(GitlabConnection.CONNECTED, new byte[8], 1L)))
        .isInstanceOf(StorageUnavailableException.class);
    assertThat(count("gitlab_connections")).isZero();
  }

  @Test
  void s9_theSchemaRefusesAStatusThatIsNeitherConnectedNorError() {
    assertThatThrownBy(() -> connections.save(OWNER, connection("caducada", ciphertext("A"), 1L)))
        .isInstanceOf(StorageUnavailableException.class);
  }

  @Test
  void s4_theConnectionOfOneOwnerIsInvisibleToAnother() {
    connections.save(OWNER, connection(GitlabConnection.CONNECTED, ciphertext("A"), 1L));

    assertThat(connections.find(OTHER)).isEmpty();
  }

  // -------------------------------------------------------- @s13 @s14 sustituir y soltar

  @Test
  void s13_savingAgainReplacesTheOnlyRowAndKeepsTasksAndLinks() {
    connections.save(OWNER, connection(GitlabConnection.CONNECTED, ciphertext("A"), 1L));
    link(OWNER, "gitlab", "gitlab.example.com:9001");

    connections.save(OWNER, connection(GitlabConnection.CONNECTED, ciphertext("B"), 2L));

    var stored = connections.find(OWNER).orElseThrow();
    assertThat(stored.version()).isEqualTo(2L);
    assertThat(stored.tokenCiphertext()[0]).isEqualTo((byte) 'B');
    assertThat(count("gitlab_connections")).isEqualTo(1);
    assertThat(count("tasks")).isEqualTo(1);
    assertThat(count("task_external_links")).isEqualTo(1);
  }

  @Test
  void s14_deletingIsIdempotentAndLeavesTasksLinksAndReceiptsUntouched() {
    connections.save(OWNER, connection(GitlabConnection.CONNECTED, ciphertext("A"), 1L));
    link(OWNER, "gitlab", "gitlab.example.com:9001");
    receipts.begin(OWNER, projectId, "gitlab", PROJECT_PATH, NOW, NOW.minus(FIFTEEN_MINUTES));

    assertThat(connections.delete(OWNER)).isTrue();
    assertThat(connections.delete(OWNER)).isFalse();

    assertThat(count("gitlab_connections")).isZero();
    assertThat(count("tasks")).isEqualTo(1);
    assertThat(count("task_external_links")).isEqualTo(1);
    assertThat(count("issue_import_receipts")).isEqualTo(1);
  }

  @Test
  void s23_annotatingTheLastErrorKeepsTheCiphertextAndRaisesNothingElse() {
    connections.save(OWNER, connection(GitlabConnection.CONNECTED, ciphertext("A"), 1L));

    connections.save(
        OWNER,
        connections.find(OWNER).orElseThrow().withError("CONNECTION_INVALID", NOW.plusSeconds(10)));

    var stored = connections.find(OWNER).orElseThrow();
    assertThat(stored.status()).isEqualTo("error");
    assertThat(stored.lastError().code()).isEqualTo("CONNECTION_INVALID");
    assertThat(stored.lastError().at()).isEqualTo(NOW.plusSeconds(10));
    assertThat(stored.tokenCiphertext()[0]).isEqualTo((byte) 'A');
  }

  // ------------------------------------------------------------- @s19 unicidad por origen

  @Test
  void s19_theSameExternalIdLivesOncePerSourceAndOnceMorePerOwner() {
    assertThat(link(OWNER, "github", "gitlab.example.com:42")).isTrue();
    assertThat(link(OWNER, "gitlab", "gitlab.example.com:42")).isTrue();
    assertThat(link(OWNER, "gitlab", "gitlab.example.com:42")).isFalse();

    var otherProject = seedProject(OTHER, "idea");
    assertThat(
            commit.save(
                OTHER,
                otherProject,
                "gitlab",
                issue("gitlab.example.com:42"),
                status -> {
                  var task =
                      Task.create(
                          UUID.randomUUID(),
                          otherProject,
                          "Issue ajena",
                          "https://gitlab.example.com/x",
                          null,
                          NOW);
                  return new TaskCreation(
                      task,
                      new TaskCreated(
                          UUID.randomUUID(),
                          otherProject,
                          OTHER,
                          NOW,
                          1,
                          "TaskCreated.v1",
                          task.id(),
                          task.title()));
                }))
        .isTrue();

    assertThat(count("task_external_links")).isEqualTo(3);
    assertThat(count("tasks")).isEqualTo(3);
  }

  @Test
  void s19_theSchemaRefusesASourceThatIsNeitherGithubNorGitlab() {
    assertThatThrownBy(() -> link(OWNER, "bitbucket", "x:1"))
        .isInstanceOf(StorageUnavailableException.class);
    assertThat(count("task_external_links")).isZero();
  }

  // ---------------------------------------------- @s27 @s28 el guardián por propietario

  @Test
  void s27_arunningReceiptOfOneSourceBlocksTheOtherAndTheOwnerIsSeenAsImporting() {
    receipts.begin(OWNER, projectId, "github", "octocat/x", NOW, NOW.minus(FIFTEEN_MINUTES));

    assertThatThrownBy(
            () ->
                receipts.begin(
                    OWNER, projectId, "gitlab", PROJECT_PATH, NOW, NOW.minus(FIFTEEN_MINUTES)))
        .isInstanceOf(IssueImportInProgressException.class);
    assertThat(receipts.importing(OWNER, NOW.minus(FIFTEEN_MINUTES))).isTrue();
    assertThat(receipts.importing(OTHER, NOW.minus(FIFTEEN_MINUTES))).isFalse();
  }

  @Test
  void s28_anAbandonedRunningReceiptStopsCountingAsAnImportInProgress() {
    receipts.begin(
        OWNER,
        projectId,
        "gitlab",
        PROJECT_PATH,
        NOW.minusSeconds(16 * 60),
        NOW.minusSeconds(16 * 60).minus(FIFTEEN_MINUTES));

    assertThat(receipts.importing(OWNER, NOW.minus(FIFTEEN_MINUTES))).isFalse();
    assertThat(receipts.importing(OWNER, NOW.minusSeconds(17 * 60))).isTrue();
  }

  @Test
  void s15_theLatestReceiptIsTheOneOfItsOwnSourceAndNotTheOther() {
    var gitlabReceipt =
        receipts.begin(
            OWNER,
            projectId,
            "gitlab",
            PROJECT_PATH,
            NOW.minusSeconds(600),
            NOW.minus(FIFTEEN_MINUTES));
    receipts.finish(OWNER, gitlabReceipt.id(), "completed", null, false, NOW.minusSeconds(590));
    var githubReceipt =
        receipts.begin(OWNER, projectId, "github", "octocat/x", NOW, NOW.minus(FIFTEEN_MINUTES));
    receipts.finish(OWNER, githubReceipt.id(), "completed", null, false, NOW.plusSeconds(5));

    assertThat(receipts.latest(OWNER, "gitlab").orElseThrow().id()).isEqualTo(gitlabReceipt.id());
    assertThat(receipts.latest(OWNER, "github").orElseThrow().id()).isEqualTo(githubReceipt.id());
    assertThat(receipts.latest(OTHER, "gitlab")).isEmpty();
  }
}
