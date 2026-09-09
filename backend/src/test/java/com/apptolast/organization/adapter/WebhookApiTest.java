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

  @Test
  void s10_listingReturnsOnlyItemsWithTheNineFieldEndpointDto() throws Exception {
    when(manage.list("owner")).thenReturn(List.of(endpoint()));
    mvc.perform(get("/api/v1/me/webhooks").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.*", hasSize(1)))
        .andExpect(jsonPath("$.items", hasSize(1)))
        .andExpect(jsonPath("$.items[0].*", hasSize(9)))
        .andExpect(jsonPath("$.items[0].id").value(W.toString()))
        .andExpect(jsonPath("$.items[0].secret").doesNotExist());
  }

  @Test
  void s10_anOwnerWithoutWebhooksGetsAnEmptyItemsArray() throws Exception {
    when(manage.list("owner")).thenReturn(List.of());
    mvc.perform(get("/api/v1/me/webhooks").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items", hasSize(0)));
  }

  @Test
  void s12_changingStatusReturnsTheEndpointWithoutETag() throws Exception {
    var disabled =
        new WebhookEndpoint(
            W,
            "https://example.com/hooks",
            "Mi hook",
            List.of("TaskCreated.v1"),
            "disabled",
            "MANUAL",
            NOW,
            NOW,
            NOW);
    when(manage.changeStatus("owner", W, "disabled")).thenReturn(disabled);
    mvc.perform(
            put("/api/v1/me/webhooks/" + W + "/status")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"status\":\"disabled\"}"))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist("ETag"))
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.*", hasSize(9)))
        .andExpect(jsonPath("$.status").value("disabled"))
        .andExpect(jsonPath("$.disabledReason").value("MANUAL"))
        .andExpect(jsonPath("$.disabledAt").value("2026-09-08T10:00:00.000000Z"));
  }

  @Test
  void s12_aStatusOutsideTheBinaryPairIsAFieldError() throws Exception {
    when(manage.changeStatus("owner", W, "paused"))
        .thenThrow(
            new com.apptolast.organization.domain.WebhookInvalidException(List.of("status")));
    mvc.perform(
            put("/api/v1/me/webhooks/" + W + "/status")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"status\":\"paused\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("WEBHOOK_INVALID"))
        .andExpect(jsonPath("$.errors[0].field").value("status"));
  }

  @Test
  void s13_deleteAnswersNoContentWithoutBody() throws Exception {
    mvc.perform(
            delete("/api/v1/me/webhooks/" + W).with(user("owner")).with(csrf().asHeader()))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));
    verify(manage).delete("owner", W);
  }

  @Test
  void s14_pingIsAcceptedAndAnsweredWithTheElevenFieldDeliveryDto() throws Exception {
    var delivery =
        com.apptolast.organization.domain.WebhookDelivery.ping(
            UUID.fromString("33333333-3333-4333-8333-333333333333"), NOW);
    when(manage.ping("owner", W)).thenReturn(delivery);
    mvc.perform(
            post("/api/v1/me/webhooks/" + W + "/ping")
                .with(user("owner"))
                .with(csrf().asHeader()))
        .andExpect(status().isAccepted())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.*", hasSize(1)))
        .andExpect(jsonPath("$.delivery.*", hasSize(11)))
        .andExpect(jsonPath("$.delivery.status").value("pending"))
        .andExpect(jsonPath("$.delivery.attempt").value(0))
        .andExpect(jsonPath("$.delivery.eventType").value("webhook.ping.v1"))
        .andExpect(jsonPath("$.delivery.eventId").value(delivery.id().toString()))
        .andExpect(jsonPath("$.delivery.httpStatus").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.delivery.latencyMs").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.delivery.errorClass").value(org.hamcrest.Matchers.nullValue()));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "NOT_FOUND,404,WEBHOOK_NOT_FOUND",
    "DISABLED,409,WEBHOOK_DISABLED",
    "DELIVERY_PENDING,409,WEBHOOK_DELIVERY_PENDING",
    "CONNECTORS_DISABLED,503,CONNECTORS_DISABLED",
    "LIMIT,409,WEBHOOK_LIMIT",
    "URL_BLOCKED,400,WEBHOOK_URL_BLOCKED",
    "URL_UNRESOLVABLE,400,WEBHOOK_URL_UNRESOLVABLE"
  })
  void s11_s34_everyOperationCodeMapsToItsStableStatusAndProblemBody(
      String code, int status, String expected) throws Exception {
    when(manage.ping("owner", W))
        .thenThrow(
            new com.apptolast.organization.application.WebhookOperationException(
                com.apptolast.organization.application.WebhookOperationException.Code.valueOf(
                    code)));
    mvc.perform(
            post("/api/v1/me/webhooks/" + W + "/ping")
                .with(user("owner"))
                .with(csrf().asHeader()))
        .andExpect(status().is(status))
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.code").value(expected));
  }

  @Test
  void s11_aPathThatIsNotAUuidIsNotFoundAndNeverReachesTheUseCase() throws Exception {
    mvc.perform(get("/api/v1/me/webhooks/no-es-uuid").with(user("owner")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("WEBHOOK_NOT_FOUND"));
    verifyNoInteractions(manage);
  }

  @Test
  void s29_deliveriesAreListedWithTheirClosedDtoAndNoBody() throws Exception {
    var delivery =
        new com.apptolast.organization.domain.WebhookDelivery(
            UUID.fromString("33333333-3333-4333-8333-333333333333"),
            UUID.fromString("11111111-1111-4111-8111-111111111111"),
            "TaskCreated.v1",
            "succeeded",
            1,
            200,
            12,
            null,
            null,
            NOW,
            NOW);
    when(manage.deliveries("owner", W)).thenReturn(List.of(delivery));
    mvc.perform(get("/api/v1/me/webhooks/" + W + "/deliveries").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.*", hasSize(1)))
        .andExpect(jsonPath("$.items[0].*", hasSize(11)))
        .andExpect(jsonPath("$.items[0].httpStatus").value(200))
        .andExpect(jsonPath("$.items[0].latencyMs").value(12))
        .andExpect(jsonPath("$.items[0].body").doesNotExist())
        .andExpect(jsonPath("$.items[0].url").doesNotExist());
  }

  @Test
  void s30_redeliverIsAcceptedWithTheReopenedDelivery() throws Exception {
    var deliveryId = UUID.fromString("33333333-3333-4333-8333-333333333333");
    var reopened =
        new com.apptolast.organization.domain.WebhookDelivery(
            deliveryId,
            UUID.fromString("11111111-1111-4111-8111-111111111111"),
            "TaskCreated.v1",
            "pending",
            0,
            null,
            null,
            null,
            NOW,
            NOW,
            NOW);
    when(manage.redeliver("owner", W, deliveryId)).thenReturn(reopened);
    mvc.perform(
            post("/api/v1/me/webhooks/" + W + "/deliveries/" + deliveryId + "/redeliver")
                .with(user("owner"))
                .with(csrf().asHeader()))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.delivery.status").value("pending"))
        .andExpect(jsonPath("$.delivery.attempt").value(0))
        .andExpect(jsonPath("$.delivery.id").value(deliveryId.toString()));
  }

  private static org.hamcrest.Matcher<java.util.Collection<?>> hasSize(int size) {
    return org.hamcrest.Matchers.hasSize(size);
  }

  private static org.hamcrest.Matcher<Iterable<? extends Object>> contains(Object... items) {
    return org.hamcrest.Matchers.contains(items);
  }
}
