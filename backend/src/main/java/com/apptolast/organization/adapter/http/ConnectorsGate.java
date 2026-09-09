package com.apptolast.organization.adapter.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Sin clave de conectores, las rutas del calendario externo responden 503 antes de leer el cuerpo o
 * tocar la base de datos. Va detrás de la autorización, así que la falta de sesión, de token CSRF o
 * de Origin de confianza siguen decidiendo antes.
 */
public final class ConnectorsGate extends OncePerRequestFilter {
  public static final String PREFIX = "/api/v1/me/external-calendar";

  private final boolean enabled;
  private final ObjectMapper json;

  public ConnectorsGate(String configuredKey, ObjectMapper json) {
    this.enabled = configuredKey != null && !configuredKey.isBlank();
    this.json = json;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return enabled || !isConnectorRoute(request.getRequestURI());
  }

  /** La ruta exacta y lo que cuelga de ella; nunca otra que solo empiece igual. */
  private static boolean isConnectorRoute(String uri) {
    return uri.equals(PREFIX) || uri.startsWith(PREFIX + "/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    response.setStatus(503);
    response.setContentType("application/problem+json");
    response.setHeader("Cache-Control", "no-store");
    json.writeValue(
        response.getOutputStream(),
        ApiErrors.problem(
            503,
            "CONNECTORS_DISABLED",
            "Los conectores externos no están disponibles en esta instalación."));
  }
}
