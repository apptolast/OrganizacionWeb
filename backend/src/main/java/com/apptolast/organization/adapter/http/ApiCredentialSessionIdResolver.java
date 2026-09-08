package com.apptolast.organization.adapter.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.HttpSessionIdResolver;

public final class ApiCredentialSessionIdResolver implements HttpSessionIdResolver {
  private final CookieHttpSessionIdResolver cookies = new CookieHttpSessionIdResolver();

  public ApiCredentialSessionIdResolver(CookieSerializer serializer) {
    cookies.setCookieSerializer(serializer);
  }

  @Override
  public List<String> resolveSessionIds(HttpServletRequest request) {
    return request.getHeader("Authorization") != null
        ? List.of()
        : cookies.resolveSessionIds(request);
  }

  @Override
  public void setSessionId(HttpServletRequest request, HttpServletResponse response, String id) {
    if (request.getHeader("Authorization") == null) cookies.setSessionId(request, response, id);
  }

  @Override
  public void expireSession(HttpServletRequest request, HttpServletResponse response) {
    if (request.getHeader("Authorization") == null) cookies.expireSession(request, response);
  }
}
