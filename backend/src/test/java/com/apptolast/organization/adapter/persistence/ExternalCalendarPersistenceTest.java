package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.apptolast.organization.domain.ExternalCalendarInput;
import com.apptolast.organization.domain.ExternalEvent;
import com.apptolast.organization.domain.FeedError;
import com.apptolast.organization.domain.SyncStatus;
import com.apptolast.organization.domain.SyncSummary;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** V26: suscripción única por propietario e instantánea reemplazada de forma atómica. */
@Testcontainers
class ExternalCalendarPersistenceTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  static JdbcTemplate jdbc;
  static DataSourceTransactionManager manager;

  static final String A = "persona-a";
  static final String B = "persona-b";
  static final Instant NOW = Instant.parse("2030-01-07T12:00:00Z");
  static final Instant EARLIER = Instant.parse("2030-01-07T09:00:00Z");

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
    jdbc.update("DELETE FROM external_calendar_subscriptions");
  }

  PostgresExternalCalendarStore store() {
    return new PostgresExternalCalendarStore(jdbc, new TransactionTemplate(manager));
  }

  static ExternalCalendarInput input(String label, String url) {
    return ExternalCalendarInput.of(label, url);
  }

  static ExternalCalendarInput work() {
    return input("Trabajo", "https://feed.example.test/calendar/ical/abc123/basic.ics");
  }

  static byte[] cipher(String marker) {
    var bytes = new byte[40];
    java.util.Arrays.fill(bytes, (byte) marker.charAt(0));
    return bytes;
  }

  static ExternalEvent event(String uid, String startAt, String endAt) {
    return new ExternalEvent(
        uid, "Evento " + uid, Instant.parse(startAt), Instant.parse(endAt), false);
  }

  static SyncSummary summary(int imported, boolean truncated) {
    return new SyncSummary("Europe/Madrid", imported, 0, 0, 0, truncated);
  }

  @Test
  void s1_absenceIsReadWithoutInsertingAnything() {
    assertThat(store().find(A)).isEmpty();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM external_calendar_subscriptions", Integer.class))
        .isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM external_calendar_events", Integer.class))
        .isZero();
  }

  @Test
  void s2_aNewSubscriptionStartsAtVersionZeroWithEveryCounterAtZero() {
    var id = UUID.randomUUID();
    var stored = store().create(A, id, work(), cipher("C"), NOW);
    assertThat(stored.version()).isZero();
    assertThat(stored.urlCiphertext()).isEqualTo(cipher("C"));
    var subscription = stored.subscription();
    assertThat(subscription.id()).isEqualTo(id);
    assertThat(subscription.label()).isEqualTo("Trabajo");
    assertThat(subscription.urlHost()).isEqualTo("feed.example.test");
    assertThat(subscription.urlTail()).isEqualTo(".ics");
    assertThat(subscription.snapshotZoneId()).isNull();
    assertThat(subscription.lastAttemptAt()).isNull();
    assertThat(subscription.lastSyncAt()).isNull();
    assertThat(subscription.lastStatus()).isNull();
    assertThat(subscription.lastError()).isNull();
    assertThat(subscription.imported()).isZero();
    assertThat(subscription.skippedRecurring()).isZero();
    assertThat(subscription.skippedCancelled()).isZero();
    assertThat(subscription.skippedInvalid()).isZero();
    assertThat(subscription.truncated()).isFalse();
    assertThat(subscription.updatedAt()).isEqualTo(NOW);
  }

  @Test
  void s2_theStoredRowNeverHoldsThePlainUrl() {
    store().create(A, UUID.randomUUID(), work(), cipher("C"), NOW);
    var row =
        jdbc.queryForMap("SELECT * FROM external_calendar_subscriptions WHERE owner_id=?", A)
            .toString();
    assertThat(row).doesNotContain("abc123");
    assertThat(
            (byte[])
                jdbc.queryForObject(
                    "SELECT url_ciphertext FROM external_calendar_subscriptions WHERE owner_id=?",
                    byte[].class,
                    A))
        .isEqualTo(cipher("C"));
  }

  @Test
  void s30_aCommittedSubscriptionSurvivesANewStore() {
    var id = UUID.randomUUID();
    store().create(A, id, work(), cipher("C"), NOW);
    var reopened = store().find(A).orElseThrow();
    assertThat(reopened.subscription().id()).isEqualTo(id);
    assertThat(reopened.version()).isZero();
  }

  /**
   * @s30 entero, que la prueba de arriba dejaba a medias: «una sincronización confirmó en base de
   *     datos lastSyncAt 11:00Z pero su respuesta se perdió; el backend se reinicia y GET devuelve
   *     200 con la misma id, ese lastSyncAt, y GET /events devuelve los eventos de esa
   *     sincronización».
   *     <p>Lo que faltaba: la respuesta perdida y la instantánea. Aquí se confirman **dos**
   *     sincronizaciones —la de las 09:00Z con cinco eventos y la de las 11:00Z, cuya respuesta se
   *     descarta a propósito, que es exactamente lo que significa «se perdió»— y sólo entonces se
   *     abre un almacén nuevo sobre la misma base, que es como se modela el reinicio en esta clase.
   */
  @Test
  void s30_theSnapshotOfASyncWhoseResponseWasLostSurvivesTheRestart() {
    var id = UUID.randomUUID();
    var nineOClock = Instant.parse("2030-01-07T09:00:00Z");
    var elevenOClock = Instant.parse("2030-01-07T11:00:00Z");
    store().create(A, id, work(), cipher("C"), NOW);
    store()
        .commitSuccess(
            A,
            0,
            summary(5, false),
            List.of(
                event("v1", "2030-01-08T09:00:00Z", "2030-01-08T10:00:00Z"),
                event("v2", "2030-01-08T10:00:00Z", "2030-01-08T11:00:00Z"),
                event("v3", "2030-01-08T11:00:00Z", "2030-01-08T12:00:00Z"),
                event("v4", "2030-01-08T12:00:00Z", "2030-01-08T13:00:00Z"),
                event("v5", "2030-01-08T13:00:00Z", "2030-01-08T14:00:00Z")),
            nineOClock);
    // La segunda sí se confirma en la base; su valor de retorno se descarta, que es lo que le pasa
    // al llamante cuando la respuesta se pierde por el camino.
    store()
        .commitSuccess(
            A,
            1,
            summary(2, false),
            List.of(
                event("w1", "2030-01-08T15:00:00Z", "2030-01-08T16:00:00Z"),
                event("w2", "2030-01-08T17:00:00Z", "2030-01-08T18:00:00Z")),
            elevenOClock);

    var afterRestart = store().find(A).orElseThrow();
    assertThat(afterRestart.subscription().id()).isEqualTo(id);
    assertThat(afterRestart.subscription().lastSyncAt()).isEqualTo(elevenOClock);
    assertThat(afterRestart.subscription().lastAttemptAt()).isEqualTo(elevenOClock);
    assertThat(afterRestart.subscription().lastStatus()).isEqualTo(SyncStatus.OK);
    assertThat(afterRestart.subscription().imported()).isEqualTo(2);
    assertThat(afterRestart.version()).isEqualTo(2);
    assertThat(
            store()
                .events(
                    A,
                    Instant.parse("2030-01-08T00:00:00Z"),
                    Instant.parse("2030-01-09T00:00:00Z"))
                .stream()
                .map(ExternalEvent::uid))
        .containsExactly("w1", "w2");
  }

  @Test
  void s6_relabellingBumpsTheVersionAndKeepsSnapshotAndCounters() {
    store().create(A, UUID.randomUUID(), work(), cipher("C"), NOW);
    store()
        .commitSuccess(
            A,
            0,
            summary(12, false),
            List.of(event("u1", "2030-01-08T09:00:00Z", "2030-01-08T10:00:00Z")),
            EARLIER);
    var relabelled = store().relabel(A, "Casa", cipher("E"), NOW);
    assertThat(relabelled.version()).isEqualTo(2);
    assertThat(relabelled.subscription().label()).isEqualTo("Casa");
    assertThat(relabelled.subscription().imported()).isEqualTo(12);
    assertThat(relabelled.subscription().lastSyncAt()).isEqualTo(EARLIER);
    assertThat(relabelled.subscription().lastStatus()).isEqualTo(SyncStatus.OK);
    assertThat(
            store()
                .events(
                    A,
                    Instant.parse("2030-01-08T00:00:00Z"),
                    Instant.parse("2030-01-09T00:00:00Z")))
        .hasSize(1);
  }

  @Test
  void s6_rebindingClearsTheSnapshotAndEveryCounter() {
    store().create(A, UUID.randomUUID(), work(), cipher("C"), NOW);
    store()
        .commitSuccess(
            A,
            0,
            summary(12, true),
            List.of(event("u1", "2030-01-08T09:00:00Z", "2030-01-08T10:00:00Z")),
            EARLIER);
    var rebound =
        store().rebind(A, input("Trabajo", "https://otro.example.test/b.ics"), cipher("D"), NOW);
    assertThat(rebound.version()).isEqualTo(2);
    assertThat(rebound.subscription().urlHost()).isEqualTo("otro.example.test");
    assertThat(rebound.subscription().imported()).isZero();
    assertThat(rebound.subscription().truncated()).isFalse();
    assertThat(rebound.subscription().lastSyncAt()).isNull();
    assertThat(rebound.subscription().lastAttemptAt()).isNull();
    assertThat(rebound.subscription().lastStatus()).isNull();
    assertThat(rebound.subscription().lastError()).isNull();
    assertThat(rebound.subscription().snapshotZoneId()).isNull();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM external_calendar_events", Integer.class))
        .isZero();
  }

  @Test
  void s6_rebindingKeepsTheIdentityOfTheSubscription() {
    var id = store().create(A, UUID.randomUUID(), work(), cipher("C"), NOW).subscription().id();
    assertThat(
            store()
                .rebind(A, input("Otro", "https://otro.example.test/b.ics"), cipher("D"), NOW)
                .subscription()
                .id())
        .isEqualTo(id);
  }

  @Test
  void s7_deletingDragsTheSnapshotAndIsIdempotent() {
    store().create(A, UUID.randomUUID(), work(), cipher("C"), NOW);
    store()
        .commitSuccess(
            A,
            0,
            summary(1, false),
            List.of(event("u1", "2030-01-08T09:00:00Z", "2030-01-08T10:00:00Z")),
            NOW);
    assertThat(store().delete(A)).isTrue();
    assertThat(store().find(A)).isEmpty();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM external_calendar_events", Integer.class))
        .isZero();
    assertThat(store().delete(A)).isFalse();
  }

  @Test
  void s25_aSuccessfulSyncReplacesTheWholeSnapshot() {
    store().create(A, UUID.randomUUID(), work(), cipher("C"), NOW);
    store()
        .commitSuccess(
            A,
            0,
            summary(2, false),
            List.of(
                event("u1", "2030-01-08T09:00:00Z", "2030-01-08T10:00:00Z"),
                event("u2", "2030-01-08T11:00:00Z", "2030-01-08T12:00:00Z")),
            EARLIER);
    var second =
        store()
            .commitSuccess(
                A,
                1,
                summary(2, false),
                List.of(
                    new ExternalEvent(
                        "u2",
                        "Nuevo",
                        Instant.parse("2030-01-08T11:00:00Z"),
                        Instant.parse("2030-01-08T12:00:00Z"),
                        false),
                    event("u3", "2030-01-08T13:00:00Z", "2030-01-08T14:00:00Z")),
                NOW);
    assertThat(second).isPresent();
    assertThat(second.orElseThrow().version()).isEqualTo(2);
    assertThat(second.orElseThrow().subscription().lastSyncAt()).isEqualTo(NOW);
    assertThat(second.orElseThrow().subscription().lastAttemptAt()).isEqualTo(NOW);
    assertThat(second.orElseThrow().subscription().lastStatus()).isEqualTo(SyncStatus.OK);
    assertThat(second.orElseThrow().subscription().lastError()).isNull();
    var events =
        store()
            .events(
                A, Instant.parse("2030-01-08T00:00:00Z"), Instant.parse("2030-01-09T00:00:00Z"));
    assertThat(events.stream().map(ExternalEvent::uid)).containsExactly("u2", "u3");
    assertThat(events.getFirst().summary()).isEqualTo("Nuevo");
  }

  /**
   * @s25, tercer Then: «una lectura concurrente durante la sincronización ve la lista anterior
   *     completa o la nueva completa, nunca una vacía ni mezclada».
   *     <p>La técnica es la de {@code HistoryReadTransactionTest}: se subclasifica el {@link
   *     JdbcTemplate} del escritor para colarse **dentro** de su transacción, justo después del
   *     DELETE y antes de insertar la lista nueva —el único instante en que la instantánea está
   *     vacía—, y desde otro hilo, y por tanto desde otra conexión, se lee. Sin el envoltorio
   *     transaccional de {@code commitSuccess} esa lectura devolvería la lista vacía.
   */
  @Test
  @Timeout(120)
  void s25_aConcurrentReadNeverSeesAnEmptyOrMixedSnapshot() throws Exception {
    store().create(A, UUID.randomUUID(), work(), cipher("C"), NOW);
    store()
        .commitSuccess(
            A,
            0,
            summary(2, false),
            List.of(
                event("u1", "2030-01-08T09:00:00Z", "2030-01-08T10:00:00Z"),
                event("u2", "2030-01-08T11:00:00Z", "2030-01-08T12:00:00Z")),
            EARLIER);

    var readers = Executors.newSingleThreadExecutor();
    var seenMidWrite = new AtomicReference<List<String>>();
    var failure = new AtomicReference<Exception>();
    var interrupting =
        new JdbcTemplate(jdbc.getDataSource()) {
          @Override
          public int update(String sql, Object... args) {
            int rows = super.update(sql, args);
            if (sql.startsWith("DELETE FROM external_calendar_events")
                && seenMidWrite.get() == null)
              try {
                seenMidWrite.set(
                    readers
                        .submit(
                            () ->
                                store()
                                    .events(
                                        A,
                                        Instant.parse("2030-01-08T00:00:00Z"),
                                        Instant.parse("2030-01-09T00:00:00Z"))
                                    .stream()
                                    .map(ExternalEvent::uid)
                                    .toList())
                        .get(30, TimeUnit.SECONDS));
              } catch (Exception unreadable) {
                failure.set(unreadable);
              }
            return rows;
          }
        };

    try {
      new PostgresExternalCalendarStore(interrupting, new TransactionTemplate(manager))
          .commitSuccess(
              A,
              1,
              summary(2, false),
              List.of(
                  event("u3", "2030-01-08T13:00:00Z", "2030-01-08T14:00:00Z"),
                  event("u4", "2030-01-08T15:00:00Z", "2030-01-08T16:00:00Z")),
              NOW);
    } finally {
      readers.shutdownNow();
    }

    assertThat(failure.get()).isNull();
    assertThat(seenMidWrite.get())
        .as("la lectura concurrente debe haberse ejecutado dentro de la escritura")
        .isNotNull();
    // Ni vacía ni mezclada: exactamente la lista anterior completa. La nueva aún no existe para
    // nadie de fuera, así que ésta es la única respuesta correcta en ese instante.
    assertThat(seenMidWrite.get()).containsExactly("u1", "u2");
    // Y al terminar, exactamente la nueva completa.
    assertThat(
            store()
                .events(
                    A, Instant.parse("2030-01-08T00:00:00Z"), Instant.parse("2030-01-09T00:00:00Z"))
                .stream()
                .map(ExternalEvent::uid))
        .containsExactly("u3", "u4");
  }

  @Test
  void s26_aSyncThatLostTheVersionRaceChangesNothing() {
    store().create(A, UUID.randomUUID(), work(), cipher("C"), NOW);
    store()
        .commitSuccess(
            A,
            0,
            summary(1, false),
            List.of(event("u1", "2030-01-08T09:00:00Z", "2030-01-08T10:00:00Z")),
            EARLIER);
    var loser =
        store()
            .commitSuccess(
                A,
                0,
                summary(9, false),
                List.of(event("u9", "2030-01-08T15:00:00Z", "2030-01-08T16:00:00Z")),
                NOW);
    assertThat(loser).isEmpty();
    assertThat(store().find(A).orElseThrow().subscription().imported()).isEqualTo(1);
    assertThat(store().find(A).orElseThrow().subscription().lastSyncAt()).isEqualTo(EARLIER);
    assertThat(
            store()
                .events(
                    A, Instant.parse("2030-01-08T00:00:00Z"), Instant.parse("2030-01-09T00:00:00Z"))
                .stream()
                .map(ExternalEvent::uid))
        .containsExactly("u1");
  }

  @Test
  void s12_aFailedSyncKeepsTheSnapshotAndTheCounters() {
    store().create(A, UUID.randomUUID(), work(), cipher("C"), NOW);
    store()
        .commitSuccess(
            A,
            0,
            summary(5, false),
            List.of(event("u1", "2030-01-08T09:00:00Z", "2030-01-08T10:00:00Z")),
            EARLIER);
    var failed = store().commitFailure(A, 1, FeedError.FEED_HTTP_ERROR, NOW).orElseThrow();
    assertThat(failed.subscription().lastStatus()).isEqualTo(SyncStatus.FAILED);
    assertThat(failed.subscription().lastError()).isEqualTo(FeedError.FEED_HTTP_ERROR);
    assertThat(failed.subscription().lastAttemptAt()).isEqualTo(NOW);
    assertThat(failed.subscription().lastSyncAt()).isEqualTo(EARLIER);
    assertThat(failed.subscription().imported()).isEqualTo(5);
    assertThat(failed.subscription().snapshotZoneId()).isEqualTo("Europe/Madrid");
    assertThat(
            store()
                .events(
                    A,
                    Instant.parse("2030-01-08T00:00:00Z"),
                    Instant.parse("2030-01-09T00:00:00Z")))
        .hasSize(1);
  }

  @Test
  void s26_aFailedSyncAlsoLosesTheVersionRace() {
    store().create(A, UUID.randomUUID(), work(), cipher("C"), NOW);
    store().commitSuccess(A, 0, summary(5, false), List.of(), EARLIER);
    assertThat(store().commitFailure(A, 0, FeedError.FEED_UNREACHABLE, NOW)).isEmpty();
    assertThat(store().find(A).orElseThrow().subscription().lastStatus()).isEqualTo(SyncStatus.OK);
  }

  @Test
  void s31_eventsAreReadInTheHalfOpenRangeOrderedByStartAndUid() {
    store().create(A, UUID.randomUUID(), work(), cipher("C"), NOW);
    store()
        .commitSuccess(
            A,
            0,
            summary(5, false),
            List.of(
                new ExternalEvent(
                    "b",
                    "A",
                    Instant.parse("2030-01-07T08:00:00Z"),
                    Instant.parse("2030-01-07T09:00:00Z"),
                    false),
                new ExternalEvent(
                    "a",
                    "E",
                    Instant.parse("2030-01-07T08:00:00Z"),
                    Instant.parse("2030-01-07T09:00:00Z"),
                    false),
                new ExternalEvent(
                    "c",
                    "B",
                    Instant.parse("2030-01-07T23:30:00Z"),
                    Instant.parse("2030-01-08T00:30:00Z"),
                    false),
                new ExternalEvent(
                    "d",
                    "C",
                    Instant.parse("2030-01-08T00:00:00Z"),
                    Instant.parse("2030-01-08T01:00:00Z"),
                    false),
                new ExternalEvent(
                    "e",
                    "D",
                    Instant.parse("2030-01-06T23:00:00Z"),
                    Instant.parse("2030-01-07T00:00:00Z"),
                    false)),
            NOW);
    var items =
        store()
            .events(
                A, Instant.parse("2030-01-07T00:00:00Z"), Instant.parse("2030-01-08T00:00:00Z"));
    assertThat(items.stream().map(ExternalEvent::summary)).containsExactly("E", "A", "B");
  }

  @Test
  void s16_allDaySurvivesTheRoundTrip() {
    store().create(A, UUID.randomUUID(), work(), cipher("C"), NOW);
    store()
        .commitSuccess(
            A,
            0,
            summary(1, false),
            List.of(
                new ExternalEvent(
                    "d1",
                    "Fiesta",
                    Instant.parse("2030-01-06T23:00:00Z"),
                    Instant.parse("2030-01-07T23:00:00Z"),
                    true)),
            NOW);
    assertThat(
            store()
                .events(
                    A, Instant.parse("2030-01-07T00:00:00Z"), Instant.parse("2030-01-08T00:00:00Z"))
                .getFirst()
                .allDay())
        .isTrue();
  }

  @Test
  void s24_fiveHundredEventsAreStoredAndReadBack() {
    store().create(A, UUID.randomUUID(), work(), cipher("C"), NOW);
    var many = new java.util.ArrayList<ExternalEvent>();
    for (int i = 0; i < 500; i++) {
      var start = Instant.parse("2030-01-08T00:00:00Z").plusSeconds(60L * i);
      many.add(new ExternalEvent("u" + i, "Evento", start, start.plusSeconds(60), false));
    }
    store().commitSuccess(A, 0, summary(650, true), many, NOW);
    assertThat(
            store()
                .events(
                    A,
                    Instant.parse("2030-01-08T00:00:00Z"),
                    Instant.parse("2030-01-09T00:00:00Z")))
        .hasSize(500);
    assertThat(store().find(A).orElseThrow().subscription().truncated()).isTrue();
  }

  @Test
  void s33_ownersAreIsolated() {
    store().create(A, UUID.randomUUID(), work(), cipher("C"), NOW);
    store()
        .commitSuccess(
            A,
            0,
            summary(1, false),
            List.of(event("u1", "2030-01-08T09:00:00Z", "2030-01-08T10:00:00Z")),
            NOW);
    store()
        .create(
            B,
            UUID.randomUUID(),
            input("Suya", "https://otro.example.test/b.ics"),
            cipher("D"),
            NOW);
    store()
        .commitSuccess(
            B,
            0,
            summary(1, false),
            List.of(event("u1", "2030-01-08T20:00:00Z", "2030-01-08T21:00:00Z")),
            NOW);
    var window =
        List.of(Instant.parse("2030-01-08T00:00:00Z"), Instant.parse("2030-01-09T00:00:00Z"));
    assertThat(store().events(A, window.getFirst(), window.get(1)).getFirst().startAt())
        .isEqualTo(Instant.parse("2030-01-08T09:00:00Z"));
    store().delete(B);
    assertThat(store().find(A)).isPresent();
    assertThat(store().events(A, window.getFirst(), window.get(1))).hasSize(1);
  }

  @Test
  void s33_theSameUidCanBelongToTwoOwners() {
    store().create(A, UUID.randomUUID(), work(), cipher("C"), NOW);
    store()
        .create(
            B,
            UUID.randomUUID(),
            input("Suya", "https://otro.example.test/b.ics"),
            cipher("D"),
            NOW);
    store()
        .commitSuccess(
            A,
            0,
            summary(1, false),
            List.of(event("dup", "2030-01-08T09:00:00Z", "2030-01-08T10:00:00Z")),
            NOW);
    store()
        .commitSuccess(
            B,
            0,
            summary(1, false),
            List.of(event("dup", "2030-01-08T09:00:00Z", "2030-01-08T10:00:00Z")),
            NOW);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM external_calendar_events", Integer.class))
        .isEqualTo(2);
  }

  @Test
  void s25_syncingDoesNotTouchTheOutbox() {
    store().create(A, UUID.randomUUID(), work(), cipher("C"), NOW);
    store()
        .commitSuccess(
            A,
            0,
            summary(1, false),
            List.of(event("u1", "2030-01-08T09:00:00Z", "2030-01-08T10:00:00Z")),
            NOW);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class)).isZero();
  }
}
