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

/**
 * Hallazgo A8 de la revisión de seguridad: `Content-Security-Policy` y `Referrer-Policy` sólo
 * existían en `deploy/nginx.conf`, así que un despliegue sin ese proxy servía la API sin ellas. La
 * postura no debe depender del ingress: las emiten las dos cadenas de seguridad.
 */
@WebMvcTest(
    controllers = ProjectReadController.class,
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@Import({
  SecurityConfiguration.class,
  org.springframework.session.config.annotation.web.http.SpringHttpSessionConfiguration.class
})
class SecurityHeadersTest {
  private static final String CONTENT_SECURITY_POLICY =
      "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:;"
          + " connect-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none'";
  private static final String REFERRER_POLICY = "same-origin";

  @Autowired MockMvc mvc;

  @MockitoBean
  org.springframework.session.SessionRepository<org.springframework.session.MapSession> sessions;

  @MockitoBean AuthenticateApiCredentialUseCase authenticate;
  @MockitoBean ConsumeApiQuotaUseCase quota;
  @MockitoBean ReadProjectsUseCase projects;
  private final String token =
      "owp_ac87ee68-133b-4851-82e7-e3ec419df62a_AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

  @Test
  void bearerChainEmitsContentSecurityPolicyAndReferrerPolicy() throws Exception {
    var access =
        new ApiCredentialAccess(
            UUID.fromString("ac87ee68-133b-4851-82e7-e3ec419df62a"),
            "owner",
            List.of("projects:read"));
    when(authenticate.authenticate(token)).thenReturn(access);
    when(projects.list("owner", null)).thenReturn(new ProjectPage(List.of(), null));
    mvc.perform(get("/api/v1/projects").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Security-Policy", CONTENT_SECURITY_POLICY))
        .andExpect(header().string("Referrer-Policy", REFERRER_POLICY))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"));
  }

  @Test
  void bearerRejectionAlsoCarriesTheSecurityHeaders() throws Exception {
    when(authenticate.authenticate(token)).thenThrow(new ApiUnauthenticatedException());
    mvc.perform(get("/api/v1/projects").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("Content-Security-Policy", CONTENT_SECURITY_POLICY))
        .andExpect(header().string("Referrer-Policy", REFERRER_POLICY));
  }

  @Test
  void sessionChainEmitsContentSecurityPolicyAndReferrerPolicy() throws Exception {
    mvc.perform(get("/api/session"))
        .andExpect(header().string("Content-Security-Policy", CONTENT_SECURITY_POLICY))
        .andExpect(header().string("Referrer-Policy", REFERRER_POLICY))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"));
  }
}
