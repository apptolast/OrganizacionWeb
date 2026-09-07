package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.application.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers
class WorkSessionTransitionStoreTest {
  @Test
  void s6_dstFoldDoesNotChangeTheRunningSnapshotOrFixedEnd() {
    var at = Instant.parse("2026-10-25T00:30:00Z");
    jdbc.update(
        "INSERT INTO availability_preferences VALUES (?,?,'Europe/Madrid',0,0,0,0,0,0,0,0,now(),now())",
        UUID.randomUUID(),
        owner);
    var original =
        new StartWorkSession(
                store, Clock.fixed(at, ZoneOffset.UTC), () -> Set.of("UTC", "Europe/Madrid"))
            .start(owner, project, task, UUID.randomUUID(), 25)
            .session();
    var row = jdbc.queryForMap("SELECT * FROM work_sessions WHERE id=?", original.id());
    var snapshot =
        new ReadWorkSessionState(
                store, Clock.fixed(Instant.parse("2026-10-25T01:30:00Z"), ZoneOffset.UTC))
            .read(owner, original.id());
    assertThat(snapshot.state().status()).isEqualTo("running");
    assertThat(snapshot.netMicroseconds()).isEqualTo(3600000000L);
    assertThat(snapshot.state().session().zoneId()).isEqualTo("Europe/Madrid");
    assertThat(snapshot.state().session().plannedEndAt()).isEqualTo(original.plannedEndAt());
    assertThat(jdbc.queryForMap("SELECT * FROM work_sessions WHERE id=?", original.id()))
        .isEqualTo(row);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_session_changes WHERE session_id=?",
                Integer.class,
                original.id()))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE aggregate_id=?",
                Integer.class,
                original.id()))
        .isEqualTo(1);
  }

  @Test
  void s6_midnightAndExpiredPlanDoNotCreateATransition() {
    var at = Instant.parse("2026-09-07T23:58:59.999999Z");
    var original =
        new StartWorkSession(store, Clock.fixed(at, ZoneOffset.UTC), () -> Set.of("UTC"))
            .start(owner, project, task, UUID.randomUUID(), 1)
            .session();
    var row = jdbc.queryForMap("SELECT * FROM work_sessions WHERE id=?", original.id());
    var snapshot =
        new ReadWorkSessionState(
                store, Clock.fixed(Instant.parse("2026-09-08T00:00:00.000001Z"), ZoneOffset.UTC))
            .read(owner, original.id());
    assertThat(snapshot.state().status()).isEqualTo("running");
    assertThat(snapshot.netMicroseconds()).isEqualTo(60000002);
    assertThat(snapshot.state().session()).isEqualTo(original);
    assertThat(jdbc.queryForMap("SELECT * FROM work_sessions WHERE id=?", original.id()))
        .isEqualTo(row);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_session_changes WHERE session_id=?",
                Integer.class,
                original.id()))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE aggregate_id=?",
                Integer.class,
                original.id()))
        .isEqualTo(1);
  }

  @Test
  void s18_startAndTransitionKeysHaveIndependentNamespaces() {
    var key = UUID.randomUUID();
    var original =
        new StartWorkSession(
                store,
                Clock.fixed(Instant.parse("2026-09-07T10:00:00Z"), ZoneOffset.UTC),
                () -> Set.of("UTC"))
            .start(owner, project, task, key, 25)
            .session();
    var change = pauseOwn(original, key).receipt();
    assertThat(store.byRequest(owner, key)).contains(original);
    assertThat(store.changeByRequest(owner, key)).contains(change);
    assertThat(change.sessionId()).isEqualTo(original.id());
  }

  @Test
  void s8_completedProjectAndTaskStillAllowPauseAndResume() {
    var original = startOwn();
    jdbc.update(
        "UPDATE tasks SET status='completed',completed_at=now(),updated_at=now() WHERE id=?", task);
    jdbc.update("UPDATE projects SET status='completed' WHERE id=?", project);
    var paused = pauseOwn(original, UUID.randomUUID()).receipt();
    var resumed =
        new ChangeWorkSession(
                store, Clock.fixed(paused.occurredAt().plusSeconds(1), ZoneOffset.UTC))
            .resume(
                owner, original.id(), UUID.randomUUID(), new WorkSessionRevision(original.id(), 2))
            .receipt();
    assertThat(resumed.after().status()).isEqualTo("running");
    assertThat(resumed.after().session()).isEqualTo(original);
    assertThat(jdbc.queryForObject("SELECT status FROM projects WHERE id=?", String.class, project))
        .isEqualTo("completed");
    assertThat(jdbc.queryForObject("SELECT status FROM tasks WHERE id=?", String.class, task))
        .isEqualTo("completed");
  }

  @Test
  void s17_uniqueKeyCollisionIsResolvedAfterRollbackInANewTransaction() {
    var original = startOwn();
    var key = UUID.randomUUID();
    var prior = pauseOwn(original, key).receipt();
    var transactions = new ArrayList<Long>();
    var firstLookup = new java.util.concurrent.atomic.AtomicBoolean(true);
    var connection =
        new JdbcTemplate(jdbc.getDataSource()) {
          @Override
          public <T> List<T> query(
              String sql, org.springframework.jdbc.core.RowMapper<T> mapper, Object... args) {
            if (sql.equals(
                "SELECT receipt FROM work_session_changes WHERE owner_id=? AND request_key=?")) {
              transactions.add(queryForObject("SELECT txid_current()", Long.class));
              if (firstLookup.getAndSet(false)) return List.of();
            }
            return super.query(sql, mapper, args);
          }
        };
    var contender = new PostgresWorkSessionStore(connection, manager, json);
    assertThatThrownBy(
            () ->
                new ChangeWorkSession(
                        contender, Clock.fixed(prior.occurredAt().plusSeconds(1), ZoneOffset.UTC))
                    .resume(owner, original.id(), key, new WorkSessionRevision(original.id(), 2)))
        .isInstanceOf(WorkSessionIdempotencyConflictException.class);
    assertThat(transactions).hasSize(2).doesNotHaveDuplicates();
    assertThat(
            new ReadWorkSessionState(store, Clock.systemUTC()).read(owner, original.id()).state())
        .isEqualTo(prior.after());
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_session_changes WHERE session_id=?",
                Integer.class,
                original.id()))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE aggregate_id=?",
                Integer.class,
                original.id()))
        .isEqualTo(2);
  }

  @Test
  void s1_databaseRejectsNegativeAccumulatedWork() {
    var original = startOwn();
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE work_sessions SET worked_microseconds=-1 WHERE id=?", original.id()))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
  }

  @Test
  void s22_snapshotExcludesAConcurrentPauseThatCommitsBeforeClock() {
    var original = startOwn();
    var clock = org.mockito.Mockito.mock(Clock.class);
    try (var worker = java.util.concurrent.Executors.newSingleThreadExecutor()) {
      org.mockito.Mockito.when(clock.instant())
          .thenAnswer(
              call -> {
                var writer = worker.submit(() -> pauseOwn(original, UUID.randomUUID()));
                assertThat(
                        writer
                            .get(5, java.util.concurrent.TimeUnit.SECONDS)
                            .receipt()
                            .after()
                            .status())
                    .isEqualTo("paused");
                assertThat(
                        jdbc.queryForObject(
                            "SELECT status FROM work_sessions WHERE id=?",
                            String.class,
                            original.id()))
                    .isEqualTo("running");
                return original.startedAt().plusSeconds(2);
              });
      var snapshot = new ReadWorkSessionState(store, clock).read(owner, original.id());
      assertThat(snapshot.state().status()).isEqualTo("running");
      assertThat(snapshot.state().revision()).isEqualTo(1);
      assertThat(snapshot.netMicroseconds()).isEqualTo(2000000);
      org.mockito.Mockito.verify(clock).instant();
      org.mockito.Mockito.verifyNoMoreInteractions(clock);
    }
    var fresh =
        new ReadWorkSessionState(
                store, Clock.fixed(original.startedAt().plusSeconds(3), ZoneOffset.UTC))
            .read(owner, original.id());
    assertThat(fresh.state().status()).isEqualTo("paused");
    assertThat(fresh.state().revision()).isEqualTo(2);
    assertThat(fresh.netMicroseconds()).isEqualTo(1000001);
  }

  @Test
  void s20_otherOwnerAndContextLocksDoNotBlockMyTransition() {
    var mine = startOwn();
    var otherOwner = UUID.randomUUID().toString();
    var otherProject = UUID.randomUUID();
    var otherTask = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,?,'Other','','active',now(),now())",
        otherProject,
        otherOwner);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,'Other','','pending',now(),now())",
        otherTask,
        otherProject);
    var other =
        new StartWorkSession(store, Clock.systemUTC(), () -> Set.of("UTC"))
            .start(otherOwner, otherProject, otherTask, UUID.randomUUID(), 25)
            .session();
    try (var worker = java.util.concurrent.Executors.newSingleThreadExecutor()) {
      new org.springframework.transaction.support.TransactionTemplate(manager)
          .execute(
              status -> {
                jdbc.queryForObject(
                    "SELECT id FROM work_sessions WHERE id=? FOR UPDATE", UUID.class, other.id());
                jdbc.queryForObject(
                    "SELECT id FROM projects WHERE id=? FOR UPDATE", UUID.class, project);
                jdbc.queryForObject("SELECT id FROM tasks WHERE id=? FOR UPDATE", UUID.class, task);
                var result = worker.submit(() -> pauseOwn(mine, UUID.randomUUID()));
                try {
                  assertThat(
                          result
                              .get(5, java.util.concurrent.TimeUnit.SECONDS)
                              .receipt()
                              .after()
                              .status())
                      .isEqualTo("paused");
                } catch (Exception error) {
                  throw new AssertionError(error);
                }
                return null;
              });
    }
  }

  @Test
  void s19_distinctKeysAtOneRevisionReturnOnePreconditionFailure() throws Exception {
    var original = startOwn();
    var results = racePause(original, UUID.randomUUID(), UUID.randomUUID());
    assertThat(results.stream().filter(WorkSessionTransitionConfirmation.class::isInstance))
        .hasSize(1);
    var failures =
        results.stream()
            .filter(
                com.apptolast.organization.domain.WorkSessionTransitionException.class::isInstance)
            .toList();
    assertThat(failures).hasSize(1);
    assertThat(
            ((com.apptolast.organization.domain.WorkSessionTransitionException) failures.getFirst())
                .code())
        .isEqualTo("PRECONDITION_FAILED");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_session_changes WHERE session_id=?",
                Integer.class,
                original.id()))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT revision FROM work_sessions WHERE id=?", Long.class, original.id()))
        .isEqualTo(2);
  }

  @Test
  void s19_sameKeyRaceConfirmsExactlyOneTransition() throws Exception {
    var original = startOwn();
    var key = UUID.randomUUID();
    var results = racePause(original, key, key);
    assertThat(results).allMatch(WorkSessionTransitionConfirmation.class::isInstance);
    var first = (WorkSessionTransitionConfirmation) results.get(0);
    var second = (WorkSessionTransitionConfirmation) results.get(1);
    assertThat(first.receipt()).isEqualTo(second.receipt());
    assertThat(List.of(first.replayed(), second.replayed())).containsExactlyInAnyOrder(false, true);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_session_changes WHERE session_id=?",
                Integer.class,
                original.id()))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE aggregate_id=?",
                Integer.class,
                original.id()))
        .isEqualTo(2);
  }

  private List<Object> racePause(
      com.apptolast.organization.domain.SessionStart original, UUID firstKey, UUID secondKey)
      throws Exception {
    return raceTransitions(
        original, List.of(() -> pauseOwn(original, firstKey), () -> pauseOwn(original, secondKey)));
  }

  private List<Object> raceTransitions(
      com.apptolast.organization.domain.SessionStart original,
      List<java.util.function.Supplier<WorkSessionTransitionConfirmation>> operations)
      throws Exception {
    try (var workers = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var futures = new ArrayList<java.util.concurrent.Future<Object>>();
      new org.springframework.transaction.support.TransactionTemplate(manager)
          .execute(
              status -> {
                jdbc.queryForObject(
                    "SELECT id FROM work_sessions WHERE id=? FOR UPDATE",
                    UUID.class,
                    original.id());
                for (var operation : operations) {
                  futures.add(
                      workers.submit(
                          () -> {
                            try {
                              return operation.get();
                            } catch (RuntimeException error) {
                              return error;
                            }
                          }));
                }
                var deadline =
                    System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
                int waiting;
                do {
                  jdbc.execute("SELECT pg_stat_clear_snapshot()");
                  waiting =
                      jdbc.queryForObject(
                          "SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock' AND query LIKE '%work_sessions%'",
                          Integer.class);
                  if (waiting == 2) break;
                  java.util.concurrent.locks.LockSupport.parkNanos(
                      java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(10));
                } while (System.nanoTime() < deadline);
                assertThat(waiting).isEqualTo(2);
                assertThat(futures).allMatch(future -> !future.isDone());
                return null;
              });
      return List.of(
          futures.get(0).get(15, java.util.concurrent.TimeUnit.SECONDS),
          futures.get(1).get(15, java.util.concurrent.TimeUnit.SECONDS));
    }
  }

  @Test
  void s24_keySqlFailureIsNotAbsence() {
    jdbc.execute("ALTER TABLE work_session_changes RENAME TO unavailable_changes");
    try {
      assertThatThrownBy(() -> store.changeByRequest(owner, UUID.randomUUID()))
          .isInstanceOf(StorageUnavailableException.class);
    } finally {
      jdbc.execute("ALTER TABLE unavailable_changes RENAME TO work_session_changes");
    }
  }

  @Test
  void s24_receiptSqlFailureIsNotAbsence() {
    jdbc.execute("ALTER TABLE work_session_changes RENAME TO unavailable_changes");
    try {
      assertThatThrownBy(() -> store.changeDetail(owner, UUID.randomUUID()))
          .isInstanceOf(StorageUnavailableException.class);
    } finally {
      jdbc.execute("ALTER TABLE unavailable_changes RENAME TO work_session_changes");
    }
  }

  @Test
  void s24_stateSqlFailureIsNotAbsence() {
    var original = startOwn();
    jdbc.execute("ALTER TABLE work_sessions RENAME TO unavailable_sessions");
    try {
      assertThatThrownBy(
              () -> new ReadWorkSessionState(store, Clock.systemUTC()).read(owner, original.id()))
          .isInstanceOf(StorageUnavailableException.class);
    } finally {
      jdbc.execute("ALTER TABLE unavailable_sessions RENAME TO work_sessions");
    }
  }

  @Test
  void s12_tokenIdentityPrecedesExistingReplay() {
    var original = startOwn();
    var key = UUID.randomUUID();
    pauseOwn(original, key);
    assertThatThrownBy(
            () ->
                new ChangeWorkSession(store, Clock.systemUTC())
                    .pause(
                        owner, original.id(), key, new WorkSessionRevision(UUID.randomUUID(), 1)))
        .isInstanceOfSatisfying(
            com.apptolast.organization.domain.WorkSessionTransitionException.class,
            error -> assertThat(error.code()).isEqualTo("PRECONDITION_FAILED"));
  }

  @Test
  void s13_foreignKeyDoesNotRevealTheReceipt() {
    var original = startOwn();
    var key = UUID.randomUUID();
    pauseOwn(original, key);
    assertThat(store.changeByRequest("foreign", key)).isEmpty();
    assertThat(store.changeByRequest(owner, UUID.randomUUID())).isEmpty();
  }

  @Test
  void s13_foreignReceiptIsIndistinguishableFromMissing() {
    var original = startOwn();
    var receipt = pauseOwn(original, UUID.randomUUID()).receipt();
    assertThat(store.changeDetail("foreign", receipt.id())).isEmpty();
    assertThat(store.changeDetail(owner, UUID.randomUUID())).isEmpty();
  }

  @Test
  void s12_foreignCommandPrecedesTokenAndReplay() {
    var original = startOwn();
    var key = UUID.randomUUID();
    pauseOwn(original, key);
    var clock = org.mockito.Mockito.mock(Clock.class);
    assertThatThrownBy(
            () ->
                new ChangeWorkSession(store, clock)
                    .pause(
                        "foreign",
                        original.id(),
                        key,
                        new WorkSessionRevision(UUID.randomUUID(), 1)))
        .isInstanceOf(WorkSessionNotFoundException.class);
    org.mockito.Mockito.verifyNoInteractions(clock);
  }

  @Test
  void s13_foreignStateIsNotFoundBeforeClock() {
    var original = startOwn();
    var clock = org.mockito.Mockito.mock(Clock.class);
    assertThatThrownBy(() -> new ReadWorkSessionState(store, clock).read("foreign", original.id()))
        .isInstanceOf(WorkSessionNotFoundException.class);
    org.mockito.Mockito.verifyNoInteractions(clock);
  }

  @Test
  void s21_deferredCommitFailureRollsBackTheCompleteTransition() {
    var original = startOwn();
    var before = jdbc.queryForMap("SELECT * FROM work_sessions WHERE id=?", original.id());
    jdbc.execute(
        "CREATE FUNCTION reject_transition_commit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'forced commit failure'; END $$");
    jdbc.execute(
        "CREATE CONSTRAINT TRIGGER reject_transition_commit AFTER INSERT ON work_session_changes DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION reject_transition_commit()");
    try {
      assertThatThrownBy(() -> pauseOwn(original, UUID.randomUUID()))
          .isInstanceOf(StorageUnavailableException.class);
    } finally {
      jdbc.execute("DROP TRIGGER reject_transition_commit ON work_session_changes");
      jdbc.execute("DROP FUNCTION reject_transition_commit()");
    }
    assertThat(jdbc.queryForMap("SELECT * FROM work_sessions WHERE id=?", original.id()))
        .isEqualTo(before);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_session_intervals WHERE session_id=?",
                Integer.class,
                original.id()))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_session_changes WHERE session_id=?",
                Integer.class,
                original.id()))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE aggregate_id=?",
                Integer.class,
                original.id()))
        .isEqualTo(1);
  }

  @Test
  void s21_suppressedOutboxWriteRollsBackEverything() {
    assertSuppressedWriteRollsBack("outbox_events", "INSERT");
  }

  @Test
  void s21_suppressedReceiptWriteWithoutWinnerIsStorageFailure() {
    assertSuppressedWriteRollsBack("work_session_changes", "INSERT");
  }

  @Test
  void s21_suppressedIntervalWriteRollsBackEverything() {
    assertSuppressedWriteRollsBack("work_session_intervals", "INSERT");
  }

  @Test
  void s21_suppressedStateWriteRollsBackEverything() {
    assertSuppressedWriteRollsBack("work_sessions", "UPDATE");
  }

  private void assertSuppressedWriteRollsBack(String table, String operation) {
    assertSuppressedWriteRollsBack(
        table, operation, original -> pauseOwn(original, UUID.randomUUID()));
  }

  private void assertSuppressedWriteRollsBack(
      String table,
      String operation,
      java.util.function.Consumer<com.apptolast.organization.domain.SessionStart> change) {
    var original = startOwn();
    var before = jdbc.queryForMap("SELECT * FROM work_sessions WHERE id=?", original.id());
    jdbc.execute(
        "CREATE FUNCTION suppress_transition_write() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RETURN NULL; END $$");
    jdbc.execute(
        "CREATE TRIGGER suppress_transition_write BEFORE "
            + operation
            + " ON "
            + table
            + " FOR EACH ROW EXECUTE FUNCTION suppress_transition_write()");
    try {
      assertThatThrownBy(() -> change.accept(original))
          .isInstanceOf(StorageUnavailableException.class);
    } finally {
      jdbc.execute("DROP TRIGGER suppress_transition_write ON " + table);
      jdbc.execute("DROP FUNCTION suppress_transition_write()");
    }
    assertThat(jdbc.queryForMap("SELECT * FROM work_sessions WHERE id=?", original.id()))
        .isEqualTo(before);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_session_intervals WHERE session_id=?",
                Integer.class,
                original.id()))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_session_changes WHERE session_id=?",
                Integer.class,
                original.id()))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE aggregate_id=?",
                Integer.class,
                original.id()))
        .isEqualTo(1);
  }

  @Test
  void s24_keyReadCompletionFailureIsNotAbsence() {
    var failing = new PostgresWorkSessionStore(jdbc, failingReadEnd(), json);
    assertThatThrownBy(() -> failing.changeByRequest(owner, UUID.randomUUID()))
        .isInstanceOf(StorageUnavailableException.class);
  }

  @Test
  void s24_receiptReadCompletionFailureIsNotAbsence() {
    var failing = new PostgresWorkSessionStore(jdbc, failingReadEnd(), json);
    assertThatThrownBy(() -> failing.changeDetail(owner, UUID.randomUUID()))
        .isInstanceOf(StorageUnavailableException.class);
  }

  @Test
  void s24_stateReadCompletionFailureIsNotAValidSnapshot() {
    var original = startOwn();
    var failing = new PostgresWorkSessionStore(jdbc, failingReadEnd(), json);
    assertThatThrownBy(
            () -> new ReadWorkSessionState(failing, Clock.systemUTC()).read(owner, original.id()))
        .isInstanceOf(StorageUnavailableException.class);
  }

  private org.springframework.transaction.PlatformTransactionManager failingReadEnd() {
    return new org.springframework.transaction.PlatformTransactionManager() {
      public org.springframework.transaction.TransactionStatus getTransaction(
          org.springframework.transaction.TransactionDefinition definition) {
        return manager.getTransaction(definition);
      }

      public void commit(org.springframework.transaction.TransactionStatus status) {
        manager.commit(status);
        throw new org.springframework.transaction.TransactionSystemException(
            "read completion failed");
      }

      public void rollback(org.springframework.transaction.TransactionStatus status) {
        manager.rollback(status);
      }
    };
  }

  @Test
  void s25_keyReadsTheOwnerReceiptThroughTheInputPort() {
    var original = startOwn();
    var key = UUID.randomUUID();
    var pause = pauseOwn(original, key);
    var result = new ReadWorkSessionChanges(store).byRequest(owner, key);
    assertThat(result).isEqualTo(pause.receipt());
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_session_changes WHERE session_id=?",
                Integer.class,
                original.id()))
        .isEqualTo(1);
  }

  @Test
  void s25_receiptDetailIsIndependentOfLaterStateAndOutbox() {
    var original = startOwn();
    var pause = pauseOwn(original, UUID.randomUUID());
    new ChangeWorkSession(store, Clock.fixed(Instant.parse("2026-09-07T11:00:00Z"), ZoneOffset.UTC))
        .resume(owner, original.id(), UUID.randomUUID(), new WorkSessionRevision(original.id(), 2));
    jdbc.update("DELETE FROM outbox_events WHERE aggregate_id=?", original.id());
    var freshStore = new PostgresWorkSessionStore(jdbc, manager, json);
    var result = freshStore.changeDetail(owner, pause.receipt().id());
    assertThat(result).contains(pause.receipt());
    assertThat(
            jdbc.queryForObject(
                "SELECT revision FROM work_sessions WHERE id=?", Long.class, original.id()))
        .isEqualTo(3);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE aggregate_id=?",
                Integer.class,
                original.id()))
        .isZero();
  }

  @Test
  void s17_keyCannotChangeExpectedRevision() {
    var original = startOwn();
    var key = UUID.randomUUID();
    pauseOwn(original, key);
    var clock = org.mockito.Mockito.mock(Clock.class);
    assertThatThrownBy(
            () ->
                new ChangeWorkSession(store, clock)
                    .pause(owner, original.id(), key, new WorkSessionRevision(original.id(), 2)))
        .isInstanceOf(WorkSessionIdempotencyConflictException.class);
    org.mockito.Mockito.verifyNoInteractions(clock);
  }

  @Test
  void s17_keyCannotChangeActionBeforeCheckingCurrentRevision() {
    var original = startOwn();
    var key = UUID.randomUUID();
    pauseOwn(original, key);
    var clock = org.mockito.Mockito.mock(Clock.class);
    assertThatThrownBy(
            () ->
                new ChangeWorkSession(store, clock)
                    .resume(owner, original.id(), key, new WorkSessionRevision(original.id(), 1)))
        .isInstanceOf(WorkSessionIdempotencyConflictException.class);
    org.mockito.Mockito.verifyNoInteractions(clock);
  }

  @Test
  void s16_replayReturnsOriginalBeforeLaterStateAndClock() {
    var original = startOwn();
    var key = UUID.randomUUID();
    var first = pauseOwn(original, key);
    new ChangeWorkSession(store, Clock.fixed(Instant.parse("2026-09-07T11:00:00Z"), ZoneOffset.UTC))
        .resume(owner, original.id(), UUID.randomUUID(), new WorkSessionRevision(original.id(), 2));
    var clock = org.mockito.Mockito.mock(Clock.class);
    var replay =
        new ChangeWorkSession(store, clock)
            .pause(owner, original.id(), key, new WorkSessionRevision(original.id(), 1));
    assertThat(replay.replayed()).isTrue();
    assertThat(replay.receipt()).isEqualTo(first.receipt());
    org.mockito.Mockito.verifyNoInteractions(clock);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_session_changes WHERE session_id=?",
                Integer.class,
                original.id()))
        .isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE aggregate_id=?",
                Integer.class,
                original.id()))
        .isEqualTo(3);
    assertThat(
            jdbc.queryForObject(
                "SELECT revision FROM work_sessions WHERE id=?", Long.class, original.id()))
        .isEqualTo(3);
  }

  @Test
  void s12_tokenForAnotherSessionIsRejectedAfterOwnership() {
    var original = startOwn();
    var clock = org.mockito.Mockito.mock(Clock.class);
    assertThatThrownBy(
            () ->
                new ChangeWorkSession(store, clock)
                    .pause(
                        owner,
                        original.id(),
                        UUID.randomUUID(),
                        new WorkSessionRevision(UUID.randomUUID(), 1)))
        .isInstanceOfSatisfying(
            com.apptolast.organization.domain.WorkSessionTransitionException.class,
            error -> assertThat(error.code()).isEqualTo("PRECONDITION_FAILED"));
    org.mockito.Mockito.verifyNoInteractions(clock);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_session_changes WHERE session_id=?",
                Integer.class,
                original.id()))
        .isZero();
  }

  @Test
  void s9_databasePreventsRunningBesidePausedForTheSameOwner() {
    var original = startOwn();
    pauseOwn(original, UUID.randomUUID());
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO work_sessions(id,owner_id,project_id,task_id,request_key,started_at,planned_minutes,planned_end_at,zone_id,status) SELECT ?,owner_id,project_id,task_id,?,started_at,planned_minutes,planned_end_at,zone_id,'running' FROM work_sessions WHERE id=?",
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    original.id()))
        .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_sessions WHERE owner_id=?", Integer.class, owner))
        .isEqualTo(1);
  }

  @Test
  void s9_pausedSessionStillOccupiesTheOwnersPlace() {
    var original = startOwn();
    pauseOwn(original, UUID.randomUUID());
    assertThatThrownBy(this::startOwn).isInstanceOf(WorkSessionAlreadyActiveException.class);
    assertThat(store.active(owner)).contains(original);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_sessions WHERE owner_id=?", Integer.class, owner))
        .isEqualTo(1);
  }

  @Test
  void s3_resumeKeepsClosedIntervalsAndStartsOnlyTheOpenSegment() {
    var original = startOwn();
    pauseOwn(original, UUID.randomUUID());
    var before =
        jdbc.queryForList("SELECT * FROM work_session_intervals WHERE session_id=?", original.id());
    var now = Instant.parse("2026-09-07T11:00:00Z");
    var result =
        new ChangeWorkSession(store, Clock.fixed(now, ZoneOffset.UTC))
            .resume(
                owner, original.id(), UUID.randomUUID(), new WorkSessionRevision(original.id(), 2));
    assertThat(result.receipt().after().status()).isEqualTo("running");
    assertThat(result.receipt().after().revision()).isEqualTo(3);
    assertThat(result.receipt().after().runningSince()).isEqualTo(now);
    assertThat(result.receipt().after().workedMicroseconds()).isEqualTo(1000001);
    assertThat(
            jdbc.queryForList(
                "SELECT * FROM work_session_intervals WHERE session_id=?", original.id()))
        .isEqualTo(before);
    var snapshot =
        new ReadWorkSessionState(store, Clock.fixed(now.plusNanos(1000), ZoneOffset.UTC))
            .read(owner, original.id());
    assertThat(snapshot.netMicroseconds()).isEqualTo(1000002);
    assertThat(store.detail(owner, original.id())).contains(original);
  }

  private com.apptolast.organization.domain.SessionStart startOwn() {
    return new StartWorkSession(
            store,
            Clock.fixed(Instant.parse("2026-09-07T10:00:00.123456Z"), ZoneOffset.UTC),
            () -> Set.of("UTC"))
        .start(owner, project, task, UUID.randomUUID(), 25)
        .session();
  }

  private WorkSessionTransitionConfirmation pauseOwn(
      com.apptolast.organization.domain.SessionStart original, UUID key) {
    return new ChangeWorkSession(
            store, Clock.fixed(Instant.parse("2026-09-07T10:00:01.123457Z"), ZoneOffset.UTC))
        .pause(owner, original.id(), key, new WorkSessionRevision(original.id(), 1));
  }

  @Test
  void s22_stateClockRunsInsideReadOnlyRepeatableRead() {
    var original =
        new StartWorkSession(
                store,
                Clock.fixed(Instant.parse("2026-09-07T10:00:00Z"), ZoneOffset.UTC),
                () -> Set.of("UTC"))
            .start(owner, project, task, UUID.randomUUID(), 25)
            .session();
    var clock = org.mockito.Mockito.mock(Clock.class);
    org.mockito.Mockito.when(clock.instant())
        .thenAnswer(
            call -> {
              assertThat(jdbc.queryForObject("SHOW transaction_read_only", String.class))
                  .isEqualTo("on");
              assertThat(jdbc.queryForObject("SHOW transaction_isolation", String.class))
                  .isEqualTo("repeatable read");
              return original.startedAt().plusSeconds(1);
            });
    assertThat(new ReadWorkSessionState(store, clock).read(owner, original.id()).netMicroseconds())
        .isEqualTo(1000000);
  }

  @Test
  void s1_stateReadsInitialProjectionWithoutMaterializingIt() {
    var original =
        new StartWorkSession(
                store,
                Clock.fixed(Instant.parse("2026-09-07T10:00:00.123456Z"), ZoneOffset.UTC),
                () -> Set.of("UTC"))
            .start(owner, project, task, UUID.randomUUID(), 25)
            .session();
    var before = jdbc.queryForMap("SELECT * FROM work_sessions WHERE id=?", original.id());
    var result =
        new ReadWorkSessionState(
                store, Clock.fixed(Instant.parse("2026-09-07T10:00:01.123456789Z"), ZoneOffset.UTC))
            .read(owner, original.id());
    assertThat(result.state().session()).isEqualTo(original);
    assertThat(result.state().status()).isEqualTo("running");
    assertThat(result.state().revision()).isEqualTo(1);
    assertThat(result.state().changedAt()).isEqualTo(original.startedAt());
    assertThat(result.state().runningSince()).isEqualTo(original.startedAt());
    assertThat(result.state().workedMicroseconds()).isZero();
    assertThat(result.netMicroseconds()).isEqualTo(1000000);
    assertThat(jdbc.queryForMap("SELECT * FROM work_sessions WHERE id=?", original.id()))
        .isEqualTo(before);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_session_intervals WHERE session_id=?",
                Integer.class,
                original.id()))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_session_changes WHERE session_id=?",
                Integer.class,
                original.id()))
        .isZero();
  }

  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  static JdbcTemplate jdbc;
  static DataSourceTransactionManager manager;
  static ObjectMapper json;
  String owner;
  UUID project;
  UUID task;
  PostgresWorkSessionStore store;

  @BeforeAll
  static void database() {
    var source =
        new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    Flyway.configure().dataSource(source).load().migrate();
    jdbc = new JdbcTemplate(source);
    manager = new DataSourceTransactionManager(source);
    json =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }

  @BeforeEach
  void context() {
    owner = UUID.randomUUID().toString();
    project = UUID.randomUUID();
    task = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,?,'P','','active',now(),now())",
        project,
        owner);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,'T','','pending',now(),now())",
        task,
        project);
    store = new PostgresWorkSessionStore(jdbc, manager, json);
  }

  @Test
  void s2_pauseCommitsExactStateIntervalReceiptAndIndependentEvent() throws Exception {
    var original =
        new StartWorkSession(
                store,
                Clock.fixed(Instant.parse("2026-09-07T10:00:00.123456Z"), ZoneOffset.UTC),
                () -> Set.of("UTC"))
            .start(owner, project, task, UUID.randomUUID(), 25)
            .session();
    var originalEvent =
        jdbc.queryForMap("SELECT * FROM outbox_events WHERE aggregate_id=?", original.id());
    var key = UUID.randomUUID();
    var result =
        new ChangeWorkSession(
                store, Clock.fixed(Instant.parse("2026-09-07T10:00:01.123457999Z"), ZoneOffset.UTC))
            .pause(owner, original.id(), key, new WorkSessionRevision(original.id(), 1));
    assertThat(result.replayed()).isFalse();
    assertThat(store.detail(owner, original.id())).contains(original);
    var state =
        jdbc.queryForMap(
            "SELECT status,revision,worked_microseconds,running_since,changed_at FROM work_sessions WHERE id=?",
            original.id());
    assertThat(state)
        .containsEntry("status", "paused")
        .containsEntry("revision", 2L)
        .containsEntry("worked_microseconds", 1000001L)
        .containsEntry("running_since", null);
    assertThat(((java.sql.Timestamp) state.get("changed_at")).toInstant())
        .isEqualTo(Instant.parse("2026-09-07T10:00:01.123457Z"));
    var intervals =
        jdbc.queryForList(
            "SELECT start_at,end_at FROM work_session_intervals WHERE session_id=?", original.id());
    assertThat(intervals).hasSize(1);
    assertThat(((java.sql.Timestamp) intervals.getFirst().get("start_at")).toInstant())
        .isEqualTo(original.startedAt());
    assertThat(((java.sql.Timestamp) intervals.getFirst().get("end_at")).toInstant())
        .isEqualTo(Instant.parse("2026-09-07T10:00:01.123457Z"));
    var receiptRow =
        jdbc.queryForMap(
            "SELECT * FROM work_session_changes WHERE owner_id=? AND request_key=?", owner, key);
    assertThat(receiptRow)
        .containsEntry("id", result.receipt().id())
        .containsEntry("session_id", original.id());
    var expectedStart =
        json.createObjectNode()
            .put("id", original.id().toString())
            .put("projectId", project.toString())
            .put("taskId", task.toString())
            .put("startedAt", "2026-09-07T10:00:00.123456Z")
            .put("plannedMinutes", 25)
            .put("plannedEndAt", "2026-09-07T10:25:00.123456Z")
            .put("zoneId", "UTC");
    var before =
        json.createObjectNode()
            .put("status", "running")
            .put("revision", 1)
            .put("changedAt", "2026-09-07T10:00:00.123456Z")
            .put("workedMicroseconds", 0)
            .put("runningSince", "2026-09-07T10:00:00.123456Z");
    before.set("session", expectedStart);
    var after =
        json.createObjectNode()
            .put("status", "paused")
            .put("revision", 2)
            .put("changedAt", "2026-09-07T10:00:01.123457Z")
            .put("workedMicroseconds", 1000001)
            .putNull("runningSince");
    after.set("session", expectedStart);
    var receipt =
        json.createObjectNode()
            .put("id", receiptRow.get("id").toString())
            .put("sessionId", original.id().toString())
            .put("action", "PAUSE")
            .put("occurredAt", "2026-09-07T10:00:01.123457Z");
    receipt.set("before", before);
    receipt.set("after", after);
    assertThat(json.readTree(receiptRow.get("receipt").toString())).isEqualTo(receipt);
    var event =
        jdbc.queryForMap(
            "SELECT * FROM outbox_events WHERE aggregate_id=? AND event_type='WorkSessionStateChanged.v1'",
            original.id());
    assertThat(json.readTree(event.get("payload").toString()))
        .isEqualTo(
            json.createObjectNode()
                .put("eventId", event.get("event_id").toString())
                .put("aggregateId", original.id().toString())
                .put("ownerId", owner)
                .put("occurredAt", "2026-09-07T10:00:01.123457Z")
                .put("schemaVersion", 1)
                .put("type", "WorkSessionStateChanged.v1")
                .put("action", "PAUSE")
                .put("revision", "2")
                .put("fromStatus", "running")
                .put("toStatus", "paused")
                .put("workedMicroseconds", "1000001")
                .putNull("runningSince"));
    assertThat(
            jdbc.queryForMap(
                "SELECT * FROM outbox_events WHERE event_id=?", originalEvent.get("event_id")))
        .isEqualTo(originalEvent);
  }

  @Test
  void s1_closeCommitsLastIntervalReceiptAndIndependentEvent() throws Exception {
    var original =
        new StartWorkSession(
                store,
                Clock.fixed(Instant.parse("2026-09-07T10:00:00.123456Z"), ZoneOffset.UTC),
                () -> Set.of("UTC"))
            .start(owner, project, task, UUID.randomUUID(), 25)
            .session();
    var originalEvent =
        jdbc.queryForMap("SELECT * FROM outbox_events WHERE aggregate_id=?", original.id());
    var key = UUID.randomUUID();
    var result =
        new ChangeWorkSession(
                store, Clock.fixed(Instant.parse("2026-09-07T10:00:01.123457999Z"), ZoneOffset.UTC))
            .close(
                owner,
                original.id(),
                key,
                new WorkSessionRevision(original.id(), 1),
                new com.apptolast.organization.domain.WorkSessionCloseNotes("Avance", "Siguiente"));
    assertThat(result.replayed()).isFalse();
    assertThat(store.detail(owner, original.id())).contains(original);
    var state =
        jdbc.queryForMap(
            "SELECT status,revision,worked_microseconds,running_since,changed_at FROM work_sessions WHERE id=?",
            original.id());
    assertThat(state)
        .containsEntry("status", "closed")
        .containsEntry("revision", 2L)
        .containsEntry("worked_microseconds", 1000001L)
        .containsEntry("running_since", null);
    assertThat(((java.sql.Timestamp) state.get("changed_at")).toInstant())
        .isEqualTo(Instant.parse("2026-09-07T10:00:01.123457Z"));
    var intervals =
        jdbc.queryForList(
            "SELECT start_at,end_at FROM work_session_intervals WHERE session_id=?", original.id());
    assertThat(intervals).hasSize(1);
    assertThat(((java.sql.Timestamp) intervals.getFirst().get("start_at")).toInstant())
        .isEqualTo(original.startedAt());
    assertThat(((java.sql.Timestamp) intervals.getFirst().get("end_at")).toInstant())
        .isEqualTo(Instant.parse("2026-09-07T10:00:01.123457Z"));
    var receiptRow =
        jdbc.queryForMap(
            "SELECT * FROM work_session_changes WHERE owner_id=? AND request_key=?", owner, key);
    assertThat(receiptRow)
        .containsEntry("id", result.receipt().id())
        .containsEntry("session_id", original.id());
    var expectedStart =
        json.createObjectNode()
            .put("id", original.id().toString())
            .put("projectId", project.toString())
            .put("taskId", task.toString())
            .put("startedAt", "2026-09-07T10:00:00.123456Z")
            .put("plannedMinutes", 25)
            .put("plannedEndAt", "2026-09-07T10:25:00.123456Z")
            .put("zoneId", "UTC");
    var before =
        json.createObjectNode()
            .put("status", "running")
            .put("revision", 1)
            .put("changedAt", "2026-09-07T10:00:00.123456Z")
            .put("workedMicroseconds", 0)
            .put("runningSince", "2026-09-07T10:00:00.123456Z");
    before.set("session", expectedStart);
    var after =
        json.createObjectNode()
            .put("status", "closed")
            .put("revision", 2)
            .put("changedAt", "2026-09-07T10:00:01.123457Z")
            .put("workedMicroseconds", 1000001)
            .putNull("runningSince");
    after.set("session", expectedStart);
    var receipt =
        json.createObjectNode()
            .put("id", receiptRow.get("id").toString())
            .put("sessionId", original.id().toString())
            .put("action", "CLOSE")
            .put("occurredAt", "2026-09-07T10:00:01.123457Z");
    receipt.set("before", before);
    receipt.set("after", after);
    receipt.set(
        "closure",
        json.createObjectNode()
            .put("progressNote", "Avance")
            .put("nextStep", "Siguiente")
            .put("workDate", "2026-09-07")
            .put("closeZoneId", "UTC"));
    assertThat(json.readTree(receiptRow.get("receipt").toString())).isEqualTo(receipt);
    var event =
        jdbc.queryForMap(
            "SELECT * FROM outbox_events WHERE aggregate_id=? AND event_type='WorkSessionClosed.v1'",
            original.id());
    assertThat(json.readTree(event.get("payload").toString()))
        .isEqualTo(
            json.createObjectNode()
                .put("eventId", event.get("event_id").toString())
                .put("aggregateId", original.id().toString())
                .put("ownerId", owner)
                .put("occurredAt", "2026-09-07T10:00:01.123457Z")
                .put("schemaVersion", 1)
                .put("type", "WorkSessionClosed.v1")
                .put("revision", "2")
                .put("fromStatus", "running")
                .put("workedMicroseconds", "1000001")
                .put("workDate", "2026-09-07")
                .put("closeZoneId", "UTC"));
    assertThat(
            jdbc.queryForMap(
                "SELECT * FROM outbox_events WHERE event_id=?", originalEvent.get("event_id")))
        .isEqualTo(originalEvent);
  }

  @Test
  void s26_readsClosureBySessionAfterOutboxRemoval() {
    var original = startOwn();
    var result =
        new ChangeWorkSession(
                store, Clock.fixed(original.startedAt().plusSeconds(1), ZoneOffset.UTC))
            .close(
                owner,
                original.id(),
                UUID.randomUUID(),
                new WorkSessionRevision(original.id(), 1),
                new com.apptolast.organization.domain.WorkSessionCloseNotes(
                    "  Avance\n", "Después"));
    jdbc.update("DELETE FROM outbox_events WHERE aggregate_id=?", original.id());
    var previous = jdbc.queryForMap("SELECT * FROM work_sessions WHERE id=?", original.id());
    assertThat(store.closure(owner, original.id())).contains(result.receipt());
    assertThat(jdbc.queryForMap("SELECT * FROM work_sessions WHERE id=?", original.id()))
        .isEqualTo(previous);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE aggregate_id=?",
                Integer.class,
                original.id()))
        .isZero();
  }

  @Test
  void s26_closureOfForeignSessionHidesItsExistence() {
    var original = startOwn();
    assertThatThrownBy(() -> store.closure("other-owner", original.id()))
        .isInstanceOf(WorkSessionNotFoundException.class);
  }

  @Test
  void s16_replayRejectsDifferentProgressNoteBeforeClock() {
    var original = startOwn();
    var key = UUID.randomUUID();
    new ChangeWorkSession(store, Clock.fixed(original.startedAt().plusSeconds(1), ZoneOffset.UTC))
        .close(
            owner,
            original.id(),
            key,
            new WorkSessionRevision(original.id(), 1),
            new com.apptolast.organization.domain.WorkSessionCloseNotes("Original", "Después"));
    var clock = org.mockito.Mockito.mock(Clock.class);
    assertThatThrownBy(
            () ->
                new ChangeWorkSession(store, clock)
                    .close(
                        owner,
                        original.id(),
                        key,
                        new WorkSessionRevision(original.id(), 1),
                        new com.apptolast.organization.domain.WorkSessionCloseNotes(
                            "Distinto", "Después")))
        .isInstanceOf(WorkSessionIdempotencyConflictException.class);
    org.mockito.Mockito.verifyNoInteractions(clock);
  }

  @Test
  void s16_replayRejectsDifferentNextStepBeforeClock() {
    var original = startOwn();
    var key = UUID.randomUUID();
    new ChangeWorkSession(store, Clock.fixed(original.startedAt().plusSeconds(1), ZoneOffset.UTC))
        .close(
            owner,
            original.id(),
            key,
            new WorkSessionRevision(original.id(), 1),
            new com.apptolast.organization.domain.WorkSessionCloseNotes("Original", "Después"));
    var clock = org.mockito.Mockito.mock(Clock.class);
    assertThatThrownBy(
            () ->
                new ChangeWorkSession(store, clock)
                    .close(
                        owner,
                        original.id(),
                        key,
                        new WorkSessionRevision(original.id(), 1),
                        new com.apptolast.organization.domain.WorkSessionCloseNotes(
                            "Original", "Otro paso")))
        .isInstanceOf(WorkSessionIdempotencyConflictException.class);
    org.mockito.Mockito.verifyNoInteractions(clock);
  }

  @Test
  void s15_replaysOriginalCloseAfterAnotherSessionStarts() {
    var original = startOwn();
    var key = UUID.randomUUID();
    var first =
        new ChangeWorkSession(
                store, Clock.fixed(original.startedAt().plusSeconds(1), ZoneOffset.UTC))
            .close(
                owner,
                original.id(),
                key,
                new WorkSessionRevision(original.id(), 1),
                new com.apptolast.organization.domain.WorkSessionCloseNotes(null, null));
    var next = startOwn();
    var nextRow = jdbc.queryForMap("SELECT * FROM work_sessions WHERE id=?", next.id());
    var clock = org.mockito.Mockito.mock(Clock.class);
    var replay =
        new ChangeWorkSession(store, clock)
            .close(
                owner,
                original.id(),
                key,
                new WorkSessionRevision(original.id(), 1),
                new com.apptolast.organization.domain.WorkSessionCloseNotes("", ""));
    assertThat(replay.replayed()).isTrue();
    assertThat(replay.receipt()).isEqualTo(first.receipt());
    assertThat(store.active(owner)).contains(next);
    assertThat(store.detail(owner, original.id())).contains(original);
    assertThat(jdbc.queryForMap("SELECT * FROM work_sessions WHERE id=?", next.id()))
        .isEqualTo(nextRow);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_session_changes WHERE session_id=?",
                Integer.class,
                original.id()))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE aggregate_id=?",
                Integer.class,
                original.id()))
        .isEqualTo(2);
    org.mockito.Mockito.verifyNoInteractions(clock);
  }

  @Test
  void s23_databaseRejectsASecondDurableClosureForTheSameSession() {
    var original = startOwn();
    var receipt =
        new ChangeWorkSession(
                store, Clock.fixed(original.startedAt().plusSeconds(1), ZoneOffset.UTC))
            .close(
                owner,
                original.id(),
                UUID.randomUUID(),
                new WorkSessionRevision(original.id(), 1),
                new com.apptolast.organization.domain.WorkSessionCloseNotes("", ""))
            .receipt();
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO work_session_changes(id,owner_id,session_id,request_key,action,expected_revision,occurred_at,receipt) SELECT ?,owner_id,session_id,?,action,expected_revision,occurred_at,receipt FROM work_session_changes WHERE id=?",
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    receipt.id()))
        .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_session_changes WHERE session_id=?",
                Integer.class,
                original.id()))
        .isEqualTo(1);
  }

  @Test
  void s21_closeSuppressedStatePreservesItsPlace() {
    assertSuppressedWriteRollsBack("work_sessions", "UPDATE", this::closeOwn);
  }

  private WorkSessionTransitionConfirmation closeOwn(
      com.apptolast.organization.domain.SessionStart session) {
    return new ChangeWorkSession(
            store, Clock.fixed(session.startedAt().plusSeconds(1), ZoneOffset.UTC))
        .close(
            owner,
            session.id(),
            UUID.randomUUID(),
            new WorkSessionRevision(session.id(), 1),
            new com.apptolast.organization.domain.WorkSessionCloseNotes("", ""));
  }

  @Test
  void s21_closeSuppressedIntervalRollsBackState() {
    assertSuppressedWriteRollsBack("work_session_intervals", "INSERT", this::closeOwn);
  }

  @Test
  void s21_closeSuppressedReceiptWithoutWinnerRollsBack() {
    assertSuppressedWriteRollsBack("work_session_changes", "INSERT", this::closeOwn);
  }

  @Test
  void s21_closeSuppressedOutboxRollsBackEverything() {
    assertSuppressedWriteRollsBack("outbox_events", "INSERT", this::closeOwn);
  }

  @Test
  void s21_closeCommitFailureRetainsTheOpenPlace() {
    var original = startOwn();
    var before = jdbc.queryForMap("SELECT * FROM work_sessions WHERE id=?", original.id());
    jdbc.execute(
        "CREATE FUNCTION reject_transition_commit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'forced commit failure'; END $$");
    jdbc.execute(
        "CREATE CONSTRAINT TRIGGER reject_transition_commit AFTER INSERT ON work_session_changes DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION reject_transition_commit()");
    try {
      assertThatThrownBy(() -> closeOwn(original)).isInstanceOf(StorageUnavailableException.class);
    } finally {
      jdbc.execute("DROP TRIGGER reject_transition_commit ON work_session_changes");
      jdbc.execute("DROP FUNCTION reject_transition_commit()");
    }
    assertThat(store.active(owner)).contains(original);
    assertThat(jdbc.queryForMap("SELECT * FROM work_sessions WHERE id=?", original.id()))
        .isEqualTo(before);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_session_intervals WHERE session_id=?",
                Integer.class,
                original.id()))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_session_changes WHERE session_id=?",
                Integer.class,
                original.id()))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE aggregate_id=?",
                Integer.class,
                original.id()))
        .isEqualTo(1);
  }

  @Test
  void s18_sameCloseKeyRaceReturnsOneOriginalReceipt() throws Exception {
    var original = startOwn();
    var key = UUID.randomUUID();
    java.util.function.Supplier<WorkSessionTransitionConfirmation> close =
        () ->
            new ChangeWorkSession(
                    store, Clock.fixed(original.startedAt().plusSeconds(1), ZoneOffset.UTC))
                .close(
                    owner,
                    original.id(),
                    key,
                    new WorkSessionRevision(original.id(), 1),
                    new com.apptolast.organization.domain.WorkSessionCloseNotes("Nota", "Después"));
    var results = raceTransitions(original, List.of(close, close));
    assertThat(results).allMatch(WorkSessionTransitionConfirmation.class::isInstance);
    var first = (WorkSessionTransitionConfirmation) results.get(0);
    var second = (WorkSessionTransitionConfirmation) results.get(1);
    assertThat(first.receipt()).isEqualTo(second.receipt());
    assertThat(List.of(first.replayed(), second.replayed())).containsExactlyInAnyOrder(false, true);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_session_changes WHERE session_id=?",
                Integer.class,
                original.id()))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE aggregate_id=?",
                Integer.class,
                original.id()))
        .isEqualTo(2);
  }

  @Test
  void s18_distinctCloseKeysRaceAtOneRevision() throws Exception {
    var original = startOwn();
    var results =
        raceTransitions(original, List.of(() -> closeOwn(original), () -> closeOwn(original)));
    assertOneTransitionAndOneStale(original, results, 2, 1);
  }

  private void assertOneTransitionAndOneStale(
      com.apptolast.organization.domain.SessionStart original,
      List<Object> results,
      long revision,
      int changes) {
    assertThat(results.stream().filter(WorkSessionTransitionConfirmation.class::isInstance))
        .hasSize(1);
    var failures =
        results.stream()
            .filter(
                com.apptolast.organization.domain.WorkSessionTransitionException.class::isInstance)
            .toList();
    assertThat(failures).hasSize(1);
    assertThat(
            ((com.apptolast.organization.domain.WorkSessionTransitionException) failures.getFirst())
                .code())
        .isEqualTo("PRECONDITION_FAILED");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_session_changes WHERE session_id=?",
                Integer.class,
                original.id()))
        .isEqualTo(changes);
    assertThat(
            jdbc.queryForObject(
                "SELECT revision FROM work_sessions WHERE id=?", Long.class, original.id()))
        .isEqualTo(revision);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE aggregate_id=?",
                Integer.class,
                original.id()))
        .isEqualTo(changes + 1);
  }

  @Test
  void s18_closeAndPauseRaceAtOneRevision() throws Exception {
    var original = startOwn();
    var results =
        raceTransitions(
            original,
            List.of(() -> closeOwn(original), () -> pauseOwn(original, UUID.randomUUID())));
    assertOneTransitionAndOneStale(original, results, 2, 1);
  }

  @Test
  void s18_closeAndResumeRaceFromPaused() throws Exception {
    var original = startOwn();
    var paused = pauseOwn(original, UUID.randomUUID()).receipt();
    var command =
        new ChangeWorkSession(
            store, Clock.fixed(paused.occurredAt().plusSeconds(1), ZoneOffset.UTC));
    var token = new WorkSessionRevision(original.id(), 2);
    var results =
        raceTransitions(
            original,
            List.of(
                () ->
                    command.close(
                        owner,
                        original.id(),
                        UUID.randomUUID(),
                        token,
                        new com.apptolast.organization.domain.WorkSessionCloseNotes("", "")),
                () -> command.resume(owner, original.id(), UUID.randomUUID(), token)));
    assertOneTransitionAndOneStale(original, results, 3, 2);
  }

  @Test
  void s26_openOwnedSessionHasNoClosureReceipt() {
    var original = startOwn();
    assertThatThrownBy(() -> new ReadWorkSessionChanges(store).closure(owner, original.id()))
        .isInstanceOf(WorkSessionChangeNotFoundException.class);
    assertThat(store.active(owner)).contains(original);
  }

  @Test
  void s27_closureReadCommitFailureIsNotAbsence() {
    var original = startOwn();
    var failing = new PostgresWorkSessionStore(jdbc, failingReadEnd(), json);
    assertThatThrownBy(() -> new ReadWorkSessionChanges(failing).closure(owner, original.id()))
        .isInstanceOf(StorageUnavailableException.class);
  }

  @Test
  void s27_closureSqlFailureIsNotAbsence() {
    var original = startOwn();
    jdbc.execute("ALTER TABLE work_session_changes RENAME TO unavailable_changes");
    try {
      assertThatThrownBy(() -> new ReadWorkSessionChanges(store).closure(owner, original.id()))
          .isInstanceOf(StorageUnavailableException.class);
    } finally {
      jdbc.execute("ALTER TABLE unavailable_changes RENAME TO work_session_changes");
    }
  }

  @Test
  void s24_closedSnapshotKeepsFinalNetWhenClockMovesBackwards() {
    var original = startOwn();
    var receipt = closeOwn(original).receipt();
    var previous = jdbc.queryForMap("SELECT * FROM work_sessions WHERE id=?", original.id());
    var at = original.startedAt().minusSeconds(1);
    var snapshot =
        new ReadWorkSessionState(store, Clock.fixed(at, ZoneOffset.UTC)).read(owner, original.id());
    assertThat(snapshot.state()).isEqualTo(receipt.after());
    assertThat(snapshot.netMicroseconds()).isEqualTo(1000000);
    assertThat(snapshot.serverNow()).isEqualTo(at);
    assertThat(jdbc.queryForMap("SELECT * FROM work_sessions WHERE id=?", original.id()))
        .isEqualTo(previous);
  }

  @Test
  void s22_crossSessionCollisionRollsBackBeforeFreshIntentLookup() {
    var first = startOwn();
    var key = UUID.randomUUID();
    var closed =
        new ChangeWorkSession(store, Clock.fixed(first.startedAt().plusSeconds(1), ZoneOffset.UTC))
            .close(
                owner,
                first.id(),
                key,
                new WorkSessionRevision(first.id(), 1),
                new com.apptolast.organization.domain.WorkSessionCloseNotes("Original", ""));
    var next = startOwn();
    var previous = jdbc.queryForMap("SELECT * FROM work_sessions WHERE id=?", next.id());
    var transactions = new ArrayList<Long>();
    var firstLookup = new java.util.concurrent.atomic.AtomicBoolean(true);
    var connection =
        new JdbcTemplate(jdbc.getDataSource()) {
          @Override
          public <T> List<T> query(
              String sql, org.springframework.jdbc.core.RowMapper<T> mapper, Object... args) {
            if (sql.equals(
                "SELECT receipt FROM work_session_changes WHERE owner_id=? AND request_key=?")) {
              transactions.add(queryForObject("SELECT txid_current()", Long.class));
              if (firstLookup.getAndSet(false)) return List.of();
            }
            return super.query(sql, mapper, args);
          }
        };
    var contender = new PostgresWorkSessionStore(connection, manager, json);
    assertThatThrownBy(
            () ->
                new ChangeWorkSession(
                        contender, Clock.fixed(next.startedAt().plusSeconds(1), ZoneOffset.UTC))
                    .close(
                        owner,
                        next.id(),
                        key,
                        new WorkSessionRevision(next.id(), 1),
                        new com.apptolast.organization.domain.WorkSessionCloseNotes(
                            "Original", "")))
        .isInstanceOf(WorkSessionIdempotencyConflictException.class);
    assertThat(transactions).hasSize(2).doesNotHaveDuplicates();
    assertThat(jdbc.queryForMap("SELECT * FROM work_sessions WHERE id=?", next.id()))
        .isEqualTo(previous);
    assertThat(store.active(owner)).contains(next);
    assertThat(store.changeByRequest(owner, key)).contains(closed.receipt());
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_session_changes WHERE session_id=?",
                Integer.class,
                next.id()))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_session_intervals WHERE session_id=?",
                Integer.class,
                next.id()))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE aggregate_id=?",
                Integer.class,
                next.id()))
        .isEqualTo(1);
  }

  @Test
  void s25_snapshotExcludesAConcurrentCloseThatCommitsBeforeClock() {
    var original = startOwn();
    var clock = org.mockito.Mockito.mock(Clock.class);
    try (var worker = java.util.concurrent.Executors.newSingleThreadExecutor()) {
      org.mockito.Mockito.when(clock.instant())
          .thenAnswer(
              call -> {
                var writer = worker.submit(() -> closeOwn(original));
                assertThat(
                        writer
                            .get(5, java.util.concurrent.TimeUnit.SECONDS)
                            .receipt()
                            .after()
                            .status())
                    .isEqualTo("closed");
                assertThat(
                        jdbc.queryForObject(
                            "SELECT status FROM work_sessions WHERE id=?",
                            String.class,
                            original.id()))
                    .isEqualTo("running");
                return original.startedAt().plusSeconds(2);
              });
      var snapshot = new ReadWorkSessionState(store, clock).read(owner, original.id());
      assertThat(snapshot.state().status()).isEqualTo("running");
      assertThat(snapshot.state().revision()).isEqualTo(1);
      assertThat(snapshot.netMicroseconds()).isEqualTo(2000000);
      org.mockito.Mockito.verify(clock).instant();
      org.mockito.Mockito.verifyNoMoreInteractions(clock);
    }
    var fresh =
        new ReadWorkSessionState(
                store, Clock.fixed(original.startedAt().plusSeconds(3), ZoneOffset.UTC))
            .read(owner, original.id());
    assertThat(fresh.state().status()).isEqualTo("closed");
    assertThat(fresh.state().revision()).isEqualTo(2);
    assertThat(fresh.netMicroseconds()).isEqualTo(1000000);
  }

  @Test
  void s19_startCannotConsumeAPlaceBeforeCloseCommits() throws Exception {
    var original = startOwn();
    var written = new java.util.concurrent.CountDownLatch(1);
    var release = new java.util.concurrent.CountDownLatch(1);
    try (var worker = java.util.concurrent.Executors.newSingleThreadExecutor()) {
      var closing =
          worker.submit(
              () ->
                  new org.springframework.transaction.support.TransactionTemplate(manager)
                      .execute(
                          status -> {
                            var result = closeOwn(original);
                            written.countDown();
                            try {
                              if (!release.await(10, java.util.concurrent.TimeUnit.SECONDS))
                                throw new AssertionError("close release timed out");
                            } catch (InterruptedException interrupted) {
                              Thread.currentThread().interrupt();
                              throw new AssertionError(interrupted);
                            }
                            return result;
                          }));
      try {
        assertThat(written.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        assertThat(closing).isNotDone();
        assertThatThrownBy(this::startOwn).isInstanceOf(WorkSessionAlreadyActiveException.class);
        assertThat(store.active(owner)).contains(original);
      } finally {
        release.countDown();
      }
      assertThat(closing.get(10, java.util.concurrent.TimeUnit.SECONDS).receipt().after().status())
          .isEqualTo("closed");
    }
    var next = startOwn();
    assertThat(store.active(owner)).contains(next);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_sessions WHERE owner_id=? AND status IN ('running','paused')",
                Integer.class,
                owner))
        .isEqualTo(1);
  }

  @Test
  void s19_rolledBackCloseKeepsPlaceAgainstConcurrentStart() throws Exception {
    var original = startOwn();
    var written = new java.util.concurrent.CountDownLatch(1);
    var release = new java.util.concurrent.CountDownLatch(1);
    try (var worker = java.util.concurrent.Executors.newSingleThreadExecutor()) {
      var closing =
          worker.submit(
              () ->
                  new org.springframework.transaction.support.TransactionTemplate(manager)
                      .execute(
                          status -> {
                            var result = closeOwn(original);
                            status.setRollbackOnly();
                            written.countDown();
                            try {
                              if (!release.await(10, java.util.concurrent.TimeUnit.SECONDS))
                                throw new AssertionError("close release timed out");
                            } catch (InterruptedException interrupted) {
                              Thread.currentThread().interrupt();
                              throw new AssertionError(interrupted);
                            }
                            return result;
                          }));
      try {
        assertThat(written.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        assertThat(closing).isNotDone();
        assertThatThrownBy(this::startOwn).isInstanceOf(WorkSessionAlreadyActiveException.class);
        assertThat(store.active(owner)).contains(original);
      } finally {
        release.countDown();
      }
      assertThat(closing.get(10, java.util.concurrent.TimeUnit.SECONDS).receipt().after().status())
          .isEqualTo("closed");
    }
    assertThatThrownBy(this::startOwn).isInstanceOf(WorkSessionAlreadyActiveException.class);
    assertThat(store.active(owner)).contains(original);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_sessions WHERE owner_id=? AND status IN ('running','paused')",
                Integer.class,
                owner))
        .isEqualTo(1);
  }

  @Test
  void s20_otherOwnerAndContextLocksDoNotBlockMyClose() {
    var mine = startOwn();
    var otherOwner = UUID.randomUUID().toString();
    var otherProject = UUID.randomUUID();
    var otherTask = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,?,'Other','','active',now(),now())",
        otherProject,
        otherOwner);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,'Other','','pending',now(),now())",
        otherTask,
        otherProject);
    var other =
        new StartWorkSession(store, Clock.systemUTC(), () -> Set.of("UTC"))
            .start(otherOwner, otherProject, otherTask, UUID.randomUUID(), 25)
            .session();
    try (var worker = java.util.concurrent.Executors.newSingleThreadExecutor()) {
      new org.springframework.transaction.support.TransactionTemplate(manager)
          .execute(
              status -> {
                jdbc.queryForObject(
                    "SELECT id FROM work_sessions WHERE id=? FOR UPDATE", UUID.class, other.id());
                jdbc.queryForObject(
                    "SELECT id FROM projects WHERE id=? FOR UPDATE", UUID.class, project);
                jdbc.queryForObject("SELECT id FROM tasks WHERE id=? FOR UPDATE", UUID.class, task);
                var result = worker.submit(() -> closeOwn(mine));
                try {
                  assertThat(
                          result
                              .get(5, java.util.concurrent.TimeUnit.SECONDS)
                              .receipt()
                              .after()
                              .status())
                      .isEqualTo("closed");
                } catch (Exception error) {
                  throw new AssertionError(error);
                }
                return null;
              });
    }
  }

  @Test
  void s15_startPauseAndResumeReplaysSurviveCloseAndNewActiveSession() {
    var original = startOwn();
    var startKey =
        jdbc.queryForObject(
            "SELECT request_key FROM work_sessions WHERE id=?", UUID.class, original.id());
    var pauseKey = UUID.randomUUID();
    var resumeKey = UUID.randomUUID();
    var pause = pauseOwn(original, pauseKey).receipt();
    var resume =
        new ChangeWorkSession(store, Clock.fixed(pause.occurredAt().plusSeconds(1), ZoneOffset.UTC))
            .resume(owner, original.id(), resumeKey, new WorkSessionRevision(original.id(), 2))
            .receipt();
    new ChangeWorkSession(store, Clock.fixed(resume.occurredAt().plusSeconds(1), ZoneOffset.UTC))
        .close(
            owner,
            original.id(),
            UUID.randomUUID(),
            new WorkSessionRevision(original.id(), 3),
            new com.apptolast.organization.domain.WorkSessionCloseNotes("", ""));
    var next = startOwn();
    var clock = org.mockito.Mockito.mock(Clock.class);
    var replay = new ChangeWorkSession(store, clock);
    assertThat(
            replay.pause(owner, original.id(), pauseKey, new WorkSessionRevision(original.id(), 1)))
        .isEqualTo(new WorkSessionTransitionConfirmation(pause, true));
    assertThat(
            replay.resume(
                owner, original.id(), resumeKey, new WorkSessionRevision(original.id(), 2)))
        .isEqualTo(new WorkSessionTransitionConfirmation(resume, true));
    assertThat(
            new StartWorkSession(
                    store,
                    clock,
                    () -> {
                      throw new AssertionError("catalog not needed");
                    })
                .start(owner, project, task, startKey, original.plannedMinutes()))
        .isEqualTo(new WorkSessionConfirmation(original, true));
    assertThat(store.active(owner)).contains(next);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_session_changes WHERE session_id=?",
                Integer.class,
                original.id()))
        .isEqualTo(3);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE aggregate_id=?",
                Integer.class,
                original.id()))
        .isEqualTo(4);
    org.mockito.Mockito.verifyNoInteractions(clock);
  }

  @Test
  void s27_closureUsesOneReadOnlySnapshotAcrossSessionAndReceipt() {
    var original = startOwn();
    try (var worker = java.util.concurrent.Executors.newSingleThreadExecutor()) {
      var connection =
          new JdbcTemplate(jdbc.getDataSource()) {
            @Override
            public <T> List<T> query(
                String sql, org.springframework.jdbc.core.RowMapper<T> mapper, Object... args) {
              if (sql.contains("action='CLOSE'")) {
                assertThat(queryForObject("SHOW transaction_read_only", String.class))
                    .isEqualTo("on");
                assertThat(queryForObject("SHOW transaction_isolation", String.class))
                    .isEqualTo("repeatable read");
                try {
                  assertThat(
                          worker
                              .submit(() -> closeOwn(original))
                              .get(5, java.util.concurrent.TimeUnit.SECONDS)
                              .receipt()
                              .after()
                              .status())
                      .isEqualTo("closed");
                } catch (Exception error) {
                  throw new AssertionError(error);
                }
              }
              return super.query(sql, mapper, args);
            }
          };
      var reader =
          new ReadWorkSessionChanges(new PostgresWorkSessionStore(connection, manager, json));
      assertThatThrownBy(() -> reader.closure(owner, original.id()))
          .isInstanceOf(WorkSessionChangeNotFoundException.class);
    }
    assertThat(store.closure(owner, original.id())).isPresent();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_session_changes WHERE session_id=?",
                Integer.class,
                original.id()))
        .isEqualTo(1);
  }

  @Test
  void s21_closeSqlErrorRollsBackEarlierWrites() {
    var original = startOwn();
    var previous = jdbc.queryForMap("SELECT * FROM work_sessions WHERE id=?", original.id());
    jdbc.execute("ALTER TABLE outbox_events RENAME TO unavailable_outbox");
    try {
      assertThatThrownBy(() -> closeOwn(original)).isInstanceOf(StorageUnavailableException.class);
    } finally {
      jdbc.execute("ALTER TABLE unavailable_outbox RENAME TO outbox_events");
    }
    assertThat(jdbc.queryForMap("SELECT * FROM work_sessions WHERE id=?", original.id()))
        .isEqualTo(previous);
    assertThat(store.active(owner)).contains(original);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_session_changes WHERE session_id=?",
                Integer.class,
                original.id()))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_session_intervals WHERE session_id=?",
                Integer.class,
                original.id()))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE aggregate_id=?",
                Integer.class,
                original.id()))
        .isEqualTo(1);
  }
}
