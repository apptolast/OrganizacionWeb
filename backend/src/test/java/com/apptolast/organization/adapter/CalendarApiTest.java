package com.apptolast.organization.adapter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.apptolast.organization.adapter.config.SecurityConfiguration;
import com.apptolast.organization.adapter.http.CalendarFeedController;
import com.apptolast.organization.adapter.http.PublicCalendarController;
import com.apptolast.organization.application.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(
    controllers = {CalendarFeedController.class, PublicCalendarController.class},
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@Import(SecurityConfiguration.class)
class CalendarApiTest {
  private static final String TOKEN = "0123456789abcdefghijklmnopqrstuvwxyz_-ABCDE";
  private static final String DOCUMENT =
      "BEGIN:VCALENDAR\r\nVERSION:2.0\r\nSUMMARY:Revisión\r\nEND:VCALENDAR\r\n";
  private static final int OCTETS = DOCUMENT.getBytes(StandardCharsets.UTF_8).length;
  private static final Instant CREATED_AT = Instant.parse("2026-09-08T12:00:00Z");

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @MockitoBean ManageCalendarFeedUseCase feeds;
  @MockitoBean RenderCalendarUseCase calendars;

  @BeforeEach
  void documents() {
    when(calendars.forToken(TOKEN)).thenReturn(DOCUMENT);
    when(calendars.forOwner("owner")).thenReturn(DOCUMENT);
    lenient()
        .when(calendars.forToken(argThat(candidate -> !TOKEN.equals(candidate))))
        .thenThrow(new CalendarNotFoundException());
  }

  static String argThat(java.util.function.Predicate<String> predicate) {
    return org.mockito.ArgumentMatchers.argThat(predicate::test);
  }

  // ---------- management ----------

  @Test
  void s1_creatingTheLinkAnswersOnlyUrlAndCreatedAt() throws Exception {
    when(feeds.generate("owner"))
        .thenReturn(
            new CalendarFeedLink(
                "https://organization.example/calendar/" + TOKEN + ".ics", CREATED_AT));
    var body =
        mvc.perform(post("/api/v1/me/calendar-feed").with(user("owner")).with(csrf().asHeader()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(json.readValue(body, Map.class))
        .containsOnlyKeys("url", "createdAt")
        .containsEntry("url", "https://organization.example/calendar/" + TOKEN + ".ics")
        .containsEntry("createdAt", "2026-09-08T12:00:00.000000Z");
  }

  @Test
  void s2_statusWithoutLinkIsInactive() throws Exception {
    when(feeds.status("owner")).thenReturn(new CalendarFeedStatus(false, null));
    var body =
        mvc.perform(get("/api/v1/me/calendar-feed").with(user("owner")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(json.readValue(body, Map.class))
        .containsOnlyKeys("active", "createdAt")
        .containsEntry("active", false)
        .containsEntry("createdAt", null);
  }

  @Test
  void s3_statusWithLinkNeverRevealsTheTokenOrItsDigest() throws Exception {
    when(feeds.status("owner")).thenReturn(new CalendarFeedStatus(true, CREATED_AT));
    var body =
        mvc.perform(get("/api/v1/me/calendar-feed").with(user("owner")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(json.readValue(body, Map.class))
        .containsOnlyKeys("active", "createdAt")
        .containsEntry("active", true)
        .containsEntry("createdAt", "2026-09-08T12:00:00.000000Z");
    assertThat(body).doesNotContain(TOKEN).doesNotContain("url");
  }

  @Test
  void s5_revokingAnswersTwoHundredFourWithoutBody() throws Exception {
    var response =
        mvc.perform(delete("/api/v1/me/calendar-feed").with(user("owner")).with(csrf().asHeader()))
            .andExpect(status().isNoContent())
            .andReturn()
            .getResponse();
    assertThat(response.getContentAsString()).isEmpty();
    verify(feeds).revoke("owner");
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "{\"name\":\"casa\"}", "null", "\"\"", "[]"})
  void s6_aNonEmptyGenerationBodyIsRejectedWithoutTouchingTheToken(String body) throws Exception {
    mvc.perform(
            post("/api/v1/me/calendar-feed")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    verifyNoInteractions(feeds);
  }

  @ParameterizedTest
  @CsvSource({
    "GET,/api/v1/me/calendar-feed",
    "POST,/api/v1/me/calendar-feed",
    "DELETE,/api/v1/me/calendar-feed",
    "GET,/api/v1/me/calendar.ics"
  })
  void s7_withoutSessionNoManagementRouteAnswersData(String method, String route) throws Exception {
    for (var bearer : new String[] {null, "Bearer cualquier-valor"}) {
      var request = request(org.springframework.http.HttpMethod.valueOf(method), route);
      if (bearer != null) request.header("Authorization", bearer);
      var response =
          mvc.perform(request.with(csrf().asHeader()))
              .andExpect(status().isUnauthorized())
              .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
              .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
              .andReturn()
              .getResponse();
      assertThat(response.getHeaders("Set-Cookie")).isEmpty();
    }
    verifyNoInteractions(feeds);
  }

  @ParameterizedTest
  @CsvSource({"POST,/api/v1/me/calendar-feed", "DELETE,/api/v1/me/calendar-feed"})
  void s8_generatingAndRevokingKeepCsrfAndOrigin(String method, String route) throws Exception {
    var verb = org.springframework.http.HttpMethod.valueOf(method);
    mvc.perform(request(verb, route).with(user("owner")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    mvc.perform(
            request(verb, route)
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("Origin", "https://otro.example"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("UNTRUSTED_ORIGIN"));
    verifyNoInteractions(feeds);
  }

  // ---------- public feed ----------

  @Test
  void s8_s11_thePublicFeedNeedsNoCredentialsAndCarriesTheExactHeaders() throws Exception {
    var response =
        mvc.perform(get("/calendar/" + TOKEN + ".ics"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    assertThat(response.getHeader("Content-Type")).isEqualTo("text/calendar; charset=utf-8");
    assertThat(response.getHeader("Cache-Control")).isEqualTo("private, no-store");
    assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
    assertThat(response.getHeader("Content-Length")).isEqualTo(String.valueOf(OCTETS));
    assertThat(response.getContentAsByteArray()).hasSize(OCTETS);
    assertThat(OCTETS).isNotEqualTo(DOCUMENT.length());
    assertThat(response.getHeader("Content-Disposition")).isNull();
    assertThat(response.getHeader("ETag")).isNull();
    assertThat(response.getHeader("Content-Encoding")).isNull();
    assertThat(response.getHeaders("Set-Cookie")).isEmpty();
  }

  /**
   * Hallazgo A8 de la feature 24: la postura de cabeceras no depende del proxy y la emiten todas
   * las cadenas de seguridad. La del feed público es una cadena más y no puede quedarse fuera.
   */
  private static final String CONTENT_SECURITY_POLICY =
      "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:;"
          + " connect-src 'self'; object-src 'none'; base-uri 'self'; frame-ancestors 'none'";

  @Test
  void s11_s15_thePublicChainAlsoEmitsTheSecurityHeadersOfTheWholeApi() throws Exception {
    for (var address :
        new String[] {"/calendar/" + TOKEN + ".ics", "/calendar/" + "z".repeat(43) + ".ics"}) {
      var response = mvc.perform(get(address)).andReturn().getResponse();
      assertThat(response.getHeader("Content-Security-Policy")).isEqualTo(CONTENT_SECURITY_POLICY);
      assertThat(response.getHeader("Referrer-Policy")).isEqualTo("same-origin");
      assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
    }
  }

  @Test
  void s11_headAnswersTheSameHeadersWithAnEmptyBody() throws Exception {
    var get = mvc.perform(get("/calendar/" + TOKEN + ".ics")).andReturn().getResponse();
    var head =
        mvc.perform(head("/calendar/" + TOKEN + ".ics"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    assertThat(head.getContentAsByteArray()).isEmpty();
    assertThat(headersOf(head)).isEqualTo(headersOf(get));
  }

  static Map<String, java.util.List<String>> headersOf(
      org.springframework.mock.web.MockHttpServletResponse response) {
    var headers = new LinkedHashMap<String, java.util.List<String>>();
    for (var name : new java.util.TreeSet<>(response.getHeaderNames()))
      headers.put(name, response.getHeaders(name));
    return headers;
  }

  @Test
  void s11_s15_theRequestLeavesNoTokenAndNoCalendarPathInTheLogs() throws Exception {
    var root =
        (ch.qos.logback.classic.Logger)
            org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    var captured =
        new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();
    captured.start();
    root.addAppender(captured);
    try {
      mvc.perform(get("/calendar/" + TOKEN + ".ics")).andExpect(status().isOk());
      mvc.perform(get("/calendar/" + "z".repeat(43) + ".ics")).andExpect(status().isNotFound());
    } finally {
      root.detachAppender(captured);
    }
    var lines =
        captured.list.stream()
            .map(ch.qos.logback.classic.spi.ILoggingEvent::getFormattedMessage)
            .toList()
            .toString();
    assertThat(lines).doesNotContain(TOKEN).doesNotContain("/calendar/");
  }

  @Test
  void s15_everyUnresolvableAddressAnswersTheSameNotFound() throws Exception {
    var expected = notFound(get("/calendar/" + TOKEN.substring(0, 42) + ".ics"));
    assertThat(notFound(get("/calendar/" + TOKEN + "A.ics"))).isEqualTo(expected);
    assertThat(notFound(get("/calendar/" + TOKEN.substring(0, 42) + "+.ics"))).isEqualTo(expected);
    assertThat(notFound(get("/calendar/" + "z".repeat(43) + ".ics"))).isEqualTo(expected);
    assertThat(notFound(get("/calendar/" + TOKEN))).isEqualTo(expected);
    assertThat(notFound(get("/calendar/" + TOKEN + ".ics").param("x", "1"))).isEqualTo(expected);
    assertThat(notFound(get("/calendar/"))).isEqualTo(expected);
    assertThat(notFound(get("/calendar/otra/" + TOKEN + ".ics"))).isEqualTo(expected);
  }

  String notFound(
      org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
      throws Exception {
    var response =
        mvc.perform(request)
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.code").value("CALENDAR_NOT_FOUND"))
            .andReturn()
            .getResponse();
    assertThat(response.getHeader("Cache-Control")).isEqualTo("private, no-store");
    assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
    assertThat(response.getHeaders("Set-Cookie")).isEmpty();
    assertThat(response.getContentAsString()).doesNotContain(TOKEN);
    return headersOf(response) + "|" + response.getContentAsString();
  }

  @ParameterizedTest
  @ValueSource(strings = {"POST", "PUT", "DELETE", "PATCH"})
  void s16_thePublicResourceOnlyAcceptsGetAndHead(String method) throws Exception {
    var verb = org.springframework.http.HttpMethod.valueOf(method);
    var known = notAllowed(verb, "/calendar/" + TOKEN + ".ics");
    var unknown = notAllowed(verb, "/calendar/" + "q".repeat(43) + ".ics");
    assertThat(known).isEqualTo(unknown);
    verifyNoInteractions(calendars);
  }

  String notAllowed(org.springframework.http.HttpMethod verb, String route) throws Exception {
    var response =
        mvc.perform(request(verb, route))
            .andExpect(status().isMethodNotAllowed())
            .andReturn()
            .getResponse();
    assertThat(response.getHeader("Allow")).isEqualTo("GET, HEAD");
    assertThat(response.getHeaders("Set-Cookie")).isEmpty();
    return response.getStatus() + "|" + headersOf(response) + "|" + response.getContentAsString();
  }

  // ---------- session download ----------

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void s17_theSessionDownloadIsTheSameDocumentAsAnAttachment(boolean active) throws Exception {
    lenient()
        .when(feeds.status("owner"))
        .thenReturn(new CalendarFeedStatus(active, active ? CREATED_AT : null));
    var response =
        mvc.perform(get("/api/v1/me/calendar.ics").with(user("owner")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    assertThat(response.getContentAsByteArray())
        .isEqualTo(DOCUMENT.getBytes(StandardCharsets.UTF_8));
    assertThat(response.getHeader("Content-Type")).isEqualTo("text/calendar; charset=utf-8");
    assertThat(response.getHeader("Cache-Control")).isEqualTo("private, no-store");
    assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
    assertThat(response.getHeader("Content-Length")).isEqualTo(String.valueOf(OCTETS));
    assertThat(response.getHeader("Content-Disposition"))
        .isEqualTo("attachment; filename=\"organizationweb-bloques.ics\"");
    verify(feeds, never()).generate(any());
    verify(feeds, never()).revoke(any());
  }

  // ---------- failures ----------

  @Test
  void s26_tooManyEventsAnswerThirteenWithoutPartialCalendar() throws Exception {
    when(calendars.forToken(TOKEN)).thenThrow(new CalendarTooLargeException());
    when(calendars.forOwner("owner")).thenThrow(new CalendarTooLargeException());
    for (var result :
        new MvcResult[] {
          mvc.perform(get("/calendar/" + TOKEN + ".ics")).andReturn(),
          mvc.perform(get("/api/v1/me/calendar.ics").with(user("owner"))).andReturn()
        }) {
      var response = result.getResponse();
      assertThat(response.getStatus()).isEqualTo(413);
      assertThat(response.getContentType()).startsWith("application/problem+json");
      assertThat(response.getContentAsString())
          .contains("CALENDAR_TOO_LARGE")
          .doesNotContain("BEGIN:VCALENDAR");
      assertThat(response.getHeader("Content-Disposition")).isNull();
    }
  }

  @Test
  void s30_anUnavailableStoreAnswersFiveHundredThreeOnEveryRoute() throws Exception {
    var failure = new StorageUnavailableException(new IllegalStateException("down"));
    when(calendars.forToken(TOKEN)).thenThrow(failure);
    when(calendars.forOwner("owner")).thenThrow(failure);
    when(feeds.status("owner")).thenThrow(failure);
    for (var result :
        new MvcResult[] {
          mvc.perform(get("/calendar/" + TOKEN + ".ics")).andReturn(),
          mvc.perform(get("/api/v1/me/calendar.ics").with(user("owner"))).andReturn(),
          mvc.perform(get("/api/v1/me/calendar-feed").with(user("owner"))).andReturn()
        }) {
      var response = result.getResponse();
      assertThat(response.getStatus()).isEqualTo(503);
      assertThat(response.getContentType()).startsWith("application/problem+json");
      assertThat(response.getContentAsString())
          .contains("STORAGE_UNAVAILABLE")
          .doesNotContain("BEGIN:VCALENDAR")
          .doesNotContain(TOKEN)
          .doesNotContain("IllegalStateException");
      assertThat(response.getHeader("Content-Disposition")).isNull();
    }
  }

  @Test
  void s10_aFailedGenerationAnswersFiveHundredThreeWithoutUrl() throws Exception {
    when(feeds.generate("owner"))
        .thenThrow(new StorageUnavailableException(new IllegalStateException("unique")));
    var body =
        mvc.perform(post("/api/v1/me/calendar-feed").with(user("owner")).with(csrf().asHeader()))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(body).doesNotContain("url").doesNotContain(TOKEN);
    verify(feeds, times(1)).generate("owner");
  }
}
