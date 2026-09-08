package com.apptolast.organization.adapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.apptolast.organization.application.CreateApiCredentialUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {
      "app.auth.username=business-owner", "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example", "app.publisher.enabled=false"
    })
@AutoConfigureMockMvc
@Import(ApiCredentialHttpPersistenceTest.FixedClock.class)
class ApiCredentialBusinessCompatibilityTest {
  @DynamicPropertySource
  static void database(DynamicPropertyRegistry properties) {
    ApiCredentialHttpPersistenceTest.database(properties);
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  @Autowired CreateApiCredentialUseCase create;

  @org.junit.jupiter.api.BeforeEach
  void resetOwnQuota() {
    jdbc.update(
        "DELETE FROM api_credential_quotas WHERE credential_id IN (SELECT id FROM api_credentials WHERE owner_id='business-owner')");
    jdbc.update("DELETE FROM api_credentials WHERE owner_id='business-owner'");
    jdbc.update("DELETE FROM api_owner_quotas WHERE owner_id='business-owner'");
  }

  private UUID project(String owner) {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,?,'Original','Description','idea','2026-09-08T11:00:00Z','2026-09-08T11:00:00Z')",
        id,
        owner);
    return id;
  }

  @Test
  void s25_bearerEditWithoutOriginOrCsrfPreservesVersionAndOutboxContract() throws Exception {
    var project = project("business-owner");
    var credential =
        create.create("business-owner", UUID.randomUUID(), "Edit", List.of("projects:write"), 7);
    mvc.perform(
            put("/api/v1/projects/" + project)
                .header("Authorization", "Bearer " + credential.secret())
                .header("If-Match", "\"" + project + ":0\"")
                .contentType("application/json")
                .content("{\"name\":\"  Changed  \",\"description\":\"<b>literal</b>\"}"))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"" + project + ":1\""))
        .andExpect(header().doesNotExist("Set-Cookie"))
        .andExpect(jsonPath("$.name").value("Changed"))
        .andExpect(jsonPath("$.description").value("<b>literal</b>"))
        .andExpect(jsonPath("$.createdAt").value("2026-09-08T11:00:00Z"))
        .andExpect(jsonPath("$.updatedAt").value("2026-09-08T12:00:10Z"));
    assertEquals(
        1L, jdbc.queryForObject("SELECT version FROM projects WHERE id=?", Long.class, project));
    var events =
        jdbc.queryForList(
            "SELECT event_id,event_type,status,payload::text FROM outbox_events WHERE aggregate_id=?",
            project);
    assertEquals(1, events.size());
    var event = events.getFirst();
    assertEquals("ProjectUpdated.v1", event.get("event_type"));
    assertEquals("pending", event.get("status"));
    var payload = json.readTree((String) event.get("payload"));
    assertEquals(7, payload.size());
    assertEquals(event.get("event_id").toString(), payload.get("eventId").textValue());
    assertEquals(project.toString(), payload.get("aggregateId").textValue());
    assertEquals("business-owner", payload.get("ownerId").textValue());
    assertEquals("Changed", payload.get("name").textValue());
    assertEquals("2026-09-08T12:00:10Z", payload.get("occurredAt").textValue());
    assertEquals(1, payload.get("schemaVersion").intValue());
    assertEquals("ProjectUpdated.v1", payload.get("type").textValue());
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT used FROM api_credential_quotas WHERE credential_id=?",
            Integer.class,
            credential.credential().id()));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT used FROM api_owner_quotas WHERE owner_id='business-owner'", Integer.class));
  }

  @Test
  void s25_s29_businessRejectionsKeepRowsAndEventsButConsumeEachAdmittedRequest() throws Exception {
    var own = project("business-owner");
    var foreign = project("other-business-owner");
    var credential =
        create.create(
            "business-owner", UUID.randomUUID(), "Rejected edit", List.of("projects:write"), 7);
    var before =
        jdbc.queryForList(
            "SELECT row_to_json(p)::text || xmin::text || ctid::text FROM projects p WHERE id IN (?,?) ORDER BY id",
            String.class,
            own,
            foreign);
    var ids = List.of(own, own, foreign);
    var statuses = List.of(412, 428, 404);
    var codes = List.of("PROJECT_CONFLICT", "PRECONDITION_REQUIRED", "PROJECT_NOT_FOUND");
    for (int i = 0; i < ids.size(); i++) {
      var request =
          put("/api/v1/projects/" + ids.get(i))
              .header("Authorization", "Bearer " + credential.secret())
              .contentType("application/json")
              .content("{\"name\":\"Forbidden change\",\"description\":\"Must not persist\"}");
      if (i != 1) request.header("If-Match", "\"" + ids.get(i) + (i == 0 ? ":1\"" : ":0\""));
      mvc.perform(request)
          .andExpect(status().is(statuses.get(i)))
          .andExpect(jsonPath("$.code").value(codes.get(i)))
          .andExpect(header().doesNotExist("Set-Cookie"));
      assertEquals(
          before,
          jdbc.queryForList(
              "SELECT row_to_json(p)::text || xmin::text || ctid::text FROM projects p WHERE id IN (?,?) ORDER BY id",
              String.class,
              own,
              foreign));
      assertEquals(
          0,
          jdbc.queryForObject(
              "SELECT count(*) FROM outbox_events WHERE aggregate_id IN (?,?)",
              Integer.class,
              own,
              foreign));
      assertEquals(
          i + 1,
          jdbc.queryForObject(
              "SELECT used FROM api_credential_quotas WHERE credential_id=?",
              Integer.class,
              credential.credential().id()));
      assertEquals(
          i + 1,
          jdbc.queryForObject(
              "SELECT used FROM api_owner_quotas WHERE owner_id='business-owner'", Integer.class));
    }
  }
}
