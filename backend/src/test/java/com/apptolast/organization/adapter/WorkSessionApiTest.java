package com.apptolast.organization.adapter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.apptolast.organization.adapter.config.SecurityConfiguration;
import com.apptolast.organization.adapter.http.WorkSessionController;
import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.SessionStart;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(
    controllers = WorkSessionController.class,
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@Import(SecurityConfiguration.class)
class WorkSessionApiTest {
  @Test
  void s9_taskIdPrecedesMissingKey() throws Exception {
    validation(
        post("/api/v1/projects/" + project + "/tasks/bad/work-sessions")
            .with(user("owner"))
            .with(csrf().asHeader())
            .header("Origin", "https://organization.example")
            .contentType("application/json")
            .content("{"),
        "taskId",
        "INVALID_FORMAT");
  }

  @Test
  void s8_mediaTypePrecedesQueryValidation() throws Exception {
    mvc.perform(write("{").contentType("text/plain").param("x", "1"))
        .andExpect(status().isUnsupportedMediaType());
    verifyNoInteractions(start, read);
  }

  @Test
  void s6_maximumMinutesReachUseCase() throws Exception {
    var longSession =
        new SessionStart(
            receipt.id(),
            project,
            task,
            receipt.startedAt(),
            1440,
            receipt.startedAt().plusSeconds(86400),
            receipt.zoneId());
    when(start.start("owner", project, task, key, 1440))
        .thenReturn(new WorkSessionConfirmation(longSession, false));
    var response =
        mvc.perform(write("{\"plannedMinutes\":1440}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString()))
        .isEqualTo(json.valueToTree(longSession));
    verify(start).start("owner", project, task, key, 1440);
  }

  @Test
  void s6_oneMinuteReachesUseCase() throws Exception {
    var shortSession =
        new SessionStart(
            receipt.id(),
            project,
            task,
            receipt.startedAt(),
            1,
            receipt.startedAt().plusSeconds(60),
            receipt.zoneId());
    when(start.start("owner", project, task, key, 1))
        .thenReturn(new WorkSessionConfirmation(shortSession, false));
    var response =
        mvc.perform(write("{\"plannedMinutes\":1}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString()))
        .isEqualTo(json.valueToTree(shortSession));
    verify(start).start("owner", project, task, key, 1);
  }

  @Test
  void s8_untrustedOriginPrecedesInvalidBody() throws Exception {
    mvc.perform(
            write("{")
                .with(
                    request -> {
                      request.removeHeader("Origin");
                      request.addHeader("Origin", "https://foreign.example");
                      return request;
                    }))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("UNTRUSTED_ORIGIN"));
    verifyNoInteractions(start, read);
  }

  @Test
  void s8_missingCsrfPrecedesInvalidBody() throws Exception {
    mvc.perform(
            post(path())
                .with(user("owner"))
                .header("Origin", "https://organization.example")
                .contentType("application/json")
                .content("{"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    verifyNoInteractions(start, read);
  }

  @Test
  void s8_anonymousReadCannotReachValidation() throws Exception {
    mvc.perform(get("/api/v1/work-sessions/bad").param("x", "1"))
        .andExpect(status().isUnauthorized())
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    verifyNoInteractions(start, read);
  }

  @Test
  void s20_writeStorageFailureIsClosed() throws Exception {
    when(start.start("owner", project, task, key, 25))
        .thenThrow(
            new StorageUnavailableException(new IllegalStateException("private database detail")));
    problem(
        write("{\"plannedMinutes\":25}"),
        503,
        "STORAGE_UNAVAILABLE",
        "El almacenamiento no está disponible. Inténtalo más tarde.",
        Map.of());
  }

  @Test
  void s24_keyStorageFailureIsNotMissing() throws Exception {
    when(read.byRequest("owner", key))
        .thenThrow(
            new StorageUnavailableException(new IllegalStateException("private database detail")));
    problem(
        get("/api/v1/work-sessions/by-request/" + key).with(user("owner")),
        503,
        "STORAGE_UNAVAILABLE",
        "El almacenamiento no está disponible. Inténtalo más tarde.",
        Map.of());
  }

  @Test
  void s24_detailStorageFailureIsNotMissing() throws Exception {
    when(read.detail("owner", receipt.id()))
        .thenThrow(
            new StorageUnavailableException(new IllegalStateException("private database detail")));
    problem(
        get("/api/v1/work-sessions/" + receipt.id()).with(user("owner")),
        503,
        "STORAGE_UNAVAILABLE",
        "El almacenamiento no está disponible. Inténtalo más tarde.",
        Map.of());
  }

  @Test
  void s24_activeStorageFailureIsNotAbsence() throws Exception {
    when(read.active("owner"))
        .thenThrow(
            new StorageUnavailableException(new IllegalStateException("private database detail")));
    problem(
        get("/api/v1/work-sessions/active").with(user("owner")),
        503,
        "STORAGE_UNAVAILABLE",
        "El almacenamiento no está disponible. Inténtalo más tarde.",
        Map.of());
  }

  @Test
  void s10_contextNotFoundDoesNotExposeSession() throws Exception {
    when(start.start("owner", project, task, key, 25)).thenThrow(new ResourceNotFoundException());
    problem(
        write("{\"plannedMinutes\":25}"),
        404,
        "RESOURCE_NOT_FOUND",
        "No se ha encontrado el recurso.",
        Map.of());
  }

  @Test
  void s22_byRequestUsesSameClosedMissingProblem() throws Exception {
    when(read.byRequest("owner", key)).thenThrow(new WorkSessionNotFoundException());
    problem(
        get("/api/v1/work-sessions/by-request/" + key).with(user("owner")),
        404,
        "WORK_SESSION_NOT_FOUND",
        "No se ha encontrado la sesión de trabajo.",
        Map.of());
  }

  @Test
  void s23_confirmedAbsenceUsesExplicitNull() throws Exception {
    when(read.active("owner")).thenReturn(Optional.empty());
    mvc.perform(get("/api/v1/work-sessions/active").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"session\":null}", true));
    verify(read).active("owner");
    verifyNoInteractions(start);
  }

  @Test
  void s9_pathIdPrecedesMissingKey() throws Exception {
    validation(
        post("/api/v1/projects/bad/tasks/bad/work-sessions")
            .with(user("owner"))
            .with(csrf().asHeader())
            .header("Origin", "https://organization.example")
            .contentType("application/json")
            .content("{"),
        "projectId",
        "INVALID_FORMAT");
  }

  @Test
  void s9_queryPrecedesInvalidPathAndMissingKey() throws Exception {
    validation(
        post("/api/v1/projects/bad/tasks/bad/work-sessions")
            .with(user("owner"))
            .with(csrf().asHeader())
            .header("Origin", "https://organization.example")
            .contentType("application/json")
            .content("{")
            .param("x", "1"),
        "query",
        "INVALID_VALUE");
  }

  @Test
  void s9_badKeyRejectedBeforeBody() throws Exception {
    validation(
        write("{")
            .with(
                request -> {
                  request.removeHeader("Idempotency-Key");
                  request.addHeader("Idempotency-Key", "bad");
                  return request;
                }),
        "Idempotency-Key",
        "INVALID_FORMAT");
  }

  @Test
  void s9_repeatedKeyRejectedBeforeBody() throws Exception {
    validation(write("{").header("Idempotency-Key", key), "Idempotency-Key", "INVALID_VALUE");
  }

  @Test
  void s7_truncatedJsonIsMalformed() throws Exception {
    mvc.perform(write("{\"plannedMinutes\":"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    verifyNoInteractions(start, read);
  }

  @Test
  void s7_trailingJsonIsMalformed() throws Exception {
    mvc.perform(write("{\"plannedMinutes\":25}{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    verifyNoInteractions(start, read);
  }

  @Test
  void s6_largeIntegerCannotWrapIntoValidMinutes() throws Exception {
    validation(write("{\"plannedMinutes\":4294967321}"), "plannedMinutes", "OUT_OF_RANGE");
  }

  @Test
  void s6_durationAboveMaximumIsRejected() throws Exception {
    validation(write("{\"plannedMinutes\":1441}"), "plannedMinutes", "OUT_OF_RANGE");
  }

  @Test
  void s6_fractionalDurationIsInvalidType() throws Exception {
    validation(write("{\"plannedMinutes\":1.5}"), "plannedMinutes", "INVALID_TYPE");
  }

  @Test
  void s6_booleanDurationIsInvalidType() throws Exception {
    validation(write("{\"plannedMinutes\":true}"), "plannedMinutes", "INVALID_TYPE");
  }

  @Test
  void s6_nullDurationIsRequired() throws Exception {
    validation(write("{\"plannedMinutes\":null}"), "plannedMinutes", "REQUIRED");
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @MockitoBean StartWorkSessionUseCase start;
  @MockitoBean ReadWorkSessionsUseCase read;
  final UUID project = UUID.randomUUID(), task = UUID.randomUUID(), key = UUID.randomUUID();
  final SessionStart receipt =
      new SessionStart(
          UUID.randomUUID(),
          project,
          task,
          Instant.parse("2026-09-06T10:00:00.123456Z"),
          25,
          Instant.parse("2026-09-06T10:25:00.123456Z"),
          "Europe/Madrid");

  String path() {
    return "/api/v1/projects/" + project + "/tasks/" + task + "/work-sessions";
  }

  MockHttpServletRequestBuilder write(String body) {
    return post(path())
        .with(user("owner"))
        .with(csrf().asHeader())
        .header("Origin", "https://organization.example")
        .header("Idempotency-Key", key)
        .contentType("application/json")
        .content(body);
  }

  @Test
  void s1_createdStartHasExactSevenFieldsLocationAndNoStore() throws Exception {
    when(start.start("owner", project, task, key, 25))
        .thenReturn(new WorkSessionConfirmation(receipt, false));
    var result =
        mvc.perform(write("{\"plannedMinutes\":25}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/work-sessions/" + receipt.id()))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(result.getContentAsString())).isEqualTo(json.valueToTree(receipt));
    assertThat(json.readTree(result.getContentAsString()).size()).isEqualTo(7);
    verify(start).start("owner", project, task, key, 25);
    verifyNoMoreInteractions(start);
    verifyNoInteractions(read);
  }

  @Test
  void s14_replayUses200AndOriginalLocation() throws Exception {
    when(start.start("owner", project, task, key, 25))
        .thenReturn(new WorkSessionConfirmation(receipt, true));
    var response =
        mvc.perform(write("{\"plannedMinutes\":25}"))
            .andExpect(status().isOk())
            .andExpect(header().string("Location", "/api/v1/work-sessions/" + receipt.id()))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString())).isEqualTo(json.valueToTree(receipt));
  }

  @Test
  void s8_queryRejectedBeforeMalformedBodyAndUseCase() throws Exception {
    validation(write("{").param("x", "1"), "query", "INVALID_VALUE");
  }

  @Test
  void s9_missingKeyPrecedesMalformedBody() throws Exception {
    validation(
        write("{")
            .with(
                request -> {
                  request.removeHeader("Idempotency-Key");
                  return request;
                }),
        "Idempotency-Key",
        "REQUIRED");
  }

  @Test
  void s9_queryPrecedesEmptyBody() throws Exception {
    validation(write("").param("x", "1"), "query", "INVALID_VALUE");
  }

  @Test
  void s7_emptyBodyIsMalformedJson() throws Exception {
    mvc.perform(write(""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    verifyNoInteractions(start, read);
  }

  @Test
  void s7_duplicateJsonIsRejectedBeforeReplay() throws Exception {
    when(start.start("owner", project, task, key, 25))
        .thenReturn(new WorkSessionConfirmation(receipt, true));
    mvc.perform(write("{\"plannedMinutes\":25,\"plannedMinutes\":25}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    verifyNoInteractions(start, read);
  }

  @Test
  void s7_rootArrayHasBodyTypeError() throws Exception {
    validation(write("[]"), "body", "INVALID_TYPE");
  }

  @Test
  void s7_extraFieldUsesLexicalOrderBeforeMissingMinutes() throws Exception {
    validation(write("{\"objective\":\"x\",\"blockId\":\"x\"}"), "blockId", "UNKNOWN_FIELD");
  }

  @Test
  void s6_missingDurationIsRequired() throws Exception {
    validation(write("{}"), "plannedMinutes", "REQUIRED");
  }

  @Test
  void s6_stringDurationIsNotCoerced() throws Exception {
    validation(write("{\"plannedMinutes\":\"25\"}"), "plannedMinutes", "INVALID_TYPE");
  }

  @Test
  void s6_zeroDurationRejectedBeforeUseCase() throws Exception {
    validation(write("{\"plannedMinutes\":0}"), "plannedMinutes", "OUT_OF_RANGE");
  }

  @Test
  void s23_activeReturnsOwnSessionWithoutTaskRoute() throws Exception {
    when(read.active("owner")).thenReturn(Optional.of(receipt));
    var response =
        mvc.perform(get("/api/v1/work-sessions/active").with(user("owner")))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist("Location"))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString()))
        .isEqualTo(json.valueToTree(Map.of("session", receipt)));
    verify(read).active("owner");
    verifyNoMoreInteractions(read);
    verifyNoInteractions(start);
  }

  @Test
  void s21_detailReturnsHistoricalReceiptWithoutLocation() throws Exception {
    when(read.detail("owner", receipt.id())).thenReturn(receipt);
    var response =
        mvc.perform(get("/api/v1/work-sessions/" + receipt.id()).with(user("owner")))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist("Location"))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString())).isEqualTo(json.valueToTree(receipt));
    verify(read).detail("owner", receipt.id());
    verifyNoMoreInteractions(read);
    verifyNoInteractions(start);
  }

  @Test
  void s21_byRequestUsesLiteralRouteAndOwnerKey() throws Exception {
    when(read.byRequest("owner", key)).thenReturn(receipt);
    var response =
        mvc.perform(get("/api/v1/work-sessions/by-request/" + key).with(user("owner")))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist("Location"))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString())).isEqualTo(json.valueToTree(receipt));
    verify(read).byRequest("owner", key);
    verifyNoMoreInteractions(read);
    verifyNoInteractions(start);
  }

  @Test
  void s9_detailQueryPrecedesInvalidId() throws Exception {
    validation(
        get("/api/v1/work-sessions/bad").with(user("owner")).param("x", "1"),
        "query",
        "INVALID_VALUE");
  }

  @Test
  void s8_activeRejectsRepeatedQuery() throws Exception {
    validation(
        get("/api/v1/work-sessions/active").with(user("owner")).param("x", "1", "2"),
        "query",
        "INVALID_VALUE");
  }

  @Test
  void s9_byRequestQueryPrecedesInvalidKey() throws Exception {
    validation(
        get("/api/v1/work-sessions/by-request/bad").with(user("owner")).param("x", "1"),
        "query",
        "INVALID_VALUE");
  }

  @Test
  void s22_missingSessionUsesClosed404Problem() throws Exception {
    when(read.detail("owner", receipt.id())).thenThrow(new WorkSessionNotFoundException());
    problem(
        get("/api/v1/work-sessions/" + receipt.id()).with(user("owner")),
        404,
        "WORK_SESSION_NOT_FOUND",
        "No se ha encontrado la sesión de trabajo.",
        Map.of());
  }

  @Test
  void s12_activeConflictOnlyAddsOwnSessionId() throws Exception {
    when(start.start("owner", project, task, key, 25))
        .thenThrow(new WorkSessionAlreadyActiveException(receipt.id()));
    problem(
        write("{\"plannedMinutes\":25}"),
        409,
        "WORK_SESSION_ALREADY_ACTIVE",
        "Ya existe una sesión de trabajo activa.",
        Map.of("sessionId", receipt.id()));
  }

  @Test
  void s15_differentIntentionIsClosedConflict() throws Exception {
    when(start.start("owner", project, task, key, 25))
        .thenThrow(new WorkSessionIdempotencyConflictException());
    problem(
        write("{\"plannedMinutes\":25}"),
        409,
        "IDEMPOTENCY_CONFLICT",
        "La clave corresponde a otra intención de inicio de trabajo.",
        Map.of());
  }

  @Test
  void s13_unrepresentableTimeIsClosedConflictNotFieldError() throws Exception {
    when(start.start("owner", project, task, key, 25))
        .thenThrow(new WorkSessionTimeOutOfRangeException());
    problem(
        write("{\"plannedMinutes\":25}"),
        409,
        "WORK_SESSION_TIME_OUT_OF_RANGE",
        "No se puede representar el inicio y el fin previsto de la sesión.",
        Map.of());
  }

  @Test
  void s11_completedProjectExplainsStartingWork() throws Exception {
    when(start.start("owner", project, task, key, 25)).thenThrow(new ProjectCompletedException());
    problem(
        write("{\"plannedMinutes\":25}"),
        409,
        "PROJECT_COMPLETED",
        "Reabre el proyecto antes de iniciar trabajo.",
        Map.of());
  }

  @Test
  void s11_completedTaskExplainsStartingWork() throws Exception {
    when(start.start("owner", project, task, key, 25)).thenThrow(new TaskCompletedException());
    problem(
        write("{\"plannedMinutes\":25}"),
        409,
        "TASK_COMPLETED",
        "Reabre la tarea antes de iniciar trabajo.",
        Map.of());
  }

  void problem(
      MockHttpServletRequestBuilder request,
      int statusCode,
      String code,
      String title,
      Map<String, Object> extra)
      throws Exception {
    var response =
        mvc.perform(request)
            .andExpect(status().is(statusCode))
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse();
    var expected =
        new HashMap<String, Object>(
            Map.of(
                "type",
                "urn:organization:problem:" + code.toLowerCase(Locale.ROOT),
                "title",
                title,
                "status",
                statusCode,
                "code",
                code));
    expected.putAll(extra);
    assertThat(json.readTree(response.getContentAsString())).isEqualTo(json.valueToTree(expected));
  }

  void validation(MockHttpServletRequestBuilder request, String field, String code)
      throws Exception {
    mvc.perform(request)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.*", org.hamcrest.Matchers.hasSize(5)))
        .andExpect(jsonPath("$.errors", org.hamcrest.Matchers.hasSize(1)))
        .andExpect(jsonPath("$.errors[0].*", org.hamcrest.Matchers.hasSize(3)))
        .andExpect(jsonPath("$.errors[0].field").value(field))
        .andExpect(jsonPath("$.errors[0].code").value(code));
    verifyNoInteractions(start, read);
  }
}
