package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.*;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers
class AppearancePersistenceTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  static JdbcTemplate jdbc;
  static DataSourceTransactionManager manager;

  @Test
  void s35_lastPublicMicrosecondCanBeCommittedAndReadByANewStore() {
    var last = java.time.Instant.parse("9999-12-31T23:59:59.999999Z");
    var save =
        new com.apptolast.organization.application.SaveAppearance(
            new PostgresAppearanceStore(jdbc, new TransactionTemplate(manager)),
            java.time.Clock.fixed(last, java.time.ZoneOffset.UTC));
    var saved =
        save.execute(
            "last-year-owner",
            new com.apptolast.organization.domain.AppearanceRevision(null, 0),
            "SYSTEM",
            "#244C3C",
            "#B7E4C7");
    assertThat(saved.updatedAt()).isEqualTo(last);
    assertThat(
            new PostgresAppearanceStore(jdbc, new TransactionTemplate(manager))
                .find("last-year-owner"))
        .contains(saved);
    assertThat(
            jdbc.queryForObject(
                    "SELECT updated_at FROM appearance_preferences WHERE owner_id=?",
                    java.time.OffsetDateTime.class,
                    "last-year-owner")
                .toInstant())
        .isEqualTo(last);
  }

  @BeforeAll
  static void database() {
    var source =
        new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    Flyway.configure().dataSource(source).load().migrate();
    jdbc = new JdbcTemplate(source);
    manager = new DataSourceTransactionManager(source);
  }

  @Test
  void s1_absenceIsReadWithoutInsertingPreferences() {
    var store = new PostgresAppearanceStore(jdbc, new TransactionTemplate(manager));
    assertThat(store.find("absent-owner")).isEmpty();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM appearance_preferences", Integer.class))
        .isZero();
  }

  @Test
  void s2_s8_s9_committedPreferenceSurvivesANewStoreAndDoesNotWriteAnEvent() {
    var store = new PostgresAppearanceStore(jdbc, new TransactionTemplate(manager));
    var save =
        new com.apptolast.organization.application.SaveAppearance(
            store,
            java.time.Clock.fixed(
                java.time.Instant.parse("0001-01-01T00:00:00.123456Z"), java.time.ZoneOffset.UTC));
    int events = jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class);
    var saved =
        save.execute(
            "durable-owner",
            new com.apptolast.organization.domain.AppearanceRevision(null, 0),
            "DARK",
            "#0000FF",
            "#00FFFF");
    var reloaded = new PostgresAppearanceStore(jdbc, new TransactionTemplate(manager));
    assertThat(reloaded.find("durable-owner")).contains(saved);
    assertThat(reloaded.find("other-owner")).isEmpty();
    assertThat(saved.updatedAt()).isEqualTo(java.time.Instant.parse("0001-01-01T00:00:00.123456Z"));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class))
        .isEqualTo(events);
  }

  @BeforeEach
  void clean() {
    jdbc.execute("TRUNCATE appearance_preferences");
  }

  @Test
  void s2_updatesTheExistingRowAndConfirmsExactlyOneRevision() {
    var store = new PostgresAppearanceStore(jdbc, new TransactionTemplate(manager));
    var save =
        new com.apptolast.organization.application.SaveAppearance(
            store, java.time.Clock.fixed(java.time.Instant.EPOCH, java.time.ZoneOffset.UTC));
    var first =
        save.execute(
            "owner",
            new com.apptolast.organization.domain.AppearanceRevision(null, 0),
            "SYSTEM",
            "#244C3C",
            "#B7E4C7");
    var changed =
        save.execute(
            "owner",
            new com.apptolast.organization.domain.AppearanceRevision(first.id(), 0),
            "DARK",
            "#0000FF",
            "#00FFFF");
    assertThat(store.find("owner")).contains(changed);
    assertThat(changed.id()).isEqualTo(first.id());
    assertThat(changed.version()).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM appearance_preferences", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void s3_noOpDoesNotIssueAnUpdate() {
    var store = new PostgresAppearanceStore(jdbc, new TransactionTemplate(manager));
    var save =
        new com.apptolast.organization.application.SaveAppearance(
            store, java.time.Clock.fixed(java.time.Instant.EPOCH, java.time.ZoneOffset.UTC));
    var first =
        save.execute(
            "owner",
            new com.apptolast.organization.domain.AppearanceRevision(null, 0),
            "SYSTEM",
            "#244C3C",
            "#B7E4C7");
    jdbc.execute(
        "CREATE FUNCTION appearance_forbid_update() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'No UPDATE expected'; END $$");
    jdbc.execute(
        "CREATE TRIGGER appearance_no_update BEFORE UPDATE ON appearance_preferences FOR EACH ROW EXECUTE FUNCTION appearance_forbid_update()");
    try {
      assertThat(
              save.execute(
                  "owner",
                  new com.apptolast.organization.domain.AppearanceRevision(first.id(), 0),
                  "SYSTEM",
                  "#244c3c",
                  "#b7e4c7"))
          .isEqualTo(first);
    } finally {
      jdbc.execute("DROP TRIGGER appearance_no_update ON appearance_preferences");
      jdbc.execute("DROP FUNCTION appearance_forbid_update()");
    }
  }

  @Test
  void s16_readFailureIsNotConfirmedAbsence() {
    var store = new PostgresAppearanceStore(jdbc, new TransactionTemplate(manager));
    jdbc.execute("ALTER TABLE appearance_preferences RENAME TO appearance_unavailable");
    try {
      assertThatThrownBy(() -> store.find("owner"))
          .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class);
    } finally {
      jdbc.execute("ALTER TABLE appearance_unavailable RENAME TO appearance_preferences");
    }
  }

  @Test
  void s35_incoherentPersistedColorsDoNotBecomeAPublicPreference() {
    jdbc.update(
        "INSERT INTO appearance_preferences VALUES (?, 'owner', 'LIGHT', '#FFFFFF', '#00FFFF', 0, '2026-09-07 12:00Z')",
        java.util.UUID.randomUUID());
    var store = new PostgresAppearanceStore(jdbc, new TransactionTemplate(manager));
    assertThatThrownBy(() -> store.find("owner"))
        .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class);
  }

  @Test
  void s35_storedTimestampOutsidePublicRangeIsUnavailable() {
    jdbc.update(
        "INSERT INTO appearance_preferences VALUES (?, 'owner', 'LIGHT', '#0000FF', '#00FFFF', 0, '10000-01-01 00:00Z')",
        java.util.UUID.randomUUID());
    var store = new PostgresAppearanceStore(jdbc, new TransactionTemplate(manager));
    assertThatThrownBy(() -> store.find("owner"))
        .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class);
  }

  @Test
  void s7_suppressedUpdateCannotBeReturnedAsCommitted() {
    var store = new PostgresAppearanceStore(jdbc, new TransactionTemplate(manager));
    var save =
        new com.apptolast.organization.application.SaveAppearance(
            store, java.time.Clock.fixed(java.time.Instant.EPOCH, java.time.ZoneOffset.UTC));
    var first =
        save.execute(
            "owner",
            new com.apptolast.organization.domain.AppearanceRevision(null, 0),
            "SYSTEM",
            "#244C3C",
            "#B7E4C7");
    jdbc.execute(
        "CREATE FUNCTION appearance_suppress() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RETURN NULL; END $$");
    jdbc.execute(
        "CREATE TRIGGER appearance_suppress BEFORE UPDATE ON appearance_preferences FOR EACH ROW EXECUTE FUNCTION appearance_suppress()");
    try {
      assertThatThrownBy(
              () ->
                  save.execute(
                      "owner",
                      new com.apptolast.organization.domain.AppearanceRevision(first.id(), 0),
                      "DARK",
                      "#0000FF",
                      "#00FFFF"))
          .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class);
      assertThat(store.find("owner")).contains(first);
    } finally {
      jdbc.execute("DROP TRIGGER appearance_suppress ON appearance_preferences");
      jdbc.execute("DROP FUNCTION appearance_suppress()");
    }
  }

  @Test
  void s7_deferredCommitFailureRollsBackTheWholePreference() {
    var store = new PostgresAppearanceStore(jdbc, new TransactionTemplate(manager));
    var save =
        new com.apptolast.organization.application.SaveAppearance(
            store, java.time.Clock.fixed(java.time.Instant.EPOCH, java.time.ZoneOffset.UTC));
    jdbc.execute(
        "CREATE FUNCTION appearance_fail_commit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'Commit rejected'; END $$");
    jdbc.execute(
        "CREATE CONSTRAINT TRIGGER appearance_fail_commit AFTER INSERT ON appearance_preferences DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION appearance_fail_commit()");
    try {
      assertThatThrownBy(
              () ->
                  save.execute(
                      "owner",
                      new com.apptolast.organization.domain.AppearanceRevision(null, 0),
                      "SYSTEM",
                      "#244C3C",
                      "#B7E4C7"))
          .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class);
      assertThat(store.find("owner")).isEmpty();
    } finally {
      jdbc.execute("DROP TRIGGER appearance_fail_commit ON appearance_preferences");
      jdbc.execute("DROP FUNCTION appearance_fail_commit()");
    }
  }

  @Test
  void s5_twoCreationsFromAbsenceConfirmOneWinnerAndOneConflict() throws Exception {
    var entered = new java.util.concurrent.CountDownLatch(2);
    var store = new PostgresAppearanceStore(jdbc, new TransactionTemplate(manager));
    com.apptolast.organization.application.AppearanceEditing simultaneous =
        (owner, operation) ->
            store.save(
                owner,
                prior -> {
                  assertThat(prior).isEmpty();
                  entered.countDown();
                  try {
                    assertThat(entered.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                  } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(error);
                  }
                  return operation.apply(prior);
                });
    var save =
        new com.apptolast.organization.application.SaveAppearance(
            simultaneous, java.time.Clock.fixed(java.time.Instant.EPOCH, java.time.ZoneOffset.UTC));
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      java.util.concurrent.Callable<Object> write =
          () -> {
            try {
              return save.execute(
                  "racing-owner",
                  new com.apptolast.organization.domain.AppearanceRevision(null, 0),
                  "SYSTEM",
                  "#244C3C",
                  "#B7E4C7");
            } catch (RuntimeException error) {
              return error;
            }
          };
      var first = executor.submit(write);
      var second = executor.submit(write);
      var outcomes =
          java.util.List.of(
              first.get(10, java.util.concurrent.TimeUnit.SECONDS),
              second.get(10, java.util.concurrent.TimeUnit.SECONDS));
      assertThat(
              outcomes.stream()
                  .filter(com.apptolast.organization.domain.Appearance.class::isInstance)
                  .count())
          .isEqualTo(1);
      assertThat(
              outcomes.stream()
                  .filter(
                      com.apptolast.organization.application.AppearanceConflictException.class
                          ::isInstance)
                  .count())
          .isEqualTo(1);
      assertThat(jdbc.queryForObject("SELECT count(*) FROM appearance_preferences", Integer.class))
          .isEqualTo(1);
      assertThat(store.find("racing-owner"))
          .contains(
              (com.apptolast.organization.domain.Appearance)
                  outcomes.stream()
                      .filter(com.apptolast.organization.domain.Appearance.class::isInstance)
                      .findFirst()
                      .orElseThrow());
    }
  }

  @Test
  void s7_suppressedInsertWithoutDurableWinnerIsUnavailableNotConflict() {
    var store = new PostgresAppearanceStore(jdbc, new TransactionTemplate(manager));
    var save =
        new com.apptolast.organization.application.SaveAppearance(
            store, java.time.Clock.fixed(java.time.Instant.EPOCH, java.time.ZoneOffset.UTC));
    jdbc.execute(
        "CREATE FUNCTION appearance_suppress_insert() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RETURN NULL; END $$");
    jdbc.execute(
        "CREATE TRIGGER appearance_suppress_insert BEFORE INSERT ON appearance_preferences FOR EACH ROW EXECUTE FUNCTION appearance_suppress_insert()");
    try {
      assertThatThrownBy(
              () ->
                  save.execute(
                      "owner",
                      new com.apptolast.organization.domain.AppearanceRevision(null, 0),
                      "SYSTEM",
                      "#244C3C",
                      "#B7E4C7"))
          .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class);
      assertThat(store.find("owner")).isEmpty();
    } finally {
      jdbc.execute("DROP TRIGGER appearance_suppress_insert ON appearance_preferences");
      jdbc.execute("DROP FUNCTION appearance_suppress_insert()");
    }
  }

  @Test
  void s5_updatesWaitForTheOwnersLockBeforeClockAndRecheckRevision() throws Exception {
    var store = new PostgresAppearanceStore(jdbc, new TransactionTemplate(manager));
    var initial =
        new com.apptolast.organization.application.SaveAppearance(
            store, java.time.Clock.fixed(java.time.Instant.EPOCH, java.time.ZoneOffset.UTC));
    var first =
        initial.execute(
            "owner",
            new com.apptolast.organization.domain.AppearanceRevision(null, 0),
            "SYSTEM",
            "#244C3C",
            "#B7E4C7");
    var clock = org.mockito.Mockito.mock(java.time.Clock.class);
    org.mockito.Mockito.when(clock.instant()).thenReturn(java.time.Instant.EPOCH.plusSeconds(1));
    var save = new com.apptolast.organization.application.SaveAppearance(store, clock);
    try (var connection = manager.getDataSource().getConnection();
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      connection.setAutoCommit(false);
      try (var lock =
          connection.prepareStatement(
              "SELECT id FROM appearance_preferences WHERE owner_id='owner' FOR UPDATE")) {
        lock.executeQuery().close();
      }
      java.util.concurrent.Callable<Object> change =
          () -> {
            try {
              return save.execute(
                  "owner",
                  new com.apptolast.organization.domain.AppearanceRevision(first.id(), 0),
                  "DARK",
                  "#0000FF",
                  "#00FFFF");
            } catch (RuntimeException error) {
              return error;
            }
          };
      var one = executor.submit(change);
      var two = executor.submit(change);
      try {
        org.awaitility.Awaitility.await()
            .atMost(java.time.Duration.ofSeconds(5))
            .untilAsserted(
                () ->
                    assertThat(
                            jdbc.queryForObject(
                                "SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock' AND query LIKE '%appearance_preferences%'",
                                Integer.class))
                        .isEqualTo(2));
        org.mockito.Mockito.verifyNoInteractions(clock);
      } finally {
        connection.rollback();
      }
      var outcomes =
          java.util.List.of(
              one.get(10, java.util.concurrent.TimeUnit.SECONDS),
              two.get(10, java.util.concurrent.TimeUnit.SECONDS));
      assertThat(
              outcomes.stream()
                  .filter(com.apptolast.organization.domain.Appearance.class::isInstance)
                  .count())
          .isEqualTo(1);
      assertThat(
              outcomes.stream()
                  .filter(
                      com.apptolast.organization.application.AppearanceConflictException.class
                          ::isInstance)
                  .count())
          .isEqualTo(1);
      org.mockito.Mockito.verify(clock).instant();
      assertThat(store.find("owner").orElseThrow().version()).isEqualTo(1);
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource(
      delimiter = '|',
      value = {
        "theme|NULL::text", "theme|'INVALID'::text", "accent_light|'#0000ff'::text",
        "version|-1::bigint", "updated_at|NULL::timestamptz", "id|NULL::uuid"
      })
  void s35_selectedCorruptMetadataIsUnavailable(String field, String expression) {
    jdbc.update(
        "INSERT INTO appearance_preferences VALUES (?, 'owner', 'LIGHT', '#0000FF', '#00FFFF', 0, '2026-09-07 12:00Z')",
        java.util.UUID.randomUUID());
    jdbc.execute("ALTER TABLE appearance_preferences RENAME TO appearance_intact");
    var columns =
        java.util.List.of(
            "id", "owner_id", "theme", "accent_light", "accent_dark", "version", "updated_at");
    // Controlled read-corruption seam: a view exposes invalid data that normal constraints forbid.
    jdbc.execute(
        "CREATE VIEW appearance_preferences AS SELECT "
            + columns.stream()
                .map(column -> column.equals(field) ? expression + " AS " + column : column)
                .collect(java.util.stream.Collectors.joining(","))
            + " FROM appearance_intact");
    try {
      assertThatThrownBy(
              () ->
                  new PostgresAppearanceStore(jdbc, new TransactionTemplate(manager)).find("owner"))
          .isInstanceOf(com.apptolast.organization.application.StorageUnavailableException.class);
    } finally {
      jdbc.execute("DROP VIEW appearance_preferences");
      jdbc.execute("ALTER TABLE appearance_intact RENAME TO appearance_preferences");
    }
  }

  @Test
  void s9_additiveUpgradePreservesPreviousSchemaAndProjectFacts() {
    var source =
        new DriverManagerDataSource(
            postgres.getJdbcUrl() + "?currentSchema=appearance_upgrade",
            postgres.getUsername(),
            postgres.getPassword());
    var migration =
        Flyway.configure().dataSource(source).schemas("appearance_upgrade").target("18").load();
    migration.migrate();
    var old = new JdbcTemplate(source);
    old.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES(?,'owner','Existing project','Preserved','idea','2026-09-07 12:00Z','2026-09-07 12:00Z')",
        java.util.UUID.randomUUID());
    String columns =
        "SELECT table_name||'.'||column_name||':'||data_type AS definition FROM information_schema.columns WHERE table_schema='appearance_upgrade' AND table_name NOT IN ('appearance_preferences','flyway_schema_history') ORDER BY table_name,ordinal_position";
    var schema = old.queryForList(columns, String.class);
    var projects = old.queryForList("SELECT * FROM projects");
    Flyway.configure()
        .dataSource(source)
        .schemas("appearance_upgrade")
        .target("19")
        .load()
        .migrate();
    assertThat(old.queryForList(columns, String.class)).isEqualTo(schema);
    assertThat(old.queryForList("SELECT * FROM projects")).isEqualTo(projects);
    assertThat(old.queryForObject("SELECT count(*) FROM appearance_preferences", Integer.class))
        .isZero();
    assertThat(old.queryForObject("SELECT count(*) FROM outbox_events", Integer.class)).isZero();
  }
}
