package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.ImportInvalidFileException;
import com.apptolast.organization.domain.AppearanceValues;
import com.apptolast.organization.domain.CustomFieldInput;
import com.apptolast.organization.domain.CustomFieldLabel;
import com.apptolast.organization.domain.CustomFieldType;
import com.apptolast.organization.domain.CustomizationScope;
import com.apptolast.organization.domain.CustomizationView;
import com.apptolast.organization.domain.ValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class ImportCustomizationValidator {
  private ImportCustomizationValidator() {}

  private static String text(JsonNode row, String name) {
    if (!row.path(name).isTextual()) throw new ImportInvalidFileException();
    return row.get(name).textValue();
  }

  public static void appearance(JsonNode row) {
    try {
      new AppearanceValues(text(row, "theme"), text(row, "accentLight"), text(row, "accentDark"));
    } catch (ValidationException invalid) {
      throw new ImportInvalidFileException();
    }
  }

  public static void configuration(JsonNode row) {
    try {
      if (!row.path("visibleFields").isArray()) throw new ImportInvalidFileException();
      var fields = new ArrayList<String>();
      for (var field : row.get("visibleFields")) {
        if (!field.isTextual()) throw new ImportInvalidFileException();
        fields.add(field.textValue());
      }
      definitions(row.path("customFields"));
      new CustomizationView(CustomizationScope.valueOf(text(row, "scope")), fields);
    } catch (IllegalArgumentException | ValidationException invalid) {
      throw new ImportInvalidFileException();
    }
  }

  private static Map<UUID, CustomFieldType> definitions(JsonNode array) {
    if (!array.isArray() || array.size() > 12) throw new ImportInvalidFileException();
    var types = new LinkedHashMap<UUID, CustomFieldType>();
    var labels = new HashSet<String>();
    for (var definition : array) {
      if (!definition.isObject()
          || definition.size() != 4
          || !definition.path("active").isBoolean()) throw new ImportInvalidFileException();
      var rawId = text(definition, "id");
      var id = UUID.fromString(rawId);
      if (!id.toString().equals(rawId)) throw new ImportInvalidFileException();
      var label = text(definition, "label");
      if (!new CustomFieldLabel(label).value().equals(label))
        throw new ImportInvalidFileException();
      if (types.containsKey(id) || !labels.add(label)) throw new ImportInvalidFileException();
      types.put(id, CustomFieldType.valueOf(text(definition, "type")));
    }
    return types;
  }

  public static void values(JsonNode values, JsonNode customFields) {
    try {
      if (!values.isArray()) throw new ImportInvalidFileException();
      var types = definitions(customFields);
      var seen = new HashSet<UUID>();
      int index = 0;
      for (var entry : values) {
        if (!entry.isObject() || entry.size() != 2 || !entry.has("value"))
          throw new ImportInvalidFileException();
        var rawId = text(entry, "fieldId");
        var id = UUID.fromString(rawId);
        if (!id.toString().equals(rawId) || !types.containsKey(id) || !seen.add(id))
          throw new ImportInvalidFileException();
        var value = entry.get("value");
        Object typed =
            value.isNull()
                ? null
                : value.isNumber()
                    ? value.decimalValue()
                    : value.isTextual()
                        ? value.textValue()
                        : value.isBoolean() ? value.booleanValue() : value;
        new CustomFieldInput(id, typed).canonical(types.get(id), index++);
      }
    } catch (IllegalArgumentException | ValidationException invalid) {
      throw new ImportInvalidFileException();
    }
  }
}
