package com.apptolast.organization.application;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @s8 leer la conexión de GitLab, con conexión y sin ella, y @s9 conectarla: el token se cifra
 *     antes de guardarse y sólo su pista de cuatro caracteres vuelve a salir.
 */
class GitlabConnectionUseCasesTest {
  private static final String OWNER = "owner-1";
  private static final String API_BASE = "https://gitlab.example.com/api/v4";
  private static final Instant CONNECTED_AT = Instant.parse("2026-09-09T10:00:00Z");
  private static final Instant FAILED_AT = Instant.parse("2026-09-09T11:30:00Z");
  private static final Instant NOW = Instant.parse("2026-09-09T12:00:00Z");
  private static final String TOKEN = "glpat-xxxxxxxxxxxxxxxxWXYZ";

  private GitlabFakes fakes;
  private ConnectorFakes.FakeReceipts receipts;

  @BeforeEach
  void setUp() {
    fakes = new GitlabFakes();
    receipts = new ConnectorFakes.FakeReceipts();
  }

  private ReadGitlabConnectionUseCase read() {
    return new ReadGitlabConnection(fakes.connections, receipts, API_BASE, fakes.cipher);
  }

  @Test
  void s8_readingWithoutConnectionAnswersNotConnectedAndNothingElse() {
    var view = read().execute(OWNER);

    assertEquals("not_connected", view.status());
    assertNull(view.apiBase());
    assertNull(view.projectPath());
    assertNull(view.projectId());
    assertNull(view.tokenHint());
    assertNull(view.lastActivityAt());
    assertNull(view.lastError());
    assertNull(view.version());
  }

  @Test
  void s8_readingAConnectedRowAnswersItsFieldsAndTheApiBaseOfTheServer() {
    fakes.connections.put(OWNER, connected());

    var view = read().execute(OWNER);

    assertEquals("connected", view.status());
    assertEquals(API_BASE, view.apiBase());
    assertEquals("grupo/proyecto", view.projectPath());
    assertEquals(4821L, view.projectId());
    assertEquals("WXYZ", view.tokenHint());
    assertEquals(CONNECTED_AT, view.lastActivityAt());
    assertNull(view.lastError());
    assertEquals(1L, view.version());
  }

  @Test
  void s8_readingARowInErrorAnswersItsLastErrorAsCodeAndInstant() {
    fakes.connections.put(OWNER, connected().withError("CONNECTION_INVALID", FAILED_AT));

    var view = read().execute(OWNER);

    assertEquals("error", view.status());
    assertEquals("CONNECTION_INVALID", view.lastError().code());
    assertEquals(FAILED_AT, view.lastError().at());
  }

  @Test
  void s15_theLastActivityIsTheEndOfTheLastImportAndNotTheInstantOfTheConnection() {
    fakes.connections.put(OWNER, connected());
    var receipt = receipts.seedCompleted(OWNER, "gitlab");

    assertEquals(receipt.finishedAt(), read().execute(OWNER).lastActivityAt());
  }

  @Test
  void s15_animportOfTheOtherSourceDoesNotCountAsGitlabActivity() {
    fakes.connections.put(OWNER, connected());
    receipts.seedCompleted(OWNER, "github");

    assertEquals(CONNECTED_AT, read().execute(OWNER).lastActivityAt());
  }

  @Test
  void s15_arunningImportCountsFromWhenItStartedBecauseItHasNotEndedYet() {
    fakes.connections.put(OWNER, connected());
    var running = receipts.seedRunning(OWNER, "gitlab", NOW.minusSeconds(30));

    assertEquals(running.startedAt(), read().execute(OWNER).lastActivityAt());
  }

  // ------------------------------------------------------------------------- @s9 conectar

  @Test
  void s9_connectingVerifiesTheProjectAndStoresTheTokenEncryptedAndNothingElse() {
    fakes.projects.accept("grupo/proyecto", 4821L);

    var view = connect().execute(OWNER, TOKEN, "grupo/proyecto");

    assertEquals("grupo/proyecto", fakes.projects.verifiedPath());
    assertEquals(TOKEN, fakes.projects.verifiedToken());
    assertEquals("connected", view.status());
    assertEquals(API_BASE, view.apiBase());
    assertEquals("grupo/proyecto", view.projectPath());
    assertEquals(4821L, view.projectId());
    assertEquals("WXYZ", view.tokenHint());
    assertEquals(NOW, view.lastActivityAt());
    assertNull(view.lastError());
    assertEquals(1L, view.version());

    var row = fakes.connections.find(OWNER).orElseThrow();
    assertEquals(TOKEN, fakes.cipher.decrypt(OWNER, row.tokenCiphertext()).orElseThrow());
    assertFalse(row.toString().contains(TOKEN));
  }

  @Test
  void s9_theStoredProjectPathIsTheCanonicalOneAnsweredByGitlab() {
    fakes.projects.accept("Grupo/Proyecto", 77L);

    var view = connect().execute(OWNER, TOKEN, "grupo/proyecto");

    assertEquals("Grupo/Proyecto", view.projectPath());
  }

  @Test
  void s13_replacingTheTokenRaisesTheVersionAndClearsTheLastError() {
    fakes.connections.put(OWNER, connected().withError("CONNECTION_INVALID", FAILED_AT));
    fakes.projects.accept("grupo/proyecto", 4821L);

    var view = connect().execute(OWNER, "glpat-otro-secreto-largo9Q2p", "grupo/proyecto");

    assertEquals("9Q2p", view.tokenHint());
    assertEquals("connected", view.status());
    assertNull(view.lastError());
    assertEquals(2L, view.version());
    assertEquals(
        "glpat-otro-secreto-largo9Q2p",
        fakes
            .cipher
            .decrypt(OWNER, fakes.connections.find(OWNER).orElseThrow().tokenCiphertext())
            .orElseThrow());
  }

  // ------------------------------------------------------------------------ @s14 desconectar

  @Test
  void s14_disconnectingTwiceInARowLeavesNoRowAndIsNotAnError() {
    fakes.connections.put(OWNER, connected());

    disconnect().execute(OWNER);
    disconnect().execute(OWNER);

    assertTrue(fakes.connections.find(OWNER).isEmpty());
  }

  private DisconnectGitlabUseCase disconnect() {
    return new DisconnectGitlab(
        fakes.connections, receipts, fakes.cipher, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  void s12_aRejectedTokenLeavesThePreviousConnectionExactlyAsItWas() {
    var previous = connected();
    fakes.connections.put(OWNER, previous);
    fakes.projects.reject(IssueSourceException.tokenRejected());

    assertThrows(
        ConnectionInvalidException.class, () -> connect().execute(OWNER, "glpat-otro9Q2p", "g/p"));

    assertEquals(previous, fakes.connections.find(OWNER).orElseThrow());
  }

  @Test
  void s12_anUnknownProjectIsAlsoAnInvalidConnectionAndNothingIsStored() {
    fakes.projects.reject(IssueSourceException.repositoryUnavailable());

    assertThrows(
        ConnectionInvalidException.class, () -> connect().execute(OWNER, TOKEN, "grupo/proyecto"));

    assertTrue(fakes.connections.find(OWNER).isEmpty());
  }

  @Test
  void s12_anExhaustedQuotaTravelsWithItsRetryAfterAndStoresNothing() {
    fakes.projects.reject(IssueSourceException.rateLimited(20));

    var error =
        assertThrows(
            ConnectorRateLimitedException.class,
            () -> connect().execute(OWNER, TOKEN, "grupo/proyecto"));

    assertEquals(20, error.retryAfterSeconds());
    assertTrue(fakes.connections.find(OWNER).isEmpty());
  }

  @Test
  void s12_aProviderThatDoesNotAnswerIsUnavailableAndStoresNothing() {
    fakes.projects.reject(IssueSourceException.unavailable());

    assertThrows(
        GitlabUnavailableException.class, () -> connect().execute(OWNER, TOKEN, "grupo/proyecto"));

    assertTrue(fakes.connections.find(OWNER).isEmpty());
  }

  // -------------------------------------------- @s27 @s28 una sola importación por persona

  @Test
  void s27_aRunningImportOfEitherSourceBlocksConnectingAndDisconnectingGitlab() {
    fakes.connections.put(OWNER, connected());
    fakes.projects.accept("grupo/proyecto", 4821L);
    receipts.seedRunning(OWNER, "github", NOW.minusSeconds(120));

    assertThrows(
        IssueImportInProgressException.class,
        () -> connect().execute(OWNER, TOKEN, "grupo/proyecto"));
    assertThrows(IssueImportInProgressException.class, () -> disconnect().execute(OWNER));

    assertNull(fakes.projects.verifiedPath());
    assertTrue(fakes.connections.find(OWNER).isPresent());
  }

  @Test
  void s28_anAbandonedRunningImportStopsBlockingAfterFifteenMinutes() {
    fakes.connections.put(OWNER, connected());
    fakes.projects.accept("grupo/proyecto", 4821L);
    receipts.seedRunning(OWNER, "gitlab", NOW.minusSeconds(16 * 60));

    assertEquals("connected", connect().execute(OWNER, TOKEN, "grupo/proyecto").status());
  }

  @Test
  void s28_aRunningImportOfFourteenMinutesStillBlocks() {
    fakes.connections.put(OWNER, connected());
    receipts.seedRunning(OWNER, "gitlab", NOW.minusSeconds(14 * 60));

    assertThrows(IssueImportInProgressException.class, () -> disconnect().execute(OWNER));
  }

  // ------------------------------------------------------- @s29 sin clave de conectores

  @Test
  void s29_withoutTheConnectorKeyEveryGitlabUseCaseRefusesWithoutTouchingAnything() {
    var stored = connected();
    fakes.connections.put(OWNER, stored);
    fakes.projects.accept("grupo/proyecto", 4821L);
    fakes.cipher.disable();

    assertThrows(ConnectorsDisabledException.class, () -> read().execute(OWNER));
    assertThrows(
        ConnectorsDisabledException.class, () -> connect().execute(OWNER, TOKEN, "grupo/proyecto"));
    assertThrows(ConnectorsDisabledException.class, () -> disconnect().execute(OWNER));

    assertEquals(stored, fakes.connections.find(OWNER).orElseThrow());
    assertNull(fakes.projects.verifiedPath());
  }

  private ConnectGitlabUseCase connect() {
    return new ConnectGitlab(
        fakes.connections,
        fakes.projects,
        receipts,
        API_BASE,
        fakes.cipher,
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private static GitlabConnection connected() {
    return new GitlabConnection(
        "grupo/proyecto",
        4821L,
        "WXYZ",
        GitlabConnection.CONNECTED,
        "cifrado".getBytes(StandardCharsets.UTF_8),
        CONNECTED_AT,
        null,
        null,
        1L);
  }
}
