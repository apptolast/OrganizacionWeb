package com.apptolast.organization.domain;

import java.time.Instant;

/** Values available to a template at evaluation time; taskTitle is null for Project* events. */
public record TemplateValues(
    String eventType, Instant occurredAt, String taskTitle, String projectName) {
  public TemplateValues withoutTask() {
    return new TemplateValues(eventType, occurredAt, null, projectName);
  }

  String value(String placeholder) {
    return switch (placeholder) {
      case "event.type" -> eventType;
      case "task.title" -> taskTitle;
      case "project.name" -> projectName;
      default -> occurredAt.toString();
    };
  }
}
