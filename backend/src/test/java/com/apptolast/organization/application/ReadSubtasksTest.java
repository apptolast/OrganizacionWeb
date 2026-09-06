package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.domain.*;
import java.time.Instant;
import java.util.*;

class ReadSubtasksTest {
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(ints = {0, 1, 20, 21})
  void s11_s35_keepsScopeAndContinuation(int count) {
    var project = UUID.randomUUID();
    var parent = UUID.randomUUID();
    var after = new TaskPosition(Instant.EPOCH, UUID.randomUUID());
    var rows = new ArrayList<Task>();
    for (int n = 0; n < count; n++)
      rows.add(Task.create(UUID.randomUUID(), project, "T" + n, null, null, Instant.EPOCH));
    SubtaskQueries queries =
        new SubtaskQueries() {
          public List<Task> list(String owner, UUID p, UUID id, TaskPosition position) {
            assertThat(owner).isEqualTo("a");
            assertThat(p).isEqualTo(project);
            assertThat(id).isEqualTo(parent);
            assertThat(position).isSameAs(after);
            return rows;
          }

          public Optional<Task> parent(String owner, UUID p, UUID id) {
            throw new AssertionError("Unexpected parent query");
          }
        };
    var page = new ReadSubtasks(queries).list("a", project, parent, after);
    assertThat(page.items()).containsExactlyElementsOf(rows.subList(0, Math.min(count, 20)));
    if (count == 21)
      assertThat(page.next())
          .isEqualTo(new TaskPosition(rows.get(19).createdAt(), rows.get(19).id()));
    else assertThat(page.next()).isNull();
    rows.clear();
    assertThat(page.items()).hasSize(Math.min(count, 20));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
  void s9_distinguishesConfirmedRootFromParent(boolean hasParent) {
    var project = UUID.randomUUID();
    var child = UUID.randomUUID();
    var parent = Task.create(UUID.randomUUID(), project, "P", null, null, Instant.EPOCH);
    Optional<Task> result = hasParent ? Optional.of(parent) : Optional.empty();
    SubtaskQueries queries =
        new SubtaskQueries() {
          public List<Task> list(String owner, UUID p, UUID id, TaskPosition position) {
            throw new AssertionError("Unexpected collection query");
          }

          public Optional<Task> parent(String owner, UUID p, UUID id) {
            assertThat(owner).isEqualTo("a");
            assertThat(p).isEqualTo(project);
            assertThat(id).isEqualTo(child);
            return result;
          }
        };
    assertThat(new ReadSubtasks(queries).parent("a", project, child)).isEqualTo(result);
  }
}
