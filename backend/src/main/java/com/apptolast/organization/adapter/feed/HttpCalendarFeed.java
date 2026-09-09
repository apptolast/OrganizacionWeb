package com.apptolast.organization.adapter.feed;

import com.apptolast.organization.adapter.net.AnchoredConnection;
import com.apptolast.organization.application.AddressPolicy;
import com.apptolast.organization.application.CalendarFeed;
import com.apptolast.organization.application.FeedFetch;
import com.apptolast.organization.application.HostResolver;
import com.apptolast.organization.domain.FeedError;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;

/**
 * Descarga de solo lectura: sin redirecciones, sin credenciales, con Accept text/calendar, corte a
 * 1 MiB y solo 200 con un tipo textual. El cuerpo se lee en trozos para poder abortar antes de que
 * el proveedor termine de emitir.
 *
 * <p><b>Enmienda B3 (project-spec.md:2492), que prevalece sobre el texto anterior.</b> «Se resuelve
 * el nombre una vez, se validan todas las direcciones devueltas y se conecta contra la dirección
 * literal ya validada, conservando el nombre original en la cabecera Host y en la indicación de
 * servidor de TLS.» Eso es lo que hace {@link #fetch(String)}: resuelve aquí, dentro de la misma
 * clase que abre la conexión, exige que <b>todas</b> las direcciones devueltas pasen la {@link
 * AddressPolicy}, y construye la petición contra la dirección literal. Como el cliente HTTP recibe
 * ya una dirección, no vuelve a preguntar al DNS: el reenlace de nombres entre la comprobación y el
 * uso deja de ser posible, en vez de aceptarse como riesgo residual.
 *
 * <p>Las tres piezas del anclaje —dirección literal, cabecera {@code Host} e indicación de servidor
 * de TLS— viven en {@link AnchoredConnection}, compartidas con el emisor de webhooks de la feature
 * 25, que ancla igual. Es una sola decisión; tenerla escrita dos veces fue lo que dejó que los dos
 * carriles decidieran lo contrario sin verse.
 *
 * <p>Anclar no cuesta el TLS, y está medido en los dos sentidos: un certificado válido para el
 * nombre se acepta aunque la conexión vaya a la dirección, y el mismo certificado se rechaza si el
 * nombre pedido es otro (ver {@code HttpCalendarFeedTest}, sección de la enmienda B3).
 *
 * <p>Límite que sí queda, y conviene no disfrazar: si el nombre resuelve a varias direcciones se
 * conecta a la primera y no se reintenta con las demás. Todas estaban validadas, así que no es un
 * agujero de seguridad; es una pérdida de tolerancia a fallos frente al comportamiento por defecto
 * del cliente.
 *
 * <p>El plazo es del <b>intercambio completo</b>: conexión, cabeceras y lectura del cuerpo. No
 * basta con {@code HttpRequest.timeout}, porque con {@code BodyHandlers.ofInputStream()} el
 * temporizador del cliente se cancela en cuanto llegan las cabeceras y las lecturas posteriores
 * quedan fuera de él. Un proveedor que envía las cabeceras al instante y luego gotea un byte cada
 * varios segundos dejaría el hilo de petición bloqueado para siempre, y con él el pool entero de
 * Tomcat: la misma familia de la enmienda B1 de los webhooks (un receptor lento que agota
 * recursos).
 *
 * <p>Se cierra por los dos lados: el bucle de lectura comprueba el instante límite en cada trozo, y
 * además se programa el cierre del cuerpo en ese instante, porque un proveedor que se calla del
 * todo dejaría el {@code read} bloqueado sin llegar nunca a la comprobación.
 */
public final class HttpCalendarFeed implements CalendarFeed {
  public static final Duration TIMEOUT = Duration.ofSeconds(5);
  public static final int LIMIT = 1024 * 1024;
  private static final int CHUNK = 16 * 1024;
  private static final String ACCEPT = "text/calendar";

  static {
    AnchoredConnection.allow();
  }

  /**
   * Un solo hilo demonio para todas las descargas: sólo cierra cuerpos vencidos, no lee nada, y
   * siendo demonio no impide que la aplicación termine.
   */
  private static final ScheduledExecutorService DEADLINES =
      Executors.newSingleThreadScheduledExecutor(
          runnable -> {
            var thread = new Thread(runnable, "calendar-feed-deadline");
            thread.setDaemon(true);
            return thread;
          });

  private final Duration timeout;
  private final HostResolver resolver;
  private final AddressPolicy policy;
  private final SSLContext tls;

  public HttpCalendarFeed(Duration timeout, HostResolver resolver, AddressPolicy policy) {
    this(timeout, resolver, policy, null);
  }

  /**
   * La confianza es inyectable sólo para las pruebas: una prueba no puede añadir una autoridad al
   * almacén del JDK, y sin eso no se puede demostrar que un certificado válido para el NOMBRE se
   * acepta mientras la conexión va a la DIRECCIÓN. Null es la confianza de la plataforma, que es la
   * que usa producción.
   */
  HttpCalendarFeed(Duration timeout, HostResolver resolver, AddressPolicy policy, SSLContext tls) {
    this.timeout = timeout;
    this.resolver = resolver;
    this.policy = policy;
    this.tls = tls;
  }

  @Override
  public FeedFetch fetch(String url) {
    URI target;
    String host;
    try {
      target = URI.create(url);
      host = target.getHost();
    } catch (IllegalArgumentException malformed) {
      return FeedFetch.failed(FeedError.FEED_UNREACHABLE);
    }
    if (host == null || host.isBlank()) return FeedFetch.failed(FeedError.FEED_UNREACHABLE);

    List<InetAddress> addresses;
    try {
      addresses = resolver.resolve(host);
    } catch (RuntimeException unresolvable) {
      return FeedFetch.failed(FeedError.FEED_UNREACHABLE);
    }
    if (addresses == null || addresses.isEmpty())
      return FeedFetch.failed(FeedError.FEED_UNREACHABLE);
    if (!addresses.stream().allMatch(policy::allows))
      return FeedFetch.failed(FeedError.FEED_REJECTED);

    return download(target, host, addresses.getFirst());
  }

  private FeedFetch download(URI target, String host, InetAddress pinned) {
    HttpRequest request;
    try {
      request =
          HttpRequest.newBuilder(AnchoredConnection.literal(target, pinned))
              .GET()
              .header("Accept", ACCEPT)
              .header("Host", AnchoredConnection.authority(target, host))
              .timeout(timeout)
              .build();
    } catch (IllegalArgumentException rejected) {
      // Incluye el caso de que el despliegue no admita la cabecera Host restringida: antes de
      // conectar sin ella —y por tanto contra el servidor equivocado— se prefiere no conectar.
      return FeedFetch.failed(FeedError.FEED_UNREACHABLE);
    }
    long deadline = System.nanoTime() + timeout.toNanos();
    try (var client = client(host)) {
      var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
      try (var body = response.body()) {
        if (response.statusCode() != 200) return FeedFetch.failed(FeedError.FEED_HTTP_ERROR);
        if (!isTextual(response)) return FeedFetch.failed(FeedError.FEED_UNSUPPORTED_TYPE);
        return read(body, deadline);
      }
    } catch (IOException | InterruptedException unreachable) {
      if (unreachable instanceof InterruptedException) Thread.currentThread().interrupt();
      return FeedFetch.failed(FeedError.FEED_UNREACHABLE);
    }
  }

  /**
   * El cliente se construye por descarga porque el nombre de servidor de TLS es propio de cada
   * destino: sin él, conectar por dirección literal presentaría la IP como SNI y el proveedor no
   * podría elegir su certificado.
   */
  private HttpClient client(String host) {
    var builder =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(timeout)
            .sslParameters(AnchoredConnection.sniFor(host));
    if (tls != null) builder.sslContext(tls);
    return builder.build();
  }

  private static boolean isTextual(HttpResponse<InputStream> response) {
    return response
        .headers()
        .firstValue("Content-Type")
        .map(value -> value.toLowerCase(Locale.ROOT).startsWith("text/"))
        .orElse(false);
  }

  /**
   * Lee hasta un byte más del límite —ese byte de más es la prueba de que el feed no cabe— y no más
   * allá del instante límite del intercambio.
   */
  private static FeedFetch read(InputStream body, long deadline) throws IOException {
    var guillotine =
        DEADLINES.schedule(
            () -> closeQuietly(body),
            Math.max(0, deadline - System.nanoTime()),
            TimeUnit.NANOSECONDS);
    try {
      var buffer = new java.io.ByteArrayOutputStream();
      var chunk = new byte[CHUNK];
      int read;
      while ((read = body.read(chunk)) >= 0) {
        if (expired(deadline)) return FeedFetch.failed(FeedError.FEED_UNREACHABLE);
        buffer.write(chunk, 0, read);
        if (buffer.size() > LIMIT) return FeedFetch.failed(FeedError.FEED_TOO_LARGE);
      }
      if (expired(deadline)) return FeedFetch.failed(FeedError.FEED_UNREACHABLE);
      return FeedFetch.downloaded(buffer.toString(StandardCharsets.UTF_8));
    } catch (IOException cut) {
      if (expired(deadline)) return FeedFetch.failed(FeedError.FEED_UNREACHABLE);
      throw cut;
    } finally {
      guillotine.cancel(false);
    }
  }

  private static boolean expired(long deadline) {
    return System.nanoTime() - deadline >= 0;
  }

  private static void closeQuietly(InputStream body) {
    try {
      body.close();
    } catch (IOException alreadyGone) {
      // Cerrar un cuerpo ya cerrado no cambia nada: el corte es lo que importa.
    }
  }
}
