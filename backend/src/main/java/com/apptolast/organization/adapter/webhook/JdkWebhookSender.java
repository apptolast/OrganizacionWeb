package com.apptolast.organization.adapter.webhook;

import com.apptolast.organization.adapter.net.AnchoredConnection;
import com.apptolast.organization.application.AddressPolicy;
import com.apptolast.organization.application.WebhookSender;
import com.apptolast.organization.domain.WebhookAttempt;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

/**
 * The real outgoing attempt over the JDK HTTP client.
 *
 * <p>Amendment B1: five seconds to connect and ten for the whole exchange, so a receiver that
 * accepts the connection and stalls still times out. The response body is discarded, never stored.
 *
 * <p><b>Amendment B3, in full and as the code does it.</b> The host is resolved once, every
 * returned address is checked, and the request is abandoned before any connection is opened if a
 * single one is blocked. The check uses the shared {@link AddressPolicy}, whose {@code allows}
 * answers PERMITTED: the rejection is its negation. The request then travels to the <b>literal
 * address already validated</b>, not to the name again, so the client never asks DNS a second time
 * and a name re-pointed between the check and the use stops being possible. The same anchoring that
 * {@code adapter.feed.HttpCalendarFeed} does for feature 28.
 *
 * <p>The name is not lost on the way: it travels in the {@code Host} header and in the TLS server
 * name indication, and the certificate is verified against it, which {@code JdkWebhookSenderTest}
 * proves in both directions —a certificate valid for the name is accepted although the connection
 * goes to the address, and one nobody vouches for is still rejected.
 *
 * <p>Limits that anchoring does leave, written down instead of hidden: if the name resolves to
 * several addresses only the first is tried, which loses the client's failover —all of them were
 * validated, so it is not a security hole— and the exchange is pinned to HTTP/1.1, because over
 * HTTP/2 the authority comes from the URI and the literal address, not the {@code Host} header,
 * would be what the receiver sees.
 */
public final class JdkWebhookSender implements WebhookSender {
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration EXCHANGE_TIMEOUT = Duration.ofSeconds(10);
  private static final String USER_AGENT = "OrganizationWeb-Webhooks/1";
  private static final String CONTENT_TYPE = "application/json; charset=utf-8";
  private static final int MILLIS_PER_NANO = 1_000_000;

  /** Any name will do to read a deadline back from a client; it is never connected to. */
  private static final String PROBE_HOST = "deadline.probe.invalid";

  static {
    AnchoredConnection.allow();
  }

  private final Clock clock;

  /**
   * El cronómetro del intento. Es un {@link java.util.function.LongSupplier} de nanos y no el
   * {@link Clock} inyectado a propósito: medir tiempo transcurrido con un reloj de pared es un
   * defecto conocido —un ajuste de NTP a mitad de intento da latencias negativas o absurdas—.
   * Inyectarlo satisface la cláusula del contrato («medido con el reloj inyectado»: nada ambiente,
   * todo controlable por la prueba) sin renunciar a la monotonía, que es lo que una latencia
   * necesita.
   */
  private final java.util.function.LongSupplier ticker;

  private final AddressPolicy policy;
  private final com.apptolast.organization.application.WebhookDestinationGuard.HostResolver
      resolver;
  private final Duration connectDeadline;
  private final Duration exchangeDeadline;
  private final SSLContext tls;

  public JdkWebhookSender(
      Clock clock,
      AddressPolicy policy,
      com.apptolast.organization.application.WebhookDestinationGuard.HostResolver resolver) {
    this(clock, policy, resolver, CONNECT_TIMEOUT, EXCHANGE_TIMEOUT);
  }

  /**
   * The deadlines are injectable so a test can produce a real timeout in milliseconds instead of
   * paying the ten seconds of production. Production wiring goes through the public constructor.
   */
  JdkWebhookSender(
      Clock clock,
      AddressPolicy policy,
      com.apptolast.organization.application.WebhookDestinationGuard.HostResolver resolver,
      Duration connectDeadline,
      Duration exchangeDeadline) {
    this(clock, policy, resolver, connectDeadline, exchangeDeadline, null);
  }

  /**
   * The trust material is injectable for the same reason as the deadlines: a test cannot add its
   * own certificate authority to the JDK's, and without that it cannot prove that a certificate
   * valid for the NAME is accepted while the connection goes to the ADDRESS. Null means the default
   * trust of the platform, which is what production uses.
   */
  JdkWebhookSender(
      Clock clock,
      AddressPolicy policy,
      com.apptolast.organization.application.WebhookDestinationGuard.HostResolver resolver,
      Duration connectDeadline,
      Duration exchangeDeadline,
      SSLContext tls) {
    this(clock, policy, resolver, connectDeadline, exchangeDeadline, tls, System::nanoTime);
  }

  /**
   * El cronómetro es inyectable porque el contrato lo exige —«latencyMs es un entero no negativo
   * medido con el reloj inyectado», features/webhooks.feature:320— y porque sin inyectarlo la
   * cláusula no se puede probar: una latencia real no es reproducible. Producción usa {@code
   * System::nanoTime} a través del constructor de arriba.
   */
  JdkWebhookSender(
      Clock clock,
      AddressPolicy policy,
      com.apptolast.organization.application.WebhookDestinationGuard.HostResolver resolver,
      Duration connectDeadline,
      Duration exchangeDeadline,
      SSLContext tls,
      java.util.function.LongSupplier ticker) {
    this.clock = clock;
    this.ticker = ticker;
    this.policy = policy;
    this.resolver = resolver;
    this.connectDeadline = connectDeadline;
    this.exchangeDeadline = exchangeDeadline;
    this.tls = tls;
  }

  /** Read back from a client, not from the field: it is the deadline actually wired. */
  Duration connectDeadline() {
    try (var probe = client(PROBE_HOST)) {
      return probe.connectTimeout().orElseThrow();
    }
  }

  Duration exchangeDeadline() {
    return exchangeDeadline;
  }

  @Override
  public WebhookAttempt send(String url, String secret, String eventId, String body) {
    var started = ticker.getAsLong();
    var payload = body.getBytes(StandardCharsets.UTF_8);
    var target = URI.create(url);
    var host = target.getHost();
    try {
      var pinned = validatedAddress(host);
      var request = request(target, host, pinned, secret, eventId, payload);
      try (var client = client(host)) {
        var response = client.send(request, discardingBody());
        return WebhookAttempt.http(response.statusCode(), elapsedMillis(started));
      }
    } catch (BlockedDestination blocked) {
      return WebhookAttempt.transport("BLOCKED_ADDRESS", elapsedMillis(started));
    } catch (UnknownHostException unresolved) {
      return WebhookAttempt.transport("DNS", elapsedMillis(started));
    } catch (HttpTimeoutException timeout) {
      return WebhookAttempt.transport("TIMEOUT", elapsedMillis(started));
    } catch (SSLException tls) {
      return WebhookAttempt.transport("TLS", elapsedMillis(started));
    } catch (ConnectException refused) {
      return WebhookAttempt.transport("CONNECTION", elapsedMillis(started));
    } catch (IOException failure) {
      return WebhookAttempt.transport(classify(failure), elapsedMillis(started));
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      return WebhookAttempt.transport("CONNECTION", elapsedMillis(started));
    }
  }

  /** The JDK wraps some causes; the underlying one decides the class. */
  private static String classify(IOException failure) {
    for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
      if (cause instanceof UnknownHostException) return "DNS";
      if (cause instanceof SSLException) return "TLS";
      if (cause instanceof HttpTimeoutException) return "TIMEOUT";
    }
    return "CONNECTION";
  }

  /**
   * The single resolution of the amendment: every address returned must pass the policy, and the
   * first one is the address the request will be anchored to.
   */
  private InetAddress validatedAddress(String host) throws UnknownHostException {
    var addresses = resolver.resolve(host);
    if (addresses == null || addresses.length == 0) throw new UnknownHostException(host);
    for (var address : addresses) if (!policy.allows(address)) throw new BlockedDestination();
    return addresses[0];
  }

  /**
   * The client is built per send because the TLS server name belongs to the destination: shared
   * across hosts it would offer the wrong name, and offering none while connecting to a literal
   * address would leave the receiver unable to pick its certificate.
   */
  private HttpClient client(String host) {
    var builder =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(connectDeadline)
            .sslParameters(AnchoredConnection.sniFor(host));
    if (tls != null) builder.sslContext(tls);
    return builder.build();
  }

  private HttpRequest request(
      URI target, String host, InetAddress pinned, String secret, String eventId, byte[] payload) {
    return HttpRequest.newBuilder(AnchoredConnection.literal(target, pinned))
        .header("Host", AnchoredConnection.authority(target, host))
        .timeout(exchangeDeadline)
        .header("Content-Type", CONTENT_TYPE)
        .header("User-Agent", USER_AGENT)
        .header("X-OrganizationWeb-Event-Id", eventId)
        .header(
            "X-OrganizationWeb-Signature",
            WebhookSignature.header(secret, clock.instant().getEpochSecond(), payload))
        .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
        .build();
  }

  /** The receiver's answer is never needed and never kept. */
  private static HttpResponse.BodyHandler<Void> discardingBody() {
    return HttpResponse.BodyHandlers.discarding();
  }

  private int elapsedMillis(long startedNanos) {
    return (int) Math.max(0, (ticker.getAsLong() - startedNanos) / MILLIS_PER_NANO);
  }

  private static final class BlockedDestination extends RuntimeException {}
}
