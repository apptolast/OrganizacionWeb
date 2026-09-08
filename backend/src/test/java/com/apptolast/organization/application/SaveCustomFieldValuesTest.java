package com.apptolast.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.apptolast.organization.domain.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class SaveCustomFieldValuesTest {
  @Test
  void s22_valueChangeAdvancesToLaterClockWithMicrosecondPrecision() {
    var entity = UUID.randomUUID();
    var field = new CustomFieldDefinition(UUID.randomUUID(), "Texto", CustomFieldType.TEXT, true);
    var schema =
        new Customization(
            UUID.randomUUID(),
            "owner-a",
            CustomizationScope.PROJECT,
            List.of(),
            List.of(field),
            Long.MAX_VALUE,
            Instant.EPOCH);
    var previous =
        new CustomFieldValuesCollection(
            UUID.randomUUID(),
            Map.of(field.id(), "anterior"),
            2,
            Instant.parse("2026-09-07T20:00:00Z"));
    var clock = mock(Clock.class);
    when(clock.instant()).thenReturn(Instant.parse("2026-09-07T21:00:00.123456789Z"));
    var save =
        new SaveCustomFieldValues(
            (owner, scope, project, id, operation) ->
                CustomFieldValues.project(
                    scope,
                    id,
                    Optional.of(schema),
                    Optional.of(operation.apply(Optional.of(schema), Optional.of(previous)))),
            clock);
    var result =
        save.save(
            "owner-a",
            CustomizationScope.PROJECT,
            entity,
            entity,
            new CustomFieldValuesRevision(
                CustomizationScope.PROJECT,
                entity,
                new CustomizationRevision(schema.id(), Long.MAX_VALUE),
                new CustomizationRevision(previous.id(), 2)),
            List.of(new CustomFieldInput(field.id(), "nuevo")));
    assertThat(result.updatedAt()).isEqualTo(Instant.parse("2026-09-07T21:00:00.123456Z"));
    verify(clock).instant();
    verifyNoMoreInteractions(clock);
    assertThat(result.revision()).isEqualTo(new CustomizationRevision(previous.id(), 3));
    assertThat(result.schema()).isEqualTo(new CustomizationRevision(schema.id(), Long.MAX_VALUE));
  }

  @Test
  void s22_valueChangeKeepsMonotoneTimeAndDoesNotConsumeTheSchemaRevision() {
    var entity = UUID.randomUUID();
    var field = new CustomFieldDefinition(UUID.randomUUID(), "Texto", CustomFieldType.TEXT, true);
    var schema =
        new Customization(
            UUID.randomUUID(),
            "owner-a",
            CustomizationScope.PROJECT,
            List.of(),
            List.of(field),
            Long.MAX_VALUE,
            Instant.EPOCH);
    var previous =
        new CustomFieldValuesCollection(
            UUID.randomUUID(),
            Map.of(field.id(), "anterior"),
            2,
            Instant.parse("2026-09-07T20:00:00Z"));
    var save =
        new SaveCustomFieldValues(
            (owner, scope, project, id, operation) ->
                CustomFieldValues.project(
                    scope,
                    id,
                    Optional.of(schema),
                    Optional.of(operation.apply(Optional.of(schema), Optional.of(previous)))),
            Clock.fixed(Instant.parse("2026-09-07T19:00:00Z"), ZoneOffset.UTC));
    var result =
        save.save(
            "owner-a",
            CustomizationScope.PROJECT,
            entity,
            entity,
            new CustomFieldValuesRevision(
                CustomizationScope.PROJECT,
                entity,
                new CustomizationRevision(schema.id(), Long.MAX_VALUE),
                new CustomizationRevision(previous.id(), 2)),
            List.of(new CustomFieldInput(field.id(), "nuevo")));
    assertThat(result.updatedAt()).isEqualTo(previous.updatedAt());
    assertThat(result.revision()).isEqualTo(new CustomizationRevision(previous.id(), 3));
    assertThat(result.schema()).isEqualTo(new CustomizationRevision(schema.id(), Long.MAX_VALUE));
  }

  @Test
  void s20_changedValuesAtMaximumRevisionFailBeforeClock() {
    var entity = UUID.randomUUID();
    var field = new CustomFieldDefinition(UUID.randomUUID(), "Texto", CustomFieldType.TEXT, true);
    var schema =
        new Customization(
            UUID.randomUUID(),
            "owner-a",
            CustomizationScope.PROJECT,
            List.of(),
            List.of(field),
            4,
            Instant.EPOCH);
    var previous =
        new CustomFieldValuesCollection(
            UUID.randomUUID(), Map.of(field.id(), "anterior"), Long.MAX_VALUE, Instant.EPOCH);
    var clock = mock(Clock.class);
    when(clock.instant()).thenThrow(new AssertionError("No clock at maximum values revision"));
    var save =
        new SaveCustomFieldValues(
            (owner, scope, project, id, operation) ->
                CustomFieldValues.project(
                    scope,
                    id,
                    Optional.of(schema),
                    Optional.of(operation.apply(Optional.of(schema), Optional.of(previous)))),
            clock);
    assertThatThrownBy(
            () ->
                save.save(
                    "owner-a",
                    CustomizationScope.PROJECT,
                    entity,
                    entity,
                    new CustomFieldValuesRevision(
                        CustomizationScope.PROJECT,
                        entity,
                        new CustomizationRevision(schema.id(), 4),
                        new CustomizationRevision(previous.id(), Long.MAX_VALUE)),
                    List.of(new CustomFieldInput(field.id(), "nuevo"))))
        .isInstanceOf(StorageUnavailableException.class);
    verifyNoInteractions(clock);
  }

  @Test
  void s14_canonicalNoOpIgnoresInputOrderAndMissingNullWithoutClock() {
    var entity = UUID.randomUUID();
    var a = new CustomFieldDefinition(UUID.randomUUID(), "A", CustomFieldType.NUMBER, true);
    var b = new CustomFieldDefinition(UUID.randomUUID(), "B", CustomFieldType.TEXT, true);
    var schema =
        new Customization(
            UUID.randomUUID(),
            "owner-a",
            CustomizationScope.PROJECT,
            List.of(),
            List.of(a, b),
            4,
            Instant.EPOCH);
    var previous =
        new CustomFieldValuesCollection(UUID.randomUUID(), Map.of(a.id(), 1), 7, Instant.EPOCH);
    var clock = mock(Clock.class);
    when(clock.instant()).thenThrow(new AssertionError("No clock on canonical no-op"));
    var save =
        new SaveCustomFieldValues(
            (owner, scope, project, id, operation) -> {
              var changed = operation.apply(Optional.of(schema), Optional.of(previous));
              assertThat(changed).isSameAs(previous);
              return CustomFieldValues.project(
                  scope, id, Optional.of(schema), Optional.of(changed));
            },
            clock);
    var result =
        save.save(
            "owner-a",
            CustomizationScope.PROJECT,
            entity,
            entity,
            new CustomFieldValuesRevision(
                CustomizationScope.PROJECT,
                entity,
                new CustomizationRevision(schema.id(), 4),
                new CustomizationRevision(previous.id(), 7)),
            List.of(
                new CustomFieldInput(b.id(), ""),
                new CustomFieldInput(a.id(), new BigDecimal("1.0"))));
    assertThat(result.revision()).isEqualTo(new CustomizationRevision(previous.id(), 7));
    assertThat(result.updatedAt()).isEqualTo(Instant.EPOCH);
    verifyNoInteractions(clock);
  }

  @Test
  void s13_changingActiveValuesPreservesInactiveValuesIdentityAndDefinitionOrder() {
    var entity = UUID.randomUUID();
    var a = new CustomFieldDefinition(UUID.randomUUID(), "A", CustomFieldType.NUMBER, true);
    var b = new CustomFieldDefinition(UUID.randomUUID(), "B", CustomFieldType.TEXT, true);
    var c = new CustomFieldDefinition(UUID.randomUUID(), "C", CustomFieldType.TEXT, false);
    var schema =
        new Customization(
            UUID.randomUUID(),
            "owner-a",
            CustomizationScope.PROJECT,
            List.of(),
            List.of(a, b, c),
            4,
            Instant.EPOCH);
    var previous =
        new CustomFieldValuesCollection(
            UUID.randomUUID(),
            Map.of(a.id(), 0, b.id(), "anterior", c.id(), "preservado"),
            7,
            Instant.EPOCH);
    var save =
        new SaveCustomFieldValues(
            (owner, scope, project, id, operation) -> {
              var changed = operation.apply(Optional.of(schema), Optional.of(previous));
              assertThat(changed.id()).isEqualTo(previous.id());
              assertThat(changed.values()).containsEntry(c.id(), "preservado");
              assertThat(changed.version()).isEqualTo(8);
              return CustomFieldValues.project(
                  scope, id, Optional.of(schema), Optional.of(changed));
            },
            Clock.fixed(Instant.parse("2026-09-07T20:00:00Z"), ZoneOffset.UTC));
    var result =
        save.save(
            "owner-a",
            CustomizationScope.PROJECT,
            entity,
            entity,
            new CustomFieldValuesRevision(
                CustomizationScope.PROJECT,
                entity,
                new CustomizationRevision(schema.id(), 4),
                new CustomizationRevision(previous.id(), 7)),
            List.of(
                new CustomFieldInput(b.id(), ""),
                new CustomFieldInput(a.id(), new BigDecimal("1e3"))));
    assertThat(result.values())
        .containsExactly(
            new CustomFieldValue(a.id(), "A", CustomFieldType.NUMBER, 1000),
            new CustomFieldValue(b.id(), "B", CustomFieldType.TEXT, null));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(ints = {0, 1, 2, 3})
  void s13_onlyTheCompleteUniqueActiveSetCanBeWritten(int defect) {
    var entity = UUID.randomUUID();
    var a = new CustomFieldDefinition(UUID.randomUUID(), "A", CustomFieldType.TEXT, true);
    var b = new CustomFieldDefinition(UUID.randomUUID(), "B", CustomFieldType.TEXT, true);
    var inactive = new CustomFieldDefinition(UUID.randomUUID(), "C", CustomFieldType.TEXT, false);
    var schema =
        new Customization(
            UUID.randomUUID(),
            "owner-a",
            CustomizationScope.PROJECT,
            List.of(),
            List.of(a, b, inactive),
            4,
            Instant.EPOCH);
    var inputs = new ArrayList<CustomFieldInput>();
    inputs.add(new CustomFieldInput(a.id(), "a"));
    if (defect == 1) inputs.add(new CustomFieldInput(a.id(), "duplicado"));
    if (defect != 0) inputs.add(new CustomFieldInput(b.id(), "b"));
    if (defect >= 2)
      inputs.add(new CustomFieldInput(defect == 2 ? inactive.id() : UUID.randomUUID(), "extra"));
    var clock = mock(Clock.class);
    var save =
        new SaveCustomFieldValues(
            (owner, scope, project, id, operation) ->
                CustomFieldValues.project(
                    scope,
                    id,
                    Optional.of(schema),
                    Optional.of(operation.apply(Optional.of(schema), Optional.empty()))),
            clock);
    assertThatThrownBy(
            () ->
                save.save(
                    "owner-a",
                    CustomizationScope.PROJECT,
                    entity,
                    entity,
                    new CustomFieldValuesRevision(
                        CustomizationScope.PROJECT,
                        entity,
                        new CustomizationRevision(schema.id(), 4),
                        new CustomizationRevision(null, 0)),
                    inputs))
        .isInstanceOfSatisfying(
            ValidationException.class,
            error -> {
              assertThat(error.errors().getFirst().field())
                  .isEqualTo(
                      defect == 0
                          ? "values"
                          : defect == 1 ? "values[1].fieldId" : "values[2].fieldId");
              assertThat(error.errors().getFirst().code()).isEqualTo("INVALID_VALUE");
            });
    verifyNoInteractions(clock);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"scope", "entity", "schema-id", "schema-version", "values-id", "values-version"})
  void s15_everyCompositeRevisionComponentPrecedesTypedValidation(String changed) {
    var entity = UUID.randomUUID();
    var field = new CustomFieldDefinition(UUID.randomUUID(), "Texto", CustomFieldType.TEXT, true);
    var schema =
        new Customization(
            UUID.randomUUID(),
            "owner-a",
            CustomizationScope.PROJECT,
            List.of(),
            List.of(field),
            4,
            Instant.EPOCH);
    var previous =
        new CustomFieldValuesCollection(
            UUID.randomUUID(), Map.of(field.id(), "Dato"), 7, Instant.EPOCH);
    var expected =
        new CustomFieldValuesRevision(
            changed.equals("scope") ? CustomizationScope.TASK : CustomizationScope.PROJECT,
            changed.equals("entity") ? UUID.randomUUID() : entity,
            new CustomizationRevision(
                changed.equals("schema-id") ? UUID.randomUUID() : schema.id(),
                changed.equals("schema-version") ? 3 : 4),
            new CustomizationRevision(
                changed.equals("values-id") ? UUID.randomUUID() : previous.id(),
                changed.equals("values-version") ? 6 : 7));
    var clock = mock(Clock.class);
    var save =
        new SaveCustomFieldValues(
            (owner, scope, project, id, operation) ->
                CustomFieldValues.project(
                    scope,
                    id,
                    Optional.of(schema),
                    Optional.of(operation.apply(Optional.of(schema), Optional.of(previous)))),
            clock);
    assertThatThrownBy(
            () ->
                save.save(
                    "owner-a",
                    CustomizationScope.PROJECT,
                    entity,
                    entity,
                    expected,
                    List.of(new CustomFieldInput(field.id(), BigDecimal.ONE))))
        .isInstanceOf(CustomizationConflictException.class);
    verifyNoInteractions(clock);
  }

  @Test
  void s11_firstValuesWriteCanonicalizesNumbersAtRevisionZeroWithSameSchema() {
    var entity = UUID.randomUUID();
    var field =
        new CustomFieldDefinition(UUID.randomUUID(), "Numero", CustomFieldType.NUMBER, true);
    var schema =
        new Customization(
            UUID.randomUUID(),
            "owner-a",
            CustomizationScope.PROJECT,
            List.of(),
            List.of(field),
            4,
            Instant.EPOCH);
    var save =
        new SaveCustomFieldValues(
            (owner, scope, project, id, operation) -> {
              assertThat(owner).isEqualTo("owner-a");
              assertThat(scope).isEqualTo(CustomizationScope.PROJECT);
              assertThat(project).isEqualTo(entity);
              assertThat(id).isEqualTo(entity);
              var result = operation.apply(Optional.of(schema), Optional.empty());
              assertThat(result.id()).isNotNull();
              assertThat(result.values()).containsEntry(field.id(), 1);
              assertThat(result.version()).isZero();
              return CustomFieldValues.project(scope, id, Optional.of(schema), Optional.of(result));
            },
            Clock.fixed(Instant.parse("2026-09-07T20:00:00.123456789Z"), ZoneOffset.UTC));
    var result =
        save.save(
            "owner-a",
            CustomizationScope.PROJECT,
            entity,
            entity,
            new CustomFieldValuesRevision(
                CustomizationScope.PROJECT,
                entity,
                new CustomizationRevision(schema.id(), 4),
                new CustomizationRevision(null, 0)),
            List.of(new CustomFieldInput(field.id(), new BigDecimal("1.0"))));
    assertThat(result.values())
        .containsExactly(new CustomFieldValue(field.id(), "Numero", CustomFieldType.NUMBER, 1));
    assertThat(result.schema()).isEqualTo(new CustomizationRevision(schema.id(), 4));
    assertThat(result.updatedAt()).isEqualTo("2026-09-07T20:00:00.123456Z");
  }
}
