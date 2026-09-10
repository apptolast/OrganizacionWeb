package com.apptolast.organization.application;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.domain.WebhookInvalidException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class CreateWebhookTest {
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-09-08T10:00:00.000000Z"), ZoneOffset.UTC);

  /**
   * Cada host que la producción pidió resolver, en orden de petición. Sin este contador, «no se
   * resuelve DNS» (@s2:26) no lo afirmaba nadie: el resolutor era un método estático mudo y una
   * creación que resolviera antes de validar habría pasado la suite entera en verde.
   */
  private final List<String> resolved = new ArrayList<>();

  private final WebhookDestinationGuard publicOnly =
      new WebhookDestinationGuard(this::recordAndResolve, AddressPolicy.blockingPrivateAddresses());

  private InetAddress[] recordAndResolve(String host) throws UnknownHostException {
    resolved.add(host);
    return resolve(host);
  }

  private static InetAddress[] resolve(String host) throws UnknownHostException {
    return switch (host) {
      case "example.com" -> new InetAddress[] {InetAddress.getByName("203.0.113.5")};
      case "mixed.example" ->
          new InetAddress[] {
            InetAddress.getByName("203.0.113.5"), InetAddress.getByName("10.0.0.5")
          };
      default -> throw new UnknownHostException(host);
    };
  }

  private CreateWebhook create(FakeWebhookEndpoints endpoints, WebhookSecrets secrets) {
    return new CreateWebhook(endpoints, secrets, publicOnly, CLOCK, new SecureRandom());
  }

  @Test
  void s1_creationReturnsOneTimeSecretAndActiveEndpointStampedWithTheClock() {
    var endpoints = new FakeWebhookEndpoints();
    var creation =
        create(endpoints, FakeWebhookSecrets.KEYED)
            .create(
                "owner-a",
                "https://example.com/hooks",
                "  Mi hook  ",
                List.of("TaskStatusChanged.v1", "TaskCreated.v1"));
    var endpoint = creation.endpoint();
    assertEquals("https://example.com/hooks", endpoint.url());
    assertEquals("Mi hook", endpoint.description());
    assertEquals(List.of("TaskCreated.v1", "TaskStatusChanged.v1"), endpoint.eventTypes());
    assertEquals("active", endpoint.status());
    assertNull(endpoint.disabledReason());
    assertNull(endpoint.disabledAt());
    assertEquals(Instant.parse("2026-09-08T10:00:00Z"), endpoint.createdAt());
    assertEquals(endpoint.createdAt(), endpoint.updatedAt());
    assertTrue(creation.secret().matches("whsec_[A-Za-z0-9_-]{43}"), creation.secret());
    assertEquals(List.of(endpoint), endpoints.stored);
    assertFalse(creation.toString().contains("whsec_"), "secret must not leak through toString");
  }

  @Test
  void s1_s8_b6_theStoredCiphertextIsBoundToTheOwnerAndTheEndpoint() {
    var endpoints = new FakeWebhookEndpoints();
    var creation =
        create(endpoints, FakeWebhookSecrets.KEYED)
            .create("owner-a", "https://example.com/hooks", "", List.of("TaskCreated.v1"));
    var stored = endpoints.ciphertexts.getFirst();
    assertEquals(
        creation.secret(),
        FakeWebhookSecrets.KEYED.decrypt("owner-a", creation.endpoint().id(), stored));
    assertThrows(
        IllegalStateException.class,
        () -> FakeWebhookSecrets.KEYED.decrypt("owner-b", creation.endpoint().id(), stored));
  }

  @Test
  void s1_secretsAreUniquePerCreation() {
    var create = create(new FakeWebhookEndpoints(), FakeWebhookSecrets.KEYED);
    var first = create.create("owner-a", "https://example.com/a", null, List.of("TaskCreated.v1"));
    var second = create.create("owner-a", "https://example.com/b", null, List.of("TaskCreated.v1"));
    assertNotEquals(first.secret(), second.secret());
    assertNotEquals(first.endpoint().id(), second.endpoint().id());
  }

  /**
   * @s2 «no se inserta ningún endpoint ni se resuelve DNS». La mitad de la inserción ya estaba
   *     medida; la de la resolución no la afirmaba nadie, y es la que dice que una URL que ni
   *     siquiera es https no llega a provocar una consulta al exterior. El orden importa: la
   *     intención se valida antes de tocar la red.
   */
  @Test
  void s2_anInvalidUrlIsRejectedBeforeResolvingAnyHost() {
    var endpoints = new FakeWebhookEndpoints();
    var create = create(endpoints, FakeWebhookSecrets.KEYED);
    for (var url :
        List.of(
            "http://example.com/hooks",
            "HTTPS://example.com/hooks",
            "https://user:pw@example.com/hooks",
            "https://example.com/hooks#frag",
            "https://example.com:0/hooks"))
      assertThrows(
          WebhookInvalidException.class,
          () -> create.create("owner-a", url, "", List.of("TaskCreated.v1")),
          url);

    assertEquals(List.of(), resolved, "una url inválida se rechaza sin resolver ningún host");
    assertTrue(endpoints.stored.isEmpty());
  }

  @Test
  void s5_blockedOrUnresolvableDestinationsAreRejectedWithoutInserting() {
    var endpoints = new FakeWebhookEndpoints();
    var create = create(endpoints, FakeWebhookSecrets.KEYED);
    for (var url :
        List.of(
            "https://127.0.0.1/h",
            "https://10.1.2.3/h",
            "https://[::1]/h",
            "https://[::ffff:10.0.0.1]/h",
            "https://mixed.example/h")) {
      var error =
          assertThrows(
              WebhookOperationException.class,
              () -> create.create("owner-a", url, "", List.of("TaskCreated.v1")));
      assertEquals(WebhookOperationException.Code.URL_BLOCKED, error.code(), url);
    }
    var unresolvable =
        assertThrows(
            WebhookOperationException.class,
            () ->
                create.create(
                    "owner-a", "https://missing.example/h", "", List.of("TaskCreated.v1")));
    assertEquals(WebhookOperationException.Code.URL_UNRESOLVABLE, unresolvable.code());
    assertTrue(endpoints.stored.isEmpty());
    // @s5: el veredicto sale de una resolución de verdad, una por destino con nombre. Las cuatro
    // direcciones literales no preguntan a nadie: se deciden sobre la propia dirección.
    assertEquals(
        List.of("mixed.example", "missing.example"),
        resolved,
        "sólo los hosts con nombre llegan al resolutor, y cada uno exactamente una vez");
  }

  @Test
  void s34_errorsResolveInTheFixedOrderValuesKeyDestinationThenQuota() {
    var full = new FakeWebhookEndpoints();
    full.limit = 0;
    var disabled = create(full, WebhookSecrets.DISABLED);
    assertThrows(
        WebhookInvalidException.class,
        () -> disabled.create("owner-a", "http://10.0.0.1/h", "", List.of("TaskCreated.v1")));
    assertEquals(
        WebhookOperationException.Code.CONNECTORS_DISABLED,
        assertThrows(
                WebhookOperationException.class,
                () ->
                    disabled.create("owner-a", "https://10.0.0.1/h", "", List.of("TaskCreated.v1")))
            .code());
    var keyed = create(full, FakeWebhookSecrets.KEYED);
    assertEquals(
        WebhookOperationException.Code.URL_BLOCKED,
        assertThrows(
                WebhookOperationException.class,
                () -> keyed.create("owner-a", "https://10.0.0.1/h", "", List.of("TaskCreated.v1")))
            .code());
    assertEquals(
        WebhookOperationException.Code.LIMIT,
        assertThrows(
                WebhookOperationException.class,
                () ->
                    keyed.create("owner-a", "https://example.com/h", "", List.of("TaskCreated.v1")))
            .code());
    assertTrue(full.stored.isEmpty());
  }

  /**
   * Los colaboradores declarados en los constructores de un caso de uso: con qué puede contar para
   * hacer su trabajo, y sobre todo con qué no.
   */
  private static List<Class<?>> collaboratorsOf(Class<?> useCase) {
    return Arrays.stream(useCase.getDeclaredConstructors())
        .<Class<?>>flatMap(constructor -> Arrays.stream(constructor.getParameterTypes()))
        .toList();
  }

  /**
   * @s5 «no se inserta ningún endpoint ni se abre conexión saliente». La mitad de la conexión es
   *     estructural, y la condición 13 del cierre pedía dejarla escrita en vez de implícita: crear
   *     un webhook no recibe {@link WebhookSender}, de modo que lo único que sale de aquí es la
   *     consulta de resolución que mide {@code
   *     s5_blockedOrUnresolvableDestinationsAreRejectedWithoutInserting}.
   *     <p>La segunda aserción es el control, para que la primera no pueda quedarse vacía.
   */
  @Test
  void s5_creatingHasNoSenderToOpenAnOutgoingConnectionWith() {
    assertFalse(
        collaboratorsOf(CreateWebhook.class).contains(WebhookSender.class),
        "CreateWebhook no recibe con qué enviar");
    assertTrue(
        collaboratorsOf(DispatchWebhooks.class).contains(WebhookSender.class),
        "control: el único que sí envía es el worker");
  }
}
