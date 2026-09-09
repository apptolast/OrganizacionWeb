package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.domain.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class ReadAutomationRunsTest {
  private static final UUID P = UUID.randomUUID();
  private static final Instant T0 = Instant.parse("2026-09-08T10:00:00Z");

  private final InMemoryAutomations rules = new InMemoryAutomations();
  private final InMemoryAutomationRuns runs = new InMemoryAutomationRuns();
  private final ReadAutomationRuns read = new ReadAutomationRuns(rules, runs);

  private AutomationRule rule(String owner) {
    var draft =
        new AutomationDraft(
            "R", true, "TaskCreated.v1", null, new CreateTaskAction(P, "Revisar", null, null));
    return rules.create(owner, new AutomationRule(UUID.randomUUID(), draft, 1, T0, T0));
  }

  private AutomationRun run(String owner, UUID ruleId, int index, String status) {
    return runs.add(
        new AutomationRun(
            UUID.randomUUID(),
            ruleId,
            owner,
            UUID.randomUUID(),
            "TaskCreated.v1",
            T0.plusSeconds(index),
            1,
            status,
            status.equals("succeeded") ? UUID.randomUUID() : null,
            null,
            status.equals("failed") ? "PROJECT_COMPLETED" : null,
            T0.plusSeconds(index)));
  }

  @Test
  void s34_pagesTwentyAtATimeNewestFirstAndClosesWithANullCursor() {
    var rule = rule("a");
    var statuses = List.of("succeeded", "retry", "failed");
    for (int index = 0; index < 25; index++) run("a", rule.id(), index, statuses.get(index % 3));

    var first = read.read("a", rule.id(), null);
    assertThat(first.items()).hasSize(ReadAutomationRuns.PAGE_SIZE);
    assertThat(first.items())
        .extracting(AutomationRun::executedAt)
        .isSortedAccordingTo(Comparator.reverseOrder());
    assertThat(first.items().getFirst().executedAt()).isEqualTo(T0.plusSeconds(24));
    assertThat(first.nextCursor()).isNotNull();

    var second = read.read("a", rule.id(), first.nextCursor());
    assertThat(second.items()).hasSize(5);
    assertThat(second.nextCursor()).isNull();
    assertThat(second.items())
        .extracting(AutomationRun::id)
        .doesNotContainAnyElementsOf(first.items().stream().map(AutomationRun::id).toList());
  }

  @Test
  void s34_anExhaustedPageEndsWithoutACursor() {
    var rule = rule("a");
    for (int index = 0; index < ReadAutomationRuns.PAGE_SIZE; index++)
      run("a", rule.id(), index, "succeeded");
    var only = read.read("a", rule.id(), null);
    assertThat(only.items()).hasSize(ReadAutomationRuns.PAGE_SIZE);
    assertThat(only.nextCursor()).isNull();
  }

  @Test
  void s35_showsOnlyTheRunsOfTheAskedRule() {
    var first = rule("a");
    var second = rule("a");
    var foreign = rule("b");
    run("a", first.id(), 1, "succeeded");
    run("a", second.id(), 2, "succeeded");
    run("b", foreign.id(), 3, "succeeded");
    assertThat(read.read("a", first.id(), null).items())
        .extracting(AutomationRun::ruleId)
        .containsExactly(first.id());
  }

  @Test
  void s35_aCursorOfAnotherRuleIsRejectedAsAnInvalidValue() {
    var first = rule("a");
    var second = rule("a");
    run("a", second.id(), 1, "succeeded");
    var foreignCursor = new AutomationRunCursor(second.id(), T0, UUID.randomUUID());
    assertThatThrownBy(() -> read.read("a", first.id(), foreignCursor))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error ->
                assertThat(error.errors()).extracting(FieldError::field).containsExactly("cursor"));
  }

  @Test
  void s35_aForeignOrUnknownRuleIsTheSameNotFound() {
    rule("a");
    var foreign = rule("b");
    assertThatThrownBy(() -> read.read("a", foreign.id(), null))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> read.read("a", UUID.randomUUID(), null))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
