package com.apptolast.organization.adapter.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(SessionRepositoryFilter.DEFAULT_ORDER - 1)
public final class SessionFailureFilter extends OncePerRequestFilter {
  private final ObjectMapper json;

  public SessionFailureFilter(ObjectMapper json) {
    this.json = json;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    try {
      chain.doFilter(request, response);
    } catch (org.springframework.dao.DataAccessException
        | org.springframework.transaction.TransactionException error) {
      if (response.isCommitted()) throw error;
      // SessionRepositoryFilter may have provisionally expired a cookie after DELETE failed.
      response.reset();
      response.setStatus(503);
      response.setHeader("Cache-Control", "no-store");
      response.setContentType("application/problem+json");
      json.writeValue(
          response.getOutputStream(),
          ApiErrors.problem(
              503,
              "SESSION_UNAVAILABLE",
              "No se puede confirmar la operación de sesión. Inténtalo de nuevo."));
    }
  }
}
