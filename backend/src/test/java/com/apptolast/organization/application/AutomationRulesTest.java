package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.domain.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class AutomationRulesTest {
  static final UUID P = UUID.randomUUID();
  static final Instant T0 = Instant.parse("2026-09-08T10:00:00Z");
  final InMemoryAutomations rules = new InMemoryAutomations();
  final AutomationTargets targets = (owner, project) -> project.equals(P);
  final WebhookEndpointLookup endpoints = (owner, endpoint) -> false;
  final ReadAutomations read = new ReadAutomations(rules);
  final Instant later = T0.plusSeconds(60);
  final ReplaceAutomation replace =
      new ReplaceAutomation(rules, targets, endpoints, Clock.fixed(later, ZoneOffset.UTC));
  final DeleteAutomation delete = new DeleteAutomation(rules);

  static AutomationDraft draft(String name) {
    return new AutomationDraft(
        name, true, "TaskCreated.v1", null, new CreateTaskAction(P, "Revisar", null, null));
  }

  AutomationRule stored(String owner, UUID id, Instant createdAt, long version) {
    return rules.create(owner, new AutomationRule(id, draft("R"), version, createdAt, createdAt));
  }

  @Test
  void s11_listsOnlyOwnRulesOrderedByCreationThenId() {
    var third = stored("a", UUID.fromString("00000000-0000-4000-8000-000000000003"), T0, 1);
    var secondSameInstant =
        stored("a", UUID.fromString("00000000-0000-4000-8000-000000000002"), T0, 1);
    var first = stored("a", UUID.randomUUID(), T0.minusSeconds(1), 1);
    stored("b", UUID.randomUUID(), T0.minusSeconds(2), 1);
    assertThat(read.list("a")).containsExactly(first, secondSameInstant, third);
    assertThat(read.list("c")).isEmpty();
  }

  @Test
  void s11_readingAForeignOrUnknownRuleIsTheSameNotFound() {
    var mine = stored("a", UUID.randomUUID(), T0, 3);
    var foreign = stored("b", UUID.randomUUID(), T0, 1);
    assertThat(read.get("a", mine.id())).isEqualTo(mine);
    assertThatThrownBy(() -> read.get("a", foreign.id()))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> read.get("a", UUID.randomUUID()))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void s12_replacingAlwaysBumpsTheVersionAndKeepsCreatedAt() {
    var rule = stored("a", UUID.randomUUID(), T0, 1);
    var unchanged = replace.replace("a", rule.id(), 1, rule.draft());
    assertThat(unchanged.version()).isEqualTo(2);
    assertThat(unchanged.createdAt()).isEqualTo(T0);
    assertThat(unchanged.updatedAt()).isEqualTo(later);
    var disabled =
        new AutomationDraft(
            "R", false, "TaskCreated.v1", null, new CreateTaskAction(P, "Revisar", null, null));
    assertThat(replace.replace("a", rule.id(), 2, disabled).draft().enabled()).isFalse();
    assertThat(read.get("a", rule.id()).version()).isEqualTo(3);
  }

  @Test
  void s12_s3_replacingValidatesReferencesLikeCreating() {
    var rule = stored("a", UUID.randomUUID(), T0, 1);
    var foreign =
        new AutomationDraft(
            "R",
            true,
            "TaskCreated.v1",
            null,
            new CreateTaskAction(UUID.randomUUID(), "Revisar", null, null));
    assertThatThrownBy(() -> replace.replace("a", rule.id(), 1, foreign))
        .isInstanceOf(AutomationTargetNotFoundException.class);
    assertThat(read.get("a", rule.id()).version()).isEqualTo(1);
  }

  @Test
  void s13_staleOrForeignPreconditionsLeaveTheRuleUntouched() {
    var rule = stored("a", UUID.randomUUID(), T0, 4);
    assertThatThrownBy(() -> replace.replace("a", rule.id(), 3, rule.draft()))
        .isInstanceOf(AutomationConflictException.class);
    assertThatThrownBy(() -> delete.delete("a", rule.id(), 5))
        .isInstanceOf(AutomationConflictException.class);
    var foreign = stored("b", UUID.randomUUID(), T0, 1);
    assertThatThrownBy(() -> delete.delete("a", foreign.id(), 1))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThat(read.get("a", rule.id())).isEqualTo(rule);
    assertThat(read.get("b", foreign.id())).isEqualTo(foreign);
  }

  @Test
  void s9_s14_theQuotaIsTwentyAndDeletingFreesASlot() {
    var create = new CreateAutomation(rules, targets, endpoints, Clock.fixed(T0, ZoneOffset.UTC));
    for (int index = 0; index < AutomationRuleStore.RULE_LIMIT; index++)
      create.create("a", draft("R" + index));
    assertThatThrownBy(() -> create.create("a", draft("Extra")))
        .isInstanceOf(AutomationLimitException.class);
    var victim = read.list("a").getFirst();
    assertThat(replace.replace("a", victim.id(), 1, victim.draft()).version()).isEqualTo(2);
    delete.delete("a", victim.id(), 2);
    assertThat(read.list("a")).hasSize(AutomationRuleStore.RULE_LIMIT - 1);
    create.create("a", draft("Otra"));
    assertThat(read.list("a")).hasSize(AutomationRuleStore.RULE_LIMIT);
    assertThatThrownBy(() -> delete.delete("a", victim.id(), 2))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
