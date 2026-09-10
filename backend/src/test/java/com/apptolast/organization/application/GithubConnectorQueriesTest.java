package com.apptolast.organization.application;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.domain.IssueImportReceipt;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @s10 consultar la conexión, @s11 desconectar, @s30 consultar un recibo y @s33 aislamiento.
 */
class GithubConnectorQueriesTest {
  private static final String OWNER = "owner-1";
  private static final String OTHER = "owner-2";
  private static final String SOURCE = GithubIssueConnections.SOURCE;

  private ConnectorFakes fakes;
  private ReadGithubConnection read;
  private DisconnectGithub disconnect;
  private ReadIssueImport readReceipt;

  @BeforeEach
  void setUp() {
    fakes = new ConnectorFakes();
    read = new ReadGithubConnection(fakes.connections, fakes.receipts, fakes.cipher);
    disconnect = new DisconnectGithub(fakes.connections, fakes.cipher);
    readReceipt = new ReadIssueImport(fakes.receipts, fakes.cipher);
  }

  private void seedConnection(String ownerId, String repository, String status) {
    fakes.connections.save(
        ownerId,
        new StoredConnection(
            repository,
            "octocat",
            status,
            fakes.cipher.encrypt(ownerId, "ghp_secreto123"),
            Instant.parse("2026-09-09T10:00:00Z")));
  }

  @Test
  void s10_withoutAConnectionTheAnswerIsConnectionNotFound() {
    assertThrows(ConnectionNotFoundException.class, () -> read.execute(OWNER));
  }

  @Test
  void s10_aValidConnectionWithoutImportsAnswersWithLastImportNull() {
    seedConnection(OWNER, "octocat/Hello-World", "valid");

    var view = read.execute(OWNER);

    assertEquals("octocat/Hello-World", view.repository());
    assertEquals("octocat", view.login());
    assertEquals("valid", view.status());
    assertEquals(Instant.parse("2026-09-09T10:00:00Z"), view.connectedAt());
    assertNull(view.lastImport());
  }

  @Test
  void s10_lastImportIsTheReceiptWithTheGreatestStartInstant() {
    seedConnection(OWNER, "octocat/Hello-World", "invalid");
    fakes.receipts.seedCompleted(OWNER);
    var newest = fakes.receipts.seedRunning(OWNER, Instant.parse("2026-09-05T08:00:00Z"));
    fakes.receipts.seedCompleted(OWNER);

    var view = read.execute(OWNER);

    assertEquals("invalid", view.status());
    assertEquals(newest, view.lastImport());
    assertEquals("running", view.lastImport().status());
    assertNull(view.lastImport().errorCode());
    assertNull(view.lastImport().finishedAt());
  }

  @Test
  void s33_lastImportNeverLeaksAcrossOwners() {
    seedConnection(OWNER, "otra/Cosa", "valid");
    fakes.receipts.seedCompleted(OTHER);

    assertNull(read.execute(OWNER).lastImport());
  }

  @Test
  void s11_disconnectingTwiceRemovesTheRowAndKeepsTheReceipts() {
    seedConnection(OWNER, "octocat/Hello-World", "valid");
    var receipt = fakes.receipts.seedCompleted(OWNER);

    disconnect.execute(OWNER);
    disconnect.execute(OWNER);

    assertEquals(0, fakes.connections.size());
    assertThrows(ConnectionNotFoundException.class, () -> read.execute(OWNER));
    assertEquals(receipt, readReceipt.execute(OWNER, SOURCE, receipt.id()));
  }

  @Test
  void s11_disconnectingNeverTouchesAnotherOwner() {
    seedConnection(OWNER, "octocat/Hello-World", "valid");
    seedConnection(OTHER, "otra/Cosa", "valid");

    disconnect.execute(OWNER);

    assertEquals("otra/Cosa", read.execute(OTHER).repository());
  }

  @Test
  void s30_theOwnReceiptComesBackWholeAndTheForeignOneIsIndistinguishableFromMissing() {
    var mine = fakes.receipts.seedCompleted(OWNER);
    var theirs = fakes.receipts.seedCompleted(OTHER);

    IssueImportReceipt found = readReceipt.execute(OWNER, SOURCE, mine.id());

    assertEquals(mine, found);
    assertThrows(
        IssueImportNotFoundException.class, () -> readReceipt.execute(OWNER, SOURCE, theirs.id()));
    assertThrows(
        IssueImportNotFoundException.class,
        () -> readReceipt.execute(OWNER, SOURCE, UUID.randomUUID()));
  }

  /**
   * La ruta lleva el gestor en el camino, así que un recibo de GitLab no es «el recibo propio» de
   * GET /connectors/github/imports/{id}: es un recibo que esa ruta no conoce. El almacén ya filtra
   * por origen al buscar el más reciente; leer uno suelto no puede hacerlo de otra manera.
   */
  @Test
  void b_aReceiptOfAnotherConnectorIsNotVisibleThroughTheGithubRoute() {
    var mine = fakes.receipts.seedCompleted(OWNER, SOURCE);
    var elsewhere = fakes.receipts.seedCompleted(OWNER, "gitlab");

    assertEquals(mine, readReceipt.execute(OWNER, SOURCE, mine.id()));
    assertThrows(
        IssueImportNotFoundException.class,
        () -> readReceipt.execute(OWNER, SOURCE, elsewhere.id()));
  }

  @Test
  void s3_withoutTheConnectorKeyNoConnectorRouteReadsOrWrites() {
    seedConnection(OWNER, "octocat/Hello-World", "valid");
    var receipt = fakes.receipts.seedCompleted(OWNER);
    fakes.cipher.disable();

    assertThrows(ConnectorsDisabledException.class, () -> read.execute(OWNER));
    assertThrows(ConnectorsDisabledException.class, () -> disconnect.execute(OWNER));
    assertThrows(
        ConnectorsDisabledException.class, () -> readReceipt.execute(OWNER, SOURCE, receipt.id()));
    assertEquals(1, fakes.connections.size());
  }
}
