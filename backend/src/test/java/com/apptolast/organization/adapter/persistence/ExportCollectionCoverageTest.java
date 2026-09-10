package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.apptolast.organization.application.StorageUnavailableException;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * @s1 @s2 ninguna colección del archivo puede resolverse sin consultar su tabla. El peor fallo de
 *     una feature cuyo propósito es «dame todos mis datos» es entregar un archivo que parece
 *     completo y no lo está: una colección sin consulta se escribiría como array vacío con count 0
 *     y la respuesta seguiría siendo 200. La guarda del conjunto de claves
 *     (ExportCollectionSetTest) no lo ve, porque la clave sí estaría.
 *     <p>Se construye sobre un DataSource sin driver: así, tocar la base de datos es observable
 *     como fallo inmediato y distinguible de la ruta que no la toca.
 */
class ExportCollectionCoverageTest {
  private static List<String> section22Collections() {
    return List.of(
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
  }

  @Test
  void s1_anUnknownCollectionFailsLoudlyInsteadOfExportingAnEmptyArray() {
    assertThat(catchThrowable(() -> writeCollection("connectorConnections")))
        .as("una colección sin consulta no puede pasar por exportada")
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("connectorConnections");
  }

  @ParameterizedTest
  @MethodSource("section22Collections")
  void s2_everyCollectionOfSection22ReachesItsQuery(String collection) {
    assertThat(catchThrowable(() -> writeCollection(collection)))
        .as("%s debe consultar su tabla, no resolverse en silencio sin base de datos", collection)
        .isInstanceOfAny(CannotGetJdbcConnectionException.class, StorageUnavailableException.class);
  }

  private static void writeCollection(String collection) throws Exception {
    var withoutDriver = new DriverManagerDataSource("jdbc:no-such-driver:export-guard");
    var queries =
        new PostgresExportDataQueries(
            new JdbcTemplate(withoutDriver), new DataSourceTransactionManager(withoutDriver));
    try (JsonGenerator json = new JsonFactory().createGenerator(new ByteArrayOutputStream())) {
      json.writeStartArray();
      queries.writeCollection("owner-a", collection, json);
    }
  }
}
