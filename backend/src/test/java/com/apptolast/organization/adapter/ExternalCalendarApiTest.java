package com.apptolast.organization.adapter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apptolast.organization.adapter.config.SecurityConfiguration;
import com.apptolast.organization.adapter.http.ExternalCalendarController;
import com.apptolast.organization.application.ExternalCalendarNotConfiguredException;
import com.apptolast.organization.application.ExternalCalendarUseCases;
import com.apptolast.organization.application.ExternalEventsView;
import com.apptolast.organization.application.SyncOutcome;
import com.apptolast.organization.domain.ExternalCalendarSubscription;
import com.apptolast.organization.domain.ExternalEvent;
import com.apptolast.organization.domain.ExternalEventsRange;
import com.apptolast.organization.domain.FeedError;
import com.apptolast.organization.domain.FieldError;
import com.apptolast.organization.domain.SyncStatus;
import com.apptolast.organization.domain.ValidationException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @s1, @s2, @s4, @s10, @s28, @s31, @s32, @s33.
 */
@WebMvcTest(
    controllers = ExternalCalendarController.class,
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@Import(SecurityConfiguration.class)
class ExternalCalendarApiTest {
  static final String ROUTE = "/api/v1/me/external-calendar";
  static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
  static final Instant NOW = Instant.parse("2030-01-07T12:00:00Z");
  static final String VALID_BODY =
      "{\"label\":\"Trabajo\",\"url\":\"https://feed.example.test/a.ics\"}";

  @Autowired MockMvc mvc;
  @MockitoBean ExternalCalendarUseCases.Read read;
  @MockitoBean ExternalCalendarUseCases.Save save;
  @MockitoBean ExternalCalendarUseCases.Delete remove;
  @MockitoBean ExternalCalendarUseCases.Sync sync;
  @MockitoBean ExternalCalendarUseCases.ReadEvents events;
  @MockitoBean com.apptolast.organization.application.SecretCipher cipher;

  @org.junit.jupiter.api.BeforeEach
  void connectorsAreEnabled() {
    when(cipher.enabled()).thenReturn(true);
  }

  static ExternalCalendarSubscription subscription() {
    return new ExternalCalendarSubscription(
        ID,
        "Trabajo",
        "feed.example.test",
        ".ics",
        null,
        null,
        null,
        null,
        null,
        0,
        0,
        0,
        0,
        false,
        NOW);
  }

  static ExternalCalendarSubscription synced() {
    return new ExternalCalendarSubscription(
        ID,
        "Trabajo",
        "feed.example.test",
        ".ics",
        NOW,
        NOW,
        SyncStatus.FAILED,
        FeedError.FEED_HTTP_ERROR,
        "Europe/Madrid",
        12,
        3,
        1,
        7,
        true,
        NOW);
  }

  @Test
  void s1_absenceIsReportedWithConfiguredFalseAndNoStore() throws Exception {
    when(read.execute("owner")).thenReturn(Optional.empty());
    mvc.perform(get(ROUTE).with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(jsonPath("$.configured").value(false))
        .andExpect(jsonPath("$.subscription").doesNotExist())
        .andExpect(jsonPath("$.length()").value(2));
    verifyNoInteractions(save, remove, sync, events);
  }

  @Test
  void s2_theSubscriptionIsPublishedWithExactlyFifteenFields() throws Exception {
    when(read.execute("owner")).thenReturn(Optional.of(synced()));
    mvc.perform(get(ROUTE).with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.configured").value(true))
        .andExpect(jsonPath("$.subscription.length()").value(15))
        .andExpect(jsonPath("$.subscription.id").value(ID.toString()))
        .andExpect(jsonPath("$.subscription.label").value("Trabajo"))
        .andExpect(jsonPath("$.subscription.urlHost").value("feed.example.test"))
        .andExpect(jsonPath("$.subscription.urlTail").value(".ics"))
        .andExpect(jsonPath("$.subscription.lastAttemptAt").value("2030-01-07T12:00:00Z"))
        .andExpect(jsonPath("$.subscription.lastSyncAt").value("2030-01-07T12:00:00Z"))
        .andExpect(jsonPath("$.subscription.lastStatus").value("FAILED"))
        .andExpect(jsonPath("$.subscription.lastError").value("FEED_HTTP_ERROR"))
        .andExpect(jsonPath("$.subscription.snapshotZoneId").value("Europe/Madrid"))
        .andExpect(jsonPath("$.subscription.imported").value(12))
        .andExpect(jsonPath("$.subscription.skippedRecurring").value(3))
        .andExpect(jsonPath("$.subscription.skippedCancelled").value(1))
        .andExpect(jsonPath("$.subscription.skippedInvalid").value(7))
        .andExpect(jsonPath("$.subscription.truncated").value(true))
        .andExpect(jsonPath("$.subscription.updatedAt").value("2030-01-07T12:00:00Z"));
  }

  @Test
  void s2_theResponseNeverCarriesTheUrlNorTheVersion() throws Exception {
    when(save.execute(
            "owner", "Trabajo", "https://feed.example.test/calendar/ical/abc123/basic.ics"))
        .thenReturn(subscription());
    mvc.perform(
            put(ROUTE)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(
                    "{\"label\":\"Trabajo\",\"url\":\"https://feed.example.test/calendar/ical/abc123/basic.ics\"}"))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.configured").value(true))
        .andExpect(jsonPath("$.subscription.length()").value(15))
        .andExpect(
            content()
                .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("abc123"))))
        .andExpect(jsonPath("$.subscription.version").doesNotExist())
        .andExpect(jsonPath("$.subscription.url").doesNotExist());
  }

  @Test
  void s7_deletingAnswersTwoHundredAndFourWithoutABody() throws Exception {
    mvc.perform(delete(ROUTE).with(user("owner")).with(csrf().asHeader()))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));
    verify(remove).execute("owner");
  }

  @Test
  void s28_syncingWithoutASubscriptionIsAProblemJsonFourOhFour() throws Exception {
    when(sync.execute("owner", false)).thenThrow(new ExternalCalendarNotConfiguredException());
    mvc.perform(
            post(ROUTE + "/sync")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"onlyIfStale\":false}"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("EXTERNAL_CALENDAR_NOT_CONFIGURED"));
  }

  @Test
  void s11_aSyncReportsPerformedAndTheSubscription() throws Exception {
    when(sync.execute("owner", false)).thenReturn(new SyncOutcome(true, synced()));
    mvc.perform(
            post(ROUTE + "/sync")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"onlyIfStale\":false}"))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.performed").value(true))
        .andExpect(jsonPath("$.subscription.lastStatus").value("FAILED"))
        .andExpect(jsonPath("$.subscription.lastError").value("FEED_HTTP_ERROR"))
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void s35_theFreshnessFlagTravelsToTheUseCase() throws Exception {
    when(sync.execute("owner", true)).thenReturn(new SyncOutcome(false, synced()));
    mvc.perform(
            post(ROUTE + "/sync")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"onlyIfStale\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.performed").value(false));
    verify(sync).execute("owner", true);
  }

  @Test
  void s31_theEventsResponseHasExactlyFourFieldsAndFiveFieldsPerItem() throws Exception {
    when(events.execute(eq("owner"), any(ExternalEventsRange.class)))
        .thenReturn(
            new ExternalEventsView(
                true,
                NOW,
                SyncStatus.OK,
                List.of(
                    new ExternalEvent(
                        "a",
                        "Reunión",
                        Instant.parse("2030-01-07T08:00:00Z"),
                        Instant.parse("2030-01-07T09:00:00Z"),
                        false))));
    mvc.perform(
            get(ROUTE + "/events")
                .with(user("owner"))
                .param("from", "2030-01-07T00:00:00Z")
                .param("to", "2030-01-08T00:00:00Z"))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.length()").value(4))
        .andExpect(jsonPath("$.configured").value(true))
        .andExpect(jsonPath("$.lastSyncAt").value("2030-01-07T12:00:00Z"))
        .andExpect(jsonPath("$.lastStatus").value("OK"))
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].length()").value(5))
        .andExpect(jsonPath("$.items[0].uid").value("a"))
        .andExpect(jsonPath("$.items[0].summary").value("Reunión"))
        .andExpect(jsonPath("$.items[0].startAt").value("2030-01-07T08:00:00Z"))
        .andExpect(jsonPath("$.items[0].endAt").value("2030-01-07T09:00:00Z"))
        .andExpect(jsonPath("$.items[0].allDay").value(false));
  }

  @Test
  void s33_anOwnerWithoutSubscriptionReadsAnEmptyEventsView() throws Exception {
    when(events.execute(eq("owner"), any(ExternalEventsRange.class)))
        .thenReturn(ExternalEventsView.unconfigured());
    mvc.perform(
            get(ROUTE + "/events")
                .with(user("owner"))
                .param("from", "2030-01-07T00:00:00Z")
                .param("to", "2030-01-08T00:00:00Z"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.configured").value(false))
        .andExpect(jsonPath("$.lastSyncAt").doesNotExist())
        .andExpect(jsonPath("$.lastStatus").doesNotExist())
        .andExpect(jsonPath("$.items.length()").value(0));
  }

  @Test
  void s32_theRangeIsValidatedBeforeReachingTheUseCase() throws Exception {
    mvc.perform(
            get(ROUTE + "/events")
                .with(user("owner"))
                .param("from", "2030-01-07T00:00:00Z")
                .param("to", "2030-01-23T00:00:01Z"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("to"))
        .andExpect(jsonPath("$.errors[0].code").value("OUT_OF_RANGE"));
    verifyNoInteractions(events);
  }

  @Test
  void s32_anUnknownQueryParameterOnEventsIsRejected() throws Exception {
    mvc.perform(
            get(ROUTE + "/events")
                .with(user("owner"))
                .param("from", "2030-01-07T00:00:00Z")
                .param("to", "2030-01-08T00:00:00Z")
                .param("zone", "UTC"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("zone"));
    verifyNoInteractions(events);
  }

  @Test
  void s10_anUnknownQueryParameterOnTheSubscriptionIsRejected() throws Exception {
    mvc.perform(get(ROUTE).with(user("owner")).param("x", "1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    verifyNoInteractions(read);
  }

  /**
   * @s10 se titula «aplicar seguridad HTTP común en las CINCO rutas», y el rechazo de parámetros
   *     desconocidos solo estaba medido en dos: GET y POST /sync. Estas dos completan las cinco. La
   *     comprobación va antes de leer el cuerpo, así que el caso de uso no llega a invocarse.
   */
  @Test
  void s10_anUnknownQueryParameterOnThePutIsRejected() throws Exception {
    mvc.perform(
            put(ROUTE)
                .with(user("owner"))
                .with(csrf().asHeader())
                .param("x", "1")
                .contentType("application/json")
                .content(VALID_BODY))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("x"))
        .andExpect(jsonPath("$.errors[0].code").value("UNKNOWN_PARAMETER"));
    verifyNoInteractions(save);
  }

  @Test
  void s10_anUnknownQueryParameterOnTheDeleteIsRejected() throws Exception {
    mvc.perform(delete(ROUTE).with(user("owner")).with(csrf().asHeader()).param("x", "1"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("x"))
        .andExpect(jsonPath("$.errors[0].code").value("UNKNOWN_PARAMETER"));
    verifyNoInteractions(remove);
  }

  @Test
  void s10_aRequestWithoutSessionIsUnauthenticated() throws Exception {
    mvc.perform(get(ROUTE)).andExpect(status().isUnauthorized());
    verifyNoInteractions(read, save, remove, sync, events);
  }

  @Test
  void s10_aWriteWithoutCsrfTokenIsForbidden() throws Exception {
    mvc.perform(put(ROUTE).with(user("owner")).contentType("application/json").content(VALID_BODY))
        .andExpect(status().isForbidden());
    verifyNoInteractions(save);
  }

  @Test
  void s10_aWriteFromAnotherOriginIsForbidden() throws Exception {
    mvc.perform(
            post(ROUTE + "/sync")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("Origin", "https://otro.example")
                .contentType("application/json")
                .content("{\"onlyIfStale\":false}"))
        .andExpect(status().isForbidden());
    verifyNoInteractions(sync);
  }

  @Test
  void s10_anUnknownFieldInThePutBodyIsRejected() throws Exception {
    mvc.perform(
            put(ROUTE)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(
                    "{\"label\":\"a\",\"url\":\"https://feed.example.test/a.ics\",\"extra\":1}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("extra"))
        .andExpect(jsonPath("$.errors[0].code").value("UNKNOWN_FIELD"));
    verifyNoInteractions(save);
  }

  @ParameterizedTest
  @ValueSource(strings = {"{", "", "   ", "[]", "\"texto\"", "null"})
  void s10_aMalformedOrNonObjectBodyIsRejected(String body) throws Exception {
    mvc.perform(
            put(ROUTE)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(save);
  }

  @Test
  void s10_aBrokenJsonBodyIsMalformedJson() throws Exception {
    mvc.perform(
            put(ROUTE)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
  }

  @Test
  void s10_aPlainTextBodyIsUnsupportedMedia() throws Exception {
    mvc.perform(
            put(ROUTE)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("text/plain")
                .content(VALID_BODY))
        .andExpect(status().isUnsupportedMediaType());
    verifyNoInteractions(save);
  }

  @ParameterizedTest
  @CsvSource({
    "'{\"onlyIfStale\":\"no\"}', onlyIfStale, INVALID_TYPE",
    "'{\"onlyIfStale\":1}', onlyIfStale, INVALID_TYPE",
    "'{\"onlyIfStale\":null}', onlyIfStale, REQUIRED",
    "'{}', onlyIfStale, REQUIRED",
    "'{\"onlyIfStale\":true,\"otro\":1}', otro, UNKNOWN_FIELD"
  })
  void s10_theSyncBodyIsValidated(String body, String field, String code) throws Exception {
    mvc.perform(
            post(ROUTE + "/sync")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value(field))
        .andExpect(jsonPath("$.errors[0].code").value(code));
    verifyNoInteractions(sync);
  }

  @ParameterizedTest
  @CsvSource({
    "'{\"url\":\"https://feed.example.test/a.ics\"}', label, REQUIRED",
    "'{\"label\":null,\"url\":\"https://feed.example.test/a.ics\"}', label, REQUIRED",
    "'{\"label\":7,\"url\":\"https://feed.example.test/a.ics\"}', label, INVALID_TYPE",
    "'{\"label\":\"Trabajo\"}', url, REQUIRED",
    "'{\"label\":\"Trabajo\",\"url\":true}', url, INVALID_TYPE",
    "'{\"label\":\"Trabajo\",\"url\":null}', url, REQUIRED"
  })
  void s4_theTypesOfTheTwoFieldsAreCheckedBeforeTheUseCase(String body, String field, String code)
      throws Exception {
    mvc.perform(
            put(ROUTE)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value(field))
        .andExpect(jsonPath("$.errors[0].code").value(code));
    verifyNoInteractions(save);
  }

  @Test
  void s4_aRejectedAddressComesBackAsAFieldErrorOnUrl() throws Exception {
    when(save.execute(anyString(), anyString(), anyString()))
        .thenThrow(
            new ValidationException(
                List.of(new FieldError("url", "BLOCKED_ADDRESS", "Apunta a una red interna."))));
    mvc.perform(
            put(ROUTE)
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(VALID_BODY))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.errors[0].field").value("url"))
        .andExpect(jsonPath("$.errors[0].code").value("BLOCKED_ADDRESS"));
  }

  @Test
  void s10_theSyncRouteRejectsUnknownQueryParameters() throws Exception {
    mvc.perform(
            post(ROUTE + "/sync")
                .with(user("owner"))
                .with(csrf().asHeader())
                .param("x", "1")
                .contentType("application/json")
                .content("{\"onlyIfStale\":false}"))
        .andExpect(status().isBadRequest());
    verify(sync, never()).execute(anyString(), anyBoolean());
  }
}
