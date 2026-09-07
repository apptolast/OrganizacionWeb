package com.apptolast.organization.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@AutoConfigureMockMvc
@Import(CloseWorkSessionIntegrationTest.Time.class)
class EndTimeIntegrationTest {
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

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  @Autowired AtomicReference<Instant> testInstant;

  @Test
  void s1_s7_s13_s23_extensionRemainsDurableAndRecoverableAfterClose() throws Exception {
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
    var start =
        body(
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
                .getResponse()
                .getContentAsString());
    var id = UUID.fromString(start.get("id").asText());
    var key = UUID.randomUUID();
    var initial = readEnd(id, 1);
    assertThat(initial.size()).isEqualTo(3);
    assertThat(initial.get("state").size()).isEqualTo(6);
    assertThat(initial.get("effectiveEndAt")).isEqualTo(start.get("plannedEndAt"));
    testInstant.set(Instant.parse("2026-09-07T22:30:00.123457999Z"));
    var response =
        mvc.perform(command(id, key, 1, "extend", "{\"additionalMinutes\":15}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();
    var fact = body(response.getContentAsString());
    assertThat(fact.size()).isEqualTo(7);
    assertThat(fact.get("action").asText()).isEqualTo("EXTEND");
    assertThat(fact.get("occurredAt").asText()).isEqualTo("2026-09-07T22:30:00.123457Z");
    assertThat(fact.get("before")).isEqualTo(initial.get("state"));
    var expectedAfter = initial.get("state").deepCopy();
    ((com.fasterxml.jackson.databind.node.ObjectNode) expectedAfter).put("revision", "2");
    assertThat(fact.get("after")).isEqualTo(expectedAfter);
    assertThat(fact.get("extension"))
        .isEqualTo(
            json.valueToTree(
                Map.of(
                    "additionalMinutes",
                    15,
                    "previousEndAt",
                    "2026-09-07T22:54:00.123456Z",
                    "effectiveEndAt",
                    "2026-09-07T23:09:00.123456Z")));
    assertThat(response.getHeader("Location"))
        .isEqualTo("/api/v1/work-session-changes/" + fact.get("id").asText());
    var stored = jdbc.queryForMap("SELECT * FROM work_sessions WHERE id=?", id);
    assertThat(((java.sql.Timestamp) stored.get("effective_end_at")).toInstant())
        .isEqualTo(Instant.parse("2026-09-07T23:09:00.123456Z"));
    assertThat(((java.sql.Timestamp) stored.get("last_decision_at")).toInstant())
        .isEqualTo(Instant.parse("2026-09-07T22:30:00.123457Z"));
    assertThat(stored).containsEntry("revision", 2L).containsEntry("worked_microseconds", 0L);
    assertThat(readEnd(id, 2).get("effectiveEndAt"))
        .isEqualTo(fact.at("/extension/effectiveEndAt"));
    var event =
        body(
            jdbc.queryForObject(
                "SELECT payload::text FROM outbox_events WHERE aggregate_id=? AND event_type='WorkSessionExtended.v1'",
                String.class,
                id));
    assertThat(event.size()).isEqualTo(11);
    assertThat(event.get("aggregateId").asText()).isEqualTo(id.toString());
    assertThat(event.get("effectiveEndAt")).isEqualTo(fact.at("/extension/effectiveEndAt"));
    assertThat(event.get("occurredAt")).isEqualTo(fact.get("occurredAt"));
    testInstant.set(Instant.parse("2026-09-07T22:31:00.123458999Z"));
    mvc.perform(command(id, UUID.randomUUID(), 2, "close", "{}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.after.status").value("closed"));
    var closed = readEnd(id, 3);
    assertThat(closed.at("/state/status").asText()).isEqualTo("closed");
    assertThat(closed.get("effectiveEndAt")).isEqualTo(fact.at("/extension/effectiveEndAt"));
    jdbc.update(
        "DELETE FROM outbox_events WHERE aggregate_id=? AND event_type='WorkSessionExtended.v1'",
        id);
    var tables =
        List.of("work_sessions", "work_session_changes", "work_session_intervals", "outbox_events");
    var beforeRecovery =
        tables.stream()
            .map(
                t ->
                    jdbc.queryForList(
                        "SELECT * FROM "
                            + t
                            + " ORDER BY "
                            + (t.equals("work_session_intervals")
                                ? "session_id,revision"
                                : t.equals("outbox_events") ? "event_id" : "id")))
            .toList();
    var replay =
        mvc.perform(command(id, key, 1, "extend", "{\"additionalMinutes\":15}"))
            .andExpect(status().isOk())
            .andExpect(header().string("Location", response.getHeader("Location")))
            .andReturn()
            .getResponse();
    assertThat(body(replay.getContentAsString())).isEqualTo(fact);
    for (var route :
        List.of(
            "/api/v1/work-session-changes/" + fact.get("id").asText(),
            "/api/v1/work-session-changes/by-request/" + key)) {
      var recovered =
          mvc.perform(get(route).with(user("owner")))
              .andExpect(status().isOk())
              .andExpect(header().doesNotExist("Location"))
              .andReturn()
              .getResponse();
      assertThat(body(recovered.getContentAsString())).isEqualTo(fact);
    }
    mvc.perform(get("/api/v1/work-sessions/" + id + "/end-time"))
        .andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/work-sessions/" + id + "/end-time").with(user("other")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("WORK_SESSION_NOT_FOUND"));
    assertThat(
            tables.stream()
                .map(
                    t ->
                        jdbc.queryForList(
                            "SELECT * FROM "
                                + t
                                + " ORDER BY "
                                + (t.equals("work_session_intervals")
                                    ? "session_id,revision"
                                    : t.equals("outbox_events") ? "event_id" : "id")))
                .toList())
        .isEqualTo(beforeRecovery);
  }

  JsonNode readEnd(UUID id, long revision) throws Exception {
    return body(
        mvc.perform(get("/api/v1/work-sessions/" + id + "/end-time").with(user("owner")))
            .andExpect(status().isOk())
            .andExpect(
                header().string("Work-Session-Revision", "work-session-" + id + "-" + revision))
            .andExpect(header().doesNotExist("ETag"))
            .andExpect(header().doesNotExist("Location"))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  JsonNode body(String value) throws Exception {
    return json.readTree(value);
  }

  MockHttpServletRequestBuilder command(
      UUID id, UUID key, long revision, String action, String body) {
    return post("/api/v1/work-sessions/" + id + "/" + action)
        .with(user("owner"))
        .with(csrf().asHeader())
        .header("Origin", "https://organization.example")
        .header("Idempotency-Key", key)
        .header("Work-Session-Revision", "work-session-" + id + "-" + revision)
        .contentType("application/json")
        .content(body);
  }
}
