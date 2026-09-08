package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.PreparedExport;
import com.fasterxml.jackson.core.JsonFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ExportJsonWriter {
  private static final List<String> COLLECTIONS = List.of("projects", "tasks", "taskStatusHistory", "availability", "plannedBlocks",
      "blockProjections", "blockChanges", "workSessions", "workSessionIntervals", "workSessionChanges",
      "appearance", "customization", "projectCustomFieldValues", "taskCustomFieldValues");

  public PreparedExport empty(String owner, Instant instant) throws IOException {
    var buffer = new ByteArrayOutputStream();
    try (var json = new JsonFactory().createGenerator(buffer)) {
      json.writeStartObject();
      json.writeStringField("format", "organizationweb-export");
      json.writeNumberField("schemaVersion", 1);
      json.writeStringField("exportedAt", DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSS'Z'").withZone(ZoneOffset.UTC).format(instant));
      json.writeStringField("owner", owner);
      json.writeObjectFieldStart("data");
      for (var collection : COLLECTIONS) { json.writeArrayFieldStart(collection); json.writeEndArray(); }
      json.writeEndObject();
      json.writeObjectFieldStart("counts");
      for (var collection : COLLECTIONS) json.writeNumberField(collection, 0);
      json.writeEndObject();
      json.writeEndObject();
    }
    var filename = "organizationweb-export-v1-" + DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmssSSSSSS'Z'").withZone(ZoneOffset.UTC).format(instant) + ".json";
    return new PreparedExport() {
      public String filename() { return filename; }
      public long contentLength() { return buffer.size(); }
      public void writeTo(OutputStream output) throws IOException { buffer.writeTo(output); }
    };
  }
}