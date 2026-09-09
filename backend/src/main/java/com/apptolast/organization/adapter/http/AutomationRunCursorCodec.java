package com.apptolast.organization.adapter.http;

import com.apptolast.organization.application.AutomationRunCursor;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/** An opaque base64url position. Anything that does not decode exactly is an invalid cursor. */
final class AutomationRunCursorCodec {
  private static final String SEPARATOR = "|";
  private static final int PARTS = 3;

  private AutomationRunCursorCodec() {}

  static String encode(AutomationRunCursor cursor) {
    if (cursor == null) return null;
    var plain =
        String.join(
            SEPARATOR,
            cursor.ruleId().toString(),
            cursor.executedAt().toString(),
            cursor.id().toString());
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(plain.getBytes(StandardCharsets.UTF_8));
  }

  static AutomationRunCursor decode(String value) {
    if (value == null) return null;
    try {
      if (!value.matches("[A-Za-z0-9_-]+")) throw AutomationBody.invalid("cursor");
      var bytes = Base64.getUrlDecoder().decode(value);
      if (!Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(value))
        throw AutomationBody.invalid("cursor");
      var parts = new String(bytes, StandardCharsets.UTF_8).split("\\" + SEPARATOR, -1);
      if (parts.length != PARTS) throw AutomationBody.invalid("cursor");
      return new AutomationRunCursor(
          UUID.fromString(parts[0]), Instant.parse(parts[1]), UUID.fromString(parts[2]));
    } catch (IllegalArgumentException | java.time.format.DateTimeParseException error) {
      throw AutomationBody.invalid("cursor");
    }
  }
}
