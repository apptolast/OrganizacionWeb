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
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
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

  record Received(
      String method, String uri, String accept, String cookie, String authorization, String host) {}

  HttpServer server;
  final List<Received> received = new CopyOnWriteArrayList<>();
  volatile HttpHandler handler;

  @BeforeEach
  void start() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.setExecutor(Executors.newFixedThreadPool(2));
    server.createContext("/", recording());
    server.start();
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  /** Anota lo que llega y delega en el manejador de cada prueba. */
  HttpHandler recording() {
    return exchange -> {
      received.add(
          new Received(
              exchange.getRequestMethod(),
              exchange.getRequestURI().toString(),
              exchange.getRequestHeaders().getFirst("Accept"),
              exchange.getRequestHeaders().getFirst("Cookie"),
              exchange.getRequestHeaders().getFirst("Authorization"),
              exchange.getRequestHeaders().getFirst("Host")));
      handler.handle(exchange);
    };
  }

  int port() {
    return server.getAddress().getPort();
  }

  String url(String path) {
    return "http://127.0.0.1:" + port() + path;
  }

  HttpCalendarFeed feed() {
    return feed(HttpCalendarFeed.TIMEOUT);
  }

  /**
   * Las pruebas hablan con un servidor en 127.0.0.1, que la política de producción bloquearía, así
   * que aquí se admite; lo que se ejercita es el resto de la descarga. La guardia de direcciones
   * tiene su propia prueba más abajo.
   */
  HttpCalendarFeed feed(Duration timeout) {
    return new HttpCalendarFeed(
        timeout, host -> List.of(InetAddress.getLoopbackAddress()), address -> true);
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
    var slow = feed(Duration.ofMillis(300));
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
    var slow = feed(Duration.ofMillis(300));
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

  /**
   * Enmienda B3: se conecta contra la dirección ya validada, no contra el nombre. El nombre usado
   * aquí no existe en ningún DNS; si el cliente volviera a resolverlo —que es lo que hacía antes—
   * la descarga terminaría en FEED_UNREACHABLE. Que llegue, y que el servidor vea el nombre
   * original en la cabecera Host, demuestra que no hubo segunda resolución.
   */
  @Test
  void s13_connectsToTheValidatedAddressAndKeepsTheNameInHost() {
    handler = exchange -> reply(exchange, 200, "text/calendar", VALID_ICS);
    var resolutions = new java.util.concurrent.atomic.AtomicInteger();
    var pinned =
        new HttpCalendarFeed(
            HttpCalendarFeed.TIMEOUT,
            host -> {
              resolutions.incrementAndGet();
              return List.of(InetAddress.getLoopbackAddress());
            },
            address -> true);
    var fetch =
        pinned.fetch("http://nombre.que.no.resuelve.invalid:" + port() + "/cal.ics?tok=WXYZ");
    assertInstanceOf(FeedFetch.Downloaded.class, fetch);
    assertEquals(1, resolutions.get(), "el nombre debe resolverse exactamente una vez");
    assertEquals(1, received.size());
    assertEquals(
        "nombre.que.no.resuelve.invalid:" + port(),
        received.getFirst().host(),
        "el proveedor debe seguir viendo su nombre en la cabecera Host");
  }

  /**
   * El reenlace cerrado: la única resolución que existe ocurre dentro de la misma clase que abre la
   * conexión, y si alguna de las direcciones devueltas está prohibida no se conecta en absoluto.
   */
  @Test
  void s13_aNameThatResolvesToAForbiddenAddressNeverConnects() {
    handler = exchange -> reply(exchange, 200, "text/calendar", VALID_ICS);
    var rejecting =
        new HttpCalendarFeed(
            HttpCalendarFeed.TIMEOUT,
            host -> List.of(InetAddress.getLoopbackAddress()),
            address -> false);
    assertEquals(FeedError.FEED_REJECTED, codeOf(rejecting.fetch(url("/cal.ics"))));
    assertTrue(received.isEmpty(), "no puede haber llegado ninguna petición al servidor");
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

  // --- @s12, enmienda B3: el apretón de manos del camino anclado ---------------------------------
  //
  // El resto de la clase habla HTTP en claro contra 127.0.0.1, así que hasta aquí lo verificado era
  // el anclaje de dirección y la cabecera Host, no el TLS. Y el TLS es justo lo que puede romper al
  // anclar: conectarse a una IP suele tirar abajo la verificación del certificado. Estas tres
  // pruebas miden que no ocurre, y son las hermanas de las de JdkWebhookSenderTest: mismo fixture,
  // mismo oráculo, misma decisión.

  /**
   * El único nombre para el que vale el certificado del fixture. No tiene ningún SAN de dirección.
   */
  static final String NOMBRE_DEL_CERTIFICADO = "destino.anclado.invalid";

  /** Otro nombre en la misma dirección que ningún certificado de aquí avala. */
  static final String OTRO_NOMBRE = "impostor.anclado.invalid";

  static final String ALMACEN = "/tls/anchored-receiver.p12";
  static final String ALIAS = "destino";
  static final char[] CLAVE_DEL_ALMACEN = "changeit".toCharArray();

  /** El proveedor de prueba sobre TLS, presentando el certificado del fixture. */
  private HttpsServer servidorTls() throws Exception {
    var claves = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    claves.init(almacen(), CLAVE_DEL_ALMACEN);
    var contexto = SSLContext.getInstance("TLS");
    contexto.init(claves.getKeyManagers(), null, null);
    var servidor = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    servidor.setHttpsConfigurator(new HttpsConfigurator(contexto));
    servidor.setExecutor(Executors.newFixedThreadPool(2));
    servidor.createContext("/", recording());
    servidor.start();
    return servidor;
  }

  private static KeyStore almacen() throws Exception {
    var almacen = KeyStore.getInstance("PKCS12");
    try (var flujo = HttpCalendarFeedTest.class.getResourceAsStream(ALMACEN)) {
      almacen.load(flujo, CLAVE_DEL_ALMACEN);
    }
    return almacen;
  }

  /**
   * Confía en el certificado del fixture y en nada más. Ojo con lo que NO hace: no desactiva la
   * verificación, sólo añade un ancla; por eso la tercera prueba sigue rechazando al desconocido.
   */
  private static SSLContext confianzaEnElFixture() throws Exception {
    var confiados = KeyStore.getInstance("PKCS12");
    confiados.load(null, null);
    confiados.setCertificateEntry("fixture", almacen().getCertificate(ALIAS));
    var gestores = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    gestores.init(confiados);
    var contexto = SSLContext.getInstance("TLS");
    contexto.init(null, gestores.getTrustManagers(), null);
    return contexto;
  }

  /** Null como confianza significa la del JDK, que es la de producción. */
  private HttpCalendarFeed feedConfiando(SSLContext confianza) {
    return new HttpCalendarFeed(
        HttpCalendarFeed.TIMEOUT,
        host -> List.of(InetAddress.getLoopbackAddress()),
        address -> true,
        confianza);
  }

  private static String urlTls(HttpsServer servidor, String host) {
    return "https://" + host + ":" + servidor.getAddress().getPort() + "/cal.ics";
  }

  @Test
  void s12_b3_unCertificadoValidoParaElNombreSeAceptaAunqueSeConecteALaDireccion()
      throws Exception {
    handler = exchange -> reply(exchange, 200, "text/calendar", VALID_ICS);
    var servidor = servidorTls();
    try {
      var fetch =
          feedConfiando(confianzaEnElFixture()).fetch(urlTls(servidor, NOMBRE_DEL_CERTIFICADO));

      assertInstanceOf(
          FeedFetch.Downloaded.class,
          fetch,
          "el certificado se verificó por nombre pese a conectar por dirección");
      assertTrue(textOf(fetch).contains("BEGIN:VCALENDAR"));
      assertEquals(
          NOMBRE_DEL_CERTIFICADO + ":" + servidor.getAddress().getPort(),
          received.getFirst().host());
    } finally {
      servidor.stop(0);
    }
  }

  /** El control de la anterior: se acepta por coincidir el nombre, no por no mirarlo. */
  @Test
  void s12_b3_elMismoCertificadoSeRechazaSiElNombrePedidoEsOtro() throws Exception {
    handler = exchange -> reply(exchange, 200, "text/calendar", VALID_ICS);
    var servidor = servidorTls();
    try {
      var fetch = feedConfiando(confianzaEnElFixture()).fetch(urlTls(servidor, OTRO_NOMBRE));

      assertEquals(FeedError.FEED_UNREACHABLE, codeOf(fetch));
      assertTrue(received.isEmpty(), "el apretón de manos falló antes de llegar ninguna petición");
    } finally {
      servidor.stop(0);
    }
  }

  /** Y el anclaje tampoco ha aflojado la cadena de confianza: sin el ancla, no se acepta. */
  @Test
  void s12_b3_unCertificadoQueNadieAvalaSeRechaza() throws Exception {
    handler = exchange -> reply(exchange, 200, "text/calendar", VALID_ICS);
    var servidor = servidorTls();
    try {
      var fetch = feedConfiando(null).fetch(urlTls(servidor, NOMBRE_DEL_CERTIFICADO));

      assertEquals(FeedError.FEED_UNREACHABLE, codeOf(fetch));
      assertTrue(received.isEmpty(), "el apretón de manos falló antes de llegar ninguna petición");
    } finally {
      servidor.stop(0);
    }
  }

  static byte[] padded(int length) {
    var body = new StringBuilder("BEGIN:VCALENDAR\r\nVERSION:2.0\r\n");
    while (body.length() < length) body.append('x');
    return body.substring(0, length).getBytes(StandardCharsets.ISO_8859_1);
  }
}
