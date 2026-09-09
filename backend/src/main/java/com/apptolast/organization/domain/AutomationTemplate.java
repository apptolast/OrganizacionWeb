package com.apptolast.organization.domain;

import java.util.List;
import java.util.Optional;

/** Plain-text templates with exactly four literal placeholders and no logic. */
public final class AutomationTemplate {
  public static final List<String> PLACEHOLDERS =
      List.of("event.type", "task.title", "project.name", "occurredAt");
  private static final String OPEN = "{{";
  private static final String CLOSE = "}}";

  private AutomationTemplate() {}

  public static Optional<String> validationCode(String template, String eventType) {
    int from = 0;
    while (true) {
      int start = template.indexOf(OPEN, from);
      if (start < 0) return Optional.empty();
      int end = template.indexOf(CLOSE, start + OPEN.length());
      if (end < 0) return Optional.of("UNCLOSED_PLACEHOLDER");
      String name = template.substring(start + OPEN.length(), end);
      if (!PLACEHOLDERS.contains(name)) return Optional.of("UNKNOWN_PLACEHOLDER");
      if (name.equals("task.title") && eventType.startsWith("Project"))
        return Optional.of("PLACEHOLDER_NOT_AVAILABLE");
      from = end + CLOSE.length();
    }
  }

  public static String render(String template, TemplateValues values) {
    var out = new StringBuilder();
    int from = 0;
    while (true) {
      int start = template.indexOf(OPEN, from);
      if (start < 0) return out.append(template, from, template.length()).toString();
      int end = template.indexOf(CLOSE, start + OPEN.length());
      out.append(template, from, start)
          .append(values.value(template.substring(start + OPEN.length(), end)));
      from = end + CLOSE.length();
    }
  }
}
