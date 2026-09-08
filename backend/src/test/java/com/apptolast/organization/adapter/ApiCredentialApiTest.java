package com.apptolast.organization.adapter;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.apptolast.organization.adapter.config.SecurityConfiguration;
import com.apptolast.organization.adapter.http.ApiCredentialController;
import com.apptolast.organization.application.ApiCredentialCreation;
import com.apptolast.organization.application.CreateApiCredentialUseCase;
import com.apptolast.organization.domain.ApiCredential;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = ApiCredentialController.class,
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@Import(SecurityConfiguration.class)
class ApiCredentialApiTest {
  @Autowired MockMvc mvc;
  @MockitoBean CreateApiCredentialUseCase create;
  @MockitoBean com.apptolast.organization.application.ReadApiCredentialsUseCase read;
  @MockitoBean com.apptolast.organization.application.RevokeApiCredentialUseCase revoke;

  @Test
  void s1_incompatibleAcceptDoesNotCreateCredential() throws Exception {
    mvc.perform(
            put("/api/v1/me/api-credentials/ac87ee68-133b-4851-82e7-e3ec419df62a")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .accept("text/plain")
                .content(
                    "{\"name\":\"Automation\",\"scopes\":[\"projects:read\"],\"expiresInDays\":7}"))
        .andExpect(status().isNotAcceptable());
    verifyNoInteractions(create, read, revoke);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "list,503,STORAGE_UNAVAILABLE", "find,503,STORAGE_UNAVAILABLE",
    "revoke,503,STORAGE_UNAVAILABLE", "absent,404,API_CREDENTIAL_NOT_FOUND"
  })
  void s16_s17_readAndRevocationErrorsRemainPrivate(String operation, int expected, String code)
      throws Exception {
    var id = UUID.fromString("ac87ee68-133b-4851-82e7-e3ec419df62a");
    var failure =
        new com.apptolast.organization.application.StorageUnavailableException(
            new IllegalStateException("test-only-private-detail"));
    String path = "/api/v1/me/api-credentials";
    boolean write = operation.equals("revoke") || operation.equals("absent");
    if (operation.equals("list")) when(read.list("owner", null)).thenThrow(failure);
    else if (operation.equals("find")) when(read.find("owner", id)).thenThrow(failure);
    else if (operation.equals("revoke")) when(revoke.revoke("owner", id)).thenThrow(failure);
    else when(revoke.revoke("owner", id)).thenReturn(java.util.Optional.empty());
    if (!operation.equals("list")) path += "/" + id + (write ? "/revocation" : "");
    mvc.perform(
            request(
                    write
                        ? org.springframework.http.HttpMethod.PUT
                        : org.springframework.http.HttpMethod.GET,
                    path)
                .with(user("owner"))
                .with(csrf().asHeader()))
        .andExpect(status().is(expected))
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.code").value(code))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("test-only-private-detail"))));
    verifyNoInteractions(create);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "PUT,/api/v1/me/api-credentials,GET",
    "GET,/api/v1/me/api-credentials/ac87ee68-133b-4851-82e7-e3ec419df62a/revocation,PUT",
    "HEAD,/api/v1/me/api-credentials/ac87ee68-133b-4851-82e7-e3ec419df62a/revocation,PUT"
  })
  void s14_siblingMethodsReturn405AndExactAllow(String method, String path, String allow)
      throws Exception {
    mvc.perform(
            request(org.springframework.http.HttpMethod.valueOf(method), path)
                .with(user("owner"))
                .with(csrf().asHeader()))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(header().string("Allow", allow));
    verifyNoInteractions(create, read, revoke);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "POST,/api/v1/me/api-credentials",
    "POST,/api/v1/me/api-credentials/ac87ee68-133b-4851-82e7-e3ec419df62a",
    "POST,/api/v1/me/api-credentials/ac87ee68-133b-4851-82e7-e3ec419df62a/revocation",
    "PATCH,/api/v1/me/api-credentials/ac87ee68-133b-4851-82e7-e3ec419df62a",
    "DELETE,/api/v1/me/api-credentials/ac87ee68-133b-4851-82e7-e3ec419df62a"
  })
  void s14_unsupportedManagementMethodsReturn405WithoutBusiness(String method, String path)
      throws Exception {
    mvc.perform(
            request(org.springframework.http.HttpMethod.valueOf(method), path)
                .with(user("owner"))
                .with(csrf().asHeader()))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(header().exists("Allow"))
        .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    verifyNoInteractions(create, read, revoke);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "GET,AC87EE68-133B-4851-82E7-E3EC419DF62A",
    "PUT,1-1-1-1-1",
    "GET,invalid"
  })
  void s2_readAndRevokeUseCanonicalIdentity(String method, String id) throws Exception {
    String path = "/api/v1/me/api-credentials/" + id + (method.equals("PUT") ? "/revocation" : "");
    mvc.perform(
            request(org.springframework.http.HttpMethod.valueOf(method), path)
                .with(user("owner"))
                .with(csrf().asHeader()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("API_CREDENTIAL_INVALID"))
        .andExpect(jsonPath("$.errors[0].field").value("id"));
    verifyNoInteractions(create, read, revoke);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "GET,/api/v1/me/api-credentials,true",
    "GET,/api/v1/me/api-credentials/ac87ee68-133b-4851-82e7-e3ec419df62a,true",
    "PUT,/api/v1/me/api-credentials/ac87ee68-133b-4851-82e7-e3ec419df62a/revocation,true",
    "GET,/api/v1/me/api-credentials?owner=other,false",
    "GET,/api/v1/me/api-credentials?cursor=one&cursor=two,false",
    "GET,/api/v1/me/api-credentials/ac87ee68-133b-4851-82e7-e3ec419df62a?cursor=one,false",
    "PUT,/api/v1/me/api-credentials/ac87ee68-133b-4851-82e7-e3ec419df62a/revocation?extra=one,false"
  })
  void s14_readAndRevokeRejectUnexpectedBodiesOrQueries(String method, String path, boolean body)
      throws Exception {
    var request =
        request(org.springframework.http.HttpMethod.valueOf(method), path)
            .with(user("owner"))
            .with(csrf().asHeader());
    if (body) request.content(" ");
    mvc.perform(request)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("API_CREDENTIAL_INVALID"));
    verifyNoInteractions(create, read, revoke);
  }

  @Test
  void s15_revocationReturnsCommittedMetadataWithoutAnEtagOrSecret() throws Exception {
    var id = UUID.fromString("ac87ee68-133b-4851-82e7-e3ec419df62a");
    var metadata =
        new ApiCredential(
            id,
            "Revocada",
            List.of("tasks:read"),
            Instant.parse("2026-09-08T12:00:00Z"),
            Instant.parse("2026-09-15T12:00:00Z"),
            Instant.parse("2026-09-08T12:03:00.12Z"));
    when(revoke.revoke("owner", id)).thenReturn(java.util.Optional.of(metadata));
    mvc.perform(
            put("/api/v1/me/api-credentials/" + id + "/revocation")
                .with(user("owner"))
                .with(csrf().asHeader()))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.length()").value(6))
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.revokedAt").value("2026-09-08T12:03:00.120Z"))
        .andExpect(jsonPath("$.secret").doesNotExist());
    verify(revoke).revoke("owner", id);
    verifyNoInteractions(create, read);
  }

  @Test
  void s13_listingPreservesPageAndOpaqueCursorWithoutPerItemQueries() throws Exception {
    var id = UUID.fromString("ac87ee68-133b-4851-82e7-e3ec419df62a");
    var metadata =
        new ApiCredential(
            id,
            "Página",
            List.of("tasks:write"),
            Instant.parse("2026-09-08T12:00:00Z"),
            Instant.parse("2026-09-15T12:00:00Z"),
            null);
    when(read.list("owner", "previous-cursor"))
        .thenReturn(
            new com.apptolast.organization.domain.ApiCredentialPage(
                List.of(metadata), "next-cursor"));
    mvc.perform(
            get("/api/v1/me/api-credentials")
                .queryParam("cursor", "previous-cursor")
                .with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$.nextCursor").value("next-cursor"))
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].length()").value(6))
        .andExpect(jsonPath("$.items[0].id").value(id.toString()));
    verify(read).list("owner", "previous-cursor");
    verifyNoMoreInteractions(read);
    verifyNoInteractions(create, revoke);
  }

  @Test
  void s12_absentOrForeignResultUsesPrivate404() throws Exception {
    var id = UUID.fromString("ac87ee68-133b-4851-82e7-e3ec419df62a");
    when(read.find("owner", id)).thenReturn(java.util.Optional.empty());
    mvc.perform(get("/api/v1/me/api-credentials/" + id).with(user("owner")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("API_CREDENTIAL_NOT_FOUND"))
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.credential").doesNotExist());
    verify(read).find("owner", id);
    verifyNoInteractions(create, revoke);
  }

  @Test
  void s12_ownCredentialReadReturnsOnlyMetadata() throws Exception {
    var id = UUID.fromString("ac87ee68-133b-4851-82e7-e3ec419df62a");
    var metadata =
        new ApiCredential(
            id,
            "Lectura",
            List.of("projects:read"),
            Instant.parse("2026-09-08T12:00:00Z"),
            Instant.parse("2026-09-15T12:00:00Z"),
            Instant.parse("2026-09-08T11:00:00Z"));
    when(read.find("owner", id)).thenReturn(java.util.Optional.of(metadata));
    mvc.perform(get("/api/v1/me/api-credentials/" + id).with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.length()").value(6))
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.name").value("Lectura"))
        .andExpect(jsonPath("$.revokedAt").value("2026-09-08T11:00:00Z"))
        .andExpect(jsonPath("$.secret").doesNotExist());
    verify(read).find("owner", id);
    verifyNoInteractions(create, revoke);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "false,true,https://organization.example,401,UNAUTHENTICATED",
    "true,false,https://organization.example,403,CSRF_INVALID",
    "true,true,https://foreign.example,403,UNTRUSTED_ORIGIN"
  })
  void s18_cookieSecurityRetainsExistingRejections(
      boolean authenticated, boolean token, String origin, int expected, String code)
      throws Exception {
    var request =
        put("/api/v1/me/api-credentials/ac87ee68-133b-4851-82e7-e3ec419df62a")
            .header("Origin", origin)
            .contentType("application/json")
            .content("{}");
    if (authenticated) request.with(user("owner"));
    if (token) request.with(csrf().asHeader());
    mvc.perform(request).andExpect(status().is(expected)).andExpect(jsonPath("$.code").value(code));
    verifyNoInteractions(create);
  }

  @Test
  void s4_invalidStructureIncludesSafeFieldError() throws Exception {
    mvc.perform(
            put("/api/v1/me/api-credentials/ac87ee68-133b-4851-82e7-e3ec419df62a")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"private-test-value\":true}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("body"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("private-test-value"))));
    verifyNoInteractions(create);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "invalid,400,API_CREDENTIAL_INVALID", "conflict,409,API_CREDENTIAL_CONFLICT",
    "limit,409,API_CREDENTIAL_LIMIT", "storage,503,STORAGE_UNAVAILABLE"
  })
  void s17_realApplicationErrorsDoNotFabricateConfirmation(String kind, int status, String code)
      throws Exception {
    RuntimeException failure =
        switch (kind) {
          case "invalid" ->
              new com.apptolast.organization.domain.ApiCredentialInvalidException("name");
          case "conflict" ->
              new com.apptolast.organization.application.ApiCredentialConflictException();
          case "limit" -> new com.apptolast.organization.application.ApiCredentialLimitException();
          default ->
              new com.apptolast.organization.application.StorageUnavailableException(
                  new IllegalStateException("test-only-private-detail"));
        };
    when(create.create(anyString(), any(), anyString(), any(), anyInt())).thenThrow(failure);
    mvc.perform(
            put("/api/v1/me/api-credentials/ac87ee68-133b-4851-82e7-e3ec419df62a")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"name\":\"A\",\"scopes\":[\"projects:read\"],\"expiresInDays\":7}"))
        .andExpect(status().is(status))
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value(code))
        .andExpect(jsonPath("$.secret").doesNotExist())
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("test-only-private-detail"))));
  }

  @Test
  void s14_queryCannotSelectAnotherOwner() throws Exception {
    when(create.create(anyString(), any(), anyString(), any(), anyInt()))
        .thenReturn(new ApiCredentialCreation(null, "test-only"));
    mvc.perform(
            put("/api/v1/me/api-credentials/ac87ee68-133b-4851-82e7-e3ec419df62a?owner=other")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"name\":\"A\",\"scopes\":[\"projects:read\"],\"expiresInDays\":7}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("API_CREDENTIAL_INVALID"));
    verifyNoInteractions(create);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "null",
        "{}",
        "{\"name\":32,\"scopes\":[\"projects:read\"],\"expiresInDays\":7}",
        "{\"name\":\"A\",\"scopes\":[1],\"expiresInDays\":7}",
        "{\"name\":\"A\",\"scopes\":[\"projects:read\"],\"expiresInDays\":\"7\"}",
        "{\"name\":\"A\",\"scopes\":[\"projects:read\"],\"expiresInDays\":7.5}"
      })
  void s2_jsonTypesDoNotCoerceToCredentialIntent(String body) throws Exception {
    when(create.create(anyString(), any(), anyString(), any(), anyInt()))
        .thenReturn(new ApiCredentialCreation(null, "test-only"));
    mvc.perform(
            put("/api/v1/me/api-credentials/ac87ee68-133b-4851-82e7-e3ec419df62a")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("API_CREDENTIAL_INVALID"));
    verifyNoInteractions(create);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"AC87EE68-133B-4851-82E7-E3EC419DF62A", "1-1-1-1-1", "invalid"})
  void s2_identityMustBeCanonicalLowercase(String id) throws Exception {
    when(create.create(anyString(), any(), anyString(), any(), anyInt()))
        .thenReturn(new ApiCredentialCreation(null, "test-only"));
    mvc.perform(
            put("/api/v1/me/api-credentials/" + id)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"name\":\"A\",\"scopes\":[\"projects:read\"],\"expiresInDays\":7}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("API_CREDENTIAL_INVALID"))
        .andExpect(jsonPath("$.errors[0].field").value("id"));
    verifyNoInteractions(create);
  }

  @Test
  void s4_exact4096BytesRemainAccepted() throws Exception {
    var id = UUID.fromString("ac87ee68-133b-4851-82e7-e3ec419df62a");
    var metadata =
        new ApiCredential(
            id,
            "A",
            List.of("projects:read"),
            Instant.parse("2026-09-08T12:00:00Z"),
            Instant.parse("2026-09-15T12:00:00Z"),
            null);
    when(create.create("owner", id, "A", List.of("projects:read"), 7))
        .thenReturn(new ApiCredentialCreation(metadata, "test-only"));
    String body = "{\"name\":\"A\",\"scopes\":[\"projects:read\"],\"expiresInDays\":7}";
    body += " ".repeat(4096 - body.length());
    mvc.perform(
            put("/api/v1/me/api-credentials/" + id)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.credential.createdAt").value("2026-09-08T12:00:00Z"));
    verify(create).create("owner", id, "A", List.of("projects:read"), 7);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "{\"name\":\"A\",\"scopes\":[\"projects:read\"],\"expiresInDays\":7,\"owner\":\"another\"}",
        "{\"name\":\"A\",\"name\":\"B\",\"scopes\":[\"projects:read\"],\"expiresInDays\":7}",
        "{\"name\":\"A\",\"scopes\":[\"projects:read\"],\"expiresInDays\":7} {}"
      })
  void s4_closedJsonRejectsUnknownDuplicateAndTrailingValues(String body) throws Exception {
    when(create.create(anyString(), any(), anyString(), any(), anyInt()))
        .thenReturn(new ApiCredentialCreation(null, "test-only"));
    mvc.perform(
            put("/api/v1/me/api-credentials/ac87ee68-133b-4851-82e7-e3ec419df62a")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("API_CREDENTIAL_INVALID"));
    verifyNoInteractions(create);
  }

  @Test
  void s4_4097BytesRejectBeforeCallingCreation() throws Exception {
    String body = "{\"name\":\"A\",\"scopes\":[\"projects:read\"],\"expiresInDays\":7}";
    body += " ".repeat(4097 - body.length());
    when(create.create(anyString(), any(), anyString(), any(), anyInt()))
        .thenReturn(new ApiCredentialCreation(null, "test-only"));
    mvc.perform(
            put("/api/v1/me/api-credentials/ac87ee68-133b-4851-82e7-e3ec419df62a")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.code").value("API_CREDENTIAL_TOO_LARGE"))
        .andExpect(header().string("Cache-Control", "no-store"));
    verifyNoInteractions(create);
  }

  @Test
  void s9_confirmedReplayReturns200WithoutSecret() throws Exception {
    var id = UUID.fromString("ac87ee68-133b-4851-82e7-e3ec419df62a");
    var metadata =
        new ApiCredential(
            id,
            "Antigua",
            List.of("projects:read"),
            Instant.parse("2025-01-01T00:00:00Z"),
            Instant.parse("2025-01-08T00:00:00Z"),
            Instant.parse("2025-01-02T00:00:00Z"));
    when(create.create("owner", id, "Antigua", List.of("projects:read"), 7))
        .thenReturn(new ApiCredentialCreation(metadata, null));
    mvc.perform(
            put("/api/v1/me/api-credentials/" + id)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(
                    """
                {"name":"Antigua","scopes":["projects:read"],"expiresInDays":7}
                """))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$.secret").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.credential.revokedAt").value("2025-01-02T00:00:00Z"));
  }

  @Test
  void s1_firstCommittedCreationReturnsLocationAndSecret() throws Exception {
    var id = UUID.fromString("ac87ee68-133b-4851-82e7-e3ec419df62a");
    var scopes = List.of("projects:read");
    var metadata =
        new ApiCredential(
            id,
            "Mi integración",
            scopes,
            Instant.parse("2026-09-08T12:00:00.123456Z"),
            Instant.parse("2026-10-08T12:00:00.123456Z"),
            null);
    String secret = "owp_" + id + "." + "A".repeat(43);
    when(create.create("owner", id, "Mi integración", scopes, 30))
        .thenReturn(new ApiCredentialCreation(metadata, secret));
    mvc.perform(
            put("/api/v1/me/api-credentials/" + id)
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("Origin", "https://organization.example")
                .contentType("application/json")
                .content(
                    """
                {"name":"Mi integración","scopes":["projects:read"],"expiresInDays":30}
                """))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/me/api-credentials/" + id))
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$.secret").value(secret))
        .andExpect(jsonPath("$.credential.length()").value(6))
        .andExpect(jsonPath("$.credential.id").value(id.toString()))
        .andExpect(jsonPath("$.credential.name").value("Mi integración"))
        .andExpect(jsonPath("$.credential.scopes[0]").value("projects:read"))
        .andExpect(jsonPath("$.credential.createdAt").value("2026-09-08T12:00:00.123456Z"))
        .andExpect(jsonPath("$.credential.expiresAt").value("2026-10-08T12:00:00.123456Z"))
        .andExpect(jsonPath("$.credential.revokedAt").isEmpty());
    verify(create).create("owner", id, "Mi integración", scopes, 30);
    verifyNoMoreInteractions(create);
  }
}
