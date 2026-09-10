package com.apptolast.organization.adapter.connectors;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.application.IssueSourceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @s9 @s11 @s12 @s16 @s17 @s24 @s25 @s32 el adaptador HTTP de GitLab contra un servidor falso en
 *     bucle local. Ninguna prueba de esta clase habla con gitlab.com.
 */
class HttpGitlabIssueSourceTest {
  private static final String TOKEN = "glpat-SECRETOSECRETO1234";
  private static final String PROJECT_PATH = "grupo/sub/proyecto";
  private static final String REFERENCE = "4821";
  private static final Instant NOW = Instant.parse("2026-09-09T12:00:00Z");

  /**
   * El techo que fija el contrato (@s25, fila «200 con cuerpo JSON de más de 5 MiB»). Se escribe
   * aquí y no se importa de producción a propósito: si alguien sube la constante del adaptador,
   * estas dos pruebas tienen que enterarse en vez de seguirla.
   */
  private static final int FIVE_MEBIBYTES = 5 * 1024 * 1024;

  /** Lo que tarda el proveedor mudo en decidirse: más que el plazo de lectura del adaptador. */
  private static final java.time.Duration PROVIDER_DELAY = java.time.Duration.ofSeconds(6);

  private FakeIssueServer gitlab;
  private HttpGitlabIssueSource source;

  @BeforeEach
  void setUp() throws IOException {
    gitlab = new FakeIssueServer();
    source =
        new HttpGitlabIssueSource(
            GitlabApiBase.of(gitlab.base() + "/api/v4"),
            new ObjectMapper(),
            Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @AfterEach
  void tearDown() {
    gitlab.close();
  }

  private void projectReplies(FakeIssueServer.Reply reply) {
    gitlab.reply("/api/v4/projects/grupo%2Fsub%2Fproyecto", reply);
  }

  private void issuesReply(FakeIssueServer.Reply reply) {
    gitlab.reply("/api/v4/projects/4821/issues", reply);
  }

  // ------------------------------------------------------------------ @s9 @s11 verificar

  @Test
  void s9_verifyingAsksForTheEncodedProjectWithThePrivateTokenHeaderAndNeverInTheUrl() {
    projectReplies(
        FakeIssueServer.Reply.ok("{\"id\":4821,\"path_with_namespace\":\"grupo/sub/proyecto\"}"));

    var project = source.verify(PROJECT_PATH, TOKEN);

    assertThat(project.id()).isEqualTo(4821L);
    assertThat(project.pathWithNamespace()).isEqualTo("grupo/sub/proyecto");
    assertThat(gitlab.received()).hasSize(1);
    var request = gitlab.received().getFirst();
    assertThat(request.method()).isEqualTo("GET");
    assertThat(request.path()).isEqualTo("/api/v4/projects/grupo%2Fsub%2Fproyecto");
    assertThat(request.query()).isNull();
    assertThat(request.headers()).containsEntry("private-token", TOKEN);
    assertThat(request.headers()).doesNotContainKey("authorization");
  }

  @Test
  void s9_averifiedProjectWithoutTheExpectedShapeIsAnUnavailableProvider() {
    projectReplies(FakeIssueServer.Reply.ok("{\"id\":\"4821\"}"));

    assertThat(reasonOf(() -> source.verify(PROJECT_PATH, TOKEN)))
        .isEqualTo(IssueSourceException.Reason.UNAVAILABLE);
  }

  @ParameterizedTest
  @CsvSource({
    "401, TOKEN_REJECTED",
    "403, TOKEN_REJECTED",
    "404, REPOSITORY_UNAVAILABLE",
    "500, UNAVAILABLE",
    "302, UNAVAILABLE"
  })
  void s12_everyRefusalOfTheProviderIsClassifiedWithoutQuotingItsBody(int status, String reason) {
    projectReplies(
        new FakeIssueServer.Reply(
            status, "{\"message\":\"invalid_token: glpat-abcdef1234\"}", Map.of()));

    var error =
        catchThrowableOfType(IssueSourceException.class, () -> source.verify(PROJECT_PATH, TOKEN));

    assertThat(error.reason()).isEqualTo(IssueSourceException.Reason.valueOf(reason));
    assertThat(error.getMessage()).doesNotContain("glpat").doesNotContain("invalid_token");
  }

  @Test
  void s12_abodyThatIsNotJsonIsAnUnavailableProvider() {
    projectReplies(new FakeIssueServer.Reply(200, "<html>mantenimiento</html>", Map.of()));

    assertThat(reasonOf(() -> source.verify(PROJECT_PATH, TOKEN)))
        .isEqualTo(IssueSourceException.Reason.UNAVAILABLE);
  }

  @Test
  void s12_anExhaustedQuotaKeepsTheRetryAfterTheProviderAsked() {
    projectReplies(FakeIssueServer.Reply.status(429, Map.of("Retry-After", "20")));

    var error =
        catchThrowableOfType(IssueSourceException.class, () -> source.verify(PROJECT_PATH, TOKEN));

    assertThat(error.reason()).isEqualTo(IssueSourceException.Reason.RATE_LIMITED);
    assertThat(error.retryAfterSeconds()).isEqualTo(20);
  }

  // --------------------------------------------------------- @s16 @s17 @s24 @s25 listar

  @Test
  void s16_listingAsksForOpenIssuesAHundredAtATimeAndReadsTheNextPageHeader() {
    issuesReply(
        new FakeIssueServer.Reply(
            200,
            "[" + gitlabIssue(9001) + "," + gitlabIssue(9002) + "]",
            Map.of("X-Next-Page", "2")));

    var page = source.list(REFERENCE, TOKEN, 1);

    assertThat(page.issues()).hasSize(2);
    assertThat(page.elements()).isEqualTo(2);
    assertThat(page.more()).isTrue();
    var request = gitlab.received().getFirst();
    assertThat(request.path()).isEqualTo("/api/v4/projects/4821/issues");
    assertThat(request.query()).isEqualTo("state=opened&per_page=100&page=1");
    assertThat(request.headers()).containsEntry("private-token", TOKEN);
  }

  @Test
  void s16_anEmptyNextPageHeaderMeansThereIsNothingMore() {
    issuesReply(
        new FakeIssueServer.Reply(200, "[" + gitlabIssue(9001) + "]", Map.of("X-Next-Page", "")));

    assertThat(source.list(REFERENCE, TOKEN, 1).more()).isFalse();
  }

  @Test
  void s15_eachIssueBecomesAnExternalIssueIdentifiedByHostAndGlobalId() {
    issuesReply(FakeIssueServer.Reply.ok("[" + gitlabIssue(9001) + "]"));

    var issue = source.list(REFERENCE, TOKEN, 1).issues().getFirst();

    assertThat(issue.externalId()).isEqualTo("127.0.0.1:9001");
    assertThat(issue.title()).isEqualTo("Issue 9001");
    assertThat(issue.url()).isEqualTo("https://gitlab.example.com/grupo/proyecto/-/issues/9001");
  }

  /**
   * @s21 y @s15: el cuerpo de la issue es lo que alimenta el completionCriterion de la tarea, y
   *     GitLab lo entrega de tres formas —con texto, como null explícito y sin el campo—. Sin
   *     afirmar las tres, invertir cualquiera de las dos mitades de la guarda del adaptador pasa
   *     inadvertido: las issues de las demás pruebas traen todas {@code "description":null}.
   */
  @Test
  void s21_thedescriptionOfTheIssueArrivesAsTheBodyOfTheExternalIssue() {
    issuesReply(FakeIssueServer.Reply.ok("[" + gitlabIssueDescribed(9001, "\"una\\ndos\"") + "]"));

    assertThat(source.list(REFERENCE, TOKEN, 1).issues().getFirst().body()).isEqualTo("una\ndos");
  }

  @Test
  void s21_anExplicitNullDescriptionIsNoBodyAtAllAndNotTheWordNull() {
    issuesReply(FakeIssueServer.Reply.ok("[" + gitlabIssueDescribed(9001, "null") + "]"));

    assertThat(source.list(REFERENCE, TOKEN, 1).issues().getFirst().body()).isNull();
  }

  @Test
  void s21_anAbsentDescriptionFieldIsNoBodyEither() {
    issuesReply(FakeIssueServer.Reply.ok("[" + gitlabIssueWithoutDescription(9001) + "]"));

    assertThat(source.list(REFERENCE, TOKEN, 1).issues().getFirst().body()).isNull();
  }

  @ParameterizedTest
  @CsvSource({
    "'\"issue_type\":\"incident\"'",
    "'\"issue_type\":\"test_case\"'",
    "'\"issue_type\":\"task\"'",
    "'\"issue_type\":\"issue\",\"moved_to_id\":555'"
  })
  void s17_whatIsNotAPlainOpenIssueIsExcludedAndCounted(String marker) {
    issuesReply(
        FakeIssueServer.Reply.ok(
            "["
                + gitlabIssue(9001)
                + ","
                + gitlabIssue(9002)
                + ","
                + gitlabIssueWith(9003, marker)
                + "]"));

    var page = source.list(REFERENCE, TOKEN, 1);

    assertThat(page.issues()).hasSize(2);
    assertThat(page.excluded()).isEqualTo(1);
    assertThat(page.elements()).isEqualTo(3);
  }

  @Test
  void s24_aquotaAnnouncedWithoutRetryAfterFallsBackToAMinute() {
    issuesReply(FakeIssueServer.Reply.status(429, Map.of()));

    var error =
        catchThrowableOfType(IssueSourceException.class, () -> source.list(REFERENCE, TOKEN, 1));

    assertThat(error.reason()).isEqualTo(IssueSourceException.Reason.RATE_LIMITED);
    assertThat(error.retryAfterSeconds()).isEqualTo(60);
  }

  @Test
  void s24_ahundredOkWithNoQuotaLeftIsAlsoAnExhaustedQuota() {
    issuesReply(
        new FakeIssueServer.Reply(
            200, "[]", Map.of("RateLimit-Remaining", "0", "Retry-After", "5")));

    var error =
        catchThrowableOfType(IssueSourceException.class, () -> source.list(REFERENCE, TOKEN, 1));

    assertThat(error.reason()).isEqualTo(IssueSourceException.Reason.RATE_LIMITED);
    assertThat(error.retryAfterSeconds()).isEqualTo(5);
  }

  @Test
  void s25_aredirectionIsNeverFollowedSoTheTokenDoesNotTravelWhereGitlabPoints() {
    gitlab.reply(
        "/api/v4/projects/4821/issues",
        FakeIssueServer.Reply.status(301, Map.of("Location", "/api/v4/robado")));
    gitlab.reply("/api/v4/robado", FakeIssueServer.Reply.ok("[]"));

    assertThat(reasonOf(() -> source.list(REFERENCE, TOKEN, 1)))
        .isEqualTo(IssueSourceException.Reason.UNAVAILABLE);
    assertThat(gitlab.received()).hasSize(1);
  }

  /**
   * @s25, fila «sin respuesta dentro del tiempo de lectura». El veredicto UNAVAILABLE por sí solo
   *     no discriminaba nada: un proveedor que tarda seis segundos y luego manda un cuerpo vacío da
   *     UNAVAILABLE igual sin plazo ninguno, porque el cuerpo vacío tampoco es un array. Lo que hay
   *     que medir es que se rinde <em>antes</em> de que el proveedor termine, y eso es el reloj.
   */
  @Test
  @Timeout(value = 20, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
  void s25_aproviderThatNeverFinishesAnsweringIsCutBeforeItDecidesToAnswer() {
    issuesReply(FakeIssueServer.Reply.ok("[" + gitlabIssue(9001) + "]"));
    gitlab.delayBody(PROVIDER_DELAY.toMillis());

    var started = System.nanoTime();
    var reason = reasonOf(() -> source.list(REFERENCE, TOKEN, 1));
    var elapsed = java.time.Duration.ofNanos(System.nanoTime() - started);

    assertThat(reason).isEqualTo(IssueSourceException.Reason.UNAVAILABLE);
    assertThat(elapsed).as("tardó %s", elapsed).isLessThan(PROVIDER_DELAY.minusMillis(500));
  }

  @Test
  void s25_abodyThatIsNotAnArrayOfIssuesIsUnavailable() {
    issuesReply(FakeIssueServer.Reply.ok("{\"error\":\"nope\"}"));

    assertThat(reasonOf(() -> source.list(REFERENCE, TOKEN, 1)))
        .isEqualTo(IssueSourceException.Reason.UNAVAILABLE);
  }

  /**
   * @s25, fila «200 con cuerpo JSON de más de 5 MiB». El cuerpo es una issue válida y bien formada:
   *     lo que el adaptador tiene que rechazar es el <em>tamaño</em>, no la forma. Sin techo el
   *     adaptador la importaría tan campante, después de haberla cargado entera en memoria, y con
   *     un {@code api-base} autoalojado —que el contrato permite— eso agota el proceso.
   */
  @Test
  void s25_abodyOverFiveMebibytesIsUnavailableInsteadOfEatingTheMemory() {
    issuesReply(FakeIssueServer.Reply.ok(issueArrayOfExactly(FIVE_MEBIBYTES + 1)));

    assertThat(reasonOf(() -> source.list(REFERENCE, TOKEN, 1)))
        .isEqualTo(IssueSourceException.Reason.UNAVAILABLE);
  }

  /** La otra mitad del techo: justo en el límite todavía se importa, o el corte sería otro. */
  @Test
  void s25_abodyOfExactlyFiveMebibytesStillImports() {
    issuesReply(FakeIssueServer.Reply.ok(issueArrayOfExactly(FIVE_MEBIBYTES)));

    assertThat(source.list(REFERENCE, TOKEN, 1).issues()).hasSize(1);
  }

  /**
   * @s25, fila «sin respuesta dentro del tiempo de lectura», la mitad que el techo de tamaño no
   *     cubre: un proveedor que manda las cabeceras y luego <em>no emite ni cierra</em> no ofrece
   *     ni un byte que contar, así que sólo un plazo lo corta. El plazo del cliente HTTP no vale,
   *     porque con {@code ofInputStream()} se cancela en cuanto llegan las cabeceras.
   *     <p>El oráculo es el {@code @Timeout} en hilo aparte: sin plazo de lectura la llamada no
   *     termina nunca y esto falla por vencimiento, en vez de colgar la suite. La prueba hermana de
   *     {@code delayBody} <b>no</b> sirve para esto: allí el proveedor acaba cerrando y el cuerpo
   *     vacío ya no es un array, de modo que da UNAVAILABLE aunque no haya plazo ninguno.
   */
  @Test
  @Timeout(value = 20, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
  void s25_aproviderThatOpensTheBodyAndGoesSilentIsCutByTheReadDeadline() {
    issuesReply(FakeIssueServer.Reply.ok("[" + gitlabIssue(9001) + "]"));
    gitlab.holdBody();

    assertThat(reasonOf(() -> source.list(REFERENCE, TOKEN, 1)))
        .isEqualTo(IssueSourceException.Reason.UNAVAILABLE);
  }

  /**
   * Array JSON de una sola issue válida cuyo tamaño en bytes es exactamente el pedido. Todo el
   * relleno va en la descripción, y todo es ASCII, así que un carácter es un byte.
   */
  private static String issueArrayOfExactly(int bytes) {
    var head =
        "[{\"id\":9001,\"title\":\"Issue 9001\",\"issue_type\":\"issue\",\"web_url\":"
            + "\"https://gitlab.example.com/grupo/proyecto/-/issues/9001\",\"description\":\"";
    var tail = "\"}]";
    return head + "x".repeat(bytes - head.length() - tail.length()) + tail;
  }

  private static IssueSourceException.Reason reasonOf(Runnable work) {
    return catchThrowableOfType(IssueSourceException.class, work::run).reason();
  }

  private static String gitlabIssue(int id) {
    return gitlabIssueWith(id, "\"issue_type\":\"issue\"");
  }

  /** La misma issue con la descripción que se le indique, ya escrita como JSON. */
  private static String gitlabIssueDescribed(int id, String descriptionJson) {
    return gitlabIssueWithoutDescription(id)
        .replace("\"issue_type\"", "\"description\":" + descriptionJson + ",\"issue_type\"");
  }

  private static String gitlabIssueWithoutDescription(int id) {
    return "{\"id\":"
        + id
        + ",\"title\":\"Issue "
        + id
        + "\",\"web_url\":"
        + "\"https://gitlab.example.com/grupo/proyecto/-/issues/"
        + id
        + "\",\"issue_type\":\"issue\"}";
  }

  private static String gitlabIssueWith(int id, String marker) {
    return "{\"id\":"
        + id
        + ",\"title\":\"Issue "
        + id
        + "\",\"description\":null,\"web_url\":"
        + "\"https://gitlab.example.com/grupo/proyecto/-/issues/"
        + id
        + "\","
        + marker
        + "}";
  }
}
