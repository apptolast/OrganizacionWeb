package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @s34 la exportación JSON v1 conserva catorce colecciones y ninguna de ellas es la conexión ni los
 *     enlaces. El conector no puede ampliar el archivo por la puerta de atrás: si alguien añadiera
 *     sus tablas al volcado, el texto cifrado del token acabaría en un fichero que la persona se
 *     descarga.
 */
class ConnectorExportExposureTest {
  private static final List<String> EXPECTED =
      List.of(
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

  @Test
  void s34_theExportStillCarriesTheSameFourteenCollections() throws Exception {
    assertThat(collectionsOfAnEmptyExport()).isEqualTo(EXPECTED).hasSize(14);
  }

  @Test
  void s34_noExportedCollectionBelongsToTheConnector() throws Exception {
    assertThat(collectionsOfAnEmptyExport())
        .noneMatch(name -> name.toLowerCase(java.util.Locale.ROOT).contains("connect"))
        .noneMatch(name -> name.toLowerCase(java.util.Locale.ROOT).contains("token"))
        .noneMatch(name -> name.toLowerCase(java.util.Locale.ROOT).contains("link"))
        .noneMatch(name -> name.toLowerCase(java.util.Locale.ROOT).contains("github"))
        .noneMatch(name -> name.toLowerCase(java.util.Locale.ROOT).contains("import"));
  }

  private static List<String> collectionsOfAnEmptyExport() throws Exception {
    var prepared =
        new ExportJsonWriter().empty("owner-a", Instant.parse("2026-09-09T12:00:00.123456Z"));
    var bytes = new ByteArrayOutputStream();
    prepared.writeTo(bytes);
    var document = new ObjectMapper().readTree(bytes.toByteArray());
    var names = new java.util.ArrayList<String>();
    document.get("data").fieldNames().forEachRemaining(names::add);
    return names;
  }
}
