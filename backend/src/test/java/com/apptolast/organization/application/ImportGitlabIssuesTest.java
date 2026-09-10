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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
    // El nombre del evento es un contrato entre features: las automatizaciones de la 30 se
    // suscriben a este literal exacto, y cambiarlo dejaría de dispararlas en silencio.
    assertEquals("TaskCreated.v1", fakes.tasks.lastEvent().type());
    assertEquals(1, fakes.tasks.lastEvent().schemaVersion());
    assertEquals(
        List.of("gitlab|gitlab.example.com:9001", "gitlab|gitlab.example.com:9002"),
        fakes.tasks.linkKeys());
  }

  @Test
  void s17_whatTheAdapterExcludedCountsAsSkippedAndLeavesNeitherTaskNorLink() {
    fakes.source.page(1, new IssuePage(List.of(issue(9001), issue(9002)), 3, 1, false));

    var receipt = importIssues().execute(OWNER, projectId);

    assertEquals(2, receipt.created());
    assertEquals(1, receipt.skipped());
    assertEquals(0, receipt.failed());
    assertEquals(2, fakes.tasks.links());
  }

  // ---------------------------------------------- @s18 @s19 @s20 idempotencia y simetría

  @Test
  void s18_repeatingTheImportOnlyCreatesTheIssuesThatWereNotLinkedYet() {
    fakes.source.page(
        1,
        new IssuePage(
            List.of(issue(9001), issue(9002), issue(9003), issue(9004), issue(9005)), 5, false));
    importIssues().execute(OWNER, projectId);
    fakes.source.page(
        1,
        new IssuePage(
            List.of(issue(9001), issue(9002), issue(9003), issue(9004), issue(9005), issue(9006)),
            6,
            false));

    var receipt = importIssues().execute(OWNER, projectId);

    assertEquals(1, receipt.created());
    assertEquals(5, receipt.skipped());
    assertEquals(0, receipt.failed());
    assertEquals(6, fakes.tasks.links());
  }

  @Test
  void s19_theSameExternalIdOnGithubDoesNotMakeTheGitlabImportSkipIt() {
    fakes.tasks.seedLink(OWNER, "github", "gitlab.example.com:9001");
    fakes.source.page(1, new IssuePage(List.of(issue(9001)), 1, false));

    var receipt = importIssues().execute(OWNER, projectId);

    assertEquals(1, receipt.created());
    assertEquals(0, receipt.skipped());
    assertEquals(
        List.of("github|gitlab.example.com:9001", "gitlab|gitlab.example.com:9001"),
        fakes.tasks.linkKeys());
  }

  @Test
  void s20_theSameIssueImportedFromEitherSourceProducesTheSameTaskAndReceiptKeys() {
    var githubProject = fakes.projects.seed(OWNER, "idea");
    fakes.connections.save(
        OWNER,
        new StoredConnection(
            "octocat/Hello-World",
            "octocat",
            StoredConnection.VALID,
            gitlab.cipher.encrypt(OWNER, "ghp_secreto"),
            NOW));
    fakes.source.page(
        1,
        new IssuePage(
            List.of(
                new ExternalIssue(
                    "101", "Revisar despliegue", "una\ndos\ntres", "https://github.test/i/101")),
            1,
            false));
    var githubReceipt =
        new ImportIssues(
                new GithubIssueConnections(fakes.connections),
                fakes.receipts,
                fakes.projects,
                fakes.source,
                fakes.tasks,
                gitlab.cipher,
                fakes.audit,
                Clock.fixed(NOW, ZoneOffset.UTC))
            .execute(OWNER, githubProject);
    var githubTask = fakes.tasks.lastTask();

    fakes.source.page(
        1,
        new IssuePage(
            List.of(
                new ExternalIssue(
                    "gitlab.example.com:9001",
                    "Revisar despliegue",
                    "una\ndos\ntres",
                    "https://gitlab.example.com/i/9001")),
            1,
            false));
    var gitlabReceipt = importIssues().execute(OWNER, projectId);
    var gitlabTask = fakes.tasks.lastTask();

    assertEquals(githubTask.title(), gitlabTask.title());
    assertEquals(githubTask.status(), gitlabTask.status());
    assertNull(gitlabTask.estimatedMinutes());
    assertEquals(
        githubTask.completionCriterion().replace("https://github.test/i/101", ""),
        gitlabTask.completionCriterion().replace("https://gitlab.example.com/i/9001", ""));
    assertEquals("github", githubReceipt.source());
    assertEquals("gitlab", gitlabReceipt.source());
    assertEquals(githubReceipt.created(), gitlabReceipt.created());
    assertEquals(githubReceipt.status(), gitlabReceipt.status());
    assertEquals(List.of("github|101", "gitlab|gitlab.example.com:9001"), fakes.tasks.linkKeys());
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

  // ------------------ @s27 la clave rotada cierra el recibo en vez de dejarlo colgado

  /**
   * @s27: rotar APP_CONNECTOR_KEY sin conservar la anterior deja un texto cifrado que ninguna clave
   *     abre. El recibo ya está insertado como {@code running} cuando eso se descubre, y si la
   *     excepción escapa sin cerrarlo el guardián responde IMPORT_IN_PROGRESS a toda escritura del
   *     propietario durante quince minutos por un fallo de configuración del servidor.
   */
  @Test
  void s27_atokenNoKeyCanOpenClosesTheReceiptInsteadOfLeavingItRunning() {
    gitlab.connections.put(OWNER, connectedWithForeignCiphertext());
    fakes.source.page(1, new IssuePage(List.of(issue(9001)), 1, false));

    assertThrows(
        SecretUndecipherableException.class, () -> importIssues().execute(OWNER, projectId));

    var receipt = fakes.receipts.latest(OWNER, "gitlab").orElseThrow();
    assertEquals("failed", receipt.status());
    assertEquals("CONNECTOR_KEY_MISMATCH", receipt.errorCode());
    assertEquals(NOW, receipt.finishedAt());
    assertFalse(fakes.receipts.importing(OWNER, NOW.minusSeconds(15 * 60)));
    assertEquals(List.of(), fakes.source.calls());
  }

  /** El mismo cifrado, sellado para otro propietario: descifrarlo con OWNER no devuelve nada. */
  private GitlabConnection connectedWithForeignCiphertext() {
    return new GitlabConnection(
        PROJECT_PATH,
        Long.parseLong(REFERENCE),
        "WXYZ",
        GitlabConnection.CONNECTED,
        gitlab.cipher.encrypt("owner-ajeno", TOKEN),
        Instant.parse("2026-09-09T10:00:00Z"),
        null,
        null,
        1L);
  }

  // ------------------------------------------- @s26 un fallo en la página 2 no borra la página 1

  // ------------------------------------------------- @s21 el mapeo issue → tarea es el de 27

  private static ExternalIssue issue(long id, String title, String body) {
    return new ExternalIssue(
        "gitlab.example.com:" + id,
        title,
        body,
        "https://gitlab.example.com/grupo/proyecto/-/issues/" + id);
  }

  @Test
  void s21_agitlabIssueLosesItsOuterUnicodeSpacesAndKeepsItsWebUrlAsCriterion() {
    var bare = issue(9001, "   Preparar demo  ", null);
    fakes.source.page(1, new IssuePage(List.of(bare), 1, false));

    importIssues().execute(OWNER, projectId);

    var task = fakes.tasks.lastTask();
    assertEquals("Preparar demo", task.title());
    assertEquals(bare.url(), task.completionCriterion());
  }

  @Test
  void s21_thebodyFollowsTheWebUrlAfterOneBlankLine() {
    var described = issue(9002, "Revisar despliegue", "una\ndos\ntres");
    fakes.source.page(1, new IssuePage(List.of(described), 1, false));

    importIssues().execute(OWNER, projectId);

    assertEquals(
        described.url() + "\n\nuna\ndos\ntres", fakes.tasks.lastTask().completionCriterion());
  }

  @Test
  void s21_agitlabIssueWithABlankTitleFailsAloneAndTheReceiptStillCompletes() {
    fakes.source.page(
        1, new IssuePage(List.of(issue(9001), issue(9002, "   ", "x"), issue(9003)), 3, false));

    var receipt = importIssues().execute(OWNER, projectId);

    assertEquals("completed", receipt.status());
    assertEquals(2, receipt.created());
    assertEquals(0, receipt.skipped());
    assertEquals(1, receipt.failed());
    assertEquals(
        List.of("gitlab|gitlab.example.com:9001", "gitlab|gitlab.example.com:9003"),
        fakes.tasks.linkKeys());
  }

  // ------------------- @s22 las precondiciones se aplican antes de contactar con GitLab

  /** Nada de lo que se comprueba antes de tiempo puede haber tocado al proveedor ni al recibo. */
  private void assertNothingHappened() {
    assertEquals(List.of(), fakes.source.calls());
    assertEquals(0, fakes.receipts.size());
    assertEquals(0, fakes.tasks.tasks());
    assertEquals(0, fakes.tasks.links());
  }

  @Test
  void s22_anUnknownOrForeignProjectIsRefusedWithoutAskingGitlabAnything() {
    var foreign = fakes.projects.seed("owner-2", "idea");

    assertThrows(ResourceNotFoundException.class, () -> importIssues().execute(OWNER, foreign));
    assertThrows(
        ResourceNotFoundException.class, () -> importIssues().execute(OWNER, UUID.randomUUID()));
    assertNothingHappened();
  }

  @Test
  void s22_acompletedProjectIsRefusedBeforeAnyReceiptOrRequest() {
    fakes.projects.status(projectId, "completed");

    assertThrows(ProjectCompletedException.class, () -> importIssues().execute(OWNER, projectId));
    assertNothingHappened();
  }

  @Test
  void s22_withoutAGitlabConnectionThereIsNothingToImportFrom() {
    gitlab.connections.delete(OWNER);

    assertThrows(ConnectionNotFoundException.class, () -> importIssues().execute(OWNER, projectId));
    assertNothingHappened();
  }

  @Test
  void s22_aconnectionInErrorRefusesToImportWithoutAskingGitlab() {
    gitlab.connections.put(OWNER, connected().withError("CONNECTION_INVALID", NOW));

    assertThrows(ConnectionInvalidException.class, () -> importIssues().execute(OWNER, projectId));
    assertNothingHappened();
  }

  /**
   * El orden importa: si el proyecto se mirase antes que la conexión, un propietario sin conectar
   * sabría por el código de error qué proyectos existen y cuáles no.
   */
  @Test
  void s22_theConnectionIsCheckedBeforeTheProject() {
    gitlab.connections.delete(OWNER);
    var foreign = fakes.projects.seed("owner-2", "idea");

    assertThrows(ConnectionNotFoundException.class, () -> importIssues().execute(OWNER, foreign));
  }

  @Test
  void s22_theConnectionStatusIsCheckedBeforeTheProjectState() {
    gitlab.connections.put(OWNER, connected().withError("CONNECTION_INVALID", NOW));
    fakes.projects.status(projectId, "completed");

    assertThrows(ConnectionInvalidException.class, () -> importIssues().execute(OWNER, projectId));
  }

  // -------------------------------------------------- @s16 paginar por X-Next-Page, tope de dos

  /** Una página de {@code count} issues; {@code more} es lo que anunciaría X-Next-Page. */
  private static IssuePage pageOf(int firstId, int count, boolean more) {
    var issues = new java.util.ArrayList<ExternalIssue>();
    for (int offset = 0; offset < count; offset++) issues.add(issue(firstId + offset));
    return new IssuePage(issues, count, more);
  }

  @ParameterizedTest(name = "{0}+{2} issues -> {4} peticiones, created {5}, truncated {6}")
  @CsvSource({
    "0,  false, 0,   false, 1, 0,   false",
    "37, false, 0,   false, 1, 37,  false",
    "100, true, 40,  false, 2, 140, false",
    "100, true, 100, true,  2, 200, true"
  })
  void s16_readsAtMostTwoPagesAndOnlySaysTruncatedWhenGitlabAnnouncesMore(
      int firstCount,
      boolean firstMore,
      int secondCount,
      boolean secondMore,
      int expectedRequests,
      int expectedCreated,
      boolean expectedTruncated) {
    fakes.source.page(1, pageOf(9001, firstCount, firstMore));
    fakes.source.page(2, pageOf(9001 + firstCount, secondCount, secondMore));

    var receipt = importIssues().execute(OWNER, projectId);

    assertEquals(expectedRequests, fakes.source.calls().size());
    assertEquals(expectedCreated, receipt.created());
    assertEquals(expectedTruncated, receipt.truncated());
    assertEquals(expectedCreated, fakes.tasks.links());
  }

  /** Una página llena, que es la única forma de que se pida la siguiente. */
  private static IssuePage fullPage(int firstId) {
    var issues = new java.util.ArrayList<ExternalIssue>();
    for (int offset = 0; offset < IssuePage.PAGE_SIZE; offset++)
      issues.add(issue(firstId + offset));
    return new IssuePage(issues, IssuePage.PAGE_SIZE, true);
  }

  @Test
  void s26_afailureOnTheSecondPageKeepsTheHundredTasksTheFirstOneConfirmed() {
    fakes.source.page(1, fullPage(9001));
    fakes.source.failOnPage(2, IssueSourceException.unavailable());

    var error =
        assertThrows(
            IssueImportFailedException.class, () -> importIssues().execute(OWNER, projectId));

    var receipt = error.receipt();
    assertEquals("failed", receipt.status());
    assertEquals("GITLAB_UNAVAILABLE", receipt.errorCode());
    assertEquals(IssuePage.PAGE_SIZE, receipt.created());
    assertEquals(0, receipt.skipped());
    assertEquals(0, receipt.failed());
    assertFalse(receipt.truncated());
    assertNotNull(receipt.finishedAt());
    assertEquals(IssuePage.PAGE_SIZE, fakes.tasks.tasks());
    assertEquals(IssuePage.PAGE_SIZE, fakes.tasks.links());
    assertEquals(IssuePage.PAGE_SIZE, fakes.tasks.events());
  }

  @Test
  void s26_asecondImportWithBothPagesHealthyOnlyCreatesWhatTheFailureLeftOut() {
    fakes.source.page(1, fullPage(9001));
    fakes.source.failOnPage(2, IssueSourceException.unavailable());
    assertThrows(IssueImportFailedException.class, () -> importIssues().execute(OWNER, projectId));
    fakes.source.failOnPage(2, null);
    fakes.source.page(2, new IssuePage(List.of(issue(9101), issue(9102)), 2, false));

    var receipt = importIssues().execute(OWNER, projectId);

    assertEquals("completed", receipt.status());
    assertEquals(2, receipt.created());
    assertEquals(IssuePage.PAGE_SIZE, receipt.skipped());
    assertEquals(IssuePage.PAGE_SIZE + 2, fakes.tasks.links());
  }

  @Test
  void s15_theIssuesAreAskedForByProjectReferenceWithTheDecryptedToken() {
    fakes.source.page(1, new IssuePage(List.of(issue(9001)), 1, false));

    importIssues().execute(OWNER, projectId);

    assertEquals(List.of("list " + REFERENCE + " page=1"), fakes.source.calls());
    assertEquals(TOKEN, fakes.source.lastToken());
  }
}
