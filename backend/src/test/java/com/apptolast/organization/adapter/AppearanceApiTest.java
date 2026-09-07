package com.apptolast.organization.adapter;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.apptolast.organization.adapter.config.SecurityConfiguration;
import com.apptolast.organization.adapter.http.AppearanceController;
import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = AppearanceController.class,
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@Import(SecurityConfiguration.class)
class AppearanceApiTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @MockitoBean ReadAppearanceUseCase read;
  @MockitoBean SaveAppearanceUseCase save;

  @Test
  void s12_repeatedIdenticalHeaderValuesAreNotCollapsed() throws Exception {
    mvc.perform(
            put("/api/v1/me/appearance")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"appearance:unconfigured\"", "\"appearance:unconfigured\"")
                .contentType("application/json")
                .content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("If-Match"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(read, save);
  }

  @ParameterizedTest
  @CsvSource({
    "anonymousGet,401",
    "anonymousPut,401",
    "noCsrf,403",
    "foreignOrigin,403",
    "plainText,415"
  })
  void s15_securityAndNegotiationPrecedeHandler(String scenario, int expected) throws Exception {
    var request =
        scenario.equals("anonymousGet")
            ? get("/api/v1/me/appearance")
            : put("/api/v1/me/appearance");
    if (!scenario.startsWith("anonymous")) request.with(user("owner"));
    if (!scenario.equals("noCsrf")) request.with(csrf().asHeader());
    if (scenario.equals("foreignOrigin")) request.header("Origin", "https://foreign.example");
    mvc.perform(
            request
                .queryParam("forbidden", "true")
                .contentType(scenario.equals("plainText") ? "text/plain" : "application/json")
                .content("{"))
        .andExpect(status().is(expected));
    verifyNoInteractions(read, save);
  }

  @ParameterizedTest
  @ValueSource(strings = {"GET", "PUT"})
  void s7_s16_storageFailureIsNotAnAbsenceOrConfirmation(String method) throws Exception {
    var failure =
        new StorageUnavailableException(new IllegalStateException("private database detail"));
    var request =
        method.equals("GET") ? get("/api/v1/me/appearance") : put("/api/v1/me/appearance");
    if (method.equals("GET")) when(read.get("owner")).thenThrow(failure);
    else
      when(save.execute("owner", new AppearanceRevision(null, 0), "SYSTEM", "#244C3C", "#B7E4C7"))
          .thenThrow(failure);
    var response =
        mvc.perform(
                request
                    .with(user("owner"))
                    .with(csrf().asHeader())
                    .header("If-Match", "\"appearance:unconfigured\"")
                    .contentType("application/json")
                    .content(
                        "{\"theme\":\"SYSTEM\",\"accentLight\":\"#244C3C\",\"accentDark\":\"#B7E4C7\"}"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"))
            .andExpect(jsonPath("$.type").value("urn:organization:problem:storage_unavailable"))
            .andExpect(jsonPath("$.configured").doesNotExist())
            .andExpect(header().doesNotExist("ETag"))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse();
    assertThat(response.getContentAsString()).doesNotContain("private database detail");
    if (method.equals("GET")) {
      verify(read).get("owner");
      verifyNoInteractions(save);
    } else {
      verify(save)
          .execute("owner", new AppearanceRevision(null, 0), "SYSTEM", "#244C3C", "#B7E4C7");
      verifyNoInteractions(read);
    }
  }

  @Test
  void s6_staleOrForeignRevisionReturnsPrivateConflict() throws Exception {
    var expected =
        new AppearanceRevision(UUID.fromString("abcdef12-3456-4789-abcd-123456789012"), 2);
    when(save.execute("owner", expected, "SYSTEM", "#244C3C", "#B7E4C7"))
        .thenThrow(new AppearanceConflictException());
    mvc.perform(
            put("/api/v1/me/appearance")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"appearance:" + expected.id() + ":2\"")
                .contentType("application/json")
                .content(
                    "{\"theme\":\"SYSTEM\",\"accentLight\":\"#244C3C\",\"accentDark\":\"#B7E4C7\"}"))
        .andExpect(status().isPreconditionFailed())
        .andExpect(jsonPath("$.code").value("APPEARANCE_CONFLICT"))
        .andExpect(jsonPath("$.type").value("urn:organization:problem:appearance_conflict"))
        .andExpect(jsonPath("$.status").value(412))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(header().doesNotExist("ETag"))
        .andExpect(jsonPath("$.configured").doesNotExist());
    verify(save).execute("owner", expected, "SYSTEM", "#244C3C", "#B7E4C7");
    verifyNoInteractions(read);
  }

  static Stream<Arguments> invalidFields() {
    return Stream.of(
        arguments("{}", "theme", "REQUIRED"),
        arguments("{\"theme\":null}", "theme", "REQUIRED"),
        arguments("{\"theme\":3}", "theme", "INVALID_TYPE"),
        arguments("{\"theme\":\"light\",\"accentLight\":null}", "theme", "INVALID_VALUE"),
        arguments("{\"theme\":\"LIGHT\"}", "accentLight", "REQUIRED"),
        arguments("{\"theme\":\"LIGHT\",\"accentLight\":null}", "accentLight", "REQUIRED"),
        arguments("{\"theme\":\"LIGHT\",\"accentLight\":[]}", "accentLight", "INVALID_TYPE"),
        arguments("{\"theme\":\"LIGHT\",\"accentLight\":\"#123\"}", "accentLight", "INVALID_VALUE"),
        arguments(
            "{\"theme\":\"DARK\",\"accentLight\":\"#FFFFFF\",\"accentDark\":false}",
            "accentLight",
            "INSUFFICIENT_CONTRAST"),
        arguments("{\"theme\":\"LIGHT\",\"accentLight\":\"#244C3C\"}", "accentDark", "REQUIRED"),
        arguments(
            "{\"theme\":\"LIGHT\",\"accentLight\":\"#244C3C\",\"accentDark\":null}",
            "accentDark",
            "REQUIRED"),
        arguments(
            "{\"theme\":\"LIGHT\",\"accentLight\":\"#244C3C\",\"accentDark\":false}",
            "accentDark",
            "INVALID_TYPE"),
        arguments(
            "{\"theme\":\"LIGHT\",\"accentLight\":\"#244C3C\",\"accentDark\":\"rgb(1,2,3)\"}",
            "accentDark",
            "INVALID_VALUE"),
        arguments(
            "{\"theme\":\"LIGHT\",\"accentLight\":\"#244C3C\",\"accentDark\":\"#000000\"}",
            "accentDark",
            "INSUFFICIENT_CONTRAST"));
  }

  @ParameterizedTest
  @MethodSource("invalidFields")
  void s10_s11_eachFieldIsFullyValidatedBeforeTheNext(String body, String field, String code)
      throws Exception {
    mvc.perform(
            put("/api/v1/me/appearance")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"appearance:unconfigured\"")
                .contentType("application/json")
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors.length()").value(1))
        .andExpect(jsonPath("$.errors[0].field").value(field))
        .andExpect(jsonPath("$.errors[0].code").value(code));
    verifyNoInteractions(read, save);
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "[]|body|INVALID_TYPE",
        "null|body|INVALID_TYPE",
        "42|body|INVALID_TYPE",
        "{\"ownerId\":\"other\"}|ownerId|UNKNOWN_FIELD",
        "{\"zeta\":0,\"alpha\":0}|alpha|UNKNOWN_FIELD"
      })
  void s10_rootAndExtraFieldsPrecedeMissingValues(String body, String field, String code)
      throws Exception {
    mvc.perform(
            put("/api/v1/me/appearance")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("If-Match", "\"appearance:unconfigured\"")
                .contentType("application/json")
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value(field))
        .andExpect(jsonPath("$.errors[0].code").value(code));
    verifyNoInteractions(read, save);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "*",
        "W/\"appearance:unconfigured\"",
        "appearance:unconfigured",
        "\"availability:unconfigured\"",
        "\"appearance:unconfigured\",\"appearance:unconfigured\"",
        "\"appearance:ABCDEF12-3456-4789-abcd-123456789012:0\"",
        "\"appearance:abcdef12-3456-4789-abcd-123456789012:9223372036854775808\"",
        "\"appearance:abcdef12-3456-4789-abcd-123456789012:01\"",
        "\"appearance:abcdef12-3456-4789-abcd-123456789012:-1\"",
        "\"appearance:abcdef12-3456-4789-abcd-123456789012:+1\"",
        "\"appearance:1-1-1-1-1:0\"",
        " \"appearance:unconfigured\" "
      })
  void s12_invalidPreconditionIsFieldErrorBeforeMalformedBody(String tag) throws Exception {
    mvc.perform(
            put("/api/v1/me/appearance")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .header("If-Match", tag)
                .content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("If-Match"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(read, save);
  }

  @Test
  void s3_configuredTagRetainsUuidAndMaximumRevisionForNoop() throws Exception {
    var id = UUID.fromString("fedcba98-7654-4321-abcd-123456789abc");
    var expected = new AppearanceRevision(id, Long.MAX_VALUE);
    var value =
        new Appearance(
            id,
            "owner",
            "SYSTEM",
            "#244C3C",
            "#B7E4C7",
            Long.MAX_VALUE,
            Instant.parse("0001-01-01T00:00:00Z"));
    when(save.execute("owner", expected, "SYSTEM", "#244C3C", "#B7E4C7")).thenReturn(value);
    var tag = "\"appearance:" + id + ":9223372036854775807\"";
    mvc.perform(
            put("/api/v1/me/appearance")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .header("If-Match", tag)
                .content(
                    "{\"theme\":\"SYSTEM\",\"accentLight\":\"#244C3C\",\"accentDark\":\"#B7E4C7\"}"))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", tag))
        .andExpect(jsonPath("$.updatedAt").value("0001-01-01T00:00:00Z"));
    verify(save).execute("owner", expected, "SYSTEM", "#244C3C", "#B7E4C7");
  }

  @Test
  void s12_missingPreconditionPrecedesAbsentBody() throws Exception {
    mvc.perform(
            put("/api/v1/me/appearance")
                .with(user("owner"))
                .with(csrf().asHeader())
                .contentType("application/json"))
        .andExpect(status().isPreconditionRequired())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("PRECONDITION_REQUIRED"))
        .andExpect(jsonPath("$.status").value(428));
    verifyNoInteractions(read, save);
  }

  @ParameterizedTest
  @ValueSource(strings = {"GET", "PUT"})
  void s13_anyQueryPrecedesPreconditionAndBody(String method) throws Exception {
    var request =
        method.equals("GET") ? get("/api/v1/me/appearance") : put("/api/v1/me/appearance");
    mvc.perform(
            request
                .with(user("owner"))
                .with(csrf().asHeader())
                .queryParam("theme", "x", "y")
                .contentType("application/json")
                .content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("query"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(read, save);
  }

  @Test
  void s2_putUsesPrincipalAndUnconfiguredRevisionAndReturnsConfirmation() throws Exception {
    var expected = new AppearanceRevision(null, 0);
    var value =
        new Appearance(
            UUID.fromString("12345678-abcd-4123-9876-123456789abc"),
            "writer",
            "LIGHT",
            "#0000FF",
            "#00FFFF",
            0,
            Instant.parse("2026-09-07T12:00:00Z"));
    when(save.execute("writer", expected, "LIGHT", "#0000ff", "#00ffff")).thenReturn(value);
    var result =
        mvc.perform(
                put("/api/v1/me/appearance")
                    .with(user("writer"))
                    .with(csrf().asHeader())
                    .contentType("application/json")
                    .header("If-Match", "\"appearance:unconfigured\"")
                    .content(
                        "{\"theme\":\"LIGHT\",\"accentLight\":\"#0000ff\",\"accentDark\":\"#00ffff\"}"))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist("Location"))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(header().string("ETag", "\"appearance:" + value.id() + ":0\""))
            .andExpect(jsonPath("$.configured").value(true))
            .andExpect(jsonPath("$.theme").value("LIGHT"))
            .andExpect(jsonPath("$.accentLight").value("#0000FF"))
            .andExpect(jsonPath("$.accentDark").value("#00FFFF"))
            .andExpect(jsonPath("$.updatedAt").value(value.updatedAt().toString()))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(result.getContentAsString()).size()).isEqualTo(5);
    verify(save).execute("writer", expected, "LIGHT", "#0000ff", "#00ffff");
    verifyNoInteractions(read);
  }

  @Test
  void s2_configuredReadKeepsExactTimestampAndBigintInTagOnly() throws Exception {
    var value =
        new Appearance(
            UUID.fromString("abcdef12-3456-4789-abcd-123456789012"),
            "second-owner",
            "DARK",
            "#0000FF",
            "#00FFFF",
            Long.MAX_VALUE,
            Instant.parse("2026-09-07T12:00:00.123456Z"));
    when(read.get("second-owner")).thenReturn(Optional.of(value));
    var response =
        mvc.perform(get("/api/v1/me/appearance").with(user("second-owner")))
            .andExpect(status().isOk())
            .andExpect(
                header().string("ETag", "\"appearance:" + value.id() + ":9223372036854775807\""))
            .andExpect(jsonPath("$.configured").value(true))
            .andExpect(jsonPath("$.theme").value("DARK"))
            .andExpect(jsonPath("$.accentLight").value("#0000FF"))
            .andExpect(jsonPath("$.accentDark").value("#00FFFF"))
            .andExpect(jsonPath("$.updatedAt").value(value.updatedAt().toString()))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString()).size()).isEqualTo(5);
    verify(read).get("second-owner");
    verifyNoInteractions(save);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {
        " ",
        "{",
        "{} {}",
        "{\"theme\":\"LIGHT\",\"theme\":\"DARK\"}",
        "{\"accentLight\":\"#0000FF\",\"accentLight\":\"#244C3C\"}"
      })
  void s14_malformedJsonNeverReachesApplication(String body) throws Exception {
    var request =
        put("/api/v1/me/appearance")
            .with(user("owner"))
            .with(csrf().asHeader())
            .contentType("application/json")
            .header("If-Match", "\"appearance:unconfigured\"");
    if (body != null) request.content(body);
    mvc.perform(request)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    verifyNoInteractions(read, save);
  }

  @Test
  void s1_absenceReturnsExactDefaultsAndPrivateCachePolicy() throws Exception {
    when(read.get("owner")).thenReturn(Optional.empty());
    var response =
        mvc.perform(get("/api/v1/me/appearance").with(user("owner")))
            .andExpect(status().isOk())
            .andExpect(header().string("ETag", "\"appearance:unconfigured\""))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString()))
        .isEqualTo(
            json.readTree(
                "{\"configured\":false,\"theme\":\"SYSTEM\",\"accentLight\":\"#244C3C\",\"accentDark\":\"#B7E4C7\",\"updatedAt\":null}"));
    verify(read).get("owner");
    verifyNoInteractions(save);
  }
}
