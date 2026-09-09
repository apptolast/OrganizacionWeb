package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.apptolast.organization.adapter.logging.Slf4jConnectorAudit;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * @s32 el barrido único: un solo recorrido —conectar y luego fallar una importación por 401— y una
 *     sola afirmación sobre todo lo que sale del servidor. Las pruebas de cada ruta ya comprueban
 *     que su respuesta no lleva el token; lo que ésta añade es que tampoco lo llevan la bitácora ni
 *     el problema del fallo, que son las dos salidas que nadie mira hasta que es tarde.
 *     <p>Se usa la auditoría real, no un doble: un doble no puede demostrar nada sobre lo que se
 *     escribe en los logs.
 */
class GitlabTokenConfinementTest {
  private static final String OWNER = "owner-1";
  private static final String API_BASE = "https://gitlab.example.com/api/v4";
  private static final Instant NOW = Instant.parse("2026-09-09T12:00:00Z");
  private static final String TOKEN = "glpat-SECRETOSECRETO1234";
  private static final String PROJECT_PATH = "grupo/proyecto";

  private ConnectorFakes fakes;
  private GitlabFakes gitlab;
  private UUID projectId;
  private ListAppender<ILoggingEvent> appender;
  private Logger logger;
  private ConnectorAudit audit;

  @BeforeEach
  void setUp() {
    fakes = new ConnectorFakes();
    gitlab = new GitlabFakes();
    projectId = fakes.projects.seed(OWNER, "idea");
    gitlab.projects.accept(PROJECT_PATH, 4821L);
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

  private GitlabConnectionView connect() {
    return new ConnectGitlab(
            gitlab.connections,
            gitlab.projects,
            fakes.receipts,
            API_BASE,
            gitlab.cipher,
            Clock.fixed(NOW, ZoneOffset.UTC))
        .execute(OWNER, TOKEN, PROJECT_PATH);
  }

  private ImportIssuesUseCase importIssues() {
    return new ImportIssues(
        new GitlabIssueConnections(gitlab.connections),
        fakes.receipts,
        fakes.projects,
        fakes.source,
        fakes.tasks,
        gitlab.cipher,
        audit,
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  /** Todo lo que el servidor entrega o escribe durante el recorrido completo, en un solo texto. */
  private String everythingThatLeavesTheServer() {
    var view = connect();
    fakes.source.failOnPage(1, IssueSourceException.tokenRejected());
    var failure =
        catchThrowableOfType(
            IssueImportFailedException.class, () -> importIssues().execute(OWNER, projectId));
    var stored = gitlab.connections.find(OWNER).orElseThrow();

    return String.join(
        "\n",
        view.toString(),
        String.valueOf(view.tokenHint()),
        failure.receipt().toString(),
        String.valueOf(failure.getMessage()),
        stored.toString(),
        fakes.source.calls().toString(),
        logged());
  }

  @Test
  void s32_neitherTheAnswersNorTheProblemNorTheLogEverCarryTheToken() {
    var everything = everythingThatLeavesTheServer();

    assertThat(everything)
        .doesNotContain(TOKEN)
        .doesNotContain("SECRETOSECRETO1234")
        .doesNotContain(Base64.getEncoder().encodeToString(TOKEN.getBytes(StandardCharsets.UTF_8)));
  }

  /**
   * La pista son cuatro caracteres y son los últimos: si saliera más, saldría parte del secreto.
   */
  @Test
  void s32_theOnlyThingThatComesBackIsTheFourCharacterHint() {
    var view = connect();

    assertThat(view.tokenHint()).isEqualTo("1234").hasSize(4);
    assertThat(TOKEN).endsWith(view.tokenHint());
  }

  @Test
  void s32_theLogOfTheFailureNamesTheStableCodeSoItCanBeDiagnosedWithoutTheToken() {
    everythingThatLeavesTheServer();

    assertThat(logged()).contains("CONNECTION_INVALID").contains("owner=" + OWNER);
  }

  /**
   * El token viaja como argumento del puerto, nunca dentro de la referencia del proyecto: es lo que
   * impide que acabe en una URL, en una consulta o en la traza de una petición.
   */
  @Test
  void s32_theProviderIsAskedByProjectReferenceAndTheTokenTravelsApart() {
    connect();
    fakes.source.page(1, new IssuePage(java.util.List.of(), 0, false));

    importIssues().execute(OWNER, projectId);

    assertThat(fakes.source.calls()).allSatisfy(call -> assertThat(call).doesNotContain(TOKEN));
    assertThat(fakes.source.lastToken()).isEqualTo(TOKEN);
  }
}
