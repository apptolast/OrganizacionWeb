package com.apptolast.organization.domain;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public record WebhookIntent(String url, String description, List<String> eventTypes) {
  /** Subscribable types, in canonical order: exactly those accepted by OutboxMessage. */
  public static final List<String> CATALOG =
      List.of(
          "ProjectCreated.v1",
          "ProjectUpdated.v1",
          "ProjectStatusChanged.v1",
          "TaskCreated.v1",
          "SubtaskCreated.v1",
          "TaskStatusChanged.v1",
          "BlockPlanned.v1",
          "BlockChanged.v1",
          "WorkSessionStarted.v1",
          "WorkSessionStateChanged.v1",
          "WorkSessionExtended.v1",
          "WorkSessionClosed.v1");

  private static final int MAX_URL_CODE_POINTS = 2048;
  private static final int MAX_DESCRIPTION_CODE_POINTS = 80;

  public WebhookIntent {
    var invalid = new ArrayList<String>();
    if (!validUrl(url)) invalid.add("url");
    description = description == null ? "" : description.replaceAll("(?U)^\\s+|\\s+$", "");
    if (!validDescription(description)) invalid.add("description");
    if (!validEventTypes(eventTypes)) invalid.add("eventTypes");
    if (!invalid.isEmpty()) throw new WebhookInvalidException(invalid);
    var selected = new HashSet<>(eventTypes);
    eventTypes = CATALOG.stream().filter(selected::contains).toList();
  }

  private static boolean validUrl(String url) {
    if (url == null || url.isEmpty() || url.codePointCount(0, url.length()) > MAX_URL_CODE_POINTS)
      return false;
    URI parsed;
    try {
      parsed = new URI(url);
    } catch (URISyntaxException error) {
      return false;
    }
    return "https".equals(parsed.getScheme())
        && parsed.getHost() != null
        && parsed.getRawUserInfo() == null
        && parsed.getRawFragment() == null
        && (parsed.getPort() == -1 || parsed.getPort() >= 1 && parsed.getPort() <= 65535);
  }

  private static boolean validDescription(String description) {
    return description.codePointCount(0, description.length()) <= MAX_DESCRIPTION_CODE_POINTS
        && description.codePoints().noneMatch(Character::isISOControl);
  }

  private static boolean validEventTypes(List<String> eventTypes) {
    return eventTypes != null
        && !eventTypes.isEmpty()
        && CATALOG.containsAll(eventTypes)
        && new HashSet<>(eventTypes).size() == eventTypes.size();
  }
}
