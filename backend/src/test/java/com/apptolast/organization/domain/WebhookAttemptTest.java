package com.apptolast.organization.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class WebhookAttemptTest {
  private static final Instant T = Instant.parse("2026-09-08T12:00:00.000000Z");
  private static final UUID D = UUID.fromString("33333333-3333-4333-8333-333333333333");

  private static WebhookDelivery pendingWith(int failedAttempts) {
    return new WebhookDelivery(
        D, D, "TaskCreated.v1", "pending", failedAttempts, null, null, null, T, T, T);
  }

  @ParameterizedTest
  @CsvSource({
    "0,1,pending,PT1M",
    "1,2,pending,PT5M",
    "2,3,pending,PT30M",
    "3,4,pending,PT2H",
    "4,5,pending,PT24H",
    "5,6,exhausted,"
  })
  void s24_theFixedTableDrivesEachRetryAndTheSixthFailureExhausts(
      int previous, int attempt, String status, String delay) {
    var recorded = pendingWith(previous).recorded(WebhookAttempt.http(500, 12), T);

    assertEquals(attempt, recorded.attempt());
    assertEquals(status, recorded.status());
    assertEquals("HTTP_ERROR", recorded.errorClass());
    assertEquals(500, recorded.httpStatus());
    assertEquals(12, recorded.latencyMs());
    assertEquals(T, recorded.createdAt());
    assertEquals(T, recorded.updatedAt());
    if (delay == null || delay.isEmpty()) assertNull(recorded.nextAttemptAt());
    else assertEquals(T.plus(java.time.Duration.parse(delay)), recorded.nextAttemptAt());
  }

  @ParameterizedTest
  @ValueSource(ints = {200, 204, 299})
  void s26_anyTwoHundredRangeClosesTheDeliveryAsSucceeded(int code) {
    var recorded = pendingWith(0).recorded(WebhookAttempt.http(code, 7), T);

    assertEquals("succeeded", recorded.status());
    assertEquals(1, recorded.attempt());
    assertEquals(code, recorded.httpStatus());
    assertNull(recorded.errorClass());
    assertNull(recorded.nextAttemptAt());
    assertEquals(7, recorded.latencyMs());
  }

  @ParameterizedTest
  @CsvSource({"404,HTTP_ERROR", "500,HTTP_ERROR", "302,REDIRECT", "199,HTTP_ERROR", "300,REDIRECT"})
  void s25_everyNonSuccessCodeIsClassifiedWithoutRetryingTheRedirect(int code, String expected) {
    var attempt = WebhookAttempt.http(code, 3);
    assertEquals(expected, attempt.errorClass());
    assertEquals(code, attempt.httpStatus());
    assertFalse(attempt.succeeded());
  }

  @ParameterizedTest
  @ValueSource(strings = {"TIMEOUT", "CONNECTION", "TLS", "DNS", "BLOCKED_ADDRESS"})
  void s25_atransportFailureHasNoHttpStatusAndKeepsTheDeliveryPending(String errorClass) {
    var attempt = WebhookAttempt.transport(errorClass, 5);
    assertNull(attempt.httpStatus());
    assertEquals(errorClass, attempt.errorClass());
    assertFalse(attempt.succeeded());

    var recorded = pendingWith(0).recorded(attempt, T);
    assertEquals("pending", recorded.status());
    assertNull(recorded.httpStatus());
    assertEquals(errorClass, recorded.errorClass());
    assertEquals(T.plus(java.time.Duration.ofMinutes(1)), recorded.nextAttemptAt());
  }

  @org.junit.jupiter.api.Test
  void s25_theMeasuredLatencyIsNeverNegative() {
    assertThrows(IllegalArgumentException.class, () -> WebhookAttempt.http(200, -1));
    assertEquals(0, WebhookAttempt.http(200, 0).latencyMs());
  }
}
