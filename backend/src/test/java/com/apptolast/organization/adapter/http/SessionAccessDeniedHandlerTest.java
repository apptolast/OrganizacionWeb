package com.apptolast.organization.adapter.http;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.core.context.SecurityContextHolder;

class SessionAccessDeniedHandlerTest {
  @AfterEach
  void clear() {
    SecurityContextHolder.clearContext();
  }

  @ParameterizedTest
  @CsvSource({
    "none,login,403",
    "none,private,401",
    "unauthenticated,private,401",
    "anonymous,private,401",
    "user,private,403",
    "anonymous,login,403",
    "user,login,403"
  })
  void distinguishesMissingIdentityFromInvalidCsrfWithoutLeakingSecrets(
      String identity, String route, int expected) throws Exception {
    org.springframework.security.core.Authentication authentication =
        switch (identity) {
          case "user" ->
              org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                  .authenticated("private-owner", "private-password", java.util.List.of());
          case "anonymous" ->
              new org.springframework.security.authentication.AnonymousAuthenticationToken(
                  "key",
                  "anonymousUser",
                  java.util.List.of(
                      new org.springframework.security.core.authority.SimpleGrantedAuthority(
                          "ROLE_ANONYMOUS")));
          case "unauthenticated" ->
              org.springframework.security.authentication.UsernamePasswordAuthenticationToken
                  .unauthenticated("private-owner", "private-password");
          default -> null;
        };
    SecurityContextHolder.getContext().setAuthentication(authentication);
    var request = new org.springframework.mock.web.MockHttpServletRequest();
    request.setServletPath(route.equals("login") ? "/api/session" : "/api/v1/projects");
    var response = new org.springframework.mock.web.MockHttpServletResponse();
    var json = new com.fasterxml.jackson.databind.ObjectMapper();
    new SessionAccessDeniedHandler(json)
        .handle(
            request,
            response,
            new org.springframework.security.web.csrf.MissingCsrfTokenException("secret-token"));
    assertThat(response.getStatus()).isEqualTo(expected);
    assertThat(response.getContentType()).isEqualTo("application/problem+json");
    assertThat(json.readTree(response.getContentAsString()).get("code").asText())
        .isEqualTo(expected == 401 ? "UNAUTHENTICATED" : "CSRF_INVALID");
    assertThat(json.readTree(response.getContentAsString()).get("title").asText())
        .isEqualTo(
            expected == 401
                ? "Identifícate para continuar."
                : "La protección de la solicitud ha caducado. Recarga antes de intentarlo de nuevo.");
    assertThat(response.getContentAsString())
        .doesNotContain("private-owner", "private-password", "secret-token");
  }
}
