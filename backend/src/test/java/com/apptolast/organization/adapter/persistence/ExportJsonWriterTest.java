package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ExportJsonWriterTest {
  @Test
  void s1_emptyAccountProducesTheClosedDocumentAndExactDownloadMetadata() throws Exception {
    var file =
        new ExportJsonWriter().empty("dueño", Instant.parse("2026-09-08T01:02:03.123456789Z"));
    var output = new ByteArrayOutputStream();
    file.writeTo(output);
    var bytes = output.toByteArray();
    assertThat(file.contentLength()).isEqualTo(bytes.length);
    assertThat(file.filename()).isEqualTo("organizationweb-export-v1-20260908T010203123456Z.json");
    var json = new ObjectMapper().readTree(bytes);
    assertThat(json.properties())
        .extracting(java.util.Map.Entry::getKey)
        .containsExactly("format", "schemaVersion", "exportedAt", "owner", "data", "counts");
    assertThat(json.path("format").asText()).isEqualTo("organizationweb-export");
    assertThat(json.path("schemaVersion").intValue()).isEqualTo(1);
    assertThat(json.path("exportedAt").asText()).isEqualTo("2026-09-08T01:02:03.123456Z");
    assertThat(json.path("owner").asText()).isEqualTo("dueño");
    var names =
        java.util.List.of(
            "projects",
            "tasks",
            "taskStatusHistory",
            "availability",
            "plannedBlocks",
            "blockProjections",
            "blockChanges",
            "workSessions",
            "workSessionIntervals",
            "workSessionChanges",
            "appearance",
            "customization",
            "projectCustomFieldValues",
            "taskCustomFieldValues");
    assertThat(json.path("data").properties())
        .extracting(java.util.Map.Entry::getKey)
        .containsExactlyElementsOf(names);
    assertThat(json.path("counts").properties())
        .extracting(java.util.Map.Entry::getKey)
        .containsExactlyElementsOf(names);
    for (var name : names) {
      assertThat(json.path("data").path(name).isArray()).isTrue();
      assertThat(json.path("data").path(name).size()).isZero();
      assertThat(json.path("counts").path(name).isIntegralNumber()).isTrue();
      assertThat(json.path("counts").path(name).intValue()).isZero();
    }
    assertThat(bytes[0]).isEqualTo((byte) '{');
  }
}
