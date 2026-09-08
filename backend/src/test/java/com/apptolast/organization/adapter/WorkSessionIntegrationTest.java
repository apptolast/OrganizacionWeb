package com.apptolast.organization.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
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
@Import(WorkSessionIntegrationTest.Time.class)
class WorkSessionIntegrationTest {
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  static {
    postgres.start();
  }

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    r.add("spring.datasource.username", postgres::getUsername);
    r.add("spring.datasource.password", postgres::getPassword);
  }

  @TestConfiguration
  static class Time {
    @Bean
    @Primary
    Clock fixedClock() {
      return Clock.fixed(Instant.parse("2026-09-06T10:00:00.123456789Z"), ZoneOffset.UTC);
    }
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  final UUID project = UUID.randomUUID(), task = UUID.randomUUID(), key = UUID.randomUUID();

  @BeforeEach
  void seed() {
    jdbc.execute(
        "TRUNCATE project_custom_field_values,task_custom_field_values, work_session_intervals,work_session_changes,work_sessions,block_changes,block_projections,planned_blocks,availability_preferences,task_status_history,tasks,outbox_events,projects");
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
  }

  @Test
  void s14_httpReplayReturnsOriginalStartWithoutAnotherWrite() throws Exception {
    var created =
        mvc.perform(startRequest()).andExpect(status().isCreated()).andReturn().getResponse();
    var sessionsBefore = jdbc.queryForList("SELECT * FROM work_sessions");
    var eventsBefore = jdbc.queryForList("SELECT * FROM outbox_events");
    var replay =
        mvc.perform(startRequest())
            .andExpect(status().isOk())
            .andExpect(header().string("Location", created.getHeader("Location")))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(replay.getContentAsString()))
        .isEqualTo(json.readTree(created.getContentAsString()));
    assertThat(jdbc.queryForList("SELECT * FROM work_sessions")).isEqualTo(sessionsBefore);
    assertThat(jdbc.queryForList("SELECT * FROM outbox_events")).isEqualTo(eventsBefore);
  }

  @Test
  void s21_readsRecoverTheSameCommittedStartWithoutWriting() throws Exception {
    var created =
        mvc.perform(startRequest()).andExpect(status().isCreated()).andReturn().getResponse();
    var expected = json.readTree(created.getContentAsString());
    var sessionsBefore = jdbc.queryForList("SELECT * FROM work_sessions");
    var eventsBefore = jdbc.queryForList("SELECT * FROM outbox_events");
    for (var route :
        List.of(created.getHeader("Location"), "/api/v1/work-sessions/by-request/" + key)) {
      var found =
          mvc.perform(get(route).with(user("owner")))
              .andExpect(status().isOk())
              .andExpect(header().doesNotExist("Location"))
              .andExpect(
                  header()
                      .string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
              .andReturn()
              .getResponse();
      assertThat(json.readTree(found.getContentAsString())).isEqualTo(expected);
    }
    var active =
        mvc.perform(get("/api/v1/work-sessions/active").with(user("owner")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    var envelope = json.createObjectNode().set("session", expected);
    assertThat(json.readTree(active.getContentAsString())).isEqualTo(envelope);
    assertThat(jdbc.queryForList("SELECT * FROM work_sessions")).isEqualTo(sessionsBefore);
    assertThat(jdbc.queryForList("SELECT * FROM outbox_events")).isEqualTo(eventsBefore);
  }

  org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder startRequest() {
    return post("/api/v1/projects/" + project + "/tasks/" + task + "/work-sessions")
        .with(user("owner"))
        .with(csrf().asHeader())
        .header("Origin", "https://organization.example")
        .header("Idempotency-Key", key)
        .contentType("application/json")
        .content("{\"plannedMinutes\":25}");
  }

  @Test
  void s1_httpStartCommitsSessionAndEventWithMicroseconds() throws Exception {
    var projectBefore = jdbc.queryForList("SELECT * FROM projects");
    var taskBefore = jdbc.queryForList("SELECT * FROM tasks");
    var availabilityBefore = jdbc.queryForList("SELECT * FROM availability_preferences");
    var response =
        mvc.perform(
                post("/api/v1/projects/" + project + "/tasks/" + task + "/work-sessions")
                    .with(user("owner"))
                    .with(csrf().asHeader())
                    .header("Origin", "https://organization.example")
                    .header("Idempotency-Key", key)
                    .contentType("application/json")
                    .content("{\"plannedMinutes\":25}"))
            .andExpect(status().isCreated())
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse();
    var body = json.readTree(response.getContentAsString());
    var id = UUID.fromString(body.get("id").asText());
    assertThat(body)
        .isEqualTo(
            json.valueToTree(
                Map.of(
                    "id",
                    id,
                    "projectId",
                    project,
                    "taskId",
                    task,
                    "startedAt",
                    "2026-09-06T10:00:00.123456Z",
                    "plannedMinutes",
                    25,
                    "plannedEndAt",
                    "2026-09-06T10:25:00.123456Z",
                    "zoneId",
                    "Europe/Madrid")));
    assertThat(response.getHeader("Location")).isEqualTo("/api/v1/work-sessions/" + id);
    var stored = jdbc.queryForMap("SELECT * FROM work_sessions");
    assertThat(stored)
        .containsEntry("id", id)
        .containsEntry("owner_id", "owner")
        .containsEntry("request_key", key)
        .containsEntry("status", "running")
        .containsEntry("planned_minutes", 25);
    assertThat(((java.sql.Timestamp) stored.get("started_at")).toInstant())
        .isEqualTo(Instant.parse(body.get("startedAt").asText()));
    assertThat(((java.sql.Timestamp) stored.get("planned_end_at")).toInstant())
        .isEqualTo(Instant.parse(body.get("plannedEndAt").asText()));
    var event = jdbc.queryForMap("SELECT * FROM outbox_events");
    assertThat(event)
        .containsEntry("aggregate_id", id)
        .containsEntry("event_type", "WorkSessionStarted.v1")
        .containsEntry("owner_id", "owner");
    var payload = json.readTree(event.get("payload").toString());
    assertThat(payload.get("aggregateId").asText()).isEqualTo(id.toString());
    assertThat(payload.get("occurredAt")).isEqualTo(body.get("startedAt"));
    assertThat(payload.get("plannedEndAt")).isEqualTo(body.get("plannedEndAt"));
    assertThat(jdbc.queryForList("SELECT * FROM projects")).isEqualTo(projectBefore);
    assertThat(jdbc.queryForList("SELECT * FROM tasks")).isEqualTo(taskBefore);
    assertThat(jdbc.queryForList("SELECT * FROM availability_preferences"))
        .isEqualTo(availabilityBefore);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class)).isZero();
  }
}
