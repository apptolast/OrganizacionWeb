package com.apptolast.organization.adapter.http;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ExportHeadersFilterTest {
  @Test
  void s21_otherRoutesKeepTheirOriginalHeaders() throws Exception {
    var request = new MockHttpServletRequest("GET", "/api/v1/me/export/other");
    var response = new MockHttpServletResponse();
    new ExportHeadersFilter()
        .doFilter(
            request,
            response,
            (req, res) -> {
              var http = (jakarta.servlet.http.HttpServletResponse) res;
              http.reset();
              http.setHeader("Cache-Control", "no-store");
            });
    assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
    assertThat(response.getHeader("X-Content-Type-Options")).isNull();
  }

  @Test
  void s21_sessionFailureKeepsItsCodeAndExportHeadersAfterReset() throws Exception {
    var request = new MockHttpServletRequest("GET", "/api/v1/me/export");
    var response = new MockHttpServletResponse();
    var json = new ObjectMapper();
    new ExportHeadersFilter()
        .doFilter(
            request,
            response,
            (req, res) ->
                new SessionFailureFilter(json)
                    .doFilter(
                        req,
                        res,
                        (innerRequest, innerResponse) -> {
                          throw new org.springframework.dao.DataAccessResourceFailureException(
                              "private SQL");
                        }));
    assertThat(response.getStatus()).isEqualTo(503);
    assertThat(json.readTree(response.getContentAsString()).get("code").asText())
        .isEqualTo("SESSION_UNAVAILABLE");
    assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store, private, no-transform");
    assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
    assertThat(response.getHeader("Content-Disposition")).isNull();
    assertThat(response.getContentAsString()).doesNotContain("private SQL");
  }
}
