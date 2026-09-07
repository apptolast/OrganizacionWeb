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
}
