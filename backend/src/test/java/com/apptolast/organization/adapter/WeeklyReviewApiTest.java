package com.apptolast.organization.adapter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.apptolast.organization.adapter.config.SecurityConfiguration;
import com.apptolast.organization.adapter.http.WeeklyReviewController;
import com.apptolast.organization.application.ReadWeeklyReviewUseCase;
import com.apptolast.organization.domain.WeeklyReview;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = WeeklyReviewController.class,
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@Import(SecurityConfiguration.class)
class WeeklyReviewApiTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @MockitoBean ReadWeeklyReviewUseCase weekly;

  @Test
  void s6_unrepresentableCivilWeekPreservesApplicationDateError() throws Exception {
    var date = LocalDate.parse("9999-12-31");
    when(weekly.get("owner", date, null))
        .thenThrow(
            new com.apptolast.organization.domain.ValidationException(
                java.util.List.of(
                    new com.apptolast.organization.domain.FieldError(
                        "date", "INVALID_VALUE", "Semana fuera de rango."))));
    mvc.perform(get("/api/v1/weekly-review").with(user("owner")).param("date", "9999-12-31"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("date"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verify(weekly).get("owner", date, null);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"", "Not/AZone", " UTC "})
  void s4_zoneValidationFromApplicationPreservesExactInputAndField(String zone) throws Exception {
    when(weekly.get("owner", null, zone))
        .thenThrow(
            new com.apptolast.organization.domain.ValidationException(
                java.util.List.of(
                    new com.apptolast.organization.domain.FieldError(
                        "zoneId", "INVALID_VALUE", "Zona inválida."))));
    mvc.perform(get("/api/v1/weekly-review").with(user("owner")).param("zoneId", zone))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("zoneId"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verify(weekly).get("owner", null, zone);
  }

  @Test
  void s6_temporalFailureHasItsOwnRecoverableProblem() throws Exception {
    when(weekly.get("owner", null, null))
        .thenThrow(new com.apptolast.organization.domain.WeeklyReviewTimeOutOfRangeException());
    mvc.perform(get("/api/v1/weekly-review").with(user("owner")))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("WEEKLY_REVIEW_TIME_OUT_OF_RANGE"))
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.days").doesNotExist());
  }

  @Test
  void s23_storageFailureUsesProblemWithoutPartialWeekOrSql() throws Exception {
    when(weekly.get("owner", null, null))
        .thenThrow(
            new com.apptolast.organization.application.StorageUnavailableException(
                new IllegalStateException("private SQL diagnostic")));
    var response =
        mvc.perform(get("/api/v1/weekly-review").with(user("owner")))
            .andExpect(status().isServiceUnavailable())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"))
            .andExpect(jsonPath("$.days").doesNotExist())
            .andReturn()
            .getResponse();
    assertThat(response.getContentAsString()).doesNotContain("private SQL diagnostic");
  }

  @Test
  void s5_authenticationPrecedesInvalidQuery() throws Exception {
    mvc.perform(get("/api/v1/weekly-review").param("owner", "other"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    verifyNoInteractions(weekly);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"", "2026-02-30", "0000-01-01", "10000-01-01", "2026-9-09"})
  void s4_invalidCivilDateUsesExistingStrictParser(String date) throws Exception {
    mvc.perform(get("/api/v1/weekly-review").with(user("owner")).param("date", date))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("date"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(weekly);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"date", "zoneId"})
  void s4_duplicateKnownParameterIsRejected(String field) throws Exception {
    var value = field.equals("date") ? "2026-09-09" : "UTC";
    mvc.perform(get("/api/v1/weekly-review").with(user("owner")).param(field, value, value))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("query"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(weekly);
  }

  @Test
  void s4_unknownQueryIsRejectedBeforeCallingApplication() throws Exception {
    mvc.perform(get("/api/v1/weekly-review").with(user("owner")).param("owner", "other"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("query"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(weekly);
  }

  @Test
  void s2_explicitSelectionAndLongValuesArePreservedWithoutNormalizingInHttp() throws Exception {
    var requested = LocalDate.parse("2026-09-09");
    var monday = LocalDate.parse("2026-09-07");
    var start = Instant.parse("2026-09-06T22:00:00Z");
    var days =
        IntStream.range(0, 7)
            .mapToObj(
                i ->
                    new WeeklyReview.Day(
                        monday.plusDays(i),
                        start.plusSeconds(i * 86400L),
                        start.plusSeconds((i + 1) * 86400L),
                        i == 0 ? 1800000001L : 0,
                        i == 0 ? 1000001 : 0,
                        7200000000L))
            .toList();
    when(weekly.get("owner", requested, "Europe/Madrid"))
        .thenReturn(
            new WeeklyReview(
                monday,
                monday.plusDays(6),
                "Europe/Madrid",
                "EXPLICIT",
                "Europe/Madrid",
                Instant.parse("2026-09-09T12:00:00.123457Z"),
                start,
                start.plusSeconds(604800),
                days,
                new WeeklyReview.Totals(1800000001L, 1000001, 50400000000L),
                2));
    mvc.perform(
            get("/api/v1/weekly-review")
                .with(user("owner"))
                .param("date", "2026-09-09")
                .param("zoneId", "Europe/Madrid"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.days[0].plannedMicroseconds").value("1800000001"))
        .andExpect(jsonPath("$.totals.plannedMicroseconds").value("1800000001"))
        .andExpect(jsonPath("$.totals.workedMicroseconds").value("1000001"))
        .andExpect(jsonPath("$.days[0].capacityMicroseconds").value("7200000000"))
        .andExpect(jsonPath("$.totals.capacityMicroseconds").value("50400000000"))
        .andExpect(jsonPath("$.unquantifiedSessionCount").value("2"));
    verify(weekly).get("owner", requested, "Europe/Madrid");
  }

  @Test
  void s1_emptyWeekUsesExactPublicShapeAndPrincipalWithoutCsrf() throws Exception {
    var monday = LocalDate.parse("2026-09-07");
    var start = Instant.parse("2026-09-07T00:00:00Z");
    var now = Instant.parse("2026-09-09T12:00:00.123456Z");
    var days =
        IntStream.range(0, 7)
            .mapToObj(
                i ->
                    new WeeklyReview.Day(
                        monday.plusDays(i),
                        start.plusSeconds(i * 86400L),
                        start.plusSeconds((i + 1) * 86400L),
                        0,
                        0,
                        null))
            .toList();
    when(weekly.get("owner", null, null))
        .thenReturn(
            new WeeklyReview(
                monday,
                monday.plusDays(6),
                "UTC",
                "UNCONFIGURED",
                null,
                now,
                start,
                start.plusSeconds(604800),
                days,
                new WeeklyReview.Totals(0, 0, null),
                0));
    var response =
        mvc.perform(get("/api/v1/weekly-review").with(user("owner")))
            .andExpect(status().isOk())
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(header().doesNotExist("ETag"))
            .andReturn()
            .getResponse();
    var body = json.readTree(response.getContentAsString());
    assertThat(body.size()).isEqualTo(11);
    assertThat(body.get("weekStart").asText()).isEqualTo("2026-09-07");
    assertThat(body.get("weekEnd").asText()).isEqualTo("2026-09-13");
    assertThat(body.get("zoneId").asText()).isEqualTo("UTC");
    assertThat(body.get("zoneSource").asText()).isEqualTo("UNCONFIGURED");
    assertThat(body.get("availabilityZoneId").isNull()).isTrue();
    assertThat(body.get("serverNow").asText()).isEqualTo(now.toString());
    assertThat(body.get("startAt").asText()).isEqualTo(start.toString());
    assertThat(body.get("endAt").asText()).isEqualTo(start.plusSeconds(604800).toString());
    assertThat(body.get("unquantifiedSessionCount")).isEqualTo(json.readTree("\"0\""));
    assertThat(body.get("totals"))
        .isEqualTo(
            json.readTree(
                """
        {"plannedMicroseconds":"0","workedMicroseconds":"0","capacityMicroseconds":null}
        """));
    assertThat(body.get("days").size()).isEqualTo(7);
    for (int i = 0; i < 7; i++) {
      var day = body.get("days").get(i);
      assertThat(day.size()).isEqualTo(6);
      assertThat(day.get("date").asText()).isEqualTo(monday.plusDays(i).toString());
      assertThat(day.get("startAt").asText()).isEqualTo(days.get(i).startAt().toString());
      assertThat(day.get("endAt").asText()).isEqualTo(days.get(i).endAt().toString());
      assertThat(day.get("plannedMicroseconds")).isEqualTo(json.readTree("\"0\""));
      assertThat(day.get("workedMicroseconds")).isEqualTo(json.readTree("\"0\""));
      assertThat(day.get("capacityMicroseconds").isNull()).isTrue();
    }
    verify(weekly).get("owner", null, null);
    verifyNoMoreInteractions(weekly);
  }
}
