package com.apptolast.organization.adapter.config;

import java.net.URI;
import org.springframework.session.web.http.DefaultCookieSerializer;

public final class SessionCookiePolicy {
  private SessionCookiePolicy() {}

  public static DefaultCookieSerializer create(String origin) {
    URI uri = URI.create(origin);
    String host = uri.getHost();
    boolean secure = "https".equalsIgnoreCase(uri.getScheme());
    if (host == null
        || uri.getRawUserInfo() != null
        || uri.getRawQuery() != null
        || uri.getRawFragment() != null
        || !uri.getRawPath().isEmpty()
        || uri.getPort() == 0
        || uri.getPort() > 65535
        || uri.getRawAuthority().endsWith(":"))
      throw new IllegalArgumentException("APP_PUBLIC_ORIGIN must be an unambiguous origin");
    if (!secure && !("http".equalsIgnoreCase(uri.getScheme()) && loopback(host)))
      throw new IllegalArgumentException("APP_PUBLIC_ORIGIN requires HTTPS outside loopback");
    var cookie = new DefaultCookieSerializer();
    cookie.setCookieName("SESSION");
    cookie.setCookiePath("/api");
    cookie.setUseHttpOnlyCookie(true);
    cookie.setSameSite("Lax");
    cookie.setUseSecureCookie(secure);
    return cookie;
  }

  private static boolean loopback(String host) {
    if (host.equalsIgnoreCase("localhost")) return true;
    try {
      return java.net.InetAddress.ofLiteral(host).isLoopbackAddress();
    } catch (IllegalArgumentException error) {
      return false;
    }
  }
}
