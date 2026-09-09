package com.apptolast.organization.application;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * Resolves the destination host and rejects blocked or unresolvable addresses.
 *
 * <p>The policy is the shared {@link AddressPolicy} of amendment B2, injected rather than reached
 * for. Note the direction: {@code allows} answers PERMITTED, so the rejection is its negation.
 */
public final class WebhookDestinationGuard {
  public interface HostResolver {
    InetAddress[] resolve(String host) throws UnknownHostException;
  }

  private final HostResolver resolver;
  private final AddressPolicy policy;

  public WebhookDestinationGuard(HostResolver resolver, AddressPolicy policy) {
    this.resolver = resolver;
    this.policy = policy;
  }

  public void check(String url) {
    var host = URI.create(url).getHost();
    InetAddress[] addresses;
    try {
      addresses =
          literal(host) ? new InetAddress[] {InetAddress.getByName(host)} : resolver.resolve(host);
    } catch (UnknownHostException error) {
      throw new WebhookOperationException(WebhookOperationException.Code.URL_UNRESOLVABLE);
    }
    if (addresses == null || addresses.length == 0)
      throw new WebhookOperationException(WebhookOperationException.Code.URL_UNRESOLVABLE);
    for (var address : addresses)
      if (!policy.allows(address))
        throw new WebhookOperationException(WebhookOperationException.Code.URL_BLOCKED);
  }

  private static boolean literal(String host) {
    return host.startsWith("[") || host.matches("[0-9.]+");
  }
}
