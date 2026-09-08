package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.time.Clock;
import java.util.*;

public final class SaveCustomFieldValues implements SaveCustomFieldValuesUseCase {
  private final CustomFieldValuesEditing store;
  private final Clock clock;

  public SaveCustomFieldValues(CustomFieldValuesEditing store, Clock clock) {
    this.store = store;
    this.clock = clock;
  }

  public CustomFieldValues save(
      String owner,
      CustomizationScope scope,
      UUID projectId,
      UUID entityId,
      CustomFieldValuesRevision expected,
      List<CustomFieldInput> values) {
    return store.changeValues(
        owner,
        scope,
        projectId,
        entityId,
        (configuration, previous) -> {
          var current =
              new CustomFieldValuesRevision(
                  scope,
                  entityId,
                  configuration
                      .map(value -> new CustomizationRevision(value.id(), value.version()))
                      .orElse(new CustomizationRevision(null, 0)),
                  previous
                      .map(value -> new CustomizationRevision(value.id(), value.version()))
                      .orElse(new CustomizationRevision(null, 0)));
          if (!current.equals(expected)) throw new CustomizationConflictException();
          var active =
              configuration.map(Customization::customFields).orElse(List.of()).stream()
                  .filter(CustomFieldDefinition::active)
                  .toList();
          var canonical = new LinkedHashMap<UUID, Object>();
          for (int index = 0; index < values.size(); index++) {
            var input = values.get(index);
            var found =
                active.stream()
                    .filter(definition -> definition.id().equals(input.fieldId()))
                    .findFirst();
            if (found.isEmpty() || canonical.containsKey(input.fieldId()))
              throw new ValidationException(
                  List.of(
                      new FieldError(
                          "values[" + index + "].fieldId",
                          "INVALID_VALUE",
                          "Selecciona cada campo activo una sola vez.")));
            var field = found.get();
            canonical.put(input.fieldId(), input.canonical(field.type(), index));
          }
          if (canonical.size() != active.size())
            throw new ValidationException(
                List.of(
                    new FieldError(
                        "values", "INVALID_VALUE", "Incluye todos los campos activos.")));
          if (previous.isPresent()
              && canonical.entrySet().stream()
                  .allMatch(
                      entry ->
                          Objects.equals(
                              previous.get().values().get(entry.getKey()), entry.getValue())))
            return previous.get();
          if (previous.isPresent() && previous.get().version() == Long.MAX_VALUE)
            throw new StorageUnavailableException(
                new ArithmeticException("Customization values revision exhausted"));
          var merged =
              new LinkedHashMap<UUID, Object>(
                  previous.map(CustomFieldValuesCollection::values).orElse(Map.of()));
          merged.putAll(canonical);
          var now = CustomizationTime.capture(clock);
          var updated =
              previous
                  .filter(value -> value.updatedAt().isAfter(now))
                  .map(CustomFieldValuesCollection::updatedAt)
                  .orElse(now);
          return new CustomFieldValuesCollection(
              previous.map(CustomFieldValuesCollection::id).orElseGet(UUID::randomUUID),
              merged,
              previous.map(value -> value.version() + 1).orElse(0L),
              updated);
        });
  }
}
