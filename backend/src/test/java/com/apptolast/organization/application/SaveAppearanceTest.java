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
    var save = new SaveAppearance((owner, operation) -> {
      assertThat(owner).isEqualTo("owner-a");
      return operation.apply(Optional.empty());
    }, Clock.fixed(instant, ZoneOffset.UTC));
    var saved = save.execute("owner-a", new AppearanceRevision(null, 0),
        "DARK", "#0000ff", "#00ffff");
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
    var existing = new Appearance(java.util.UUID.randomUUID(), "owner-a", "SYSTEM",
        "#244C3C", "#B7E4C7", 0, Instant.EPOCH);
    var save = new SaveAppearance((owner, operation) -> operation.apply(Optional.of(existing)),
        Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
    assertThatThrownBy(() -> save.execute("owner-a", new AppearanceRevision(java.util.UUID.randomUUID(), 0),
        "DARK", "#0000FF", "#00FFFF")).isInstanceOf(AppearanceConflictException.class);
  }}
