package com.apptolast.organization.adapter.webhook;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.application.AddressPolicy;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
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

  private static JdkWebhookSender sender(AddressPolicy policy) {
    return new JdkWebhookSender(CLOCK, policy, InetAddress::getAllByName);
  }

  private record Receiver(HttpServer server, List<HttpExchange> received) implements AutoCloseable {
    String url() {
      return "http://127.0.0.1:" + server.getAddress().getPort() + "/hooks";
    }

    @Override
    public void close() {
      server.stop(0);
    }
  }

  private static Receiver start(Consumer<HttpExchange> handler) throws IOException {
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    var received = new ArrayList<HttpExchange>();
    server.createContext(
        "/hooks",
        exchange -> {
          received.add(exchange);
          handler.accept(exchange);
        });
    server.start();
    return new Receiver(server, received);
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
  void s25_b3_anAddressBlockedAtSendTimeStopsTheRequestBeforeConnecting() throws Exception {
    try (var receiver = start(exchange -> respond(exchange, 200, new byte[0]))) {
      var outcome = sender(NO_ADDRESS_ALLOWED).send(receiver.url(), SECRET, EVENT, BODY);

      assertEquals("BLOCKED_ADDRESS", outcome.errorClass());
      assertNull(outcome.httpStatus());
      assertTrue(receiver.received().isEmpty());
    }
  }
}
