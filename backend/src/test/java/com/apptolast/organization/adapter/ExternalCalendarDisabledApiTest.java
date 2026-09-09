package com.apptolast.organization.adapter;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apptolast.organization.adapter.config.SecurityConfiguration;
import com.apptolast.organization.adapter.http.ExternalCalendarController;
import com.apptolast.organization.application.ExternalCalendarUseCases;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** @s8: sin APP_CONNECTOR_KEY las cinco rutas responden 503 sin leer el cuerpo ni la base de datos. */
@WebMvcTest(
    controllers = ExternalCalendarController.class,
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example",
      "app.connectors.key="
    })
@Import(SecurityConfiguration.class)
class ExternalCalendarDisabledApiTest {
  static final String ROUTE = "/api/v1/me/external-calendar";

  @Autowired MockMvc mvc;
  @MockitoBean ExternalCalendarUseCases.Read read;
  @MockitoBean ExternalCalendarUseCases.Save save;
  @MockitoBean ExternalCalendarUseCases.Delete remove;
  @MockitoBean ExternalCalendarUseCases.Sync sync;
  @MockitoBean ExternalCalendarUseCases.ReadEvents events;

  @AfterEach
  void nothingWasAsked() {
    verifyNoInteractions(read, save, remove, sync, events);
  }

  static void disabled(ResultActions performed) throws Exception {
    performed
        .andExpect(status().isServiceUnavailable())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("CONNECTORS_DISABLED"))
        .andExpect(jsonPath("$.status").value(503));
  }

  @Test
  void s8_readingTheSubscription() throws Exception {
    disabled(mvc.perform(get(ROUTE).with(user("owner"))));
  }

  @Test
  void s8_savingWithAMalformedBody() throws Exception {
    disabled(
        mvc.perform(
            put(ROUTE)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{")));
  }

  @Test
  void s8_deletingTheSubscription() throws Exception {
    disabled(mvc.perform(delete(ROUTE).with(user("owner")).with(csrf().asHeader())));
  }

  @Test
  void s8_synchronisingWithAMalformedBody() throws Exception {
    disabled(
        mvc.perform(
            post(ROUTE + "/sync")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{")));
  }

  @Test
  void s8_readingEventsWithAnInvalidRange() throws Exception {
    disabled(
        mvc.perform(get(ROUTE + "/events").with(user("owner")).param("from", "x").param("to", "y")));
  }

  @Test
  void s10_theSessionStillDecidesBeforeTheConnectorGate() throws Exception {
    mvc.perform(get(ROUTE)).andExpect(status().isUnauthorized());
  }

  @Test
  void s10_theCsrfTokenStillDecidesBeforeTheConnectorGate() throws Exception {
    mvc.perform(delete(ROUTE).with(user("owner"))).andExpect(status().isForbidden());
  }

  /** La guardia cubre la ruta y lo que cuelga de ella, no cualquier ruta que empiece igual. */
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"/api/v1/me/external-calendars", "/api/v1/me/external-calendar-otro", "/api/v1/me/otra-cosa"})
  void s8_onlyTheFiveRoutesAreGated(String route) throws Exception {
    mvc.perform(get(route).with(user("owner")))
        .andExpect(status().is(org.hamcrest.Matchers.not(503)));
  }
}
