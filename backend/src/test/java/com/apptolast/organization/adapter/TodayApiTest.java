package com.apptolast.organization.adapter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(
    properties = {
      "app.auth.username=persona-a",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@AutoConfigureMockMvc
class TodayApiTest {
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

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper json;
  @MockitoBean Clock clock;
  @MockitoBean com.apptolast.organization.application.ZoneCatalog catalog;

  @BeforeEach
  void reset() {
    when(catalog.zones()).thenReturn(Set.of("UTC", "Europe/Madrid"));
    when(clock.instant()).thenReturn(Instant.parse("2030-01-07T12:00:00Z"));
    when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    jdbc.execute(
        "TRUNCATE block_changes,block_projections,planned_blocks,availability_preferences,task_status_history,tasks,outbox_events,projects");
    jdbc.update(
        "INSERT INTO availability_preferences(id,owner_id,zone_id,monday_minutes,tuesday_minutes,wednesday_minutes,thursday_minutes,friday_minutes,saturday_minutes,sunday_minutes,version,created_at,updated_at) VALUES (?,'persona-a','UTC',120,120,120,120,120,120,120,0,now(),now())",
        UUID.randomUUID());
  }

  @Test
  void s1_emptySnapshotHasExactClosedSchemaAndDoesNotWrite() throws Exception {
    var before = jdbc.queryForList("SELECT * FROM availability_preferences");
    var response =
        mvc.perform(get("/api/v1/today").with(user("persona-a")))
            .andExpect(status().isOk())
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString()))
        .isEqualTo(
            json.readTree(
                """
      {"serverNow":"2030-01-07T12:00:00Z","date":"2030-01-07","zoneId":"UTC","zoneSource":"AVAILABILITY","availabilityZoneId":"UTC","dayStartAt":"2030-01-07T00:00:00Z","dayEndAt":"2030-01-08T00:00:00Z","budgetMinutes":120,"plannedSeconds":0,"remainingSeconds":7200,"excessSeconds":0,"currentBlockId":null,"nextBlockId":null,"closingAt":null,"items":[]}
      """));
    assertThat(jdbc.queryForList("SELECT * FROM availability_preferences")).isEqualTo(before);
    for (var table : List.of("projects", "tasks", "planned_blocks", "outbox_events"))
      assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class)).isZero();
  }

  @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
  org.springframework.transaction.PlatformTransactionManager transactions;

  UUID seedBlock(String owner, String projectStatus, String taskStatus, String start, String end) {
    var project = UUID.randomUUID();
    var task = UUID.randomUUID();
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,?,'Proyecto actualizado','',?,now(),now())",
        project,
        owner,
        projectStatus);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,completed_at,created_at,updated_at) VALUES (?,?,'Tarea actualizada','',?,?,TIMESTAMPTZ '2030-01-01 00:00:00+00',TIMESTAMPTZ '2030-01-01 00:00:00+00')",
        task,
        project,
        taskStatus,
        taskStatus.equals("completed")
            ? java.sql.Timestamp.from(Instant.parse("2030-01-01T00:00:00Z"))
            : null);
    var from = Instant.parse(start);
    var to = Instant.parse(end);
    jdbc.update(
        "INSERT INTO planned_blocks(id,project_id,task_id,request_key,objective,start_local,end_local,zone_id,start_offset,end_offset,allow_over_budget,start_at,end_at,duration_minutes,created_at) VALUES (?,?,?,?,'Meta',?,?,'UTC','Z','Z',true,?,?,?,?)",
        id,
        project,
        task,
        UUID.randomUUID(),
        LocalDateTime.ofInstant(from, ZoneOffset.UTC),
        LocalDateTime.ofInstant(to, ZoneOffset.UTC),
        java.sql.Timestamp.from(from),
        java.sql.Timestamp.from(to),
        (int) Duration.between(from, to).toMinutes(),
        java.sql.Timestamp.from(Instant.parse("2030-01-07T12:00:00.000001Z")));
    return id;
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "pending,active",
    "completed,active",
    "pending,completed",
    "completed,completed"
  })
  void s8_readsCurrentNamesAndUnmodifiedBlockForEveryEntityState(
      String taskStatus, String projectStatus) throws Exception {
    var id =
        seedBlock(
            "persona-a", projectStatus, taskStatus, "2030-01-07T10:00:00Z", "2030-01-07T11:00:00Z");
    var row = jdbc.queryForMap("SELECT * FROM planned_blocks WHERE id=?", id);
    var response =
        mvc.perform(get("/api/v1/today").with(user("persona-a")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    var result = json.readTree(response.getContentAsString());
    assertThat(result.get("items").size()).isEqualTo(1);
    var item = result.get("items").get(0);
    assertThat(item.size()).isEqualTo(3);
    assertThat(item.get("projectName").asText()).isEqualTo("Proyecto actualizado");
    assertThat(item.get("taskTitle").asText()).isEqualTo("Tarea actualizada");
    var block = item.get("block");
    assertThat(block.size()).isEqualTo(9);
    assertThat(block.get("id").asText()).isEqualTo(id.toString());
    assertThat(block.get("projectId").asText()).isEqualTo(row.get("project_id").toString());
    assertThat(block.get("taskId").asText()).isEqualTo(row.get("task_id").toString());
    assertThat(block.get("objective").asText()).isEqualTo("Meta");
    assertThat(block.get("startAt").asText()).isEqualTo("2030-01-07T10:00:00Z");
    assertThat(block.get("endAt").asText()).isEqualTo("2030-01-07T11:00:00Z");
    assertThat(block.get("zoneId").asText()).isEqualTo("UTC");
    assertThat(block.get("durationMinutes").asInt()).isEqualTo(60);
    assertThat(block.get("createdAt").asText()).isEqualTo("2030-01-07T12:00:00.000001Z");
    assertThat(result.get("plannedSeconds").asLong()).isEqualTo(3600);
    assertThat(jdbc.queryForMap("SELECT * FROM planned_blocks WHERE id=?", id)).isEqualTo(row);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @Test
  void s9_returnsAllTwentyOneSortedBlocksAndWholeDaySummary() throws Exception {
    var ids = new ArrayList<UUID>();
    for (int index = 20; index >= 0; index--) {
      var start = Instant.parse("2030-01-07T08:00:00Z").plusSeconds(index * 1800L);
      ids.addFirst(
          seedBlock(
              "persona-a",
              "active",
              "pending",
              start.toString(),
              start.plusSeconds(900).toString()));
    }
    var response =
        mvc.perform(get("/api/v1/today").with(user("persona-a")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    var result = json.readTree(response.getContentAsString());
    assertThat(result.get("items").size()).isEqualTo(21);
    for (int index = 0; index < 21; index++)
      assertThat(result.get("items").get(index).get("block").get("id").asText())
          .isEqualTo(ids.get(index).toString());
    assertThat(result.has("nextCursor")).isFalse();
    assertThat(result.get("plannedSeconds").asLong()).isEqualTo(18900);
    assertThat(result.get("remainingSeconds").asLong()).isZero();
    assertThat(result.get("excessSeconds").asLong()).isEqualTo(11700);
    assertThat(result.get("currentBlockId").asText()).isEqualTo(ids.get(8).toString());
    assertThat(result.get("nextBlockId").asText()).isEqualTo(ids.get(9).toString());
    assertThat(result.get("closingAt").asText()).isEqualTo("2030-01-07T18:15:00Z");
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
  void s10_snapshotDoesNotMixConcurrentPreferenceNamesOrBlocks(boolean absent) throws Exception {
    if (absent) jdbc.execute("DELETE FROM availability_preferences");
    else
      seedBlock("persona-a", "active", "pending", "2030-01-07T10:00:00Z", "2030-01-07T11:00:00Z");
    var manager =
        new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource());
    var writer = new org.springframework.transaction.support.TransactionTemplate(manager);
    var triggered = new java.util.concurrent.atomic.AtomicBoolean();
    var observed =
        new JdbcTemplate(jdbc.getDataSource()) {
          @Override
          public <T> List<T> query(
              String sql, org.springframework.jdbc.core.RowMapper<T> mapper, Object... args) {
            var result = super.query(sql, mapper, args);
            if (sql.startsWith("SELECT * FROM availability_preferences")
                && triggered.compareAndSet(false, true)) {
              try {
                java.util.concurrent.CompletableFuture.runAsync(
                        () ->
                            writer.executeWithoutResult(
                                status -> {
                                  if (absent)
                                    jdbc.update(
                                        "INSERT INTO availability_preferences(id,owner_id,zone_id,monday_minutes,tuesday_minutes,wednesday_minutes,thursday_minutes,friday_minutes,saturday_minutes,sunday_minutes,version,created_at,updated_at) VALUES (?,'persona-a','Europe/Madrid',30,30,30,30,30,30,30,0,now(),now())",
                                        UUID.randomUUID());
                                  else
                                    jdbc.update(
                                        "UPDATE availability_preferences SET zone_id='Europe/Madrid',monday_minutes=30,version=version+1");
                                  seedBlock(
                                      "persona-a",
                                      "active",
                                      "pending",
                                      "2030-01-07T13:00:00Z",
                                      "2030-01-07T14:00:00Z");
                                  jdbc.update("UPDATE projects SET name='Nombre nuevo'");
                                  jdbc.update("UPDATE tasks SET title='Título nuevo'");
                                }))
                    .get(5, java.util.concurrent.TimeUnit.SECONDS);
              } catch (Exception error) {
                throw new AssertionError(error);
              }
            }
            return result;
          }
        };
    var availability =
        new com.apptolast.organization.adapter.persistence.PostgresAvailabilityStore(
            observed, writer);
    var queries =
        new com.apptolast.organization.adapter.persistence.PostgresTodayQueries(
            availability, observed, manager);
    var result =
        new com.apptolast.organization.application.ReadToday(queries, clock, catalog)
            .get("persona-a");
    assertThat(triggered).isTrue();
    assertThat(result.window().zoneId()).isEqualTo("UTC");
    assertThat(result.window().zoneSource()).isEqualTo(absent ? "UNCONFIGURED" : "AVAILABILITY");
    assertThat(result.items()).hasSize(absent ? 0 : 1);
    assertThat(result.plannedSeconds()).isEqualTo(absent ? 0 : 3600);
    if (!absent) {
      assertThat(result.items().getFirst().projectName()).isEqualTo("Proyecto actualizado");
      assertThat(result.items().getFirst().taskTitle()).isEqualTo("Tarea actualizada");
      assertThat(result.window().budgetMinutes()).isEqualTo(120);
    }
    var next =
        new com.apptolast.organization.application.ReadToday(queries, clock, catalog)
            .get("persona-a");
    assertThat(next.window().zoneId()).isEqualTo("Europe/Madrid");
    assertThat(next.window().budgetMinutes()).isEqualTo(30);
    assertThat(next.items()).hasSize(absent ? 1 : 2);
    assertThat(next.items())
        .allSatisfy(
            item -> {
              assertThat(item.projectName()).isEqualTo("Nombre nuevo");
              assertThat(item.taskTitle()).isEqualTo("Título nuevo");
            });
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @Test
  void s11_capturesClockOnceAndCanonicalizesMicrosBeforeChoosingDay() throws Exception {
    var id =
        seedBlock("persona-a", "active", "pending", "2030-01-07T23:00:00Z", "2030-01-08T00:30:00Z");
    when(clock.instant())
        .thenReturn(
            Instant.parse("2030-01-07T23:59:59.123456789Z"), Instant.parse("2030-01-08T00:00:01Z"));
    org.mockito.Mockito.clearInvocations(clock);
    var response =
        mvc.perform(get("/api/v1/today").with(user("persona-a")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    var result = json.readTree(response.getContentAsString());
    assertThat(result.get("serverNow").asText()).isEqualTo("2030-01-07T23:59:59.123456Z");
    assertThat(result.get("date").asText()).isEqualTo("2030-01-07");
    assertThat(result.get("dayStartAt").asText()).isEqualTo("2030-01-07T00:00:00Z");
    assertThat(result.get("dayEndAt").asText()).isEqualTo("2030-01-08T00:00:00Z");
    assertThat(result.get("currentBlockId").asText()).isEqualTo(id.toString());
    assertThat(result.get("nextBlockId").isNull()).isTrue();
    assertThat(result.get("plannedSeconds").asLong()).isEqualTo(3600);
    org.mockito.Mockito.verify(clock, org.mockito.Mockito.times(1)).instant();
  }

  @Test
  void s12_neverIncludesOtherOwnersBlocksNamesOrCapacity() throws Exception {
    var foreign =
        seedBlock("persona-b", "active", "pending", "2030-01-07T11:00:00Z", "2030-01-07T13:00:00Z");
    jdbc.update("UPDATE projects SET name='Nombre privado B'");
    jdbc.update("UPDATE tasks SET title='Título privado B'");
    jdbc.update(
        "INSERT INTO availability_preferences(id,owner_id,zone_id,monday_minutes,tuesday_minutes,wednesday_minutes,thursday_minutes,friday_minutes,saturday_minutes,sunday_minutes,version,created_at,updated_at) SELECT ?, 'persona-b','Europe/Madrid',0,0,0,0,0,0,0,0,now(),now()",
        UUID.randomUUID());
    var response =
        mvc.perform(get("/api/v1/today").with(user("persona-a")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    var result = json.readTree(response.getContentAsString());
    assertThat(result.get("items")).isEmpty();
    assertThat(result.get("plannedSeconds").asLong()).isZero();
    assertThat(result.get("currentBlockId").isNull()).isTrue();
    assertThat(result.get("nextBlockId").isNull()).isTrue();
    assertThat(result.get("closingAt").isNull()).isTrue();
    assertThat(result.get("budgetMinutes").asInt()).isEqualTo(120);
    assertThat(result.get("zoneId").asText()).isEqualTo("UTC");
    assertThat(response.getContentAsString())
        .doesNotContain(foreign.toString(), "Nombre privado B", "Título privado B", "persona-b");
  }

  @Test
  void s13_authenticationPrecedesUnknownQueryValidation() throws Exception {
    mvc.perform(get("/api/v1/today?ownerId=persona-b"))
        .andExpect(status().isUnauthorized())
        .andExpect(
            header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "date,2030-01-08",
    "zoneId,Europe/Madrid",
    "ownerId,persona-b",
    "cursor,antiguo",
    "extra,''"
  })
  void s14_rejectsEveryClientParameter(String field, String value) throws Exception {
    var before = jdbc.queryForList("SELECT * FROM availability_preferences");
    var response =
        mvc.perform(get("/api/v1/today").queryParam(field, value).with(user("persona-a")))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andReturn()
            .getResponse();
    var result = json.readTree(response.getContentAsString());
    assertThat(result.get("code").asText()).isEqualTo("VALIDATION_ERROR");
    assertThat(result.get("errors").size()).isEqualTo(1);
    assertThat(result.get("errors").get(0).get("field").asText()).isEqualTo(field);
    assertThat(result.has("items")).isFalse();
    assertThat(jdbc.queryForList("SELECT * FROM availability_preferences")).isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"availability_preferences", "planned_blocks", "commit"})
  void s15_storageFailuresNeverBecomeEmptySnapshots(String phase) throws Exception {
    if (phase.equals("commit"))
      org.mockito.Mockito.doAnswer(
              call -> {
                boolean agenda =
                    org.springframework.transaction.support.TransactionSynchronizationManager
                        .isCurrentTransactionReadOnly();
                call.callRealMethod();
                if (agenda)
                  throw new org.springframework.transaction.TransactionSystemException(
                      "private SQL secret");
                return null;
              })
          .when(transactions)
          .commit(org.mockito.ArgumentMatchers.any());
    else jdbc.execute("ALTER TABLE " + phase + " RENAME TO today_unavailable");
    try {
      var response =
          mvc.perform(get("/api/v1/today").with(user("persona-a")))
              .andExpect(status().isServiceUnavailable())
              .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
              .andExpect(
                  header()
                      .string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
              .andReturn()
              .getResponse();
      assertThat(json.readTree(response.getContentAsString()))
          .isEqualTo(
              json.readTree(
                  """
        {"type":"urn:organization:problem:storage_unavailable","title":"El almacenamiento no está disponible. Inténtalo más tarde.","status":503,"code":"STORAGE_UNAVAILABLE"}
        """));
      assertThat(response.getContentAsString())
          .doesNotContain("SELECT", "private", "secret", "today_unavailable");
    } finally {
      if (!phase.equals("commit")) jdbc.execute("ALTER TABLE today_unavailable RENAME TO " + phase);
    }
    assertThat(jdbc.queryForObject("SELECT count(*) FROM availability_preferences", Long.class))
        .isEqualTo(1);
    for (var table : List.of("projects", "tasks", "planned_blocks", "outbox_events"))
      assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class)).isZero();
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
  void s3_httpFallbackPreservesHistoricalBlockAndUnknownCapacity(boolean absent) throws Exception {
    var id =
        seedBlock(
            "persona-a", "completed", "completed", "2030-01-07T11:00:00Z", "2030-01-07T13:00:00Z");
    jdbc.update("UPDATE planned_blocks SET zone_id='Legacy/Retired'");
    if (absent) jdbc.update("DELETE FROM availability_preferences");
    else jdbc.update("UPDATE availability_preferences SET zone_id='Legacy/Retired'");
    var before = jdbc.queryForList("SELECT * FROM planned_blocks");
    var result =
        json.readTree(
            mvc.perform(get("/api/v1/today").with(user("persona-a")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(result.size()).isEqualTo(15);
    assertThat(result.get("zoneId").asText()).isEqualTo("UTC");
    assertThat(result.get("zoneSource").asText())
        .isEqualTo(absent ? "UNCONFIGURED" : "UNAVAILABLE");
    if (absent) assertThat(result.get("availabilityZoneId").isNull()).isTrue();
    else assertThat(result.get("availabilityZoneId").asText()).isEqualTo("Legacy/Retired");
    for (var field : List.of("budgetMinutes", "remainingSeconds", "excessSeconds"))
      assertThat(result.get(field).isNull()).isTrue();
    assertThat(result.get("plannedSeconds").asLong()).isEqualTo(7200);
    assertThat(result.get("currentBlockId").asText()).isEqualTo(id.toString());
    assertThat(result.get("items").size()).isEqualTo(1);
    assertThat(result.at("/items/0/block/zoneId").asText()).isEqualTo("Legacy/Retired");
    assertThat(jdbc.queryForList("SELECT * FROM planned_blocks")).isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @Test
  void s10_postgresSnapshotIsActuallyReadOnlyRepeatableRead() {
    var manager =
        new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource());
    var availability =
        new com.apptolast.organization.adapter.persistence.PostgresAvailabilityStore(
            jdbc, new org.springframework.transaction.support.TransactionTemplate(manager));
    var queries =
        new com.apptolast.organization.adapter.persistence.PostgresTodayQueries(
            availability, jdbc, manager);
    var agenda =
        queries.read(
            "persona-a",
            preference -> {
              assertThat(jdbc.queryForObject("SHOW transaction_read_only", String.class))
                  .isEqualTo("on");
              assertThat(jdbc.queryForObject("SHOW transaction_isolation", String.class))
                  .isEqualTo("repeatable read");
              return com.apptolast.organization.domain.TodayWindow.at(
                  Instant.parse("2030-01-07T12:00:00Z"), preference, Set.of("UTC"));
            });
    assertThat(agenda.items()).isEmpty();
    assertThat(jdbc.queryForObject("SHOW transaction_read_only", String.class)).isEqualTo("off");
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "2030-01-06T23:00:00Z,2030-01-07T00:00:00Z,0,0",
    "2030-01-07T00:00:00Z,2030-01-07T01:00:00Z,1,3600",
    "2030-01-06T23:30:00Z,2030-01-07T00:30:00Z,1,1800",
    "2030-01-07T23:30:00Z,2030-01-08T00:30:00Z,1,1800",
    "2030-01-08T00:00:00Z,2030-01-08T01:00:00Z,0,0"
  })
  void s4_sqlLoadsOnlyPositiveIntersections(String start, String end, int count, long seconds) {
    var id = seedBlock("persona-a", "active", "pending", start, end);
    var rows = new java.util.concurrent.atomic.AtomicReference<List<?>>();
    var observed =
        new JdbcTemplate(jdbc.getDataSource()) {
          @Override
          public <T> List<T> query(
              String sql, org.springframework.jdbc.core.RowMapper<T> mapper, Object... arguments) {
            var result = super.query(sql, mapper, arguments);
            if (sql.contains("FROM planned_blocks")) rows.set(result);
            return result;
          }
        };
    var manager =
        new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource());
    var availability =
        new com.apptolast.organization.adapter.persistence.PostgresAvailabilityStore(
            observed, new org.springframework.transaction.support.TransactionTemplate(manager));
    var queries =
        new com.apptolast.organization.adapter.persistence.PostgresTodayQueries(
            availability, observed, manager);
    var result =
        queries.read(
            "persona-a",
            preference ->
                com.apptolast.organization.domain.TodayWindow.at(
                    Instant.parse("2030-01-07T12:00:00Z"), preference, Set.of("UTC")));
    assertThat(rows.get()).hasSize(count);
    assertThat(result.items()).hasSize(count);
    assertThat(result.plannedSeconds()).isEqualTo(seconds);
    if (count == 1) {
      assertThat(result.items().getFirst().block().id()).isEqualTo(id);
      assertThat(result.items().getFirst().block().time().startAt())
          .isEqualTo(Instant.parse(start));
      assertThat(result.items().getFirst().block().time().endAt()).isEqualTo(Instant.parse(end));
    }
  }
}
