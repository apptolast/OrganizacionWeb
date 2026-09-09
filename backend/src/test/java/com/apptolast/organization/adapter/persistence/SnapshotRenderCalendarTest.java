package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.application.CalendarNotFoundException;
import com.apptolast.organization.application.RenderCalendarUseCase;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

/**
 * @s28: resolving the token, the availability zone and the blocks must come out of one snapshot, so
 *     the whole render is wrapped in a single read-only repeatable-read transaction.
 */
class SnapshotRenderCalendarTest {
  private final List<TransactionDefinition> begun = new ArrayList<>();
  private final List<String> outcomes = new ArrayList<>();

  private final PlatformTransactionManager manager =
      new PlatformTransactionManager() {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
          begun.add(definition);
          return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
          outcomes.add("commit");
        }

        @Override
        public void rollback(TransactionStatus status) {
          outcomes.add("rollback");
        }
      };

  static final class Delegate implements RenderCalendarUseCase {
    final List<String> calls = new ArrayList<>();
    RuntimeException failure;

    @Override
    public String forToken(String candidate) {
      calls.add("forToken");
      if (failure != null) throw failure;
      return "BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n";
    }

    @Override
    public String forOwner(String owner) {
      calls.add("forOwner");
      if (failure != null) throw failure;
      return "BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n";
    }
  }

  @Test
  void s28_bothReadsRunInsideOneReadOnlyRepeatableReadTransaction() {
    var delegate = new Delegate();
    var renderer = new SnapshotRenderCalendar(delegate, manager);

    assertThat(renderer.forToken("candidato")).startsWith("BEGIN:VCALENDAR");
    assertThat(renderer.forOwner("persona-a")).startsWith("BEGIN:VCALENDAR");

    assertThat(delegate.calls).containsExactly("forToken", "forOwner");
    assertThat(outcomes).containsExactly("commit", "commit");
    assertThat(begun).hasSize(2);
    assertThat(begun)
        .allSatisfy(
            definition -> {
              assertThat(definition.isReadOnly()).isTrue();
              assertThat(definition.getIsolationLevel())
                  .isEqualTo(TransactionDefinition.ISOLATION_REPEATABLE_READ);
            });
  }

  @Test
  void s15_s30_aFailureRollsBackAndTravelsUnchanged() {
    var delegate = new Delegate();
    delegate.failure = new CalendarNotFoundException();
    var renderer = new SnapshotRenderCalendar(delegate, manager);

    assertThatThrownBy(() -> renderer.forToken("candidato"))
        .isInstanceOf(CalendarNotFoundException.class);
    assertThat(outcomes).containsExactly("rollback");
  }
}
