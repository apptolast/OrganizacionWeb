package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.application.StorageUnavailableException;
import com.apptolast.organization.domain.CalendarEntry;
import com.apptolast.organization.domain.CalendarFeedSecret;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

/** V24 plus the two adapters: one row per owner, and the feed read out of a single snapshot. */
@Testcontainers
class CalendarPersistenceTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  static JdbcTemplate jdbc;
  static DataSourceTransactionManager manager;

  private static final Instant WINDOW_FROM = Instant.parse("2026-08-09T12:00:00Z");
  private static final Instant WINDOW_TO = Instant.parse("2027-09-08T12:00:00Z");
  private static final Instant CREATED_AT = Instant.parse("2026-09-01T09:15:30.123456Z");

  @BeforeAll
  static void database() {
    var source =
        new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    Flyway.configure().dataSource(source).load().migrate();
    jdbc = new JdbcTemplate(source);
    manager = new DataSourceTransactionManager(source);
  }

  @BeforeEach
  void clean() {
    jdbc.update("DELETE FROM calendar_feed_tokens");
    jdbc.update("DELETE FROM block_changes");
    jdbc.update("DELETE FROM block_projections");
    jdbc.update("DELETE FROM planned_blocks");
    jdbc.update("DELETE FROM tasks");
    jdbc.update("DELETE FROM outbox_events");
    jdbc.update("DELETE FROM projects");
    jdbc.update("DELETE FROM availability_preferences");
  }

  static PostgresCalendarStore store() {
    return new PostgresCalendarStore(jdbc, manager);
  }

  // ---------- tokens ----------

  @Test
  void s1_s29_onlyTheThirtyTwoOctetFingerprintIsStoredAndOutlivesTheStore() {
    var secret = CalendarFeedSecret.issue(new java.security.SecureRandom());
    store().replace("persona-a", secret.fingerprint(), CREATED_AT);

    var row = jdbc.queryForMap("SELECT * FROM calendar_feed_tokens WHERE owner_id=?", "persona-a");
    assertThat(row.keySet()).containsExactlyInAnyOrder("owner_id", "token_hash", "created_at");
    assertThat((byte[]) row.get("token_hash")).hasSize(32).isEqualTo(secret.fingerprint());
    assertThat(row.values().toString()).doesNotContain(secret.token());

    var reopened = store();
    assertThat(reopened.createdAt("persona-a")).contains(CREATED_AT);
    assertThat(reopened.ownerOf(CalendarFeedSecret.fingerprintOf(secret.token())))
        .contains("persona-a");
  }

  @Test
  void s4_regeneratingLeavesOneRowAndTheOldFingerprintResolvesToNobody() {
    var first = CalendarFeedSecret.issue(new java.security.SecureRandom());
    var second = CalendarFeedSecret.issue(new java.security.SecureRandom());
    store().replace("persona-a", first.fingerprint(), CREATED_AT);
    store().replace("persona-a", second.fingerprint(), CREATED_AT.plusSeconds(300));

    assertThat(jdbc.queryForObject("SELECT count(*) FROM calendar_feed_tokens", Integer.class))
        .isOne();
    assertThat(store().ownerOf(first.fingerprint())).isEmpty();
    assertThat(store().ownerOf(second.fingerprint())).contains("persona-a");
    assertThat(store().createdAt("persona-a")).contains(CREATED_AT.plusSeconds(300));
  }

  @Test
  void s5_revokeDeletesTheRowAndRepeatingItChangesNothing() {
    var secret = CalendarFeedSecret.issue(new java.security.SecureRandom());
    store().replace("persona-a", secret.fingerprint(), CREATED_AT);
    store()
        .replace(
            "persona-b",
            CalendarFeedSecret.issue(new java.security.SecureRandom()).fingerprint(),
            CREATED_AT);

    store().revoke("persona-a");
    store().revoke("persona-a");

    assertThat(store().createdAt("persona-a")).isEmpty();
    assertThat(store().ownerOf(secret.fingerprint())).isEmpty();
    assertThat(store().createdAt("persona-b")).contains(CREATED_AT);
  }

  @Test
  void s10_aColludingFingerprintFailsOnceAndKeepsThePreviousToken() {
    var mine = CalendarFeedSecret.issue(new java.security.SecureRandom());
    var taken = CalendarFeedSecret.issue(new java.security.SecureRandom());
    store().replace("persona-a", mine.fingerprint(), CREATED_AT);
    store().replace("persona-b", taken.fingerprint(), CREATED_AT);

    assertThatThrownBy(() -> store().replace("persona-a", taken.fingerprint(), CREATED_AT))
        .isInstanceOf(StorageUnavailableException.class);

    assertThat(store().createdAt("persona-a")).contains(CREATED_AT);
    assertThat(store().ownerOf(mine.fingerprint())).contains("persona-a");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM calendar_feed_tokens", Integer.class))
        .isEqualTo(2);
  }

  @Test
  void s15_anUnknownFingerprintResolvesToNobodyWithoutWriting() {
    assertThat(store().ownerOf(CalendarFeedSecret.fingerprintOf("nunca-emitido"))).isEmpty();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM calendar_feed_tokens", Integer.class))
        .isZero();
  }

  // ---------- feed ----------

  static final UUID PROJECT = UUID.fromString("0f2b3a1c-9d8e-4f70-a1b2-c3d4e5f60718");
  static final UUID TASK = UUID.fromString("7c1e5d2a-3b4f-4a6c-9d8e-0f1a2b3c4d5e");
  static final UUID BLOCK = UUID.fromString("3a9f1e62-5b7c-4d0e-8f21-6a4b9c0d1e2f");

  static void project(UUID id, String owner, String status) {
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at,version)"
            + " VALUES (?,?,?,?,?,?,?,0)",
        id,
        owner,
        "P de " + owner,
        "",
        status,
        Timestamp.from(CREATED_AT),
        Timestamp.from(CREATED_AT));
  }

  static void task(UUID id, UUID projectId, String title, String status) {
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at,"
            + "completed_at,version) VALUES (?,?,?,'',?,?,?,?,0)",
        id,
        projectId,
        title,
        status,
        Timestamp.from(CREATED_AT),
        Timestamp.from(CREATED_AT),
        status.equals("completed") ? Timestamp.from(CREATED_AT) : null);
  }

  static void block(
      UUID id, UUID projectId, UUID taskId, String objective, Instant from, Instant to) {
    jdbc.update(
        "INSERT INTO planned_blocks(id,project_id,task_id,request_key,objective,start_local,"
            + "end_local,zone_id,start_offset,end_offset,allow_over_budget,start_at,end_at,"
            + "duration_minutes,created_at) VALUES (?,?,?,?,?,?,?,'Europe/Madrid','+01:00',"
            + "'+01:00',false,?,?,?,?)",
        id,
        projectId,
        taskId,
        UUID.randomUUID(),
        objective,
        LocalDateTime.ofInstant(from, java.time.ZoneOffset.UTC),
        LocalDateTime.ofInstant(to, java.time.ZoneOffset.UTC),
        Timestamp.from(from),
        Timestamp.from(to),
        (int) java.time.Duration.between(from, to).toMinutes(),
        Timestamp.from(CREATED_AT));
  }

  static void projection(UUID blockId, long version, String status, Instant updatedAt) {
    jdbc.update(
        "INSERT INTO block_projections(block_id,version,status,updated_at) VALUES (?,?,?,?)",
        blockId,
        version,
        status,
        Timestamp.from(updatedAt));
  }

  static void movedProjection(UUID blockId, long version, Instant from, Instant to, Instant at) {
    jdbc.update(
        "INSERT INTO block_projections(block_id,version,status,updated_at,start_local,end_local,"
            + "zone_id,start_offset,end_offset,start_at,end_at,duration_minutes) VALUES"
            + " (?,?,'planned',?,?,?,'Europe/Madrid','+01:00','+01:00',?,?,?)",
        blockId,
        version,
        Timestamp.from(at),
        LocalDateTime.ofInstant(from, java.time.ZoneOffset.UTC),
        LocalDateTime.ofInstant(to, java.time.ZoneOffset.UTC),
        Timestamp.from(from),
        Timestamp.from(to),
        (int) java.time.Duration.between(from, to).toMinutes());
  }

  static void availability(String owner, String zone) {
    jdbc.update(
        "INSERT INTO availability_preferences(id,owner_id,zone_id,monday_minutes,tuesday_minutes,"
            + "wednesday_minutes,thursday_minutes,friday_minutes,saturday_minutes,sunday_minutes,"
            + "version,created_at,updated_at) VALUES (?,?,?,60,60,60,60,60,0,0,0,?,?)",
        UUID.randomUUID(),
        owner,
        zone,
        Timestamp.from(CREATED_AT),
        Timestamp.from(CREATED_AT));
  }

  static void plannedB() {
    project(PROJECT, "persona-a", "active");
    task(TASK, PROJECT, "Revisión; plan, fase 2", "pending");
    block(
        BLOCK,
        PROJECT,
        TASK,
        "Objetivo de B",
        Instant.parse("2026-10-25T08:00:00Z"),
        Instant.parse("2026-10-25T09:30:00Z"));
  }

  static List<CalendarEntry> entries(String owner) {
    return store().read(owner, WINDOW_FROM, WINDOW_TO).entries();
  }

  @Test
  void s12_aPlannedBlockWithoutProjectionCarriesVersionOneAndItsCreationStamp() {
    plannedB();
    assertThat(entries("persona-a"))
        .containsExactly(
            new CalendarEntry(
                BLOCK,
                PROJECT,
                TASK,
                "Revisión; plan, fase 2",
                "Objetivo de B",
                Instant.parse("2026-10-25T08:00:00Z"),
                Instant.parse("2026-10-25T09:30:00Z"),
                1,
                CREATED_AT));
  }

  @Test
  void s19_aCancelledBlockLeavesTheFeedButNotTheDatabase() {
    plannedB();
    projection(BLOCK, 2, "cancelled", Instant.parse("2026-09-07T18:30:45.654321Z"));
    assertThat(entries("persona-a")).isEmpty();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Integer.class)).isOne();
  }

  @Test
  void s20_aMovedBlockPublishesTheProjectionIntervalVersionAndStamp() {
    plannedB();
    movedProjection(
        BLOCK,
        2,
        Instant.parse("2026-10-26T08:00:00Z"),
        Instant.parse("2026-10-26T09:30:00Z"),
        Instant.parse("2026-09-07T18:30:45.654321Z"));
    assertThat(entries("persona-a"))
        .singleElement()
        .satisfies(
            entry -> {
              assertThat(entry.startAt()).isEqualTo(Instant.parse("2026-10-26T08:00:00Z"));
              assertThat(entry.endAt()).isEqualTo(Instant.parse("2026-10-26T09:30:00Z"));
              assertThat(entry.version()).isEqualTo(2);
              assertThat(entry.stampedAt()).isEqualTo(Instant.parse("2026-09-07T18:30:45.654321Z"));
            });
  }

  @Test
  void s21_completedOrPausedOwnersStillPublishTheirBlocksWithTheTaskTitle() {
    project(PROJECT, "persona-a", "paused");
    task(TASK, PROJECT, "Cierre final", "completed");
    block(
        BLOCK,
        PROJECT,
        TASK,
        "Objetivo de B",
        Instant.parse("2026-10-25T08:00:00Z"),
        Instant.parse("2026-10-25T09:30:00Z"));
    assertThat(entries("persona-a"))
        .singleElement()
        .extracting(CalendarEntry::title)
        .isEqualTo("Cierre final");
  }

  @Test
  void s27_theFeedOfAnOwnerNeverContainsBlocksOfAnother() {
    plannedB();
    var other = UUID.randomUUID();
    var otherTask = UUID.randomUUID();
    project(other, "persona-b", "active");
    task(otherTask, other, "Tarea ajena", "pending");
    block(
        UUID.randomUUID(),
        other,
        otherTask,
        "Objetivo ajeno",
        Instant.parse("2026-10-25T08:00:00Z"),
        Instant.parse("2026-10-25T09:30:00Z"));

    assertThat(entries("persona-a")).extracting(CalendarEntry::blockId).containsExactly(BLOCK);
    assertThat(entries("persona-b"))
        .extracting(CalendarEntry::title)
        .containsExactly("Tarea ajena");
  }

  /**
   * The schema stores blocks aligned to the minute, so the second-level instants of @s18 are
   * exercised by {@code CalendarWindowTest}; here the same semi-open rule is checked against the
   * SQL at the closest representable boundaries.
   */
  @Test
  void s18_theWindowIsSemiOpenOnBothEnds() {
    project(PROJECT, "persona-a", "active");
    task(TASK, PROJECT, "T", "pending");
    var endsExactlyAtTheStart = UUID.randomUUID();
    var endsOneMinuteInside = UUID.randomUUID();
    var startsExactlyAtTheEnd = UUID.randomUUID();
    var startsOneMinuteInside = UUID.randomUUID();
    block(
        endsExactlyAtTheStart,
        PROJECT,
        TASK,
        "o",
        Instant.parse("2026-08-09T11:00:00Z"),
        WINDOW_FROM);
    block(
        endsOneMinuteInside,
        PROJECT,
        TASK,
        "o",
        Instant.parse("2026-08-09T11:01:00Z"),
        Instant.parse("2026-08-09T12:01:00Z"));
    block(
        startsExactlyAtTheEnd,
        PROJECT,
        TASK,
        "o",
        WINDOW_TO,
        Instant.parse("2027-09-08T13:00:00Z"));
    block(
        startsOneMinuteInside,
        PROJECT,
        TASK,
        "o",
        Instant.parse("2027-09-08T11:59:00Z"),
        Instant.parse("2027-09-08T12:59:00Z"));

    assertThat(entries("persona-a"))
        .extracting(CalendarEntry::blockId)
        .containsExactlyInAnyOrder(endsOneMinuteInside, startsOneMinuteInside);
  }

  @Test
  void s14_theZoneIsTheAvailabilityZoneAndIsAbsentWithoutAvailability() {
    plannedB();
    assertThat(store().read("persona-a", WINDOW_FROM, WINDOW_TO).zoneId()).isEmpty();
    availability("persona-a", "America/Bogota");
    assertThat(store().read("persona-a", WINDOW_FROM, WINDOW_TO).zoneId())
        .contains("America/Bogota");
  }

  @Test
  void s28_zoneAndBlocksComeFromASingleRepeatableReadSnapshot() throws Exception {
    plannedB();
    availability("persona-a", "Europe/Madrid");
    var template = new org.springframework.transaction.support.TransactionTemplate(manager);
    template.setReadOnly(true);
    template.setIsolationLevel(
        org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ);

    var readings =
        template.execute(
            status -> {
              var first = store().read("persona-a", WINDOW_FROM, WINDOW_TO);
              commitAMoveFromAnotherConnection();
              var second = store().read("persona-a", WINDOW_FROM, WINDOW_TO);
              return List.of(first, second);
            });

    assertThat(readings.get(0)).isEqualTo(readings.get(1));
    assertThat(readings.get(0).entries())
        .singleElement()
        .extracting(CalendarEntry::version)
        .isEqualTo(1L);
  }

  static void commitAMoveFromAnotherConnection() {
    var thread =
        new Thread(
            () ->
                movedProjection(
                    BLOCK,
                    2,
                    Instant.parse("2026-10-26T08:00:00Z"),
                    Instant.parse("2026-10-26T09:30:00Z"),
                    Instant.parse("2026-09-07T18:30:45.654321Z")));
    thread.start();
    try {
      thread.join();
    } catch (InterruptedException error) {
      Thread.currentThread().interrupt();
    }
  }

  @Test
  void s30_anUnreachableDatabaseIsReportedAsStorageUnavailable() {
    var broken =
        new JdbcTemplate(
            new DriverManagerDataSource("jdbc:postgresql://127.0.0.1:1/nada", "nadie", "nada"));
    var down =
        new PostgresCalendarStore(broken, new DataSourceTransactionManager(broken.getDataSource()));
    assertThatThrownBy(() -> down.read("persona-a", WINDOW_FROM, WINDOW_TO))
        .isInstanceOf(StorageUnavailableException.class);
    assertThatThrownBy(() -> down.createdAt("persona-a"))
        .isInstanceOf(StorageUnavailableException.class);
    assertThat(Optional.empty()).isEmpty();
  }
}
