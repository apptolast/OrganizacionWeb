package com.apptolast.organization.application;

import com.apptolast.organization.domain.*;
import java.time.Clock;
import java.util.UUID;

public final class UpdateCustomField implements UpdateCustomFieldUseCase {
  private final CustomizationEditing store;
  private final Clock clock;

  public UpdateCustomField(CustomizationEditing store, Clock clock) {
    this.store = store;
    this.clock = clock;
  }

  public Customization update(
      String owner,
      CustomizationScope scope,
      UUID fieldId,
      CustomizationRevision expected,
      String label,
      boolean active) {
    var normalized = new CustomFieldLabel(label).value();
    return store.change(
        owner,
        scope,
        prior -> {
          var old = prior.orElseThrow(ResourceNotFoundException::new);
          if (old.customFields().stream().noneMatch(field -> field.id().equals(fieldId)))
            throw new ResourceNotFoundException();
          if (!old.id().equals(expected.id()) || old.version() != expected.version())
            throw new CustomizationConflictException();
          if (old.customFields().stream()
              .anyMatch(field -> !field.id().equals(fieldId) && field.label().equals(normalized)))
            throw new ValidationException(
                java.util.List.of(
                    new FieldError(
                        "label", "INVALID_VALUE", "Ya existe un campo con esa etiqueta.")));
          var fields =
              old.customFields().stream()
                  .map(
                      field ->
                          field.id().equals(fieldId)
                              ? new CustomFieldDefinition(
                                  field.id(), normalized, field.type(), active)
                              : field)
                  .toList();
          if (fields.equals(old.customFields())) return old;
          if (old.version() == Long.MAX_VALUE)
            throw new StorageUnavailableException(
                new ArithmeticException("Customization revision exhausted"));
          var now = CustomizationTime.capture(clock);
          return new Customization(
              old.id(),
              owner,
              scope,
              old.visibleFields(),
              fields,
              old.version() + 1,
              now.isBefore(old.updatedAt()) ? old.updatedAt() : now);
        });
  }
}
