package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.ApiCredentialAccess;
import com.apptolast.organization.application.ApiRateLimitedException;
import com.apptolast.organization.application.ApiUnauthenticatedException;
import com.apptolast.organization.application.AuthenticateApiCredentialUseCase;
import com.apptolast.organization.application.ConsumeApiQuotaUseCase;
import com.apptolast.organization.application.StorageUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

public final class ApiCredentialBearerFilter extends OncePerRequestFilter {
  private record Permission(RequestMatcher request, String scope) {}

  private static final List<Permission> PERMISSIONS =
      List.of(
          new Permission(
              PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/api/v1/projects"),
              "projects:read"),
          new Permission(
              PathPatternRequestMatcher.withDefaults()
                  .matcher(HttpMethod.GET, "/api/v1/projects/{segment0}"),
              "projects:read"),
          new Permission(
              PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/v1/projects"),
              "projects:write"),
          new Permission(
              PathPatternRequestMatcher.withDefaults()
                  .matcher(HttpMethod.PUT, "/api/v1/projects/{segment0}"),
              "projects:write"),
          new Permission(
              PathPatternRequestMatcher.withDefaults()
                  .matcher(HttpMethod.GET, "/api/v1/projects/{segment0}/tasks"),
              "tasks:read"),
          new Permission(
              PathPatternRequestMatcher.withDefaults()
                  .matcher(HttpMethod.GET, "/api/v1/projects/{segment0}/tasks/{segment1}"),
              "tasks:read"),
          new Permission(
              PathPatternRequestMatcher.withDefaults()
                  .matcher(HttpMethod.GET, "/api/v1/projects/{segment0}/tasks/{segment1}/status"),
              "tasks:read"),
          new Permission(
              PathPatternRequestMatcher.withDefaults()
                  .matcher(HttpMethod.GET, "/api/v1/projects/{segment0}/tasks/{segment1}/parent"),
              "tasks:read"),
          new Permission(
              PathPatternRequestMatcher.withDefaults()
                  .matcher(HttpMethod.GET, "/api/v1/projects/{segment0}/tasks/{segment1}/subtasks"),
              "tasks:read"),
          new Permission(
              PathPatternRequestMatcher.withDefaults()
                  .matcher(HttpMethod.POST, "/api/v1/projects/{segment0}/tasks"),
              "tasks:write"),
          new Permission(
              PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/api/v1/today"),
              "agenda:read"),
          new Permission(
              PathPatternRequestMatcher.withDefaults()
                  .matcher(HttpMethod.GET, "/api/v1/projects/{segment0}/tasks/{segment1}/blocks"),
              "agenda:read"),
          new Permission(
              PathPatternRequestMatcher.withDefaults()
                  .matcher(
                      HttpMethod.GET,
                      "/api/v1/projects/{segment0}/tasks/{segment1}/blocks/{segment2}"),
              "agenda:read"),
          new Permission(
              PathPatternRequestMatcher.withDefaults()
                  .matcher(
                      HttpMethod.GET,
                      "/api/v1/projects/{segment0}/tasks/{segment1}/blocks/{segment2}/state"),
              "agenda:read"),
          new Permission(
              PathPatternRequestMatcher.withDefaults()
                  .matcher(
                      HttpMethod.GET,
                      "/api/v1/projects/{segment0}/tasks/{segment1}/blocks/by-request/{segment2}"),
              "agenda:read"),
          new Permission(
              PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/api/v1/history"),
              "history:read"),
          new Permission(
              PathPatternRequestMatcher.withDefaults()
                  .matcher(HttpMethod.GET, "/api/v1/weekly-review"),
              "history:read"),
          new Permission(
              PathPatternRequestMatcher.withDefaults()
                  .matcher(HttpMethod.GET, "/api/v1/projects/{segment0}/tasks/{segment1}/history"),
              "history:read"));
  private final ObjectProvider<AuthenticateApiCredentialUseCase> authenticate;
  private final ObjectProvider<ConsumeApiQuotaUseCase> quota;
  private final ObjectMapper json;
  private final String publicOrigin;

  public ApiCredentialBearerFilter(
      ObjectProvider<AuthenticateApiCredentialUseCase> authenticate,
      ObjectProvider<ConsumeApiQuotaUseCase> quota,
      ObjectMapper json,
      String publicOrigin) {
    this.authenticate = authenticate;
    this.quota = quota;
    this.json = json;
    this.publicOrigin = publicOrigin;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    ApiCredentialAccess access;
    try {
      var headers = java.util.Collections.list(request.getHeaders("Authorization"));
      if (headers.size() != 1) throw new ApiUnauthenticatedException();
      String header = headers.getFirst();
      if (header.length() <= 7 || !header.regionMatches(true, 0, "Bearer ", 0, 7))
        throw new ApiUnauthenticatedException();
      String token = header.substring(7);
      if (token.chars().anyMatch(c -> Character.isWhitespace(c) || c == ','))
        throw new ApiUnauthenticatedException();
      access = authenticate.getObject().authenticate(token);
      String origin = request.getHeader("Origin");
      if (!java.util.Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod())
          && origin != null
          && !origin.equals(publicOrigin)) {
        problem(response, 403, "UNTRUSTED_ORIGIN", "El origen de la solicitud no está permitido.");
        return;
      }
      boolean openApi =
          request.getMethod().equals("GET")
              && request
                  .getRequestURI()
                  .equals(request.getContextPath() + "/api/v1/integration-openapi.json");
      var permission =
          PERMISSIONS.stream().filter(rule -> rule.request().matches(request)).findFirst();
      if (!openApi
          && (permission.isEmpty() || !access.scopes().contains(permission.get().scope()))) {
        problem(response, 403, "API_SCOPE_DENIED", "La credencial no permite esta operación.");
        return;
      }
      if (!openApi) quota.getObject().consume(access);
    } catch (ApiRateLimitedException error) {
      response.setHeader("Retry-After", Integer.toString(error.retryAfterSeconds()));
      problem(response, 429, "API_RATE_LIMITED", "Se ha alcanzado el límite de solicitudes.");
      return;
    } catch (StorageUnavailableException error) {
      problem(response, 503, "STORAGE_UNAVAILABLE", "El almacenamiento no está disponible.");
      return;
    } catch (ApiUnauthenticatedException error) {
      response.setHeader("WWW-Authenticate", "Bearer");
      problem(response, 401, "API_UNAUTHENTICATED", "Credencial no válida.");
      return;
    }
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(access.owner(), null, List.of()));
    SecurityContextHolder.setContext(context);
    chain.doFilter(request, response);
  }

  private void problem(HttpServletResponse response, int status, String code, String message)
      throws IOException {
    response.setStatus(status);
    response.setHeader("Cache-Control", "no-store");
    response.setContentType("application/problem+json");
    json.writeValue(response.getOutputStream(), ApiErrors.problem(status, code, message));
  }
}
