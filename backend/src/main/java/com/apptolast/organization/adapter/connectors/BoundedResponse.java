package com.apptolast.organization.adapter.connectors;

import com.apptolast.organization.application.IssueSourceException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Respuesta de un gestor de issues cuyo cuerpo ya se ha leído bajo un techo de tamaño y un plazo.
 * La comparten los dos adaptadores HTTP porque el peligro es el mismo en los dos.
 *
 * <p>Existe por la fila «200 con cuerpo JSON de más de 5 MiB» del {@code @s25} de {@code
 * features/additional_connectors.feature}. Antes el cuerpo se leía con {@code
 * BodyHandlers.ofString()}, que no tiene techo ninguno, y la base de la API es configurable
 * precisamente para instancias autoalojadas: una respuesta hostil —o un intermediario— agotaba la
 * memoria del proceso. El enfoque es el que ya usa {@code HttpCalendarFeed} para los feeds de
 * calendario: leer en trozos para poder abortar antes de que el proveedor termine de emitir.
 *
 * <p><b>El plazo no es un adorno del techo, es su otra mitad.</b> El techo sólo rescata del
 * proveedor que <em>sigue emitiendo</em>; uno que abre el cuerpo y se calla dejaría el hilo de
 * petición bloqueado, y con él el pool entero de Tomcat. Y con {@code ofInputStream()} no basta
 * {@code HttpRequest.timeout}, porque su temporizador se cancela en cuanto llegan las cabeceras y
 * las lecturas posteriores quedan fuera de él. Se cierra por los dos lados, igual que el feed: el
 * bucle mira el instante límite en cada trozo, y además se programa el cierre del cuerpo en ese
 * instante, porque un proveedor mudo no llega nunca a la comprobación.
 */
record BoundedResponse(int status, HttpHeaders headers, String body) {
  /** Techo del contrato: el {@code @s25} declara indisponible el cuerpo de más de 5 MiB. */
  static final int LIMIT = 5 * 1024 * 1024;

  private static final int CHUNK = 16 * 1024;

  /**
   * Un solo hilo demonio para todas las lecturas: sólo cierra cuerpos vencidos, no lee nada, y
   * siendo demonio no impide que la aplicación termine.
   */
  private static final ScheduledExecutorService DEADLINES =
      Executors.newSingleThreadScheduledExecutor(
          runnable -> {
            var thread = new Thread(runnable, "issue-source-deadline");
            thread.setDaemon(true);
            return thread;
          });

  /**
   * Lee el cuerpo entero bajo el techo y el plazo, y lo cierra pase lo que pase: cerrarlo es lo que
   * aborta la descarga de un cuerpo que no cabe.
   */
  static BoundedResponse read(HttpResponse<InputStream> response, long deadline)
      throws IOException {
    try (var stream = response.body()) {
      return new BoundedResponse(
          response.statusCode(), response.headers(), bounded(stream, deadline));
    }
  }

  Optional<String> header(String name) {
    return headers.firstValue(name);
  }

  /**
   * Lee hasta un byte más del techo —ese byte de más es la prueba de que el cuerpo no cabe— y no
   * más allá del instante límite del intercambio.
   */
  private static String bounded(InputStream body, long deadline) throws IOException {
    var guillotine =
        DEADLINES.schedule(
            () -> closeQuietly(body),
            Math.max(0, deadline - System.nanoTime()),
            TimeUnit.NANOSECONDS);
    try {
      var buffer = new ByteArrayOutputStream();
      var chunk = new byte[CHUNK];
      int read;
      while ((read = body.read(chunk)) >= 0) {
        if (expired(deadline)) throw IssueSourceException.unavailable();
        buffer.write(chunk, 0, read);
        if (buffer.size() > LIMIT) throw IssueSourceException.unavailable();
      }
      if (expired(deadline)) throw IssueSourceException.unavailable();
      return buffer.toString(StandardCharsets.UTF_8);
    } catch (IOException cut) {
      if (expired(deadline)) throw IssueSourceException.unavailable();
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
