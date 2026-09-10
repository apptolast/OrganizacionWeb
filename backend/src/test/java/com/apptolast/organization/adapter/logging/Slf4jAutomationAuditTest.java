package com.apptolast.organization.adapter.logging;

import static org.assertj.core.api.Assertions.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * El adaptador de bitácora, medido directamente y no a través del caso de uso.
 *
 * <p>Existe porque PIT no puede vigilarlo: su interceptor {@code FLOGCALL}, activo por defecto,
 * descarta las mutaciones que caen dentro de una llamada a un framework de logging. El único
 * enunciado de {@code runFinished} es {@code LOG.info(...)} y sus cinco argumentos son parámetros
 * pasados tal cual, sin ninguna expresión que calcular, así que después del filtro no queda nada
 * que mutar y la clase no aparece ni una vez en {@code mutations.xml}. El contraste que lo prueba
 * está en {@link Slf4jExternalCalendarAudit}, de la misma forma pero con un ternario entre los
 * argumentos: recibe exactamente ese mutante y ninguno más.
 *
 * <p>Con la campaña ciega, la cláusula de @s19 —«el log del worker contiene ruleId, eventId,
 * outcome, attempt y code y no contiene el título ni el nombre del proyecto»
 * (features/automations.feature:266)— sólo la sujeta esta prueba. Mismo reparto que {@link
 * Slf4jWebhookAuditTest} en la feature 25.
 */
class Slf4jAutomationAuditTest {
  private static final UUID RULE = UUID.fromString("11111111-1111-4111-8111-111111111111");
  private static final UUID EVENT = UUID.fromString("22222222-2222-4222-8222-222222222222");

  private final ListAppender<ILoggingEvent> captured = new ListAppender<>();
  private Logger logger;

  @BeforeEach
  void attach() {
    logger = (Logger) LoggerFactory.getLogger("organization.automations");
    captured.start();
    logger.addAppender(captured);
  }

  @AfterEach
  void detach() {
    logger.detachAppender(captured);
  }

  private String loggedText() {
    return captured.list.stream()
        .map(ILoggingEvent::getFormattedMessage)
        .reduce("", (all, line) -> all + line + "\n");
  }

  @Test
  void s19_aFinishedRunIsNamedByItsFiveIdentifiersAndByNothingElse() {
    new Slf4jAutomationAudit().runFinished(RULE, EVENT, "failed", 3, "PROJECT_COMPLETED");

    assertThat(loggedText())
        .contains("ruleId=" + RULE)
        .contains("eventId=" + EVENT)
        .contains("outcome=failed")
        .contains("attempt=3")
        .contains("code=PROJECT_COMPLETED");
  }

  @Test
  void s19_aRunThatWentWellStillCarriesTheCodeKeyWithNothingInIt() {
    new Slf4jAutomationAudit().runFinished(RULE, EVENT, "succeeded", 1, null);

    assertThat(loggedText()).contains("outcome=succeeded", "attempt=1", "code=null");
  }
}
