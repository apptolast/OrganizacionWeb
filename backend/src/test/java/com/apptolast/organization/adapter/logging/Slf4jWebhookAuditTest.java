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

  @Test
  void s35_anAttemptLogsTheEventTheEndpointAndTheErrorClassAndNothingElse() {
    new Slf4jWebhookAudit().attempt(ENDPOINT, EVENT, "pending", "HTTP_ERROR");

    var text = loggedText();
    assertTrue(text.contains(EVENT.toString()), "the event id identifies the delivery");
    assertTrue(text.contains(ENDPOINT.toString()), "the endpoint id identifies the webhook");
    assertTrue(text.contains("HTTP_ERROR"));
    assertTrue(text.contains("pending"));
  }

  @Test
  void s35_aSuccessfulAttemptCarriesNoErrorClass() {
    new Slf4jWebhookAudit().attempt(ENDPOINT, EVENT, "succeeded", null);

    assertTrue(loggedText().contains("succeeded"));
  }

  @Test
  void s21_s35_aDiscardedRowIsAuditedWithItsEventAndCode() {
    new Slf4jWebhookAudit().discarded(ENDPOINT, EVENT, "INVALID_EVENT");

    var text = loggedText();
    assertTrue(text.contains(EVENT.toString()));
    assertTrue(text.contains("INVALID_EVENT"));
  }

  @Test
  void s9_s35_aWorkerErrorIsAuditedByCodeAlone() {
    new Slf4jWebhookAudit().workerError("CONFIGURATION_ERROR");

    assertTrue(loggedText().contains("CONFIGURATION_ERROR"));
  }

  @Test
  void s35_theAuditHasNoWayToReceiveAUrlASecretASignatureNorABody() {
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
