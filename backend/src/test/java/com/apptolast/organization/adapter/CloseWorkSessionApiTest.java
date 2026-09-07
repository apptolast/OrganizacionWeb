package com.apptolast.organization.adapter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.apptolast.organization.adapter.config.SecurityConfiguration;
import com.apptolast.organization.adapter.http.WorkSessionStateController;
import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(
    controllers = WorkSessionStateController.class,
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@Import(SecurityConfiguration.class)
class CloseWorkSessionApiTest {
  @Test
  void s27_closureStorageFailureDoesNotBecomeAbsence() throws Exception {
    when(receipts.closure("owner", id))
        .thenThrow(new StorageUnavailableException(new IllegalStateException("private SQL")));
    problem(
        get("/api/v1/work-sessions/" + id + "/closure").with(user("owner")),
        503,
        "STORAGE_UNAVAILABLE",
        "El almacenamiento no está disponible. Inténtalo más tarde.");
    verifyNoInteractions(change, states);
  }

  @Test
  void s26_openOwnedSessionHasChangeNotFoundProblem() throws Exception {
    when(receipts.closure("owner", id)).thenThrow(new WorkSessionChangeNotFoundException());
    problem(
        get("/api/v1/work-sessions/" + id + "/closure").with(user("owner")),
        404,
        "WORK_SESSION_CHANGE_NOT_FOUND",
        "No se ha encontrado el cambio de sesión.");
    verify(receipts).closure("owner", id);
    verifyNoInteractions(change, states);
  }

  @Test
  void s26_closureOfOtherOwnerHasSessionNotFoundProblem() throws Exception {
    when(receipts.closure("owner", id)).thenThrow(new WorkSessionNotFoundException());
    problem(
        get("/api/v1/work-sessions/" + id + "/closure").with(user("owner")),
        404,
        "WORK_SESSION_NOT_FOUND",
        "No se ha encontrado la sesión de trabajo.");
    verify(receipts).closure("owner", id);
    verifyNoInteractions(change, states);
  }

  @Test
  void s6_closureQueryPrecedesInvalidSessionIdentifier() throws Exception {
    mvc.perform(get("/api/v1/work-sessions/invalid/closure").with(user("owner")).param("x", "1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("query"));
    verifyNoInteractions(change, states, receipts);
  }

  @Test
  void s26_closureBySessionReturnsOriginalReceiptWithoutCurrentStateLookup() throws Exception {
    when(receipts.closure("owner", id)).thenReturn(receipt);
    var response =
        mvc.perform(get("/api/v1/work-sessions/" + id + "/closure").with(user("owner")))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist("Location"))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString()))
        .isEqualTo(json.valueToTree(expectedReceipt()));
    verify(receipts).closure("owner", id);
    verifyNoInteractions(change, states);
  }

  @Test
  void s7_temporalRejectionPreservesTitleFromTransitions() throws Exception {
    when(change.close(
            "owner", id, key, new WorkSessionRevision(id, 1), new WorkSessionCloseNotes("", "")))
        .thenThrow(new WorkSessionTransitionException("WORK_SESSION_TIME_OUT_OF_RANGE"));
    problem(
        write("{}"),
        409,
        "WORK_SESSION_TIME_OUT_OF_RANGE",
        "No se puede registrar la transición en ese instante.");
    verifyNoInteractions(states, receipts);
  }

  @Test
  void s16_changedIntentionHasClosedConflictResponse() throws Exception {
    when(change.close("owner", id, key, new WorkSessionRevision(id, 1), notes))
        .thenThrow(new WorkSessionIdempotencyConflictException());
    problem(
        write("{\"progressNote\":\"Avance\",\"nextStep\":\"Continuar\"}"),
        409,
        "IDEMPOTENCY_CONFLICT",
        "La clave corresponde a otra intención de cambio de sesión.");
    verifyNoInteractions(states, receipts);
  }

  @Test
  void s24_closedStateKeepsSixFieldsAndSnapshotRevision() throws Exception {
    var now = end.plusSeconds(3600);
    when(states.read("owner", id)).thenReturn(new WorkSessionSnapshot(after, now, 60000001));
    var response =
        mvc.perform(get("/api/v1/work-sessions/" + id + "/state").with(user("owner")))
            .andExpect(status().isOk())
            .andExpect(header().string("Work-Session-Revision", "work-session-" + id + "-2"))
            .andExpect(header().doesNotExist("ETag"))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString()))
        .isEqualTo(
            json.valueToTree(
                Map.of(
                    "state",
                    expectedState("closed", "2", end, "60000001", null),
                    "serverNow",
                    now.toString(),
                    "netMicroseconds",
                    "60000001")));
    verifyNoInteractions(change, receipts);
  }

  @Test
  void s12_completeTokenIdentityReachesApplicationBeforeReplay() throws Exception {
    var other = UUID.randomUUID();
    var expected = new WorkSessionRevision(other, 1);
    when(change.close("owner", id, key, expected, new WorkSessionCloseNotes("", "")))
        .thenThrow(new WorkSessionTransitionException("PRECONDITION_FAILED"));
    problem(
        write("{}")
            .with(
                request -> {
                  request.removeHeader("Work-Session-Revision");
                  request.addHeader("Work-Session-Revision", "work-session-" + other + "-1");
                  return request;
                }),
        412,
        "PRECONDITION_FAILED",
        "La sesión ha cambiado. Consulta su estado actual.");
    verify(change).close("owner", id, key, expected, new WorkSessionCloseNotes("", ""));
    verifyNoInteractions(states, receipts);
  }

  void problem(MockHttpServletRequestBuilder request, int status, String code, String title)
      throws Exception {
    var response =
        mvc.perform(request)
            .andExpect(status().is(status))
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString()))
        .isEqualTo(
            json.valueToTree(
                Map.of(
                    "type",
                    "urn:organization:problem:" + code.toLowerCase(Locale.ROOT),
                    "title",
                    title,
                    "status",
                    status,
                    "code",
                    code)));
  }

  @Test
  void s5_excessUnicodePointsAreRejected() throws Exception {
    mvc.perform(write(json.writeValueAsString(Map.of("nextStep", "\uD83D\uDE00".repeat(2001)))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("nextStep"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(change, states, receipts);
  }

  @Test
  void s5_isolatedSurrogateIsRejectedInNextStep() throws Exception {
    mvc.perform(write("{\"nextStep\":\"\\uD800\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("nextStep"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(change, states, receipts);
  }

  @Test
  void s5_decodedNulIsRejectedBeforeOwnerAndReplay() throws Exception {
    mvc.perform(write("{\"progressNote\":\"\\u0000\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("progressNote"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(change, states, receipts);
  }

  @Test
  void s4_unicodeBoundaryAndWhitespaceArePreserved() throws Exception {
    var text = "\uD83D\uDE00".repeat(2000);
    var next = "  Seguir\nmañana  ";
    var submitted = new WorkSessionCloseNotes(text, next);
    var fact =
        new WorkSessionTransitionReceipt(
            changeId,
            id,
            "CLOSE",
            end,
            before,
            after,
            new WorkSessionClosure(text, next, LocalDate.parse("2026-09-08"), "Europe/Madrid"));
    when(change.close("owner", id, key, new WorkSessionRevision(id, 1), submitted))
        .thenReturn(new WorkSessionTransitionConfirmation(fact, false));
    mvc.perform(write(json.writeValueAsString(Map.of("progressNote", text, "nextStep", next))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.closure.progressNote").value(text))
        .andExpect(jsonPath("$.closure.nextStep").value(next));
    verify(change).close("owner", id, key, new WorkSessionRevision(id, 1), submitted);
  }

  @Test
  void s4_missingAndNullNotesNormalizeBeforeApplication() throws Exception {
    var normalized = new WorkSessionCloseNotes("", "");
    var emptyReceipt =
        new WorkSessionTransitionReceipt(
            changeId,
            id,
            "CLOSE",
            end,
            before,
            after,
            new WorkSessionClosure("", "", LocalDate.parse("2026-09-08"), "Europe/Madrid"));
    when(change.close("owner", id, key, new WorkSessionRevision(id, 1), normalized))
        .thenReturn(new WorkSessionTransitionConfirmation(emptyReceipt, false));
    mvc.perform(write("{\"nextStep\":null}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.closure.progressNote").value(""))
        .andExpect(jsonPath("$.closure.nextStep").value(""));
    verify(change).close("owner", id, key, new WorkSessionRevision(id, 1), normalized);
  }

  @Test
  void s28_changeByKeyReturnsCompleteHistoricalClosureWithoutLocation() throws Exception {
    when(receipts.byRequest("owner", key)).thenReturn(receipt);
    var response =
        mvc.perform(get("/api/v1/work-session-changes/by-request/" + key).with(user("owner")))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist("Location"))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString()))
        .isEqualTo(json.valueToTree(expectedReceipt()));
    verify(receipts).byRequest("owner", key);
    verifyNoInteractions(change, states);
  }

  @Test
  void s6_csrfFailurePrecedesInvalidNotes() throws Exception {
    mvc.perform(
            post("/api/v1/work-sessions/" + id + "/close")
                .with(user("owner"))
                .header("Origin", "https://organization.example")
                .contentType("application/json")
                .content("{\"progressNote\":4}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    verifyNoInteractions(change, states, receipts);
  }

  @Test
  void s6_anonymousRequestDoesNotReachNotes() throws Exception {
    mvc.perform(
            post("/api/v1/work-sessions/" + id + "/close")
                .contentType("application/json")
                .content("{\"progressNote\":4}"))
        .andExpect(status().isUnauthorized());
    verifyNoInteractions(change, states, receipts);
  }

  @Test
  void s6_systemPropertyIsRejected() throws Exception {
    mvc.perform(write("{\"owner\":\"other\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("owner"))
        .andExpect(jsonPath("$.errors[0].code").value("UNKNOWN_FIELD"));
    verifyNoInteractions(change, states, receipts);
  }

  @Test
  void s6_trailingJsonPrecedesNoteValidation() throws Exception {
    mvc.perform(write("{\"progressNote\":4} {}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    verifyNoInteractions(change, states, receipts);
  }

  @Test
  void s6_invalidRevisionSyntaxPrecedesMalformedJson() throws Exception {
    mvc.perform(
            write("{")
                .with(
                    request -> {
                      request.removeHeader("Work-Session-Revision");
                      request.addHeader("Work-Session-Revision", "work-session-" + id + "-1junk");
                      return request;
                    }))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("Work-Session-Revision"));
    verifyNoInteractions(change, states, receipts);
  }

  @Test
  void s6_missingRevisionPrecedesMalformedJson() throws Exception {
    mvc.perform(
            write("{")
                .with(
                    request -> {
                      request.removeHeader("Work-Session-Revision");
                      return request;
                    }))
        .andExpect(status().is(428))
        .andExpect(jsonPath("$.code").value("PRECONDITION_REQUIRED"));
    verifyNoInteractions(change, states, receipts);
  }

  @Test
  void s6_queryPrecedesInvalidPathAndBody() throws Exception {
    mvc.perform(
            write("{")
                .with(
                    request -> {
                      request.setRequestURI("/api/v1/work-sessions/invalid/close");
                      return request;
                    })
                .param("extra", "1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("query"));
    verifyNoInteractions(change, states, receipts);
  }

  @Test
  void s12_replayKeepsHistoricalReceiptAndLocation() throws Exception {
    when(change.close("owner", id, key, new WorkSessionRevision(id, 1), notes))
        .thenReturn(new WorkSessionTransitionConfirmation(receipt, true));
    var response =
        mvc.perform(write("{\"progressNote\":\"Avance\",\"nextStep\":\"Continuar\"}"))
            .andExpect(status().isOk())
            .andExpect(header().string("Location", "/api/v1/work-session-changes/" + changeId))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString()))
        .isEqualTo(json.valueToTree(expectedReceipt()));
    verify(change).close("owner", id, key, new WorkSessionRevision(id, 1), notes);
    verifyNoInteractions(states, receipts);
  }

  @Test
  void s5_numericNoteIsRejectedBeforeApplication() throws Exception {
    mvc.perform(write("{\"progressNote\":4}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("progressNote"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(change, states, receipts);
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @MockitoBean ChangeWorkSessionUseCase change;
  @MockitoBean ExtendWorkSessionUseCase extensions;
  @MockitoBean ReadWorkSessionEndUseCase ends;
  @MockitoBean ReadWorkSessionStateUseCase states;
  @MockitoBean ReadWorkSessionChangesUseCase receipts;
  final UUID id = UUID.randomUUID(), project = UUID.randomUUID(), task = UUID.randomUUID();
  final UUID key = UUID.randomUUID(), changeId = UUID.randomUUID();
  final Instant start = Instant.parse("2026-09-07T22:29:00.123456Z");
  final Instant end = Instant.parse("2026-09-07T22:30:00.123457Z");
  final SessionStart session =
      new SessionStart(id, project, task, start, 25, start.plusSeconds(1500), "Europe/Madrid");
  final WorkSessionState before = new WorkSessionState(session, "running", 1, start, 0, start);
  final WorkSessionState after = new WorkSessionState(session, "closed", 2, end, 60000001, null);
  final WorkSessionCloseNotes notes = new WorkSessionCloseNotes("Avance", "Continuar");
  final WorkSessionTransitionReceipt receipt =
      new WorkSessionTransitionReceipt(
          changeId,
          id,
          "CLOSE",
          end,
          before,
          after,
          new WorkSessionClosure(
              "Avance", "Continuar", LocalDate.parse("2026-09-08"), "Europe/Madrid"));

  @Test
  void s1_closeReturnsExactSevenFieldsAndLocation() throws Exception {
    when(change.close("owner", id, key, new WorkSessionRevision(id, 1), notes))
        .thenReturn(new WorkSessionTransitionConfirmation(receipt, false));
    var response =
        mvc.perform(write("{\"progressNote\":\"Avance\",\"nextStep\":\"Continuar\"}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/work-session-changes/" + changeId))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString()))
        .isEqualTo(json.valueToTree(expectedReceipt()));
    verify(change).close("owner", id, key, new WorkSessionRevision(id, 1), notes);
    verifyNoInteractions(states, receipts);
  }

  Map<String, Object> expectedReceipt() {
    return Map.of(
        "id",
        changeId.toString(),
        "sessionId",
        id.toString(),
        "action",
        "CLOSE",
        "occurredAt",
        end.toString(),
        "before",
        expectedState("running", "1", start, "0", start),
        "after",
        expectedState("closed", "2", end, "60000001", null),
        "closure",
        Map.of(
            "progressNote",
            "Avance",
            "nextStep",
            "Continuar",
            "workDate",
            "2026-09-08",
            "closeZoneId",
            "Europe/Madrid"));
  }

  Map<String, Object> expectedState(
      String status, String revision, Instant changed, String worked, Instant since) {
    var value = new HashMap<String, Object>();
    value.put("session", session);
    value.put("status", status);
    value.put("revision", revision);
    value.put("changedAt", changed.toString());
    value.put("workedMicroseconds", worked);
    value.put("runningSince", since == null ? null : since.toString());
    return value;
  }

  MockHttpServletRequestBuilder write(String body) {
    return post("/api/v1/work-sessions/" + id + "/close")
        .with(user("owner"))
        .with(csrf().asHeader())
        .header("Origin", "https://organization.example")
        .header("Idempotency-Key", key.toString())
        .header("Work-Session-Revision", "work-session-" + id + "-1")
        .contentType("application/json")
        .content(body);
  }
}
