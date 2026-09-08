package com.apptolast.organization.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CustomFieldValues(
    UUID entityId,
    CustomizationScope scope,
    CustomizationRevision schema,
    CustomizationRevision revision,
    List<CustomFieldValue> values,
    Instant updatedAt) {
  public static CustomFieldValues project(
      CustomizationScope scope,
      UUID entityId,
      java.util.Optional<Customization> configuration,
      java.util.Optional<CustomFieldValuesCollection> collection) {
    var fields =
        configuration.map(Customization::customFields).orElse(List.of()).stream()
            .filter(CustomFieldDefinition::active)
            .map(
                field ->
                    new CustomFieldValue(
                        field.id(),
                        field.label(),
                        field.type(),
                        collection.map(value -> value.values().get(field.id())).orElse(null)))
            .toList();
    return new CustomFieldValues(
        entityId,
        scope,
        configuration
            .map(value -> new CustomizationRevision(value.id(), value.version()))
            .orElse(new CustomizationRevision(null, 0)),
        collection
            .map(value -> new CustomizationRevision(value.id(), value.version()))
            .orElse(new CustomizationRevision(null, 0)),
        fields,
        collection.map(CustomFieldValuesCollection::updatedAt).orElse(null));
  }
}
