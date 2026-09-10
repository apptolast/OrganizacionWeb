package com.apptolast.organization.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.apptolast.organization.adapter.config.SecurityConfiguration;
import com.apptolast.organization.adapter.http.WebhookController;
import com.apptolast.organization.application.ApiCredentialAccess;
import com.apptolast.organization.application.AuthenticateApiCredentialUseCase;
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
  @MockitoBean AuthenticateApiCredentialUseCase authenticateCredential;

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
    mvc.perform(delete("/api/v1/me/webhooks/" + W).with(user("owner")).with(csrf().asHeader()))
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
            post("/api/v1/me/webhooks/" + W + "/ping").with(user("owner")).with(csrf().asHeader()))
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

  /**
   * @s35 «los problem+json contienen código y mensaje en español sin URL, secreto, trazas ni datos
   *     de otra cuenta». Esta prueba afirmaba sólo el {@code $.code}: el mensaje visible para la
   *     persona no lo miraba nadie (B5 del panel).
   *     <p>El campo del mensaje se llama {@code title}: es el nombre que le da RFC 7807, que es el
   *     formato que el contrato nombra por su tipo de contenido. No hay enmienda que hacer, sólo
   *     que la cláusula se comprueba sobre {@code title}.
   *     <p>El oráculo es la <b>igualdad exacta</b> del texto, no su mera existencia: mata al
   *     mutante que B5 describe —devolver el detalle de la excepción, con la URL del endpoint
   *     dentro— y también a la traducción al inglés de una cláusula que exige español. Y el
   *     recuento de claves cierra la puerta a colar un campo nuevo con lo que sea.
   */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "NOT_FOUND,404,WEBHOOK_NOT_FOUND,No se encuentra el webhook.",
    "DISABLED,409,WEBHOOK_DISABLED,Activa el webhook antes de esta operación.",
    "DELIVERY_PENDING,409,WEBHOOK_DELIVERY_PENDING,Ya hay una entrega pendiente para este webhook.",
    "CONNECTORS_DISABLED,503,CONNECTORS_DISABLED,Falta configuración del servidor para conectores.",
    "LIMIT,409,WEBHOOK_LIMIT,Has alcanzado el límite de cinco webhooks.",
    "URL_BLOCKED,400,WEBHOOK_URL_BLOCKED,La dirección de destino no está permitida.",
    "URL_UNRESOLVABLE,400,WEBHOOK_URL_UNRESOLVABLE,No se resuelve el host de destino."
  })
  void s11_s34_s35_everyOperationCodeMapsToItsStatusCodeAndSpanishMessage(
      String code, int status, String expected, String message) throws Exception {
    when(manage.ping("owner", W))
        .thenThrow(
            new com.apptolast.organization.application.WebhookOperationException(
                com.apptolast.organization.application.WebhookOperationException.Code.valueOf(
                    code)));
    var body =
        mvc.perform(
                post("/api/v1/me/webhooks/" + W + "/ping")
                    .with(user("owner"))
                    .with(csrf().asHeader()))
            .andExpect(status().is(status))
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.code").value(expected))
            .andExpect(jsonPath("$.title").value(message))
            .andExpect(jsonPath("$.status").value(status))
            .andExpect(
                jsonPath("$.type")
                    .value(
                        "urn:organization:problem:" + expected.toLowerCase(java.util.Locale.ROOT)))
            .andExpect(jsonPath("$.*", hasSize(4)))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertNoLeak(body);
  }

  /** Lo que @s35 prohíbe en un cuerpo de error, buscado sobre el texto completo de la respuesta. */
  private static void assertNoLeak(String body) {
    for (var forbidden :
        List.of(
            "https://", "whsec_", "?token=", "v1=", "example.com", "com.apptolast", "Exception"))
      org.junit.jupiter.api.Assertions.assertFalse(
          body.contains(forbidden),
          "el cuerpo de error no debe contener «" + forbidden + "»: " + body);
  }

  /**
   * @s35, la mitad que no se podía comprobar afirmando textos constantes: <b>que el detalle de un
   *     fallo inesperado no salga por la respuesta</b>.
   *     <p>Aquí el caso de uso revienta con un mensaje envenenado a propósito, con la URL completa
   *     del endpoint, su cadena de consulta y algo con forma de secreto. Si algún día el manejador
   *     genérico decide «ayudar» poniendo {@code error.getMessage()} o la traza en el cuerpo, esta
   *     prueba se pone roja. La referencia de correlación sí viaja: es un UUID nuevo que no
   *     identifica nada del propietario y es lo que permite cruzar la respuesta con el registro.
   */
  @Test
  void s35_anUnexpectedFailureAnswersWithoutTheUrlTheSecretOrAnyTrace() throws Exception {
    when(manage.deliveries("owner", W))
        .thenThrow(
            new IllegalStateException(
                "POST https://example.com/hooks?token=abc falló firmando con"
                    + " whsec_ESTO-NO-ES-UN-SECRETO-REAL"));

    var body =
        mvc.perform(get("/api/v1/me/webhooks/" + W + "/deliveries").with(user("owner")))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
            .andExpect(
                jsonPath("$.title")
                    .value(
                        "No se ha podido completar la operación. Usa la referencia al solicitar"
                            + " ayuda."))
            .andExpect(jsonPath("$.correlationId").isString())
            .andExpect(jsonPath("$.*", hasSize(5)))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertNoLeak(body);
  }

  @Test
  void s11_aPathThatIsNotAUuidIsNotFoundAndNeverReachesTheUseCase() throws Exception {
    mvc.perform(get("/api/v1/me/webhooks/no-es-uuid").with(user("owner")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("WEBHOOK_NOT_FOUND"));
    verifyNoInteractions(manage);
  }

  /**
   * @s29 «cada elemento tiene exactamente id, eventId, eventType, status, attempt, httpStatus,
   *     latencyMs, errorClass, nextAttemptAt, createdAt y updatedAt».
   *     <p>La versión anterior contaba once campos y sólo nombraba dos (B8.g del panel). Contar no
   *     es nombrar: los cuatro accesores de {@code WebhookDeliveryView} sobrevivían a la mutación y
   *     la única aserción de {@code attempt} en la frontera era {@code .value(0)}, justo contra el
   *     mutante «replaced int return with 0». Aquí cada uno de los once campos se afirma <b>por su
   *     nombre</b> y con un valor distinto de los demás y distinto de cero.
   */
  @Test
  void s29_everyOneOfTheElevenDeliveryFieldsIsServedByNameWithItsOwnValue() throws Exception {
    var deliveryId = UUID.fromString("33333333-3333-4333-8333-333333333333");
    var eventId = UUID.fromString("11111111-1111-4111-8111-111111111111");
    var delivery =
        new com.apptolast.organization.domain.WebhookDelivery(
            deliveryId,
            eventId,
            "TaskCreated.v1",
            "pending",
            4,
            503,
            1234,
            "HTTP_ERROR",
            Instant.parse("2026-09-08T14:30:00.000000Z"),
            Instant.parse("2026-09-08T10:00:00.000000Z"),
            Instant.parse("2026-09-08T12:15:30.000000Z"));
    when(manage.deliveries("owner", W)).thenReturn(List.of(delivery));
    mvc.perform(get("/api/v1/me/webhooks/" + W + "/deliveries").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.*", hasSize(1)))
        .andExpect(jsonPath("$.items[0].*", hasSize(11)))
        .andExpect(jsonPath("$.items[0].id").value(deliveryId.toString()))
        .andExpect(jsonPath("$.items[0].eventId").value(eventId.toString()))
        .andExpect(jsonPath("$.items[0].eventType").value("TaskCreated.v1"))
        .andExpect(jsonPath("$.items[0].status").value("pending"))
        .andExpect(jsonPath("$.items[0].attempt").value(4))
        .andExpect(jsonPath("$.items[0].httpStatus").value(503))
        .andExpect(jsonPath("$.items[0].latencyMs").value(1234))
        .andExpect(jsonPath("$.items[0].errorClass").value("HTTP_ERROR"))
        .andExpect(jsonPath("$.items[0].nextAttemptAt").value("2026-09-08T14:30:00.000000Z"))
        .andExpect(jsonPath("$.items[0].createdAt").value("2026-09-08T10:00:00.000000Z"))
        .andExpect(jsonPath("$.items[0].updatedAt").value("2026-09-08T12:15:30.000000Z"))
        .andExpect(jsonPath("$.items[0].body").doesNotExist())
        .andExpect(jsonPath("$.items[0].url").doesNotExist());
  }

  /**
   * @s29 los cuatro campos sin desenlace viajan como null y no desaparecen del DTO.
   */
  @Test
  void s29_aDeliveryWithoutOutcomeYetKeepsItsFourNullFields() throws Exception {
    var deliveryId = UUID.fromString("44444444-4444-4444-8444-444444444444");
    var delivery =
        new com.apptolast.organization.domain.WebhookDelivery(
            deliveryId,
            deliveryId,
            "webhook.ping.v1",
            "pending",
            0,
            null,
            null,
            null,
            null,
            NOW,
            NOW);
    when(manage.deliveries("owner", W)).thenReturn(List.of(delivery));
    mvc.perform(get("/api/v1/me/webhooks/" + W + "/deliveries").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].*", hasSize(11)))
        .andExpect(jsonPath("$.items[0].httpStatus").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.items[0].latencyMs").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.items[0].errorClass").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.items[0].nextAttemptAt").value(org.hamcrest.Matchers.nullValue()));
  }

  /**
   * @s11 fila 1 y @s8:122. La lectura de un webhook por su identificador no se ejercía
   *     <b>nunca</b>: {@code WebhookController.find} salía NO_COVERAGE en la campaña y los únicos
   *     GET de esa ruta en esta clase eran con un identificador que no es UUID y con el sufijo
   *     {@code /deliveries} (B8.d del panel).
   */
  @Test
  void s8_s11_readingOnesOwnWebhookServesTheNineFieldEndpointWithoutTheSecret() throws Exception {
    when(manage.find("owner", W)).thenReturn(endpoint());

    mvc.perform(get("/api/v1/me/webhooks/" + W).with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(jsonPath("$.*", hasSize(9)))
        .andExpect(jsonPath("$.id").value(W.toString()))
        .andExpect(jsonPath("$.url").value("https://example.com/hooks"))
        .andExpect(jsonPath("$.description").value("Mi hook"))
        .andExpect(
            jsonPath(
                "$.eventTypes",
                org.hamcrest.Matchers.contains("TaskCreated.v1", "TaskStatusChanged.v1")))
        .andExpect(jsonPath("$.status").value("active"))
        .andExpect(jsonPath("$.disabledReason").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.disabledAt").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.createdAt").value("2026-09-08T10:00:00.000000Z"))
        .andExpect(jsonPath("$.updatedAt").value("2026-09-08T10:00:00.000000Z"))
        .andExpect(jsonPath("$.secret").doesNotExist());
  }

  /**
   * El ajeno y el inexistente responden lo mismo, por diseño: distinguirlos revelaría que el
   * recurso existe en otra cuenta.
   */
  @Test
  void s11_readingAWebhookOfAnotherAccountOrOfNoAccountIsTheSameNotFound() throws Exception {
    var foreign = UUID.fromString("55555555-5555-4555-8555-555555555555");
    var absent = UUID.fromString("66666666-6666-4666-8666-666666666666");
    for (var missing : List.of(foreign, absent)) {
      when(manage.find("owner", missing))
          .thenThrow(
              new com.apptolast.organization.application.WebhookOperationException(
                  com.apptolast.organization.application.WebhookOperationException.Code.NOT_FOUND));
      mvc.perform(get("/api/v1/me/webhooks/" + missing).with(user("owner")))
          .andExpect(status().isNotFound())
          .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
          .andExpect(header().string("Cache-Control", "no-store"))
          .andExpect(jsonPath("$.code").value("WEBHOOK_NOT_FOUND"))
          .andExpect(jsonPath("$.title").value("No se encuentra el webhook."));
    }
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

  private static String bodyOfExactly(int bytes) {
    var envelope =
        "{\"url\":\"https://example.com/hooks\",\"eventTypes\":[\"TaskCreated.v1\"],"
            + "\"description\":\"\"}";
    var padding = bytes - envelope.length();
    return "{\"url\":\"https://example.com/hooks\",\"eventTypes\":[\"TaskCreated.v1\"],"
        + "\"description\":\""
        + "a".repeat(padding)
        + "\"}";
  }

  @Test
  void s4_aBodyOfExactlyFourKibibytesIsStillAccepted() throws Exception {
    var body = bodyOfExactly(4096);
    assertEquals(4096, body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
    when(create.create(eq("owner"), any(), any(), any()))
        .thenReturn(new WebhookCreation(endpoint(), SECRET));
    mvc.perform(
            post("/api/v1/me/webhooks")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isCreated());
  }

  @Test
  void s4_aBodyOverTheLimitIsRefusedAsTooLargeWithoutParsingTheJson() throws Exception {
    mvc.perform(
            post("/api/v1/me/webhooks")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(bodyOfExactly(4097)))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("WEBHOOK_TOO_LARGE"));
    verifyNoInteractions(create);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "{\"url\":\"https://example.com/h\",\"eventTypes\":[\"TaskCreated.v1\"],\"otra\":1}",
        "{\"url\":\"https://example.com/h\",\"url\":\"https://example.com/i\","
            + "\"eventTypes\":[\"TaskCreated.v1\"]}",
        "{\"url\":\"https://example.com/h\",\"eventTypes\":[\"TaskCreated.v1\"]} 1"
      })
  void s4_structuralDefectsAreMalformedJsonAndNeverReachTheUseCase(String body) throws Exception {
    mvc.perform(
            post("/api/v1/me/webhooks")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    verifyNoInteractions(create);
  }

  @Test
  void s4_aNonJsonContentTypeIsUnsupportedMedia() throws Exception {
    mvc.perform(
            post("/api/v1/me/webhooks")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("text/plain")
                .content("hola"))
        .andExpect(status().isUnsupportedMediaType());
    verifyNoInteractions(create);
  }

  @Test
  void s4_anyQueryStringOnCreationIsAFieldError() throws Exception {
    mvc.perform(
            post("/api/v1/me/webhooks?owner=otro")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"url\":\"https://example.com/h\",\"eventTypes\":[\"TaskCreated.v1\"]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("WEBHOOK_INVALID"));
    verifyNoInteractions(create);
  }

  @Test
  void s33_withoutASessionEveryRouteIsUnauthenticatedAndTouchesNothing() throws Exception {
    mvc.perform(get("/api/v1/me/webhooks")).andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/v1/me/webhooks")
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"url\":\"https://example.com/h\"}"))
        .andExpect(status().isUnauthorized());
    verifyNoInteractions(create, manage);
  }

  @Test
  void s33_anInvalidCsrfTokenForbidsTheWriteWithoutTouchingWebhooks() throws Exception {
    mvc.perform(
            post("/api/v1/me/webhooks")
                .with(user("owner"))
                .contentType("application/json")
                .content("{\"url\":\"https://example.com/h\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(delete("/api/v1/me/webhooks/" + W).with(user("owner")))
        .andExpect(status().isForbidden());
    verifyNoInteractions(create, manage);
  }

  @Test
  void s33_anUntrustedOriginForbidsTheStatusChange() throws Exception {
    mvc.perform(
            put("/api/v1/me/webhooks/" + W + "/status")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("Origin", "https://evil.example")
                .contentType("application/json")
                .content("{\"status\":\"disabled\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("UNTRUSTED_ORIGIN"));
    verifyNoInteractions(manage);
  }

  /**
   * Las dos filas Bearer de @s33, enmendadas el 9 de septiembre de 2026 con la misma lectura que
   * ratificó el propietario para la feature 26: una credencial Bearer válida <em>sí</em> está
   * autenticada, luego la respuesta correcta es 403 API_SCOPE_DENIED —«sé quién eres y esto no es
   * para ti»— y no 401. El canal de credenciales de máquina no alcanza ninguna ruta de webhooks.
   */
  @Test
  void s33_aBearerCredentialOfTheIntegrationChannelReachesNoWebhookRoute() throws Exception {
    when(authenticateCredential.authenticate("una-credencial-valida"))
        .thenReturn(
            new ApiCredentialAccess(
                UUID.randomUUID(),
                "owner",
                List.of("projects:read", "projects:write", "tasks:read", "tasks:write")));

    for (var request :
        List.of(
            get("/api/v1/me/webhooks").header("Authorization", "Bearer una-credencial-valida"),
            post("/api/v1/me/webhooks/" + W + "/ping")
                .header("Authorization", "Bearer una-credencial-valida"))) {
      mvc.perform(request)
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.code").value("API_SCOPE_DENIED"))
          .andExpect(
              header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    }

    verifyNoInteractions(create, manage);
  }

  @Test
  void s33_patchOnAWebhookIsMethodNotAllowed() throws Exception {
    mvc.perform(
            patch("/api/v1/me/webhooks/" + W)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isMethodNotAllowed());
    verifyNoInteractions(manage);
  }

  private static org.hamcrest.Matcher<java.util.Collection<?>> hasSize(int size) {
    return org.hamcrest.Matchers.hasSize(size);
  }

  private static org.hamcrest.Matcher<Iterable<? extends Object>> contains(Object... items) {
    return org.hamcrest.Matchers.contains(items);
  }
}
