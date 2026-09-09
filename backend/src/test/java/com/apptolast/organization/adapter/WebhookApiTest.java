package com.apptolast.organization.adapter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.apptolast.organization.adapter.config.SecurityConfiguration;
import com.apptolast.organization.adapter.http.WebhookController;
import com.apptolast.organization.application.CreateWebhookUseCase;
import com.apptolast.organization.application.ManageWebhookUseCase;
import com.apptolast.organization.application.WebhookCreation;
import com.apptolast.organization.domain.WebhookEndpoint;
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
    controllers = WebhookController.class,
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@Import(SecurityConfiguration.class)
class WebhookApiTest {
  private static final Instant NOW = Instant.parse("2026-09-08T10:00:00.000000Z");
  private static final UUID W = UUID.fromString("22222222-2222-4222-8222-222222222222");
  private static final String SECRET = "whsec_AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8";

  @Autowired MockMvc mvc;
  @MockitoBean CreateWebhookUseCase create;
  @MockitoBean ManageWebhookUseCase manage;

  private static WebhookEndpoint endpoint() {
    return new WebhookEndpoint(
        W,
        "https://example.com/hooks",
        "Mi hook",
        List.of("TaskCreated.v1", "TaskStatusChanged.v1"),
        "active",
        null,
        null,
        NOW,
        NOW);
  }

  @Test
  void s1_creationReturnsTheSecretOnceBesideAnEndpointOfExactlyNineFields() throws Exception {
    when(create.create(
            eq("owner"),
            eq("https://example.com/hooks"),
            eq("  Mi hook  "),
            eq(List.of("TaskStatusChanged.v1", "TaskCreated.v1"))))
        .thenReturn(new WebhookCreation(endpoint(), SECRET));

    mvc.perform(
            post("/api/v1/me/webhooks")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(
                    "{\"url\":\"https://example.com/hooks\",\"description\":\"  Mi hook  \","
                        + "\"eventTypes\":[\"TaskStatusChanged.v1\",\"TaskCreated.v1\"]}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/me/webhooks/" + W))
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(jsonPath("$.*", hasSize(2)))
        .andExpect(jsonPath("$.secret").value(SECRET))
        .andExpect(jsonPath("$.endpoint.*", hasSize(9)))
        .andExpect(jsonPath("$.endpoint.id").value(W.toString()))
        .andExpect(jsonPath("$.endpoint.url").value("https://example.com/hooks"))
        .andExpect(jsonPath("$.endpoint.description").value("Mi hook"))
        .andExpect(
            jsonPath("$.endpoint.eventTypes")
                .value(contains("TaskCreated.v1", "TaskStatusChanged.v1")))
        .andExpect(jsonPath("$.endpoint.status").value("active"))
        .andExpect(jsonPath("$.endpoint.disabledReason").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.endpoint.disabledAt").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.endpoint.createdAt").value("2026-09-08T10:00:00.000000Z"))
        .andExpect(jsonPath("$.endpoint.updatedAt").value("2026-09-08T10:00:00.000000Z"));
    verifyNoInteractions(manage);
  }

  private static org.hamcrest.Matcher<java.util.Collection<?>> hasSize(int size) {
    return org.hamcrest.Matchers.hasSize(size);
  }

  private static org.hamcrest.Matcher<Iterable<? extends Object>> contains(Object... items) {
    return org.hamcrest.Matchers.contains(items);
  }
}
