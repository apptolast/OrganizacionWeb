package com.apptolast.organization.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WebhookEndpointTest {
  private static final Instant CREATED = Instant.parse("2026-09-01T00:00:00Z");
  private static final Instant NOW = Instant.parse("2026-09-08T11:00:00Z");
  private static final WebhookEndpoint ACTIVE =
      new WebhookEndpoint(
          UUID.randomUUID(),
          "https://example.com/h",
          "",
          List.of("TaskCreated.v1"),
          "active",
          null,
          null,
          CREATED,
          CREATED);

  @Test
  void s12_disablingAnActiveEndpointRecordsManualReasonAndInstant() {
    var disabled = ACTIVE.withStatus("disabled", NOW);
    assertEquals("disabled", disabled.status());
    assertEquals("MANUAL", disabled.disabledReason());
    assertEquals(NOW, disabled.disabledAt());
    assertEquals(NOW, disabled.updatedAt());
    assertEquals(CREATED, disabled.createdAt());
  }

  @Test
  void s12_repeatingTheSameStatusChangesNothing() {
    var disabled = ACTIVE.withStatus("disabled", CREATED);
    assertSame(disabled, disabled.withStatus("disabled", NOW));
    assertSame(ACTIVE, ACTIVE.withStatus("active", NOW));
  }

  @Test
  void s12_s28_activatingClearsReasonAndInstantWhateverTheReason() {
    var exhausted = ACTIVE.disabledByExhaustion(CREATED);
    assertEquals("DELIVERY_EXHAUSTED", exhausted.disabledReason());
    assertEquals(CREATED, exhausted.disabledAt());
    var reactivated = exhausted.withStatus("active", NOW);
    assertEquals("active", reactivated.status());
    assertNull(reactivated.disabledReason());
    assertNull(reactivated.disabledAt());
    assertEquals(NOW, reactivated.updatedAt());
  }

  @Test
  void s27_exhaustionOnAnAlreadyDisabledEndpointKeepsTheEarlierReason() {
    var manual = ACTIVE.withStatus("disabled", CREATED);
    assertSame(manual, manual.disabledByExhaustion(NOW));
  }
}
