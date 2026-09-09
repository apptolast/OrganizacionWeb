package com.apptolast.organization.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apptolast.organization.domain.ExternalEvent;
import com.apptolast.organization.domain.SyncSummary;
import com.apptolast.organization.domain.ValidationException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** @s2, @s3, @s4 (resolución), @s6, @s7. */
class SaveExternalCalendarTest {
  static final String OWNER = "persona-a";
  static final Instant NOW = Instant.parse("2030-01-07T12:00:00Z");
  static final Instant EARLIER = Instant.parse("2030-01-07T09:00:00Z");
  static final String URL = "https://feed.example.test/calendar/ical/abc123/basic.ics";
  static final String OTHER = "https://otro.example.test/b.ics";

  InMemoryExternalCalendarStore store;
  OutboundGuard.Verdict verdict;
  CountingCipher cipher;

  /** Cada sellado añade un byte distinto: así se ve si se volvió a cifrar o no. */
  static final class CountingCipher implements SecretCipher {
    int seals;

    @Override
    public byte[] encrypt(String ownerId, String url) {
      var plain = (url + "#" + seals++).getBytes(StandardCharsets.UTF_8);
      return plain;
    }

    @Override
    public Optional<String> decrypt(String ownerId, byte[] stored) {
      var text = new String(stored, StandardCharsets.UTF_8);
      return Optional.of(text.substring(0, text.lastIndexOf('#')));
    }
  }

  @BeforeEach
  void setUp() {
    store = new InMemoryExternalCalendarStore();
    verdict = OutboundGuard.Verdict.ALLOWED;
    cipher = new CountingCipher();
  }

  SaveExternalCalendar save() {
    return new SaveExternalCalendar(
        store, cipher, host -> verdict, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  void subscribedWithSnapshot() {
    save().execute(OWNER, "Trabajo", URL);
    store.commitSuccess(
        OWNER,
        0,
        new SyncSummary("Europe/Madrid", 12, 0, 0, 0, false),
        List.of(
            new ExternalEvent(
                "u1",
                "Anterior",
                Instant.parse("2030-01-08T09:00:00Z"),
                Instant.parse("2030-01-08T10:00:00Z"),
                false)),
        EARLIER);
    store.relabel(OWNER, "Trabajo", cipher.encrypt(OWNER, URL), EARLIER);
    store.relabel(OWNER, "Trabajo", cipher.encrypt(OWNER, URL), EARLIER);
  }

  @Test
  void s2_savingWithoutASubscriptionCreatesItAtVersionZero() {
    var saved = save().execute(OWNER, "Trabajo", URL);
    assertEquals("Trabajo", saved.label());
    assertEquals("feed.example.test", saved.urlHost());
    assertEquals(".ics", saved.urlTail());
    assertNull(saved.snapshotZoneId());
    assertNull(saved.lastAttemptAt());
    assertNull(saved.lastSyncAt());
    assertNull(saved.lastStatus());
    assertNull(saved.lastError());
    assertEquals(0, saved.imported());
    assertFalse(saved.truncated());
    assertEquals(NOW, saved.updatedAt());
    assertEquals(0, store.find(OWNER).orElseThrow().version());
  }

  @Test
  void s2_theStoredCiphertextIsNotThePlainUrl() {
    save().execute(OWNER, "Trabajo", URL);
    var stored = store.find(OWNER).orElseThrow().urlCiphertext();
    assertEquals(Optional.of(URL), cipher.decrypt(OWNER, stored));
    assertEquals(1, cipher.seals);
  }

  @Test
  void s3_savingTheSameUrlAgainResealsItWithANewCiphertext() {
    var first = save().execute(OWNER, "Trabajo", URL);
    var before = store.find(OWNER).orElseThrow().urlCiphertext();
    var second = save().execute(OWNER, "Otro", URL);
    var after = store.find(OWNER).orElseThrow().urlCiphertext();
    assertEquals(first.id(), second.id());
    assertEquals(1, store.find(OWNER).orElseThrow().version());
    assertFalse(java.util.Arrays.equals(before, after), "C2 debe ser distinto de C1");
    assertEquals(Optional.of(URL), cipher.decrypt(OWNER, before));
    assertEquals(Optional.of(URL), cipher.decrypt(OWNER, after));
  }

  @Test
  void s6_changingOnlyTheLabelKeepsTheSnapshotAndTheCounters() {
    subscribedWithSnapshot();
    var saved = save().execute(OWNER, "Casa", URL);
    assertEquals(4, store.find(OWNER).orElseThrow().version());
    assertEquals("Casa", saved.label());
    assertEquals(12, saved.imported());
    assertEquals(EARLIER, saved.lastSyncAt());
    assertEquals(1, store.stored(OWNER).size());
  }

  @Test
  void s6_changingTheUrlClearsTheSnapshotAndTheState() {
    subscribedWithSnapshot();
    var saved = save().execute(OWNER, "Trabajo", OTHER);
    assertEquals(4, store.find(OWNER).orElseThrow().version());
    assertEquals("otro.example.test", saved.urlHost());
    assertEquals(0, saved.imported());
    assertNull(saved.lastSyncAt());
    assertNull(saved.lastAttemptAt());
    assertTrue(store.stored(OWNER).isEmpty());
  }

  @Test
  void s6_savingExactlyTheSameLabelAndUrlChangesNothing() {
    subscribedWithSnapshot();
    var before = store.find(OWNER).orElseThrow();
    int sealsBefore = cipher.seals;
    var saved = save().execute(OWNER, "Trabajo", URL);
    assertEquals(3, store.find(OWNER).orElseThrow().version());
    assertEquals(12, saved.imported());
    assertEquals(EARLIER, saved.lastSyncAt());
    assertEquals(1, store.stored(OWNER).size());
    assertArrayEquals(before.urlCiphertext(), store.find(OWNER).orElseThrow().urlCiphertext());
    assertEquals(sealsBefore, cipher.seals, "no se vuelve a cifrar si no hay cambio");
  }

  @Test
  void s5_theLabelIsStrippedBeforeBeingCompared() {
    subscribedWithSnapshot();
    var version = store.find(OWNER).orElseThrow().version();
    save().execute(OWNER, "  Trabajo  ", URL);
    assertEquals(version, store.find(OWNER).orElseThrow().version());
  }

  @Test
  void s29_anUnreadableSecretIsTreatedAsADifferentUrlSoTheSnapshotIsCleared() {
    subscribedWithSnapshot();
    var blind =
        new SaveExternalCalendar(
            store,
            new SecretCipher() {
              @Override
              public byte[] encrypt(String ownerId, String url) {
                return cipher.encrypt(ownerId, url);
              }

              @Override
              public Optional<String> decrypt(String ownerId, byte[] stored) {
                return Optional.empty();
              }
            },
            host -> verdict,
            Clock.fixed(NOW, ZoneOffset.UTC));
    var saved = blind.execute(OWNER, "Trabajo", URL);
    assertEquals(4, store.find(OWNER).orElseThrow().version());
    assertEquals(0, saved.imported());
    assertTrue(store.stored(OWNER).isEmpty());
  }

  @ParameterizedTest
  @CsvSource({"UNRESOLVABLE, UNRESOLVABLE_HOST", "BLOCKED, BLOCKED_ADDRESS"})
  void s4_aHostThatDoesNotPassTheGuardIsRejectedOnTheUrlFieldWithoutWriting(
      OutboundGuard.Verdict rejected, String code) {
    verdict = rejected;
    var error = assertThrows(ValidationException.class, () -> save().execute(OWNER, "Trabajo", URL));
    assertEquals(1, error.errors().size());
    assertEquals("url", error.errors().getFirst().field());
    assertEquals(code, error.errors().getFirst().code());
    assertTrue(store.find(OWNER).isEmpty());
  }

  @Test
  void s4_theGuardIsNotEvenAskedWhenTheUrlIsSyntacticallyInvalid() {
    verdict = OutboundGuard.Verdict.BLOCKED;
    var error =
        assertThrows(
            ValidationException.class, () -> save().execute(OWNER, "Trabajo", "no es una url"));
    assertEquals("INVALID_FORMAT", error.errors().getFirst().code());
  }

  @Test
  void s4_anExistingSubscriptionIsUntouchedWhenTheNewUrlIsRejected() {
    subscribedWithSnapshot();
    verdict = OutboundGuard.Verdict.BLOCKED;
    assertThrows(ValidationException.class, () -> save().execute(OWNER, "Trabajo", OTHER));
    assertEquals(3, store.find(OWNER).orElseThrow().version());
    assertEquals(1, store.stored(OWNER).size());
  }

  @Test
  void s7_deletingReportsWhetherThereWasSomethingToDelete() {
    var delete = new DeleteExternalCalendar(store);
    subscribedWithSnapshot();
    delete.execute(OWNER);
    assertTrue(store.find(OWNER).isEmpty());
    assertTrue(store.stored(OWNER).isEmpty());
    delete.execute(OWNER);
    assertTrue(store.find(OWNER).isEmpty());
  }
}
