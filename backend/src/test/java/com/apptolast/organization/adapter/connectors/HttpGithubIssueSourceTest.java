package com.apptolast.organization.adapter.connectors;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.application.IssueSourceException;
import com.apptolast.organization.application.IssueSourceException.Reason;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @s1 cabeceras, @s13 consulta fija y sin redirecciones, @s16 paginación, @s20 cuota agotada,
 * @s28 caídas y respuestas inesperadas, @s35 todo sale hacia la base configurada.
 */
class HttpGithubIssueSourceTest {
  private static final String REPOSITORY = "octocat/Hello-World";
  private static final String TOKEN = "ghp_secreto123";
  private static final Instant NOW = Instant.parse("2026-09-09T12:00:00Z");
  private static final String ISSUES_PATH = "/repos/octocat/Hello-World/issues";

  private FakeGithub github;
  private HttpGithubIssueSource source;

  @BeforeEach
  void setUp() throws IOException {
    github = new FakeGithub();
    source = newSource(NOW);
  }

  private HttpGithubIssueSource newSource(Instant now) {
    return new HttpGithubIssueSource(
        GithubApiBase.of(github.base()), new ObjectMapper(), Clock.fixed(now, ZoneOffset.UTC));
  }

  @AfterEach
  void tearDown() {
    github.close();
  }

  private IssueSourceException listFailure() {
    return assertThrows(IssueSourceException.class, () -> source.list(REPOSITORY, TOKEN, 1));
  }

  // ------------------------------------------------------------------------ @s1 cabeceras

  @Test
  void s1_verifyingAsksTheRepositoryAndTheUserWithTheFourRequiredHeaders() {
    github.reply("/repos/octocat/Hello-World", FakeGithub.Reply.ok("{\"full_name\":\"octocat/Hello-World\"}"));
    github.reply("/user", FakeGithub.Reply.ok("{\"login\":\"octocat\"}"));

    var identity = source.verify(REPOSITORY, TOKEN);

    assertEquals("octocat/Hello-World", identity.fullName());
    assertEquals("octocat", identity.login());
    assertEquals(2, github.received().size());
    for (var request : github.received()) {
      assertEquals("Bearer " + TOKEN, request.headers().get("authorization"));
      assertEquals("application/vnd.github+json", request.headers().get("accept"));
      assertEquals("2022-11-28", request.headers().get("x-github-api-version"));
      assertEquals("OrganizationWeb", request.headers().get("user-agent"));
    }
  }

  @Test
  void s1_theCanonicalNameComesFromGithubNotFromWhatTheCallerTyped() {
    github.reply("/repos/OCTOCAT/hello-world", FakeGithub.Reply.ok("{\"full_name\":\"octocat/Hello-World\"}"));
    github.reply("/user", FakeGithub.Reply.ok("{\"login\":\"octocat\"}"));

    assertEquals("octocat/Hello-World", source.verify("OCTOCAT/hello-world", TOKEN).fullName());
    assertEquals("/repos/OCTOCAT/hello-world", github.received().getFirst().path());
  }

  // ------------------------------------------------------- @s13 @s35 la consulta es fija

  @Test
  void s13_s35_theIssuesRequestUsesTheFixedQueryAgainstTheConfiguredBase() {
    github.reply(ISSUES_PATH, FakeGithub.Reply.ok("[]"));

    source.list(REPOSITORY, TOKEN, 1);

    var request = github.received().getFirst();
    assertEquals("GET", request.method());
    assertEquals(ISSUES_PATH, request.path());
    assertEquals("state=open&per_page=100&sort=created&direction=desc&page=1", request.query());
  }

  @Test
  void s13_aRedirectIsNotFollowedAndCountsAsAnUnavailableGithub() {
    github.reply(
        ISSUES_PATH,
        FakeGithub.Reply.status(302, Map.of("Location", github.base() + "/otra-ruta")));
    github.reply("/otra-ruta", FakeGithub.Reply.ok("[]"));

    assertEquals(Reason.UNAVAILABLE, listFailure().reason());
    assertEquals(1, github.received().size());
    assertEquals(ISSUES_PATH, github.received().getFirst().path());
  }

  // ------------------------------------------------------------------- @s12 @s16 lectura

  @Test
  void s12_pullRequestsAreDiscardedButStillCountForPaging() {
    github.reply(
        ISSUES_PATH,
        FakeGithub.Reply.ok(
            "["
                + FakeGithub.issueJson(101, "Uno", null)
                + ","
                + FakeGithub.issueJson(102, "Dos", null)
                + ","
                + FakeGithub.issueJson(103, "Tres", null)
                + ",{\"id\":104,\"title\":\"Un PR\",\"html_url\":\"https://x/104\",\"pull_request\":{\"url\":\"https://x\"}}]"));

    var page = source.list(REPOSITORY, TOKEN, 1);

    assertEquals(4, page.elements());
    assertEquals(3, page.issues().size());
    assertEquals(java.util.List.of("101", "102", "103"), page.issues().stream().map(i -> i.externalId()).toList());
    assertFalse(page.more());
  }

  @Test
  void s12_theIssueKeepsItsTitleBodyAndUrlAsGithubSentThem() {
    github.reply(
        ISSUES_PATH,
        FakeGithub.Reply.ok("[" + FakeGithub.issueJson(7, "Arreglar login", "una nota") + "]"));

    var issue = source.list(REPOSITORY, TOKEN, 1).issues().getFirst();

    assertEquals("7", issue.externalId());
    assertEquals("Arreglar login", issue.title());
    assertEquals("una nota", issue.body());
    assertEquals("https://github.com/octocat/Hello-World/issues/7", issue.url());
  }

  @Test
  void s16_moreIsTrueOnlyWhenGithubAnnouncesANextPage() {
    github.reply(
        ISSUES_PATH,
        new FakeGithub.Reply(
            200,
            FakeGithub.issuesJson(1, 2),
            Map.of(
                "Link",
                "<https://api.github.com/repositories/1/issues?page=2>; rel=\"next\", "
                    + "<https://api.github.com/repositories/1/issues?page=9>; rel=\"last\"")));

    assertTrue(source.list(REPOSITORY, TOKEN, 1).more());
  }

  @Test
  void s16_aLinkHeaderWithoutANextRelationDoesNotAnnounceMore() {
    github.reply(
        ISSUES_PATH,
        new FakeGithub.Reply(
            200,
            FakeGithub.issuesJson(1, 2),
            Map.of("Link", "<https://api.github.com/repositories/1/issues?page=1>; rel=\"prev\"")));

    assertFalse(source.list(REPOSITORY, TOKEN, 1).more());
  }

  // -------------------------------------------------------------------- @s20 cuota agotada

  @ParameterizedTest
  @CsvSource({"30,30", "1,1", "3600,3600"})
  void s20_aTooManyRequestsUsesItsRetryAfter(String header, int expected) {
    github.reply(ISSUES_PATH, FakeGithub.Reply.status(429, Map.of("Retry-After", header)));

    var error = listFailure();

    assertEquals(Reason.RATE_LIMITED, error.reason());
    assertEquals(expected, error.retryAfterSeconds());
  }

  @Test
  void s20_aTooManyRequestsWithoutHeadersFallsBackToSixtySeconds() {
    github.reply(ISSUES_PATH, FakeGithub.Reply.status(429, Map.of()));

    assertEquals(60, listFailure().retryAfterSeconds());
  }

  @Test
  void s20_aForbiddenWithoutQuotaLeftUsesTheResetInstant() {
    github.reply(
        ISSUES_PATH,
        FakeGithub.Reply.status(
            403,
            Map.of(
                "x-ratelimit-remaining", "0",
                "x-ratelimit-reset", String.valueOf(NOW.plusSeconds(120).getEpochSecond()))));

    var error = listFailure();

    assertEquals(Reason.RATE_LIMITED, error.reason());
    assertEquals(120, error.retryAfterSeconds());
  }

  @Test
  void s20_aResetAlreadyInThePastStillAsksToWaitOneSecond() {
    github.reply(
        ISSUES_PATH,
        FakeGithub.Reply.status(
            403,
            Map.of(
                "x-ratelimit-remaining", "0",
                "x-ratelimit-reset", String.valueOf(NOW.minusSeconds(5).getEpochSecond()))));

    assertEquals(1, listFailure().retryAfterSeconds());
  }

  @Test
  void s20_retryAfterWinsOverTheResetInstant() {
    github.reply(
        ISSUES_PATH,
        FakeGithub.Reply.status(
            403,
            Map.of(
                "Retry-After", "7",
                "x-ratelimit-reset", String.valueOf(NOW.plusSeconds(900).getEpochSecond()))));

    var error = listFailure();

    assertEquals(Reason.RATE_LIMITED, error.reason());
    assertEquals(7, error.retryAfterSeconds());
  }

  @Test
  void s20_aForbiddenWithoutQuotaLeftAndWithoutInstantsFallsBackToSixtySeconds() {
    github.reply(ISSUES_PATH, FakeGithub.Reply.status(403, Map.of("x-ratelimit-remaining", "0")));

    var error = listFailure();

    assertEquals(Reason.RATE_LIMITED, error.reason());
    assertEquals(60, error.retryAfterSeconds());
  }

  // ------------------------------------------------------- @s7 @s8 @s28 el resto de respuestas

  @Test
  void s7_anUnauthorizedMeansTheTokenWasRejected() {
    github.reply(ISSUES_PATH, FakeGithub.Reply.status(401, Map.of()));

    assertEquals(Reason.TOKEN_REJECTED, listFailure().reason());
  }

  @Test
  void s8_aNotFoundMeansTheRepositoryIsNotAvailable() {
    github.reply(ISSUES_PATH, FakeGithub.Reply.status(404, Map.of()));

    assertEquals(Reason.REPOSITORY_UNAVAILABLE, listFailure().reason());
  }

  @Test
  void s8_aForbiddenWithoutRateLimitMarksIsAlsoAnUnavailableRepository() {
    github.reply(ISSUES_PATH, FakeGithub.Reply.status(403, Map.of()));

    assertEquals(Reason.REPOSITORY_UNAVAILABLE, listFailure().reason());
  }

  @ParameterizedTest
  @ValueSource(ints = {500, 502, 503, 418, 301})
  void s28_anyOtherStatusIsAnUnavailableGithub(int status) {
    github.reply(ISSUES_PATH, FakeGithub.Reply.status(status, Map.of()));

    assertEquals(Reason.UNAVAILABLE, listFailure().reason());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "{\"issues\":[]}",
        "[{\"title\":\"sin id\",\"html_url\":\"https://x/1\"}]",
        "[{\"id\":\"no-es-numero\",\"title\":\"t\",\"html_url\":\"https://x/1\"}]",
        "[[]]",
        "no es json"
      })
  void s28_aBodyThatIsNotAnArrayOfIssuesIsAnUnavailableGithub(String body) {
    github.reply(ISSUES_PATH, FakeGithub.Reply.ok(body));

    assertEquals(Reason.UNAVAILABLE, listFailure().reason());
  }

  @Test
  void s28_aServerThatNeverSendsTheBodyGivesUpWellUnderFiveSeconds() {
    github.reply(ISSUES_PATH, FakeGithub.Reply.ok("[]"));
    github.delayBody(Duration.ofSeconds(20).toMillis());

    var started = System.nanoTime();
    var error = listFailure();
    var elapsed = Duration.ofNanos(System.nanoTime() - started);

    assertEquals(Reason.UNAVAILABLE, error.reason());
    assertTrue(elapsed.compareTo(Duration.ofMillis(4900)) < 0, "tardó " + elapsed);
  }

  @Test
  void s28_aRefusedConnectionGivesUpWellUnderThreeSeconds() throws IOException {
    var closed = new FakeGithub();
    var base = closed.base();
    closed.close();
    var offline =
        new HttpGithubIssueSource(
            GithubApiBase.of(base), new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));

    var started = System.nanoTime();
    var error =
        assertThrows(IssueSourceException.class, () -> offline.list(REPOSITORY, TOKEN, 1));
    var elapsed = Duration.ofNanos(System.nanoTime() - started);

    assertEquals(Reason.UNAVAILABLE, error.reason());
    assertTrue(elapsed.compareTo(Duration.ofMillis(2900)) < 0, "tardó " + elapsed);
  }

  @Test
  void s34_noFailureMessageEverCarriesTheToken() {
    github.reply(ISSUES_PATH, FakeGithub.Reply.status(401, Map.of()));

    var error = listFailure();

    assertFalse(error.getMessage().contains(TOKEN));
    assertFalse(String.valueOf(error.getCause()).contains(TOKEN));
  }
}
