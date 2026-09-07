package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CreateCustomField implements CreateCustomFieldUseCase {
  private final CustomizationEditing store;
  private final Clock clock;

  public CreateCustomField(CustomizationEditing store, Clock clock) {
    this.store = store;
    this.clock = clock;
  }

  public Customization create(
      String owner,
      CustomizationScope scope,
      CustomizationRevision expected,
      String label,
      CustomFieldType type) {
    String normalized = new CustomFieldLabel(label).value();
    return store.change(
        owner,
        scope,
        prior -> {
          if (prior.isEmpty()
              ? expected.id() != null
              : !prior.get().id().equals(expected.id())
                  || prior.get().version() != expected.version())
            throw new CustomizationConflictException();
          if (prior.stream()
              .flatMap(value -> value.customFields().stream())
              .anyMatch(field -> field.label().equals(normalized)))
            throw new ValidationException(
                List.of(
                    new FieldError(
                        "label", "INVALID_VALUE", "Ya existe un campo con esa etiqueta.")));
          if (prior.isPresent() && prior.get().customFields().size() >= 12)
            throw new ValidationException(
                List.of(
                    new FieldError(
                        "customFields",
                        "INVALID_VALUE",
                        "Se permiten hasta 12 campos por ámbito.")));
          if (prior.isPresent() && prior.get().version() == Long.MAX_VALUE)
            throw new StorageUnavailableException(
                new ArithmeticException("Customization revision exhausted"));
          var fields =
              new ArrayList<CustomFieldDefinition>(
                  prior.map(Customization::customFields).orElse(List.of()));
          fields.add(new CustomFieldDefinition(UUID.randomUUID(), normalized, type, true));
          var now = CustomizationTime.capture(clock);
          var updated =
              prior
                  .filter(value -> value.updatedAt().isAfter(now))
                  .map(Customization::updatedAt)
                  .orElse(now);
          return new Customization(
              prior.map(Customization::id).orElseGet(UUID::randomUUID),
              owner,
              scope,
              prior
                  .map(Customization::visibleFields)
                  .orElse(
                      scope == CustomizationScope.PROJECT
                          ? List.of("createdAt")
                          : List.of("completionCriterion", "estimatedMinutes")),
              List.copyOf(fields),
              prior.map(value -> value.version() + 1).orElse(0L),
              updated);
        });
  }
}
