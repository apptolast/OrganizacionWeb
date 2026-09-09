package com.apptolast.organization.support;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * El único punto por el que las pruebas vacían la base entre escenarios.
 *
 * <p>No enumera tablas a propósito: las descubre en el catálogo. Enumerarlas a mano fue lo que
 * tumbó la CI —la migración V25 añadió {@code task_external_links}, que apunta a {@code tasks}, y
 * ninguna de las listas escritas a mano la nombraba, así que PostgreSQL rechazaba el {@code
 * TRUNCATE} entero—, y volvería a tumbarla con cada migración que traiga una clave ajena nueva.
 */
public final class TestDatabase {
  private static final String FLYWAY_HISTORY = "flyway_schema_history";
  private static final String APPLICATION_TABLES =
      """
      SELECT quote_ident(table_name) FROM information_schema.tables
      WHERE table_schema='public' AND table_type='BASE TABLE' AND table_name<>?
      """;

  private TestDatabase() {}

  /** Deja vacías todas las tablas de la aplicación, y sólo esas: el historial de Flyway sigue. */
  public static void empty(JdbcTemplate jdbc) {
    jdbc.execute(emptyStatement(jdbc));
  }

  private static String emptyStatement(JdbcTemplate jdbc) {
    var tables = jdbc.queryForList(APPLICATION_TABLES, String.class, FLYWAY_HISTORY);
    if (tables.isEmpty())
      throw new IllegalStateException("El esquema no tiene tablas: ¿faltan las migraciones?");
    return "TRUNCATE " + String.join(",", tables) + " RESTART IDENTITY CASCADE";
  }
}
