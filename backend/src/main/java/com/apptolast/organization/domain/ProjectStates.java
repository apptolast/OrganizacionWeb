package com.apptolast.organization.domain;

import java.util.Map;
import java.util.Set;

public final class ProjectStates {
  private static final Map<String, Set<String>> TRANSITIONS =
      Map.of(
          "idea",
          Set.of("active", "completed"),
          "active",
          Set.of("paused", "completed"),
          "paused",
          Set.of("active", "completed"),
          "completed",
          Set.of("paused"));

  private ProjectStates() {}

  public static boolean valid(String value) {
    return value != null && TRANSITIONS.containsKey(value);
  }

  public static boolean allows(String from, String to) {
    return valid(from) && to != null && TRANSITIONS.get(from).contains(to);
  }
}
