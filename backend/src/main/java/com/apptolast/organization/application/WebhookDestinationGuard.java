package com.apptolast.organization.application;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.function.Predicate;

/** Resolves the destination host and rejects blocked or unresolvable addresses. */
public final class WebhookDestinationGuard {
  public interface HostResolver {
    InetAddress[] resolve(String host) throws UnknownHostException;
  }

  private final HostResolver resolver;
  private final Predicate<InetAddress> blocked;

  public WebhookDestinationGuard(HostResolver resolver, Predicate<InetAddress> blocked) {
    this.resolver = resolver;
    this.blocked = blocked;
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
      if (blocked.test(address))
        throw new WebhookOperationException(WebhookOperationException.Code.URL_BLOCKED);
  }

  private static boolean literal(String host) {
    return host.startsWith("[") || host.matches("[0-9.]+");
  }
}
