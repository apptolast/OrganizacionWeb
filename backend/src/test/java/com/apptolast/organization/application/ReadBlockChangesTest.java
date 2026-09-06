package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.apptolast.organization.domain.BlockChangePosition;
import com.apptolast.organization.domain.BlockChangeReceipt;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ReadBlockChangesTest {
  @Test
  void s16_emptyHistoryKeepsContextAndHasNoContinuation() {
    var project = UUID.randomUUID();
    var task = UUID.randomUUID();
    var queries = mock(BlockChangeQueries.class);
    when(queries.list("owner", project, task, null)).thenReturn(List.of());
    var page = new ReadBlockChanges(queries).list("owner", project, task, null);
    verify(queries).list("owner", project, task, null);
    verifyNoMoreInteractions(queries);
    assertThat(page.items()).isEmpty();
    assertThat(page.next()).isNull();
  }

  @Test
  void s16_exactlyTwentyReceiptsAreTerminalAndKeepTheirOrder() {
    var rows =
        IntStream.range(0, 20)
            .mapToObj(
                n ->
                    new BlockChangeReceipt(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "CANCELLED",
                        n + 2,
                        Instant.parse("2026-09-06T10:00:00.123456Z"),
                        null,
                        null))
            .toList();
    var queries = mock(BlockChangeQueries.class);
    when(queries.list(anyString(), any(), any(), any())).thenReturn(rows);
    var page =
        new ReadBlockChanges(queries).list("owner", UUID.randomUUID(), UUID.randomUUID(), null);
    assertThat(page.items()).containsExactlyElementsOf(rows);
    assertThat(page.next()).isNull();
  }

  @Test
  void s16_twentyOneReceiptsUseTheLastServedPositionForContinuation() {
    var cursor = new BlockChangePosition(Instant.parse("2026-09-07T00:00:00Z"), UUID.randomUUID());
    var rows =
        IntStream.range(0, 21)
            .mapToObj(
                n ->
                    new BlockChangeReceipt(
                        new UUID(0, 100 - n),
                        UUID.randomUUID(),
                        "CANCELLED",
                        99 - n,
                        Instant.parse("2026-09-06T10:00:00.123456Z"),
                        null,
                        null))
            .toList();
    var queries = mock(BlockChangeQueries.class);
    when(queries.list(anyString(), any(), any(), same(cursor))).thenReturn(rows);
    var page =
        new ReadBlockChanges(queries).list("owner", UUID.randomUUID(), UUID.randomUUID(), cursor);
    assertThat(page.items()).containsExactlyElementsOf(rows.subList(0, 20));
    assertThat(page.next())
        .isEqualTo(new BlockChangePosition(rows.get(19).occurredAt(), rows.get(19).id()));
  }

  @Test
  void s18_forwardsReceiptIdAndExactContextWithoutChangingTheReceipt() {
    var project = UUID.randomUUID();
    var task = UUID.randomUUID();
    var id = UUID.randomUUID();
    var receipt = mock(BlockChangeReceipt.class);
    var queries = mock(BlockChangeQueries.class);
    when(queries.detail("owner", project, task, id)).thenReturn(receipt);
    assertThat(new ReadBlockChanges(queries).detail("owner", project, task, id)).isSameAs(receipt);
    verify(queries).detail("owner", project, task, id);
    verifyNoMoreInteractions(queries);
  }

  @Test
  void s25_forwardsOriginalKeyAndContextWithoutReadingCurrentState() {
    var project = UUID.randomUUID();
    var task = UUID.randomUUID();
    var key = UUID.randomUUID();
    var receipt = mock(BlockChangeReceipt.class);
    var queries = mock(BlockChangeQueries.class);
    when(queries.byRequest("owner", project, task, key)).thenReturn(receipt);
    assertThat(new ReadBlockChanges(queries).byRequest("owner", project, task, key))
        .isSameAs(receipt);
    verify(queries).byRequest("owner", project, task, key);
    verifyNoMoreInteractions(queries);
  }
}
