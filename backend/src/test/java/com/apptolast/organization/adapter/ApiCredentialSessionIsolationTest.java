package com.apptolast.organization.adapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.apptolast.organization.adapter.config.SessionCookiePolicy;
import com.apptolast.organization.adapter.http.ApiCredentialSessionIdResolver;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.session.MapSession;
import org.springframework.session.SessionRepository;
import org.springframework.session.web.http.SessionRepositoryFilter;

class ApiCredentialSessionIsolationTest {
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "false,set",
    "false,expire",
    "true,set",
    "true,expire"
  })
  void s18_cookieWritesKeepPolicyAndAuthorizationSuppressesThem(boolean bearer, String action) {
    var resolver =
        new ApiCredentialSessionIdResolver(
            SessionCookiePolicy.create("https://organization.example"));
    var request = new MockHttpServletRequest("GET", "/api/v1/projects");
    if (bearer) request.addHeader("Authorization", "");
    var response = new MockHttpServletResponse();
    if (action.equals("set")) resolver.setSessionId(request, response, "human-session");
    else resolver.expireSession(request, response);
    String cookie = response.getHeader("Set-Cookie");
    if (bearer) {
      assertNull(cookie);
    } else {
      assertNotNull(cookie);
      assertTrue(cookie.startsWith("SESSION="));
      assertTrue(cookie.contains("Path=/api"));
      assertTrue(cookie.contains("HttpOnly"));
      assertTrue(cookie.contains("Secure"));
      assertTrue(cookie.contains("SameSite=Lax"));
      if (action.equals("expire")) assertTrue(cookie.contains("Max-Age=0"));
      else
        assertTrue(
            cookie.contains(
                Base64.getEncoder()
                    .encodeToString("human-session".getBytes(StandardCharsets.UTF_8))));
    }
  }

  @Test
  void s18_cookieOnlyRequestRetainsExistingSessionIdentity() throws Exception {
    @SuppressWarnings("unchecked")
    SessionRepository<MapSession> repository = mock(SessionRepository.class);
    var session = new MapSession("existing-cookie");
    session.setAttribute("identity", "human-owner");
    when(repository.findById("existing-cookie")).thenReturn(session);
    var filter = new SessionRepositoryFilter<>(repository);
    filter.setHttpSessionIdResolver(
        new ApiCredentialSessionIdResolver(
            SessionCookiePolicy.create("https://organization.example")));
    var request = new MockHttpServletRequest("GET", "/api/v1/projects");
    request.setCookies(
        new Cookie(
            "SESSION",
            Base64.getEncoder()
                .encodeToString("existing-cookie".getBytes(StandardCharsets.UTF_8))));
    filter.doFilter(
        request,
        new MockHttpServletResponse(),
        (wrapped, output) ->
            assertEquals(
                "human-owner",
                ((HttpServletRequest) wrapped).getSession(false).getAttribute("identity")));
    verify(repository).findById("existing-cookie");
  }

  @Test
  void s20_authorizationPreventsSessionLookupEvenWhenDownstreamAsksForExistingSession()
      throws Exception {
    @SuppressWarnings("unchecked")
    SessionRepository<MapSession> repository = mock(SessionRepository.class);
    var filter = new SessionRepositoryFilter<>(repository);
    filter.setHttpSessionIdResolver(
        new ApiCredentialSessionIdResolver(
            SessionCookiePolicy.create("https://organization.example")));
    var request = new MockHttpServletRequest("GET", "/api/v1/projects");
    request.addHeader("Authorization", "Bearer invalid-test-only");
    request.setCookies(
        new Cookie(
            "SESSION",
            Base64.getEncoder()
                .encodeToString("existing-cookie".getBytes(StandardCharsets.UTF_8))));
    var response = new MockHttpServletResponse();
    filter.doFilter(
        request,
        response,
        (wrapped, output) -> assertNull(((HttpServletRequest) wrapped).getSession(false)));
    verifyNoInteractions(repository);
    assertNull(response.getHeader("Set-Cookie"));
  }
}
