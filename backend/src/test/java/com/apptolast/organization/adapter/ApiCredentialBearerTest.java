package com.apptolast.organization.adapter;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.apptolast.organization.adapter.config.SecurityConfiguration;
import com.apptolast.organization.adapter.http.ProjectReadController;
import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.ProjectPage;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = {
      ProjectReadController.class,
      com.apptolast.organization.adapter.http.IntegrationOpenApiController.class
    },
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@Import({
  SecurityConfiguration.class,
  org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration.class
})
class ApiCredentialBearerTest {
  @Autowired MockMvc mvc;

  @MockitoBean
  org.springframework.session.SessionRepository<org.springframework.session.MapSession> sessions;

  @MockitoBean AuthenticateApiCredentialUseCase authenticate;
  @MockitoBean ConsumeApiQuotaUseCase quota;
  @MockitoBean ReadProjectsUseCase projects;
  private final ApiCredentialAccess access =
      new ApiCredentialAccess(
          UUID.fromString("ac87ee68-133b-4851-82e7-e3ec419df62a"),
          "owner",
          List.of("projects:read"));
  private final String token =
      "owp_ac87ee68-133b-4851-82e7-e3ec419df62a_AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"", "Basic abc", "Bearer", "Bearer  abc", "Bearer abc,def", "Bearer invalid"})
  void s19_malformedOrInvalidBearerNeverFallsBackToCookie(String header) throws Exception {
    when(authenticate.authenticate(anyString())).thenThrow(new ApiUnauthenticatedException());
    mvc.perform(
            get("/api/v1/projects")
                .header("Authorization", header)
                .with(
                    org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.user("cookie-owner")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("API_UNAUTHENTICATED"))
        .andExpect(header().string("WWW-Authenticate", "Bearer"))
        .andExpect(header().doesNotExist("Set-Cookie"));
    verifyNoInteractions(quota, projects);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "GET,/api/v1/projects",
    "POST,/api/v1/projects",
    "GET,/api/v1/projects/p",
    "PUT,/api/v1/projects/p",
    "GET,/api/v1/projects/p/tasks",
    "POST,/api/v1/projects/p/tasks",
    "GET,/api/v1/projects/p/tasks/t",
    "GET,/api/v1/projects/p/tasks/t/status",
    "GET,/api/v1/projects/p/tasks/t/parent",
    "GET,/api/v1/projects/p/tasks/t/subtasks",
    "GET,/api/v1/today",
    "GET,/api/v1/projects/p/tasks/t/blocks",
    "GET,/api/v1/projects/p/tasks/t/blocks/b",
    "GET,/api/v1/projects/p/tasks/t/blocks/b/state",
    "GET,/api/v1/projects/p/tasks/t/blocks/by-request/r",
    "GET,/api/v1/history",
    "GET,/api/v1/weekly-review",
    "GET,/api/v1/projects/p/tasks/t/history"
  })
  void s23_eachOperationRequiresItsOwnScopeBeforeQuota(String method, String path)
      throws Exception {
    when(authenticate.authenticate(token))
        .thenReturn(new ApiCredentialAccess(access.id(), "owner", List.of()));
    mvc.perform(
            request(org.springframework.http.HttpMethod.valueOf(method), path)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("API_SCOPE_DENIED"));
    verify(authenticate).authenticate(token);
    verifyNoInteractions(quota, projects);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
  void s24_authenticationPrecedesOriginAndOriginPrecedesScope(boolean valid) throws Exception {
    if (valid) when(authenticate.authenticate(token)).thenReturn(access);
    else when(authenticate.authenticate(token)).thenThrow(new ApiUnauthenticatedException());
    mvc.perform(
            post("/api/v1/projects")
                .header("Authorization", "Bearer " + token)
                .header("Origin", "https://foreign.example"))
        .andExpect(status().is(valid ? 403 : 401))
        .andExpect(jsonPath("$.code").value(valid ? "UNTRUSTED_ORIGIN" : "API_UNAUTHENTICATED"));
    verifyNoInteractions(quota, projects);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "authenticate,503,STORAGE_UNAVAILABLE",
    "quota,503,STORAGE_UNAVAILABLE",
    "limited,429,API_RATE_LIMITED",
    "revoked,401,API_UNAUTHENTICATED"
  })
  void s25_s27_admissionFailuresStopBeforeBusiness(String stage, int status, String code)
      throws Exception {
    var failure =
        new StorageUnavailableException(new IllegalStateException("test-private-database-detail"));
    if (stage.equals("authenticate")) when(authenticate.authenticate(token)).thenThrow(failure);
    else {
      when(authenticate.authenticate(token)).thenReturn(access);
      if (stage.equals("quota")) doThrow(failure).when(quota).consume(access);
      else if (stage.equals("limited"))
        doThrow(new ApiRateLimitedException(17)).when(quota).consume(access);
      else doThrow(new ApiUnauthenticatedException()).when(quota).consume(access);
    }
    var result =
        mvc.perform(get("/api/v1/projects").header("Authorization", "Bearer " + token))
            .andExpect(status().is(status))
            .andExpect(jsonPath("$.code").value(code))
            .andExpect(header().doesNotExist("Set-Cookie"))
            .andExpect(
                content()
                    .string(
                        org.hamcrest.Matchers.not(
                            org.hamcrest.Matchers.containsString("test-private-database-detail"))));
    if (stage.equals("limited")) result.andExpect(header().string("Retry-After", "17"));
    if (stage.equals("revoked")) result.andExpect(header().string("WWW-Authenticate", "Bearer"));
    verifyNoInteractions(projects);
    if (stage.equals("authenticate")) verifyNoInteractions(quota);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "HEAD,/api/v1/projects",
    "OPTIONS,/api/v1/projects",
    "DELETE,/api/v1/projects/p",
    "PUT,/api/v1/projects/p/status",
    "PUT,/api/v1/projects/p/tasks/t/status",
    "POST,/api/v1/projects/p/tasks/t/subtasks",
    "GET,/api/v1/me/export",
    "POST,/api/v1/me/import/preview",
    "GET,/api/v1/me/api-credentials",
    "GET,/api/session",
    "POST,/api/session",
    "POST,/api/session/logout",
    "GET,/api/v1/not-allowed",
    "HEAD,/api/v1/integration-openapi.json",
    "GET,/api/v1/projects/",
    "GET,/api/v1/projects/p/tasks/",
    "POST,/api/v1/projects/p/tasks/t/blocks",
    "GET,/api/v1/work-sessions/active",
    "GET,/api/v1/me/appearance"
  })
  void s23_allowlistDeniesOtherMethodsAndRoutesEvenWithAllScopes(String method, String path)
      throws Exception {
    when(authenticate.authenticate(token))
        .thenReturn(
            new ApiCredentialAccess(
                access.id(),
                "owner",
                List.of(
                    "projects:read",
                    "projects:write",
                    "tasks:read",
                    "tasks:write",
                    "agenda:read",
                    "history:read")));
    mvc.perform(
            request(org.springframework.http.HttpMethod.valueOf(method), path)
                .header("Authorization", "Bearer " + token)
                .cookie(new jakarta.servlet.http.Cookie("SESSION", "c2Vzc2lvbi1pZA==")))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist("Set-Cookie"));
    verifyNoInteractions(quota, projects, sessions);
  }

  @Test
  void s19_duplicateAuthorizationIsRejectedBeforeAuthentication() throws Exception {
    mvc.perform(
            get("/api/v1/projects")
                .header("Authorization", "Bearer " + token, "Bearer " + token)
                .cookie(new jakarta.servlet.http.Cookie("SESSION", "c2Vzc2lvbi1pZA==")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("API_UNAUTHENTICATED"))
        .andExpect(header().string("WWW-Authenticate", "Bearer"))
        .andExpect(header().doesNotExist("Set-Cookie"));
    verifyNoInteractions(authenticate, quota, projects, sessions);
  }

  @Test
  void s20_realSessionFilterDoesNotConsultCookieOrCreateSessionForBearer() throws Exception {
    when(authenticate.authenticate(token)).thenReturn(access);
    when(projects.list("owner", null)).thenReturn(new ProjectPage(List.of(), null));
    mvc.perform(
            get("/api/v1/projects")
                .header("Authorization", "Bearer " + token)
                .cookie(new jakarta.servlet.http.Cookie("SESSION", "c2Vzc2lvbi1pZA==")))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist("Set-Cookie"));
    verifyNoInteractions(sessions);
  }

  @Test
  void s32_openApiAuthenticatesWithoutScopesOrQuota() throws Exception {
    when(authenticate.authenticate(token))
        .thenReturn(new ApiCredentialAccess(access.id(), "owner", List.of()));
    mvc.perform(get("/api/v1/integration-openapi.json").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openapi").value("3.1.0"))
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(header().doesNotExist("Set-Cookie"));
    verify(authenticate).authenticate(token);
    verifyNoInteractions(quota, projects);
  }

  @Test
  void s20_bearerAuthenticatesAdmitsThenReadsAsOwnerWithoutCookie() throws Exception {
    when(authenticate.authenticate(token)).thenReturn(access);
    when(projects.list("owner", null)).thenReturn(new ProjectPage(List.of(), null));
    mvc.perform(get("/api/v1/projects").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isEmpty())
        .andExpect(header().doesNotExist("Set-Cookie"));
    var order = inOrder(authenticate, quota, projects);
    order.verify(authenticate).authenticate(token);
    order.verify(quota).consume(access);
    order.verify(projects).list("owner", null);
  }
}
