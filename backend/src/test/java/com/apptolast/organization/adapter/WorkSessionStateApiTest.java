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
class WorkSessionStateApiTest {
  @Test
  void s12_jsonNullIsInvalidType() throws Exception {
    validation(write("resume", "null"), "body", "INVALID_TYPE");
  }

  @Test
  void s12_mediaTypePrecedesQuery() throws Exception {
    mvc.perform(write("pause", "{").contentType("text/plain").param("x", "1"))
        .andExpect(status().isUnsupportedMediaType());
    verifyNoInteractions(change, states, receipts);
  }

  @Test
  void s13_missingChangeKeyIsClosedNotFound() throws Exception {
    when(receipts.byRequest("owner", key)).thenThrow(new WorkSessionChangeNotFoundException());
    problem(
        get("/api/v1/work-session-changes/by-request/" + key).with(user("owner")),
        404,
        "WORK_SESSION_CHANGE_NOT_FOUND",
        "No se ha encontrado el cambio de sesión.");
  }

  @Test
  void s12_ownershipFailurePrecedesOtherTokenIdentity() throws Exception {
    var other = UUID.randomUUID();
    when(change.pause("owner", id, key, new WorkSessionRevision(other, 1)))
        .thenThrow(new WorkSessionNotFoundException());
    problem(
        write("pause", "{}")
            .with(
                request -> {
                  request.removeHeader("Work-Session-Revision");
                  request.addHeader("Work-Session-Revision", "work-session-" + other + "-1");
                  return request;
                }),
        404,
        "WORK_SESSION_NOT_FOUND",
        "No se ha encontrado la sesión de trabajo.");
    verify(change).pause("owner", id, key, new WorkSessionRevision(other, 1));
  }

  @Test
  void s10_maximumRevisionReachesBusinessWithoutRounding() throws Exception {
    when(change.pause("owner", id, key, new WorkSessionRevision(id, Long.MAX_VALUE)))
        .thenThrow(new WorkSessionTransitionException("WORK_SESSION_REVISION_EXHAUSTED"));
    problem(
        write("pause", "{}")
            .with(
                request -> {
                  request.removeHeader("Work-Session-Revision");
                  request.addHeader(
                      "Work-Session-Revision", "work-session-" + id + "-9223372036854775807");
                  return request;
                }),
        409,
        "WORK_SESSION_REVISION_EXHAUSTED",
        "La sesión no admite más revisiones.");
    verify(change).pause("owner", id, key, new WorkSessionRevision(id, Long.MAX_VALUE));
  }

  @Test
  void s16_replayReturnsOriginalReceiptAndLocation() throws Exception {
    when(change.pause("owner", id, key, new WorkSessionRevision(id, 1)))
        .thenReturn(new WorkSessionTransitionConfirmation(receipt, true));
    var response =
        mvc.perform(write("pause", "{}"))
            .andExpect(status().isOk())
            .andExpect(header().string("Location", "/api/v1/work-session-changes/" + changeId))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString()))
        .isEqualTo(json.valueToTree(expectedReceipt()));
    verify(change).pause("owner", id, key, new WorkSessionRevision(id, 1));
    verifyNoMoreInteractions(change);
    verifyNoInteractions(states, receipts);
  }

  @Test
  void s21_commandStorageFailureIsSafe503() throws Exception {
    when(change.pause("owner", id, key, new WorkSessionRevision(id, 1)))
        .thenThrow(new StorageUnavailableException(new IllegalStateException("private SQL")));
    problem(
        write("pause", "{}"),
        503,
        "STORAGE_UNAVAILABLE",
        "El almacenamiento no está disponible. Inténtalo más tarde.");
  }

  @Test
  void s24_keyLookupFailureIsNotMissing() throws Exception {
    when(receipts.byRequest("owner", key))
        .thenThrow(new StorageUnavailableException(new IllegalStateException("private SQL")));
    problem(
        get("/api/v1/work-session-changes/by-request/" + key).with(user("owner")),
        503,
        "STORAGE_UNAVAILABLE",
        "El almacenamiento no está disponible. Inténtalo más tarde.");
  }

  @Test
  void s24_receiptFailureIsNotMissing() throws Exception {
    when(receipts.detail("owner", changeId))
        .thenThrow(new StorageUnavailableException(new IllegalStateException("private SQL")));
    problem(
        get("/api/v1/work-session-changes/" + changeId).with(user("owner")),
        503,
        "STORAGE_UNAVAILABLE",
        "El almacenamiento no está disponible. Inténtalo más tarde.");
  }

  @Test
  void s24_stateFailureIsNotAbsence() throws Exception {
    when(states.read("owner", id))
        .thenThrow(new StorageUnavailableException(new IllegalStateException("private SQL")));
    problem(
        get("/api/v1/work-sessions/" + id + "/state").with(user("owner")),
        503,
        "STORAGE_UNAVAILABLE",
        "El almacenamiento no está disponible. Inténtalo más tarde.");
  }

  @Test
  void s12_pauseRequiresTrustedOrigin() throws Exception {
    mvc.perform(
            write("pause", "{")
                .with(
                    request -> {
                      request.removeHeader("Origin");
                      request.addHeader("Origin", "https://foreign.example");
                      return request;
                    }))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("UNTRUSTED_ORIGIN"));
    verifyNoInteractions(change, states, receipts);
  }

  @Test
  void s12_resumeRequiresCsrfBeforeValidation() throws Exception {
    mvc.perform(
            post("/api/v1/work-sessions/" + id + "/resume")
                .with(user("owner"))
                .header("Origin", "https://organization.example")
                .contentType("application/json")
                .content("{"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    verifyNoInteractions(change, states, receipts);
  }

  @Test
  void s12_anonymousStateCannotRevealData() throws Exception {
    mvc.perform(get("/api/v1/work-sessions/" + id + "/state").param("x", "1"))
        .andExpect(status().isUnauthorized());
    verifyNoInteractions(change, states, receipts);
  }

  @Test
  void s11_resumeQueriesPrecedeMissingHeaders() throws Exception {
    validation(
        write("resume", "{}")
            .param("x", "1", "2")
            .with(
                request -> {
                  request.removeHeader("Idempotency-Key");
                  request.removeHeader("Work-Session-Revision");
                  return request;
                }),
        "query",
        "INVALID_VALUE");
  }

  @Test
  void s10_repeatedRevisionHeaderIsInvalid() throws Exception {
    validation(
        write("pause", "{}").header("Work-Session-Revision", "work-session-" + id + "-1"),
        "Work-Session-Revision",
        "INVALID_VALUE");
  }

  @Test
  void s10_listOfRevisionTokensIsInvalid() throws Exception {
    validation(
        write("pause", "{}")
            .with(
                request -> {
                  request.removeHeader("Work-Session-Revision");
                  request.addHeader(
                      "Work-Session-Revision",
                      "work-session-" + id + "-1,work-session-" + id + "-1");
                  return request;
                }),
        "Work-Session-Revision",
        "INVALID_VALUE");
  }

  @Test
  void s10_spaceInRevisionIsInvalid() throws Exception {
    validation(
        write("pause", "{}")
            .with(
                request -> {
                  request.removeHeader("Work-Session-Revision");
                  request.addHeader("Work-Session-Revision", "work-session-" + id + "- 1");
                  return request;
                }),
        "Work-Session-Revision",
        "INVALID_VALUE");
  }

  @Test
  void s10_trailingGarbageRevisionIsInvalid() throws Exception {
    validation(
        write("pause", "{}")
            .with(
                request -> {
                  request.removeHeader("Work-Session-Revision");
                  request.addHeader("Work-Session-Revision", "work-session-" + id + "-1garbage");
                  return request;
                }),
        "Work-Session-Revision",
        "INVALID_VALUE");
  }

  @Test
  void s10_leadingZeroRevisionIsInvalid() throws Exception {
    validation(
        write("pause", "{}")
            .with(
                request -> {
                  request.removeHeader("Work-Session-Revision");
                  request.addHeader("Work-Session-Revision", "work-session-" + id + "-01");
                  return request;
                }),
        "Work-Session-Revision",
        "INVALID_VALUE");
  }

  @Test
  void s10_signedRevisionIsInvalid() throws Exception {
    validation(
        write("pause", "{}")
            .with(
                request -> {
                  request.removeHeader("Work-Session-Revision");
                  request.addHeader("Work-Session-Revision", "work-session-" + id + "-+1");
                  return request;
                }),
        "Work-Session-Revision",
        "INVALID_VALUE");
  }

  @Test
  void s10_zeroRevisionIsInvalid() throws Exception {
    validation(
        write("pause", "{}")
            .with(
                request -> {
                  request.removeHeader("Work-Session-Revision");
                  request.addHeader("Work-Session-Revision", "work-session-" + id + "-0");
                  return request;
                }),
        "Work-Session-Revision",
        "INVALID_VALUE");
  }

  @Test
  void s17_reusedKeyHasChangeSpecificConflict() throws Exception {
    when(change.pause("owner", id, key, new WorkSessionRevision(id, 1)))
        .thenThrow(new WorkSessionIdempotencyConflictException());
    problem(
        write("pause", "{}"),
        409,
        "IDEMPOTENCY_CONFLICT",
        "La clave corresponde a otra intención de cambio de sesión.");
  }

  @Test
  void s23_readClockOutOfRangeHasTransitionTitle() throws Exception {
    when(states.read("owner", id))
        .thenThrow(new WorkSessionTransitionException("WORK_SESSION_TIME_OUT_OF_RANGE"));
    problem(
        get("/api/v1/work-sessions/" + id + "/state").with(user("owner")),
        409,
        "WORK_SESSION_TIME_OUT_OF_RANGE",
        "No se puede registrar la transición en ese instante.");
  }

  @Test
  void s15_exhaustedRevisionHasClosedConflict() throws Exception {
    when(change.pause("owner", id, key, new WorkSessionRevision(id, 1)))
        .thenThrow(new WorkSessionTransitionException("WORK_SESSION_REVISION_EXHAUSTED"));
    problem(
        write("pause", "{}"),
        409,
        "WORK_SESSION_REVISION_EXHAUSTED",
        "La sesión no admite más revisiones.");
  }

  @Test
  void s14_incompatibleStateHasClosedConflict() throws Exception {
    when(change.pause("owner", id, key, new WorkSessionRevision(id, 1)))
        .thenThrow(new WorkSessionTransitionException("WORK_SESSION_STATE_CONFLICT"));
    problem(
        write("pause", "{}"),
        409,
        "WORK_SESSION_STATE_CONFLICT",
        "El estado de la sesión no permite esta acción.");
  }

  @Test
  void s12_otherTokenIdentityReachesOwnershipBefore412() throws Exception {
    var other = UUID.randomUUID();
    when(change.pause("owner", id, key, new WorkSessionRevision(other, 1)))
        .thenThrow(new WorkSessionTransitionException("PRECONDITION_FAILED"));
    problem(
        write("pause", "{}")
            .with(
                request -> {
                  request.removeHeader("Work-Session-Revision");
                  request.addHeader("Work-Session-Revision", "work-session-" + other + "-1");
                  return request;
                }),
        412,
        "PRECONDITION_FAILED",
        "La sesión ha cambiado. Consulta su estado actual.");
    verify(change).pause("owner", id, key, new WorkSessionRevision(other, 1));
  }

  @Test
  void s13_missingChangeIsClosedNotFound() throws Exception {
    when(receipts.detail("owner", changeId)).thenThrow(new WorkSessionChangeNotFoundException());
    problem(
        get("/api/v1/work-session-changes/" + changeId).with(user("owner")),
        404,
        "WORK_SESSION_CHANGE_NOT_FOUND",
        "No se ha encontrado el cambio de sesión.");
  }

  @Test
  void s13_missingSessionIsClosedNotFound() throws Exception {
    when(states.read("owner", id)).thenThrow(new WorkSessionNotFoundException());
    problem(
        get("/api/v1/work-sessions/" + id + "/state").with(user("owner")),
        404,
        "WORK_SESSION_NOT_FOUND",
        "No se ha encontrado la sesión de trabajo.");
  }

  @Test
  void s12_duplicateJsonIsMalformedBeforeExtraField() throws Exception {
    problem(
        write("pause", "{\"x\":1,\"x\":2}"),
        400,
        "MALFORMED_JSON",
        "No se puede leer el JSON enviado.");
    verifyNoInteractions(change, states, receipts);
  }

  @Test
  void s12_missingBodyIsMalformedJson() throws Exception {
    problem(write("pause", ""), 400, "MALFORMED_JSON", "No se puede leer el JSON enviado.");
    verifyNoInteractions(change, states, receipts);
  }

  @Test
  void s12_arrayBodyIsInvalidType() throws Exception {
    validation(write("pause", "[]"), "body", "INVALID_TYPE");
  }

  @Test
  void s12_extraFieldUsesLexicalFirst() throws Exception {
    validation(write("pause", "{\"z\":1,\"a\":2}"), "a", "UNKNOWN_FIELD");
  }

  @Test
  void s12_concatenatedJsonDoesNotReachBusiness() throws Exception {
    problem(write("pause", "{} {}"), 400, "MALFORMED_JSON", "No se puede leer el JSON enviado.");
    verifyNoInteractions(change, states, receipts);
  }

  @Test
  void s12_repeatedKeyIsInvalidValue() throws Exception {
    validation(
        write("pause", "{}").header("Idempotency-Key", key.toString()),
        "Idempotency-Key",
        "INVALID_VALUE");
  }

  @Test
  void s12_missingKeyHasItsRequiredField() throws Exception {
    validation(
        write("pause", "{}")
            .with(
                request -> {
                  request.removeHeader("Idempotency-Key");
                  return request;
                }),
        "Idempotency-Key",
        "REQUIRED");
  }

  @Test
  void s10_revisionOverflowIsValidation() throws Exception {
    validation(
        write("pause", "{}")
            .with(
                request -> {
                  request.removeHeader("Work-Session-Revision");
                  request.addHeader(
                      "Work-Session-Revision", "work-session-" + id + "-9223372036854775808");
                  return request;
                }),
        "Work-Session-Revision",
        "INVALID_VALUE");
  }

  @Test
  void s10_quotedRevisionIsInvalid() throws Exception {
    validation(
        write("pause", "{}")
            .with(
                request -> {
                  request.removeHeader("Work-Session-Revision");
                  request.addHeader("Work-Session-Revision", "\"work-session-" + id + "-1\"");
                  return request;
                }),
        "Work-Session-Revision",
        "INVALID_VALUE");
  }

  @Test
  void s12_invalidKeyPrecedesMissingRevision() throws Exception {
    validation(
        write("pause", "{}")
            .with(
                request -> {
                  request.removeHeader("Idempotency-Key");
                  request.addHeader("Idempotency-Key", "bad");
                  request.removeHeader("Work-Session-Revision");
                  return request;
                }),
        "Idempotency-Key",
        "INVALID_FORMAT");
  }

  @Test
  void s12_missingRevisionPrecedesMalformedBody() throws Exception {
    problem(
        write("pause", "{} {}")
            .with(
                request -> {
                  request.removeHeader("Work-Session-Revision");
                  return request;
                }),
        428,
        "PRECONDITION_REQUIRED",
        "Envía la revisión actual requerida.");
    verifyNoInteractions(change, states, receipts);
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
  void s11_byRequestQueryPrecedesInvalidIdentity() throws Exception {
    validation(
        get("/api/v1/work-session-changes/by-request/bad").with(user("owner")).param("x", "1", "2"),
        "query",
        "INVALID_VALUE");
  }

  @Test
  void s11_receiptQueryPrecedesInvalidIdentity() throws Exception {
    validation(
        get("/api/v1/work-session-changes/bad").with(user("owner")).param("x", "1", "2"),
        "query",
        "INVALID_VALUE");
  }

  @Test
  void s11_stateQueryPrecedesInvalidIdentity() throws Exception {
    validation(
        get("/api/v1/work-sessions/bad/state").with(user("owner")).param("x", "1", "2"),
        "query",
        "INVALID_VALUE");
  }

  @Test
  void s11_pauseRejectsQueriesBeforeBusiness() throws Exception {
    validation(write("pause", "{}").param("x", "1", "2"), "query", "INVALID_VALUE");
  }

  void validation(MockHttpServletRequestBuilder request, String field, String code)
      throws Exception {
    mvc.perform(request)
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors.length()").value(1))
        .andExpect(jsonPath("$.errors[0].field").value(field))
        .andExpect(jsonPath("$.errors[0].code").value(code));
    verifyNoInteractions(change, states, receipts);
  }

  @Test
  void s25_receiptByKeyDoesNotConsultCurrentState() throws Exception {
    when(receipts.byRequest("owner", key)).thenReturn(receipt);
    var response =
        mvc.perform(get("/api/v1/work-session-changes/by-request/" + key).with(user("owner")))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist("Location"))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString()))
        .isEqualTo(json.valueToTree(expectedReceipt()));
    verify(receipts).byRequest("owner", key);
    verifyNoInteractions(change, states);
  }

  @Test
  void s25_receiptByIdDoesNotConsultCurrentState() throws Exception {
    when(receipts.detail("owner", changeId)).thenReturn(receipt);
    var response =
        mvc.perform(get("/api/v1/work-session-changes/" + changeId).with(user("owner")))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist("Location"))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString()))
        .isEqualTo(json.valueToTree(expectedReceipt()));
    verify(receipts).detail("owner", changeId);
    verifyNoInteractions(change, states);
  }

  @Test
  void s1_stateSnapshotHasItsOwnRevisionHeaderAndExactStrings() throws Exception {
    when(states.read("owner", id)).thenReturn(new WorkSessionSnapshot(before, when, 1000001));
    var response =
        mvc.perform(get("/api/v1/work-sessions/" + id + "/state").with(user("owner")))
            .andExpect(status().isOk())
            .andExpect(header().string("Work-Session-Revision", "work-session-" + id + "-1"))
            .andExpect(header().doesNotExist("ETag"))
            .andExpect(header().doesNotExist("Location"))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString()))
        .isEqualTo(
            json.valueToTree(
                Map.of(
                    "state",
                    expectedState("running", "1", start, "0", start),
                    "serverNow",
                    when.toString(),
                    "netMicroseconds",
                    "1000001")));
    verify(states).read("owner", id);
    verifyNoInteractions(change, receipts);
  }

  @Test
  void s3_resumeUsesItsOwnActionAndReceipt() throws Exception {
    var resumed =
        new WorkSessionState(
            session, "running", 3, when.plusSeconds(3600), 1000001, when.plusSeconds(3600));
    var result =
        new WorkSessionTransitionReceipt(
            changeId, id, "RESUME", resumed.changedAt(), after, resumed);
    when(change.resume("owner", id, key, new WorkSessionRevision(id, 2)))
        .thenReturn(new WorkSessionTransitionConfirmation(result, false));
    var response =
        mvc.perform(
                write("resume", "{}")
                    .with(
                        request -> {
                          request.removeHeader("Work-Session-Revision");
                          request.addHeader("Work-Session-Revision", "work-session-" + id + "-2");
                          return request;
                        }))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/work-session-changes/" + changeId))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString()))
        .isEqualTo(
            json.valueToTree(
                Map.of(
                    "id",
                    changeId.toString(),
                    "sessionId",
                    id.toString(),
                    "action",
                    "RESUME",
                    "occurredAt",
                    resumed.changedAt().toString(),
                    "before",
                    expectedState("paused", "2", when, "1000001", null),
                    "after",
                    expectedState(
                        "running", "3", resumed.changedAt(), "1000001", resumed.changedAt()))));
    verify(change).resume("owner", id, key, new WorkSessionRevision(id, 2));
    verifyNoMoreInteractions(change);
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @MockitoBean ChangeWorkSessionUseCase change;
  @MockitoBean ReadWorkSessionStateUseCase states;
  @MockitoBean ReadWorkSessionChangesUseCase receipts;
  final UUID id = UUID.randomUUID(), project = UUID.randomUUID(), task = UUID.randomUUID();
  final UUID key = UUID.randomUUID(), changeId = UUID.randomUUID();
  final Instant start = Instant.parse("2026-09-07T10:00:00.123456Z");
  final Instant when = Instant.parse("2026-09-07T10:00:01.123457Z");
  final SessionStart session =
      new SessionStart(id, project, task, start, 25, start.plusSeconds(1500), "Historical/Removed");
  final WorkSessionState before = new WorkSessionState(session, "running", 1, start, 0, start);
  final WorkSessionState after = new WorkSessionState(session, "paused", 2, when, 1000001, null);
  final WorkSessionTransitionReceipt receipt =
      new WorkSessionTransitionReceipt(changeId, id, "PAUSE", when, before, after);

  @Test
  void s2_pauseReturnsClosedReceiptWithDecimalStrings() throws Exception {
    when(change.pause("owner", id, key, new WorkSessionRevision(id, 1)))
        .thenReturn(new WorkSessionTransitionConfirmation(receipt, false));
    var response =
        mvc.perform(write("pause", "{}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/work-session-changes/" + changeId))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString()))
        .isEqualTo(json.valueToTree(expectedReceipt()));
    verify(change).pause("owner", id, key, new WorkSessionRevision(id, 1));
    verifyNoInteractions(states, receipts);
  }

  Map<String, Object> expectedReceipt() {
    return Map.of(
        "id",
        changeId.toString(),
        "sessionId",
        id.toString(),
        "action",
        "PAUSE",
        "occurredAt",
        when.toString(),
        "before",
        expectedState("running", "1", start, "0", start),
        "after",
        expectedState("paused", "2", when, "1000001", null));
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

  MockHttpServletRequestBuilder write(String action, String body) {
    return post("/api/v1/work-sessions/" + id + "/" + action)
        .with(user("owner"))
        .with(csrf().asHeader())
        .header("Origin", "https://organization.example")
        .header("Idempotency-Key", key.toString())
        .header("Work-Session-Revision", "work-session-" + id + "-1")
        .contentType("application/json")
        .content(body);
  }
}
