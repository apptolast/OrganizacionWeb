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

  private static JdkWebhookSender sender(AddressPolicy policy) {
    return new JdkWebhookSender(CLOCK, policy, InetAddress::getAllByName);
  }

  /** The same sender, but resolving names through the fabricated zone of these tests. */
  private static JdkWebhookSender senderInLoopbackZone() {
    return new JdkWebhookSender(CLOCK, EVERY_ADDRESS_ALLOWED, LOOPBACK_ZONE);
  }

  /** The same sender with the two deadlines shortened, so the timeout row is affordable. */
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
   * resolver instead of using the address already validated, the send would die with DNS.
   */
  private static final String PINNED_NAME = "receptor.webhooks.invalid";

  /** The zone of these tests: the only name it knows answers the loopback. */
  private static final WebhookDestinationGuard.HostResolver LOOPBACK_ZONE =
      host -> {
        if (!PINNED_NAME.equals(host)) throw new UnknownHostException(host);
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
    var keys = KeyStore.getInstance("PKCS12");
    try (var stream =
        JdkWebhookSenderTest.class.getResourceAsStream("/webhooks/untrusted-receiver.p12")) {
      keys.load(stream, KEYSTORE_PASSWORD);
    }
    var managers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    managers.init(keys, KEYSTORE_PASSWORD);
    var context = SSLContext.getInstance("TLS");
    context.init(managers.getKeyManagers(), null, null);
    var server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.setHttpsConfigurator(new HttpsConfigurator(context));
    return listen("https", server, handler);
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
      assertTrue(
          outcome.latencyMs() >= TEST_EXCHANGE_DEADLINE.toMillis(),
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

  @Test
  void s25_b3_anAddressBlockedAtSendTimeStopsTheRequestBeforeConnecting() throws Exception {
    try (var receiver = start(exchange -> respond(exchange, 200, new byte[0]))) {
      var outcome = sender(NO_ADDRESS_ALLOWED).send(receiver.url(), SECRET, EVENT, BODY);

      assertEquals("BLOCKED_ADDRESS", outcome.errorClass());
      assertNull(outcome.httpStatus());
      assertTrue(receiver.received().isEmpty());
    }
  }
}
