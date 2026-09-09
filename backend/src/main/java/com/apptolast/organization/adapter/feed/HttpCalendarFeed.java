package com.apptolast.organization.adapter.feed;

import com.apptolast.organization.application.CalendarFeed;
import com.apptolast.organization.application.FeedFetch;
import com.apptolast.organization.domain.FeedError;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Descarga de solo lectura: sin redirecciones, sin credenciales, con Accept text/calendar, corte a
 * 1 MiB y solo 200 con un tipo textual. El cuerpo se lee en trozos para poder abortar antes de que
 * el proveedor termine de emitir.
 *
 * <p>El plazo es del <b>intercambio completo</b>: conexión, cabeceras y lectura del cuerpo. No basta
 * con {@code HttpRequest.timeout}, porque con {@code BodyHandlers.ofInputStream()} el temporizador
 * del cliente se cancela en cuanto llegan las cabeceras y las lecturas posteriores quedan fuera de
 * él. Un proveedor que envía las cabeceras al instante y luego gotea un byte cada varios segundos
 * dejaría el hilo de petición bloqueado para siempre, y con él el pool entero de Tomcat: la misma
 * familia de la enmienda B1 de los webhooks (un receptor lento que agota recursos).
 *
 * <p>Se cierra por los dos lados: el bucle de lectura comprueba el instante límite en cada trozo, y
 * además se programa el cierre del cuerpo en ese instante, porque un proveedor que se calla del todo
 * dejaría el {@code read} bloqueado sin llegar nunca a la comprobación.
 */
public final class HttpCalendarFeed implements CalendarFeed {
  public static final Duration TIMEOUT = Duration.ofSeconds(5);
  public static final int LIMIT = 1024 * 1024;
  private static final int CHUNK = 16 * 1024;
  private static final String ACCEPT = "text/calendar";

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

  private final HttpClient client;
  private final Duration timeout;

  public HttpCalendarFeed(Duration timeout) {
    this.timeout = timeout;
    this.client =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(timeout)
            .build();
  }

  @Override
  public FeedFetch fetch(String url) {
    HttpRequest request;
    try {
      request =
          HttpRequest.newBuilder(URI.create(url))
              .GET()
              .header("Accept", ACCEPT)
              .timeout(timeout)
              .build();
    } catch (IllegalArgumentException malformed) {
      return FeedFetch.failed(FeedError.FEED_UNREACHABLE);
    }
    long deadline = System.nanoTime() + timeout.toNanos();
    try {
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
