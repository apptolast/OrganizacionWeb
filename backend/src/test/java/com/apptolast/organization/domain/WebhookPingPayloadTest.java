package com.apptolast.organization.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WebhookPingPayloadTest {
  private static final UUID D = UUID.fromString("33333333-3333-4333-8333-333333333333");
  private static final UUID W = UUID.fromString("22222222-2222-4222-8222-222222222222");

  @Test
  void s17_theBodyCarriesTheSixAgreedPropertiesInOrder() {
    var body = WebhookPingPayload.of(D, W, "owner-a", Instant.parse("2026-09-08T10:00:00.000000Z"));
    assertEquals(
        "{\"eventId\":\"33333333-3333-4333-8333-333333333333\","
            + "\"aggregateId\":\"22222222-2222-4222-8222-222222222222\","
            + "\"ownerId\":\"owner-a\","
            + "\"occurredAt\":\"2026-09-08T10:00:00.000000Z\","
            + "\"schemaVersion\":1,"
            + "\"type\":\"webhook.ping.v1\"}",
        body);
  }

  @Test
  void s17_theInstantAlwaysCarriesSixFractionalDigits() {
    assertTrue(
        WebhookPingPayload.of(D, W, "o", Instant.parse("2026-09-08T10:00:00Z"))
            .contains("\"occurredAt\":\"2026-09-08T10:00:00.000000Z\""));
    assertTrue(
        WebhookPingPayload.of(D, W, "o", Instant.parse("2026-09-08T10:00:00.123456Z"))
            .contains("\"occurredAt\":\"2026-09-08T10:00:00.123456Z\""));
  }

  @Test
  void s17_theOwnerIsEscapedAsJsonText() {
    var quote = "\"";
    var backslash = "\\";
    var body = WebhookPingPayload.of(D, W, "a" + quote + "b" + backslash + "c", Instant.EPOCH);
    assertTrue(
        body.contains("\"ownerId\":\"a" + backslash + quote + "b" + backslash + backslash + "c\""),
        body);
  }

  @Test
  void s17_controlCharactersInTheOwnerBecomeUnicodeEscapes() {
    var startOfHeading = (char) 1;
    var lineFeed = (char) 10;
    var owner = "a" + startOfHeading + "b" + lineFeed + "c";
    var body = WebhookPingPayload.of(D, W, owner, Instant.EPOCH);
    assertTrue(body.contains("\"ownerId\":\"a\\u0001b\\nc\""), body);
  }
}
