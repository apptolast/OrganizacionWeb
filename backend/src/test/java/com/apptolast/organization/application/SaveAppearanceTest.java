package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.domain.*;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SaveAppearanceTest {
  @Test
  void s2_createsTheFirstPreferenceWithCanonicalColorsAndMicroseconds() {
    var instant = Instant.parse("2026-09-07T16:00:00.123456789Z");
    var save =
        new SaveAppearance(
            (owner, operation) -> {
              assertThat(owner).isEqualTo("owner-a");
              return operation.apply(Optional.empty());
            },
            Clock.fixed(instant, ZoneOffset.UTC));
    var saved =
        save.execute("owner-a", new AppearanceRevision(null, 0), "DARK", "#0000ff", "#00ffff");
    assertThat(saved.id()).isNotNull();
    assertThat(saved.owner()).isEqualTo("owner-a");
    assertThat(saved.theme()).isEqualTo("DARK");
    assertThat(saved.accentLight()).isEqualTo("#0000FF");
    assertThat(saved.accentDark()).isEqualTo("#00FFFF");
    assertThat(saved.version()).isZero();
    assertThat(saved.updatedAt()).isEqualTo(Instant.parse("2026-09-07T16:00:00.123456Z"));
  }

  @Test
  void s6_rejectsARevisionWhoseIdentityDoesNotMatchTheOwnersRow() {
    var existing =
        new Appearance(
            java.util.UUID.randomUUID(),
            "owner-a",
            "SYSTEM",
            "#244C3C",
            "#B7E4C7",
            0,
            Instant.EPOCH);
    var save =
        new SaveAppearance(
            (owner, operation) -> operation.apply(Optional.of(existing)),
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
    assertThatThrownBy(
            () ->
                save.execute(
                    "owner-a",
                    new AppearanceRevision(java.util.UUID.randomUUID(), 0),
                    "DARK",
                    "#0000FF",
                    "#00FFFF"))
        .isInstanceOf(AppearanceConflictException.class);
  }

  @Test
  void s10_validatesValuesBeforeConsultingStoredRevision() {
    var save =
        new SaveAppearance(
            (owner, operation) -> {
              throw new AssertionError("Invalid input consulted storage");
            },
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
    assertThatThrownBy(
            () ->
                save.execute(
                    "owner-a", new AppearanceRevision(null, 0), "LIGHT", "#FFFFFF", "#00FFFF"))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error ->
                assertThat(error.errors())
                    .extracting(FieldError::code)
                    .containsExactly("INSUFFICIENT_CONTRAST"));
  }

  @Test
  void s3_s35_currentNoOpPreservesTheExactRowWithoutReadingClockAtMaximumVersion() {
    var existing =
        new Appearance(
            java.util.UUID.randomUUID(),
            "owner-a",
            "SYSTEM",
            "#244C3C",
            "#B7E4C7",
            Long.MAX_VALUE,
            Instant.EPOCH);
    var clock = org.mockito.Mockito.mock(Clock.class);
    var save =
        new SaveAppearance((owner, operation) -> operation.apply(Optional.of(existing)), clock);
    assertThat(
            save.execute(
                "owner-a",
                new AppearanceRevision(existing.id(), Long.MAX_VALUE),
                "SYSTEM",
                "#244c3c",
                "#b7e4c7"))
        .isSameAs(existing);
    org.mockito.Mockito.verifyNoInteractions(clock);
  }

  @Test
  void s4_staleVersionCannotSucceedAsAnAlreadySatisfiedIntent() {
    var existing =
        new Appearance(
            java.util.UUID.randomUUID(),
            "owner-a",
            "SYSTEM",
            "#244C3C",
            "#B7E4C7",
            2,
            Instant.EPOCH);
    var save =
        new SaveAppearance(
            (owner, operation) -> operation.apply(Optional.of(existing)),
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
    assertThatThrownBy(
            () ->
                save.execute(
                    "owner-a",
                    new AppearanceRevision(existing.id(), 1),
                    "SYSTEM",
                    "#244C3C",
                    "#B7E4C7"))
        .isInstanceOf(AppearanceConflictException.class);
  }

  @Test
  void s6_configuredRevisionCannotCreateAnAbsentOwnersRow() {
    var save =
        new SaveAppearance(
            (owner, operation) -> operation.apply(Optional.empty()),
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
    assertThatThrownBy(
            () ->
                save.execute(
                    "owner-a",
                    new AppearanceRevision(java.util.UUID.randomUUID(), 0),
                    "SYSTEM",
                    "#244C3C",
                    "#B7E4C7"))
        .isInstanceOf(AppearanceConflictException.class);
  }

  @Test
  void s2_updatesTheSameIdentityOnceAndDoesNotRegressTime() {
    var oldTime = Instant.parse("2026-09-07T16:00:00.123456Z");
    var existing =
        new Appearance(
            java.util.UUID.randomUUID(), "owner-a", "SYSTEM", "#244C3C", "#B7E4C7", 2, oldTime);
    var save =
        new SaveAppearance(
            (owner, operation) -> operation.apply(Optional.of(existing)),
            Clock.fixed(oldTime.minusSeconds(60), ZoneOffset.UTC));
    assertThat(
            save.execute(
                "owner-a", new AppearanceRevision(existing.id(), 2), "DARK", "#0000FF", "#00FFFF"))
        .isEqualTo(
            new Appearance(existing.id(), "owner-a", "DARK", "#0000FF", "#00FFFF", 3, oldTime));
  }

  @Test
  void s35_realChangeAtMaximumVersionFailsWithoutWrapping() {
    var existing =
        new Appearance(
            java.util.UUID.randomUUID(),
            "owner-a",
            "SYSTEM",
            "#244C3C",
            "#B7E4C7",
            Long.MAX_VALUE,
            Instant.EPOCH);
    var save =
        new SaveAppearance(
            (owner, operation) -> operation.apply(Optional.of(existing)),
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
    assertThatThrownBy(
            () ->
                save.execute(
                    "owner-a",
                    new AppearanceRevision(existing.id(), Long.MAX_VALUE),
                    "DARK",
                    "#0000FF",
                    "#00FFFF"))
        .isInstanceOf(StorageUnavailableException.class);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"0000-12-31T23:59:59Z", "+10000-01-01T00:00:00Z"})
  void s35_rejectsOutOfRangeClockDuringRealChange(String value) {
    var save =
        new SaveAppearance(
            (owner, operation) -> operation.apply(Optional.empty()),
            Clock.fixed(Instant.parse(value), ZoneOffset.UTC));
    assertThatThrownBy(
            () ->
                save.execute(
                    "owner-a", new AppearanceRevision(null, 0), "SYSTEM", "#244C3C", "#B7E4C7"))
        .isInstanceOf(StorageUnavailableException.class);
  }

  @Test
  void s35_failedClockCannotReturnAConfirmedPreference() {
    var clock = org.mockito.Mockito.mock(Clock.class);
    org.mockito.Mockito.when(clock.instant())
        .thenThrow(new IllegalStateException("Clock unavailable"));
    var save = new SaveAppearance((owner, operation) -> operation.apply(Optional.empty()), clock);
    assertThatThrownBy(
            () ->
                save.execute(
                    "owner-a", new AppearanceRevision(null, 0), "SYSTEM", "#244C3C", "#B7E4C7"))
        .isInstanceOf(StorageUnavailableException.class);
  }
}
