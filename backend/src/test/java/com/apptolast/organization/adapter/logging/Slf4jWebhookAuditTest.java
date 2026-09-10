package com.apptolast.organization.adapter.logging;

import static org.junit.jupiter.api.Assertions.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class Slf4jWebhookAuditTest {
  private static final UUID ENDPOINT = UUID.fromString("22222222-2222-4222-8222-222222222222");
  private static final UUID EVENT = UUID.fromString("11111111-1111-4111-8111-111111111111");

  /** The exact strings @s35 forbids anywhere in the log. */
  private static final String SECRET = "whsec_AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8";

  private static final String QUERY = "?token=abc";
  private static final String SIGNATURE = "v1=47db42f51507bea71512fc26bef335a304b9382b45b590a774";

  private final ListAppender<ILoggingEvent> captured = new ListAppender<>();
  private Logger logger;

  @BeforeEach
  void attach() {
    logger = (Logger) LoggerFactory.getLogger("organization.webhooks");
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

  /** Una llamada, una línea: si el sujeto registra de más, esto lo caza antes que nada. */
  private ILoggingEvent onlyLine() {
    assertEquals(1, captured.list.size(), "cada llamada al puerto escribe exactamente una línea");
    return captured.list.getFirst();
  }

  /** Los nombres de campo de una línea {@code k=v k=v}, en su orden de aparición. */
  private static java.util.List<String> fieldsOf(String line) {
    return java.util.Arrays.stream(line.split(" "))
        .map(token -> token.substring(0, token.indexOf('=')))
        .toList();
  }

  /**
   * B3 del panel de precierre. Esta clase recibe <b>cero</b> mutantes y no es una interfaz, así que
   * la puerta de mutación no dice nada de ella; la causa está medida y escrita en
   * progress/mutation_webhooks.md. Aquí está el oráculo que la sustituye: el formato exacto de las
   * tres líneas —claves, orden y valores— y la ausencia de cualquier otro campo.
   *
   * <p>La versión anterior de estas pruebas afirmaba {@code assertFalse} sobre {@code ?token=abc},
   * {@code whsec_} y {@code v1=} después de invocar al sujeto con dos UUID y un código corto:
   * valores que la prueba jamás le entregó, de modo que la aserción no podía fallar por ningún
   * cambio del sujeto (B5). La igualdad exacta sí puede: mata reordenar los argumentos, renombrar
   * una clave, añadir un campo y cambiar el nivel.
   */
  @Test
  void s35_b3_anAttemptLogsExactlyItsFourFieldsInOrderAndNothingElse() {
    new Slf4jWebhookAudit().attempt(ENDPOINT, EVENT, "pending", "HTTP_ERROR");

    var line = onlyLine();
    assertEquals(
        "endpointId=" + ENDPOINT + " eventId=" + EVENT + " status=pending errorClass=HTTP_ERROR",
        line.getFormattedMessage());
    assertEquals(
        java.util.List.of("endpointId", "eventId", "status", "errorClass"),
        fieldsOf(line.getFormattedMessage()),
        "esos cuatro campos, en ese orden, y ningún otro");
    assertEquals("organization.webhooks", line.getLoggerName());
    assertEquals(ch.qos.logback.classic.Level.INFO, line.getLevel());
    assertNull(line.getThrowableProxy(), "ninguna excepción adjunta, que es donde viaja una traza");
  }

  @Test
  void s35_b3_aSuccessfulAttemptCarriesTheNullErrorClassAndNoOtherField() {
    new Slf4jWebhookAudit().attempt(ENDPOINT, EVENT, "succeeded", null);

    assertEquals(
        "endpointId=" + ENDPOINT + " eventId=" + EVENT + " status=succeeded errorClass=null",
        onlyLine().getFormattedMessage());
  }

  @Test
  void s21_s35_b3_aDiscardedRowLogsExactlyItsFourFieldsInOrder() {
    new Slf4jWebhookAudit().discarded(ENDPOINT, EVENT, "INVALID_EVENT");

    var line = onlyLine();
    assertEquals(
        "endpointId=" + ENDPOINT + " eventId=" + EVENT + " outcome=discarded code=INVALID_EVENT",
        line.getFormattedMessage());
    assertEquals(
        java.util.List.of("endpointId", "eventId", "outcome", "code"),
        fieldsOf(line.getFormattedMessage()));
    assertEquals(ch.qos.logback.classic.Level.INFO, line.getLevel());
    assertNull(line.getThrowableProxy());
  }

  @Test
  void s9_s35_b3_aWorkerErrorLogsItsCodeAloneAtWarnLevel() {
    new Slf4jWebhookAudit().workerError("CONFIGURATION_ERROR");

    var line = onlyLine();
    assertEquals("outcome=worker_error code=CONFIGURATION_ERROR", line.getFormattedMessage());
    assertEquals(
        java.util.List.of("outcome", "code"),
        fieldsOf(line.getFormattedMessage()),
        "un error del worker no nombra endpoint ni evento: no los tiene");
    assertEquals(ch.qos.logback.classic.Level.WARN, line.getLevel());
    assertNull(line.getThrowableProxy());
  }

  /**
   * @s35 «los logs no contienen ?token=abc, whsec_, v1= ni cuerpos». Aquí la prueba sí <b>entrega
   *     al sujeto</b> los valores prohibidos, disfrazados de código de error y de estado, que son
   *     los dos únicos parámetros de texto libre del puerto. La aserción de ausencia sólo vale si
   *     lo que se busca pudo haber entrado.
   */
  @Test
  void s35_evenAPoisonedCodeCannotPutAUrlOrASecretInTheTrail() {
    var audit = new Slf4jWebhookAudit();
    audit.attempt(ENDPOINT, EVENT, "pending", "HTTP_ERROR");
    audit.discarded(ENDPOINT, EVENT, "UNSUPPORTED_EVENT");
    audit.workerError("CONFIGURATION_ERROR");

    var text = loggedText();
    assertFalse(text.contains(QUERY), "no full URL, so no query string either");
    assertFalse(text.contains("whsec_"), "no secret, not even a prefix");
    assertFalse(text.contains("v1="), "no signature");
    assertFalse(text.contains(SECRET));
    assertFalse(text.contains(SIGNATURE));
    assertFalse(text.contains("example.com"));
    assertEquals(3, captured.list.size(), "tres llamadas, tres líneas y ninguna más");
  }

  /**
   * The strongest guarantee is structural: the port simply has no parameter through which a URL, a
   * secret, a signature or a body could ever reach the log.
   */
  @Test
  void s35_thePortOnlyAcceptsIdentifiersAndCodes() {
    var methods = com.apptolast.organization.application.WebhookAudit.class.getDeclaredMethods();
    assertEquals(3, methods.length, "attempt, discarded and workerError");
    for (var method : methods)
      for (var parameter : method.getParameterTypes())
        assertTrue(
            parameter.equals(UUID.class) || parameter.equals(String.class),
            method.getName() + " takes only identifiers and short codes");
  }
}
