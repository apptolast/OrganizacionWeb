package com.apptolast.organization.domain;

import static org.assertj.core.api.Assertions.*;

import java.time.*;
import java.util.*;

class AvailabilityTest {
  @org.junit.jupiter.api.Test
  void s5_s6_keepsSevenDailyBudgetsAndDerivesWeeklyTotal() {
    var days = new EnumMap<DayOfWeek, Integer>(DayOfWeek.class);
    for (var day : DayOfWeek.values()) days.put(day, 60);
    var preference =
        new Availability(
            UUID.randomUUID(), "persona-a", "Europe/Madrid", days, 0, Instant.EPOCH, Instant.EPOCH);
    assertThat(preference.weeklyMinutes()).isEqualTo(420);
    assertThat(preference.dailyMinutes()).containsExactlyInAnyOrderEntriesOf(days);
  }

  @org.junit.jupiter.api.Test
  void s8_snapshotCannotBeChangedThroughCallerMap() {
    var days = new EnumMap<DayOfWeek, Integer>(DayOfWeek.class);
    for (var day : DayOfWeek.values()) days.put(day, 60);
    var preference =
        new Availability(
            UUID.randomUUID(), "a", "Historical/Zone", days, 0, Instant.EPOCH, Instant.EPOCH);
    days.put(DayOfWeek.MONDAY, 0);
    assertThat(preference.weeklyMinutes()).isEqualTo(420);
    assertThatThrownBy(() -> preference.dailyMinutes().put(DayOfWeek.MONDAY, 0))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "id",
        "owner",
        "blankOwner",
        "zone",
        "blankZone",
        "days",
        "missingDay",
        "nullDay",
        "negative",
        "tooLarge",
        "version",
        "created",
        "updated",
        "backwards"
      })
  void s5_s9_rejectsInvalidReconstructedPreference(String defect) {
    var days = new EnumMap<DayOfWeek, Integer>(DayOfWeek.class);
    for (var day : DayOfWeek.values()) days.put(day, 60);
    if (defect.equals("missingDay")) days.remove(DayOfWeek.FRIDAY);
    if (defect.equals("nullDay")) days.put(DayOfWeek.MONDAY, null);
    if (defect.equals("negative")) days.put(DayOfWeek.MONDAY, -1);
    if (defect.equals("tooLarge")) days.put(DayOfWeek.SUNDAY, 1441);
    assertThatThrownBy(
            () ->
                new Availability(
                    defect.equals("id") ? null : UUID.randomUUID(),
                    defect.equals("owner") ? null : defect.equals("blankOwner") ? " " : "a",
                    defect.equals("zone")
                        ? null
                        : defect.equals("blankZone") ? " " : "Historical/Zone",
                    defect.equals("days") ? null : days,
                    defect.equals("version") ? -1 : 0,
                    defect.equals("created") ? null : Instant.EPOCH,
                    defect.equals("updated")
                        ? null
                        : defect.equals("backwards")
                            ? Instant.EPOCH.minusSeconds(1)
                            : Instant.EPOCH))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
