package com.apptolast.organization.adapter.http;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SessionFailureFilterTest {
  @ParameterizedTest
  @ValueSource(strings = {"sql", "transaction", "committed", "success", "unexpected"})
  void sessionBoundaryResetsOnlyUncommittedStorageFailures(String outcome) throws Exception {
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    var filter = new SessionFailureFilter(json);
    var request =
        new org.springframework.mock.web.MockHttpServletRequest("POST", "/api/session/logout");
    var response = new org.springframework.mock.web.MockHttpServletResponse();
    RuntimeException failure =
        outcome.equals("transaction")
            ? new org.springframework.transaction.TransactionSystemException("secret SQL")
            : outcome.equals("unexpected")
                ? new IllegalStateException("secret internal")
                : new org.springframework.dao.DataAccessResourceFailureException("secret SQL");
    jakarta.servlet.FilterChain chain =
        (req, res) -> {
          response.setStatus(204);
          response.setHeader("Set-Cookie", "SESSION=; Max-Age=0; Path=/api");
          if (outcome.equals("success")) return;
          if (outcome.equals("committed")) response.flushBuffer();
          throw failure;
        };
    if (outcome.equals("committed") || outcome.equals("unexpected")) {
      org.assertj.core.api.Assertions.assertThatThrownBy(
              () -> filter.doFilter(request, response, chain))
          .isSameAs(failure);
      assertThat(response.getStatus()).isEqualTo(204);
    } else {
      filter.doFilter(request, response, chain);
      if (outcome.equals("success")) {
        assertThat(response.getStatus()).isEqualTo(204);
        assertThat(response.getHeader("Set-Cookie")).contains("Max-Age=0");
      } else {
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getHeader("Set-Cookie")).isNull();
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getContentType()).isEqualTo("application/problem+json");
        assertThat(json.readTree(response.getContentAsString()).get("code").asText())
            .isEqualTo("SESSION_UNAVAILABLE");
        assertThat(response.getContentAsString()).doesNotContain("secret", "SQL");
      }
    }
  }
}
