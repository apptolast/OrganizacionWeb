package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.application.*;
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
class WorkSessionTransitionMigrationTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  @Test
  void s1_upgradePreservesPublishedStartAndDerivesStateWithoutGetWrites() {
    var source =
        new DriverManagerDataSource(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    Flyway.configure().dataSource(source).target("15").load().migrate();
    var jdbc = new JdbcTemplate(source);
    var manager = new DataSourceTransactionManager(source);
    var json =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    var project = UUID.randomUUID();
    var task = UUID.randomUUID();
    var key = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner','P','','active',now(),now())",
        project);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at) VALUES (?,?,'T','','pending',now(),now())",
        task,
        project);
    var store = new PostgresWorkSessionStore(jdbc, manager, json);
    var at = Instant.parse("2026-09-07T10:00:00.123456Z");
    var original =
        new StartWorkSession(store, Clock.fixed(at, ZoneOffset.UTC), () -> Set.of("UTC"))
            .start("owner", project, task, key, 25)
            .session();
    var before = jdbc.queryForMap("SELECT * FROM work_sessions WHERE id=?", original.id());
    var events = jdbc.queryForList("SELECT * FROM outbox_events");
    Flyway.configure().dataSource(source).load().migrate();
    var migrated = jdbc.queryForMap("SELECT * FROM work_sessions WHERE id=?", original.id());
    for (var entry : before.entrySet())
      assertThat(migrated).containsEntry(entry.getKey(), entry.getValue());
    var snapshot =
        new ReadWorkSessionState(store, Clock.fixed(at.plusSeconds(1), ZoneOffset.UTC))
            .read("owner", original.id());
    assertThat(snapshot.state().revision()).isEqualTo(1);
    assertThat(snapshot.state().status()).isEqualTo("running");
    assertThat(snapshot.state().changedAt()).isEqualTo(at);
    assertThat(snapshot.state().runningSince()).isEqualTo(at);
    assertThat(snapshot.state().workedMicroseconds()).isZero();
    assertThat(snapshot.netMicroseconds()).isEqualTo(1000000);
    assertThat(store.byRequest("owner", key)).contains(original);
    assertThat(jdbc.queryForMap("SELECT * FROM work_sessions WHERE id=?", original.id()))
        .isEqualTo(migrated);
    assertThat(jdbc.queryForList("SELECT * FROM outbox_events")).isEqualTo(events);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM work_session_intervals", Integer.class))
        .isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM work_session_changes", Integer.class))
        .isZero();
  }
}
