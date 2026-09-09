package com.apptolast.organization.application;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.domain.ExternalIssue;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @s12 crear tarea, evento y enlace por issue, @s14 título, @s16 paginación, @s17 repetir,
 * @s18 fallo aislado, @s19 confirmación conjunta, @s20 cuota, @s22 token caducado, @s23 proyecto,
 * @s24 proyecto terminado, @s26 recibo huérfano, @s28 caídas y @s29 orden de las precondiciones.
 */
class ImportGithubIssuesTest {
  private static final String OWNER = "owner-1";
  private static final Instant NOW = Instant.parse("2026-09-09T12:00:00Z");
  private static final String REPOSITORY = "octocat/Hello-World";

  private ConnectorFakes fakes;
  private UUID projectId;
  private ImportIssues importIssues;

  @BeforeEach
  void setUp() {
    fakes = new ConnectorFakes();
    projectId = fakes.projects.seed(OWNER, "idea");
    connect("valid");
    importIssues = newImport(NOW);
  }

  private ImportIssues newImport(Instant now) {
    return new ImportIssues(
        new GithubIssueConnections(fakes.connections),
        fakes.receipts,
        fakes.projects,
        fakes.source,
        fakes.tasks,
        fakes.cipher,
        fakes.audit,
        Clock.fixed(now, ZoneOffset.UTC));
  }

  private void connect(String status) {
    fakes.connections.save(
        OWNER,
        new StoredConnection(
            REPOSITORY,
            "octocat",
            status,
            fakes.cipher.encrypt(OWNER, "ghp_secreto123"),
            NOW.minusSeconds(60)));
  }

  private static ExternalIssue issue(String id) {
    return new ExternalIssue(
        id, "Issue " + id, null, "https://github.com/octocat/Hello-World/issues/" + id);
  }

  private void githubPage(int page, List<ExternalIssue> issues, int elements, boolean more) {
    fakes.source.page(page, new IssuePage(issues, elements, more));
  }

  private void githubIssues(ExternalIssue... issues) {
    githubPage(1, List.of(issues), issues.length, false);
  }

  private static List<ExternalIssue> issues(int from, int count) {
    return IntStream.range(from, from + count).mapToObj(n -> issue(String.valueOf(n))).toList();
  }

  // ----------------------------------------------------------------- @s12 camino feliz

  @Test
  void s12_eachOpenIssueBecomesOneTaskOneEventAndOneLink() {
    githubIssues(issue("101"), issue("102"), issue("103"));

    var receipt = importIssues.execute(OWNER, projectId);

    assertEquals("completed", receipt.status());
    assertEquals(3, receipt.created());
    assertEquals(0, receipt.skipped());
    assertEquals(0, receipt.failed());
    assertFalse(receipt.truncated());
    assertNull(receipt.errorCode());
    assertEquals(projectId, receipt.projectId());
    assertEquals(REPOSITORY, receipt.projectPath());
    assertEquals("github", receipt.source());
    assertFalse(receipt.finishedAt().isBefore(receipt.startedAt()));
    assertEquals(3, fakes.tasks.tasks());
    assertEquals(3, fakes.tasks.events());
    assertEquals(3, fakes.tasks.links());
  }

  @Test
  void s12_theCreatedTaskCarriesTheIssueTitleCriterionAndNoEstimate() {
    githubIssues(new ExternalIssue("101", "  Arreglar login  ", "línea1\r\nlínea2", "https://x/7"));

    importIssues.execute(OWNER, projectId);

    var task = fakes.tasks.lastTask();
    assertEquals("Arreglar login", task.title());
    assertEquals("https://x/7\n\nlínea1\nlínea2", task.completionCriterion());
    assertNull(task.estimatedMinutes());
    assertEquals("pending", task.status());
    assertEquals(projectId, task.projectId());
  }

  @Test
  void s12_theIssueIsAskedOfTheStoredRepositoryWithTheDecipheredToken() {
    githubIssues(issue("101"));

    importIssues.execute(OWNER, projectId);

    assertEquals(List.of("list " + REPOSITORY + " page=1"), fakes.source.calls());
    assertEquals("ghp_secreto123", fakes.source.lastToken());
  }

  // ------------------------------------------------------------------ @s16 paginación

  @ParameterizedTest
  @CsvSource({
    "0,1,0,false",
    "99,1,99,false",
    "100,2,100,false",
    "150,2,150,false",
    "200,2,200,false",
    "201,2,200,true"
  })
  void s16_atMostTwoPagesOfAHundredAndTruncatedReflectsWhatWasLeft(
      int open, int calls, int created, boolean truncated) {
    int first = Math.min(open, 100);
    int second = Math.min(Math.max(open - 100, 0), 100);
    githubPage(1, issues(1, first), first, open > 100);
    githubPage(2, issues(101, second), second, open > 200);

    var receipt = importIssues.execute(OWNER, projectId);

    assertEquals(calls, fakes.source.calls().size());
    assertEquals(
        IntStream.rangeClosed(1, calls).mapToObj(p -> "list " + REPOSITORY + " page=" + p).toList(),
        fakes.source.calls());
    assertEquals("completed", receipt.status());
    assertEquals(created, receipt.created());
    assertEquals(truncated, receipt.truncated());
    assertEquals(created, fakes.tasks.tasks());
  }

  @Test
  void s16_theSecondPageIsAskedOnlyBecauseTheFirstCameFullNotBecauseOfALinkHeader() {
    githubPage(1, issues(1, 100), 100, false);
    githubPage(2, issues(101, 5), 5, false);

    assertEquals(105, importIssues.execute(OWNER, projectId).created());
    assertEquals(2, fakes.source.calls().size());
  }

  @Test
  void s12_elementsThatAreNotIssuesCountForPagingButCreateNothing() {
    githubPage(1, issues(1, 3), 4, false);

    var receipt = importIssues.execute(OWNER, projectId);

    assertEquals(3, receipt.created());
    assertEquals(3, fakes.tasks.links());
    assertEquals(1, fakes.source.calls().size());
  }

  // ------------------------------------------------------------------- @s17 repetir

  @Test
  void s17_analreadyLinkedIssueIsSkippedWithoutCreatingTaskOrEvent() {
    IntStream.rangeClosed(1, 5).forEach(n -> fakes.tasks.seedLink(OWNER, String.valueOf(n)));
    githubPage(1, issues(1, 5), 5, false);

    var receipt = importIssues.execute(OWNER, projectId);

    assertEquals(0, receipt.created());
    assertEquals(5, receipt.skipped());
    assertEquals(0, receipt.failed());
    assertEquals(0, fakes.tasks.tasks());
    assertEquals(0, fakes.tasks.events());
    assertEquals(5, fakes.tasks.links());
  }

  @Test
  void s17_onlyTheNewIssueIsCreatedOnASecondImport() {
    IntStream.rangeClosed(1, 5).forEach(n -> fakes.tasks.seedLink(OWNER, String.valueOf(n)));
    githubPage(
        1,
        List.of(issue("1"), issue("2"), issue("3"), issue("4"), issue("5"), issue("999")),
        6,
        false);

    var receipt = importIssues.execute(OWNER, projectId);

    assertEquals(1, receipt.created());
    assertEquals(5, receipt.skipped());
    assertEquals(6, fakes.tasks.links());
  }

  @Test
  void s17_anIssueClosedInGithubSimplyStopsBeingConsidered() {
    IntStream.rangeClosed(1, 5).forEach(n -> fakes.tasks.seedLink(OWNER, String.valueOf(n)));
    githubPage(1, issues(1, 4), 4, false);

    var receipt = importIssues.execute(OWNER, projectId);

    assertEquals(0, receipt.created());
    assertEquals(4, receipt.skipped());
  }

  // ------------------------------------------------- @s18 un fallo no detiene a los demás

  @Test
  void s18_anIssueWithABlankTitleFailsAloneAndTheReceiptStillCompletes() {
    githubPage(
        1,
        List.of(
            issue("1"),
            issue("2"),
            new ExternalIssue("3", "   ", null, "https://x/3"),
            issue("4"),
            issue("5")),
        5,
        false);

    var receipt = importIssues.execute(OWNER, projectId);

    assertEquals("completed", receipt.status());
    assertEquals(4, receipt.created());
    assertEquals(1, receipt.failed());
    assertEquals(0, receipt.skipped());
    assertEquals(4, fakes.tasks.tasks());
    assertEquals(4, fakes.tasks.events());
    assertEquals(4, fakes.tasks.links());
    assertEquals(5, receipt.created() + receipt.skipped() + receipt.failed());
  }

  // -------------------------------------------- @s19 confirmar o revertir tarea, evento y enlace

  @Test
  void s19_aStorageFailureStopsTheImportAndLeavesOnlyWhatWasConfirmed() {
    githubPage(1, issues(1, 5), 5, false);
    fakes.tasks.failStorageOn("3");

    var error =
        assertThrows(
            IssueImportFailedException.class, () -> importIssues.execute(OWNER, projectId));

    var receipt = error.receipt();
    assertEquals("failed", receipt.status());
    assertEquals("STORAGE_UNAVAILABLE", receipt.errorCode());
    assertEquals(2, receipt.created());
    assertEquals(0, receipt.skipped());
    assertEquals(0, receipt.failed());
    assertEquals(2, fakes.tasks.tasks());
    assertEquals(2, fakes.tasks.events());
    assertEquals(2, fakes.tasks.links());
  }

  @Test
  void s19_aLaterImportWithoutTheFailureResumesWithoutDuplicating() {
    githubPage(1, issues(1, 5), 5, false);
    fakes.tasks.failStorageOn("3");
    assertThrows(IssueImportFailedException.class, () -> importIssues.execute(OWNER, projectId));
    fakes.tasks.failStorageOn(null);

    var receipt = newImport(NOW.plusSeconds(1)).execute(OWNER, projectId);

    assertEquals(3, receipt.created());
    assertEquals(2, receipt.skipped());
    assertEquals(5, fakes.tasks.links());
  }

  // --------------------------------------------------------------------- @s20 @s22 @s28 errores

  @Test
  void s20_anExhaustedQuotaFailsTheReceiptAndKeepsTheConnectionValid() {
    fakes.source.fail(IssueSourceException.rateLimited(120));

    var error =
        assertThrows(
            IssueImportFailedException.class, () -> importIssues.execute(OWNER, projectId));

    assertEquals("RATE_LIMITED", error.receipt().errorCode());
    assertEquals(120, error.retryAfterSeconds());
    assertEquals("failed", error.receipt().status());
    assertEquals("valid", fakes.connections.find(OWNER).orElseThrow().status());
  }

  @Test
  void s28_anUnreachableGithubFailsTheReceiptAndKeepsTheConnectionValid() {
    fakes.source.fail(IssueSourceException.unavailable());

    var error =
        assertThrows(
            IssueImportFailedException.class, () -> importIssues.execute(OWNER, projectId));

    assertEquals("GITHUB_UNAVAILABLE", error.receipt().errorCode());
    assertEquals(0, error.receipt().created());
    assertEquals("valid", fakes.connections.find(OWNER).orElseThrow().status());
  }

  @Test
  void s8_anUnavailableRepositoryFailsTheReceiptAndKeepsTheConnectionValid() {
    fakes.source.fail(IssueSourceException.repositoryUnavailable());

    var error =
        assertThrows(
            IssueImportFailedException.class, () -> importIssues.execute(OWNER, projectId));

    assertEquals("GITHUB_REPOSITORY_UNAVAILABLE", error.receipt().errorCode());
    assertEquals("valid", fakes.connections.find(OWNER).orElseThrow().status());
  }

  @Test
  void s22_aRejectedTokenHalfwayKeepsWhatWasCreatedAndTurnsTheConnectionInvalid() {
    githubPage(1, issues(1, 100), 100, true);
    fakes.source.failOnPage(2, IssueSourceException.tokenRejected());

    var error =
        assertThrows(
            IssueImportFailedException.class, () -> importIssues.execute(OWNER, projectId));

    assertEquals("CONNECTION_INVALID", error.receipt().errorCode());
    assertEquals(100, error.receipt().created());
    assertEquals("invalid", fakes.connections.find(OWNER).orElseThrow().status());
  }

  @Test
  void s22_anInvalidConnectionRefusesToImportWithoutAskingGithub() {
    connect("invalid");

    assertThrows(ConnectionInvalidException.class, () -> importIssues.execute(OWNER, projectId));
    assertEquals(List.of(), fakes.source.calls());
    assertEquals(0, fakes.receipts.size());
  }

  // ------------------------------------------------------- @s23 @s24 proyecto destino

  @Test
  void s23_anUnknownOrForeignProjectIsIndistinguishableAndCreatesNoReceipt() {
    var foreign = fakes.projects.seed("owner-2", "idea");

    assertThrows(ResourceNotFoundException.class, () -> importIssues.execute(OWNER, foreign));
    assertThrows(
        ResourceNotFoundException.class, () -> importIssues.execute(OWNER, UUID.randomUUID()));
    assertEquals(0, fakes.receipts.size());
    assertEquals(List.of(), fakes.source.calls());
  }

  @Test
  void s24_aProjectAlreadyCompletedIsRefusedBeforeAnyReceiptOrRequest() {
    fakes.projects.status(projectId, "completed");

    assertThrows(ProjectCompletedException.class, () -> importIssues.execute(OWNER, projectId));
    assertEquals(0, fakes.receipts.size());
    assertEquals(List.of(), fakes.source.calls());
  }

  @Test
  void s24_aProjectCompletedHalfwayStopsTheImportWithTheReceiptItHad() {
    githubPage(1, issues(1, 5), 5, false);
    fakes.tasks.completeProjectOn("3");

    var error =
        assertThrows(
            IssueImportFailedException.class, () -> importIssues.execute(OWNER, projectId));

    assertEquals("PROJECT_COMPLETED", error.receipt().errorCode());
    assertEquals("failed", error.receipt().status());
    assertEquals(2, error.receipt().created());
    assertEquals(2, fakes.tasks.tasks());
  }

  // ------------------------------------------------- @s25 @s26 una importación cada vez

  @Test
  void s25_aSecondImportWhileOneIsRunningIsRefusedWithoutAReceipt() {
    fakes.receipts.seedRunning(OWNER, NOW.minusSeconds(5));

    assertThrows(
        IssueImportInProgressException.class, () -> importIssues.execute(OWNER, projectId));
    assertEquals(1, fakes.receipts.size());
    assertEquals(List.of(), fakes.source.calls());
  }

  @Test
  void s26_aRunningReceiptJustUnderFifteenMinutesStillBlocks() {
    var stale = fakes.receipts.seedRunning(OWNER, NOW.minus(Duration.ofSeconds(14 * 60 + 59)));

    assertThrows(
        IssueImportInProgressException.class, () -> importIssues.execute(OWNER, projectId));
    assertEquals("running", fakes.receipts.current(stale.id()).status());
  }

  @Test
  void s26_aRunningReceiptPastFifteenMinutesIsInterruptedAndTheImportProceeds() {
    var stale = fakes.receipts.seedRunning(OWNER, NOW.minus(Duration.ofSeconds(15 * 60 + 1)));
    githubIssues(issue("101"));

    assertEquals("completed", importIssues.execute(OWNER, projectId).status());

    var interrupted = fakes.receipts.current(stale.id());
    assertEquals("failed", interrupted.status());
    assertEquals("INTERRUPTED", interrupted.errorCode());
    assertNotNull(interrupted.finishedAt());
    assertEquals(0, interrupted.created());
  }

  // ------------------------------------------------------ @s27 @s29 progreso y precondiciones

  @Test
  void s27_theReceiptRecordsProgressIssueByIssueSoACrashLeavesWhatWasConfirmed() {
    githubPage(1, issues(1, 5), 5, false);
    fakes.tasks.failStorageOn("3");

    assertThrows(IssueImportFailedException.class, () -> importIssues.execute(OWNER, projectId));

    assertEquals(List.of(1, 2), fakes.receipts.progressCreated());
  }

  @Test
  void s29_theConnectionIsCheckedBeforeTheProject() {
    fakes.connections.delete(OWNER);
    var foreign = fakes.projects.seed("owner-2", "idea");

    assertThrows(ConnectionNotFoundException.class, () -> importIssues.execute(OWNER, foreign));
  }

  @Test
  void s29_theConnectionStatusIsCheckedBeforeTheProjectState() {
    connect("invalid");
    fakes.projects.status(projectId, "completed");

    assertThrows(ConnectionInvalidException.class, () -> importIssues.execute(OWNER, projectId));
  }

  @Test
  void s29_theProjectIsCheckedBeforeTheRunningReceipt() {
    fakes.receipts.seedRunning(OWNER, NOW.minusSeconds(5));
    var foreign = fakes.projects.seed("owner-2", "idea");

    assertThrows(ResourceNotFoundException.class, () -> importIssues.execute(OWNER, foreign));
  }

  @Test
  void s29_theProjectStateIsCheckedBeforeTheRunningReceipt() {
    fakes.receipts.seedRunning(OWNER, NOW.minusSeconds(5));
    fakes.projects.status(projectId, "completed");

    assertThrows(ProjectCompletedException.class, () -> importIssues.execute(OWNER, projectId));
  }

  @Test
  void s34_theAuditRecordsTheCountersOfAFinishedImportAndNeverTheToken() {
    githubPage(1, issues(1, 3), 3, false);

    importIssues.execute(OWNER, projectId);

    assertEquals(List.of("finished github owner-1 octocat/Hello-World 3 0 0 false"), fakes.audit.lines());
    assertFalse(String.join(" ", fakes.audit.lines()).contains("ghp_secreto123"));
  }

  @Test
  void s34_theAuditRecordsWhatABrokenImportManagedToDoAndWhatGithubAnswered() {
    githubPage(1, issues(1, 100), 100, true);
    fakes.source.failOnPage(2, IssueSourceException.rateLimited(120));

    assertThrows(IssueImportFailedException.class, () -> importIssues.execute(OWNER, projectId));

    assertEquals(
        List.of("import-failed github owner-1 octocat/Hello-World RATE_LIMITED 100 429"),
        fakes.audit.lines());
  }

  @Test
  void s3_withoutTheConnectorKeyNothingIsReadOrWritten() {
    fakes.cipher.disable();

    assertThrows(ConnectorsDisabledException.class, () -> importIssues.execute(OWNER, projectId));
    assertEquals(0, fakes.receipts.size());
    assertEquals(List.of(), fakes.source.calls());
  }
}
