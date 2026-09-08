package com.apptolast.organization.domain;

/**
 * Issue abierta tal y como la entrega un gestor externo, ya normalizada al vocabulario del
 * producto. El puerto {@code IssueSource} entrega estos valores; el mapeo a tarea vive aquí para
 * que un segundo conector lo reutilice sin tocar el caso de uso.
 */
public record ExternalIssue(String externalId, String title, String body, String url) {
  private static final int TITLE_LIMIT = 160;
  private static final String ELLIPSIS = "…";

  public String taskTitle() {
    return cut(trim(title), TITLE_LIMIT);
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
