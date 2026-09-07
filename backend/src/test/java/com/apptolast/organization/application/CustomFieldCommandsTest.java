package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.apptolast.organization.domain.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CustomFieldCommandsTest {
  @Test
  void s22_updatePreservesPreviousTimeWhenClockGoesBackwards() {
    var field = new CustomFieldDefinition(UUID.randomUUID(), "Dato", CustomFieldType.TEXT, true);
    var previous =
        new Customization(
            UUID.randomUUID(),
            "owner-a",
            CustomizationScope.PROJECT,
            List.of(),
            List.of(field),
            4,
            Instant.parse("2026-09-07T20:00:00Z"));
    var update =
        new UpdateCustomField(
            (owner, scope, operation) -> operation.apply(Optional.of(previous)),
            Clock.fixed(Instant.parse("2026-09-07T19:00:00Z"), ZoneOffset.UTC));
    var result =
        update.update(
            "owner-a",
            CustomizationScope.PROJECT,
            field.id(),
            new CustomizationRevision(previous.id(), 4),
            "Dato",
            false);
    assertThat(result.updatedAt()).isEqualTo(previous.updatedAt());
    assertThat(result.version()).isEqualTo(5);
  }

  @Test
  void s20_realDefinitionUpdateAtMaximumRevisionFailsWithoutClock() {
    var field = new CustomFieldDefinition(UUID.randomUUID(), "Dato", CustomFieldType.TEXT, true);
    var previous =
        new Customization(
            UUID.randomUUID(),
            "owner-a",
            CustomizationScope.PROJECT,
            List.of(),
            List.of(field),
            Long.MAX_VALUE,
            Instant.parse("2026-09-07T20:00:00Z"));
    var clock = mock(Clock.class);
    when(clock.instant()).thenThrow(new AssertionError("No clock at exhausted revision"));
    var update =
        new UpdateCustomField(
            (owner, scope, operation) -> operation.apply(Optional.of(previous)), clock);
    assertThatThrownBy(
            () ->
                update.update(
                    "owner-a",
                    CustomizationScope.PROJECT,
                    field.id(),
                    new CustomizationRevision(previous.id(), Long.MAX_VALUE),
                    "Dato",
                    false))
        .isInstanceOf(StorageUnavailableException.class);
    verifyNoInteractions(clock);
  }

  @Test
  void s4_renameCannotReuseAnotherInactiveDefinitionLabel() {
    var first = new CustomFieldDefinition(UUID.randomUUID(), "Primero", CustomFieldType.TEXT, true);
    var second =
        new CustomFieldDefinition(UUID.randomUUID(), "Reservado", CustomFieldType.DATE, false);
    var previous =
        new Customization(
            UUID.randomUUID(),
            "owner-a",
            CustomizationScope.PROJECT,
            List.of(),
            List.of(first, second),
            4,
            Instant.parse("2026-09-07T20:00:00Z"));
    var clock = mock(Clock.class);
    when(clock.instant()).thenThrow(new AssertionError("No clock on duplicate label"));
    var update =
        new UpdateCustomField(
            (owner, scope, operation) -> operation.apply(Optional.of(previous)), clock);
    assertThatThrownBy(
            () ->
                update.update(
                    "owner-a",
                    CustomizationScope.PROJECT,
                    first.id(),
                    new CustomizationRevision(previous.id(), 4),
                    "Reservado",
                    true))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error -> {
              assertThat(error.errors().getFirst().field()).isEqualTo("label");
              assertThat(error.errors().getFirst().code()).isEqualTo("INVALID_VALUE");
            });
    verifyNoInteractions(clock);
  }

  @Test
  void s21_currentIdenticalUpdateAtMaximumVersionIsNoOpWithoutClock() {
    var field = new CustomFieldDefinition(UUID.randomUUID(), "Dato", CustomFieldType.TEXT, false);
    var previous =
        new Customization(
            UUID.randomUUID(),
            "owner-a",
            CustomizationScope.PROJECT,
            List.of(),
            List.of(field),
            Long.MAX_VALUE,
            Instant.parse("2026-09-07T20:00:00Z"));
    var clock = mock(Clock.class);
    when(clock.instant()).thenThrow(new AssertionError("No clock on unchanged definition"));
    var update =
        new UpdateCustomField(
            (owner, scope, operation) -> operation.apply(Optional.of(previous)), clock);
    assertThat(
            update.update(
                "owner-a",
                CustomizationScope.PROJECT,
                field.id(),
                new CustomizationRevision(previous.id(), Long.MAX_VALUE),
                "  Dato  ",
                false))
        .isSameAs(previous);
    verifyNoInteractions(clock);
  }

  @Test
  void s8_staleUpdateCannotSucceedEvenWhenLabelAndActiveAreUnchanged() {
    var field = new CustomFieldDefinition(UUID.randomUUID(), "Dato", CustomFieldType.TEXT, true);
    var previous =
        new Customization(
            UUID.randomUUID(),
            "owner-a",
            CustomizationScope.PROJECT,
            List.of(),
            List.of(field),
            4,
            Instant.parse("2026-09-07T20:00:00Z"));
    var clock = mock(Clock.class);
    when(clock.instant()).thenThrow(new AssertionError("No clock after stale update"));
    var update =
        new UpdateCustomField(
            (owner, scope, operation) -> operation.apply(Optional.of(previous)), clock);
    assertThatThrownBy(
            () ->
                update.update(
                    "owner-a",
                    CustomizationScope.PROJECT,
                    field.id(),
                    new CustomizationRevision(previous.id(), 3),
                    "Dato",
                    true))
        .isInstanceOf(CustomizationConflictException.class);
    verifyNoInteractions(clock);
  }

  @Test
  void s17_missingDefinitionPrecedesStaleRevisionAndClock() {
    var previous =
        new Customization(
            UUID.randomUUID(),
            "owner-a",
            CustomizationScope.PROJECT,
            List.of(),
            List.of(),
            4,
            Instant.parse("2026-09-07T20:00:00Z"));
    var clock = mock(Clock.class);
    when(clock.instant()).thenThrow(new AssertionError("No clock on missing definition"));
    var update =
        new UpdateCustomField(
            (owner, scope, operation) -> operation.apply(Optional.of(previous)), clock);
    assertThatThrownBy(
            () ->
                update.update(
                    "owner-a",
                    CustomizationScope.PROJECT,
                    UUID.randomUUID(),
                    new CustomizationRevision(previous.id(), 3),
                    "Nombre",
                    true))
        .isInstanceOf(ResourceNotFoundException.class);
    verifyNoInteractions(clock);
  }

  @Test
  void s7_renamesAndDeactivatesDefinitionPreservingTypeIdentityAndOrder() {
    var first = new CustomFieldDefinition(UUID.randomUUID(), "Primero", CustomFieldType.DATE, true);
    var second =
        new CustomFieldDefinition(UUID.randomUUID(), "Segundo", CustomFieldType.TEXT, true);
    var previous =
        new Customization(
            UUID.randomUUID(),
            "owner-a",
            CustomizationScope.PROJECT,
            List.of("updatedAt"),
            List.of(first, second),
            4,
            Instant.parse("2026-09-07T20:00:00Z"));
    var update =
        new UpdateCustomField(
            (owner, scope, operation) -> operation.apply(Optional.of(previous)),
            Clock.fixed(Instant.parse("2026-09-07T21:00:00Z"), ZoneOffset.UTC));
    var result =
        update.update(
            "owner-a",
            CustomizationScope.PROJECT,
            first.id(),
            new CustomizationRevision(previous.id(), 4),
            "  Renombrado  ",
            false);
    assertThat(result.id()).isEqualTo(previous.id());
    assertThat(result.visibleFields()).isEqualTo(previous.visibleFields());
    assertThat(result.customFields())
        .containsExactly(
            new CustomFieldDefinition(first.id(), "Renombrado", CustomFieldType.DATE, false),
            second);
    assertThat(result.version()).isEqualTo(5);
    assertThat(result.updatedAt()).isEqualTo("2026-09-07T21:00:00Z");
  }

  @ParameterizedTest
  @ValueSource(strings = {"0000-12-31T23:59:59Z", "+10000-01-01T00:00:00Z"})
  void s20_creationRejectsAnUnrepresentableClock(String instant) {
    var create =
        new CreateCustomField(
            (owner, scope, operation) -> operation.apply(Optional.empty()),
            Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    assertThatThrownBy(
            () ->
                create.create(
                    "owner-a",
                    CustomizationScope.PROJECT,
                    new CustomizationRevision(null, 0),
                    "Otro",
                    CustomFieldType.TEXT))
        .isInstanceOf(StorageUnavailableException.class);
  }

  @Test
  void s22_creationKeepsMonotoneTimestampWhenClockGoesBackwards() {
    var previous =
        new Customization(
            UUID.randomUUID(),
            "owner-a",
            CustomizationScope.PROJECT,
            List.of(),
            List.of(),
            4,
            Instant.parse("2026-09-07T20:00:00Z"));
    var create =
        new CreateCustomField(
            (owner, scope, operation) -> operation.apply(Optional.of(previous)),
            Clock.fixed(Instant.parse("2026-09-07T19:00:00Z"), ZoneOffset.UTC));
    var result =
        create.create(
            "owner-a",
            CustomizationScope.PROJECT,
            new CustomizationRevision(previous.id(), 4),
            "Otro",
            CustomFieldType.TEXT);
    assertThat(result.updatedAt()).isEqualTo(previous.updatedAt());
    assertThat(result.version()).isEqualTo(5);
  }

  @Test
  void s20_creationCannotWrapMaximumConfigurationRevision() {
    var previous =
        new Customization(
            UUID.randomUUID(),
            "owner-a",
            CustomizationScope.PROJECT,
            List.of(),
            List.of(),
            Long.MAX_VALUE,
            Instant.parse("2026-09-07T20:00:00Z"));
    var clock = mock(Clock.class);
    when(clock.instant()).thenThrow(new AssertionError("No clock at maximum revision"));
    var create =
        new CreateCustomField(
            (owner, scope, operation) -> operation.apply(Optional.of(previous)), clock);
    assertThatThrownBy(
            () ->
                create.create(
                    "owner-a",
                    CustomizationScope.PROJECT,
                    new CustomizationRevision(previous.id(), Long.MAX_VALUE),
                    "Otro",
                    CustomFieldType.TEXT))
        .isInstanceOf(StorageUnavailableException.class);
    verifyNoInteractions(clock);
  }

  @Test
  void s6_twelveDefinitionsIncludingInactivePreventAnotherCreation() {
    var fields =
        java.util.stream.IntStream.range(0, 12)
            .mapToObj(
                index ->
                    new CustomFieldDefinition(
                        UUID.randomUUID(), "Dato " + index, CustomFieldType.TEXT, false))
            .toList();
    var previous =
        new Customization(
            UUID.randomUUID(),
            "owner-a",
            CustomizationScope.PROJECT,
            List.of(),
            fields,
            4,
            Instant.parse("2026-09-07T20:00:00Z"));
    var clock = mock(Clock.class);
    when(clock.instant()).thenThrow(new AssertionError("No clock at capacity"));
    var create =
        new CreateCustomField(
            (owner, scope, operation) -> operation.apply(Optional.of(previous)), clock);
    assertThatThrownBy(
            () ->
                create.create(
                    "owner-a",
                    CustomizationScope.PROJECT,
                    new CustomizationRevision(previous.id(), 4),
                    "Otro",
                    CustomFieldType.TEXT))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error -> {
              assertThat(error.errors().getFirst().field()).isEqualTo("customFields");
              assertThat(error.errors().getFirst().code()).isEqualTo("INVALID_VALUE");
            });
    verifyNoInteractions(clock);
  }

  @Test
  void s4_duplicateInactiveLabelIsRejectedAfterCurrentRevisionWithoutClock() {
    var field = new CustomFieldDefinition(UUID.randomUUID(), "Dato", CustomFieldType.DATE, false);
    var previous =
        new Customization(
            UUID.randomUUID(),
            "owner-a",
            CustomizationScope.PROJECT,
            List.of(),
            List.of(field),
            4,
            Instant.parse("2026-09-07T20:00:00Z"));
    var clock = mock(Clock.class);
    when(clock.instant()).thenThrow(new AssertionError("No clock on invalid label"));
    var create =
        new CreateCustomField(
            (owner, scope, operation) -> operation.apply(Optional.of(previous)), clock);
    assertThatThrownBy(
            () ->
                create.create(
                    "owner-a",
                    CustomizationScope.PROJECT,
                    new CustomizationRevision(previous.id(), 4),
                    "  Dato  ",
                    CustomFieldType.TEXT))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error -> {
              assertThat(error.errors().getFirst().field()).isEqualTo("label");
              assertThat(error.errors().getFirst().code()).isEqualTo("INVALID_VALUE");
            });
    verifyNoInteractions(clock);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "\u2003\u00a0",
        "\u0000",
        "\ud800",
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
      })
  void s4_invalidLabelIsRejectedBeforeReadingConfiguration(String label) {
    var store = mock(CustomizationEditing.class);
    var create = new CreateCustomField(store, Clock.systemUTC());
    assertThatThrownBy(
            () ->
                create.create(
                    "owner-a",
                    CustomizationScope.PROJECT,
                    new CustomizationRevision(null, 0),
                    label,
                    CustomFieldType.TEXT))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error -> {
              assertThat(error.errors()).hasSize(1);
              assertThat(error.errors().getFirst().field()).isEqualTo("label");
              assertThat(error.errors().getFirst().code()).isEqualTo("INVALID_VALUE");
            });
    verifyNoInteractions(store);
  }

  @Test
  void s3_firstTaskDefinitionUsesTaskDefaults() {
    var create =
        new CreateCustomField(
            (owner, scope, operation) -> operation.apply(Optional.empty()),
            Clock.fixed(Instant.parse("2026-09-07T20:00:00Z"), ZoneOffset.UTC));
    var result =
        create.create(
            "owner-a",
            CustomizationScope.TASK,
            new CustomizationRevision(null, 0),
            "Hecho",
            CustomFieldType.BOOLEAN);
    assertThat(result.visibleFields()).containsExactly("completionCriterion", "estimatedMinutes");
    assertThat(result.scope()).isEqualTo(CustomizationScope.TASK);
  }

  @Test
  void s8_createRejectsStaleRevisionBeforeConsumingClock() {
    var previous =
        new Customization(
            UUID.randomUUID(),
            "owner-a",
            CustomizationScope.PROJECT,
            List.of(),
            List.of(),
            4,
            Instant.parse("2026-09-07T20:00:00Z"));
    var clock = mock(Clock.class);
    when(clock.instant()).thenThrow(new AssertionError("No clock after conflict"));
    var create =
        new CreateCustomField(
            (owner, scope, operation) -> operation.apply(Optional.of(previous)), clock);
    assertThatThrownBy(
            () ->
                create.create(
                    "owner-a",
                    CustomizationScope.PROJECT,
                    new CustomizationRevision(previous.id(), 3),
                    "Nuevo",
                    CustomFieldType.TEXT))
        .isInstanceOf(CustomizationConflictException.class);
    verifyNoInteractions(clock);
  }

  @Test
  void s3_appendsToExistingConfigurationWithoutReplacingItsIdentityViewOrDefinitions() {
    var previousField =
        new CustomFieldDefinition(UUID.randomUUID(), "Anterior", CustomFieldType.DATE, false);
    var previous =
        new Customization(
            UUID.randomUUID(),
            "owner-a",
            CustomizationScope.TASK,
            List.of("updatedAt"),
            List.of(previousField),
            4,
            Instant.parse("2026-09-07T20:00:00Z"));
    var create =
        new CreateCustomField(
            (owner, scope, operation) -> operation.apply(Optional.of(previous)),
            Clock.fixed(Instant.parse("2026-09-07T21:00:00Z"), ZoneOffset.UTC));
    var result =
        create.create(
            "owner-a",
            CustomizationScope.TASK,
            new CustomizationRevision(previous.id(), 4),
            "Nuevo",
            CustomFieldType.BOOLEAN);
    assertThat(result.id()).isEqualTo(previous.id());
    assertThat(result.visibleFields()).isEqualTo(previous.visibleFields());
    assertThat(result.customFields()).hasSize(2);
    assertThat(result.customFields().getFirst()).isEqualTo(previousField);
    assertThat(result.customFields().getLast().label()).isEqualTo("Nuevo");
    assertThat(result.version()).isEqualTo(5);
    assertThat(result.updatedAt()).isEqualTo("2026-09-07T21:00:00Z");
  }

  @Test
  void s3_createsAnActiveDefinitionWithProjectDefaultsAndServerIdentity() {
    var create =
        new CreateCustomField(
            (owner, scope, operation) -> {
              assertThat(owner).isEqualTo("owner-a");
              assertThat(scope).isEqualTo(CustomizationScope.PROJECT);
              return operation.apply(Optional.empty());
            },
            Clock.fixed(Instant.parse("2026-09-07T20:00:00.123456789Z"), ZoneOffset.UTC));
    var result =
        create.create(
            "owner-a",
            CustomizationScope.PROJECT,
            new CustomizationRevision(null, 0),
            "  Dato  ",
            CustomFieldType.TEXT);
    assertThat(result.id()).isNotNull();
    assertThat(result.visibleFields()).containsExactly("createdAt");
    assertThat(result.customFields()).hasSize(1);
    var field = result.customFields().getFirst();
    assertThat(field.id()).isNotNull().isNotEqualTo(result.id());
    assertThat(field.label()).isEqualTo("Dato");
    assertThat(field.type()).isEqualTo(CustomFieldType.TEXT);
    assertThat(field.active()).isTrue();
    assertThat(result.version()).isZero();
    assertThat(result.updatedAt()).isEqualTo("2026-09-07T20:00:00.123456Z");
  }
}
