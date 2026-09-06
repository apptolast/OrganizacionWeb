package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.domain.*;
import java.time.*;
import java.util.*;

class SaveAvailabilityTest {
  static Map<DayOfWeek, Integer> days(int value) {
    var result = new EnumMap<DayOfWeek, Integer>(DayOfWeek.class);
    for (var day : DayOfWeek.values()) result.put(day, value);
    return result;
  }

  @org.junit.jupiter.api.Test
  void s3_createsOwnPreferenceWithMicrosecondDate() {
    AvailabilityEditing store =
        (owner, operation) -> {
          assertThat(owner).isEqualTo("persona-a");
          return operation.apply(Optional.empty());
        };
    var result =
        new SaveAvailability(
                store,
                () -> Set.of("Europe/Madrid", "UTC"),
                Clock.fixed(Instant.parse("2030-01-01T00:00:00.123456789Z"), ZoneOffset.UTC))
            .execute("persona-a", new AvailabilityRevision(null, 0), "Europe/Madrid", days(60));
    assertThat(result.id()).isNotNull();
    assertThat(result.ownerId()).isEqualTo("persona-a");
    assertThat(result.zoneId()).isEqualTo("Europe/Madrid");
    assertThat(result.dailyMinutes()).isEqualTo(days(60));
    assertThat(result.version()).isZero();
    assertThat(result.createdAt()).isEqualTo(Instant.parse("2030-01-01T00:00:00.123456Z"));
    assertThat(result.updatedAt()).isEqualTo(result.createdAt());
  }

  @org.junit.jupiter.api.Test
  void s46_validatesCurrentCatalogBeforeAttemptingHistoricalNoOp() {
    var prior =
        new Availability(
            UUID.randomUUID(),
            "persona-a",
            "Historical/Zone",
            days(60),
            0,
            Instant.EPOCH,
            Instant.EPOCH);
    AvailabilityEditing store =
        (owner, operation) -> {
          throw new AssertionError("Invalid zone reached storage");
        };
    assertThatThrownBy(
            () ->
                new SaveAvailability(store, () -> Set.of("UTC"), Clock.systemUTC())
                    .execute(
                        "persona-a",
                        new AvailabilityRevision(prior.id(), 0),
                        prior.zoneId(),
                        prior.dailyMinutes()))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error -> {
              assertThat(error.errors())
                  .singleElement()
                  .satisfies(
                      field -> {
                        assertThat(field.field()).isEqualTo("zoneId");
                        assertThat(field.code()).isEqualTo("INVALID_VALUE");
                        assertThat(field.message()).isNotBlank();
                      });
            });
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"advance", "equal", "backward"})
  void s7_s9_changesContentWithStableIdentityAndNondecreasingDate(String time) {
    var initial = Instant.parse("2030-01-01T00:00:00.123456Z");
    var prior =
        new Availability(
            UUID.randomUUID(), "persona-a", "Europe/Madrid", days(60), 2, Instant.EPOCH, initial);
    var now =
        time.equals("advance")
            ? initial.plusSeconds(1).plusNanos(789)
            : time.equals("equal") ? initial.plusNanos(123) : initial.minusSeconds(1);
    var result =
        new SaveAvailability(
                (owner, operation) -> operation.apply(Optional.of(prior)),
                () -> Set.of("UTC"),
                Clock.fixed(now, ZoneOffset.UTC))
            .execute("persona-a", new AvailabilityRevision(prior.id(), 2), "UTC", days(30));
    assertThat(result.id()).isEqualTo(prior.id());
    assertThat(result.createdAt()).isEqualTo(prior.createdAt());
    assertThat(result.version()).isEqualTo(3);
    assertThat(result.zoneId()).isEqualTo("UTC");
    assertThat(result.dailyMinutes()).isEqualTo(days(30));
    assertThat(result.updatedAt())
        .isEqualTo(time.equals("advance") ? initial.plusSeconds(1) : initial);
  }

  @org.junit.jupiter.api.Test
  void s8_returnsSameSnapshotForCurrentNoOp() {
    var prior =
        new Availability(UUID.randomUUID(), "a", "UTC", days(60), 2, Instant.EPOCH, Instant.EPOCH);
    var result =
        new SaveAvailability(
                (owner, operation) -> operation.apply(Optional.of(prior)),
                () -> Set.of("UTC"),
                Clock.systemUTC())
            .execute("a", new AvailabilityRevision(prior.id(), 2), "UTC", days(60));
    assertThat(result).isSameAs(prior);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"oldChanged", "oldNoOp", "unconfigured", "foreign", "missingId", "missingRow"})
  void s11_checksRevisionBeforeNoOp(String defect) {
    var prior =
        new Availability(UUID.randomUUID(), "a", "UTC", days(60), 2, Instant.EPOCH, Instant.EPOCH);
    var expected =
        defect.startsWith("old")
            ? new AvailabilityRevision(prior.id(), 1)
            : defect.equals("unconfigured")
                ? new AvailabilityRevision(null, 0)
                : new AvailabilityRevision(UUID.randomUUID(), 2);
    var useCase =
        new SaveAvailability(
            (owner, operation) ->
                operation.apply(
                    defect.equals("missingRow") ? Optional.empty() : Optional.of(prior)),
            () -> Set.of("UTC"),
            Clock.systemUTC());
    assertThatThrownBy(
            () ->
                useCase.execute("a", expected, "UTC", days(defect.equals("oldChanged") ? 30 : 60)))
        .isInstanceOf(AvailabilityConflictException.class);
  }
}
