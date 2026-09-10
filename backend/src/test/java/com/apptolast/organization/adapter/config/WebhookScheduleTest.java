package com.apptolast.organization.adapter.config;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.application.DispatchWebhooksUseCase;
import com.apptolast.organization.application.EnqueueWebhookDeliveriesUseCase;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class WebhookScheduleTest {
  private final List<String> calls = new ArrayList<>();
  private final EnqueueWebhookDeliveriesUseCase enqueue = () -> calls.add("enqueue");
  private final DispatchWebhooksUseCase dispatch = () -> calls.add("dispatch");

  @Test
  void s18_s20_eachTickEnqueuesBeforeItDispatchesSoANewEventCanShipInTheSameTick() {
    new WebhookSchedule(enqueue, dispatch).tick();

    assertEquals(List.of("enqueue", "dispatch"), calls);
  }

  @Test
  void s32_aFailingCycleNeverEscapesTheScheduledMethod() {
    EnqueueWebhookDeliveriesUseCase broken =
        () -> {
          throw new IllegalStateException("test-only failure");
        };

    assertDoesNotThrow(() -> new WebhookSchedule(broken, dispatch).tick());
    assertEquals(List.of("dispatch"), calls, "a broken enqueue must not stop the dispatch");
  }

  @Test
  void s32_theWorkerOnlyExistsWhenTheFlagIsExplicitlyTrue() {
    var runner =
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TaskSchedulingAutoConfiguration.class))
            .withBean(EnqueueWebhookDeliveriesUseCase.class, () -> enqueue)
            .withBean(DispatchWebhooksUseCase.class, () -> dispatch)
            .withUserConfiguration(WebhookConfiguration.class);

    runner.run(context -> assertFalse(context.containsBean("webhookSchedule")));
    runner
        .withPropertyValues("app.webhooks.enabled=false")
        .run(context -> assertFalse(context.containsBean("webhookSchedule")));
    runner
        .withPropertyValues("app.webhooks.enabled=true")
        .run(context -> assertTrue(context.containsBean("webhookSchedule")));
  }

  /** La URL registrada del Given de @s35, con su cadena de consulta. */
  private static final String POISONED_URL = "https://example.com/hooks?token=abc";

  /** Con forma de secreto y sin serlo: esta cadena no abre nada en ningún sitio. */
  private static final String FAKE_SECRET = "whsec_ESTO-NO-ES-UN-SECRETO-REAL";

  private static final String SIGNATURE = "t=1788861600,v1=00112233445566778899aabbccddeeff";
  private static final String BODY = "{\"eventId\":\"abc\",\"titulo\":\"dato de otra cuenta\"}";

  /**
   * @s35 «los logs contienen eventId, endpointId y clase de error y no contienen ?token=abc,
   *     whsec_, v1= ni cuerpos de respuesta». El Given sitúa el riesgo en el <b>ciclo del
   *     worker</b> —el planificador, JDBC, el cliente HTTP—, y ninguna prueba enganchaba un
   *     appender al logger <b>ROOT</b> mientras el worker corría (B5 del panel). Sin ROOT no se ve
   *     lo que escriben terceros ni {@code WebhookSchedule}, sólo el adaptador de auditoría.
   *     <p>La aserción no es vacía: el ciclo con éxito hace pasar de verdad la URL envenenada, el
   *     secreto y el cuerpo por el sujeto, y el ciclo con fallo revienta con una excepción cuyo
   *     mensaje los lleva dentro, que es justo lo que traería una excepción de JDBC o del cliente
   *     HTTP. Se mira también el {@code Throwable} adjunto, mensaje y traza: es la vía por la que
   *     un {@code LOG.warn(mensaje, fallo)} sacaría la URL sin escribirla en el texto.
   */
  @Test
  void s35_neitherASuccessfulNorAFailedWorkerTickPutsAUrlSecretSignatureOrBodyInTheRootLog() {
    var seen = new ArrayList<String>();
    var root =
        (ch.qos.logback.classic.Logger)
            org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    var captured =
        new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
    captured.start();
    root.addAppender(captured);
    try {
      new WebhookSchedule(() -> {}, dispatchThatReallySends(seen)).tick();
      new WebhookSchedule(poisoned(), poisonedDispatch()).tick();
    } finally {
      root.detachAppender(captured);
    }

    assertEquals(
        List.of(POISONED_URL, FAKE_SECRET, BODY),
        seen,
        "el ciclo con éxito entregó de verdad la URL, el secreto y el cuerpo al sujeto");
    var logged = everythingLogged(captured);
    assertTrue(logged.contains(ENDPOINT.toString()), "el rastro sí nombra el endpoint");
    assertTrue(logged.contains("succeeded"), "y el estado del intento");
    for (var forbidden :
        List.of("?token=abc", "whsec_", "v1=", "dato de otra cuenta", "example.com"))
      assertFalse(logged.contains(forbidden), "el rastro no debe contener «" + forbidden + "»");
  }

  /** Todo lo registrado: el texto, el mensaje del throwable adjunto y su traza. */
  private static String everythingLogged(
      ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> captured) {
    var all = new StringBuilder();
    for (var event : captured.list) {
      all.append(event.getFormattedMessage()).append('\n');
      for (var thrown = event.getThrowableProxy(); thrown != null; thrown = thrown.getCause()) {
        all.append(thrown.getClassName()).append(' ').append(thrown.getMessage()).append('\n');
        for (var frame : thrown.getStackTraceElementProxyArray())
          all.append(frame.getStackTraceElement()).append('\n');
      }
    }
    return all.toString();
  }

  private static final java.util.UUID ENDPOINT =
      java.util.UUID.fromString("22222222-2222-4222-8222-222222222222");

  /** Un ciclo que revienta con la URL, la firma y el secreto dentro del mensaje de la excepción. */
  private static EnqueueWebhookDeliveriesUseCase poisoned() {
    return WebhookScheduleTest::blowUpWithEverythingForbidden;
  }

  private static DispatchWebhooksUseCase poisonedDispatch() {
    return WebhookScheduleTest::blowUpWithEverythingForbidden;
  }

  private static void blowUpWithEverythingForbidden() {
    throw new IllegalStateException(
        "POST " + POISONED_URL + " " + SIGNATURE + " " + FAKE_SECRET + " " + BODY);
  }

  /** El worker real: reclama, firma, envía y audita con {@link Slf4jWebhookAudit}. */
  private static DispatchWebhooksUseCase dispatchThatReallySends(List<String> seen) {
    var endpoint =
        new com.apptolast.organization.domain.WebhookEndpoint(
            ENDPOINT,
            POISONED_URL,
            "",
            List.of("TaskCreated.v1"),
            "active",
            null,
            null,
            java.time.Instant.EPOCH,
            java.time.Instant.EPOCH);
    var id = java.util.UUID.randomUUID();
    var delivery =
        new com.apptolast.organization.domain.WebhookDelivery(
            id,
            id,
            "TaskCreated.v1",
            "pending",
            0,
            null,
            null,
            null,
            java.time.Instant.EPOCH,
            java.time.Instant.EPOCH,
            java.time.Instant.EPOCH);
    var claimed =
        new com.apptolast.organization.application.ClaimedDelivery(
            endpoint, "owner-a", delivery, BODY, FAKE_SECRET);
    var pending = new java.util.ArrayDeque<>(List.of(claimed));
    com.apptolast.organization.application.WebhookWork work =
        new com.apptolast.organization.application.WebhookWork() {
          @Override
          public java.util.Optional<com.apptolast.organization.application.ClaimedDelivery>
              claimNext(java.time.Instant now) {
            return java.util.Optional.ofNullable(pending.poll());
          }

          @Override
          public void record(
              com.apptolast.organization.application.ClaimedDelivery claim,
              com.apptolast.organization.domain.WebhookDelivery result,
              com.apptolast.organization.domain.WebhookEndpoint disabled) {}
        };
    com.apptolast.organization.application.WebhookSender sender =
        (url, secret, eventId, body) -> {
          seen.add(url);
          seen.add(secret);
          seen.add(body);
          return com.apptolast.organization.domain.WebhookAttempt.http(200, 7);
        };
    return new com.apptolast.organization.application.DispatchWebhooks(
        work,
        sender,
        new com.apptolast.organization.adapter.logging.Slf4jWebhookAudit(),
        alwaysAvailable(),
        java.time.Clock.systemUTC());
  }

  private static com.apptolast.organization.application.WebhookSecrets alwaysAvailable() {
    return new com.apptolast.organization.application.WebhookSecrets() {
      @Override
      public boolean available() {
        return true;
      }

      @Override
      public byte[] encrypt(String ownerId, java.util.UUID endpointId, String secret) {
        throw new UnsupportedOperationException();
      }

      @Override
      public String decrypt(String ownerId, java.util.UUID endpointId, byte[] ciphertext) {
        throw new UnsupportedOperationException();
      }
    };
  }

  /**
   * B9 del panel. El plazo del worker vivía sólo en dos constantes de anotación: PIT no genera
   * mutante alguno sobre ellas y {@code grep -rn fixedDelay backend/src/test} no devolvía nada, así
   * que se podían cambiar a cualquier valor y la suite entera seguía verde. Ahora están medidas.
   *
   * <p>La unidad se afirma junto a los números a propósito: {@code timeUnit = SECONDS} con los
   * mismos 1000 daría un worker mil veces más lento sin tocar una sola cifra.
   *
   * <p>Queda dicho lo que esta prueba <b>no</b> resuelve: el When de @s32 dice «transcurren 1500 ms
   * desde el arranque» y su tercera fila exige 20 entregas tras el primer ciclo y las 5 restantes
   * tras el segundo. Con 1000 de retardo inicial y 1000 entre ciclos, a los 1500 ms sólo ha corrido
   * un tic: el segundo llega a los 2000. El contrato leído al pie de la letra no se cumple, y
   * enmendarlo es del propietario, no de este carril. La pregunta exacta está escrita en
   * progress/decisiones_pendientes.md; hasta que se ratifique, la cláusula temporal de @s32 sigue
   * declarada abierta en progress/mutation_webhooks.md.
   */
  @Test
  void s32_b9_theTickRunsEverySecondAfterOneSecondOfInitialDelay() throws Exception {
    var scheduled =
        WebhookSchedule.class
            .getMethod("tick")
            .getAnnotation(org.springframework.scheduling.annotation.Scheduled.class);

    assertNotNull(scheduled, "sin @Scheduled no hay worker periódico que valga");
    assertEquals(
        1000, scheduled.initialDelay(), "el primer ciclo llega un segundo tras el arranque");
    assertEquals(1000, scheduled.fixedDelay(), "y los siguientes, uno por segundo");
    assertEquals(
        java.util.concurrent.TimeUnit.MILLISECONDS,
        scheduled.timeUnit(),
        "los dos números son milisegundos; en segundos serían mil veces más");
  }
}
