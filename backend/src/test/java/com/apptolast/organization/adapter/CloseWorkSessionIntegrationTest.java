package com.apptolast.organization.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@AutoConfigureMockMvc
@Import(CloseWorkSessionIntegrationTest.Time.class)
class CloseWorkSessionIntegrationTest {
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  static {
    postgres.start();
  }

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @TestConfiguration
  static class Time {
    @Bean
    AtomicReference<Instant> testInstant() {
      return new AtomicReference<>(Instant.parse("2026-09-07T22:29:00.123456789Z"));
    }

    @Bean
    @Primary
    Clock testClock(AtomicReference<Instant> instant) {
      return new Clock() {
        @Override
        public ZoneId getZone() {
          return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
          return Clock.fixed(instant(), zone);
        }

        @Override
        public Instant instant() {
          return instant.get();
        }
      };
    }
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  @Autowired AtomicReference<Instant> testInstant;

  @Test
  void s1_s26_httpCloseCommitsAndClosureReadRecoversTheSameDurableFact() throws Exception {
    var project = UUID.randomUUID();
    var task = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,'owner','P','','active',now(),now())",
        project);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,estimated_minutes,created_at,updated_at) VALUES (?,?,'T','','pending',40,now(),now())",
        task,
        project);
    jdbc.update(
        "INSERT INTO availability_preferences(id,owner_id,zone_id,monday_minutes,tuesday_minutes,wednesday_minutes,thursday_minutes,friday_minutes,saturday_minutes,sunday_minutes,version,created_at,updated_at) VALUES (?,'owner','Europe/Madrid',120,120,120,120,120,120,120,0,now(),now())",
        UUID.randomUUID());
    var startResponse =
        mvc.perform(
                post("/api/v1/projects/" + project + "/tasks/" + task + "/work-sessions")
                    .with(user("owner"))
                    .with(csrf().asHeader())
                    .header("Origin", "https://organization.example")
                    .header("Idempotency-Key", UUID.randomUUID())
                    .contentType("application/json")
                    .content("{\"plannedMinutes\":25}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();
    var start = json.readTree(startResponse.getContentAsString());
    var id = UUID.fromString(start.get("id").asText());
    var key = UUID.randomUUID();
    testInstant.set(Instant.parse("2026-09-07T22:30:00.123457999Z"));
    var response =
        mvc.perform(
                post("/api/v1/work-sessions/" + id + "/close")
                    .with(user("owner"))
                    .with(csrf().asHeader())
                    .header("Origin", "https://organization.example")
                    .header("Idempotency-Key", key)
                    .header("Work-Session-Revision", "work-session-" + id + "-1")
                    .contentType("application/json")
                    .content(
                        "{\"progressNote\":\"  Avance parcial\\n\",\"nextStep\":\"Continuar\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();
    var fact = json.readTree(response.getContentAsString());
    assertThat(fact.size()).isEqualTo(7);
    assertThat(fact.get("action").asText()).isEqualTo("CLOSE");
    assertThat(fact.get("sessionId").asText()).isEqualTo(id.toString());
    assertThat(fact.get("occurredAt").asText()).isEqualTo("2026-09-07T22:30:00.123457Z");
    assertThat(fact.get("before").get("session")).isEqualTo(start);
    assertThat(fact.get("after").get("session")).isEqualTo(start);
    assertThat(fact.get("after").get("status").asText()).isEqualTo("closed");
    assertThat(fact.get("after").get("revision").asText()).isEqualTo("2");
    assertThat(fact.get("after").get("workedMicroseconds").asText()).isEqualTo("60000001");
    assertThat(fact.get("after").get("runningSince").isNull()).isTrue();
    assertThat(fact.get("closure"))
        .isEqualTo(
            json.valueToTree(
                Map.of(
                    "progressNote",
                    "  Avance parcial\n",
                    "nextStep",
                    "Continuar",
                    "workDate",
                    "2026-09-08",
                    "closeZoneId",
                    "Europe/Madrid")));
    assertThat(response.getHeader("Location"))
        .isEqualTo("/api/v1/work-session-changes/" + fact.get("id").asText());
    var storedReceipt =
        jdbc.queryForMap("SELECT * FROM work_session_changes WHERE request_key=?", key);
    assertThat(json.readTree(storedReceipt.get("receipt").toString()).get("closure"))
        .isEqualTo(fact.get("closure"));
    var stored = jdbc.queryForMap("SELECT * FROM work_sessions WHERE id=?", id);
    assertThat(stored)
        .containsEntry("status", "closed")
        .containsEntry("revision", 2L)
        .containsEntry("worked_microseconds", 60000001L);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM work_session_intervals WHERE session_id=?", Long.class, id))
        .isEqualTo(1);
    var interval = jdbc.queryForMap("SELECT * FROM work_session_intervals WHERE session_id=?", id);
    assertThat(((java.sql.Timestamp) interval.get("start_at")).toInstant())
        .isEqualTo(Instant.parse(start.get("startedAt").asText()));
    assertThat(((java.sql.Timestamp) interval.get("end_at")).toInstant())
        .isEqualTo(Instant.parse(fact.get("occurredAt").asText()));
    var event =
        jdbc.queryForMap(
            "SELECT * FROM outbox_events WHERE aggregate_id=? AND event_type='WorkSessionClosed.v1'",
            id);
    var payload = json.readTree(event.get("payload").toString());
    assertThat(payload.size()).isEqualTo(11);
    assertThat(payload.get("occurredAt")).isEqualTo(fact.get("occurredAt"));
    assertThat(payload.get("workedMicroseconds"))
        .isEqualTo(fact.get("after").get("workedMicroseconds"));
    assertThat(payload.get("workDate")).isEqualTo(fact.get("closure").get("workDate"));
    var tables =
        List.of("work_sessions", "work_session_changes", "work_session_intervals", "outbox_events");
    var beforeReads =
        tables.stream().map(table -> jdbc.queryForList("SELECT * FROM " + table)).toList();
    var found =
        mvc.perform(get("/api/v1/work-sessions/" + id + "/closure").with(user("owner")))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist("Location"))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(found.getContentAsString())).isEqualTo(fact);
    var active =
        mvc.perform(get("/api/v1/work-sessions/active").with(user("owner")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    assertThat(json.readTree(active.getContentAsString()))
        .isEqualTo(json.readTree("{\"session\":null}"));
    assertThat(tables.stream().map(table -> jdbc.queryForList("SELECT * FROM " + table)).toList())
        .isEqualTo(beforeReads);
  }
}
