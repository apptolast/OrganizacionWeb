package com.apptolast.organization.domain;

/**
 * Issue abierta tal y como la entrega un gestor externo, ya normalizada al vocabulario del
 * producto. El puerto {@code IssueSource} entrega estos valores; el mapeo a tarea vive aquí para
 * que un segundo conector lo reutilice sin tocar el caso de uso.
 */
public record ExternalIssue(String externalId, String title, String body, String url) {
  private static final int TITLE_LIMIT = 160;
  private static final int CRITERION_LIMIT = 2000;
  private static final int BODY_LINES = 20;
  private static final String ELLIPSIS = "…";
  private static final String BLANK_LINE = "\n\n";

  public String taskTitle() {
    return cut(trim(title), TITLE_LIMIT);
  }

  public String taskCompletionCriterion() {
    String excerpt = firstLines(trim(body));
    return cut(excerpt.isEmpty() ? url : url + BLANK_LINE + excerpt, CRITERION_LIMIT);
  }

  private static String firstLines(String body) {
    return body.lines().limit(BODY_LINES).collect(java.util.stream.Collectors.joining("\n"));
  }

  static String trim(String raw) {
    return raw == null ? "" : raw.replaceAll("(?U)^\\s+|\\s+$", "");
  }

  static String cut(String text, int limit) {
    int points = text.codePointCount(0, text.length());
    if (points <= limit) return text;
    return text.substring(0, text.offsetByCodePoints(0, limit - 1)) + ELLIPSIS;
  }
}
