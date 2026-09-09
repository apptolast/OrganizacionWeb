package com.apptolast.organization.domain;

import java.util.ArrayList;
import java.util.UUID;

/** A validated rule body: what the owner sends to create, replace or simulate a rule. */
public record AutomationDraft(
    String name,
    boolean enabled,
    String eventType,
    UUID conditionProjectId,
    AutomationAction action) {
  private static final int NAME_LIMIT = 80;

  public AutomationDraft {
    name = name == null ? "" : name.replaceAll("(?U)^\\s+|\\s+$", "");
    if (name.isEmpty()) throw CreateTaskAction.invalid("name", "REQUIRED");
    if (CreateTaskAction.codePoints(name) > NAME_LIMIT)
      throw CreateTaskAction.invalid("name", "TOO_LONG");
    if (action == null) throw CreateTaskAction.invalid("action", "REQUIRED");
    eventType = AutomationEventType.of(eventType).orElseThrow(UnknownEventTypeException::new);
    if (action instanceof CreateTaskAction task) checkTemplates(task, eventType);
  }

  private static void checkTemplates(CreateTaskAction task, String eventType) {
    var errors = new ArrayList<FieldError>();
    AutomationTemplate.validationCode(task.titleTemplate(), eventType)
        .ifPresent(code -> errors.add(templateError("action.titleTemplate", code)));
    if (task.criterionTemplate() != null)
      AutomationTemplate.validationCode(task.criterionTemplate(), eventType)
          .ifPresent(code -> errors.add(templateError("action.criterionTemplate", code)));
    if (!errors.isEmpty()) throw new AutomationTemplateException(errors);
  }

  private static FieldError templateError(String field, String code) {
    return new FieldError(field, code, "Revisa los marcadores de esta plantilla.");
  }
}
