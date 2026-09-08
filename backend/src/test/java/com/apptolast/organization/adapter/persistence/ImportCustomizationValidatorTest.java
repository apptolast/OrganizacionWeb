package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ImportCustomizationValidatorTest {
  private final ObjectMapper json = new ObjectMapper();

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "{}",
        "{\"theme\":1,\"accentLight\":\"#244c3c\",\"accentDark\":\"#b8e0c2\"}",
        "{\"theme\":\"LIGHT\",\"accentLight\":\"#ffffff\",\"accentDark\":\"#b8e0c2\"}"
      })
  void s9_invalidAppearanceHasSafeImportError(String raw) throws Exception {
    var row = json.readTree(raw);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> ImportCustomizationValidator.appearance(row))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @Test
  void s9_validAppearancePreservesHistoricalColorSpelling() throws Exception {
    var row =
        json.readTree(
            "{\"theme\":\"DARK\",\"accentLight\":\"#244c3c\",\"accentDark\":\"#b8e0c2\"}");
    var before = row.deepCopy();
    ImportCustomizationValidator.appearance(row);
    assertThat(row).isEqualTo(before);
  }

  @Test
  void s9_configurationKeepsInactiveDefinitionAndViewOrder() throws Exception {
    var row =
        json.readTree(
            "{\"scope\":\"TASK\",\"visibleFields\":[\"updatedAt\",\"completionCriterion\"],\"customFields\":[{\"id\":\"11111111-1111-4111-8111-111111111111\",\"label\":\"Campo 😀\",\"type\":\"NUMBER\",\"active\":false}]}");
    var before = row.deepCopy();
    ImportCustomizationValidator.configuration(row);
    assertThat(row).isEqualTo(before);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "{}",
        "{\"scope\":\"OTHER\",\"visibleFields\":[],\"customFields\":[]}",
        "{\"scope\":\"PROJECT\",\"visibleFields\":[\"estimatedMinutes\"],\"customFields\":[]}",
        "{\"scope\":\"TASK\",\"visibleFields\":[\"createdAt\",\"createdAt\"],\"customFields\":[]}",
        "{\"scope\":\"TASK\",\"visibleFields\":{},\"customFields\":[]}"
      })
  void s9_invalidViewUsesExistingScopeRules(String raw) throws Exception {
    var row = json.readTree(raw);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> ImportCustomizationValidator.configuration(row))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "{}",
        "[{}]",
        "[{\"id\":\"11111111-1111-4111-8111-111111111111\",\"label\":\"Nombre\",\"type\":\"OTHER\",\"active\":true}]",
        "[{\"id\":\"1-1-1-1-1\",\"label\":\"Nombre\",\"type\":\"TEXT\",\"active\":true}]",
        "[{\"id\":\"11111111-1111-4111-8111-111111111111\",\"label\":\" Nombre \",\"type\":\"TEXT\",\"active\":true}]",
        "[{\"id\":\"11111111-1111-4111-8111-111111111111\",\"label\":\"Nombre\",\"type\":\"TEXT\",\"active\":true,\"extra\":1}]"
      })
  void s9_definitionShapeAndDomainAreClosed(String definitions) throws Exception {
    var row = json.createObjectNode().put("scope", "PROJECT");
    row.putArray("visibleFields");
    row.set("customFields", json.readTree(definitions));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> ImportCustomizationValidator.configuration(row))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"id", "label", "thirteen"})
  void s9_definitionsHaveTwelveUniqueIdentitiesAndLabels(String defect) {
    var row = json.createObjectNode().put("scope", "PROJECT");
    row.putArray("visibleFields");
    var defs = row.putArray("customFields");
    for (int i = 0; i < (defect.equals("thirteen") ? 13 : 2); i++) {
      defs.addObject()
          .put("id", new java.util.UUID(0, defect.equals("id") ? 1 : i + 1).toString())
          .put("label", defect.equals("label") ? "Igual" : "Campo " + i)
          .put("type", "TEXT")
          .put("active", true);
    }
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> ImportCustomizationValidator.configuration(row))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @Test
  void s9_valuesKeepInactiveFalseZeroNullAndUnicodeWithoutInventingMissingFields() {
    var defs = json.createArrayNode();
    var values = json.createArrayNode();
    String[] types = {"TEXT", "NUMBER", "BOOLEAN", "DATE", "TEXT", "TEXT"};
    for (int i = 0; i < types.length; i++) {
      String id = new java.util.UUID(0, i + 1).toString();
      defs.addObject()
          .put("id", id)
          .put("label", "Campo " + i)
          .put("type", types[i])
          .put("active", false);
      if (i == 5) continue;
      var entry = values.addObject().put("fieldId", id);
      switch (i) {
        case 0 -> entry.put("value", "  😀texto  ");
        case 1 -> entry.put("value", new java.math.BigDecimal("0.0"));
        case 2 -> entry.put("value", false);
        case 3 -> entry.put("value", "0001-01-01");
        default -> entry.putNull("value");
      }
    }
    var before = values.deepCopy();
    ImportCustomizationValidator.values(values, defs);
    assertThat(values).isEqualTo(before);
    assertThat(values.size()).isEqualTo(5);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "{}",
        "[{}]",
        "[{\"fieldId\":\"00000000-0000-0000-0000-000000000002\",\"value\":null}]",
        "[{\"fieldId\":\"0-0-0-0-1\",\"value\":null}]",
        "[{\"fieldId\":\"00000000-0000-0000-0000-000000000001\",\"value\":null,\"extra\":1}]",
        "[{\"fieldId\":\"00000000-0000-0000-0000-000000000001\",\"value\":null},{\"fieldId\":\"00000000-0000-0000-0000-000000000001\",\"value\":null}]"
      })
  void s9_valueEntriesAreClosedUniqueAndResolveEvenWhenNull(String raw) throws Exception {
    var defs =
        json.readTree(
            "[{\"id\":\"00000000-0000-0000-0000-000000000001\",\"label\":\"Campo\",\"type\":\"TEXT\",\"active\":false}]");
    var values = json.readTree(raw);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> ImportCustomizationValidator.values(values, defs))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "NUMBER,1000000001",
    "NUMBER,1.0000000000000000000001",
    "NUMBER,'\"1\"'",
    "BOOLEAN,0",
    "DATE,'\"2025-02-29\"'",
    "TEXT,{}"
  })
  void s9_valuesRejectInvalidTypedContentWithoutRounding(String type, String rawValue)
      throws Exception {
    var defs = json.createArrayNode();
    defs.addObject()
        .put("id", new java.util.UUID(0, 1).toString())
        .put("label", "Campo")
        .put("type", type)
        .put("active", false);
    var values = json.createArrayNode();
    values
        .addObject()
        .put("fieldId", new java.util.UUID(0, 1).toString())
        .set(
            "value",
            json.reader()
                .with(
                    com.fasterxml.jackson.databind.DeserializationFeature
                        .USE_BIG_DECIMAL_FOR_FLOATS)
                .readTree(rawValue));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> ImportCustomizationValidator.values(values, defs))
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }

  @Test
  void s9_twelveDefinitionsAndTypedInclusiveBoundsRemainUnchanged() {
    var row = json.createObjectNode().put("scope", "PROJECT");
    row.putArray("visibleFields").add("updatedAt").add("createdAt");
    var defs = row.putArray("customFields");
    var values = json.createArrayNode();
    for (int i = 0; i < 12; i++) {
      var id = new java.util.UUID(0, i + 1).toString();
      defs.addObject()
          .put("id", id)
          .put("label", "😀".repeat(59) + (char) ('A' + i))
          .put("type", i < 2 ? "NUMBER" : i == 2 ? "DATE" : "TEXT")
          .put("active", i % 2 == 0);
      var entry = values.addObject().put("fieldId", id);
      if (i < 2) entry.put("value", new java.math.BigDecimal(i == 0 ? "-1000000000.0" : "1e9"));
      else if (i == 2) entry.put("value", "9999-12-31");
      else entry.put("value", i == 3 ? "😀".repeat(1000) : "");
    }
    var before = values.deepCopy();
    ImportCustomizationValidator.configuration(row);
    ImportCustomizationValidator.values(values, defs);
    assertThat(values).isEqualTo(before);
    ImportCustomizationValidator.values(json.createArrayNode(), json.createArrayNode());
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"long-label", "long-text", "nul", "surrogate"})
  void s9_invalidUnicodeOrLengthIsRejectedWithoutNormalization(String defect) {
    var row = json.createObjectNode().put("scope", "PROJECT");
    row.putArray("visibleFields");
    var defs = row.putArray("customFields");
    defs.addObject()
        .put("id", new java.util.UUID(0, 1).toString())
        .put("label", defect.equals("long-label") ? "😀".repeat(61) : "Campo")
        .put("type", "TEXT")
        .put("active", false);
    var values = json.createArrayNode();
    values
        .addObject()
        .put("fieldId", new java.util.UUID(0, 1).toString())
        .put(
            "value",
            defect.equals("nul")
                ? "\u0000"
                : defect.equals("surrogate") ? "\ud800" : "😀".repeat(1001));
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> {
              ImportCustomizationValidator.configuration(row);
              ImportCustomizationValidator.values(values, defs);
            })
        .isInstanceOf(com.apptolast.organization.application.ImportInvalidFileException.class);
  }
}
