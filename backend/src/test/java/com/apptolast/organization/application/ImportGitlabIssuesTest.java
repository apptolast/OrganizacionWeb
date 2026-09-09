package com.apptolast.organization.application;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.domain.ExternalIssue;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @s15 el mismo caso de uso de importación de 27, servido esta vez por GitLab: el recibo declara su
 *     origen y los enlaces nacen con {@code source} gitlab.
 */
class ImportGitlabIssuesTest {
  private static final String OWNER = "owner-1";
  private static final Instant NOW = Instant.parse("2026-09-09T12:00:00Z");
  private static final String PROJECT_PATH = "grupo/proyecto";
  private static final String REFERENCE = "4821";
  private static final String TOKEN = "glpat-xxxxxxxxxxxxxxxxWXYZ";

  private ConnectorFakes fakes;
  private GitlabFakes gitlab;
  private UUID projectId;

  @BeforeEach
  void setUp() {
    fakes = new ConnectorFakes();
    gitlab = new GitlabFakes();
    projectId = fakes.projects.seed(OWNER, "idea");
    gitlab.connections.put(OWNER, connected());
  }

  private GitlabConnection connected() {
    return new GitlabConnection(
        PROJECT_PATH,
        Long.parseLong(REFERENCE),
        "WXYZ",
        GitlabConnection.CONNECTED,
        gitlab.cipher.encrypt(OWNER, TOKEN),
        Instant.parse("2026-09-09T10:00:00Z"),
        null,
        null,
        1L);
  }

  private ImportIssuesUseCase importIssues() {
    return new ImportIssues(
        new GitlabIssueConnections(gitlab.connections),
        fakes.receipts,
        fakes.projects,
        fakes.source,
        fakes.tasks,
        gitlab.cipher,
        fakes.audit,
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private static ExternalIssue issue(long id) {
    return new ExternalIssue(
        "gitlab.example.com:" + id,
        "Issue " + id,
        null,
        "https://gitlab.example.com/grupo/proyecto/-/issues/" + id);
  }

  @Test
  void s15_importingOpenIssuesCreatesLinkedTasksAndAReceiptThatNamesItsSource() {
    fakes.source.page(1, new IssuePage(List.of(issue(9001), issue(9002)), 2, false));

    var receipt = importIssues().execute(OWNER, projectId);

    assertEquals("gitlab", receipt.source());
    assertEquals(PROJECT_PATH, receipt.projectPath());
    assertEquals(projectId, receipt.projectId());
    assertEquals("completed", receipt.status());
    assertEquals(2, receipt.created());
    assertEquals(0, receipt.skipped());
    assertEquals(0, receipt.failed());
    assertFalse(receipt.truncated());
    assertNull(receipt.errorCode());
    assertEquals(NOW, receipt.finishedAt());
    assertEquals(2, fakes.tasks.tasks());
    assertEquals(2, fakes.tasks.events());
    assertEquals(
        List.of("gitlab|gitlab.example.com:9001", "gitlab|gitlab.example.com:9002"),
        fakes.tasks.linkKeys());
  }

  // ------------------------------------------------------ @s23 @s24 @s25 fallos del gestor

  @Test
  void s23_aRejectedTokenMarksTheConnectionInErrorAndLeavesAFailedReceipt() {
    fakes.source.failOnPage(1, IssueSourceException.tokenRejected());

    var error =
        assertThrows(
            IssueImportFailedException.class, () -> importIssues().execute(OWNER, projectId));

    var receipt = error.receipt();
    assertEquals("failed", receipt.status());
    assertEquals("CONNECTION_INVALID", receipt.errorCode());
    assertEquals(0, receipt.created());
    assertNotNull(receipt.finishedAt());

    var row = gitlab.connections.find(OWNER).orElseThrow();
    assertEquals("error", row.status());
    assertEquals("CONNECTION_INVALID", row.lastError().code());
    assertEquals(NOW, row.lastError().at());
    assertNotNull(row.tokenCiphertext());
  }

  @Test
  void s25_anUnavailableProviderAnnotatesTheFailureButLeavesTheConnectionConnected() {
    fakes.source.failOnPage(1, IssueSourceException.unavailable());

    var error =
        assertThrows(
            IssueImportFailedException.class, () -> importIssues().execute(OWNER, projectId));

    assertEquals("GITLAB_UNAVAILABLE", error.receipt().errorCode());
    var row = gitlab.connections.find(OWNER).orElseThrow();
    assertEquals("connected", row.status());
    assertEquals("GITLAB_UNAVAILABLE", row.lastError().code());
    assertEquals(0, fakes.tasks.tasks());
  }

  @Test
  void s24_anExhaustedQuotaKeepsTheConnectionAndFailsTheReceiptWithItsRetryAfter() {
    fakes.source.failOnPage(1, IssueSourceException.rateLimited(30));

    var error =
        assertThrows(
            IssueImportFailedException.class, () -> importIssues().execute(OWNER, projectId));

    assertEquals("RATE_LIMITED", error.receipt().errorCode());
    assertEquals(30, error.retryAfterSeconds());
    assertEquals("connected", gitlab.connections.find(OWNER).orElseThrow().status());
    assertEquals(0, fakes.tasks.tasks());
  }

  @Test
  void s15_theIssuesAreAskedForByProjectReferenceWithTheDecryptedToken() {
    fakes.source.page(1, new IssuePage(List.of(issue(9001)), 1, false));

    importIssues().execute(OWNER, projectId);

    assertEquals(List.of("list " + REFERENCE + " page=1"), fakes.source.calls());
    assertEquals(TOKEN, fakes.source.lastToken());
  }
}
