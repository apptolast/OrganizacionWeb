package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.apptolast.organization.application.StartWorkSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.*;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@Testcontainers
class WorkSessionPersistenceTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  @Test
  void s1_commitsRealStartAndEventWithoutChangingPlanning() throws Exception {
    var source =
        new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    Flyway.configure().dataSource(source).target("13").load().migrate();
    var jdbc = new JdbcTemplate(source);
    var json =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    var project = UUID.randomUUID();
    var task = UUID.randomUUID();
    var key = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES"
            + " (?,'owner','P','','active',now(),now())",
        project);
    jdbc.update(
        "INSERT INTO"
            + " tasks(id,project_id,title,completion_criterion,status,estimated_minutes,created_at,updated_at)"
            + " VALUES (?,?,'T','','pending',40,now(),now())",
        task,
        project);
    jdbc.update(
        "INSERT INTO"
            + " availability_preferences(id,owner_id,zone_id,monday_minutes,tuesday_minutes,wednesday_minutes,thursday_minutes,friday_minutes,saturday_minutes,sunday_minutes,version,created_at,updated_at)"
            + " VALUES (?,'owner','Europe/Madrid',120,120,120,120,120,120,120,0,now(),now())",
        UUID.randomUUID());
    var priorEvent = UUID.randomUUID();
    var priorTime = Instant.parse("2026-09-01T00:00:00Z");
    var priorPayload =
        new com.apptolast.organization.application.ProjectCreated(
            priorEvent, project, "owner", priorTime, 1, "P", "ProjectCreated.v1");
    jdbc.update(
        "INSERT INTO"
            + " outbox_events(event_id,aggregate_id,owner_id,event_type,schema_version,occurred_at,payload)"
            + " VALUES (?, ?,'owner','ProjectCreated.v1',1,?,?::jsonb)",
        priorEvent,
        project,
        java.sql.Timestamp.from(priorTime),
        json.writeValueAsString(priorPayload));
    var priorOutbox = jdbc.queryForMap("SELECT * FROM outbox_events WHERE event_id=?", priorEvent);
    var projectBefore = jdbc.queryForList("SELECT * FROM projects");
    var taskBefore = jdbc.queryForList("SELECT * FROM tasks");
    var availabilityBefore = jdbc.queryForList("SELECT * FROM availability_preferences");
    Flyway.configure().dataSource(source).load().migrate();
    var clock = Clock.fixed(Instant.parse("2026-09-06T10:00:00.123456789Z"), ZoneOffset.UTC);
    var result =
        new StartWorkSession(
                new PostgresWorkSessionStore(jdbc, new DataSourceTransactionManager(source), json),
                clock)
            .start("owner", project, task, key, 25);
    var session = result.session();
    assertThat(result.replayed()).isFalse();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM work_sessions", Integer.class))
        .isEqualTo(1);
    var stored = jdbc.queryForMap("SELECT * FROM work_sessions WHERE id=?", session.id());
    assertThat(stored)
        .containsEntry("owner_id", "owner")
        .containsEntry("request_key", key)
        .containsEntry("project_id", project)
        .containsEntry("task_id", task)
        .containsEntry("planned_minutes", 25)
        .containsEntry("zone_id", "Europe/Madrid")
        .containsEntry("status", "running");
    assertThat(((java.sql.Timestamp) stored.get("started_at")).toInstant())
        .isEqualTo(session.startedAt());
    assertThat(((java.sql.Timestamp) stored.get("planned_end_at")).toInstant())
        .isEqualTo(session.plannedEndAt());
    var event = jdbc.queryForMap("SELECT * FROM outbox_events WHERE aggregate_id=?", session.id());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class))
        .isEqualTo(2);
    assertThat(jdbc.queryForMap("SELECT * FROM outbox_events WHERE event_id=?", priorEvent))
        .isEqualTo(priorOutbox);
    assertThat(event)
        .containsEntry("aggregate_id", session.id())
        .containsEntry("owner_id", "owner")
        .containsEntry("event_type", "WorkSessionStarted.v1")
        .containsEntry("schema_version", 1)
        .containsEntry("status", "pending");
    var payload = json.readTree(event.get("payload").toString());
    assertThat(payload)
        .isEqualTo(
            json.valueToTree(
                new com.apptolast.organization.application.WorkSessionStarted(
                    (UUID) event.get("event_id"),
                    session.id(),
                    "owner",
                    session.startedAt(),
                    1,
                    "WorkSessionStarted.v1",
                    project,
                    task,
                    25,
                    session.plannedEndAt(),
                    "Europe/Madrid")));
    assertThat(jdbc.queryForList("SELECT * FROM projects")).isEqualTo(projectBefore);
    assertThat(jdbc.queryForList("SELECT * FROM tasks")).isEqualTo(taskBefore);
    assertThat(jdbc.queryForList("SELECT * FROM availability_preferences"))
        .isEqualTo(availabilityBefore);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Integer.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_projections", Integer.class))
        .isZero();
  }
}
