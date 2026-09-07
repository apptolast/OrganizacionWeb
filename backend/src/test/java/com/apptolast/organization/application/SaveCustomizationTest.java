package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.apptolast.organization.domain.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class SaveCustomizationTest {
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "0001-01-01T00:00:00Z,0001-01-01T00:00:00Z,0001-01-01T00:00:00Z",
    "9999-12-31T23:59:59.999998Z,9999-12-31T23:59:59.999999Z,9999-12-31T23:59:59.999999Z",
    "2026-09-07T10:00:00Z,2026-09-07T10:00:01.123456789Z,2026-09-07T10:00:01.123456Z"
  })
  void s22_publicCalendarEndpointsAndMicrosecondsConfirmOneRevision(
      String previousTime, String now, String expected) {
    var prior =
        new Customization(
            UUID.randomUUID(),
            "owner-a",
            CustomizationScope.PROJECT,
            List.of(),
            List.of(),
            4,
            Instant.parse(previousTime));
    var clock = org.mockito.Mockito.mock(Clock.class);
    org.mockito.Mockito.when(clock.instant()).thenReturn(Instant.parse(now));
    var saved =
        new SaveCustomizationView(
                (owner, scope, operation) -> operation.apply(Optional.of(prior)), clock)
            .save(
                "owner-a",
                CustomizationScope.PROJECT,
                new CustomizationRevision(prior.id(), 4),
                List.of("createdAt"));
    assertThat(saved.updatedAt()).isEqualTo(Instant.parse(expected));
    assertThat(saved.version()).isEqualTo(5);
    org.mockito.Mockito.verify(clock).instant();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"0000-12-31T23:59:59Z", "+10000-01-01T00:00:00Z"})
  void s20_unrepresentableClockCannotProduceAPublicConfirmation(String instant) {
    var save =
        new SaveCustomizationView(
            (owner, scope, operation) -> operation.apply(Optional.empty()),
            Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                save.save(
                    "owner-a",
                    CustomizationScope.PROJECT,
                    new CustomizationRevision(null, 0),
                    List.of("createdAt")))
        .isInstanceOf(StorageUnavailableException.class);
  }

  @Test
  void s20_changingMaximumConfigurationRevisionFailsWithoutClockOrWrap() {
    var prior =
        new Customization(
            UUID.randomUUID(),
            "owner-a",
            CustomizationScope.PROJECT,
            List.of("createdAt"),
            List.of(),
            Long.MAX_VALUE,
            Instant.EPOCH);
    var clock = org.mockito.Mockito.mock(Clock.class);
    var save =
        new SaveCustomizationView(
            (owner, scope, operation) -> operation.apply(Optional.of(prior)), clock);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                save.save(
                    "owner-a",
                    CustomizationScope.PROJECT,
                    new CustomizationRevision(prior.id(), prior.version()),
                    List.of()))
        .isInstanceOf(StorageUnavailableException.class);
    org.mockito.Mockito.verifyNoInteractions(clock);
  }

  @Test
  void s25_aTaskOnlyVisibleFieldCannotBeSavedForProjects() {
    var store = org.mockito.Mockito.mock(CustomizationEditing.class);
    var save = new SaveCustomizationView(store, Clock.systemUTC());
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                save.save(
                    "owner-a",
                    CustomizationScope.PROJECT,
                    new CustomizationRevision(null, 0),
                    List.of("estimatedMinutes")))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error -> assertThat(error.errors().getFirst().field()).isEqualTo("visibleFields"));
    org.mockito.Mockito.verifyNoInteractions(store);
  }

  @Test
  void s25_duplicateVisibleFieldsAreRejectedBeforeReadingStaleStorage() {
    var store = org.mockito.Mockito.mock(CustomizationEditing.class);
    var save = new SaveCustomizationView(store, Clock.systemUTC());
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                save.save(
                    "owner-a",
                    CustomizationScope.TASK,
                    new CustomizationRevision(UUID.randomUUID(), 3),
                    List.of("createdAt", "createdAt")))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error ->
                assertThat(error.errors())
                    .extracting(FieldError::field, FieldError::code)
                    .containsExactly(
                        org.assertj.core.api.Assertions.tuple("visibleFields", "INVALID_VALUE")));
    org.mockito.Mockito.verifyNoInteractions(store);
  }

  @Test
  void s2_s22_changingViewKeepsDefinitionsIdentityAndMonotoneTime() {
    var field = new CustomFieldDefinition(UUID.randomUUID(), "Dato", CustomFieldType.DATE, false);
    var prior =
        new Customization(
            UUID.randomUUID(),
            "owner-a",
            CustomizationScope.PROJECT,
            List.of("createdAt"),
            List.of(field),
            4,
            Instant.parse("2026-09-07T10:00:00Z"));
    var save =
        new SaveCustomizationView(
            (owner, scope, operation) -> operation.apply(Optional.of(prior)),
            Clock.fixed(Instant.parse("2026-09-07T09:00:00Z"), ZoneOffset.UTC));
    var result =
        save.save(
            "owner-a",
            CustomizationScope.PROJECT,
            new CustomizationRevision(prior.id(), 4),
            List.of());
    assertThat(result.id()).isEqualTo(prior.id());
    assertThat(result.version()).isEqualTo(5);
    assertThat(result.customFields()).containsExactly(field);
    assertThat(result.visibleFields()).isEmpty();
    assertThat(result.updatedAt()).isEqualTo(prior.updatedAt());
  }

  @Test
  void s8_s21_currentNoOpPreservesTheFullObjectAtMaximumRevisionWithoutClock() {
    var field = new CustomFieldDefinition(UUID.randomUUID(), "Hidden", CustomFieldType.TEXT, false);
    var prior =
        new Customization(
            UUID.randomUUID(),
            "owner-a",
            CustomizationScope.TASK,
            List.of("estimatedMinutes"),
            List.of(field),
            Long.MAX_VALUE,
            Instant.EPOCH);
    var clock = org.mockito.Mockito.mock(Clock.class);
    var save =
        new SaveCustomizationView(
            (owner, scope, operation) -> operation.apply(Optional.of(prior)), clock);
    assertThat(
            save.save(
                "owner-a",
                CustomizationScope.TASK,
                new CustomizationRevision(prior.id(), prior.version()),
                prior.visibleFields()))
        .isSameAs(prior);
    org.mockito.Mockito.verifyNoInteractions(clock);
  }

  @Test
  void s8_staleRevisionPrecedesAnOtherwiseIdenticalViewAndDoesNotReadClock() {
    var prior =
        new Customization(
            UUID.randomUUID(),
            "owner-a",
            CustomizationScope.PROJECT,
            List.of("createdAt"),
            List.of(),
            4,
            Instant.EPOCH);
    CustomizationEditing store = (owner, scope, operation) -> operation.apply(Optional.of(prior));
    var clock = org.mockito.Mockito.mock(Clock.class);
    org.mockito.Mockito.when(clock.instant())
        .thenThrow(new AssertionError("Clock must not be read"));
    var save = new SaveCustomizationView(store, clock);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                save.save(
                    "owner-a",
                    CustomizationScope.PROJECT,
                    new CustomizationRevision(prior.id(), 3),
                    prior.visibleFields()))
        .isInstanceOf(CustomizationConflictException.class);
    org.mockito.Mockito.verifyNoInteractions(clock);
  }

  @Test
  void s2_firstViewSaveCreatesOnlyItsScopeAtRevisionZero() {
    CustomizationEditing store =
        (owner, scope, operation) -> {
          assertThat(owner).isEqualTo("owner-a");
          assertThat(scope).isEqualTo(CustomizationScope.PROJECT);
          return operation.apply(Optional.empty());
        };
    var save =
        new SaveCustomizationView(
            store, Clock.fixed(Instant.parse("2026-09-07T21:00:00.123456789Z"), ZoneOffset.UTC));
    var result =
        save.save(
            "owner-a",
            CustomizationScope.PROJECT,
            new CustomizationRevision(null, 0),
            List.of("updatedAt", "createdAt"));
    assertThat(result.id()).isNotNull();
    assertThat(result.owner()).isEqualTo("owner-a");
    assertThat(result.scope()).isEqualTo(CustomizationScope.PROJECT);
    assertThat(result.visibleFields()).containsExactly("updatedAt", "createdAt");
    assertThat(result.customFields()).isEmpty();
    assertThat(result.version()).isZero();
    assertThat(result.updatedAt()).isEqualTo("2026-09-07T21:00:00.123456Z");
  }
}
