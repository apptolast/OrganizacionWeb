package com.apptolast.organization.adapter.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

class SessionCookiePolicyTest {
  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "http://example.com",
        "http://192.0.2.1",
        "https://example.com:",
        "https://user@example.com",
        "https://example.com/path",
        "https://example.com?query",
        "https://example.com#fragment",
        "https://example.com:0",
        "https://example.com:65536",
        "ftp://example.com",
        "not a URI",
        "https://",
        "http://127.0.0.1.evil.example"
      })
  void s13_rejectsAmbiguousOrInsecurePublicOrigins(String origin) {
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> SessionCookiePolicy.create(origin))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @CsvSource({
    "https://organization.example,true",
    "https://organization.example:65535,true",
    "http://localhost,false",
    "http://127.0.0.1:8080,false",
    "http://127.0.0.2:1,false",
    "http://[::1]:8080,false"
  })
  void s13_cookieAttributesComeOnlyFromConfiguredOrigin(String origin, boolean secure) {
    var serializer = SessionCookiePolicy.create(origin);
    var request = new org.springframework.mock.web.MockHttpServletRequest();
    request.setSecure(!secure);
    request.addHeader("X-Forwarded-Proto", secure ? "http" : "https");
    var response = new org.springframework.mock.web.MockHttpServletResponse();
    serializer.writeCookieValue(
        new org.springframework.session.web.http.CookieSerializer.CookieValue(
            request, response, "synthetic-session"));
    String header = response.getHeader("Set-Cookie");
    assertThat(header).contains("SESSION=", "Path=/api", "HttpOnly", "SameSite=Lax");
    assertThat(header.contains("; Secure")).isEqualTo(secure);
  }
}
