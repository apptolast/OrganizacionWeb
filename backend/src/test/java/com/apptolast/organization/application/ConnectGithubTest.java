package com.apptolast.organization.application;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.domain.ValidationException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @s1 conectar guarda el token cifrado, @s5 y @s6 validan antes de salir a la red, @s7 token
 * rechazado, @s8 repositorio no disponible, @s9 reconectar y @s21 cuota agotada al conectar.
 */
class ConnectGithubTest {
  private static final String OWNER = "owner-1";
  private static final String TOKEN = "ghp_secreto123";
  private static final Instant NOW = Instant.parse("2026-09-09T10:00:00Z");

  private ConnectorFakes fakes;
  private ConnectGithub connect;

  @BeforeEach
  void setUp() {
    fakes = new ConnectorFakes();
    connect = newConnect(NOW);
  }

  private ConnectGithub newConnect(Instant now) {
    return new ConnectGithub(
        fakes.connections,
        fakes.receipts,
        fakes.source,
        fakes.cipher,
        fakes.audit,
        Clock.fixed(now, ZoneOffset.UTC));
  }

  @Test
  void s1_storesTheCipheredTokenAndAnswersWithTheClosedView() {
    fakes.source.identify("octocat/Hello-World", "octocat");

    var view = connect.execute(OWNER, " OCTOCAT/hello-world ", TOKEN);

    assertEquals("octocat/Hello-World", view.repository());
    assertEquals("octocat", view.login());
    assertEquals("valid", view.status());
    assertEquals(NOW, view.connectedAt());
    assertNull(view.lastImport());
  }

  @Test
  void s1_theStoredRowKeepsTheTokenOnlyAsCiphertextBoundToTheOwner() {
    fakes.source.identify("octocat/Hello-World", "octocat");

    connect.execute(OWNER, "octocat/Hello-World", TOKEN);

    var stored = fakes.connections.find(OWNER).orElseThrow();
    assertEquals(-1, indexOfPlaintext(stored.tokenCiphertext()));
    assertEquals(TOKEN, fakes.cipher.decrypt(OWNER, stored.tokenCiphertext()));
    assertThrows(
        SecretUndecipherableException.class,
        () -> fakes.cipher.decrypt("otro-owner", stored.tokenCiphertext()));
  }

  @Test
  void s1_theRepositoryIsVerifiedWithTheTokenTheCallerSent() {
    fakes.source.identify("octocat/Hello-World", "octocat");

    connect.execute(OWNER, "octocat/Hello-World", TOKEN);

    assertEquals(java.util.List.of("verify octocat/Hello-World " + TOKEN), fakes.source.calls());
  }

  @Test
  void s5_anInvalidRepositoryIsRejectedBeforeTouchingGithub() {
    var error = assertThrows(ValidationException.class, () -> connect.execute(OWNER, "octocat", TOKEN));

    assertEquals("repository", error.errors().getFirst().field());
    assertEquals("INVALID_FORMAT", error.errors().getFirst().code());
    assertEquals(java.util.List.of(), fakes.source.calls());
    assertTrue(fakes.connections.find(OWNER).isEmpty());
  }

  @Test
  void s6_anInvalidTokenIsRejectedBeforeTouchingGithub() {
    var error =
        assertThrows(
            ValidationException.class, () -> connect.execute(OWNER, "octocat/Hello-World", ""));

    assertEquals("token", error.errors().getFirst().field());
    assertEquals("REQUIRED", error.errors().getFirst().code());
    assertEquals(java.util.List.of(), fakes.source.calls());
    assertTrue(fakes.connections.find(OWNER).isEmpty());
  }

  @Test
  void s7_aRejectedTokenLeavesNothingStored() {
    fakes.source.fail(IssueSourceException.tokenRejected());

    assertThrows(
        GithubTokenRejectedException.class,
        () -> connect.execute(OWNER, "octocat/Hello-World", "ghp_malo"));
    assertTrue(fakes.connections.find(OWNER).isEmpty());
  }

  @Test
  void s8_anUnavailableRepositoryLeavesNothingStored() {
    fakes.source.fail(IssueSourceException.repositoryUnavailable());

    assertThrows(
        GithubRepositoryUnavailableException.class,
        () -> connect.execute(OWNER, "octocat/Hello-World", TOKEN));
    assertTrue(fakes.connections.find(OWNER).isEmpty());
  }

  @Test
  void s8_anUnavailableRepositoryLeavesAPreviousConnectionByteForByteEqual() {
    fakes.source.identify("octocat/Otro", "octocat");
    connect.execute(OWNER, "octocat/Otro", TOKEN);
    var before = fakes.connections.find(OWNER).orElseThrow();
    fakes.source.fail(IssueSourceException.repositoryUnavailable());

    assertThrows(
        GithubRepositoryUnavailableException.class,
        () -> connect.execute(OWNER, "octocat/Hello-World", TOKEN));

    var after = fakes.connections.find(OWNER).orElseThrow();
    assertEquals("octocat/Otro", after.repository());
    assertArrayEquals(before.tokenCiphertext(), after.tokenCiphertext());
    assertEquals(before.connectedAt(), after.connectedAt());
  }

  @Test
  void s21_anExhaustedQuotaLeavesNothingStoredAndKeepsTheDelay() {
    fakes.source.fail(IssueSourceException.rateLimited(45));

    var error =
        assertThrows(
            ConnectorRateLimitedException.class,
            () -> connect.execute(OWNER, "octocat/Hello-World", TOKEN));

    assertEquals(45, error.retryAfterSeconds());
    assertTrue(fakes.connections.find(OWNER).isEmpty());
  }

  @Test
  void s28_anUnreachableGithubLeavesNothingStored() {
    fakes.source.fail(IssueSourceException.unavailable());

    assertThrows(
        GithubUnavailableException.class,
        () -> connect.execute(OWNER, "octocat/Hello-World", TOKEN));
    assertTrue(fakes.connections.find(OWNER).isEmpty());
  }

  @Test
  void s9_reconnectingReplacesRepositoryTokenAndInstantAndKeepsTheLastReceipt() {
    fakes.source.identify("octocat/Hello-World", "octocat");
    connect.execute(OWNER, "octocat/Hello-World", TOKEN);
    var first = fakes.connections.find(OWNER).orElseThrow().tokenCiphertext();
    var receipt = fakes.receipts.seedCompleted(OWNER);
    var later = NOW.plusSeconds(3600);
    fakes.source.identify("octocat/Segundo", "octocat");

    var view = newConnect(later).execute(OWNER, "octocat/Segundo", "ghp_otro456");

    assertEquals("octocat/Segundo", view.repository());
    assertEquals("valid", view.status());
    assertEquals(later, view.connectedAt());
    assertEquals(receipt, view.lastImport());
    assertEquals(1, fakes.connections.size());
    assertFalse(
        java.util.Arrays.equals(first, fakes.connections.find(OWNER).orElseThrow().tokenCiphertext()));
  }

  @Test
  void s22_reconnectingAfterAnInvalidConnectionRestoresTheValidStatus() {
    fakes.source.identify("octocat/Hello-World", "octocat");
    connect.execute(OWNER, "octocat/Hello-World", TOKEN);
    fakes.connections.invalidate(OWNER);

    assertEquals("valid", connect.execute(OWNER, "octocat/Hello-World", TOKEN).status());
    assertEquals("valid", fakes.connections.find(OWNER).orElseThrow().status());
  }

  @Test
  void s3_withoutTheConnectorKeyNothingIsReadOrWritten() {
    fakes.cipher.disable();

    assertThrows(
        ConnectorsDisabledException.class,
        () -> connect.execute(OWNER, "octocat/Hello-World", TOKEN));
    assertEquals(java.util.List.of(), fakes.source.calls());
    assertTrue(fakes.connections.find(OWNER).isEmpty());
  }

  @Test
  void s34_theAuditRecordsOwnerRepositoryAndLoginButNeverTheToken() {
    fakes.source.identify("octocat/Hello-World", "octocat");

    connect.execute(OWNER, "octocat/Hello-World", TOKEN);

    assertEquals(
        java.util.List.of("connected owner-1 octocat/Hello-World octocat"), fakes.audit.lines());
    assertFalse(String.join(" ", fakes.audit.lines()).contains(TOKEN));
  }

  @Test
  void s34_theAuditRecordsWhyAConnectionWasRefusedAndWhatGithubAnswered() {
    fakes.source.fail(IssueSourceException.tokenRejected());

    assertThrows(
        GithubTokenRejectedException.class,
        () -> connect.execute(OWNER, "octocat/Hello-World", "ghp_malo"));

    assertEquals(
        java.util.List.of("refused owner-1 octocat/Hello-World GITHUB_TOKEN_REJECTED 401"),
        fakes.audit.lines());
    assertFalse(String.join(" ", fakes.audit.lines()).contains("ghp_malo"));
  }

  @Test
  void s34_nothingIsAuditedWhenTheRequestNeverReachedGithub() {
    assertThrows(ValidationException.class, () -> connect.execute(OWNER, "octocat", TOKEN));

    assertEquals(java.util.List.of(), fakes.audit.lines());
  }

  private static int indexOfPlaintext(byte[] ciphertext) {
    var plain = TOKEN.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    outer:
    for (int i = 0; i + plain.length <= ciphertext.length; i++) {
      for (int j = 0; j < plain.length; j++) if (ciphertext[i + j] != plain[j]) continue outer;
      return i;
    }
    return -1;
  }
}
