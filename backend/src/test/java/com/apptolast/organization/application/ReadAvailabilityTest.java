package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.domain.*;
import java.util.*;

class ReadAvailabilityTest {
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void s1_s45_readsAbsenceOrHistoricalTextWithoutCatalog(boolean configured) {
    var days = new EnumMap<java.time.DayOfWeek, Integer>(java.time.DayOfWeek.class);
    for (var day : java.time.DayOfWeek.values()) days.put(day, 0);
    var expected =
        configured
            ? Optional.of(
                new Availability(
                    UUID.randomUUID(),
                    "a",
                    "Historical/Zone",
                    days,
                    0,
                    java.time.Instant.EPOCH,
                    java.time.Instant.EPOCH))
            : Optional.<Availability>empty();
    var read =
        new ReadAvailability(
            owner -> {
              assertThat(owner).isEqualTo("a");
              return expected;
            },
            () -> {
              throw new AssertionError("Historical read consulted catalogue");
            });
    assertThat(read.get("a")).isSameAs(expected);
  }

  @org.junit.jupiter.api.Test
  void s2_returnsSortedExactCatalog() {
    var read =
        new ReadAvailability(
            owner -> {
              throw new AssertionError("Catalogue consulted preferences");
            },
            () -> Set.of("UTC", "CET", "Europe/Madrid", "US/Eastern"));
    assertThat(read.zones()).containsExactly("CET", "Europe/Madrid", "US/Eastern", "UTC");
  }
}
