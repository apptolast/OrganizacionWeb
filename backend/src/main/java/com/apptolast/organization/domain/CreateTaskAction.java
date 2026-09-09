package com.apptolast.organization.domain;

import java.util.List;
import java.util.UUID;

public record CreateTaskAction(
    UUID projectId, String titleTemplate, String criterionTemplate, Integer estimatedMinutes)
    implements AutomationAction {
  public static final int TITLE_LIMIT = 160;
  public static final int CRITERION_LIMIT = 2000;
  private static final int MINUTES_MIN = 1;
  private static final int MINUTES_MAX = 1440;

  public CreateTaskAction {
    if (projectId == null) throw invalid("action.projectId", "REQUIRED");
    if (titleTemplate == null || titleTemplate.isEmpty())
      throw invalid("action.titleTemplate", "REQUIRED");
    if (codePoints(titleTemplate) > TITLE_LIMIT) throw invalid("action.titleTemplate", "TOO_LONG");
    if (criterionTemplate != null && codePoints(criterionTemplate) > CRITERION_LIMIT)
      throw invalid("action.criterionTemplate", "TOO_LONG");
    if (estimatedMinutes != null
        && (estimatedMinutes < MINUTES_MIN || estimatedMinutes > MINUTES_MAX))
      throw invalid("action.estimatedMinutes", "OUT_OF_RANGE");
  }

  @Override
  public String type() {
    return "CREATE_TASK";
  }

  /**
   * The deterministic failure a run would hit once the templates are resolved. A resolved value
   * over the limit is never truncated: the run fails and the simulation says so beforehand.
   */
  public java.util.Optional<String> resolvedFailure(String title, String criterion) {
    if (codePoints(title) > TITLE_LIMIT) return java.util.Optional.of("TITLE_TOO_LONG");
    if (codePoints(criterion) > CRITERION_LIMIT) return java.util.Optional.of("CRITERION_TOO_LONG");
    return java.util.Optional.empty();
  }

  static int codePoints(String value) {
    return value.codePointCount(0, value.length());
  }

  static ValidationException invalid(String field, String code) {
    return new ValidationException(
        List.of(new FieldError(field, code, "Revisa el valor de este campo.")));
  }
}
