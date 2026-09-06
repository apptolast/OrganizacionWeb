package com.apptolast.organization.adapter.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

public final class SessionAccessDeniedHandler implements AccessDeniedHandler {
  private final ObjectMapper json;

  public SessionAccessDeniedHandler(ObjectMapper json) {
    this.json = json;
  }

  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException error)
      throws IOException {
    var authentication =
        org.springframework.security.core.context.SecurityContextHolder.getContext()
            .getAuthentication();
    boolean anonymous =
        authentication == null
            || !authentication.isAuthenticated()
            || authentication
                instanceof org.springframework.security.authentication.AnonymousAuthenticationToken;
    boolean unauthenticated = !request.getServletPath().equals("/api/session") && anonymous;
    int status = unauthenticated ? 401 : 403;
    response.setStatus(status);
    response.setContentType("application/problem+json");
    json.writeValue(
        response.getOutputStream(),
        ApiErrors.problem(
            status,
            unauthenticated ? "UNAUTHENTICATED" : "CSRF_INVALID",
            unauthenticated
                ? "Identifícate para continuar."
                : "La protección de la solicitud ha caducado. Recarga antes de intentarlo de nuevo."));
  }
}
