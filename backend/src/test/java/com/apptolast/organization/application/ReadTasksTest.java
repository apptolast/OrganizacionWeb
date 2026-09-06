package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.domain.*;
import java.time.Instant;
import java.util.*;

class ReadTasksTest {
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(ints = {0, 1, 20, 21})
  void s19_s20_keepsTwentyAndSignalsOnlyActualContinuation(int count) {
    var project = UUID.randomUUID();
    var position = new TaskPosition(Instant.EPOCH, UUID.randomUUID());
    var rows = new ArrayList<Task>();
    for (int n = 0; n < count; n++)
      rows.add(Task.create(UUID.randomUUID(), project, "T" + n, null, null, Instant.EPOCH));
    TaskQueries queries =
        new TaskQueries() {
          public List<Task> list(String owner, UUID id, TaskPosition after) {
            assertThat(owner).isEqualTo("a");
            assertThat(id).isEqualTo(project);
            assertThat(after).isEqualTo(position);
            return rows;
          }

          public Task detail(String owner, UUID projectId, UUID id) {
            throw new AssertionError("Unexpected detail lookup");
          }
        };
    var page = new ReadTasks(queries).list("a", project, position);
    assertThat(page.items()).containsExactlyElementsOf(rows.subList(0, Math.min(20, count)));
    if (count > 20)
      assertThat(page.next())
          .isEqualTo(new TaskPosition(rows.get(19).createdAt(), rows.get(19).id()));
    else assertThat(page.next()).isNull();
    rows.clear();
    assertThat(page.items()).hasSize(Math.min(20, count));
  }

  @org.junit.jupiter.api.Test
  void s23_detailPreservesScopeAndConfirmedValues() {
    var project = UUID.randomUUID();
    var task = Task.create(UUID.randomUUID(), project, "T", null, null, Instant.EPOCH);
    TaskQueries queries =
        new TaskQueries() {
          public List<Task> list(String owner, UUID id, TaskPosition after) {
            throw new AssertionError("Unexpected list lookup");
          }

          public Task detail(String owner, UUID projectId, UUID id) {
            assertThat(owner).isEqualTo("a");
            assertThat(projectId).isEqualTo(project);
            assertThat(id).isEqualTo(task.id());
            return task;
          }
        };
    assertThat(new ReadTasks(queries).detail("a", project, task.id())).isEqualTo(task);
  }
}
