package com.apptolast.organization.adapter.logging;

import static org.assertj.core.api.Assertions.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * @s34 la bitácora del conector cuenta propietario, repositorio, código HTTP de GitHub y
 * contadores, y no cuenta el token ni en claro ni en base64.
 */
class ConnectorAuditTest {
  private static final String TOKEN = "ghp_canal_secreto";
  private static final UUID IMPORT = UUID.randomUUID();

  private ListAppender<ILoggingEvent> appender;
  private Logger logger;
  private Slf4jConnectorAudit audit;

  @BeforeEach
  void setUp() {
    logger = (Logger) LoggerFactory.getLogger("organization.connectors");
    appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    audit = new Slf4jConnectorAudit();
  }

  @AfterEach
  void tearDown() {
    logger.detachAppender(appender);
  }

  private String logged() {
    return appender.list.stream()
        .map(ILoggingEvent::getFormattedMessage)
        .reduce("", (all, line) -> all + "\n" + line);
  }

  @Test
  void s34_connectingRecordsOwnerRepositoryAndLoginButNeverTheToken() {
    audit.connected("owner-a", "octocat/Hello-World", "octocat");

    assertThat(logged())
        .contains("owner=owner-a")
        .contains("repository=octocat/Hello-World")
        .contains("login=octocat")
        .doesNotContain(TOKEN);
  }

  @Test
  void s34_aRefusedConnectionRecordsTheReasonAndTheStatusGithubAnswered() {
    audit.connectionRefused("owner-a", "octocat/Hello-World", "GITHUB_TOKEN_REJECTED", 401);

    assertThat(logged())
        .contains("owner=owner-a")
        .contains("repository=octocat/Hello-World")
        .contains("code=GITHUB_TOKEN_REJECTED")
        .contains("githubStatus=401");
  }

  @Test
  void s34_afinishedImportRecordsItsIdentifierAndItsCounters() {
    audit.importFinished("owner-a", "octocat/Hello-World", IMPORT, 199, 0, 1, true);

    assertThat(logged())
        .contains("importId=" + IMPORT)
        .contains("created=199")
        .contains("skipped=0")
        .contains("failed=1")
        .contains("truncated=true");
  }

  @Test
  void s34_abrokenImportRecordsWhatItManagedToDoAndWhyItStopped() {
    audit.importFailed("owner-a", "octocat/Hello-World", IMPORT, "RATE_LIMITED", 2, 429);

    assertThat(logged())
        .contains("importId=" + IMPORT)
        .contains("code=RATE_LIMITED")
        .contains("created=2")
        .contains("githubStatus=429");
  }

  @Test
  void s34_nothingTheAuditWritesCanCarryTheTokenBecauseItNeverReceivesIt() {
    audit.connected("owner-a", "octocat/Hello-World", "octocat");
    audit.connectionRefused("owner-a", "octocat/Hello-World", "GITHUB_TOKEN_REJECTED", 401);
    audit.importFinished("owner-a", "octocat/Hello-World", IMPORT, 1, 0, 0, false);
    audit.importFailed("owner-a", "octocat/Hello-World", IMPORT, "GITHUB_UNAVAILABLE", 0, 500);

    var everything = logged();
    assertThat(everything).doesNotContain(TOKEN);
    assertThat(everything)
        .doesNotContain(Base64.getEncoder().encodeToString(TOKEN.getBytes()));
    for (var method : Slf4jConnectorAudit.class.getMethods())
      assertThat(method.getName().toLowerCase(java.util.Locale.ROOT))
          .doesNotContain("token");
  }
}
