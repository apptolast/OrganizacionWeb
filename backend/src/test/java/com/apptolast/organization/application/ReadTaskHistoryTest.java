package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.domain.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReadTaskHistoryTest {
  @ParameterizedTest
  @ValueSource(ints = {0, 1, 20, 21})
  void s10_s29_preservesScopeAndVersionContinuation(int count) {
    var project = UUID.randomUUID();
    var task = UUID.randomUUID();
    var after = new TaskHistoryPosition(100);
    var rows = new ArrayList<TaskHistoryEntry>();
    for (int n = 0; n < count; n++)
      rows.add(
          new TaskHistoryEntry(
              UUID.randomUUID(), 99 - n * 2, "pending", "completed", Instant.EPOCH));
    TaskHistoryQueries queries =
        (owner, p, t, cursor) -> {
          assertThat(owner).isEqualTo("owner");
          assertThat(p).isEqualTo(project);
          assertThat(t).isEqualTo(task);
          assertThat(cursor).isSameAs(after);
          return rows;
        };
    var page = new ReadTaskHistory(queries).list("owner", project, task, after);
    assertThat(page.items()).containsExactlyElementsOf(rows.subList(0, Math.min(count, 20)));
    if (count == 21) assertThat(page.next()).isEqualTo(new TaskHistoryPosition(61));
    else assertThat(page.next()).isNull();
    rows.clear();
    assertThat(page.items()).hasSize(Math.min(count, 20));
  }
}
