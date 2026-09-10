package com.apptolast.organization.adapter.connectors;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.application.IssueSourceException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.net.ssl.SSLSession;
import org.junit.jupiter.api.Test;

/**
 * El techo y el plazo con los que se lee todo cuerpo del proveedor (@s25). Las pruebas del
 * adaptador sólo llegan a esta clase a través de un servidor real, que no sabe emitir lecturas de
 * cero bytes ni fallos de red a voluntad; aquí se le entrega el flujo directamente, que es lo único
 * que discrimina las tres decisiones que toma el bucle.
 */
class BoundedResponseTest {
  private static final long FAR_AWAY = Duration.ofMinutes(1).toNanos();

  private static long deadlineIn(Duration remaining) {
    return System.nanoTime() + remaining.toNanos();
  }

  private static BoundedResponse read(InputStream body) throws IOException {
    return read(body, HttpHeaders.of(Map.of(), (name, value) -> true));
  }

  private static BoundedResponse read(InputStream body, HttpHeaders headers) throws IOException {
    return BoundedResponse.read(
        new StubResponse(200, headers, body), deadlineIn(Duration.ofMinutes(1)));
  }

  /**
   * Una lectura de cero bytes <b>no</b> es el final del cuerpo: {@code InputStream.read} puede
   * devolver cero y seguir habiendo datos detrás. Cortar ahí trunca la respuesta en silencio, y una
   * lista de issues truncada es JSON inválido o, peor, una lista corta que parece completa.
   */
  @Test
  void s25_areadOfZeroBytesIsNotTheEndOfTheBody() throws IOException {
    var body = new StutteringStream("[{\"id\":9001}]");

    assertThat(read(body).body()).isEqualTo("[{\"id\":9001}]");
    assertThat(body.zeroByteReads()).isPositive();
  }

  /**
   * Un fallo de red que llega con plazo de sobra es un fallo de red, no un vencimiento: propagarlo
   * como {@link IssueSourceException} aquí borraría la distinción antes de que nadie la mire.
   */
  @Test
  void s25_anetworkFailureWellWithinTheDeadlineTravelsAsItselfAndNotAsATimeout() {
    var broken = new BrokenStream("la conexión se cayó");

    assertThatThrownBy(() -> read(broken))
        .isInstanceOf(IOException.class)
        .isNotInstanceOf(IssueSourceException.class)
        .hasMessage("la conexión se cayó");
  }

  /** Un cuerpo que se corta con el plazo ya vencido sí es un vencimiento. */
  @Test
  void s25_thesameFailureOnceTheDeadlineIsSpentIsAnUnavailableProvider() {
    var broken = new BrokenStream("la conexión se cayó");

    assertThatThrownBy(
            () ->
                BoundedResponse.read(
                    new StubResponse(200, HttpHeaders.of(Map.of(), (name, value) -> true), broken),
                    System.nanoTime() - FAR_AWAY))
        .isInstanceOf(IssueSourceException.class);
  }

  /** El techo del contrato: un byte por encima de 5 MiB ya no cabe, y justo en el techo sí. */
  @Test
  void s25_theCeilingIsFiveMebibytesInclusive() throws IOException {
    assertThat(read(streamOf(BoundedResponse.LIMIT)).body()).hasSize(BoundedResponse.LIMIT);

    assertThatThrownBy(() -> read(streamOf(BoundedResponse.LIMIT + 1)))
        .isInstanceOf(IssueSourceException.class);
  }

  /** El estado y las cabeceras llegan enteros: de ellos cuelgan X-Next-Page y la cuota. */
  @Test
  void s16_theStatusAndTheHeadersOfTheProviderTravelUntouched() throws IOException {
    var headers = HttpHeaders.of(Map.of("X-Next-Page", List.of("2")), (name, value) -> true);

    var response = read(streamOf(0), headers);

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.headers().firstValue("x-next-page")).contains("2");
    assertThat(response.header("X-Next-Page")).contains("2");
  }

  private static InputStream streamOf(int bytes) {
    return new ByteArrayInputStream("x".repeat(bytes).getBytes(StandardCharsets.UTF_8));
  }

  /** Devuelve cero bytes antes de cada trozo real, como hace un socket que aún no tiene datos. */
  private static final class StutteringStream extends InputStream {
    private final byte[] content;
    private int position;
    private boolean owed = true;
    private int zeroByteReads;

    private StutteringStream(String content) {
      this.content = content.getBytes(StandardCharsets.UTF_8);
    }

    int zeroByteReads() {
      return zeroByteReads;
    }

    @Override
    public int read() {
      return position < content.length ? content[position++] : -1;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) {
      if (owed) {
        owed = false;
        zeroByteReads++;
        return 0;
      }
      if (position == content.length) return -1;
      int copied = Math.min(length, content.length - position);
      System.arraycopy(content, position, buffer, offset, copied);
      position += copied;
      owed = true;
      return copied;
    }
  }

  /** Se rompe a la primera lectura, como una conexión que el proveedor cierra a lo bruto. */
  private static final class BrokenStream extends InputStream {
    private final String reason;

    private BrokenStream(String reason) {
      this.reason = reason;
    }

    @Override
    public int read() throws IOException {
      throw new IOException(reason);
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
      throw new IOException(reason);
    }
  }

  /** Lo mínimo de {@link HttpResponse} que {@link BoundedResponse#read} llega a mirar. */
  private record StubResponse(int status, HttpHeaders headers, InputStream stream)
      implements HttpResponse<InputStream> {
    @Override
    public int statusCode() {
      return status;
    }

    @Override
    public HttpRequest request() {
      return HttpRequest.newBuilder(uri()).build();
    }

    @Override
    public Optional<HttpResponse<InputStream>> previousResponse() {
      return Optional.empty();
    }

    @Override
    public InputStream body() {
      return stream;
    }

    @Override
    public Optional<SSLSession> sslSession() {
      return Optional.empty();
    }

    @Override
    public URI uri() {
      return URI.create("https://127.0.0.1/api/v4/projects/4821/issues");
    }

    @Override
    public HttpClient.Version version() {
      return HttpClient.Version.HTTP_1_1;
    }
  }
}
