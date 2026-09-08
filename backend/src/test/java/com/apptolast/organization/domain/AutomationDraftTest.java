package com.apptolast.organization.domain;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AutomationDraftTest {
  private static final UUID PROJECT = UUID.randomUUID();
  private static final String EMOJI = "\ud83d\ude00";

  private static CreateTaskAction action(String title, String criterion, Integer minutes) {
    return new CreateTaskAction(PROJECT, title, criterion, minutes);
  }

  @Test
  void s1_trimsUnicodeWhiteSpaceAroundTheName() {
    var draft =
        new AutomationDraft(
            "\u2003 Seguimiento \u2003", true, "TaskCreated.v1", null, action("Revisar", null, 30));
    assertThat(draft.name()).isEqualTo("Seguimiento");
    assertThat(draft.action()).isEqualTo(action("Revisar", null, 30));
  }

  @Test
  void s8_rejectsEachInvalidFieldByCodePoints() {
    var okAction = action("Revisar", null, null);
    assertField(
        () -> new AutomationDraft(" \u2003 ", true, "TaskCreated.v1", null, okAction), "name");
    assertField(
        () -> new AutomationDraft(EMOJI.repeat(81), true, "TaskCreated.v1", null, okAction),
        "name");
    assertThat(new AutomationDraft(EMOJI.repeat(80), true, "TaskCreated.v1", null, okAction))
        .isNotNull();
    assertField(() -> action(EMOJI.repeat(161), null, null), "action.titleTemplate");
    assertThat(action(EMOJI.repeat(160), EMOJI.repeat(2000), 1440)).isNotNull();
    assertField(() -> action("", null, null), "action.titleTemplate");
    assertField(() -> action("Ok", EMOJI.repeat(2001), null), "action.criterionTemplate");
    assertField(() -> action("Ok", null, 0), "action.estimatedMinutes");
    assertField(() -> action("Ok", null, 1441), "action.estimatedMinutes");
    assertField(() -> new CreateTaskAction(null, "Ok", null, null), "action.projectId");
    assertField(() -> new NotifyWebhookAction(null), "action.endpointId");
  }

  @Test
  void s2_rejectsUnknownEventTypesBeforeTemplates() {
    assertThatThrownBy(
            () ->
                new AutomationDraft(
                    "Regla", true, "TaskCreated.v2", null, action("{{nada}}", null, null)))
        .isInstanceOf(UnknownEventTypeException.class);
  }

  @Test
  void s7_collectsOneTemplateErrorPerField() {
    assertThatThrownBy(
            () ->
                new AutomationDraft(
                    "Regla",
                    true,
                    "TaskCreated.v1",
                    null,
                    action("{{task.title", "{{nada}}", null)))
        .isInstanceOfSatisfying(
            AutomationTemplateException.class,
            error ->
                assertThat(error.errors())
                    .extracting(FieldError::field, FieldError::code)
                    .containsExactly(
                        tuple("action.titleTemplate", "UNCLOSED_PLACEHOLDER"),
                        tuple("action.criterionTemplate", "UNKNOWN_PLACEHOLDER")));
    assertThat(
            new AutomationDraft(
                "Regla",
                true,
                "ProjectStatusChanged.v1",
                null,
                new NotifyWebhookAction(UUID.randomUUID())))
        .isNotNull();
  }

  private static void assertField(
      org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String field) {
    assertThatThrownBy(call)
        .isInstanceOfSatisfying(
            ValidationException.class,
            error ->
                assertThat(error.errors()).extracting(FieldError::field).isEqualTo(List.of(field)));
  }
}
