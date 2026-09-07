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
class EndTimeApiTest {
  @Test
  void s5_extensionQueryPrecedesInvalidIdentifier() throws Exception {
    mvc.perform(
            post("/api/v1/work-sessions/invalid/extend?unexpected=1")
                .with(user("owner"))
                .with(csrf().asHeader())
                .header("Origin", "https://organization.example")
                .contentType("application/json")
                .content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("query"));
    verifyNoInteractions(ends, extend, change, states, receipts);
  }

  @Test
  void s17_extensionStorageFailureDoesNotFabricateConfirmation() throws Exception {
    when(extend.extend("owner", id, key, new WorkSessionRevision(id, 1), 15))
        .thenThrow(new StorageUnavailableException(new IllegalStateException("private SQL")));
    var response =
        mvc.perform(write("{\"additionalMinutes\":15}"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"))
            .andExpect(header().doesNotExist("Location"))
            .andReturn()
            .getResponse();
    assertThat(response.getContentAsString()).doesNotContain("private SQL");
    verifyNoInteractions(ends, change, states, receipts);
  }

  @Test
  void s8_extensionIdempotencyConflictPreservesProblem() throws Exception {
    when(extend.extend("owner", id, key, new WorkSessionRevision(id, 1), 15))
        .thenThrow(new WorkSessionIdempotencyConflictException());
    mvc.perform(write("{\"additionalMinutes\":15}"))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"))
        .andExpect(jsonPath("$.extension").doesNotExist());
    verifyNoInteractions(ends, change, states, receipts);
  }

  @Test
  void s5_extensionRequiresCsrfBeforeApplication() throws Exception {
    mvc.perform(
            post("/api/v1/work-sessions/" + id + "/extend")
                .with(user("owner"))
                .header("Origin", "https://organization.example")
                .contentType("application/json")
                .content("{\"additionalMinutes\":15}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CSRF_INVALID"));
    verifyNoInteractions(ends, extend, change, states, receipts);
  }

  @Test
  void s2_maximumQuantityPreservesPausedStateAndOriginalStart() throws Exception {
    var pausedBefore = new WorkSessionState(session, "paused", 2, now, 60_000_001, null);
    var pausedAfter = new WorkSessionState(session, "paused", 3, now, 60_000_001, null);
    var result =
        new WorkSessionTransitionReceipt(
            receipt.id(),
            id,
            "EXTEND",
            now,
            pausedBefore,
            pausedAfter,
            null,
            new WorkSessionExtension(1440, originalEnd, originalEnd.plusSeconds(86400)));
    when(extend.extend("owner", id, key, new WorkSessionRevision(id, 2), 1440))
        .thenReturn(new WorkSessionTransitionConfirmation(result, false));
    var request =
        write("{\"additionalMinutes\":1440}")
            .with(
                r -> {
                  r.removeHeader("Work-Session-Revision");
                  r.addHeader("Work-Session-Revision", "work-session-" + id + "-2");
                  return r;
                });
    var response =
        mvc.perform(request)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.extension.additionalMinutes").value(1440))
            .andExpect(jsonPath("$.extension.effectiveEndAt").value("1970-01-02T00:24:00.123456Z"))
            .andExpect(jsonPath("$.after.status").value("paused"))
            .andExpect(jsonPath("$.after.workedMicroseconds").value("60000001"))
            .andReturn()
            .getResponse();
    var body = json.readTree(response.getContentAsString());
    assertThat(body.at("/after/runningSince").isNull()).isTrue();
    assertThat(body.at("/after/session")).isEqualTo(json.valueToTree(session));
    verify(extend).extend("owner", id, key, new WorkSessionRevision(id, 2), 1440);
  }

  @Test
  void s1_minimumQuantityReachesApplicationExactly() throws Exception {
    var result =
        new WorkSessionTransitionReceipt(
            receipt.id(),
            id,
            "EXTEND",
            now,
            before,
            after,
            null,
            new WorkSessionExtension(1, originalEnd, originalEnd.plusSeconds(60)));
    when(extend.extend("owner", id, key, new WorkSessionRevision(id, 1), 1))
        .thenReturn(new WorkSessionTransitionConfirmation(result, false));
    mvc.perform(write("{\"additionalMinutes\":1}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.extension.additionalMinutes").value(1))
        .andExpect(jsonPath("$.extension.effectiveEndAt").value("1970-01-01T00:25:00.123456Z"));
    verify(extend).extend("owner", id, key, new WorkSessionRevision(id, 1), 1);
  }

  @Test
  void s16_endStorageFailureDoesNotFabricateSnapshot() throws Exception {
    when(ends.read("owner", id))
        .thenThrow(new StorageUnavailableException(new IllegalStateException("private SQL")));
    var response =
        mvc.perform(get("/api/v1/work-sessions/" + id + "/end-time").with(user("owner")))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"))
            .andExpect(jsonPath("$.state").doesNotExist())
            .andReturn()
            .getResponse();
    assertThat(response.getContentAsString()).doesNotContain("private SQL");
    verifyNoInteractions(extend, change, states, receipts);
  }

  @Test
  void s14_endClockFailureIsSafe409() throws Exception {
    when(ends.read("owner", id))
        .thenThrow(new WorkSessionTransitionException("WORK_SESSION_TIME_OUT_OF_RANGE"));
    mvc.perform(get("/api/v1/work-sessions/" + id + "/end-time").with(user("owner")))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("WORK_SESSION_TIME_OUT_OF_RANGE"))
        .andExpect(jsonPath("$.state").doesNotExist());
    verifyNoInteractions(extend, change, states, receipts);
  }

  @Test
  void s14_endNotOwnedIsSafe404() throws Exception {
    when(ends.read("owner", id)).thenThrow(new WorkSessionNotFoundException());
    mvc.perform(get("/api/v1/work-sessions/" + id + "/end-time").with(user("owner")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("WORK_SESSION_NOT_FOUND"))
        .andExpect(jsonPath("$.state").doesNotExist());
    verify(ends).read("owner", id);
    verifyNoInteractions(extend, change, states, receipts);
  }

  @Test
  void s6_foreignTokenIdentityReachesDecisionIntact() throws Exception {
    var foreign = UUID.randomUUID();
    var expected = new WorkSessionRevision(foreign, 1);
    when(extend.extend("owner", id, key, expected, 15))
        .thenThrow(new WorkSessionTransitionException("PRECONDITION_FAILED"));
    var request =
        write("{\"additionalMinutes\":15}")
            .with(
                r -> {
                  r.removeHeader("Work-Session-Revision");
                  r.addHeader("Work-Session-Revision", "work-session-" + foreign + "-1");
                  return r;
                });
    mvc.perform(request)
        .andExpect(status().isPreconditionFailed())
        .andExpect(jsonPath("$.code").value("PRECONDITION_FAILED"));
    verify(extend).extend("owner", id, key, expected, 15);
    verifyNoInteractions(ends, change, states, receipts);
  }

  @Test
  void s5_clientCannotSupplyEffectiveEnd() throws Exception {
    mvc.perform(write("{\"additionalMinutes\":15,\"effectiveEndAt\":\"1970-01-01T01:00:00Z\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("effectiveEndAt"))
        .andExpect(jsonPath("$.errors[0].code").value("UNKNOWN_FIELD"));
    verifyNoInteractions(ends, extend, change, states, receipts);
  }

  @Test
  void s5_trailingJsonDocumentIsRejected() throws Exception {
    mvc.perform(write("{\"additionalMinutes\":15} {}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    verifyNoInteractions(ends, extend, change, states, receipts);
  }

  @Test
  void s5_malformedRevisionPrecedesMalformedJson() throws Exception {
    var request =
        write("{")
            .with(
                r -> {
                  r.removeHeader("Work-Session-Revision");
                  r.addHeader("Work-Session-Revision", "work-session-" + id + "-01");
                  return r;
                });
    mvc.perform(request)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("Work-Session-Revision"));
    verifyNoInteractions(ends, extend, change, states, receipts);
  }

  @Test
  void s5_missingRevisionPrecedesMalformedJson() throws Exception {
    var request = write("{");
    request.with(
        r -> {
          r.removeHeader("Work-Session-Revision");
          return r;
        });
    mvc.perform(request)
        .andExpect(status().is(428))
        .andExpect(jsonPath("$.code").value("PRECONDITION_REQUIRED"));
    verifyNoInteractions(ends, extend, change, states, receipts);
  }

  @Test
  void s5_anonymousEndRequestPrecedesQueryAndIdValidation() throws Exception {
    mvc.perform(get("/api/v1/work-sessions/invalid/end-time?unexpected=1"))
        .andExpect(status().isUnauthorized());
    verifyNoInteractions(ends, extend, change, states, receipts);
  }

  @Test
  void s23_receiptByIdPreservesExtensionWithoutCurrentRead() throws Exception {
    when(receipts.detail("owner", receipt.id())).thenReturn(receipt);
    var response =
        mvc.perform(get("/api/v1/work-session-changes/" + receipt.id()).with(user("owner")))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist("Location"))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString()))
        .isEqualTo(json.valueToTree(expectedReceipt()));
    verify(receipts).detail("owner", receipt.id());
    verifyNoInteractions(change, states, extend, ends);
  }

  @Test
  void s23_recoveryByKeyPreservesExtensionReceiptWithoutLocation() throws Exception {
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
    verifyNoInteractions(change, states, extend, ends);
  }

  @Test
  void s7_replayReturnsOriginalReceiptAndLocation() throws Exception {
    when(extend.extend("owner", id, key, new WorkSessionRevision(id, 1), 15))
        .thenReturn(new WorkSessionTransitionConfirmation(receipt, true));
    var response =
        mvc.perform(write("{\"additionalMinutes\":15}"))
            .andExpect(status().isOk())
            .andExpect(header().string("Location", "/api/v1/work-session-changes/" + receipt.id()))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString()))
        .isEqualTo(json.valueToTree(expectedReceipt()));
    verify(extend).extend("owner", id, key, new WorkSessionRevision(id, 1), 15);
    verifyNoInteractions(change, states, receipts, ends);
  }

  @Test
  void s5_endQueryPrecedesInvalidIdentifier() throws Exception {
    mvc.perform(get("/api/v1/work-sessions/invalid/end-time?unexpected=1").with(user("owner")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("query"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(ends, extend, change, states, receipts);
  }

  @Test
  void s13_endTimeReturnsExactSnapshotAndRevisionWithoutCommand() throws Exception {
    when(ends.read("owner", id)).thenReturn(new WorkSessionEndSnapshot(after, now, newEnd));
    var response =
        mvc.perform(get("/api/v1/work-sessions/" + id + "/end-time").with(user("owner")))
            .andExpect(status().isOk())
            .andExpect(header().string("Work-Session-Revision", "work-session-" + id + "-2"))
            .andExpect(header().doesNotExist("ETag"))
            .andExpect(header().doesNotExist("Location"))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString()))
        .isEqualTo(
            json.valueToTree(
                Map.of(
                    "state",
                    expectedState("2"),
                    "serverNow",
                    now.toString(),
                    "effectiveEndAt",
                    newEnd.toString())));
    verify(ends).read("owner", id);
    verifyNoInteractions(extend, change, states, receipts);
  }

  @Test
  void s4_largeIntegerCannotWrapToValidMinutes() throws Exception {
    mvc.perform(write("{\"additionalMinutes\":4294967311}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].code").value("OUT_OF_RANGE"));
    verifyNoInteractions(extend, change, states, ends, receipts);
  }

  @Test
  void s4_minutesAboveMaximumCannotReachApplication() throws Exception {
    mvc.perform(write("{\"additionalMinutes\":1441}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].code").value("OUT_OF_RANGE"));
    verifyNoInteractions(extend, change, states, ends, receipts);
  }

  @Test
  void s4_zeroMinutesCannotReachApplication() throws Exception {
    mvc.perform(write("{\"additionalMinutes\":0}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].code").value("OUT_OF_RANGE"));
    verifyNoInteractions(extend, change, states, ends, receipts);
  }

  @Test
  void s4_fractionalMinutesDoesNotTruncate() throws Exception {
    mvc.perform(write("{\"additionalMinutes\":1.5}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].code").value("OUT_OF_RANGE"));
    verifyNoInteractions(extend, change, states, ends, receipts);
  }

  @Test
  void s4_textMinutesDoesNotCoerceToInteger() throws Exception {
    mvc.perform(write("{\"additionalMinutes\":\"15\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("additionalMinutes"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_TYPE"));
    verifyNoInteractions(extend, change, states, ends, receipts);
  }

  @Test
  void s4_missingMinutesIsRequiredBeforeApplication() throws Exception {
    mvc.perform(write("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("additionalMinutes"))
        .andExpect(jsonPath("$.errors[0].code").value("REQUIRED"));
    verifyNoInteractions(extend, change, states, ends, receipts);
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @MockitoBean ChangeWorkSessionUseCase change;
  @MockitoBean ReadWorkSessionStateUseCase states;
  @MockitoBean ReadWorkSessionChangesUseCase receipts;
  @MockitoBean ExtendWorkSessionUseCase extend;
  @MockitoBean ReadWorkSessionEndUseCase ends;

  final UUID id = UUID.randomUUID();
  final UUID key = UUID.randomUUID();
  final Instant start = Instant.parse("1969-12-31T23:59:00.123456Z");
  final Instant now = Instant.parse("1970-01-01T00:00:00.123457Z");
  final Instant originalEnd = Instant.parse("1970-01-01T00:24:00.123456Z");
  final Instant newEnd = Instant.parse("1970-01-01T00:39:00.123456Z");
  final SessionStart session =
      new SessionStart(id, UUID.randomUUID(), UUID.randomUUID(), start, 25, originalEnd, "UTC");
  final WorkSessionState before = new WorkSessionState(session, "running", 1, start, 0, start);
  final WorkSessionState after = new WorkSessionState(session, "running", 2, start, 0, start);
  final WorkSessionTransitionReceipt receipt =
      new WorkSessionTransitionReceipt(
          UUID.randomUUID(),
          id,
          "EXTEND",
          now,
          before,
          after,
          null,
          new WorkSessionExtension(15, originalEnd, newEnd));

  @Test
  void s1_extensionReturnsExactSevenFieldsWithoutChangingWorkState() throws Exception {
    when(extend.extend("owner", id, key, new WorkSessionRevision(id, 1), 15))
        .thenReturn(new WorkSessionTransitionConfirmation(receipt, false));
    var response =
        mvc.perform(write("{\"additionalMinutes\":15}"))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/work-session-changes/" + receipt.id()))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString()))
        .isEqualTo(json.valueToTree(expectedReceipt()));
    verify(extend).extend("owner", id, key, new WorkSessionRevision(id, 1), 15);
    verifyNoInteractions(change, states, receipts, ends);
  }

  MockHttpServletRequestBuilder write(String body) {
    return post("/api/v1/work-sessions/" + id + "/extend")
        .with(user("owner"))
        .with(csrf().asHeader())
        .header("Origin", "https://organization.example")
        .header("Idempotency-Key", key.toString())
        .header("Work-Session-Revision", "work-session-" + id + "-1")
        .contentType("application/json")
        .content(body);
  }

  Map<String, Object> expectedState(String revision) {
    return Map.of(
        "session",
        session,
        "status",
        "running",
        "revision",
        revision,
        "changedAt",
        start.toString(),
        "workedMicroseconds",
        "0",
        "runningSince",
        start.toString());
  }

  Map<String, Object> expectedReceipt() {
    return Map.of(
        "id",
        receipt.id(),
        "sessionId",
        id,
        "action",
        "EXTEND",
        "occurredAt",
        now.toString(),
        "before",
        expectedState("1"),
        "after",
        expectedState("2"),
        "extension",
        Map.of(
            "additionalMinutes",
            15,
            "previousEndAt",
            originalEnd.toString(),
            "effectiveEndAt",
            newEnd.toString()));
  }
}
