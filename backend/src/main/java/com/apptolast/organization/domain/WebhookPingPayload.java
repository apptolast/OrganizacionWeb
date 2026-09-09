package com.apptolast.organization.domain;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.UUID;

/** The synthetic body of a ping, byte for byte as the receiver must see it. */
public final class WebhookPingPayload {
  private static final DateTimeFormatter INSTANT =
      new DateTimeFormatterBuilder().appendInstant(6).toFormatter();
  private static final int SCHEMA_VERSION = 1;
  private static final char LAST_CONTROL_CHARACTER = 0x1f;

  private WebhookPingPayload() {}

  public static String of(UUID deliveryId, UUID endpointId, String ownerId, Instant occurredAt) {
    return "{\"eventId\":\""
        + deliveryId
        + "\",\"aggregateId\":\""
        + endpointId
        + "\",\"ownerId\":\""
        + escape(ownerId)
        + "\",\"occurredAt\":\""
        + INSTANT.format(occurredAt)
        + "\",\"schemaVersion\":"
        + SCHEMA_VERSION
        + ",\"type\":\""
        + WebhookDelivery.PING
        + "\"}";
  }

  private static String escape(String text) {
    var escaped = new StringBuilder(text.length());
    for (var index = 0; index < text.length(); index++) escaped.append(escape(text.charAt(index)));
    return escaped.toString();
  }

  private static String escape(char character) {
    return switch (character) {
      case '"' -> "\\\"";
      case '\\' -> "\\\\";
      case '\b' -> "\\b";
      case '\f' -> "\\f";
      case '\n' -> "\\n";
      case '\r' -> "\\r";
      case '\t' -> "\\t";
      default ->
          character <= LAST_CONTROL_CHARACTER
              ? String.format("\\u%04x", (int) character)
              : String.valueOf(character);
    };
  }
}
