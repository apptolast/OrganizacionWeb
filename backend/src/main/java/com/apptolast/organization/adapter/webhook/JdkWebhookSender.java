package com.apptolast.organization.adapter.webhook;

import com.apptolast.organization.application.AddressPolicy;
import com.apptolast.organization.application.WebhookSender;
import com.apptolast.organization.domain.WebhookAttempt;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import javax.net.ssl.SSLException;

/**
 * The real outgoing attempt over the JDK HTTP client.
 *
 * <p>Amendment B1: five seconds to connect and ten for the whole exchange, so a receiver that
 * accepts the connection and stalls still times out. The response body is discarded, never stored.
 *
 * <p>Amendment B3: the host is resolved once and every returned address is checked before
 * connecting, so no name can be re-pointed between the check and the use. The check uses the shared
 * {@link AddressPolicy}, whose {@code allows} answers PERMITTED: the rejection is its negation.
 */
public final class JdkWebhookSender implements WebhookSender {
  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration EXCHANGE_TIMEOUT = Duration.ofSeconds(10);
  private static final String USER_AGENT = "OrganizationWeb-Webhooks/1";
  private static final String CONTENT_TYPE = "application/json; charset=utf-8";
  private static final int MILLIS_PER_NANO = 1_000_000;

  private final Clock clock;
  private final AddressPolicy policy;
  private final com.apptolast.organization.application.WebhookDestinationGuard.HostResolver
      resolver;
  private final HttpClient client;

  public JdkWebhookSender(
      Clock clock,
      AddressPolicy policy,
      com.apptolast.organization.application.WebhookDestinationGuard.HostResolver resolver) {
    this.clock = clock;
    this.policy = policy;
    this.resolver = resolver;
    this.client =
        HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
  }

  @Override
  public WebhookAttempt send(String url, String secret, String eventId, String body) {
    var started = System.nanoTime();
    var payload = body.getBytes(StandardCharsets.UTF_8);
    try {
      guardDestination(url);
      var response = client.send(request(url, secret, eventId, payload), discardingBody());
      return WebhookAttempt.http(response.statusCode(), elapsedMillis(started));
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

  private void guardDestination(String url) throws UnknownHostException {
    var host = URI.create(url).getHost();
    var addresses = resolver.resolve(host);
    if (addresses == null || addresses.length == 0) throw new UnknownHostException(host);
    for (var address : addresses) if (!policy.allows(address)) throw new BlockedDestination();
  }

  private HttpRequest request(String url, String secret, String eventId, byte[] payload) {
    return HttpRequest.newBuilder(URI.create(url))
        .timeout(EXCHANGE_TIMEOUT)
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

  private static int elapsedMillis(long startedNanos) {
    return (int) Math.max(0, (System.nanoTime() - startedNanos) / MILLIS_PER_NANO);
  }

  private static final class BlockedDestination extends RuntimeException {}
}
