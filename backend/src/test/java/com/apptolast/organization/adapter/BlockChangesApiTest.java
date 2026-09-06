package com.apptolast.organization.adapter;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.apptolast.organization.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest(
    properties = {
      "app.auth.username=persona-a",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@AutoConfigureMockMvc
class BlockChangesApiTest {
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  static {
    // PIT repeats JUnit class lifecycles inside one JVM. Keep the database endpoint stable
    // for Spring's cached context; Ryuk removes this JVM-owned container on exit.
    postgres.start();
  }

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    r.add("spring.datasource.username", postgres::getUsername);
    r.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  UUID project;
  UUID parent;

  @BeforeEach
  void setup() {
    jdbc.execute(
        "TRUNCATE block_changes,block_projections,planned_blocks, task_status_history, tasks,"
            + " outbox_events, projects");
    project = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES"
            + " (?,'persona-a','P','','idea',now(),now())",
        project);
    parent = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at)"
            + " VALUES (?, ?, 'Parent', '', 'pending', now(), now())",
        parent,
        project);
  }

  String path() {
    return "/api/v1/projects/" + project + "/tasks/" + parent + "/blocks/changes";
  }

  @Test
  void s16_readsConfirmedEmptyBlockChangeHistory() throws Exception {
    var response =
        mvc.perform(get(path()).with(user("persona-a")))
            .andExpect(status().isOk())
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString()))
        .isEqualTo(json.readTree("{\"items\":[],\"nextCursor\":null}"));
  }

  BlockChangeReceipt cancellation(UUID id, UUID key, Instant occurredAt) throws Exception {
    var block = UUID.randomUUID();
    var start = LocalDateTime.parse("2026-09-02T10:00");
    var end = start.plusHours(1);
    var created = Instant.parse("2026-09-01T00:00:00Z");
    var request =
        new BlockRequest("Meta", start, end, "UTC", ZoneOffset.UTC, ZoneOffset.UTC, false);
    var before =
        new PlannedBlock(
            block,
            project,
            parent,
            request,
            new ResolvedBlockTime(
                start.toInstant(ZoneOffset.UTC),
                end.toInstant(ZoneOffset.UTC),
                ZoneOffset.UTC,
                ZoneOffset.UTC,
                60),
            created);
    jdbc.update(
        "INSERT INTO"
            + " planned_blocks(id,project_id,task_id,request_key,objective,start_local,end_local,zone_id,start_offset,end_offset,allow_over_budget,start_at,end_at,duration_minutes,created_at)"
            + " VALUES (?,?,?,?,?,?,?,'UTC','Z','Z',false,?,?,60,?)",
        block,
        project,
        parent,
        UUID.randomUUID(),
        "Meta",
        start,
        end,
        Timestamp.from(before.time().startAt()),
        Timestamp.from(before.time().endAt()),
        Timestamp.from(created));
    var receipt = new BlockChangeReceipt(id, block, "CANCELLED", 2, occurredAt, before, null);
    jdbc.update(
        "INSERT INTO"
            + " block_changes(id,project_id,task_id,block_id,request_key,kind,version,occurred_at,receipt)"
            + " VALUES (?,?,?,?,?,'CANCELLED',2,?,?::jsonb)",
        id,
        project,
        parent,
        block,
        key,
        Timestamp.from(occurredAt),
        json.writeValueAsString(receipt));
    jdbc.update(
        "INSERT INTO block_projections(block_id,version,status,updated_at) VALUES"
            + " (?,2,'cancelled',?)",
        block,
        Timestamp.from(occurredAt));
    return receipt;
  }

  @Test
  void s18_readsClosedHistoricalReceiptById() throws Exception {
    var receipt =
        cancellation(
            UUID.randomUUID(), UUID.randomUUID(), Instant.parse("2026-09-03T10:00:00.123456Z"));
    var response =
        mvc.perform(get(path() + "/" + receipt.id()).with(user("persona-a")))
            .andExpect(status().isOk())
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse();
    var body = json.readTree(response.getContentAsString());
    assertThat(body.size()).isEqualTo(7);
    assertThat(body.get("id").textValue()).isEqualTo(receipt.id().toString());
    assertThat(body.get("blockId").textValue()).isEqualTo(receipt.blockId().toString());
    assertThat(body.get("kind").textValue()).isEqualTo("CANCELLED");
    assertThat(body.get("revision").textValue()).isEqualTo("\"block:" + receipt.blockId() + ":2\"");
    assertThat(body.get("occurredAt").textValue()).isEqualTo("2026-09-03T10:00:00.123456Z");
    assertThat(body.get("after").isNull()).isTrue();
    assertThat(body.get("before"))
        .isEqualTo(
            json.readTree(
                """
                {"id":"%s","projectId":"%s","taskId":"%s","objective":"Meta",
                 "startAt":"2026-09-02T10:00:00Z","endAt":"2026-09-02T11:00:00Z",
                 "zoneId":"UTC","durationMinutes":60,"createdAt":"2026-09-01T00:00:00Z"}
                """
                    .formatted(receipt.blockId(), project, parent)));
  }

  @Test
  void s18_recoversTheSameReceiptByRequestKeyWithoutLocationRequirement() throws Exception {
    var key = UUID.randomUUID();
    var receipt =
        cancellation(UUID.randomUUID(), key, Instant.parse("2026-09-03T10:00:00.123456Z"));
    var byId =
        mvc.perform(get(path() + "/" + receipt.id()).with(user("persona-a")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var byKey =
        mvc.perform(get(path() + "/by-request/" + key).with(user("persona-a")))
            .andExpect(status().isOk())
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(byKey.getContentAsString())).isEqualTo(json.readTree(byId));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Integer.class))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Integer.class)).isZero();
  }

  @Test
  void s16_pagesTwentyOneReceiptsWithClosedScopedCursorAndNoDuplicates() throws Exception {
    var at = Instant.parse("2026-09-03T10:00:00.123456Z");
    for (int n = 1; n <= 21; n++) cancellation(new UUID(0, n), UUID.randomUUID(), at);
    var first =
        json.readTree(
            mvc.perform(get(path()).with(user("persona-a")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(first.size()).isEqualTo(2);
    assertThat(first.get("items").size()).isEqualTo(20);
    assertThat(first.get("items").get(0).get("id").textValue())
        .isEqualTo(new UUID(0, 21).toString());
    assertThat(first.get("items").get(19).get("id").textValue())
        .isEqualTo(new UUID(0, 2).toString());
    assertThat(first.get("nextCursor").isTextual()).isTrue();
    var cursor = first.get("nextCursor").textValue();
    assertThat(cursor).matches("[A-Za-z0-9_-]+");
    var decoded = json.readTree(java.util.Base64.getUrlDecoder().decode(cursor));
    assertThat(decoded)
        .isEqualTo(
            json.valueToTree(
                java.util.Map.of(
                    "collection",
                    "blockChanges",
                    "projectId",
                    project.toString(),
                    "taskId",
                    parent.toString(),
                    "occurredAt",
                    at.toString(),
                    "id",
                    new UUID(0, 2).toString())));
    var second =
        json.readTree(
            mvc.perform(get(path()).param("cursor", cursor).with(user("persona-a")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(second.get("items").size()).isEqualTo(1);
    assertThat(second.get("items").get(0).get("id").textValue())
        .isEqualTo(new UUID(0, 1).toString());
    assertThat(second.get("nextCursor").isNull()).isTrue();
  }

  @Test
  void s17_rejectsUnknownHistoryQueryBeforeParsingContextIds() throws Exception {
    mvc.perform(
            get(path().replace(project.toString(), "invalid"))
                .param("unknown", "1")
                .with(user("persona-a")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("query"));
  }

  @Test
  void s16_exactlyTwentyReceiptsHaveNoContinuation() throws Exception {
    for (int n = 1; n <= 20; n++)
      cancellation(new UUID(0, n), UUID.randomUUID(), Instant.parse("2026-09-03T10:00:00Z"));
    var body =
        json.readTree(
            mvc.perform(get(path()).with(user("persona-a")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(body.get("items").size()).isEqualTo(20);
    assertThat(body.get("nextCursor").isNull()).isTrue();
  }

  @Test
  void s17_rejectsRepeatedOtherwiseValidCursor() throws Exception {
    var cursor =
        java.util.Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                json.writeValueAsBytes(
                    java.util.Map.of(
                        "collection",
                        "blockChanges",
                        "projectId",
                        project.toString(),
                        "taskId",
                        parent.toString(),
                        "occurredAt",
                        "2026-09-03T10:00:00Z",
                        "id",
                        UUID.randomUUID().toString())));
    mvc.perform(get(path()).param("cursor", cursor, cursor).with(user("persona-a")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("cursor"));
  }

  @Test
  void s18_missingReceiptIdReturnsItsPublic404WithoutStorageDetails() throws Exception {
    var response =
        mvc.perform(get(path() + "/" + UUID.randomUUID()).with(user("persona-a")))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(jsonPath("$.code").value("BLOCK_CHANGE_NOT_FOUND"))
            .andReturn()
            .getResponse();
    assertThat(response.getContentAsString()).doesNotContain("exception", "SELECT", "stackTrace");
  }

  @Test
  void s18_rejectsReceiptIdQueryBeforeParsingIds() throws Exception {
    mvc.perform(
            get(path().replace(project.toString(), "invalid") + "/invalid")
                .param("cursor", "ignored")
                .with(user("persona-a")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("query"));
  }

  @Test
  void s18_rejectsReceiptKeyQueryBeforeParsingIds() throws Exception {
    mvc.perform(
            get(path().replace(project.toString(), "invalid") + "/by-request/invalid")
                .param("cursor", "ignored")
                .with(user("persona-a")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("query"));
  }

  @Test
  void s18_missingReceiptKeyReturnsItsDistinct404() throws Exception {
    mvc.perform(get(path() + "/by-request/" + UUID.randomUUID()).with(user("persona-a")))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(jsonPath("$.code").value("BLOCK_CHANGE_NOT_FOUND"));
  }

  @Test
  void s18_foreignHistoryIsNotAnEmptySuccessfulPage() throws Exception {
    var receipt =
        cancellation(UUID.randomUUID(), UUID.randomUUID(), Instant.parse("2026-09-03T10:00:00Z"));
    var response =
        mvc.perform(get(path()).with(user("persona-b")))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
            .andReturn()
            .getResponse();
    assertThat(response.getContentAsString())
        .doesNotContain(receipt.id().toString(), receipt.blockId().toString(), "Meta", "items");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void s18_historyStorageFailureReturns503WithoutSqlOrReceipt() throws Exception {
    var receipt =
        cancellation(UUID.randomUUID(), UUID.randomUUID(), Instant.parse("2026-09-03T10:00:00Z"));
    jdbc.execute("ALTER TABLE block_changes RENAME TO unavailable_block_changes");
    try {
      var response =
          mvc.perform(get(path()).with(user("persona-a")))
              .andExpect(status().isServiceUnavailable())
              .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
              .andExpect(
                  header()
                      .string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
              .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"))
              .andReturn()
              .getResponse();
      assertThat(response.getContentAsString())
          .doesNotContain(
              "SELECT",
              "block_changes",
              "exception",
              "stackTrace",
              receipt.id().toString(),
              "Meta");
    } finally {
      jdbc.execute("ALTER TABLE unavailable_block_changes RENAME TO block_changes");
    }
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void s18_foreignKeyContextPrecedesReceiptAbsence() throws Exception {
    var key = UUID.randomUUID();
    var receipt = cancellation(UUID.randomUUID(), key, Instant.parse("2026-09-03T10:00:00Z"));
    var response =
        mvc.perform(get(path() + "/by-request/" + key).with(user("persona-b")))
            .andExpect(status().isNotFound())
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
            .andReturn()
            .getResponse();
    assertThat(response.getContentAsString())
        .doesNotContain(receipt.id().toString(), key.toString(), "Meta");
  }

  @Test
  void s18_receiptIdCannotCrossTaskWithinOwnProject() throws Exception {
    var receipt =
        cancellation(UUID.randomUUID(), UUID.randomUUID(), Instant.parse("2026-09-03T10:00:00Z"));
    var otherTask = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at)"
            + " VALUES (?,?,'Other','','pending',now(),now())",
        otherTask,
        project);
    var response =
        mvc.perform(
                get(path().replace(parent.toString(), otherTask.toString()) + "/" + receipt.id())
                    .with(user("persona-a")))
            .andExpect(status().isNotFound())
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andExpect(jsonPath("$.code").value("BLOCK_CHANGE_NOT_FOUND"))
            .andReturn()
            .getResponse();
    assertThat(response.getContentAsString())
        .doesNotContain(receipt.id().toString(), receipt.blockId().toString(), "Meta");
  }

  @Test
  void s17_historyRejectsAnotherCollectionWithItsOwnTimestampField() throws Exception {
    var cursor =
        java.util.Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                json.writeValueAsBytes(
                    java.util.Map.of(
                        "collection",
                        "blocks",
                        "projectId",
                        project.toString(),
                        "taskId",
                        parent.toString(),
                        "occurredAt",
                        "2026-09-03T10:00:00Z",
                        "id",
                        UUID.randomUUID().toString())));
    mvc.perform(get(path()).param("cursor", cursor).with(user("persona-a")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("cursor"));
  }

  @Test
  void s19_anonymousHistoryDoesNotReachQueryValidation() throws Exception {
    mvc.perform(get(path()).param("unknown", "1"))
        .andExpect(status().isUnauthorized())
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
        .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
  }
}
