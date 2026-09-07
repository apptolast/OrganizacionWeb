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
class WorkSessionClosureMigrationTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  @Test
  void s23_upgradePreservesRunningPausedAndAllPublishedFacts() {
    var source =
        new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    Flyway.configure().dataSource(source).target("16").load().migrate();
    var jdbc = new JdbcTemplate(source);
    var json =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    var store = new PostgresWorkSessionStore(jdbc, new DataSourceTransactionManager(source), json);
    var at = Instant.parse("2026-09-07T10:00:00.123456Z");
    var sessions = new ArrayList<SessionStart>();
    var keys = new ArrayList<UUID>();
    for (var owner : List.of("running-owner", "paused-owner")) {
      var project = UUID.randomUUID();
      var task = UUID.randomUUID();
      var key = UUID.randomUUID();
      keys.add(key);
      jdbc.update(
          "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,?,'P','','active',now(),now())",
          project,
          owner);
      jdbc.update(
          "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,'T','','pending',now(),now())",
          task,
          project);
      sessions.add(
          new StartWorkSession(store, Clock.fixed(at, ZoneOffset.UTC), () -> Set.of("UTC"))
              .start(owner, project, task, key, 25)
              .session());
    }
    // Historical V16 fixture: State6 and receipt6, before V18 projection columns existed.
    var session = sessions.get(1);
    var beforePause = new WorkSessionState(session, "running", 1, at, 0, at);
    var afterPause = new WorkSessionState(session, "paused", 2, at.plusSeconds(1), 1000000, null);
    var paused =
        new WorkSessionTransitionReceipt(
            UUID.randomUUID(), session.id(), "PAUSE", at.plusSeconds(1), beforePause, afterPause);
    jdbc.update(
        "UPDATE work_sessions SET status='paused',revision=2,changed_at=?,worked_microseconds=1000000 WHERE id=?",
        java.sql.Timestamp.from(paused.occurredAt()),
        session.id());
    jdbc.update(
        "INSERT INTO work_session_intervals VALUES (?,2,?,?)",
        session.id(),
        java.sql.Timestamp.from(at),
        java.sql.Timestamp.from(paused.occurredAt()));
    jdbc.update(
        "INSERT INTO work_session_changes VALUES (?,?,?,?,'PAUSE',1,?,?::jsonb)",
        paused.id(),
        "paused-owner",
        session.id(),
        UUID.randomUUID(),
        java.sql.Timestamp.from(paused.occurredAt()),
        json.valueToTree(
                Map.of(
                    "id",
                    paused.id(),
                    "sessionId",
                    session.id(),
                    "action",
                    "PAUSE",
                    "occurredAt",
                    paused.occurredAt(),
                    "before",
                    beforePause,
                    "after",
                    afterPause))
            .toString());
    var eventId = UUID.randomUUID();
    var event =
        json.createObjectNode()
            .put("eventId", eventId.toString())
            .put("aggregateId", session.id().toString())
            .put("ownerId", "paused-owner")
            .put("occurredAt", paused.occurredAt().toString())
            .put("schemaVersion", 1)
            .put("type", "WorkSessionStateChanged.v1")
            .put("action", "PAUSE")
            .put("revision", "2")
            .put("fromStatus", "running")
            .put("toStatus", "paused")
            .put("workedMicroseconds", "1000000")
            .putNull("runningSince");
    jdbc.update(
        "INSERT INTO outbox_events(event_id,aggregate_id,owner_id,event_type,schema_version,occurred_at,payload) VALUES (?,?,'paused-owner','WorkSessionStateChanged.v1',1,?,?::jsonb)",
        eventId,
        session.id(),
        java.sql.Timestamp.from(paused.occurredAt()),
        event.toString());
    var rows = jdbc.queryForList("SELECT * FROM work_sessions ORDER BY id");
    var intervals =
        jdbc.queryForList("SELECT * FROM work_session_intervals ORDER BY session_id,revision");
    var receipts = jdbc.queryForList("SELECT * FROM work_session_changes ORDER BY id");
    var events = jdbc.queryForList("SELECT * FROM outbox_events ORDER BY event_id");
    Flyway.configure().dataSource(source).load().migrate();
    assertThat(store.active("running-owner")).contains(sessions.get(0));
    assertThat(store.active("paused-owner")).contains(sessions.get(1));
    assertThat(store.byRequest("running-owner", keys.get(0))).contains(sessions.get(0));
    assertThat(store.byRequest("paused-owner", keys.get(1))).contains(sessions.get(1));
    assertThat(store.changeDetail("paused-owner", paused.id())).contains(paused);
    assertThat(store.closure("running-owner", sessions.get(0).id())).isEmpty();
    assertThat(store.closure("paused-owner", sessions.get(1).id())).isEmpty();
    var migratedRows = jdbc.queryForList("SELECT * FROM work_sessions ORDER BY id");
    assertThat(migratedRows).hasSize(rows.size());
    for (int index = 0; index < rows.size(); index++) {
      assertThat(migratedRows.get(index)).containsAllEntriesOf(rows.get(index));
    }
    assertThat(
            jdbc.queryForList("SELECT * FROM work_session_intervals ORDER BY session_id,revision"))
        .isEqualTo(intervals);
    assertThat(jdbc.queryForList("SELECT * FROM work_session_changes ORDER BY id"))
        .isEqualTo(receipts);
    assertThat(jdbc.queryForList("SELECT * FROM outbox_events ORDER BY event_id"))
        .isEqualTo(events);
    var close = new ChangeWorkSession(store, Clock.fixed(at.plusSeconds(2), ZoneOffset.UTC));
    assertThat(
            close
                .close(
                    "running-owner",
                    sessions.get(0).id(),
                    UUID.randomUUID(),
                    new WorkSessionRevision(sessions.get(0).id(), 1),
                    new WorkSessionCloseNotes("", ""))
                .receipt()
                .after()
                .workedMicroseconds())
        .isEqualTo(2000000);
    assertThat(
            close
                .close(
                    "paused-owner",
                    sessions.get(1).id(),
                    UUID.randomUUID(),
                    new WorkSessionRevision(sessions.get(1).id(), 2),
                    new WorkSessionCloseNotes("", ""))
                .receipt()
                .after()
                .workedMicroseconds())
        .isEqualTo(1000000);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_session_intervals WHERE session_id=?",
                Integer.class,
                sessions.get(1).id()))
        .isEqualTo(1);
  }
}
