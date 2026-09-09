package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.domain.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CreateAutomationTest {
  static final UUID P = UUID.randomUUID();
  static final UUID Q = UUID.randomUUID();
  static final Instant NOW = Instant.parse("2026-09-08T10:15:30.123456789Z");
  final InMemoryAutomations rules = new InMemoryAutomations();
  final AutomationTargets targets = (owner, project) -> owner.equals("a") && project.equals(P);
  final WebhookEndpointLookup endpoints = (owner, endpoint) -> false;
  final CreateAutomation create =
      new CreateAutomation(rules, targets, endpoints, Clock.fixed(NOW, ZoneOffset.UTC));

  static AutomationDraft draft(UUID condition, AutomationAction action) {
    return new AutomationDraft("  Seguimiento  ", true, "TaskCreated.v1", condition, action);
  }

  @Test
  void s1_storesVersionOneWithServerIdAndMicrosecondTimestamps() {
    var rule =
        create.create(
            "a", draft(null, new CreateTaskAction(P, "Revisar {{task.title}}", null, 30)));
    assertThat(rule.id()).isNotNull();
    assertThat(rule.version()).isEqualTo(1);
    assertThat(rule.createdAt()).isEqualTo(Instant.parse("2026-09-08T10:15:30.123456Z"));
    assertThat(rule.updatedAt()).isEqualTo(rule.createdAt());
    assertThat(rule.draft().name()).isEqualTo("Seguimiento");
    assertThat(rules.list("a")).containsExactly(rule);
  }

  @Test
  void s3_s4_rejectsForeignOrMissingProjectsWithoutWriting() {
    assertThatThrownBy(() -> create.create("a", draft(Q, new CreateTaskAction(P, "x", null, null))))
        .isInstanceOfSatisfying(
            AutomationTargetNotFoundException.class,
            error -> assertThat(error.field()).isEqualTo("condition.projectId"));
    assertThatThrownBy(() -> create.create("a", draft(P, new CreateTaskAction(Q, "x", null, null))))
        .isInstanceOfSatisfying(
            AutomationTargetNotFoundException.class,
            error -> assertThat(error.field()).isEqualTo("action.projectId"));
    assertThatThrownBy(
            () -> create.create("b", draft(null, new CreateTaskAction(P, "x", null, null))))
        .isInstanceOf(AutomationTargetNotFoundException.class);
    assertThat(rules.list("a")).isEmpty();
  }

  @Test
  void s5_requiresAnActiveOwnEndpointForWebhookActions() {
    var webhook =
        new AutomationDraft("R", true, "ProjectStatusChanged.v1", null, new NotifyWebhookAction(Q));
    assertThatThrownBy(() -> create.create("a", webhook))
        .isInstanceOf(WebhookEndpointNotFoundException.class);
    assertThat(rules.list("a")).isEmpty();
    var accepting =
        new CreateAutomation(
            rules,
            targets,
            (owner, endpoint) -> endpoint.equals(Q),
            Clock.fixed(NOW, ZoneOffset.UTC));
    assertThat(accepting.create("a", webhook).draft().action())
        .isEqualTo(new NotifyWebhookAction(Q));
  }
}
