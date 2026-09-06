package com.apptolast.organization.adapter.http;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.web.csrf.DefaultCsrfToken;

class SessionControllerTest {
  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void sessionRepresentationContainsOnlyIdentityAndOpaqueCsrf(boolean authenticated) {
    java.security.Principal principal = authenticated ? () -> "owner" : null;
    var result =
        new SessionController()
            .session(principal, new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "opaque"));
    assertThat(result)
        .hasSize(4)
        .containsEntry("authenticated", authenticated)
        .containsEntry("username", authenticated ? "owner" : null)
        .containsEntry("csrfToken", "opaque")
        .containsEntry("csrfHeaderName", "X-CSRF-TOKEN");
  }
}
