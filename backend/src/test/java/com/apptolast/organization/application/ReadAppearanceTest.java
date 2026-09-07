package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class ReadAppearanceTest {
  @Test
  void s1_readsConfirmedAbsenceForTheAuthenticatedOwner() {
    var read =
        new ReadAppearance(
            owner -> {
              assertThat(owner).isEqualTo("owner-a");
              return Optional.empty();
            });
    assertThat(read.get("owner-a")).isEmpty();
  }

  @Test
  void s2_readsTheStoredValuesAndRevisionWithoutChangingThem() {
    var value =
        new com.apptolast.organization.domain.Appearance(
            java.util.UUID.fromString("167989f4-2112-43ba-945d-1c690c974438"),
            "owner-a",
            "DARK",
            "#0000FF",
            "#00FFFF",
            0,
            java.time.Instant.parse("2026-09-07T16:00:00.123456Z"));
    var expected = Optional.of(value);
    var read = new ReadAppearance(owner -> expected);
    assertThat(read.get("owner-a")).isSameAs(expected);
    assertThat(value.theme()).isEqualTo("DARK");
    assertThat(value.accentLight()).isEqualTo("#0000FF");
    assertThat(value.accentDark()).isEqualTo("#00FFFF");
    assertThat(value.version()).isZero();
  }
}
