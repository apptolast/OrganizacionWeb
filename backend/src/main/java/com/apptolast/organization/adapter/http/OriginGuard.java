package com.apptolast.organization.adapter.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.web.filter.OncePerRequestFilter;

public final class OriginGuard extends OncePerRequestFilter {
  private final String publicOrigin;
  private final ObjectMapper json;

  public OriginGuard(String publicOrigin, ObjectMapper json) {
    this.publicOrigin = publicOrigin;
    this.json = json;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String origin = request.getHeader("Origin");
    if (!Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod())
        && origin != null
        && !origin.equals(publicOrigin)) {
      response.setStatus(403);
      response.setContentType("application/problem+json");
      json.writeValue(
          response.getOutputStream(),
          ApiErrors.problem(
              403, "UNTRUSTED_ORIGIN", "El origen de la solicitud no está permitido."));
      return;
    }
    chain.doFilter(request, response);
  }
}
