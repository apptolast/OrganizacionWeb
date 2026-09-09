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

/**
 * Descarga de solo lectura: sin redirecciones, sin credenciales, con Accept text/calendar, corte a 1
 * MiB y solo 200 con un tipo textual. El cuerpo se lee en trozos para poder abortar antes de que el
 * proveedor termine de emitir.
 */
public final class HttpCalendarFeed implements CalendarFeed {
  public static final Duration TIMEOUT = Duration.ofSeconds(5);
  public static final int LIMIT = 1024 * 1024;
  private static final int CHUNK = 16 * 1024;
  private static final String ACCEPT = "text/calendar";

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
    try {
      var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
      try (var body = response.body()) {
        if (response.statusCode() != 200) return FeedFetch.failed(FeedError.FEED_HTTP_ERROR);
        if (!isTextual(response)) return FeedFetch.failed(FeedError.FEED_UNSUPPORTED_TYPE);
        return read(body);
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

  /** Lee hasta un byte más del límite: ese byte de más es la prueba de que el feed no cabe. */
  private static FeedFetch read(InputStream body) throws IOException {
    var buffer = new java.io.ByteArrayOutputStream();
    var chunk = new byte[CHUNK];
    int read;
    while ((read = body.read(chunk)) >= 0) {
      buffer.write(chunk, 0, read);
      if (buffer.size() > LIMIT) return FeedFetch.failed(FeedError.FEED_TOO_LARGE);
    }
    return FeedFetch.downloaded(buffer.toString(StandardCharsets.UTF_8));
  }
}
