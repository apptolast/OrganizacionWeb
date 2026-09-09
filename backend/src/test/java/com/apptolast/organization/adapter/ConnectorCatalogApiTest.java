package com.apptolast.organization.adapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.apptolast.organization.adapter.config.SecurityConfiguration;
import com.apptolast.organization.adapter.http.ApiErrors;
import com.apptolast.organization.adapter.http.ConnectorCatalogController;
import com.apptolast.organization.application.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @s1 seis filas cerradas en orden fijo; @s3 las que cifran salen deshabilitadas sin más; @s5 el
 *     error es un código y un instante; @s6 leer no escribe ni pregunta dos cosas distintas; @s7 el
 *     almacenamiento caído no da catálogo optimista; @s31 sólo la sesión cookie autentica.
 */
@WebMvcTest(
    controllers = ConnectorCatalogController.class,
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@Import({SecurityConfiguration.class, ApiErrors.class})
class ConnectorCatalogApiTest {
  private static final String CATALOG = "/api/v1/me/connectors";
  private static final Instant AT = Instant.parse("2026-09-10T08:30:00Z");
  private static final List<String> ORDER =
      List.of(
          "api_credentials", "webhooks", "ics_calendar", "github", "external_calendar", "gitlab");
  private static final List<String> ROW_FIELDS =
      List.of("id", "status", "lastActivityAt", "lastError");

  @Autowired MockMvc mvc;
  @Autowired com.fasterxml.jackson.databind.ObjectMapper mapper;
  @MockitoBean ReadConnectorCatalogUseCase catalog;
  @MockitoBean AuthenticateApiCredentialUseCase authenticateCredential;
  @MockitoBean ConsumeApiQuotaUseCase quota;

  private static ConnectorCatalog empty() {
    return new ConnectorCatalog(ORDER.stream().map(ConnectorRow::notConnected).toList());
  }

  private String body() throws Exception {
    return mvc.perform(get(CATALOG).with(user("owner")))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private List<String> keysOf(com.fasterxml.jackson.databind.JsonNode node) {
    var keys = new java.util.ArrayList<String>();
    node.fieldNames().forEachRemaining(keys::add);
    return keys;
  }

  // ------------------------------------------------------------------------------------- @s1

  @Test
  void s1_thecatalogAnswersNoStoreAndAnObjectWithExactlyTheConnectorsKey() throws Exception {
    when(catalog.execute("owner")).thenReturn(empty());

    var body =
        mvc.perform(get(CATALOG).with(user("owner")))
            .andExpect(status().isOk())
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertEquals(List.of("connectors"), keysOf(mapper.readTree(body)));
  }

  @Test
  void s1_theSixRowsComeInTheOrderOfTheCatalogAndWithFourFieldsEach() throws Exception {
    when(catalog.execute("owner")).thenReturn(empty());

    var connectors = mapper.readTree(body()).get("connectors");

    assertEquals(6, connectors.size());
    for (int index = 0; index < ORDER.size(); index++) {
      var row = connectors.get(index);
      assertEquals(ORDER.get(index), row.get("id").asText());
      assertEquals(ROW_FIELDS, keysOf(row), ORDER.get(index));
      assertEquals("not_connected", row.get("status").asText());
      assertTrue(row.get("lastActivityAt").isNull(), ORDER.get(index));
      assertTrue(row.get("lastError").isNull(), ORDER.get(index));
    }
  }

  // ---------------------------------------------------------------------------------- @s2 @s5

  @Test
  void s2_arowInErrorPublishesItsCodeAndItsInstantAndNothingElse() throws Exception {
    when(catalog.execute("owner"))
        .thenReturn(
            new ConnectorCatalog(
                List.of(
                    ConnectorRow.notConnected("api_credentials"),
                    ConnectorRow.notConnected("webhooks"),
                    ConnectorRow.notConnected("ics_calendar"),
                    ConnectorRow.notConnected("github"),
                    ConnectorRow.notConnected("external_calendar"),
                    ConnectorRow.error(
                        "gitlab", AT, new ConnectorError("CONNECTION_INVALID", AT)))));

    var gitlab = mapper.readTree(body()).get("connectors").get(5);

    assertEquals("error", gitlab.get("status").asText());
    assertEquals(AT.toString(), gitlab.get("lastActivityAt").asText());
    assertEquals(List.of("code", "at"), keysOf(gitlab.get("lastError")));
    assertEquals("CONNECTION_INVALID", gitlab.get("lastError").get("code").asText());
    assertEquals(AT.toString(), gitlab.get("lastError").get("at").asText());
  }

  @Test
  void s5_thewholeBodyCarriesNoSecretNoUrlAndNoProjectPath() throws Exception {
    when(catalog.execute("owner"))
        .thenReturn(
            new ConnectorCatalog(
                List.of(
                    ConnectorRow.notConnected("api_credentials"),
                    ConnectorRow.notConnected("webhooks"),
                    ConnectorRow.notConnected("ics_calendar"),
                    ConnectorRow.notConnected("github"),
                    ConnectorRow.notConnected("external_calendar"),
                    ConnectorRow.error(
                        "gitlab", AT, new ConnectorError("CONNECTION_INVALID", AT)))));

    var body = body();

    assertFalse(body.contains("glpat"));
    assertFalse(body.contains("invalid_token"));
    assertFalse(body.contains("WXYZ"));
    assertFalse(body.contains("grupo/proyecto"));
    assertFalse(body.contains("http"));
  }

  // ------------------------------------------------------------------------------------- @s3

  @Test
  void s3_thedisabledRowsTravelAsDisabledWithBothNulls() throws Exception {
    when(catalog.execute("owner"))
        .thenReturn(
            new ConnectorCatalog(
                List.of(
                    ConnectorRow.connected("api_credentials", null),
                    ConnectorRow.disabled("webhooks"),
                    ConnectorRow.connected("ics_calendar", null),
                    ConnectorRow.disabled("github"),
                    ConnectorRow.disabled("external_calendar"),
                    ConnectorRow.disabled("gitlab"))));

    var connectors = mapper.readTree(body()).get("connectors");

    for (int index : new int[] {1, 3, 4, 5}) {
      assertEquals("disabled", connectors.get(index).get("status").asText());
      assertTrue(connectors.get(index).get("lastActivityAt").isNull());
      assertTrue(connectors.get(index).get("lastError").isNull());
    }
    assertEquals("connected", connectors.get(0).get("status").asText());
    assertEquals("connected", connectors.get(2).get("status").asText());
  }

  // ------------------------------------------------------------------------------------- @s6

  @Test
  void s6_threeReadsInARowGiveTheSameBodyAndOnlyEverRead() throws Exception {
    when(catalog.execute("owner")).thenReturn(empty());

    var first = body();
    var second = body();
    var third = body();

    assertEquals(first, second);
    assertEquals(second, third);
    verify(catalog, times(3)).execute("owner");
    verifyNoMoreInteractions(catalog);
  }

  // ------------------------------------------------------------------------------------- @s7

  @Test
  void s7_astorageFailureAnswersServiceUnavailableWithoutAnyOptimisticRow() throws Exception {
    when(catalog.execute("owner"))
        .thenThrow(new StorageUnavailableException(new RuntimeException("postgres")));

    var body =
        mvc.perform(get(CATALOG).with(user("owner")))
            .andExpect(status().is(503))
            .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertFalse(body.contains("connectors"));
    assertFalse(body.contains("connected"));
  }

  // ------------------------------------------------------------------------------------ @s31

  @Test
  void s31_withoutASessionTheCatalogAnswersNothing() throws Exception {
    mvc.perform(get(CATALOG)).andExpect(status().isUnauthorized());

    verifyNoInteractions(catalog);
  }

  @Test
  void s31_avalidBearerCredentialIsIdentifiedButDeniedByScope() throws Exception {
    when(authenticateCredential.authenticate("secreto-de-integracion"))
        .thenReturn(new ApiCredentialAccess(UUID.randomUUID(), "owner", List.of("tasks:read")));

    mvc.perform(get(CATALOG).header("Authorization", "Bearer secreto-de-integracion"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("API_SCOPE_DENIED"));

    verifyNoInteractions(catalog);
  }

  @Test
  void s31_aqueryStringOnTheCatalogIsRefusedBeforeReachingTheUseCase() throws Exception {
    mvc.perform(get(CATALOG + "?owner=otro").with(user("owner")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    verifyNoInteractions(catalog);
  }
}
