package com.apptolast.organization.adapter.webhook;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class WebhookSignatureTest {
  private static final String SECRET = "whsec_AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8";
  private static final String BODY =
      "{\"eventId\":\"11111111-1111-4111-8111-111111111111\",\"aggregateId\":\"22222222-2222-4222-8222-222222222222\",\"ownerId\":\"owner-a\",\"occurredAt\":\"2026-09-08T10:00:00.000000Z\",\"schemaVersion\":1,\"type\":\"webhook.ping.v1\"}";

  @Test
  void s15_signatureCoversTimestampAndExactBodyWithTheWholeSecretAsKey() {
    var body = BODY.getBytes(StandardCharsets.UTF_8);
    assertEquals(209, body.length);
    assertEquals(
        "t=1788861600,v1=47db42f51507bea71512fc26bef335a304b9382b45b590a774cfc977d6cd708f",
        WebhookSignature.header(SECRET, 1788861600L, body));
    assertEquals(
        "t=1788861660,v1=fc161fb2f63428f0680cae6871216284bb420af9917ee794c8159d0400abe460",
        WebhookSignature.header(SECRET, 1788861660L, body));
  }
}
