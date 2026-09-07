package com.apptolast.organization.adapter;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.apptolast.organization.adapter.config.SecurityConfiguration;
import com.apptolast.organization.adapter.http.HistoryController;
import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = HistoryController.class,
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@Import(SecurityConfiguration.class)
class HistoryApiTest {
  @Autowired MockMvc mvc;
  @MockitoBean ReadHistoryUseCase history;

  private final UUID id = UUID.fromString("11111111-1111-4111-8111-111111111111");
  private final UUID project = UUID.fromString("22222222-2222-4222-8222-222222222222");
  private final UUID task = UUID.fromString("33333333-3333-4333-8333-333333333333");
  private final Instant now = Instant.parse("2026-09-07T10:00:00.123456Z");

  private PlannedBlock block() {
    var start = LocalDateTime.parse("2026-09-08T10:00");
    return new PlannedBlock(
        id,
        project,
        task,
        new BlockRequest(
            "Objetivo original",
            start,
            start.plusMinutes(25),
            "UTC",
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            true),
        new ResolvedBlockTime(
            start.toInstant(ZoneOffset.UTC),
            start.plusMinutes(25).toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC,
            ZoneOffset.UTC,
            25),
        now);
  }

  @Test
  void s1_originalSessionStartKeepsSevenFieldsAndMicrosecondsBeforeEpoch() throws Exception {
    var began = Instant.parse("0001-01-01T00:00:00.123456Z");
    var start = new SessionStart(id, project, task, began, 25, began.plusSeconds(1500), "UTC");
    when(history.list(eq("owner"), any(), isNull()))
        .thenReturn(
            new HistoryPage(
                List.of(
                    new HistoryEntry<>(
                        id, "SESSION_STARTED", began, project, "Entrega", task, "Publicar", start)),
                null));
    mvc.perform(get("/api/v1/history").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].length()").value(8))
        .andExpect(jsonPath("$.items[0].occurredAt").value(began.toString()))
        .andExpect(jsonPath("$.items[0].details.length()").value(7))
        .andExpect(jsonPath("$.items[0].details.startedAt").value(began.toString()))
        .andExpect(jsonPath("$.items[0].details.plannedEndAt").value("0001-01-01T00:25:00.123456Z"))
        .andExpect(jsonPath("$.items[0].details.plannedMinutes").value(25))
        .andExpect(jsonPath("$.items[0].details.status").doesNotExist());
  }

  @Test
  void s1_extensionKeepsHistoricalEndsWithoutInventingNetTime() throws Exception {
    var start =
        new SessionStart(id, project, task, now.minusSeconds(60), 25, now.plusSeconds(1440), "UTC");
    var before = new WorkSessionState(start, "running", 1, start.startedAt(), 0, start.startedAt());
    var after = new WorkSessionState(start, "running", 2, start.startedAt(), 0, start.startedAt());
    var extension =
        new WorkSessionExtension(15, start.plannedEndAt(), start.plannedEndAt().plusSeconds(900));
    var receipt =
        new WorkSessionTransitionReceipt(id, id, "EXTEND", now, before, after, null, extension);
    when(history.list(eq("owner"), any(), isNull()))
        .thenReturn(
            new HistoryPage(
                List.of(
                    new HistoryEntry<>(
                        id, "SESSION_CHANGED", now, project, "Entrega", task, "Publicar", receipt)),
                null));
    mvc.perform(get("/api/v1/history").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].details.length()").value(7))
        .andExpect(jsonPath("$.items[0].details.extension.length()").value(3))
        .andExpect(jsonPath("$.items[0].details.extension.additionalMinutes").value(15))
        .andExpect(
            jsonPath("$.items[0].details.extension.previousEndAt")
                .value(start.plannedEndAt().toString()))
        .andExpect(
            jsonPath("$.items[0].details.extension.effectiveEndAt")
                .value(extension.effectiveEndAt().toString()))
        .andExpect(
            jsonPath("$.items[0].details.after.changedAt").value(start.startedAt().toString()))
        .andExpect(jsonPath("$.items[0].details.after.workedMicroseconds").value("0"))
        .andExpect(jsonPath("$.items[0].details.closure").doesNotExist());
  }

  @Test
  void s1_closeReceiptKeepsHistoricalNotesAndDateWithoutExtension() throws Exception {
    var start =
        new SessionStart(id, project, task, now.minusSeconds(60), 25, now.plusSeconds(1440), "UTC");
    var before = new WorkSessionState(start, "running", 1, start.startedAt(), 0, start.startedAt());
    var after = new WorkSessionState(start, "closed", 2, now, 60000000, null);
    var closure =
        new WorkSessionClosure("  avance\n🙂", " próximo ", LocalDate.of(2026, 9, 7), "UTC");
    var receipt = new WorkSessionTransitionReceipt(id, id, "CLOSE", now, before, after, closure);
    when(history.list(eq("owner"), any(), isNull()))
        .thenReturn(
            new HistoryPage(
                List.of(
                    new HistoryEntry<>(
                        id, "SESSION_CHANGED", now, project, "Entrega", task, "Publicar", receipt)),
                null));
    mvc.perform(get("/api/v1/history").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].details.length()").value(7))
        .andExpect(jsonPath("$.items[0].details.closure.length()").value(4))
        .andExpect(jsonPath("$.items[0].details.closure.progressNote").value("  avance\n🙂"))
        .andExpect(jsonPath("$.items[0].details.closure.nextStep").value(" próximo "))
        .andExpect(jsonPath("$.items[0].details.closure.workDate").value("2026-09-07"))
        .andExpect(jsonPath("$.items[0].details.extension").doesNotExist());
  }

  @Test
  void s12_repeatedCategoryDoesNotSilentlyChooseTheFirstValue() throws Exception {
    mvc.perform(
            get("/api/v1/history").with(user("owner")).param("category", "sessions", "planning"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("category"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(history);
  }

  @Test
  void s12_unknownQueryIsRejectedBeforeUnavailableStorage() throws Exception {
    mvc.perform(get("/api/v1/history?limit=20").with(user("owner")))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("query"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_VALUE"));
    verifyNoInteractions(history);
  }

  @Test
  void s8_filtersNormalizeContextAndPreserveInclusiveExtremeDates() throws Exception {
    var filters =
        new HistoryFilters(
            "sessions", project, task, LocalDate.of(1, 1, 1), LocalDate.of(9999, 12, 31));
    when(history.list("owner", filters, null)).thenReturn(new HistoryPage(List.of(), null));
    mvc.perform(
            get("/api/v1/history")
                .with(user("owner"))
                .param("category", "sessions")
                .param("projectId", project.toString().toUpperCase(java.util.Locale.ROOT))
                .param("taskId", task.toString())
                .param("from", "0001-01-01")
                .param("to", "9999-12-31"))
        .andExpect(status().isOk());
    verify(history).list("owner", filters, null);
    verifyNoMoreInteractions(history);
  }

  @Test
  void s1_pauseReceiptPreservesSixFieldsAndDecimalCounterStrings() throws Exception {
    var start =
        new SessionStart(id, project, task, now.minusSeconds(60), 25, now.plusSeconds(1440), "UTC");
    var before = new WorkSessionState(start, "running", 1, start.startedAt(), 0, start.startedAt());
    var after = new WorkSessionState(start, "paused", 2, now, 60000000, null);
    var receipt = new WorkSessionTransitionReceipt(id, id, "PAUSE", now, before, after);
    when(history.list(eq("owner"), any(), isNull()))
        .thenReturn(
            new HistoryPage(
                List.of(
                    new HistoryEntry<>(
                        id, "SESSION_CHANGED", now, project, "Entrega", task, "Publicar", receipt)),
                null));
    mvc.perform(get("/api/v1/history").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].details.length()").value(6))
        .andExpect(jsonPath("$.items[0].details.after.revision").value("2"))
        .andExpect(jsonPath("$.items[0].details.after.workedMicroseconds").value("60000000"))
        .andExpect(
            jsonPath("$.items[0].details.after.runningSince")
                .value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.items[0].details.closure").doesNotExist())
        .andExpect(jsonPath("$.items[0].details.extension").doesNotExist());
  }

  @Test
  void s1_blockCancellationUsesPublicRevisionAndOriginalBefore() throws Exception {
    var receipt =
        new BlockChangeReceipt(id, id, "CANCELLED", 9007199254740993L, now, block(), null);
    when(history.list(eq("owner"), any(), isNull()))
        .thenReturn(
            new HistoryPage(
                List.of(
                    new HistoryEntry<>(
                        id, "BLOCK_CHANGED", now, project, "Entrega", task, "Publicar", receipt)),
                null));
    mvc.perform(get("/api/v1/history").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].details.length()").value(7))
        .andExpect(
            jsonPath("$.items[0].details.revision").value("\"block:" + id + ":9007199254740993\""))
        .andExpect(jsonPath("$.items[0].details.before.objective").value("Objetivo original"))
        .andExpect(jsonPath("$.items[0].details.after").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.items[0].details.version").doesNotExist());
  }

  @Test
  void s1_originalBlockIsFlattenedWithoutRequestOrBudgetConsent() throws Exception {
    when(history.list(eq("owner"), any(), isNull()))
        .thenReturn(
            new HistoryPage(
                List.of(
                    new HistoryEntry<>(
                        id, "BLOCK_PLANNED", now, project, "Entrega", task, "Publicar", block())),
                null));
    mvc.perform(get("/api/v1/history").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].details.length()").value(9))
        .andExpect(jsonPath("$.items[0].details.objective").value("Objetivo original"))
        .andExpect(jsonPath("$.items[0].details.durationMinutes").value(25))
        .andExpect(jsonPath("$.items[0].details.startAt").value("2026-09-08T10:00:00Z"))
        .andExpect(jsonPath("$.items[0].details.createdAt").value(now.toString()))
        .andExpect(jsonPath("$.items[0].details.request").doesNotExist());
  }

  @Test
  void s1_taskStatusDetailDoesNotExposeInternalTaskVersion() throws Exception {
    var id = java.util.UUID.fromString("11111111-1111-4111-8111-111111111111");
    var project = java.util.UUID.fromString("22222222-2222-4222-8222-222222222222");
    var task = java.util.UUID.fromString("33333333-3333-4333-8333-333333333333");
    var time = java.time.Instant.parse("2026-09-07T10:00:00.123456Z");
    var detail =
        new com.apptolast.organization.domain.TaskHistoryEntry(
            id, 99, "pending", "completed", time);
    when(history.list(eq("owner"), any(), isNull()))
        .thenReturn(
            new HistoryPage(
                List.of(
                    new HistoryEntry<>(
                        id,
                        "TASK_STATUS_CHANGED",
                        time,
                        project,
                        "Entrega",
                        task,
                        "Publicar",
                        detail)),
                null));
    mvc.perform(get("/api/v1/history").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .json(
                    """
            {"items":[{"id":"11111111-1111-4111-8111-111111111111","type":"TASK_STATUS_CHANGED",
            "occurredAt":"2026-09-07T10:00:00.123456Z","projectId":"22222222-2222-4222-8222-222222222222",
            "projectName":"Entrega","taskId":"33333333-3333-4333-8333-333333333333","taskTitle":"Publicar",
            "details":{"id":"11111111-1111-4111-8111-111111111111","fromStatus":"pending","toStatus":"completed",
            "occurredAt":"2026-09-07T10:00:00.123456Z"}}],"nextCursor":null}
            """,
                    true));
  }

  @Test
  void s6_emptyHistoryPreservesClosedPageAndPrivateCachePolicy() throws Exception {
    var filters = new HistoryFilters(null, null, null, null, null);
    when(history.list("owner", filters, null)).thenReturn(new HistoryPage(List.of(), null));
    mvc.perform(get("/api/v1/history").with(user("owner")))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(content().json("{\"items\":[],\"nextCursor\":null}", true))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(header().doesNotExist("ETag"))
        .andExpect(header().doesNotExist("Location"));
    verify(history).list("owner", filters, null);
    verifyNoMoreInteractions(history);
  }
}
