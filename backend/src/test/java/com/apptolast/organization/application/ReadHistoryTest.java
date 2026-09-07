package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.apptolast.organization.domain.SessionStart;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReadHistoryTest {
  @Test
  void s1_returnsTheOriginalSessionStartWithCurrentContextLabels() {
    var project = UUID.randomUUID();
    var task = UUID.randomUUID();
    var id = UUID.randomUUID();
    var at = Instant.parse("2026-09-07T10:00:00.123456Z");
    var start = new SessionStart(id, project, task, at, 25, at.plusSeconds(1500), "UTC");
    var entry =
        new HistoryEntry<>(id, "SESSION_STARTED", at, project, "Entrega", task, "Publicar", start);
    var filters = new HistoryFilters("sessions", project, task, null, null);
    var upper = new HistoryPosition(at.plusSeconds(3600), "SESSION_STARTED", UUID.randomUUID());
    var after = new HistoryPosition(at.plusSeconds(1800), "SESSION_STARTED", UUID.randomUUID());
    var cursor = new HistoryCursor("owner", filters, upper, after);
    var queries = mock(HistoryQueries.class);
    when(queries.list("owner", filters, cursor)).thenReturn(List.of(entry));

    var page = new ReadHistory(queries).list("owner", filters, cursor);

    assertThat(page.items()).containsExactly(entry);
    assertThat(page.items().getFirst().details()).isSameAs(start);
    assertThat(page.next()).isNull();
    verify(queries).list("owner", filters, cursor);
    verifyNoMoreInteractions(queries);
  }

  @Test
  void s6_lookaheadCreatesACursorFromTheLastDeliveredFact() {
    var filters = new HistoryFilters(null, null, null, null, null);
    var at = Instant.parse("2026-09-07T10:00:00Z");
    var project = UUID.randomUUID();
    var task = UUID.randomUUID();
    List<HistoryEntry<?>> rows =
        java.util.stream.IntStream.range(0, 21)
            .<HistoryEntry<?>>mapToObj(
                n -> {
                  var start =
                      new SessionStart(
                          UUID.randomUUID(),
                          project,
                          task,
                          at.minusSeconds(n),
                          25,
                          at.minusSeconds(n).plusSeconds(1500),
                          "UTC");
                  return new HistoryEntry<>(
                      start.id(),
                      "SESSION_STARTED",
                      start.startedAt(),
                      project,
                      "P",
                      task,
                      "T",
                      start);
                })
            .toList();
    var queries = mock(HistoryQueries.class);
    when(queries.list("owner", filters, null)).thenReturn(rows);
    var page = new ReadHistory(queries).list("owner", filters, null);
    assertThat(page.items()).isEqualTo(rows.subList(0, 20));
    assertThat(page.next())
        .isEqualTo(
            new HistoryCursor(
                "owner",
                filters,
                new HistoryPosition(
                    rows.getFirst().occurredAt(), "SESSION_STARTED", rows.getFirst().id()),
                new HistoryPosition(
                    rows.get(19).occurredAt(), "SESSION_STARTED", rows.get(19).id())));
  }

  @Test
  void s14_continuationKeepsTheInitialUpperInsteadOfTheCurrentFirstRow() {
    var filters = new HistoryFilters(null, null, null, null, null);
    var at = Instant.parse("2026-09-07T10:00:00Z");
    var upper = new HistoryPosition(at.plusSeconds(7200), "SESSION_STARTED", UUID.randomUUID());
    var cursor =
        new HistoryCursor(
            "owner",
            filters,
            upper,
            new HistoryPosition(at.plusSeconds(3600), "SESSION_STARTED", UUID.randomUUID()));
    var project = UUID.randomUUID();
    var task = UUID.randomUUID();
    List<HistoryEntry<?>> rows =
        java.util.stream.IntStream.range(0, 21)
            .<HistoryEntry<?>>mapToObj(
                n -> {
                  var start =
                      new SessionStart(
                          UUID.randomUUID(),
                          project,
                          task,
                          at.minusSeconds(n),
                          25,
                          at.minusSeconds(n).plusSeconds(1500),
                          "UTC");
                  return new HistoryEntry<>(
                      start.id(),
                      "SESSION_STARTED",
                      start.startedAt(),
                      project,
                      "P",
                      task,
                      "T",
                      start);
                })
            .toList();
    var queries = mock(HistoryQueries.class);
    when(queries.list("owner", filters, cursor)).thenReturn(rows);
    var page = new ReadHistory(queries).list("owner", filters, cursor);
    assertThat(page.next().upper()).isEqualTo(upper);
    assertThat(page.items()).isEqualTo(rows.subList(0, 20));
    assertThat(page.next().after().id()).isEqualTo(rows.get(19).id());
  }

  @Test
  void s6_exactlyTwentyFactsHaveNoNextCursor() {
    var queries = mock(HistoryQueries.class);
    var filters = new HistoryFilters(null, null, null, null, null);
    var at = Instant.parse("2026-09-07T10:00:00Z");
    List<HistoryEntry<?>> rows =
        java.util.stream.IntStream.range(0, 20)
            .<HistoryEntry<?>>mapToObj(
                n ->
                    new HistoryEntry<>(
                        new UUID(0, n + 1),
                        "TASK_STATUS_CHANGED",
                        at.minusSeconds(n),
                        UUID.randomUUID(),
                        "P",
                        UUID.randomUUID(),
                        "T",
                        "detail"))
            .toList();
    when(queries.list("owner", filters, null)).thenReturn(rows);
    var page = new ReadHistory(queries).list("owner", filters, null);
    assertThat(page.items()).containsExactlyElementsOf(rows);
    assertThat(page.next()).isNull();
  }
}
