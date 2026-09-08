package com.apptolast.organization.adapter.http;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(SessionRepositoryFilter.DEFAULT_ORDER - 2)
public final class ExportHeadersFilter extends OncePerRequestFilter {
  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().equals(request.getContextPath() + "/api/v1/me/export");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    response.setHeader("Cache-Control", "no-store, private, no-transform");
    response.setHeader("X-Content-Type-Options", "nosniff");
    chain.doFilter(
        request,
        new jakarta.servlet.http.HttpServletResponseWrapper(response) {
          @Override
          public void reset() {
            super.reset();
            setHeader("Cache-Control", "no-store, private, no-transform");
            setHeader("X-Content-Type-Options", "nosniff");
          }

          @Override
          public void setHeader(String name, String value) {
            super.setHeader(
                name,
                name.equalsIgnoreCase("Cache-Control") ? "no-store, private, no-transform" : value);
          }
        });
  }
}
