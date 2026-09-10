package com.apptolast.organization.adapter.webhook;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.application.AddressPolicy;
import com.apptolast.organization.application.WebhookDestinationGuard;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class JdkWebhookSenderTest {
  private static final Instant T = Instant.parse("2026-09-08T10:00:00Z");
  private static final Clock CLOCK = Clock.fixed(T, ZoneOffset.UTC);
  private static final String SECRET = "whsec_AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8";
  private static final String EVENT = "11111111-1111-4111-8111-111111111111";
  private static final String BODY = "{\"eventId\":\"" + EVENT + "\"}";

  /**
   * Nothing is blocked, so the loopback receiver of these tests is reachable. Mind the direction:
   * the sender now takes an {@link AddressPolicy}, whose {@code allows} answers PERMITTED, the
   * opposite of the {@code Predicate<InetAddress> blocked} it took before the policies of features
   * 25 and 28 were unified.
   */
  private static final AddressPolicy EVERY_ADDRESS_ALLOWED = address -> true;

  /** The whole internet is off limits: no destination survives the guard. */
  private static final AddressPolicy NO_ADDRESS_ALLOWED = address -> false;

  /** Short enough that a stalling receiver costs the suite a fraction of a second, not ten. */
  private static final Duration TEST_CONNECT_DEADLINE = Duration.ofSeconds(2);

  private static final Duration TEST_EXCHANGE_DEADLINE = Duration.ofMillis(300);

  private static final char[] KEYSTORE_PASSWORD = "changeit".toCharArray();

  /** Shared with the external calendar of feature 28: one anchoring, one fixture. */
  private static final String NAMED_KEYSTORE = "/tls/anchored-receiver.p12";

  private static final String NAMED_ALIAS = "destino";

  private static final String UNTRUSTED_KEYSTORE = "/webhooks/untrusted-receiver.p12";

  /**
   * A handshake plus a round trip on the loopback needs more than the 300 ms of the timeout row.
   */
  private static final Duration TLS_EXCHANGE_DEADLINE = Duration.ofSeconds(5);

  private static JdkWebhookSender sender(AddressPolicy policy) {
    return new JdkWebhookSender(CLOCK, policy, InetAddress::getAllByName);
  }

  /** The same sender, but resolving names through the fabricated zone of these tests. */
  private static JdkWebhookSender senderInLoopbackZone() {
    return new JdkWebhookSender(CLOCK, EVERY_ADDRESS_ALLOWED, LOOPBACK_ZONE);
  }

  /** The sender of the fabricated zone, trusting the fixture certificate and nothing else. */
  private static JdkWebhookSender senderTrustingTheFixture() throws Exception {
    return new JdkWebhookSender(
        CLOCK,
        EVERY_ADDRESS_ALLOWED,
        LOOPBACK_ZONE,
        TEST_CONNECT_DEADLINE,
        TLS_EXCHANGE_DEADLINE,
        trustingOnlyTheFixture());
  }

  /** The same sender with the two deadlines shortened, so the timeout row is affordable. */
  /**
   * El mismo emisor, con el cronómetro bajo control de la prueba. Sin esto, la cláusula «latencyMs
   * medido con el reloj inyectado» de features/webhooks.feature:320 no se puede comprobar: una
   * latencia real no es reproducible, así que el oráculo tendría que conformarse con «es un entero
   * no negativo», que lo cumple cualquier cosa, incluido un cero constante.
   */
  private static JdkWebhookSender senderTicking(long... nanos) {
    var pasos = new java.util.concurrent.atomic.AtomicInteger();
    return new JdkWebhookSender(
        CLOCK,
        EVERY_ADDRESS_ALLOWED,
        InetAddress::getAllByName,
        TEST_CONNECT_DEADLINE,
        TEST_EXCHANGE_DEADLINE,
        null,
        () -> nanos[Math.min(pasos.getAndIncrement(), nanos.length - 1)]);
  }

  private static JdkWebhookSender senderWithShortDeadlines() {
    return new JdkWebhookSender(
        CLOCK,
        EVERY_ADDRESS_ALLOWED,
        InetAddress::getAllByName,
        TEST_CONNECT_DEADLINE,
        TEST_EXCHANGE_DEADLINE);
  }

  /**
   * A name that no real DNS can answer —{@code .invalid} is reserved by RFC 2606— mapped to the
   * loopback by {@link #LOOPBACK_ZONE}. If the sender ever resolved it again through the system
   * resolver instead of using the address already validated, the send would die with DNS. It is
   * also the only name the fixture certificate is valid for.
   */
  private static final String PINNED_NAME = "destino.anclado.invalid";

  /** Another name of the same zone, on the same address, that no certificate here vouches for. */
  private static final String IMPOSTOR_NAME = "impostor.anclado.invalid";

  /** The zone of these tests: the two names it knows answer the loopback, nothing else resolves. */
  private static final WebhookDestinationGuard.HostResolver LOOPBACK_ZONE =
      host -> {
        if (!PINNED_NAME.equals(host) && !IMPOSTOR_NAME.equals(host))
          throw new UnknownHostException(host);
        return new InetAddress[] {InetAddress.getByName("127.0.0.1")};
      };

  private record Receiver(String scheme, HttpServer server, List<HttpExchange> received)
      implements AutoCloseable {
    String url() {
      return urlFor("127.0.0.1");
    }

    /** The same receiver reached by a name instead of by its literal address. */
    String urlFor(String host) {
      return scheme + "://" + host + ":" + server.getAddress().getPort() + "/hooks";
    }

    @Override
    public void close() {
      server.stop(0);
    }
  }

  private static Receiver start(Consumer<HttpExchange> handler) throws IOException {
    return listen("http", HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0), handler);
  }

  /**
   * A receiver over TLS whose certificate is self-signed, so no default trust anchor vouches for
   * it. The keystore lives in src/test/resources and never leaves the tests.
   */
  private static Receiver startUntrustedTls(Consumer<HttpExchange> handler) throws Exception {
    return listen("https", httpsServerPresenting(UNTRUSTED_KEYSTORE), handler);
  }

  /**
   * A receiver over TLS whose certificate is valid for {@link #PINNED_NAME} and <b>for no address
   * at all</b>: it carries a dNSName alternative name and no iPAddress one. That is what makes it
   * an oracle. Reached through the anchored path the connection goes to 127.0.0.1, so the
   * certificate can only be accepted if the name survives the anchoring —in the server name
   * indication and in the identity check—; if it did not, the JDK would verify against the address,
   * find nothing to match, and fail the handshake.
   */
  private static Receiver startTlsValidForTheName(Consumer<HttpExchange> handler) throws Exception {
    return listen("https", httpsServerPresenting(NAMED_KEYSTORE), handler);
  }

  private static HttpsServer httpsServerPresenting(String keystore) throws Exception {
    var managers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    managers.init(load(keystore), KEYSTORE_PASSWORD);
    var context = SSLContext.getInstance("TLS");
    context.init(managers.getKeyManagers(), null, null);
    var server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.setHttpsConfigurator(new HttpsConfigurator(context));
    return server;
  }

  /**
   * The trust of the tests: the certificate of the fixture, and nothing else. A test cannot add an
   * authority to the JDK's own store, so the sender takes this context through its test-only
   * constructor. Note what it does NOT do: it trusts one certificate, it does not turn verification
   * off, so an untrusted receiver keeps being rejected.
   */
  private static SSLContext trustingOnlyTheFixture() throws Exception {
    var trusted = KeyStore.getInstance("PKCS12");
    trusted.load(null, null);
    trusted.setCertificateEntry("fixture", load(NAMED_KEYSTORE).getCertificate(NAMED_ALIAS));
    var managers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    managers.init(trusted);
    var context = SSLContext.getInstance("TLS");
    context.init(null, managers.getTrustManagers(), null);
    return context;
  }

  private static KeyStore load(String resource) throws Exception {
    var keys = KeyStore.getInstance("PKCS12");
    try (var stream = JdkWebhookSenderTest.class.getResourceAsStream(resource)) {
      keys.load(stream, KEYSTORE_PASSWORD);
    }
    return keys;
  }

  private static Receiver listen(String scheme, HttpServer server, Consumer<HttpExchange> handler) {
    var received = new ArrayList<HttpExchange>();
    server.createContext(
        "/hooks",
        exchange -> {
          received.add(exchange);
          handler.accept(exchange);
        });
    server.start();
    return new Receiver(scheme, server, received);
  }

  private static void await(CountDownLatch latch) {
    try {
      latch.await(10, TimeUnit.SECONDS);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
    }
  }

  private static void respond(HttpExchange exchange, int code, byte[] body) {
    try {
      exchange.sendResponseHeaders(code, body.length == 0 ? -1 : body.length);
      if (body.length > 0) exchange.getResponseBody().write(body);
      exchange.close();
    } catch (IOException error) {
      throw new IllegalStateException(error);
    }
  }

  @Test
  void s16_theRequestCarriesTheAgreedHeadersAndNoCredential() throws Exception {
    var seen = new AtomicReference<HttpExchange>();
    try (var receiver =
        start(
            exchange -> {
              seen.set(exchange);
              respond(exchange, 200, new byte[0]);
            })) {
      var outcome = sender(EVERY_ADDRESS_ALLOWED).send(receiver.url(), SECRET, EVENT, BODY);

      var headers = seen.get().getRequestHeaders();
      assertEquals("POST", seen.get().getRequestMethod());
      assertEquals("application/json; charset=utf-8", headers.getFirst("Content-Type"));
      assertEquals("OrganizationWeb-Webhooks/1", headers.getFirst("User-Agent"));
      assertEquals(EVENT, headers.getFirst("X-OrganizationWeb-Event-Id"));
      assertNull(headers.getFirst("Cookie"));
      assertNull(headers.getFirst("Authorization"));
      assertTrue(outcome.succeeded());
    }
  }

  @Test
  void s15_theSignatureCoversTheExactBodyAndTheSendingInstant() throws Exception {
    var seen = new AtomicReference<HttpExchange>();
    var body = new AtomicReference<byte[]>();
    try (var receiver =
        start(
            exchange -> {
              seen.set(exchange);
              try {
                body.set(exchange.getRequestBody().readAllBytes());
              } catch (IOException error) {
                throw new IllegalStateException(error);
              }
              respond(exchange, 200, new byte[0]);
            })) {
      sender(EVERY_ADDRESS_ALLOWED).send(receiver.url(), SECRET, EVENT, BODY);

      assertArrayEquals(BODY.getBytes(StandardCharsets.UTF_8), body.get());
      assertEquals(
          WebhookSignature.header(
              SECRET, T.getEpochSecond(), BODY.getBytes(StandardCharsets.UTF_8)),
          seen.get().getRequestHeaders().getFirst("X-OrganizationWeb-Signature"));
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {200, 204, 299})
  void s26_anySuccessWithinTheDeadlineSucceedsAndDiscardsTheBody(int code) throws Exception {
    var payload = "x".repeat(1024).getBytes(StandardCharsets.UTF_8);
    try (var receiver =
        start(exchange -> respond(exchange, code, code == 204 ? new byte[0] : payload))) {
      var outcome = sender(EVERY_ADDRESS_ALLOWED).send(receiver.url(), SECRET, EVENT, BODY);

      assertTrue(outcome.succeeded());
      assertEquals(code, outcome.httpStatus());
      assertNull(outcome.errorClass());
      assertTrue(outcome.latencyMs() >= 0);
      assertFalse(outcome.toString().contains("x".repeat(64)));
    }
  }

  @ParameterizedTest
  @CsvSource({"404,HTTP_ERROR", "500,HTTP_ERROR"})
  void s25_anErrorResponseIsClassifiedWithItsCode(int code, String expected) throws Exception {
    try (var receiver = start(exchange -> respond(exchange, code, new byte[0]))) {
      var outcome = sender(EVERY_ADDRESS_ALLOWED).send(receiver.url(), SECRET, EVENT, BODY);

      assertEquals(expected, outcome.errorClass());
      assertEquals(code, outcome.httpStatus());
    }
  }

  @Test
  void s25_aRedirectIsNeverFollowedAndTheTargetIsNeverCalled() throws Exception {
    var followed = new ArrayList<HttpExchange>();
    try (var target =
        start(
            exchange -> {
              followed.add(exchange);
              respond(exchange, 200, new byte[0]);
            })) {
      try (var receiver =
          start(
              exchange -> {
                exchange.getResponseHeaders().add("Location", target.url());
                respond(exchange, 302, new byte[0]);
              })) {
        var outcome = sender(EVERY_ADDRESS_ALLOWED).send(receiver.url(), SECRET, EVENT, BODY);

        assertEquals("REDIRECT", outcome.errorClass());
        assertEquals(302, outcome.httpStatus());
      }
      assertTrue(followed.isEmpty());
    }
  }

  @Test
  void s25_aClosedPortIsAConnectionFailureWithoutHttpStatus() throws Exception {
    var receiver = start(exchange -> respond(exchange, 200, new byte[0]));
    var url = receiver.url();
    receiver.close();

    var outcome = sender(EVERY_ADDRESS_ALLOWED).send(url, SECRET, EVENT, BODY);

    assertEquals("CONNECTION", outcome.errorClass());
    assertNull(outcome.httpStatus());
  }

  @Test
  void s25_aHostThatDoesNotResolveIsADnsFailure() {
    var outcome =
        sender(EVERY_ADDRESS_ALLOWED).send("http://no-existe.invalid/hooks", SECRET, EVENT, BODY);

    assertEquals("DNS", outcome.errorClass());
    assertNull(outcome.httpStatus());
  }

  @Test
  void b1_theProductionDeadlinesAreFiveSecondsToConnectAndTenForTheWholeExchange() {
    var production = sender(EVERY_ADDRESS_ALLOWED);

    assertEquals(Duration.ofSeconds(5), production.connectDeadline());
    assertEquals(Duration.ofSeconds(10), production.exchangeDeadline());
  }

  @Test
  void s25_aReceiverThatAcceptsTheConnectionAndNeverAnswersIsATimeout() throws Exception {
    var accepted = new CountDownLatch(1);
    var release = new CountDownLatch(1);
    try (var receiver =
        start(
            exchange -> {
              accepted.countDown();
              await(release);
              respond(exchange, 200, new byte[0]);
            })) {
      var outcome = senderWithShortDeadlines().send(receiver.url(), SECRET, EVENT, BODY);
      release.countDown();

      assertTrue(accepted.await(2, TimeUnit.SECONDS), "the receiver did accept the connection");
      assertEquals("TIMEOUT", outcome.errorClass());
      assertNull(outcome.httpStatus());
      assertFalse(outcome.succeeded());
      // El margen de 5 ms no afloja el oraculo, corrige una medicion: el temporizador
      // del JDK puede disparar el plazo alrededor de un milisegundo antes, y el
      // producto trunca otro al pasar a milisegundos. Sin el, la comparacion con 300
      // recibia 299 bajo carga y caia; con el sigue siendo imposible pasar sin haber
      // esperado el plazo, que es lo que la fila del contrato afirma. El diseno que no
      // dependeria del reloj de la maquina es inyectarle el reloj al emisor, y queda
      // anotado como deuda en progress/tdd_webhooks_cierre_dictamen.md.
      assertTrue(
          outcome.latencyMs() >= TEST_EXCHANGE_DEADLINE.toMillis() - 5,
          "the exchange deadline elapsed before giving up, got " + outcome.latencyMs() + " ms");
    }
  }

  @Test
  void s25_aReceiverWithAnUntrustedCertificateIsATlsFailure() throws Exception {
    try (var receiver = startUntrustedTls(exchange -> respond(exchange, 200, new byte[0]))) {
      var outcome = sender(EVERY_ADDRESS_ALLOWED).send(receiver.url(), SECRET, EVENT, BODY);

      assertEquals("TLS", outcome.errorClass());
      assertNull(outcome.httpStatus());
      assertFalse(outcome.succeeded());
      assertTrue(receiver.received().isEmpty(), "the handshake failed before any request arrived");
    }
  }

  /**
   * Amendment B3: the request must travel to the address already validated, not to the name again.
   * The proof does not need a second answer from DNS: it is enough that the name has no answer at
   * all outside the fabricated zone. If the client resolved on its own, the send would fail with
   * DNS instead of reaching the receiver.
   */
  @Test
  void s25_b3_theRequestTravelsToTheValidatedAddressInsteadOfResolvingTheNameAgain()
      throws Exception {
    var seen = new AtomicReference<HttpExchange>();
    try (var receiver =
        start(
            exchange -> {
              seen.set(exchange);
              respond(exchange, 200, new byte[0]);
            })) {
      var outcome = senderInLoopbackZone().send(receiver.urlFor(PINNED_NAME), SECRET, EVENT, BODY);

      assertTrue(
          outcome.succeeded(),
          "the send reached the validated address, got errorClass " + outcome.errorClass());
      assertEquals(200, outcome.httpStatus());
      assertEquals(1, receiver.received().size());
    }
  }

  /** Anchoring must not lose the name: the receiver still has to see it in Host. */
  @Test
  void s25_b3_theOriginalNameStillTravelsInTheHostHeader() throws Exception {
    var seen = new AtomicReference<HttpExchange>();
    try (var receiver =
        start(
            exchange -> {
              seen.set(exchange);
              respond(exchange, 200, new byte[0]);
            })) {
      senderInLoopbackZone().send(receiver.urlFor(PINNED_NAME), SECRET, EVENT, BODY);

      assertEquals(
          PINNED_NAME + ":" + receiver.server().getAddress().getPort(),
          seen.get().getRequestHeaders().getFirst("Host"));
    }
  }

  /**
   * The point anchoring can break, and the reason it was revoked once: connecting to an address
   * usually ruins certificate verification. It does not have to. Here the certificate is valid for
   * the name and for no address, the connection goes to the address, and it is accepted —which can
   * only happen if the name travelled in the server name indication and the identity check used it.
   */
  @Test
  void s25_b3_aCertificateValidForTheNameIsAcceptedAlthoughTheConnectionGoesToTheAddress()
      throws Exception {
    try (var receiver = startTlsValidForTheName(exchange -> respond(exchange, 200, new byte[0]))) {
      var outcome =
          senderTrustingTheFixture().send(receiver.urlFor(PINNED_NAME), SECRET, EVENT, BODY);

      assertTrue(
          outcome.succeeded(),
          "the handshake verified the certificate by name, got errorClass " + outcome.errorClass());
      assertEquals(200, outcome.httpStatus());
      assertEquals(1, receiver.received().size());
    }
  }

  /**
   * The other half of the same proof: verification by name is not merely present, it still says no.
   * The very certificate the previous test accepts is rejected when the destination is a different
   * name, so what buys the acceptance is the match, not a check that got turned off.
   */
  @Test
  void s25_b3_theSameCertificateIsRejectedWhenTheNameAskedForIsAnother() throws Exception {
    try (var receiver = startTlsValidForTheName(exchange -> respond(exchange, 200, new byte[0]))) {
      var outcome =
          senderTrustingTheFixture().send(receiver.urlFor(IMPOSTOR_NAME), SECRET, EVENT, BODY);

      assertEquals("TLS", outcome.errorClass());
      assertNull(outcome.httpStatus());
      assertTrue(receiver.received().isEmpty(), "the handshake failed before any request arrived");
    }
  }

  /**
   * And the third: anchoring did not weaken the trust chain either. Same anchored path, same
   * fabricated zone, a receiver nobody vouches for.
   */
  @Test
  void s25_b3_anUntrustedCertificateIsStillRejectedOnTheAnchoredPath() throws Exception {
    try (var receiver = startUntrustedTls(exchange -> respond(exchange, 200, new byte[0]))) {
      var outcome =
          senderTrustingTheFixture().send(receiver.urlFor(PINNED_NAME), SECRET, EVENT, BODY);

      assertEquals("TLS", outcome.errorClass());
      assertNull(outcome.httpStatus());
      assertTrue(receiver.received().isEmpty(), "the handshake failed before any request arrived");
    }
  }

  @Test
  void s25_b3_anAddressBlockedAtSendTimeStopsTheRequestBeforeConnecting() throws Exception {
    try (var receiver = start(exchange -> respond(exchange, 200, new byte[0]))) {
      var outcome = sender(NO_ADDRESS_ALLOWED).send(receiver.url(), SECRET, EVENT, BODY);

      assertEquals("BLOCKED_ADDRESS", outcome.errorClass());
      assertNull(outcome.httpStatus());
      assertTrue(receiver.received().isEmpty());
    }
  }

  @Test
  void s25_latencyMsComesFromTheInjectedTickerAndNotFromTheAmbientClock() throws Exception {
    // 1 ms de arranque y 1,25 s al terminar: 1250 ms exactos, imposibles de obtener por
    // casualidad contra un receptor local que responde en microsegundos.
    try (var receiver = start(exchange -> respond(exchange, 200, new byte[0]))) {
      var outcome =
          senderTicking(1_000_000L, 1_251_000_000L).send(receiver.url(), SECRET, EVENT, BODY);

      assertEquals(200, outcome.httpStatus());
      assertEquals(1250, outcome.latencyMs(), "la latencia no sale del cronómetro inyectado");
    }
  }
}
