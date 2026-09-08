package com.apptolast.organization.adapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(
    properties = {
      "app.auth.username=http-owner", "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example", "app.publisher.enabled=false"
    })
@AutoConfigureMockMvc
@org.springframework.context.annotation.Import(ApiCredentialHttpPersistenceTest.FixedClock.class)
class ApiCredentialHttpPersistenceTest {
  @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
  static class FixedClock {
    @org.springframework.context.annotation.Bean
    @org.springframework.context.annotation.Primary
    java.time.Clock integrationClock() {
      return java.time.Clock.fixed(
          java.time.Instant.parse("2026-09-08T12:00:10Z"), java.time.ZoneOffset.UTC);
    }
  }

  static final class Database {
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:17.9-alpine");

    static {
      PG.start();
    }
  }

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry properties) {
    var pg = Database.PG;
    properties.add("spring.datasource.url", pg::getJdbcUrl);
    properties.add("spring.datasource.username", pg::getUsername);
    properties.add("spring.datasource.password", pg::getPassword);
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  @Autowired org.springframework.security.core.userdetails.UserDetailsService users;
  @MockitoSpyBean JdbcIndexedSessionRepository sessions;
  @Autowired com.apptolast.organization.application.CreateApiCredentialUseCase create;
  @Autowired com.apptolast.organization.application.RevokeApiCredentialUseCase revoke;

  @org.junit.jupiter.api.BeforeEach
  void cleanOnlyThisFixturesOwner() {
    jdbc.update(
        "DELETE FROM api_credential_quotas WHERE credential_id IN (SELECT id FROM api_credentials WHERE owner_id IN ('http-owner','orphan-owner'))");
    jdbc.update("DELETE FROM api_credentials WHERE owner_id IN ('http-owner','orphan-owner')");
    jdbc.update("DELETE FROM api_owner_quotas WHERE owner_id IN ('http-owner','orphan-owner')");
    jdbc.update("DELETE FROM projects WHERE owner_id='http-owner'");
    jdbc.update("DELETE FROM spring_session WHERE principal_name='http-owner'");
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"invalid", "revoked", "disabled", "orphan"})
  void s20_s31_realInvalidRevokedOrDisabledCredentialNeverUsesCookieOrQuota(String state)
      throws Exception {
    var id = UUID.randomUUID();
    var token =
        create
            .create(
                state.equals("orphan") ? "orphan-owner" : "http-owner",
                id,
                "Rejected credential",
                java.util.List.of("projects:read"),
                7)
            .secret();
    var manager = (org.springframework.security.provisioning.InMemoryUserDetailsManager) users;
    var original = manager.loadUserByUsername("http-owner");
    if (state.equals("revoked")) revoke.revoke("http-owner", id);
    if (state.equals("invalid"))
      token = token.substring(0, 41) + (token.charAt(41) == 'A' ? "B" : "A") + token.substring(42);
    if (state.equals("disabled"))
      manager.updateUser(
          org.springframework.security.core.userdetails.User.withUserDetails(original)
              .disabled(true)
              .build());
    clearInvocations(sessions);
    try {
      mvc.perform(
              get("/api/v1/projects")
                  .header("Authorization", "Bearer " + token)
                  .cookie(new jakarta.servlet.http.Cookie("SESSION", "c2Vzc2lvbi1pZA==")))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.code").value("API_UNAUTHENTICATED"))
          .andExpect(header().string("WWW-Authenticate", "Bearer"))
          .andExpect(header().doesNotExist("Set-Cookie"));
      verifyNoInteractions(sessions);
      assertEquals(
          0,
          jdbc.queryForObject(
              "SELECT count(*) FROM api_credential_quotas WHERE credential_id=?",
              Integer.class,
              id));
      assertEquals(
          0,
          jdbc.queryForObject(
              "SELECT count(*) FROM api_owner_quotas WHERE owner_id='http-owner'", Integer.class));
    } finally {
      if (state.equals("disabled")) manager.updateUser(original);
    }
  }

  @Test
  void s21_bearerLogoutIsDeniedWithoutChangingCookieSessionAndCookieLogoutStillWorks()
      throws Exception {
    var login =
        mvc.perform(
                post("/api/session")
                    .with(csrf().asHeader())
                    .header("Origin", "https://organization.example")
                    .param("username", "http-owner")
                    .param("password", "test-only-secret"))
            .andExpect(status().isNoContent())
            .andReturn()
            .getResponse();
    var cookie = login.getCookie("SESSION");
    assertNotNull(cookie);
    var token =
        create
            .create(
                "http-owner",
                UUID.randomUUID(),
                "Logout separation",
                java.util.List.of("projects:read"),
                7)
            .secret();
    var before =
        jdbc.queryForList(
            "SELECT row_to_json(s)::text || xmin::text || ctid::text FROM spring_session s ORDER BY primary_id",
            String.class);
    clearInvocations(sessions);
    mvc.perform(post("/logout").cookie(cookie).header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("API_SCOPE_DENIED"))
        .andExpect(header().doesNotExist("Set-Cookie"));
    verifyNoInteractions(sessions);
    assertEquals(
        before,
        jdbc.queryForList(
            "SELECT row_to_json(s)::text || xmin::text || ctid::text FROM spring_session s ORDER BY primary_id",
            String.class));
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT count(*) FROM api_owner_quotas WHERE owner_id='http-owner'", Integer.class));
    mvc.perform(get("/api/session").cookie(cookie))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authenticated").value(true));
    mvc.perform(
            post("/api/session/logout")
                .cookie(cookie)
                .with(csrf().asHeader())
                .header("Origin", "https://organization.example"))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/projects").cookie(cookie)).andExpect(status().isUnauthorized());
  }

  @Test
  void s21_defaultFrameworkLogoutCannotBypassBearerAuthentication() throws Exception {
    clearInvocations(sessions);
    mvc.perform(
            post("/logout")
                .header("Authorization", "Bearer invalid")
                .cookie(new jakarta.servlet.http.Cookie("SESSION", "c2Vzc2lvbi1pZA==")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("API_UNAUTHENTICATED"))
        .andExpect(header().string("WWW-Authenticate", "Bearer"))
        .andExpect(header().doesNotExist("Set-Cookie"));
    verifyNoInteractions(sessions);
  }

  @Test
  void s17_s20_realTokenStorageFailureIsNotSessionFailureOrCookieFallback() throws Exception {
    var token =
        create
            .create(
                "http-owner",
                UUID.randomUUID(),
                "Unavailable storage",
                java.util.List.of("projects:read"),
                7)
            .secret();
    clearInvocations(sessions);
    jdbc.execute("ALTER TABLE api_credentials RENAME TO api_credentials_http_unavailable");
    try {
      mvc.perform(
              get("/api/v1/projects")
                  .header("Authorization", "Bearer " + token)
                  .cookie(new jakarta.servlet.http.Cookie("SESSION", "c2Vzc2lvbi1pZA==")))
          .andExpect(status().isServiceUnavailable())
          .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"))
          .andExpect(header().doesNotExist("Set-Cookie"))
          .andExpect(
              content()
                  .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(token))))
          .andExpect(
              content()
                  .string(
                      org.hamcrest.Matchers.not(
                          org.hamcrest.Matchers.containsString("api_credentials"))));
      verifyNoInteractions(sessions);
      assertEquals(
          0,
          jdbc.queryForObject(
              "SELECT count(*) FROM api_owner_quotas WHERE owner_id='http-owner'", Integer.class));
    } finally {
      jdbc.execute("ALTER TABLE api_credentials_http_unavailable RENAME TO api_credentials");
    }
  }

  @Test
  void s19_s26_s32_realBearerWritesWithoutCsrfThenOpenApiAndRevocationDoNotConsume()
      throws Exception {
    var id = UUID.randomUUID();
    var token =
        create
            .create(
                "http-owner",
                id,
                "Business HTTP",
                java.util.List.of("projects:write", "projects:read"),
                7)
            .secret();
    clearInvocations(sessions);
    var created =
        mvc.perform(
                post("/api/v1/projects")
                    .header("Authorization", "bEaReR " + token)
                    .header("Origin", "https://organization.example")
                    .contentType("application/json")
                    .content("{\"name\":\"Integration project\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.ownerId").value("http-owner"))
            .andExpect(header().doesNotExist("Set-Cookie"))
            .andReturn()
            .getResponse();
    var projectId = json.readTree(created.getContentAsByteArray()).get("id").textValue();
    mvc.perform(get("/api/v1/projects/" + projectId).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(header().exists("ETag"))
        .andExpect(jsonPath("$.name").value("Integration project"));
    int used =
        jdbc.queryForObject(
            "SELECT sum(used) FROM api_credential_quotas WHERE credential_id=?", Integer.class, id);
    assertEquals(2, used);
    mvc.perform(get("/api/v1/integration-openapi.json").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openapi").value("3.1.0"));
    assertTrue(revoke.revoke("http-owner", id).isPresent());
    mvc.perform(get("/api/v1/projects/" + projectId).header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("API_UNAUTHENTICATED"))
        .andExpect(header().string("WWW-Authenticate", "Bearer"))
        .andExpect(header().doesNotExist("Set-Cookie"));
    assertEquals(
        used,
        jdbc.queryForObject(
            "SELECT sum(used) FROM api_credential_quotas WHERE credential_id=?",
            Integer.class,
            id));
    verifyNoInteractions(sessions);
  }

  @Test
  void s1_s19_s20_realSessionCreatesCredentialThenBearerAdmitsWithoutSessionAccess()
      throws Exception {
    var login =
        mvc.perform(
                post("/api/session")
                    .with(csrf().asHeader())
                    .header("Origin", "https://organization.example")
                    .param("username", "http-owner")
                    .param("password", "test-only-secret"))
            .andExpect(status().isNoContent())
            .andReturn()
            .getResponse();
    var cookie = login.getCookie("SESSION");
    assertNotNull(cookie);
    var id = UUID.randomUUID();
    var created =
        mvc.perform(
                put("/api/v1/me/api-credentials/" + id)
                    .cookie(cookie)
                    .with(csrf().asHeader())
                    .header("Origin", "https://organization.example")
                    .contentType("application/json")
                    .content(
                        "{\"name\":\"Real HTTP\",\"scopes\":[\"projects:read\"],\"expiresInDays\":7}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andReturn()
            .getResponse();
    var token = json.readTree(created.getContentAsByteArray()).get("secret").textValue();
    assertNotNull(token);
    var before =
        jdbc.queryForList(
            "SELECT row_to_json(s)::text || xmin::text || ctid::text FROM spring_session s ORDER BY primary_id",
            String.class);
    clearInvocations(sessions);
    mvc.perform(get("/api/v1/projects").cookie(cookie).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isEmpty())
        .andExpect(header().doesNotExist("Set-Cookie"));
    verifyNoInteractions(sessions);
    assertEquals(
        before,
        jdbc.queryForList(
            "SELECT row_to_json(s)::text || xmin::text || ctid::text FROM spring_session s ORDER BY primary_id",
            String.class));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT used FROM api_credential_quotas WHERE credential_id=?", Integer.class, id));
  }
}
