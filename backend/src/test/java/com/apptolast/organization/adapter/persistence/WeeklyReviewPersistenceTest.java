package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.apptolast.organization.application.*;
import java.time.*;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers
class WeeklyReviewPersistenceTest {
  @Test
  void s23_unreadablePreferenceDoesNotBecomeAnUnconfiguredWeek() {
    jdbc.execute("ALTER TABLE availability_preferences RENAME TO weekly_missing_preference");
    try {
      org.assertj.core.api.Assertions.assertThatThrownBy(() -> review("owner", "2026-09-07"))
          .isInstanceOf(StorageUnavailableException.class);
    } finally {
      jdbc.execute("ALTER TABLE weekly_missing_preference RENAME TO availability_preferences");
    }
  }

  @Test
  void s22_futureIntervalHasTemporalPrecedenceOverInconsistentEarlierMetadata() {
    var c = context("owner");
    var id =
        session(
            c,
            "owner",
            "paused",
            "2026-09-07T09:00:00Z",
            "2026-09-07T09:20:00Z",
            2_400_000_000L,
            null);
    interval(id, 2, "2026-09-07T09:00:00Z", "2026-09-07T09:40:00Z");
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> review("owner", "2026-09-07", Instant.parse("2026-09-07T09:30:00Z")))
        .isInstanceOf(com.apptolast.organization.domain.WeeklyReviewTimeOutOfRangeException.class);
  }

  @Test
  void s22_futureChangedAtCannotBeHiddenByAnOlderLastDecision() {
    var c = context("owner");
    var id =
        session(
            c,
            "owner",
            "paused",
            "2026-09-07T09:00:00Z",
            "2026-09-07T09:40:00Z",
            2_400_000_000L,
            null);
    interval(id, 2, "2026-09-07T09:00:00Z", "2026-09-07T09:40:00Z");
    jdbc.update("UPDATE work_sessions SET last_decision_at='2026-09-07 09:20Z' WHERE id=?", id);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> review("owner", "2026-09-07", Instant.parse("2026-09-07T09:30:00Z")))
        .isInstanceOf(com.apptolast.organization.domain.WeeklyReviewTimeOutOfRangeException.class);
  }

  @Test
  void s22_confirmedIntervalAheadOfClockIsATemporalConflict() {
    var c = context("owner");
    var id =
        session(
            c,
            "owner",
            "paused",
            "2026-09-07T09:00:00Z",
            "2026-09-07T09:40:00Z",
            2_400_000_000L,
            null);
    interval(id, 2, "2026-09-07T09:00:00Z", "2026-09-07T09:40:00Z");
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> review("owner", "2026-09-07", Instant.parse("2026-09-07T09:30:00Z")))
        .isInstanceOf(com.apptolast.organization.domain.WeeklyReviewTimeOutOfRangeException.class);
  }

  @Test
  void s19_liveProjectionCannotBeginBeforeTheOriginalSession() {
    var c = context("owner");
    session(
        c,
        "owner",
        "running",
        "2026-09-07T09:10:00Z",
        "2026-09-07T09:00:00Z",
        0,
        "2026-09-07T09:00:00Z");
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> review("owner", "2026-09-07"))
        .isInstanceOf(StorageUnavailableException.class);
  }

  @Test
  void s11_closedWorkCrossingMondayCountsOnlyItsIntersection() {
    var c = context("owner");
    var cross =
        session(
            c,
            "owner",
            "closed",
            "2026-09-06T23:30:00Z",
            "2026-09-07T00:30:00Z",
            3_600_000_000L,
            null);
    interval(cross, 2, "2026-09-06T23:30:00Z", "2026-09-07T00:30:00Z");
    var boundary =
        session(
            c,
            "owner",
            "closed",
            "2026-09-06T23:30:00Z",
            "2026-09-07T00:00:00Z",
            1_800_000_000L,
            null);
    interval(boundary, 2, "2026-09-06T23:30:00Z", "2026-09-07T00:00:00Z");
    var result = review("owner", "2026-09-07");
    assertThat(result.days())
        .extracting(com.apptolast.organization.domain.WeeklyReview.Day::workedMicroseconds)
        .containsExactly(1_800_000_000L, 0L, 0L, 0L, 0L, 0L, 0L);
    assertThat(result.totals().workedMicroseconds()).isEqualTo(1_800_000_000L);
  }

  @Test
  void s19_unknownLegacyCannotHideAnUnexplainedAccumulatedDuration() {
    var c = context("owner");
    session(c, "owner", "closed", "2026-09-07T08:00:00Z", null, 60_000_000L, null);
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> review("owner", "2026-09-07"))
        .isInstanceOf(StorageUnavailableException.class);
  }

  @Test
  void s11_yearOneDatesUseTheProlepticCalendarInPostgres() {
    var c = context("owner");
    block(c[0], c[1], "0001-01-01T09:00:00Z", "0001-01-01T09:01:00Z");
    var id =
        session(
            c,
            "owner",
            "closed",
            "0001-01-01T09:00:00Z",
            "0001-01-01T09:01:00Z",
            60_000_000L,
            null);
    interval(id, 2, "0001-01-01T09:00:00Z", "0001-01-01T09:01:00Z");
    var result = review("owner", "0001-01-01");
    assertThat(result.weekStart()).isEqualTo(LocalDate.of(1, 1, 1));
    assertThat(result.startAt()).isEqualTo(Instant.parse("0001-01-01T00:00:00Z"));
    assertThat(result.days().getFirst().plannedMicroseconds()).isEqualTo(60_000_000L);
    assertThat(result.days().getFirst().workedMicroseconds()).isEqualTo(60_000_000L);
  }

  @Test
  void s20_everySourceIsScopedToTheAuthenticatedOwner() {
    for (var owner : java.util.List.of("owner", "other")) {
      var c = context(owner);
      preference(owner, owner.equals("owner") ? 30 : 120);
      block(c[0], c[1], "2026-09-07T08:00:00Z", "2026-09-07T09:00:00Z");
      var id =
          session(
              c,
              owner,
              "closed",
              "2026-09-07T09:00:00Z",
              "2026-09-07T09:10:00Z",
              600_000_000L,
              null);
      interval(id, 2, "2026-09-07T09:00:00Z", "2026-09-07T09:10:00Z");
      session(c, owner, "closed", "2026-09-07T10:00:00Z", null, 0, null);
    }
    var result = review("owner", "2026-09-07");
    assertThat(result.totals().plannedMicroseconds()).isEqualTo(3_600_000_000L);
    assertThat(result.totals().workedMicroseconds()).isEqualTo(600_000_000L);
    assertThat(result.totals().capacityMicroseconds()).isEqualTo(12_600_000_000L);
    assertThat(result.unquantifiedSessionCount()).isEqualTo(1);
  }

  @Test
  void s21_writerAfterSnapshotCannotMixNewIntervalsWithOldRunningTail() {
    var c = context("owner");
    var id =
        session(
            c,
            "owner",
            "running",
            "2026-09-07T09:00:00Z",
            "2026-09-07T09:00:00Z",
            0,
            "2026-09-07T09:00:00Z");
    var planned = block(c[0], c[1], "2026-09-07T09:00:00Z", "2026-09-07T10:00:00Z");
    preference("owner", 120);
    var otherSource =
        new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    var writer = new JdbcTemplate(otherSource);
    var clock = mock(Clock.class);
    when(clock.instant())
        .thenAnswer(
            call -> {
              new TransactionTemplate(new DataSourceTransactionManager(otherSource))
                  .executeWithoutResult(
                      tx -> {
                        writer.update(
                            "UPDATE work_sessions SET status='closed',changed_at='2026-09-07 09:20Z',last_decision_at='2026-09-07 09:20Z',running_since=NULL,worked_microseconds=1200000000,revision=2 WHERE id=?",
                            id);
                        writer.update(
                            "INSERT INTO work_session_intervals VALUES (?,2,'2026-09-07 09:00Z','2026-09-07 09:20Z')",
                            id);
                        writer.update(
                            "INSERT INTO block_projections(block_id,version,status,updated_at,start_local,end_local,zone_id,start_offset,end_offset,start_at,end_at,duration_minutes) VALUES (?,1,'planned',now(),'2026-09-08 09:00','2026-09-08 10:00','UTC','Z','Z','2026-09-08 09:00Z','2026-09-08 10:00Z',60)",
                            planned);
                        writer.update(
                            "UPDATE availability_preferences SET monday_minutes=60 WHERE owner_id='owner'");
                      });
              return Instant.parse("2026-09-07T10:00:00.123456789Z");
            });
    var queries =
        new PostgresWeeklyReviewQueries(
            new PostgresAvailabilityStore(jdbc, new TransactionTemplate(manager)), jdbc, manager);
    var first =
        new ReadWeeklyReview(queries, clock, () -> Set.of("UTC"))
            .get("owner", LocalDate.parse("2026-09-07"), "UTC");
    assertThat(first.totals().workedMicroseconds()).isEqualTo(3_600_123_456L);
    assertThat(first.days().getFirst().plannedMicroseconds()).isEqualTo(3_600_000_000L);
    assertThat(first.days().getFirst().capacityMicroseconds()).isEqualTo(7_200_000_000L);
    verify(clock).instant();
    var next = review("owner", "2026-09-07", first.serverNow());
    assertThat(next.totals().workedMicroseconds()).isEqualTo(1_200_000_000L);
    assertThat(next.days().getFirst().plannedMicroseconds()).isZero();
    assertThat(next.days().get(1).plannedMicroseconds()).isEqualTo(3_600_000_000L);
    assertThat(next.days().getFirst().capacityMicroseconds()).isEqualTo(3_600_000_000L);
  }

  private static void preference(String owner, int minutes) {
    jdbc.update(
        "INSERT INTO availability_preferences(id,owner_id,zone_id,monday_minutes,tuesday_minutes,wednesday_minutes,thursday_minutes,friday_minutes,saturday_minutes,sunday_minutes,version,created_at,updated_at) VALUES (?,?,'UTC',?,?,?,?,?,?,?,1,now(),now())",
        java.util.UUID.randomUUID(),
        owner,
        minutes,
        minutes,
        minutes,
        minutes,
        minutes,
        minutes,
        minutes);
  }

  @Test
  void s19_pausedSessionCannotRetainAnOpenTail() {
    var c = context("owner");
    var id =
        session(
            c,
            "owner",
            "paused",
            "2026-09-07T09:00:00Z",
            "2026-09-07T09:20:00Z",
            1_200_000_000L,
            "2026-09-07T09:10:00Z");
    interval(id, 2, "2026-09-07T09:00:00Z", "2026-09-07T09:20:00Z");
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> review("owner", "2026-09-07"))
        .isInstanceOf(StorageUnavailableException.class);
  }

  @Test
  void s19_unknownSelectedSessionStatusIsNotTreatedAsPaused() {
    var c = context("owner");
    var id =
        session(
            c,
            "owner",
            "illegible",
            "2026-09-07T09:00:00Z",
            "2026-09-07T09:20:00Z",
            1_200_000_000L,
            null);
    interval(id, 2, "2026-09-07T09:00:00Z", "2026-09-07T09:20:00Z");
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> review("owner", "2026-09-07"))
        .isInstanceOf(StorageUnavailableException.class);
  }

  @Test
  void s19_reversedIntervalCannotCancelPositiveWorkInTheIntegritySum() {
    var c = context("owner");
    var id =
        session(
            c,
            "owner",
            "paused",
            "2026-09-07T09:00:00Z",
            "2026-09-07T10:20:00Z",
            600_000_000L,
            null);
    interval(id, 2, "2026-09-07T09:20:00Z", "2026-09-07T09:10:00Z");
    interval(id, 3, "2026-09-07T10:00:00Z", "2026-09-07T10:20:00Z");
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> review("owner", "2026-09-07"))
        .isInstanceOf(StorageUnavailableException.class);
  }

  @Test
  void s19_runningTailCannotOverlapAlreadyClosedWork() {
    var c = context("owner");
    var id =
        session(
            c,
            "owner",
            "running",
            "2026-09-07T09:00:00Z",
            "2026-09-07T09:40:00Z",
            1_200_000_000L,
            "2026-09-07T09:10:00Z");
    interval(id, 2, "2026-09-07T09:00:00Z", "2026-09-07T09:20:00Z");
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> review("owner", "2026-09-07"))
        .isInstanceOf(StorageUnavailableException.class);
  }

  @Test
  void s19_arithmeticFailureInsideTheReadTransactionMapsToStorageUnavailable() {
    var queries =
        new PostgresWeeklyReviewQueries(
            new PostgresAvailabilityStore(jdbc, new TransactionTemplate(manager)), jdbc, manager);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                queries.read(
                    "owner",
                    preference -> {
                      Math.addExact(Long.MAX_VALUE, 1);
                      throw new AssertionError("unreachable");
                    }))
        .isInstanceOf(StorageUnavailableException.class)
        .hasCauseInstanceOf(ArithmeticException.class);
  }

  @Test
  void s23_commitFailureCannotLeakTheCompletedSummary() {
    var failing = spy(manager);
    doAnswer(
            call -> {
              call.callRealMethod();
              throw new org.springframework.transaction.TransactionSystemException(
                  "commit acknowledgement lost");
            })
        .when(failing)
        .commit(any());
    var queries =
        new PostgresWeeklyReviewQueries(
            new PostgresAvailabilityStore(jdbc, new TransactionTemplate(failing)), jdbc, failing);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                new ReadWeeklyReview(
                        queries,
                        Clock.fixed(Instant.parse("2026-09-09T12:00:00Z"), ZoneOffset.UTC),
                        () -> Set.of("UTC"))
                    .get("owner", null, null))
        .isInstanceOf(StorageUnavailableException.class);
    verify(failing).commit(any());
  }

  @Test
  void s23_factReadFailureIsStorageUnavailableWithoutPartialSummary() {
    jdbc.execute("ALTER TABLE planned_blocks RENAME TO weekly_missing_blocks");
    try {
      org.assertj.core.api.Assertions.assertThatThrownBy(() -> review("owner", "2026-09-07"))
          .isInstanceOf(StorageUnavailableException.class);
    } finally {
      jdbc.execute("ALTER TABLE weekly_missing_blocks RENAME TO planned_blocks");
    }
  }

  @Test
  void s22_selectedUnknownLegacyStillChecksItsDurableDecision() {
    var c = context("owner");
    var id = session(c, "owner", "closed", "2026-09-07T08:00:00Z", null, 0, null);
    jdbc.update("UPDATE work_sessions SET last_decision_at='2026-09-10 09:00Z' WHERE id=?", id);
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> review("owner", "2026-09-07"))
        .isInstanceOf(com.apptolast.organization.domain.WeeklyReviewTimeOutOfRangeException.class);
  }

  @Test
  void s14_futureWeekDoesNotSelectALiveTailEntirelyBeforeIt() {
    var c = context("owner");
    var id =
        session(
            c,
            "owner",
            "running",
            "2026-09-07T09:00:00Z",
            "2026-09-07T09:00:00Z",
            0,
            "2026-09-07T09:00:00Z");
    jdbc.update("UPDATE work_sessions SET last_decision_at='2026-09-15 09:00Z' WHERE id=?", id);
    assertThat(review("owner", "2026-09-21").totals().workedMicroseconds()).isZero();
  }

  @Test
  void s18_runningLegacyWithoutRunningSinceUsesItsStartWithoutBackfill() {
    var c = context("owner");
    var id = session(c, "owner", "running", "2026-09-07T09:00:00Z", null, 0, null);
    var result = review("owner", "2026-09-07", Instant.parse("2026-09-07T09:10:00Z"));
    assertThat(result.totals().workedMicroseconds()).isEqualTo(600_000_000L);
    assertThat(
            jdbc.queryForObject(
                "SELECT running_since FROM work_sessions WHERE id=?", OffsetDateTime.class, id))
        .isNull();
  }

  @Test
  void s19_missingEndWithAReceiptIsCorruptionNotUnknownLegacyDuration() {
    var c = context("owner");
    var id = session(c, "owner", "closed", "2026-09-07T08:00:00Z", null, 0, null);
    jdbc.update(
        "INSERT INTO work_session_changes(id,owner_id,session_id,request_key,action,expected_revision,occurred_at,receipt) VALUES (?,'owner',?,?,'CLOSE',1,'2026-09-07 09:00Z','{}')",
        java.util.UUID.randomUUID(),
        id,
        java.util.UUID.randomUUID());
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> review("owner", "2026-09-07"))
        .isInstanceOf(StorageUnavailableException.class);
  }

  @Test
  void s17_legacyClosedSessionsCountOnlyWhenTheirUnknownLifeStartsInTheWeek() {
    var c = context("owner");
    session(c, "owner", "closed", "2026-09-07T08:00:00Z", null, 0, null);
    session(c, "owner", "closed", "2026-09-06T08:00:00Z", null, 0, null);
    var known =
        session(
            c,
            "owner",
            "closed",
            "2026-09-07T09:00:00Z",
            "2026-09-07T09:10:00Z",
            600_000_000L,
            null);
    interval(known, 2, "2026-09-07T09:00:00Z", "2026-09-07T09:10:00Z");
    var before = jdbc.queryForList("SELECT * FROM work_sessions ORDER BY id");
    var result = review("owner", "2026-09-07");
    assertThat(result.unquantifiedSessionCount()).isEqualTo(1);
    assertThat(result.totals().workedMicroseconds()).isEqualTo(600_000_000L);
    assertThat(jdbc.queryForList("SELECT * FROM work_sessions ORDER BY id")).isEqualTo(before);
  }

  @Test
  void s19_intervalAfterClosedSessionEndCannotBeCredited() {
    var c = context("owner");
    var id =
        session(
            c,
            "owner",
            "closed",
            "2026-09-07T09:00:00Z",
            "2026-09-07T09:20:00Z",
            1_800_000_000L,
            null);
    interval(id, 2, "2026-09-07T09:00:00Z", "2026-09-07T09:30:00Z");
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> review("owner", "2026-09-07"))
        .isInstanceOf(StorageUnavailableException.class);
  }

  @Test
  void s19_intervalBeforeSessionStartCannotBeCredited() {
    var c = context("owner");
    var id =
        session(
            c,
            "owner",
            "paused",
            "2026-09-07T09:10:00Z",
            "2026-09-07T09:20:00Z",
            1_200_000_000L,
            null);
    interval(id, 2, "2026-09-07T09:00:00Z", "2026-09-07T09:20:00Z");
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> review("owner", "2026-09-07"))
        .isInstanceOf(StorageUnavailableException.class);
  }

  @Test
  void s19_overlappingIntervalsAreRejectedEvenWhenTheirSumMatches() {
    var c = context("owner");
    var id =
        session(
            c,
            "owner",
            "paused",
            "2026-09-07T09:00:00Z",
            "2026-09-07T09:30:00Z",
            2_400_000_000L,
            null);
    interval(id, 2, "2026-09-07T09:00:00Z", "2026-09-07T09:20:00Z");
    interval(id, 3, "2026-09-07T09:10:00Z", "2026-09-07T09:30:00Z");
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> review("owner", "2026-09-07"))
        .isInstanceOf(StorageUnavailableException.class);
  }

  @Test
  void s19_selectedSessionsClosedSumMustMatchItsAccumulatedWork() {
    var c = context("owner");
    var id =
        session(
            c,
            "owner",
            "paused",
            "2026-09-07T09:00:00Z",
            "2026-09-07T09:20:00Z",
            600_000_000L,
            null);
    interval(id, 2, "2026-09-07T09:00:00Z", "2026-09-07T09:20:00Z");
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> review("owner", "2026-09-07"))
        .isInstanceOf(StorageUnavailableException.class);
  }

  @Test
  void s22_pastIntervalStillChecksTheLiveSessionsLatestExtensionDecision() {
    var c = context("owner");
    var id =
        session(
            c,
            "owner",
            "paused",
            "2026-09-07T09:00:00Z",
            "2026-09-07T09:20:00Z",
            1_200_000_000L,
            null);
    interval(id, 2, "2026-09-07T09:00:00Z", "2026-09-07T09:20:00Z");
    jdbc.update(
        "UPDATE work_sessions SET last_decision_at='2026-09-14 09:00Z',effective_end_at='2026-09-14 10:00Z',revision=3 WHERE id=?",
        id);
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> review("owner", "2026-09-07", Instant.parse("2026-09-09T12:00:00Z")))
        .isInstanceOf(com.apptolast.organization.domain.WeeklyReviewTimeOutOfRangeException.class);
  }

  @Test
  void s10_runningSessionAddsOnlyItsLiveTailToClosedIntervals() {
    var c = context("owner");
    var id =
        session(
            c,
            "owner",
            "running",
            "2026-09-07T09:00:00Z",
            "2026-09-07T09:40:00Z",
            1_200_000_000L,
            "2026-09-07T09:40:00Z");
    jdbc.update("UPDATE work_sessions SET revision=3 WHERE id=?", id);
    interval(id, 2, "2026-09-07T09:00:00Z", "2026-09-07T09:20:00Z");
    var result = review("owner", "2026-09-07", Instant.parse("2026-09-07T10:00:00Z"));
    assertThat(result.totals().workedMicroseconds()).isEqualTo(2_400_000_000L);
    assertThat(result.totals().plannedMicroseconds()).isZero();
  }

  @Test
  void s10_pausedSessionCountsClosedIntervalsWithoutAddingItsAccumulatedTotal() {
    var c = context("owner");
    var id =
        session(
            c,
            "owner",
            "paused",
            "2026-09-07T09:00:00Z",
            "2026-09-07T09:20:00Z",
            1_200_000_000L,
            null);
    interval(id, 2, "2026-09-07T09:00:00Z", "2026-09-07T09:20:00Z");
    var result = review("owner", "2026-09-07");
    assertThat(result.days())
        .extracting(com.apptolast.organization.domain.WeeklyReview.Day::workedMicroseconds)
        .containsExactly(1_200_000_000L, 0L, 0L, 0L, 0L, 0L, 0L);
    assertThat(result.totals().workedMicroseconds()).isEqualTo(1_200_000_000L);
    assertThat(result.totals().plannedMicroseconds()).isZero();
  }

  private static java.util.UUID session(
      java.util.UUID[] context,
      String owner,
      String state,
      String start,
      String changed,
      long worked,
      String running) {
    var id = java.util.UUID.randomUUID();
    var at = OffsetDateTime.parse(start);
    jdbc.update(
        "INSERT INTO work_sessions(id,owner_id,project_id,task_id,request_key,started_at,planned_minutes,planned_end_at,zone_id,status,revision,changed_at,worked_microseconds,running_since) VALUES (?,?,?,?,?,?,25,?,'UTC',?,2,?,?,?)",
        id,
        owner,
        context[0],
        context[1],
        java.util.UUID.randomUUID(),
        at,
        at.plusMinutes(25),
        state,
        changed == null ? null : OffsetDateTime.parse(changed),
        worked,
        running == null ? null : OffsetDateTime.parse(running));
    return id;
  }

  private static void interval(java.util.UUID id, long revision, String start, String end) {
    jdbc.update(
        "INSERT INTO work_session_intervals(session_id,revision,start_at,end_at) VALUES (?,?,?,?)",
        id,
        revision,
        OffsetDateTime.parse(start),
        OffsetDateTime.parse(end));
  }

  @Test
  void s9_cancelledReservationDoesNotContributeItsOriginalPlan() {
    var context = context("owner");
    var id = block(context[0], context[1], "2026-09-07T09:00:00Z", "2026-09-07T10:00:00Z");
    jdbc.update(
        "INSERT INTO block_projections(block_id,version,status,updated_at) VALUES (?,1,'cancelled',now())",
        id);
    assertThat(review("owner", "2026-09-07").totals().plannedMicroseconds()).isZero();
  }

  @Test
  void s9_movedReservationUsesOnlyItsCurrentProjectedInterval() {
    var context = context("owner");
    var id = block(context[0], context[1], "2026-09-07T09:00:00Z", "2026-09-07T10:00:00Z");
    jdbc.update(
        "INSERT INTO block_projections(block_id,version,status,updated_at,start_local,end_local,zone_id,start_offset,end_offset,start_at,end_at,duration_minutes) VALUES (?,1,'planned',now(),'2026-09-08 09:00','2026-09-08 10:00','UTC','Z','Z','2026-09-08 09:00Z','2026-09-08 10:00Z',60)",
        id);
    var result = review("owner", "2026-09-07");
    assertThat(result.days())
        .extracting(com.apptolast.organization.domain.WeeklyReview.Day::plannedMicroseconds)
        .containsExactly(0L, 3_600_000_000L, 0L, 0L, 0L, 0L, 0L);
    assertThat(result.totals().plannedMicroseconds()).isEqualTo(3_600_000_000L);
  }

  private static java.util.UUID[] context(String owner) {
    var p = java.util.UUID.randomUUID();
    var t = java.util.UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,?,'Meta','','idea',now(),now())",
        p,
        owner);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,'Tarea','','pending',now(),now())",
        t,
        p);
    return new java.util.UUID[] {p, t};
  }

  @Test
  void s8_projectedPlanIsReadAndClippedEvenForCompletedContext() {
    var project = java.util.UUID.randomUUID();
    var task = java.util.UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner','Meta','','completed',now(),now())",
        project);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at,completed_at) VALUES (?,?,'Tarea','','completed',now(),now(),now())",
        task,
        project);
    block(project, task, "2026-09-06T23:30:00Z", "2026-09-07T00:30:00Z");
    block(project, task, "2026-09-07T23:30:00Z", "2026-09-08T00:30:00Z");
    var before = jdbc.queryForList("SELECT * FROM planned_blocks ORDER BY id");
    var result = review("owner", "2026-09-07");
    assertThat(result.days())
        .extracting(com.apptolast.organization.domain.WeeklyReview.Day::plannedMicroseconds)
        .containsExactly(3_600_000_000L, 1_800_000_000L, 0L, 0L, 0L, 0L, 0L);
    assertThat(result.totals().plannedMicroseconds()).isEqualTo(5_400_000_000L);
    assertThat(result.totals().workedMicroseconds()).isZero();
    assertThat(jdbc.queryForList("SELECT * FROM planned_blocks ORDER BY id")).isEqualTo(before);
  }

  private static java.util.UUID block(
      java.util.UUID project, java.util.UUID task, String from, String to) {
    var start = OffsetDateTime.parse(from);
    var end = OffsetDateTime.parse(to);
    var id = java.util.UUID.randomUUID();
    jdbc.update(
        "INSERT INTO planned_blocks(id,project_id,task_id,request_key,objective,start_local,end_local,zone_id,start_offset,end_offset,allow_over_budget,start_at,end_at,duration_minutes,created_at) VALUES (?,?,?,?, 'Plan',?,?,'UTC','Z','Z',false,?,?,?,?)",
        id,
        project,
        task,
        java.util.UUID.randomUUID(),
        start.toLocalDateTime(),
        end.toLocalDateTime(),
        start,
        end,
        Duration.between(start, end).toMinutes(),
        start);
    return id;
  }

  private static com.apptolast.organization.domain.WeeklyReview review(String owner, String date) {
    return review(owner, date, Instant.parse("2026-09-09T12:00:00Z"));
  }

  private static com.apptolast.organization.domain.WeeklyReview review(
      String owner, String date, Instant now) {
    var queries =
        new PostgresWeeklyReviewQueries(
            new PostgresAvailabilityStore(jdbc, new TransactionTemplate(manager)), jdbc, manager);
    return new ReadWeeklyReview(queries, Clock.fixed(now, ZoneOffset.UTC), () -> Set.of("UTC"))
        .get(owner, LocalDate.parse(date), "UTC");
  }

  @BeforeEach
  void clean() {
    jdbc.execute(
        "TRUNCATE work_session_intervals,work_session_changes,work_sessions,block_changes,block_projections,planned_blocks,task_status_history,tasks,outbox_events,projects,availability_preferences");
  }

  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  static JdbcTemplate jdbc;
  static DataSourceTransactionManager manager;

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
  void s1_emptyWeekReadsPostgresInReadOnlyRepeatableRead() {
    var clock = mock(Clock.class);
    when(clock.instant())
        .thenAnswer(
            call -> {
              assertThat(jdbc.queryForObject("SHOW transaction_isolation", String.class))
                  .isEqualTo("repeatable read");
              assertThat(jdbc.queryForObject("SHOW transaction_read_only", String.class))
                  .isEqualTo("on");
              return Instant.parse("2026-09-09T12:00:00.123456789Z");
            });
    var catalog = mock(ZoneCatalog.class);
    when(catalog.zones()).thenReturn(Set.of("UTC"));
    var queries =
        new PostgresWeeklyReviewQueries(
            new PostgresAvailabilityStore(jdbc, new TransactionTemplate(manager)), jdbc, manager);
    var result = new ReadWeeklyReview(queries, clock, catalog).get("owner", null, null);
    assertThat(result.weekStart()).isEqualTo(LocalDate.parse("2026-09-07"));
    assertThat(result.serverNow()).isEqualTo(Instant.parse("2026-09-09T12:00:00.123456Z"));
    assertThat(result.days())
        .hasSize(7)
        .allSatisfy(
            day -> {
              assertThat(day.plannedMicroseconds()).isZero();
              assertThat(day.workedMicroseconds()).isZero();
              assertThat(day.capacityMicroseconds()).isNull();
            });
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class)).isZero();
    verify(clock).instant();
  }
}
