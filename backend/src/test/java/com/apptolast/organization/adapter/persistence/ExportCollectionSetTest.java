package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @s1 @s33 el archivo lleva exactamente las colecciones de la sección 22, ni una menos ni una más.
 *     Se compara el conjunto y no su tamaño porque una omisión compensada con una añadidura deja el
 *     número intacto: quien pide sus datos se los llevaría incompletos, o con una tabla privada
 *     dentro, sin que la cuenta lo delatara. Guarda reinjertada tras perderse con la retirada de la
 *     feature 27, donde vivía como ConnectorExportExposureTest.
 */
class ExportCollectionSetTest {
  private static final int COLLECTIONS_IN_SECTION_22 = 14;

  private static final List<String> SECTION_22_COLLECTIONS =
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
  void s1_dataCarriesExactlyTheCollectionsOfSection22() throws Exception {
    assertThat(keysOf("data"))
        .as("colecciones exportadas en data frente a la sección 22")
        .containsExactlyInAnyOrderElementsOf(SECTION_22_COLLECTIONS)
        .hasSize(COLLECTIONS_IN_SECTION_22);
  }

  @Test
  void s1_countsCarriesExactlyTheSameCollectionsAsData() throws Exception {
    assertThat(keysOf("counts"))
        .as("claves de counts frente a las colecciones de data")
        .containsExactlyInAnyOrderElementsOf(keysOf("data"));
  }

  private static List<String> keysOf(String envelopeField) throws Exception {
    var prepared =
        new ExportJsonWriter().empty("dueño", Instant.parse("2026-09-09T12:00:00.123456Z"));
    var bytes = new ByteArrayOutputStream();
    prepared.writeTo(bytes);
    var document = new ObjectMapper().readTree(bytes.toByteArray());
    var keys = new ArrayList<String>();
    document.get(envelopeField).fieldNames().forEachRemaining(keys::add);
    return keys;
  }
}
