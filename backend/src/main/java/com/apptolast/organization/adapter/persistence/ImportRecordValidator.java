package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.ImportInvalidFileException;
import com.apptolast.organization.domain.Project;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

final class ImportRecordValidator {
  private ImportRecordValidator() {}

  static void project(JsonNode row, String owner) {
    try {
      var name = text(row, "name");
      if (name.codePointCount(0, name.length()) > 120) throw new ImportInvalidFileException();
      new Project(
          UUID.fromString(text(row, "id")),
          owner,
          text(row, "name"),
          text(row, "description"),
          text(row, "status"),
          Instant.parse(text(row, "createdAt")),
          Instant.parse(text(row, "updatedAt")));
    } catch (IllegalArgumentException
        | java.time.DateTimeException
        | com.apptolast.organization.domain.ValidationException invalid) {
      throw new ImportInvalidFileException();
    }
  }

  private static String text(JsonNode row, String field) {
    var value = row.get(field);
    if (value == null || !value.isTextual()) throw new ImportInvalidFileException();
    return value.textValue();
  }
}
