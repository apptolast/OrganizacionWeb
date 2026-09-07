package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers
class WorkSessionEndMigrationTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  @Test
  void s22_upgradePreservesHistoricalStatesAndReceiptsWithoutGetBackfill() throws Exception {
    var source =
        new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    Flyway.configure().dataSource(source).target("17").load().migrate();
    var jdbc = new JdbcTemplate(source);
    var json =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    var store = new PostgresWorkSessionStore(jdbc, new DataSourceTransactionManager(source), json);
    var at = Instant.parse("2026-09-07T10:00:00.123456Z");
    var starts = new ArrayList<SessionStart>();
    var startKeys = new ArrayList<UUID>();
    var receipts = new ArrayList<WorkSessionTransitionReceipt>();
    var changeKeys = new ArrayList<UUID>();
    for (var owner : List.of("running", "paused", "closed")) {
      var project = UUID.randomUUID();
      var task = UUID.randomUUID();
      var key = UUID.randomUUID();
      jdbc.update(
          "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,?,'P','','active',now(),now())",
          project,
          owner);
      jdbc.update(
          "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,'T','','pending',now(),now())",
          task,
          project);
      var start =
          new StartWorkSession(store, Clock.fixed(at, ZoneOffset.UTC), () -> Set.of("UTC"))
              .start(owner, project, task, key, 25)
              .session();
      starts.add(start);
      startKeys.add(key);
      if (owner.equals("running")) continue;
      var before = new WorkSessionState(start, "running", 1, at, 0, at);
      var paused = new WorkSessionState(start, "paused", 2, at.plusSeconds(1), 1000000, null);
      receipts.add(seed(jdbc, json, owner, "PAUSE", before, paused, null, changeKeys));
      jdbc.update(
          "INSERT INTO work_session_intervals VALUES (?,2,?,?)",
          start.id(),
          java.sql.Timestamp.from(at),
          java.sql.Timestamp.from(paused.changedAt()));
      if (owner.equals("closed")) {
        var running =
            new WorkSessionState(
                start, "running", 3, at.plusSeconds(2), 1000000, at.plusSeconds(2));
        receipts.add(seed(jdbc, json, owner, "RESUME", paused, running, null, changeKeys));
        var closed = new WorkSessionState(start, "closed", 4, at.plusSeconds(3), 2000000, null);
        var closure =
            new WorkSessionClosure(
                "Avance histórico", "Siguiente", LocalDate.parse("2026-09-07"), "UTC");
        receipts.add(seed(jdbc, json, owner, "CLOSE", running, closed, closure, changeKeys));
        jdbc.update(
            "INSERT INTO work_session_intervals VALUES (?,4,?,?)",
            start.id(),
            java.sql.Timestamp.from(running.changedAt()),
            java.sql.Timestamp.from(closed.changedAt()));
      }
    }
    var rows = jdbc.queryForList("SELECT * FROM work_sessions ORDER BY id");
    var changes = jdbc.queryForList("SELECT * FROM work_session_changes ORDER BY id");
    var intervals =
        jdbc.queryForList("SELECT * FROM work_session_intervals ORDER BY session_id,revision");
    var events = jdbc.queryForList("SELECT * FROM outbox_events ORDER BY event_id");
    Flyway.configure().dataSource(source).load().migrate();
    var migrated = jdbc.queryForList("SELECT * FROM work_sessions ORDER BY id");
    assertThat(migrated).hasSize(rows.size());
    for (int i = 0; i < rows.size(); i++) {
      assertThat(migrated.get(i)).containsAllEntriesOf(rows.get(i));
      assertThat(migrated.get(i))
          .containsEntry("effective_end_at", null)
          .containsEntry("last_decision_at", null);
    }
    for (int i = 0; i < starts.size(); i++) {
      var start = starts.get(i);
      var owner = List.of("running", "paused", "closed").get(i);
      var end =
          new ReadWorkSessionEnd(store, Clock.fixed(at.plusSeconds(4), ZoneOffset.UTC))
              .read(owner, start.id());
      assertThat(end.effectiveEndAt()).isEqualTo(start.plannedEndAt());
      assertThat(end.state().status()).isEqualTo(owner);
      assertThat(store.byRequest(owner, startKeys.get(i))).contains(start);
      store.readEnd(
          owner,
          start.id(),
          context -> {
            assertThat(context.lastDecisionAt()).isEqualTo(end.state().changedAt());
            return end;
          });
    }
    for (int i = 0; i < receipts.size(); i++) {
      var receipt = receipts.get(i);
      var owner = receipt.sessionId().equals(starts.get(1).id()) ? "paused" : "closed";
      assertThat(store.changeDetail(owner, receipt.id())).contains(receipt);
      assertThat(store.changeByRequest(owner, changeKeys.get(i))).contains(receipt);
      var historical =
          json.readTree(
              jdbc.queryForObject(
                  "SELECT receipt::text FROM work_session_changes WHERE id=?",
                  String.class,
                  receipt.id()));
      assertThat(historical.size()).isEqualTo(receipt.action().equals("CLOSE") ? 7 : 6);
      assertThat(historical.has("extension")).isFalse();
    }
    assertThat(jdbc.queryForList("SELECT * FROM work_sessions ORDER BY id")).isEqualTo(migrated);
    assertThat(jdbc.queryForList("SELECT * FROM work_session_changes ORDER BY id"))
        .isEqualTo(changes);
    assertThat(
            jdbc.queryForList("SELECT * FROM work_session_intervals ORDER BY session_id,revision"))
        .isEqualTo(intervals);
    assertThat(jdbc.queryForList("SELECT * FROM outbox_events ORDER BY event_id"))
        .isEqualTo(events);
  }

  private WorkSessionTransitionReceipt seed(
      JdbcTemplate jdbc,
      ObjectMapper json,
      String owner,
      String action,
      WorkSessionState before,
      WorkSessionState after,
      WorkSessionClosure closure,
      List<UUID> keys) {
    var key = UUID.randomUUID();
    keys.add(key);
    var receipt =
        new WorkSessionTransitionReceipt(
            UUID.randomUUID(),
            before.session().id(),
            action,
            after.changedAt(),
            before,
            after,
            closure);
    var tree =
        json.createObjectNode()
            .put("id", receipt.id().toString())
            .put("sessionId", receipt.sessionId().toString())
            .put("action", action)
            .put("occurredAt", receipt.occurredAt().toString());
    tree.set("before", json.valueToTree(before));
    tree.set("after", json.valueToTree(after));
    if (closure != null) tree.set("closure", json.valueToTree(closure));
    jdbc.update(
        "UPDATE work_sessions SET status=?,revision=?,changed_at=?,worked_microseconds=?,running_since=? WHERE id=?",
        after.status(),
        after.revision(),
        java.sql.Timestamp.from(after.changedAt()),
        after.workedMicroseconds(),
        after.runningSince() == null ? null : java.sql.Timestamp.from(after.runningSince()),
        receipt.sessionId());
    jdbc.update(
        "INSERT INTO work_session_changes VALUES (?,?,?,?,?,?,?,?::jsonb)",
        receipt.id(),
        owner,
        receipt.sessionId(),
        key,
        action,
        before.revision(),
        java.sql.Timestamp.from(receipt.occurredAt()),
        tree.toString());
    return receipt;
  }
}
