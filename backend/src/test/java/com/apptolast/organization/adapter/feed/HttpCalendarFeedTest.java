package com.apptolast.organization.adapter.feed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apptolast.organization.application.FeedFetch;
import com.apptolast.organization.domain.FeedError;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @s12 y @s13: la descarga usa Accept text/calendar, no sigue redirecciones, corta a 1 MiB y solo
 *     acepta 200 con un tipo text/*.
 */
class HttpCalendarFeedTest {
  static final String VALID_ICS =
      "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nBEGIN:VEVENT\r\nUID:u1@example\r\n"
          + "DTSTART:20300108T090000Z\r\nDTEND:20300108T100000Z\r\nSUMMARY:Reunión\r\n"
          + "END:VEVENT\r\nEND:VCALENDAR\r\n";

  record Received(String method, String uri, String accept, String cookie, String authorization) {}

  HttpServer server;
  final List<Received> received = new CopyOnWriteArrayList<>();
  volatile HttpHandler handler;

  @BeforeEach
  void start() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.setExecutor(Executors.newFixedThreadPool(2));
    server.createContext(
        "/",
        exchange -> {
          received.add(
              new Received(
                  exchange.getRequestMethod(),
                  exchange.getRequestURI().toString(),
                  exchange.getRequestHeaders().getFirst("Accept"),
                  exchange.getRequestHeaders().getFirst("Cookie"),
                  exchange.getRequestHeaders().getFirst("Authorization")));
          handler.handle(exchange);
        });
    server.start();
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  String url(String path) {
    return "http://127.0.0.1:" + server.getAddress().getPort() + path;
  }

  HttpCalendarFeed feed() {
    return new HttpCalendarFeed(HttpCalendarFeed.TIMEOUT);
  }

  static void reply(HttpExchange exchange, int status, String contentType, byte[] body)
      throws IOException {
    if (contentType != null) exchange.getResponseHeaders().add("Content-Type", contentType);
    exchange.sendResponseHeaders(status, body.length);
    try (var out = exchange.getResponseBody()) {
      out.write(body);
    }
  }

  static void reply(HttpExchange exchange, int status, String contentType, String body)
      throws IOException {
    reply(exchange, status, contentType, body.getBytes(StandardCharsets.UTF_8));
  }

  static FeedError codeOf(FeedFetch fetch) {
    return assertInstanceOf(FeedFetch.Failed.class, fetch).code();
  }

  static String textOf(FeedFetch fetch) {
    return assertInstanceOf(FeedFetch.Downloaded.class, fetch).text();
  }

  @Test
  void s13_sendsExactlyOneGetWithAcceptTextCalendarAndNoCredentials() {
    handler = exchange -> reply(exchange, 200, "text/calendar", VALID_ICS);
    var fetch = feed().fetch(url("/cal.ics?tok=WXYZ"));
    assertEquals(VALID_ICS, textOf(fetch));
    assertEquals(1, received.size());
    var request = received.getFirst();
    assertEquals("GET", request.method());
    assertEquals("/cal.ics?tok=WXYZ", request.uri());
    assertEquals("text/calendar", request.accept());
    assertNull(request.cookie(), "no se envía cookie de sesión");
    assertNull(request.authorization(), "no se envía Authorization");
  }

  @ParameterizedTest
  @CsvSource({"301", "302", "303", "307", "308"})
  void s13_doesNotFollowRedirectsAndReportsAnHttpError(int status) {
    handler =
        exchange -> {
          exchange.getResponseHeaders().add("Location", url("/otro.ics"));
          reply(exchange, status, "text/calendar", "");
        };
    assertEquals(FeedError.FEED_HTTP_ERROR, codeOf(feed().fetch(url("/cal.ics"))));
    assertEquals(1, received.size(), "el destino de la redirección no recibe ninguna petición");
  }

  @ParameterizedTest
  @CsvSource({"204", "400", "401", "403", "404", "429", "500", "503"})
  void s12_anyStatusOtherThanTwoHundredIsAnHttpError(int status) {
    handler = exchange -> reply(exchange, status, "text/calendar", "");
    assertEquals(FeedError.FEED_HTTP_ERROR, codeOf(feed().fetch(url("/cal.ics"))));
  }

  @Test
  void s12_aRefusedConnectionIsUnreachable() throws IOException {
    var closed = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    int port = closed.getAddress().getPort();
    closed.stop(0);
    assertEquals(
        FeedError.FEED_UNREACHABLE, codeOf(feed().fetch("http://127.0.0.1:" + port + "/cal.ics")));
  }

  @Test
  void s12_aServerThatDoesNotSendHeadersInTimeIsUnreachable() throws InterruptedException {
    var released = new CountDownLatch(1);
    handler =
        exchange -> {
          try {
            released.await(10, TimeUnit.SECONDS);
          } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
          }
          reply(exchange, 200, "text/calendar", VALID_ICS);
        };
    var slow = new HttpCalendarFeed(Duration.ofMillis(300));
    assertEquals(FeedError.FEED_UNREACHABLE, codeOf(slow.fetch(url("/cal.ics"))));
    released.countDown();
  }

  /**
   * @s12: un proveedor que envía las cabeceras al instante y luego gotea el cuerpo sin cerrarlo
   *     nunca. El plazo es del intercambio completo, no de las cabeceras: si sólo cubriera las
   *     cabeceras esta prueba no terminaría jamás, y por eso lleva {@link Timeout}.
   */
  @Test
  @Timeout(15)
  void s12_aBodyThatDripsForeverIsUnreachable() throws InterruptedException {
    var stopped = new CountDownLatch(1);
    handler =
        exchange -> {
          exchange.getResponseHeaders().add("Content-Type", "text/calendar");
          exchange.sendResponseHeaders(200, 0);
          try (var out = exchange.getResponseBody()) {
            while (!stopped.await(50, TimeUnit.MILLISECONDS)) {
              out.write('X');
              out.flush();
            }
          } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
          } catch (IOException cut) {
            // El cliente cortó: es justo lo que la prueba quiere provocar.
          }
        };
    var slow = new HttpCalendarFeed(Duration.ofMillis(300));
    long started = System.nanoTime();
    FeedError code;
    try {
      code = codeOf(slow.fetch(url("/cal.ics")));
    } finally {
      stopped.countDown();
    }
    long elapsed = Duration.ofNanos(System.nanoTime() - started).toMillis();
    assertEquals(FeedError.FEED_UNREACHABLE, code);
    assertTrue(elapsed < 5_000, "la lectura del cuerpo tardó " + elapsed + " ms en cortarse");
  }

  @Test
  void s13_theProductionTimeoutIsFiveSeconds() {
    assertEquals(Duration.ofSeconds(5), HttpCalendarFeed.TIMEOUT);
  }

  @Test
  void s13_readsABodyOfExactlyOneMebibyte() {
    handler = exchange -> reply(exchange, 200, "text/calendar", padded(HttpCalendarFeed.LIMIT));
    var text = textOf(feed().fetch(url("/cal.ics")));
    assertEquals(HttpCalendarFeed.LIMIT, text.length());
    assertTrue(text.startsWith("BEGIN:VCALENDAR"));
  }

  @Test
  void s12_aBodyOfOneMebibytePlusOneByteIsTooLarge() {
    handler = exchange -> reply(exchange, 200, "text/calendar", padded(HttpCalendarFeed.LIMIT + 1));
    assertEquals(FeedError.FEED_TOO_LARGE, codeOf(feed().fetch(url("/cal.ics"))));
  }

  /** Un cuerpo anunciado que el proveedor nunca termina de emitir. */
  static final long ENDLESS_LENGTH = 4L * HttpCalendarFeed.LIMIT;

  /** Lo único que el servidor llega a emitir: el límite más el margen para cruzarlo. */
  static final long EMITTED_BEFORE_GOING_SILENT = HttpCalendarFeed.LIMIT + 64L * 1024;

  static final int SERVER_CHUNK = 8 * 1024;

  /** Margen para que un cliente que no corta delate su espera sin colgar la suite. */
  static final int SILENCE_BEFORE_HANGING_UP_SECONDS = 5;

  /**
   * El servidor anuncia {@link #ENDLESS_LENGTH} pero solo emite {@link
   * #EMITTED_BEFORE_GOING_SILENT} y enmudece: el resto del cuerpo no llega nunca. Así, la única
   * forma de obtener FEED_TOO_LARGE es dictar el veredicto habiendo consumido a lo sumo ese
   * prefijo; quien agote el cuerpo se queda esperando los MiB que faltan y acaba en
   * FEED_UNREACHABLE cuando el servidor cuelga. El oráculo no mide cuánto alcanzó a escribir el
   * servidor —eso depende de los búferes del socket y de la carrera entre ambos extremos— sino que
   * acota por construcción cuánto puede llegar a leer el cliente.
   *
   * <p>El colgado final es imprescindible: {@code HttpRequest.timeout} no cubre la lectura del
   * cuerpo con {@code BodyHandlers.ofInputStream}, de modo que sin él un cliente sin corte por
   * tamaño se quedaría bloqueado para siempre en vez de fallar.
   */
  @Test
  void s13_abortsAnEndlessBodyAfterReadingJustPastTheLimit() {
    var clientHasAborted = new CountDownLatch(1);
    handler =
        exchange -> {
          exchange.getResponseHeaders().add("Content-Type", "text/calendar");
          exchange.sendResponseHeaders(200, ENDLESS_LENGTH);
          var chunk = new byte[SERVER_CHUNK];
          java.util.Arrays.fill(chunk, (byte) 'x');
          try {
            var out = exchange.getResponseBody();
            for (long emitted = 0; emitted < EMITTED_BEFORE_GOING_SILENT; emitted += SERVER_CHUNK) {
              out.write(chunk);
              out.flush();
            }
            clientHasAborted.await(SILENCE_BEFORE_HANGING_UP_SECONDS, TimeUnit.SECONDS);
          } catch (IOException aborted) {
            // el cliente cerró la conexión: es exactamente lo que se espera
          } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
          } finally {
            // Sin cerrar antes el flujo: así el cuerpo queda incompleto y se corta la conexión.
            exchange.close();
          }
        };
    try {
      assertEquals(
          FeedError.FEED_TOO_LARGE,
          codeOf(feed().fetch(url("/cal.ics"))),
          "el servidor enmudece tras "
              + EMITTED_BEFORE_GOING_SILENT
              + " bytes de los "
              + ENDLESS_LENGTH
              + " anunciados: quien no corte por tamaño se queda esperando el resto del cuerpo");
    } finally {
      clientHasAborted.countDown();
    }
  }

  @ParameterizedTest
  @CsvSource({
    "text/calendar",
    "text/calendar; charset=utf-8",
    "TEXT/CALENDAR",
    "text/plain",
    "text/html"
  })
  void s13_acceptsAnyTextualContentType(String contentType) {
    handler = exchange -> reply(exchange, 200, contentType, VALID_ICS);
    assertEquals(VALID_ICS, textOf(feed().fetch(url("/cal.ics"))));
  }

  @ParameterizedTest
  @CsvSource({"application/json", "application/octet-stream", "image/png"})
  void s12_rejectsANonTextualContentType(String contentType) {
    handler = exchange -> reply(exchange, 200, contentType, VALID_ICS);
    assertEquals(FeedError.FEED_UNSUPPORTED_TYPE, codeOf(feed().fetch(url("/cal.ics"))));
  }

  @Test
  void s12_anAbsentContentTypeIsUnsupported() {
    handler = exchange -> reply(exchange, 200, null, VALID_ICS);
    assertEquals(FeedError.FEED_UNSUPPORTED_TYPE, codeOf(feed().fetch(url("/cal.ics"))));
  }

  @Test
  void s14_decodesTheBodyAsUtfEight() {
    handler = exchange -> reply(exchange, 200, "text/calendar; charset=utf-8", VALID_ICS);
    assertTrue(textOf(feed().fetch(url("/cal.ics"))).contains("Reunión"));
  }

  @Test
  void s12_anUnparseableUrlIsUnreachableInsteadOfAnException() {
    assertEquals(FeedError.FEED_UNREACHABLE, codeOf(feed().fetch("https://")));
  }

  static byte[] padded(int length) {
    var body = new StringBuilder("BEGIN:VCALENDAR\r\nVERSION:2.0\r\n");
    while (body.length() < length) body.append('x');
    return body.substring(0, length).getBytes(StandardCharsets.ISO_8859_1);
  }
}
