package com.apptolast.organization.adapter;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(
    properties = {
      "app.auth.username=persona-a",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example"
    })
@AutoConfigureMockMvc
class RescheduleApiTest {
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
  @Autowired com.apptolast.organization.application.TodayQueries today;

  @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
  org.springframework.transaction.PlatformTransactionManager transactions;

  @org.springframework.test.context.bean.override.mockito.MockitoBean java.time.Clock clock;
  UUID project, task, block, key;

  String base() {
    return "/api/v1/projects/" + project + "/tasks/" + task + "/blocks";
  }

  UUID availability() {
    var id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO"
            + " availability_preferences(id,owner_id,zone_id,monday_minutes,tuesday_minutes,wednesday_minutes,thursday_minutes,friday_minutes,saturday_minutes,sunday_minutes,version,created_at,updated_at)"
            + " VALUES (?,'persona-a','UTC',120,120,120,120,120,120,120,0,now(),now())",
        id);
    return id;
  }

  org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder movement(
      UUID requestKey, UUID preference) {
    return post(base() + "/" + block + "/reschedule")
        .with(user("persona-a"))
        .with(csrf().asHeader())
        .contentType("application/json")
        .content(
            """
            {"startLocal":"2030-01-07T12:00","endLocal":"2030-01-07T13:00","zoneId":"UTC","startOffset":"Z","endOffset":"Z","allowOverBudget":false}
            """)
        .header("If-Match", "\"block:" + block + ":1\"")
        .header("Availability-Revision", "\"availability:" + preference + ":0\"")
        .header("Idempotency-Key", requestKey);
  }

  @Test
  void s2_creationReplayAfterTwoMovesReturnsOnlyTheOriginalFact() throws Exception {
    var preference = availability();
    var original =
        mvc.perform(get(base() + "/by-request/" + key).with(user("persona-a")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    mvc.perform(movement(UUID.randomUUID(), preference)).andExpect(status().isCreated());
    mvc.perform(
            movement(UUID.randomUUID(), preference)
                .with(
                    request -> {
                      request.removeHeader("If-Match");
                      request.addHeader("If-Match", "\"block:" + block + ":2\"");
                      return request;
                    })
                .content(
                    """
                    {"startLocal":"2030-01-07T14:00","endLocal":"2030-01-07T15:00","zoneId":"UTC","startOffset":"Z","endOffset":"Z","allowOverBudget":false}
                    """))
        .andExpect(status().isCreated());
    var projection = jdbc.queryForList("SELECT * FROM block_projections");
    org.mockito.Mockito.clearInvocations(clock);
    var replay =
        mvc.perform(
                creationAtDestination(key, preference)
                    .content(
                        """
                        {"objective":"Preparar borrador","startLocal":"2030-01-07T10:00","endLocal":"2030-01-07T11:00","zoneId":"UTC","startOffset":"Z","endOffset":"Z","allowOverBudget":false}
                        """))
            .andExpect(status().isOk())
            .andExpect(header().string("Location", base() + "/" + block))
            .andReturn()
            .getResponse();
    assertThat(replay.getContentAsString()).isEqualTo(original);
    assertThat(jdbc.queryForList("SELECT * FROM block_projections")).isEqualTo(projection);
    assertThat(
            jdbc.queryForObject(
                "SELECT version FROM block_projections WHERE block_id=?", Long.class, block))
        .isEqualTo(3);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isEqualTo(2);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(2);
    org.mockito.Mockito.verifyNoInteractions(clock);
  }

  @Test
  void s5_previewRequiresBlockRevisionWithoutWriting() throws Exception {
    availability();
    mvc.perform(
            post(base() + "/" + block + "/reschedule/preview")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(
                    """
                    {"startLocal":"2030-01-07T12:00","endLocal":"2030-01-07T13:00","zoneId":"UTC","startOffset":"Z","endOffset":"Z"}
                    """))
        .andExpect(status().is(428))
        .andExpect(jsonPath("$.code").value("PRECONDITION_REQUIRED"));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_projections", Long.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
    org.mockito.Mockito.verifyNoInteractions(clock);
  }

  @Test
  void s19_malformedMovementDocumentPrecedesHistoricalReplay() throws Exception {
    var preference = availability();
    var requestKey = UUID.randomUUID();
    mvc.perform(movement(requestKey, preference)).andExpect(status().isCreated());
    org.mockito.Mockito.clearInvocations(clock);
    mvc.perform(movement(requestKey, preference).content("{} {}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(1);
    org.mockito.Mockito.verifyNoInteractions(clock);
  }

  @Test
  void s19_missingAvailabilityRevisionPrecedesMoveReplay() throws Exception {
    var preference = availability();
    var requestKey = UUID.randomUUID();
    mvc.perform(movement(requestKey, preference)).andExpect(status().isCreated());
    org.mockito.Mockito.clearInvocations(clock);
    mvc.perform(
            movement(requestKey, preference)
                .with(
                    request -> {
                      request.removeHeader("Availability-Revision");
                      return request;
                    }))
        .andExpect(status().is(428))
        .andExpect(jsonPath("$.code").value("PRECONDITION_REQUIRED"));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(1);
    org.mockito.Mockito.verifyNoInteractions(clock);
  }

  @Test
  void s19_moveQueryPrecedesMissingRevisionAndHistoricalReplay() throws Exception {
    var preference = availability();
    var requestKey = UUID.randomUUID();
    mvc.perform(movement(requestKey, preference)).andExpect(status().isCreated());
    org.mockito.Mockito.clearInvocations(clock);
    mvc.perform(
            movement(requestKey, preference)
                .queryParam("unexpected", "x")
                .with(
                    request -> {
                      request.removeHeader("If-Match");
                      return request;
                    }))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("query"));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(1);
    org.mockito.Mockito.verifyNoInteractions(clock);
  }

  @Test
  void s24_movePreviewKeepsItsBudgetWhileCancellationWaits() throws Exception {
    availability();
    var other = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO"
            + " planned_blocks(id,project_id,task_id,request_key,objective,start_local,end_local,zone_id,start_offset,end_offset,allow_over_budget,start_at,end_at,duration_minutes,created_at)"
            + " SELECT ?,project_id,task_id,?,objective,start_local+interval '1"
            + " day',end_local+interval '1"
            + " day',zone_id,start_offset,end_offset,allow_over_budget,start_at+interval '1"
            + " day',end_at+interval '1 day',duration_minutes,created_at FROM planned_blocks WHERE"
            + " id=?",
        other,
        UUID.randomUUID(),
        block);
    var originals = jdbc.queryForList("SELECT * FROM planned_blocks ORDER BY id");
    var triggered = new java.util.concurrent.atomic.AtomicBoolean();
    var writer =
        new java.util.concurrent.atomic.AtomicReference<
            java.util.concurrent.Future<org.springframework.test.web.servlet.MvcResult>>();
    try (var workers = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
      var observed =
          new JdbcTemplate(jdbc.getDataSource()) {
            @Override
            public <T> List<T> query(
                String sql, org.springframework.jdbc.core.RowMapper<T> mapper, Object... args) {
              var result = super.query(sql, mapper, args);
              if (sql.contains("JOIN projects owner_project")
                  && triggered.compareAndSet(false, true)) {
                writer.set(
                    workers.submit(
                        () -> mvc.perform(cancellation(block, UUID.randomUUID())).andReturn()));
                try {
                  awaitDatabaseWaiters("SELECT id FROM availability_preferences WHERE owner_id", 1);
                  assertThat(writer.get().isDone()).isFalse();
                } catch (Exception failure) {
                  throw new AssertionError(failure);
                }
              }
              return result;
            }
          };
      var transaction =
          new org.springframework.transaction.support.TransactionTemplate(
              new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                  jdbc.getDataSource()));
      var availabilityStore =
          new com.apptolast.organization.adapter.persistence.PostgresAvailabilityStore(
              observed, transaction);
      var store =
          new com.apptolast.organization.adapter.persistence.PostgresBlockStore(
              observed, transaction, availabilityStore, json);
      var move =
          new com.apptolast.organization.application.MoveBlock(store, () -> Set.of("UTC"), clock);
      var request =
          new com.apptolast.organization.domain.BlockMoveRequest(
              java.time.LocalDateTime.parse("2030-01-07T14:00"),
              java.time.LocalDateTime.parse("2030-01-07T15:00"),
              "UTC",
              java.time.ZoneOffset.UTC,
              java.time.ZoneOffset.UTC,
              false);
      var first = move.preview("persona-a", project, task, other, 1, request);
      assertThat(first.days().getFirst().plannedSeconds()).isEqualTo(3600);
      assertThat(
              writer.get().get(10, java.util.concurrent.TimeUnit.SECONDS).getResponse().getStatus())
          .isEqualTo(201);
      var next = move.preview("persona-a", project, task, other, 1, request);
      assertThat(next.days().getFirst().plannedSeconds()).isZero();
    }
    assertThat(jdbc.queryForList("SELECT * FROM planned_blocks ORDER BY id")).isEqualTo(originals);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM block_projections WHERE block_id=?", Long.class, other))
        .isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(1);
  }

  @Test
  void s22_creationAfterPreviewOccupiesTheDestinationBeforeMovement() throws Exception {
    var preference = availability();
    var original = jdbc.queryForList("SELECT * FROM planned_blocks WHERE id=?", block);
    mvc.perform(
            post(base() + "/" + block + "/reschedule/preview")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(
                    """
                    {"startLocal":"2030-01-07T12:00","endLocal":"2030-01-07T13:00","zoneId":"UTC","startOffset":"Z","endOffset":"Z"}
                    """)
                .header("If-Match", "\"block:" + block + ":1\""))
        .andExpect(status().isOk());
    var created =
        mvc.perform(creationAtDestination(UUID.randomUUID(), preference))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();
    var occupyingId = json.readTree(created.getContentAsString()).get("id").asText();
    mvc.perform(movement(UUID.randomUUID(), preference))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("BLOCK_OVERLAP"))
        .andExpect(jsonPath("$.conflict.id").value(occupyingId));
    assertThat(jdbc.queryForList("SELECT * FROM planned_blocks WHERE id=?", block))
        .isEqualTo(original);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class)).isEqualTo(2);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_projections", Long.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(1);
  }

  @Test
  void s22_movementWinsDestinationBeforeConcurrentCreation() throws Exception {
    var preference = availability();
    var originals = jdbc.queryForList("SELECT * FROM planned_blocks");
    var responses =
        orderedRequestsWhileBlockLocked(
            () -> movement(UUID.randomUUID(), preference),
            () -> creationAtDestination(UUID.randomUUID(), preference));
    assertThat(responses)
        .extracting(org.springframework.mock.web.MockHttpServletResponse::getStatus)
        .containsExactly(201, 409);
    var rejected = json.readTree(responses.getLast().getContentAsString());
    assertThat(rejected.get("code").asText()).isEqualTo("BLOCK_OVERLAP");
    assertThat(rejected.get("conflict"))
        .isEqualTo(
            json.valueToTree(
                Map.of(
                    "projectId",
                    project.toString(),
                    "taskId",
                    task.toString(),
                    "id",
                    block.toString())));
    assertOneEffectiveChange(originals);
  }

  org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder creationAtDestination(
      UUID requestKey, UUID preference) {
    return post(base())
        .with(user("persona-a"))
        .with(csrf().asHeader())
        .contentType("application/json")
        .content(
            """
            {"objective":"Reservar destino","startLocal":"2030-01-07T12:00","endLocal":"2030-01-07T13:00","zoneId":"UTC","startOffset":"Z","endOffset":"Z","allowOverBudget":false}
            """)
        .header("Availability-Revision", "\"availability:" + preference + ":0\"")
        .header("Idempotency-Key", requestKey);
  }

  @Test
  void s18_stateReadOnlyCompletionFailureReturnsStorageProblemWithoutWrites() throws Exception {
    var originals = jdbc.queryForList("SELECT * FROM planned_blocks");
    org.mockito.Mockito.doAnswer(
            call -> {
              boolean readOnly =
                  org.springframework.transaction.support.TransactionSynchronizationManager
                      .isCurrentTransactionReadOnly();
              call.callRealMethod();
              if (readOnly)
                throw new org.springframework.transaction.TransactionSystemException(
                    "private SQL secret");
              return null;
            })
        .when(transactions)
        .commit(org.mockito.ArgumentMatchers.any());
    var response =
        mvc.perform(get(base() + "/" + block + "/state").with(user("persona-a")))
            .andExpect(status().isServiceUnavailable())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString()))
        .isEqualTo(
            json.readTree(
                """
                {"type":"urn:organization:problem:storage_unavailable","title":"El almacenamiento no está disponible. Inténtalo más tarde.","status":503,"code":"STORAGE_UNAVAILABLE"}
                """));
    assertThat(jdbc.queryForList("SELECT * FROM planned_blocks")).isEqualTo(originals);
    for (var table :
        List.of("availability_preferences", "block_projections", "block_changes", "outbox_events"))
      assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class)).isZero();
  }

  @Test
  void s21_sameKeyForConcurrentCancellationAndMovementIsAnIntentionConflict() throws Exception {
    var preference = availability();
    var requestKey = UUID.randomUUID();
    var originals = jdbc.queryForList("SELECT * FROM planned_blocks");
    var responses =
        orderedRequestsWhileBlockLocked(
            () -> cancellation(block, requestKey), () -> movement(requestKey, preference));
    assertThat(responses)
        .extracting(org.springframework.mock.web.MockHttpServletResponse::getStatus)
        .containsExactly(201, 409);
    assertThat(json.readTree(responses.getLast().getContentAsString()).get("code").asText())
        .isEqualTo("IDEMPOTENCY_CONFLICT");
    assertOneEffectiveChange(originals);
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM block_projections WHERE block_id=?", String.class, block))
        .isEqualTo("cancelled");
    org.mockito.Mockito.verify(clock, org.mockito.Mockito.times(1)).instant();
  }

  @Test
  void s21_cancellationWinsBeforeConcurrentMovementWithNewKey() throws Exception {
    var preference = availability();
    var originals = jdbc.queryForList("SELECT * FROM planned_blocks");
    var responses =
        orderedRequestsWhileBlockLocked(
            () -> cancellation(block, UUID.randomUUID()),
            () -> movement(UUID.randomUUID(), preference));
    assertThat(responses)
        .extracting(org.springframework.mock.web.MockHttpServletResponse::getStatus)
        .containsExactly(201, 412);
    assertThat(json.readTree(responses.getLast().getContentAsString()).get("code").asText())
        .isEqualTo("BLOCK_CONFLICT");
    assertOneEffectiveChange(originals);
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM block_projections WHERE block_id=?", String.class, block))
        .isEqualTo("cancelled");
  }

  @Test
  void s21_movementWinsBeforeConcurrentCancellationWithNewKey() throws Exception {
    var preference = availability();
    var originals = jdbc.queryForList("SELECT * FROM planned_blocks");
    var responses =
        orderedRequestsWhileBlockLocked(
            () -> movement(UUID.randomUUID(), preference),
            () -> cancellation(block, UUID.randomUUID()));
    assertThat(responses)
        .extracting(org.springframework.mock.web.MockHttpServletResponse::getStatus)
        .containsExactly(201, 412);
    assertThat(json.readTree(responses.getLast().getContentAsString()).get("code").asText())
        .isEqualTo("BLOCK_CONFLICT");
    assertOneEffectiveChange(originals);
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM block_projections WHERE block_id=?", String.class, block))
        .isEqualTo("planned");
  }

  List<org.springframework.mock.web.MockHttpServletResponse> orderedRequestsWhileBlockLocked(
      java.util.function.Supplier<
              org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder>
          firstRequest,
      java.util.function.Supplier<
              org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder>
          secondRequest)
      throws Exception {
    try (var blocker =
        java.sql.DriverManager.getConnection(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
      blocker.setAutoCommit(false);
      try (var lock =
          blocker.prepareStatement("SELECT id FROM planned_blocks WHERE id=? FOR UPDATE")) {
        lock.setObject(1, block);
        lock.executeQuery().close();
      }
      try (var workers = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
        try {
          var first =
              workers.submit(() -> mvc.perform(firstRequest.get()).andReturn().getResponse());
          awaitDatabaseWaiters("SELECT id FROM planned_blocks WHERE id=", 1);
          var second =
              workers.submit(() -> mvc.perform(secondRequest.get()).andReturn().getResponse());
          awaitDatabaseWaiters("SELECT id FROM availability_preferences WHERE owner_id=", 1);
          blocker.commit();
          return List.of(
              first.get(10, java.util.concurrent.TimeUnit.SECONDS),
              second.get(10, java.util.concurrent.TimeUnit.SECONDS));
        } finally {
          blocker.rollback();
        }
      }
    }
  }

  @Test
  void s21_distinctCancellationKeysCompeteForOneRevisionWithoutPreference() throws Exception {
    var originals = jdbc.queryForList("SELECT * FROM planned_blocks");
    var responses =
        raceOnRow(
            "planned_blocks",
            block,
            "SELECT id FROM planned_blocks WHERE id=",
            () -> cancellation(block, UUID.randomUUID()),
            () -> cancellation(block, UUID.randomUUID()));
    assertThat(responses)
        .extracting(org.springframework.mock.web.MockHttpServletResponse::getStatus)
        .containsExactlyInAnyOrder(201, 412);
    var rejected =
        responses.stream()
            .filter(response -> response.getStatus() == 412)
            .findFirst()
            .orElseThrow();
    assertThat(json.readTree(rejected.getContentAsString()).get("code").asText())
        .isEqualTo("BLOCK_CONFLICT");
    assertOneEffectiveChange(originals);
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM block_projections WHERE block_id=?", String.class, block))
        .isEqualTo("cancelled");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM availability_preferences", Long.class))
        .isZero();
  }

  @Test
  void s21_distinctMovementKeysCompeteForOneRevision() throws Exception {
    var preference = availability();
    var originals = jdbc.queryForList("SELECT * FROM planned_blocks");
    var responses =
        raceOnRow(
            "availability_preferences",
            preference,
            "SELECT id FROM availability_preferences WHERE owner_id=",
            () -> movement(UUID.randomUUID(), preference),
            () ->
                movement(UUID.randomUUID(), preference)
                    .content(
                        """
                        {"startLocal":"2030-01-07T14:00","endLocal":"2030-01-07T15:00","zoneId":"UTC","startOffset":"Z","endOffset":"Z","allowOverBudget":false}
                        """));
    assertThat(responses)
        .extracting(org.springframework.mock.web.MockHttpServletResponse::getStatus)
        .containsExactlyInAnyOrder(201, 412);
    var rejected =
        responses.stream()
            .filter(response -> response.getStatus() == 412)
            .findFirst()
            .orElseThrow();
    assertThat(json.readTree(rejected.getContentAsString()).get("code").asText())
        .isEqualTo("BLOCK_CONFLICT");
    assertOneEffectiveChange(originals);
  }

  void assertOneEffectiveChange(List<Map<String, Object>> originals) {
    assertThat(jdbc.queryForList("SELECT * FROM planned_blocks")).isEqualTo(originals);
    assertThat(
            jdbc.queryForObject(
                "SELECT version FROM block_projections WHERE block_id=?", Long.class, block))
        .isEqualTo(2);
    for (var table : List.of("block_projections", "block_changes", "outbox_events"))
      assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class)).isEqualTo(1);
  }

  @Test
  void s24_todaySnapshotKeepsReservationUntilTheNextReadAfterCancellation() throws Exception {
    availability();
    try (var writer = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
      var snapshot =
          today.read(
              "persona-a",
              observedPreference -> {
                try {
                  writer
                      .submit(
                          () ->
                              mvc.perform(cancellation(block, UUID.randomUUID()))
                                  .andExpect(status().isCreated()))
                      .get(10, java.util.concurrent.TimeUnit.SECONDS);
                } catch (Exception error) {
                  throw new AssertionError(error);
                }
                return com.apptolast.organization.domain.TodayWindow.at(
                    java.time.Instant.parse("2030-01-07T09:00:00Z"),
                    observedPreference,
                    Set.of("UTC"));
              });
      assertThat(snapshot.items()).hasSize(1);
      assertThat(snapshot.items().getFirst().block().id()).isEqualTo(block);
      assertThat(snapshot.plannedSeconds()).isEqualTo(3600);
      assertThat(snapshot.nextBlockId()).isEqualTo(block);
    }
    var next =
        today.read(
            "persona-a",
            currentPreference ->
                com.apptolast.organization.domain.TodayWindow.at(
                    java.time.Instant.parse("2030-01-07T09:00:00Z"),
                    currentPreference,
                    Set.of("UTC")));
    assertThat(next.items()).isEmpty();
    assertThat(next.plannedSeconds()).isZero();
    assertThat(next.remainingSeconds()).isEqualTo(7200);
    assertThat(next.nextBlockId()).isNull();
    assertThat(next.closingAt()).isNull();
    for (var table : List.of("block_changes", "outbox_events"))
      assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class)).isEqualTo(1);
  }

  @Test
  void s24_todaySnapshotKeepsTheWholeIntervalBeforeConcurrentMovement() throws Exception {
    var preference = availability();
    var originals = jdbc.queryForList("SELECT * FROM planned_blocks");
    try (var writer = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
      var snapshot =
          today.read(
              "persona-a",
              observedPreference -> {
                try {
                  writer
                      .submit(
                          () ->
                              mvc.perform(movement(UUID.randomUUID(), preference))
                                  .andExpect(status().isCreated()))
                      .get(10, java.util.concurrent.TimeUnit.SECONDS);
                } catch (Exception error) {
                  throw new AssertionError(error);
                }
                return com.apptolast.organization.domain.TodayWindow.at(
                    java.time.Instant.parse("2030-01-07T09:00:00Z"),
                    observedPreference,
                    Set.of("UTC"));
              });
      assertThat(snapshot.items()).hasSize(1);
      assertThat(snapshot.items().getFirst().block().time().startAt())
          .isEqualTo(java.time.Instant.parse("2030-01-07T10:00:00Z"));
      assertThat(snapshot.items().getFirst().block().time().endAt())
          .isEqualTo(java.time.Instant.parse("2030-01-07T11:00:00Z"));
      assertThat(snapshot.items().getFirst().projectName()).isEqualTo("Proyecto");
      assertThat(snapshot.items().getFirst().taskTitle()).isEqualTo("Tarea");
      assertThat(snapshot.plannedSeconds()).isEqualTo(3600);
    }
    var next =
        today.read(
            "persona-a",
            currentPreference ->
                com.apptolast.organization.domain.TodayWindow.at(
                    java.time.Instant.parse("2030-01-07T09:00:00Z"),
                    currentPreference,
                    Set.of("UTC")));
    assertThat(next.items()).hasSize(1);
    assertThat(next.items().getFirst().block().time().startAt())
        .isEqualTo(java.time.Instant.parse("2030-01-07T12:00:00Z"));
    assertThat(next.items().getFirst().block().time().endAt())
        .isEqualTo(java.time.Instant.parse("2030-01-07T13:00:00Z"));
    assertThat(next.plannedSeconds()).isEqualTo(3600);
    assertThat(jdbc.queryForList("SELECT * FROM planned_blocks")).isEqualTo(originals);
    for (var table : List.of("block_changes", "outbox_events"))
      assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class)).isEqualTo(1);
  }

  @Test
  void s22_competingMovesAcrossProjectsRecalculateTheSharedBudget() throws Exception {
    var preference = availability();
    jdbc.update("UPDATE availability_preferences SET monday_minutes=60 WHERE id=?", preference);
    jdbc.update(
        "UPDATE planned_blocks SET start_local=start_local+interval '1"
            + " day',end_local=end_local+interval '1 day',start_at=start_at+interval '1"
            + " day',end_at=end_at+interval '1 day' WHERE id=?",
        block);
    var otherProject = UUID.randomUUID();
    var otherTask = UUID.randomUUID();
    var otherBlock = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) SELECT"
            + " ?,owner_id,'Otro proyecto',description,status,created_at,updated_at FROM projects"
            + " WHERE id=?",
        otherProject,
        project);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at)"
            + " SELECT ?,?,'Otra tarea',completion_criterion,status,created_at,updated_at FROM"
            + " tasks WHERE id=?",
        otherTask,
        otherProject,
        task);
    jdbc.update(
        "INSERT INTO"
            + " planned_blocks(id,project_id,task_id,request_key,objective,start_local,end_local,zone_id,start_offset,end_offset,allow_over_budget,start_at,end_at,duration_minutes,created_at)"
            + " SELECT ?,?,?,?,objective,start_local+interval '1 day',end_local+interval '1"
            + " day',zone_id,start_offset,end_offset,allow_over_budget,start_at+interval '1"
            + " day',end_at+interval '1 day',duration_minutes,created_at FROM planned_blocks WHERE"
            + " id=?",
        otherBlock,
        otherProject,
        otherTask,
        UUID.randomUUID(),
        block);
    var originals = jdbc.queryForList("SELECT * FROM planned_blocks ORDER BY id");
    try (var blocker =
        java.sql.DriverManager.getConnection(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
      blocker.setAutoCommit(false);
      try (var lock =
          blocker.prepareStatement(
              "SELECT id FROM availability_preferences WHERE id=? FOR UPDATE")) {
        lock.setObject(1, preference);
        lock.executeQuery().close();
      }
      try (var workers = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
        try {
          var first =
              workers.submit(
                  () ->
                      mvc.perform(movement(UUID.randomUUID(), preference))
                          .andReturn()
                          .getResponse());
          var second =
              workers.submit(
                  () ->
                      mvc.perform(
                              post("/api/v1/projects/"
                                      + otherProject
                                      + "/tasks/"
                                      + otherTask
                                      + "/blocks/"
                                      + otherBlock
                                      + "/reschedule")
                                  .with(user("persona-a"))
                                  .with(csrf().asHeader())
                                  .contentType("application/json")
                                  .content(
                                      """
                                      {"startLocal":"2030-01-07T14:00","endLocal":"2030-01-07T15:00","zoneId":"UTC","startOffset":"Z","endOffset":"Z","allowOverBudget":false}
                                      """)
                                  .header("If-Match", "\"block:" + otherBlock + ":1\"")
                                  .header(
                                      "Availability-Revision",
                                      "\"availability:" + preference + ":0\"")
                                  .header("Idempotency-Key", UUID.randomUUID()))
                          .andReturn()
                          .getResponse());
          awaitDatabaseWaiters("SELECT id FROM availability_preferences WHERE owner_id=", 2);
          blocker.commit();
          var responses =
              List.of(
                  first.get(10, java.util.concurrent.TimeUnit.SECONDS),
                  second.get(10, java.util.concurrent.TimeUnit.SECONDS));
          assertThat(responses)
              .extracting(org.springframework.mock.web.MockHttpServletResponse::getStatus)
              .containsExactlyInAnyOrder(201, 409);
          var rejected =
              json.readTree(
                  responses.stream()
                      .filter(response -> response.getStatus() == 409)
                      .findFirst()
                      .orElseThrow()
                      .getContentAsString());
          assertThat(rejected.get("code").asText()).isEqualTo("BUDGET_EXCEEDED");
          assertThat(rejected.get("budgetZoneId").asText()).isEqualTo("UTC");
          assertThat(rejected.get("days").size()).isEqualTo(1);
          assertThat(rejected.get("days").get(0).get("plannedSeconds").asLong()).isEqualTo(3600);
          assertThat(rejected.get("days").get(0).get("requestedSeconds").asLong()).isEqualTo(3600);
          assertThat(rejected.get("days").get(0).get("excessSeconds").asLong()).isEqualTo(3600);
          assertThat(jdbc.queryForList("SELECT * FROM planned_blocks ORDER BY id"))
              .isEqualTo(originals);
          for (var table : List.of("block_projections", "block_changes", "outbox_events"))
            assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class))
                .isEqualTo(1);
        } finally {
          blocker.rollback();
        }
      }
    }
  }

  @Test
  void s21_waitingMovementWithSameKeyReplaysWinningReceipt() throws Exception {
    var requestKey = UUID.randomUUID();
    var preference = availability();
    var responses =
        raceOnRow(
            "availability_preferences",
            preference,
            "SELECT id FROM availability_preferences WHERE owner_id=",
            () -> movement(requestKey, preference),
            () -> movement(requestKey, preference));
    assertReplayedOnce(responses);
  }

  @Test
  void s41_distinctBlocksWithSameKeyCollideWithoutPartialChangesOrPreference() throws Exception {
    var other = UUID.randomUUID();
    var requestKey = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO"
            + " planned_blocks(id,project_id,task_id,request_key,objective,start_local,end_local,zone_id,start_offset,end_offset,allow_over_budget,start_at,end_at,duration_minutes,created_at)"
            + " SELECT ?,project_id,task_id,?,objective,start_local+interval '2"
            + " hours',end_local+interval '2"
            + " hours',zone_id,start_offset,end_offset,allow_over_budget,start_at+interval '2"
            + " hours',end_at+interval '2 hours',duration_minutes,created_at FROM planned_blocks"
            + " WHERE id=?",
        other,
        UUID.randomUUID(),
        block);
    var originals = jdbc.queryForList("SELECT * FROM planned_blocks ORDER BY id");
    jdbc.execute(
        "CREATE FUNCTION reschedule_receipt_barrier() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN"
            + " PERFORM pg_advisory_xact_lock_shared(130041); RETURN NEW; END; $$");
    jdbc.execute(
        "CREATE TRIGGER reschedule_receipt_barrier BEFORE INSERT ON block_changes FOR EACH ROW"
            + " EXECUTE FUNCTION reschedule_receipt_barrier()");
    try {
      try (var gate =
              java.sql.DriverManager.getConnection(
                  postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
          var latch = gate.createStatement()) {
        latch.execute("SELECT pg_advisory_lock(130041)");
        try (var workers = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
          try {
            var first =
                workers.submit(
                    () -> mvc.perform(cancellation(block, requestKey)).andReturn().getResponse());
            var second =
                workers.submit(
                    () -> mvc.perform(cancellation(other, requestKey)).andReturn().getResponse());
            awaitDatabaseWaiters("INSERT INTO block_changes", 2);
            latch.execute("SELECT pg_advisory_unlock(130041)");
            var responses =
                List.of(
                    first.get(10, java.util.concurrent.TimeUnit.SECONDS),
                    second.get(10, java.util.concurrent.TimeUnit.SECONDS));
            assertThat(responses)
                .extracting(org.springframework.mock.web.MockHttpServletResponse::getStatus)
                .containsExactlyInAnyOrder(201, 409);
            var rejected =
                responses.stream()
                    .filter(response -> response.getStatus() == 409)
                    .findFirst()
                    .orElseThrow();
            assertThat(json.readTree(rejected.getContentAsString()).get("code").asText())
                .isEqualTo("IDEMPOTENCY_CONFLICT");
            var accepted =
                responses.stream()
                    .filter(response -> response.getStatus() == 201)
                    .findFirst()
                    .orElseThrow();
            var winner =
                UUID.fromString(
                    json.readTree(accepted.getContentAsString()).get("blockId").asText());
            var loser = winner.equals(block) ? other : block;
            assertThat(jdbc.queryForList("SELECT block_id FROM block_projections", UUID.class))
                .containsExactly(winner);
            assertThat(
                    jdbc.queryForObject(
                        "SELECT version FROM block_projections WHERE block_id=?",
                        Long.class,
                        winner))
                .isEqualTo(2);
            mvc.perform(get(base() + "/" + loser + "/state").with(user("persona-a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("planned"))
                .andExpect(header().string("ETag", "\"block:" + loser + ":1\""));
            assertThat(jdbc.queryForList("SELECT * FROM planned_blocks ORDER BY id"))
                .isEqualTo(originals);
            for (var table : List.of("block_changes", "outbox_events"))
              assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class))
                  .isEqualTo(1);
            assertThat(
                    jdbc.queryForObject(
                        "SELECT count(*) FROM availability_preferences", Long.class))
                .isZero();
          } finally {
            latch.execute("SELECT pg_advisory_unlock(130041)");
          }
        }
      }
    } finally {
      jdbc.execute("DROP TRIGGER reschedule_receipt_barrier ON block_changes");
      jdbc.execute("DROP FUNCTION reschedule_receipt_barrier()");
    }
  }

  @Test
  void s21_waitingCancellationWithSameKeyReplaysWinningReceipt() throws Exception {
    var requestKey = UUID.randomUUID();
    var responses =
        raceOnRow(
            "planned_blocks",
            block,
            "SELECT id FROM planned_blocks WHERE id=",
            () -> cancellation(block, requestKey),
            () -> cancellation(block, requestKey));
    assertReplayedOnce(responses);
  }

  void assertReplayedOnce(List<org.springframework.mock.web.MockHttpServletResponse> responses)
      throws Exception {
    assertThat(responses)
        .extracting(org.springframework.mock.web.MockHttpServletResponse::getStatus)
        .containsExactlyInAnyOrder(201, 200);
    assertThat(responses.getFirst().getContentAsString())
        .isEqualTo(responses.getLast().getContentAsString());
    assertThat(responses.getFirst().getHeader("Location"))
        .isEqualTo(responses.getLast().getHeader("Location"));
    assertThat(
            jdbc.queryForObject(
                "SELECT version FROM block_projections WHERE block_id=?", Long.class, block))
        .isEqualTo(2);
    for (var table : List.of("block_changes", "outbox_events"))
      assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class)).isEqualTo(1);
    org.mockito.Mockito.verify(clock, org.mockito.Mockito.times(1)).instant();
  }

  List<org.springframework.mock.web.MockHttpServletResponse> raceOnRow(
      String table,
      UUID id,
      String waitingQuery,
      java.util.function.Supplier<
              org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder>
          firstRequest,
      java.util.function.Supplier<
              org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder>
          secondRequest)
      throws Exception {
    try (var blocker =
        java.sql.DriverManager.getConnection(
            postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
      blocker.setAutoCommit(false);
      try (var lock =
          blocker.prepareStatement("SELECT id FROM " + table + " WHERE id=? FOR UPDATE")) {
        lock.setObject(1, id);
        lock.executeQuery().close();
      }
      try (var workers = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
        try {
          var first =
              workers.submit(() -> mvc.perform(firstRequest.get()).andReturn().getResponse());
          var second =
              workers.submit(() -> mvc.perform(secondRequest.get()).andReturn().getResponse());
          awaitDatabaseWaiters(waitingQuery, 2);
          blocker.commit();
          return List.of(
              first.get(10, java.util.concurrent.TimeUnit.SECONDS),
              second.get(10, java.util.concurrent.TimeUnit.SECONDS));
        } finally {
          blocker.rollback();
        }
      }
    }
  }

  org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder cancellation(
      UUID id, UUID requestKey) {
    return post(base() + "/" + id + "/cancel")
        .with(user("persona-a"))
        .with(csrf().asHeader())
        .contentType("application/json")
        .content("{}")
        .header("If-Match", "\"block:" + id + ":1\"")
        .header("Idempotency-Key", requestKey);
  }

  void awaitDatabaseWaiters(String query, int expected) throws Exception {
    long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
    int waiting;
    do {
      jdbc.execute("SELECT pg_stat_clear_snapshot()");
      waiting =
          jdbc.queryForObject(
              "SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND"
                  + " wait_event_type='Lock' AND query LIKE ?",
              Integer.class,
              "%" + query + "%");
      if (waiting >= expected) return;
      Thread.sleep(10);
    } while (System.nanoTime() < deadline);
    assertThat(waiting)
        .as("PostgreSQL sessions waiting for %s", query)
        .isGreaterThanOrEqualTo(expected);
  }

  @Test
  void s20_rejectedCommitRollsBackEveryMovementWrite() throws Exception {
    var preference = availability();
    Map<String, List<Map<String, Object>>> before = new LinkedHashMap<>();
    for (var table :
        List.of(
            "projects",
            "tasks",
            "planned_blocks",
            "availability_preferences",
            "block_projections",
            "block_changes",
            "outbox_events")) before.put(table, jdbc.queryForList("SELECT * FROM " + table));
    jdbc.execute(
        "CREATE FUNCTION reschedule_commit_failure() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN"
            + " RAISE EXCEPTION 'private SQL secret'; END; $$");
    jdbc.execute(
        "CREATE CONSTRAINT TRIGGER reschedule_commit_failure AFTER INSERT ON outbox_events"
            + " DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION"
            + " reschedule_commit_failure()");
    try {
      var response =
          mvc.perform(movement(UUID.randomUUID(), preference))
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
      before.forEach(
          (table, rows) ->
              assertThat(jdbc.queryForList("SELECT * FROM " + table)).as(table).isEqualTo(rows));
    } finally {
      jdbc.execute("DROP TRIGGER reschedule_commit_failure ON outbox_events");
      jdbc.execute("DROP FUNCTION reschedule_commit_failure()");
    }
  }

  @Test
  void s20_suppressedOutboxRollsBackMovementAndReceipt() throws Exception {
    var preference = availability();
    var original = jdbc.queryForList("SELECT * FROM planned_blocks");
    var availabilityBefore = jdbc.queryForList("SELECT * FROM availability_preferences");
    jdbc.execute(
        "CREATE FUNCTION reschedule_suppression() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN"
            + " RETURN NULL; END; $$");
    jdbc.execute(
        "CREATE TRIGGER reschedule_suppression BEFORE INSERT ON outbox_events FOR EACH ROW EXECUTE"
            + " FUNCTION reschedule_suppression()");
    try {
      mvc.perform(movement(UUID.randomUUID(), preference))
          .andExpect(status().isServiceUnavailable())
          .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"));
      assertThat(jdbc.queryForList("SELECT * FROM planned_blocks")).isEqualTo(original);
      assertThat(jdbc.queryForList("SELECT * FROM availability_preferences"))
          .isEqualTo(availabilityBefore);
      for (var table : List.of("block_projections", "block_changes", "outbox_events"))
        assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class)).isZero();
    } finally {
      jdbc.execute("DROP TRIGGER reschedule_suppression ON outbox_events");
      jdbc.execute("DROP FUNCTION reschedule_suppression()");
    }
  }

  @Test
  void s20_suppressedReceiptRollsBackProjectionAndEvent() throws Exception {
    var original = jdbc.queryForList("SELECT * FROM planned_blocks");
    jdbc.execute(
        "CREATE FUNCTION reschedule_suppression() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN"
            + " RETURN NULL; END; $$");
    jdbc.execute(
        "CREATE TRIGGER reschedule_suppression BEFORE INSERT ON block_changes FOR EACH ROW EXECUTE"
            + " FUNCTION reschedule_suppression()");
    try {
      mvc.perform(
              post(base() + "/" + block + "/cancel")
                  .with(user("persona-a"))
                  .with(csrf().asHeader())
                  .contentType("application/json")
                  .content("{}")
                  .header("If-Match", "\"block:" + block + ":1\"")
                  .header("Idempotency-Key", UUID.randomUUID()))
          .andExpect(status().isServiceUnavailable())
          .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"));
      assertThat(jdbc.queryForList("SELECT * FROM planned_blocks")).isEqualTo(original);
      for (var table :
          List.of(
              "block_projections", "block_changes", "outbox_events", "availability_preferences"))
        assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class)).isZero();
    } finally {
      jdbc.execute("DROP TRIGGER reschedule_suppression ON block_changes");
      jdbc.execute("DROP FUNCTION reschedule_suppression()");
    }
  }

  @Test
  void s20_suppressedProjectionRollsBackTheWholeCancellation() throws Exception {
    var original = jdbc.queryForList("SELECT * FROM planned_blocks");
    jdbc.execute(
        "CREATE FUNCTION reschedule_suppression() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN"
            + " RETURN NULL; END; $$");
    jdbc.execute(
        "CREATE TRIGGER reschedule_suppression BEFORE INSERT ON block_projections FOR EACH ROW"
            + " EXECUTE FUNCTION reschedule_suppression()");
    try {
      mvc.perform(
              post(base() + "/" + block + "/cancel")
                  .with(user("persona-a"))
                  .with(csrf().asHeader())
                  .contentType("application/json")
                  .content("{}")
                  .header("If-Match", "\"block:" + block + ":1\"")
                  .header("Idempotency-Key", UUID.randomUUID()))
          .andExpect(status().isServiceUnavailable())
          .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"));
      assertThat(jdbc.queryForList("SELECT * FROM planned_blocks")).isEqualTo(original);
      for (var table :
          List.of(
              "block_projections", "block_changes", "outbox_events", "availability_preferences"))
        assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class)).isZero();
    } finally {
      jdbc.execute("DROP TRIGGER reschedule_suppression ON block_projections");
      jdbc.execute("DROP FUNCTION reschedule_suppression()");
    }
  }

  @Test
  void s4_stateRevisionFromUppercasePathCanBeUsedForCancellation() throws Exception {
    var response =
        mvc.perform(
                get(base() + "/" + block.toString().toUpperCase(java.util.Locale.ROOT) + "/state")
                    .with(user("persona-a")))
            .andExpect(status().isOk())
            .andExpect(header().string("ETag", "\"block:" + block + ":1\""))
            .andReturn()
            .getResponse();
    mvc.perform(
            post(base() + "/" + block + "/cancel")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{}")
                .header("If-Match", response.getHeader("ETag"))
                .header("Idempotency-Key", UUID.randomUUID()))
        .andExpect(status().isCreated());
    assertThat(
            jdbc.queryForObject(
                "SELECT version FROM block_projections WHERE block_id=?", Long.class, block))
        .isEqualTo(2);
  }

  @Test
  void s11_todayMovesReservationBetweenDaysWithoutChangingCreation() throws Exception {
    var original = jdbc.queryForList("SELECT * FROM planned_blocks");
    var receipt =
        json.readTree(
            mvc.perform(
                    movement(UUID.randomUUID(), availability())
                        .content(
                            """
                            {"startLocal":"2030-01-08T10:00","endLocal":"2030-01-08T11:00","zoneId":"UTC","startOffset":"Z","endOffset":"Z","allowOverBudget":false}
                            """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
    mvc.perform(get("/api/v1/today").with(user("persona-a")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.date").value("2030-01-07"))
        .andExpect(jsonPath("$.items").isEmpty())
        .andExpect(jsonPath("$.plannedSeconds").value(0));
    org.mockito.Mockito.when(clock.instant())
        .thenReturn(java.time.Instant.parse("2030-01-08T09:00:00Z"));
    var today =
        json.readTree(
            mvc.perform(get("/api/v1/today").with(user("persona-a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2030-01-08"))
                .andExpect(jsonPath("$.plannedSeconds").value(3600))
                .andExpect(jsonPath("$.remainingSeconds").value(3600))
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(today.get("items").size()).isEqualTo(1);
    assertThat(today.get("items").get(0).get("block")).isEqualTo(receipt.get("after"));
    assertThat(jdbc.queryForList("SELECT * FROM planned_blocks")).isEqualTo(original);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(1);
  }

  @Test
  void s11_todayExcludesCancelledReservations() throws Exception {
    availability();
    mvc.perform(
            post(base() + "/" + block + "/cancel")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{}")
                .header("If-Match", "\"block:" + block + ":1\"")
                .header("Idempotency-Key", UUID.randomUUID()))
        .andExpect(status().isCreated());
    mvc.perform(get("/api/v1/today").with(user("persona-a")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isEmpty())
        .andExpect(jsonPath("$.plannedSeconds").value(0))
        .andExpect(jsonPath("$.remainingSeconds").value(7200))
        .andExpect(jsonPath("$.nextBlockId").isEmpty());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(1);
  }

  @Test
  void s11_cancelledIntervalIsAvailableForANewCreation() throws Exception {
    var preference = availability();
    mvc.perform(
            post(base() + "/" + block + "/cancel")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{}")
                .header("If-Match", "\"block:" + block + ":1\"")
                .header("Idempotency-Key", UUID.randomUUID()))
        .andExpect(status().isCreated());
    var original = jdbc.queryForMap("SELECT * FROM planned_blocks WHERE id=?", block);
    mvc.perform(
            post(base())
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content(
                    """
                    {"objective":"Nueva reserva","startLocal":"2030-01-07T10:00","endLocal":"2030-01-07T11:00","zoneId":"UTC","startOffset":"Z","endOffset":"Z","allowOverBudget":false}
                    """)
                .header("Availability-Revision", "\"availability:" + preference + ":0\"")
                .header("Idempotency-Key", UUID.randomUUID()))
        .andExpect(status().isCreated());
    assertThat(jdbc.queryForMap("SELECT * FROM planned_blocks WHERE id=?", block))
        .isEqualTo(original);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class)).isEqualTo(2);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(2);
  }

  @Test
  void s3_movedBlockUsesCurrentIntervalInListAndDetail() throws Exception {
    var receipt =
        json.readTree(
            mvc.perform(movement(UUID.randomUUID(), availability()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());
    var page =
        json.readTree(
            mvc.perform(get(base()).with(user("persona-a")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(page.get("items").size()).isEqualTo(1);
    assertThat(page.get("items").get(0)).isEqualTo(receipt.get("after"));
    var detail =
        json.readTree(
            mvc.perform(get(base() + "/" + block).with(user("persona-a")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(detail).isEqualTo(receipt.get("after"));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(1);
  }

  @Test
  void s3_cancelledBlockLeavesTheCurrentList() throws Exception {
    mvc.perform(
            post(base() + "/" + block + "/cancel")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{}")
                .header("If-Match", "\"block:" + block + ":1\"")
                .header("Idempotency-Key", UUID.randomUUID()))
        .andExpect(status().isCreated());
    mvc.perform(get(base()).with(user("persona-a")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isEmpty())
        .andExpect(jsonPath("$.nextCursor").isEmpty());
    assertThat(jdbc.queryForObject("SELECT count(*) FROM planned_blocks", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(1);
  }

  @Test
  void s3_cancelledBlockHasNoCurrentDetailButRetainsStateAndCreationReceipt() throws Exception {
    mvc.perform(
            post(base() + "/" + block + "/cancel")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{}")
                .header("If-Match", "\"block:" + block + ":1\"")
                .header("Idempotency-Key", UUID.randomUUID()))
        .andExpect(status().isCreated());
    var before = jdbc.queryForList("SELECT * FROM block_projections");
    mvc.perform(get(base() + "/" + block).with(user("persona-a")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("BLOCK_NOT_FOUND"));
    mvc.perform(get(base() + "/" + block + "/state").with(user("persona-a")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("cancelled"));
    mvc.perform(get(base() + "/by-request/" + key).with(user("persona-a")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(block.toString()))
        .andExpect(jsonPath("$.startAt").value("2030-01-07T10:00:00Z"));
    assertThat(jdbc.queryForList("SELECT * FROM block_projections")).isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(1);
  }

  @Test
  void s18_stateUsesAnActualReadOnlyDatabaseTransaction() {
    var observed = new ArrayList<String>();
    var manager =
        new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource()) {
          @Override
          protected void doBegin(
              Object transaction,
              org.springframework.transaction.TransactionDefinition definition) {
            super.doBegin(transaction, definition);
            observed.add(jdbc.queryForObject("SHOW transaction_read_only", String.class));
          }
        };
    var template = new org.springframework.transaction.support.TransactionTemplate(manager);
    var preferences =
        new com.apptolast.organization.adapter.persistence.PostgresAvailabilityStore(
            jdbc, template);
    var store =
        new com.apptolast.organization.adapter.persistence.PostgresBlockStore(
            jdbc, template, preferences, json);
    assertThat(store.state("persona-a", project, task, block).block().id()).isEqualTo(block);
    assertThat(observed).containsExactly("on");
    assertThat(jdbc.queryForObject("SHOW transaction_read_only", String.class)).isEqualTo("off");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_projections", Long.class)).isZero();
  }

  @Test
  void s15_moveReplayDoesNotAliasTextualZoneIntentions() throws Exception {
    var preference = availability();
    var requestKey = UUID.randomUUID();
    mvc.perform(movement(requestKey, preference)).andExpect(status().isCreated());
    var before = jdbc.queryForList("SELECT * FROM block_projections");
    org.mockito.Mockito.clearInvocations(clock);
    mvc.perform(
            movement(requestKey, preference)
                .content(
                    """
                    {"startLocal":"2030-01-07T12:00","endLocal":"2030-01-07T13:00","zoneId":"Etc/UTC","startOffset":"Z","endOffset":"Z","allowOverBudget":false}
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
    assertThat(jdbc.queryForList("SELECT * FROM block_projections")).isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(1);
    org.mockito.Mockito.verifyNoInteractions(clock);
  }

  @Test
  void s15_cancelRejectsKeyOfMovementOnSameBlock() throws Exception {
    var preference = availability();
    var requestKey = UUID.randomUUID();
    mvc.perform(movement(requestKey, preference)).andExpect(status().isCreated());
    var before = jdbc.queryForList("SELECT * FROM block_projections");
    org.mockito.Mockito.clearInvocations(clock);
    mvc.perform(
            post(base() + "/" + block + "/cancel")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{}")
                .header("If-Match", "\"block:" + block + ":2\"")
                .header("Idempotency-Key", requestKey))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
    assertThat(jdbc.queryForList("SELECT * FROM block_projections")).isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(1);
    org.mockito.Mockito.verifyNoInteractions(clock);
  }

  @Test
  void s15_moveReplayPrecedesRevisionPreferenceAndClock() throws Exception {
    var preference = availability();
    var requestKey = UUID.randomUUID();
    var first =
        mvc.perform(movement(requestKey, preference))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();
    jdbc.update(
        "UPDATE availability_preferences SET version=1,zone_id='Unavailable/Zone' WHERE id=?",
        preference);
    var before = jdbc.queryForList("SELECT * FROM block_projections");
    org.mockito.Mockito.clearInvocations(clock);
    org.mockito.Mockito.when(clock.instant())
        .thenThrow(new AssertionError("Historical replay must not read clock"));
    var repeated =
        mvc.perform(movement(requestKey, preference))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    assertThat(repeated.getContentAsString()).isEqualTo(first.getContentAsString());
    assertThat(repeated.getHeader("Location")).isEqualTo(first.getHeader("Location"));
    assertThat(jdbc.queryForList("SELECT * FROM block_projections")).isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(1);
    org.mockito.Mockito.verifyNoInteractions(clock);
  }

  @Test
  void s7_movePreviewExcludesOwnIntervalAndDoesNotWrite() throws Exception {
    var preference = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO"
            + " availability_preferences(id,owner_id,zone_id,monday_minutes,tuesday_minutes,wednesday_minutes,thursday_minutes,friday_minutes,saturday_minutes,sunday_minutes,version,created_at,updated_at)"
            + " VALUES (?,'persona-a','UTC',120,120,120,120,120,120,120,0,now(),now())",
        preference);
    var original = jdbc.queryForList("SELECT * FROM planned_blocks");
    var response =
        mvc.perform(
                post(base() + "/" + block + "/reschedule/preview")
                    .with(user("persona-a"))
                    .with(csrf().asHeader())
                    .contentType("application/json")
                    .content(
                        """
                        {"startLocal":"2030-01-07T10:30","endLocal":"2030-01-07T11:30","zoneId":"UTC","startOffset":null,"endOffset":null}
                        """)
                    .header("If-Match", "\"block:" + block + ":1\""))
            .andExpect(status().isOk())
            .andExpect(header().string("ETag", "\"block:" + block + ":1\""))
            .andExpect(jsonPath("$.objective").value("Preparar borrador"))
            .andExpect(jsonPath("$.startAt").value("2030-01-07T10:30:00Z"))
            .andExpect(jsonPath("$.days[0].plannedSeconds").value(0))
            .andExpect(jsonPath("$.days[0].requestedSeconds").value(3600))
            .andExpect(
                jsonPath("$.availabilityEtag").value("\"availability:" + preference + ":0\""))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString()).size()).isEqualTo(10);
    assertThat(jdbc.queryForList("SELECT * FROM planned_blocks")).isEqualTo(original);
    for (var table : List.of("block_projections", "block_changes", "outbox_events"))
      assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class)).isZero();
  }

  @Test
  void s8_moveCommitsNewIntervalAndPreservesOriginalCreation() throws Exception {
    var preference = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO"
            + " availability_preferences(id,owner_id,zone_id,monday_minutes,tuesday_minutes,wednesday_minutes,thursday_minutes,friday_minutes,saturday_minutes,sunday_minutes,version,created_at,updated_at)"
            + " VALUES (?,'persona-a','UTC',120,120,120,120,120,120,120,0,now(),now())",
        preference);
    var original = jdbc.queryForList("SELECT * FROM planned_blocks");
    var response =
        mvc.perform(
                post(base() + "/" + block + "/reschedule")
                    .with(user("persona-a"))
                    .with(csrf().asHeader())
                    .contentType("application/json")
                    .content(
                        """
                        {"startLocal":"2030-01-07T12:00","endLocal":"2030-01-07T13:00","zoneId":"UTC","startOffset":"Z","endOffset":"Z","allowOverBudget":false}
                        """)
                    .header("If-Match", "\"block:" + block + ":1\"")
                    .header("Availability-Revision", "\"availability:" + preference + ":0\"")
                    .header("Idempotency-Key", UUID.randomUUID()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();
    var receipt = json.readTree(response.getContentAsString());
    assertThat(receipt.size()).isEqualTo(7);
    assertThat(receipt.get("kind").asText()).isEqualTo("RESCHEDULED");
    assertThat(receipt.get("revision").asText()).isEqualTo("\"block:" + block + ":2\"");
    assertThat(receipt.get("before").get("startAt").asText()).isEqualTo("2030-01-07T10:00:00Z");
    assertThat(receipt.get("after").get("startAt").asText()).isEqualTo("2030-01-07T12:00:00Z");
    assertThat(response.getHeader("Location"))
        .isEqualTo(base() + "/changes/" + receipt.get("id").asText());
    mvc.perform(get(base() + "/" + block + "/state").with(user("persona-a")))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"block:" + block + ":2\""))
        .andExpect(jsonPath("$.block.startAt").value("2030-01-07T12:00:00Z"));
    assertThat(jdbc.queryForList("SELECT * FROM planned_blocks")).isEqualTo(original);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isEqualTo(1);
    var event =
        json.readTree(jdbc.queryForObject("SELECT payload::text FROM outbox_events", String.class));
    assertThat(event.get("type").asText()).isEqualTo("BlockChanged.v1");
    assertThat(event.get("kind").asText()).isEqualTo("RESCHEDULED");
    assertThat(event.get("after").get("startAt")).isEqualTo(receipt.get("after").get("startAt"));
    org.mockito.Mockito.verify(clock, org.mockito.Mockito.times(1)).instant();
  }

  @Test
  void s15_cancelKeyCannotRecoverReceiptForAnotherBlock() throws Exception {
    var other = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO"
            + " planned_blocks(id,project_id,task_id,request_key,objective,start_local,end_local,zone_id,start_offset,end_offset,allow_over_budget,start_at,end_at,duration_minutes,created_at)"
            + " SELECT ?,project_id,task_id,?,objective,start_local+INTERVAL '2"
            + " hours',end_local+INTERVAL '2"
            + " hours',zone_id,start_offset,end_offset,allow_over_budget,start_at+INTERVAL '2"
            + " hours',end_at+INTERVAL '2 hours',duration_minutes,created_at FROM planned_blocks"
            + " WHERE id=?",
        other,
        UUID.randomUUID(),
        block);
    var requestKey = UUID.randomUUID();
    mvc.perform(
            post(base() + "/" + block + "/cancel")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{}")
                .header("If-Match", "\"block:" + block + ":1\"")
                .header("Idempotency-Key", requestKey))
        .andExpect(status().isCreated());
    var before = jdbc.queryForList("SELECT * FROM block_projections");
    org.mockito.Mockito.clearInvocations(clock);
    mvc.perform(
            post(base() + "/" + other + "/cancel")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{}")
                .header("If-Match", "\"block:" + other + ":1\"")
                .header("Idempotency-Key", requestKey))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
    assertThat(jdbc.queryForList("SELECT * FROM block_projections")).isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(1);
    org.mockito.Mockito.verifyNoInteractions(clock);
  }

  @Test
  void s6_cancelExhaustedVersionWithoutPreferenceDoesNotOverflow() throws Exception {
    jdbc.update(
        "INSERT INTO block_projections(block_id,version,status,updated_at) VALUES"
            + " (?,9223372036854775807,'planned',TIMESTAMPTZ '2030-01-07 08:59Z')",
        block);
    var before = jdbc.queryForList("SELECT * FROM block_projections");
    mvc.perform(
            post(base() + "/" + block + "/cancel")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{}")
                .header("If-Match", "\"block:" + block + ":9223372036854775807\"")
                .header("Idempotency-Key", UUID.randomUUID()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("BLOCK_VERSION_EXHAUSTED"));
    assertThat(jdbc.queryForList("SELECT * FROM block_projections")).isEqualTo(before);
    for (var table : List.of("availability_preferences", "block_changes", "outbox_events"))
      assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class)).isZero();
  }

  @Test
  void s6_cancelCurrentCancelledBlockIsDefinitiveConflict() throws Exception {
    jdbc.update(
        "INSERT INTO block_projections(block_id,version,status,updated_at) VALUES"
            + " (?,2,'cancelled',TIMESTAMPTZ '2030-01-07 08:59Z')",
        block);
    var before = jdbc.queryForList("SELECT * FROM block_projections");
    mvc.perform(
            post(base() + "/" + block + "/cancel")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{}")
                .header("If-Match", "\"block:" + block + ":2\"")
                .header("Idempotency-Key", UUID.randomUUID()))
        .andExpect(status().isConflict())
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(jsonPath("$.code").value("BLOCK_CANCELLED"));
    assertThat(jdbc.queryForList("SELECT * FROM block_projections")).isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @Test
  void s19_cancelTrailingJsonPrecedesHistoricalReplay() throws Exception {
    var requestKey = UUID.randomUUID();
    mvc.perform(
            post(base() + "/" + block + "/cancel")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{}")
                .header("If-Match", "\"block:" + block + ":1\"")
                .header("Idempotency-Key", requestKey))
        .andExpect(status().isCreated());
    var before = jdbc.queryForList("SELECT * FROM block_projections");
    org.mockito.Mockito.clearInvocations(clock);
    mvc.perform(
            post(base() + "/" + block + "/cancel")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{} null")
                .header("If-Match", "\"block:" + block + ":1\"")
                .header("Idempotency-Key", requestKey))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    assertThat(jdbc.queryForList("SELECT * FROM block_projections")).isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(1);
    org.mockito.Mockito.verifyNoInteractions(clock);
  }

  @Test
  void s6_cancelStaleRevisionReturnsConflictWithoutWriting() throws Exception {
    jdbc.update(
        "INSERT INTO block_projections(block_id,version,status,updated_at) VALUES"
            + " (?,2,'cancelled',TIMESTAMPTZ '2030-01-07 08:59Z')",
        block);
    var before = jdbc.queryForList("SELECT * FROM block_projections");
    mvc.perform(
            post(base() + "/" + block + "/cancel")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{}")
                .header("If-Match", "\"block:" + block + ":1\"")
                .header("Idempotency-Key", UUID.randomUUID()))
        .andExpect(status().isPreconditionFailed())
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(jsonPath("$.code").value("BLOCK_CONFLICT"));
    assertThat(jdbc.queryForList("SELECT * FROM block_projections")).isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @Test
  void s18_stateRejectsUnknownQueryBeforeMalformedIds() throws Exception {
    mvc.perform(
            get("/api/v1/projects/not-a-uuid/tasks/also-invalid/blocks/invalid/state")
                .with(user("persona-a"))
                .queryParam("extra", "1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("query"));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_projections", Long.class)).isZero();
  }

  @Test
  void s19_cancelRejectsTrailingJsonWithoutWriting() throws Exception {
    mvc.perform(
            post(base() + "/" + block + "/cancel")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{} {}")
                .header("If-Match", "\"block:" + block + ":1\"")
                .header("Idempotency-Key", UUID.randomUUID()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_projections", Long.class)).isZero();
  }

  @Test
  void s19_cancelMissingBodyIsMalformedAfterValidHeaders() throws Exception {
    mvc.perform(
            post(base() + "/" + block + "/cancel")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .header("If-Match", "\"block:" + block + ":1\"")
                .header("Idempotency-Key", UUID.randomUUID()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isZero();
  }

  @Test
  void s19_cancelMalformedJsonUsesSharedProblemWithoutWriting() throws Exception {
    mvc.perform(
            post(base() + "/" + block + "/cancel")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{")
                .header("If-Match", "\"block:" + block + ":1\"")
                .header("Idempotency-Key", UUID.randomUUID()))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(
            content()
                .json(
                    """
                    {"type":"urn:organization:problem:malformed_json","title":"No se puede leer el JSON enviado.","status":400,"code":"MALFORMED_JSON"}
                    """,
                    org.springframework.test.json.JsonCompareMode.STRICT));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_projections", Long.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @Test
  void s19_cancelRejectsArrayRootWithoutWriting() throws Exception {
    mvc.perform(
            post(base() + "/" + block + "/cancel")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("[]")
                .header("If-Match", "\"block:" + block + ":1\"")
                .header("Idempotency-Key", UUID.randomUUID()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("body"))
        .andExpect(jsonPath("$.errors[0].code").value("INVALID_TYPE"));
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @Test
  void s19_cancelRejectsExtraBodyBeforeReturningExistingReceipt() throws Exception {
    var requestKey = UUID.randomUUID();
    mvc.perform(
            post(base() + "/" + block + "/cancel")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{}")
                .header("If-Match", "\"block:" + block + ":1\"")
                .header("Idempotency-Key", requestKey))
        .andExpect(status().isCreated());
    var prior = jdbc.queryForList("SELECT * FROM block_projections");
    mvc.perform(
            post(base() + "/" + block + "/cancel")
                .with(user("persona-a"))
                .with(csrf().asHeader())
                .contentType("application/json")
                .content("{\"objective\":\"Otro\"}")
                .header("If-Match", "\"block:" + block + ":1\"")
                .header("Idempotency-Key", requestKey))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.errors[0].field").value("objective"))
        .andExpect(jsonPath("$.errors[0].code").value("UNKNOWN_FIELD"));
    assertThat(jdbc.queryForList("SELECT * FROM block_projections")).isEqualTo(prior);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isEqualTo(1);
  }

  @BeforeEach
  void seed() {
    org.mockito.Mockito.when(clock.instant())
        .thenReturn(java.time.Instant.parse("2030-01-07T09:00:00.123456789Z"));
    jdbc.execute(
        "TRUNCATE"
            + " block_changes,block_projections,planned_blocks,availability_preferences,task_status_history,tasks,outbox_events,projects");
    project = UUID.randomUUID();
    task = UUID.randomUUID();
    block = UUID.randomUUID();
    key = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES"
            + " (?,'persona-a','Proyecto','','active',now(),now())",
        project);
    jdbc.update(
        "INSERT INTO tasks(id,project_id,title,completion_criterion,status,created_at,updated_at)"
            + " VALUES (?,?,'Tarea','','pending',now(),now())",
        task,
        project);
    jdbc.update(
        "INSERT INTO"
            + " planned_blocks(id,project_id,task_id,request_key,objective,start_local,end_local,zone_id,start_offset,end_offset,allow_over_budget,start_at,end_at,duration_minutes,created_at)"
            + " VALUES (?,?,?,?,'Preparar borrador',TIMESTAMP '2030-01-07 10:00',TIMESTAMP"
            + " '2030-01-07 11:00','UTC','Z','Z',false,TIMESTAMPTZ '2030-01-07 10:00Z',TIMESTAMPTZ"
            + " '2030-01-07 11:00Z',60,TIMESTAMPTZ '2030-01-06 10:00Z')",
        block,
        project,
        task,
        key);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "query,400,query",
    "project,400,projectId",
    "task,400,taskId",
    "block,400,blockId",
    "absent,428,none",
    "weak,400,If-Match",
    "wildcard,400,If-Match",
    "list,400,If-Match",
    "repeat,400,If-Match",
    "other,400,If-Match",
    "zero,400,If-Match",
    "leading,400,If-Match",
    "overflow,400,If-Match",
    "keyMissing,400,Idempotency-Key",
    "keyBad,400,Idempotency-Key",
    "keyRepeated,400,Idempotency-Key"
  })
  void s5_s19_cancelHeadersAreValidatedInContractOrder(String defect, int status, String field)
      throws Exception {
    var revision = "\"block:" + block + ":1\"";
    revision =
        switch (defect) {
          case "weak" -> "W/" + revision;
          case "wildcard" -> "*";
          case "list" -> revision + "," + revision;
          case "other" -> "\"block:" + UUID.randomUUID() + ":1\"";
          case "zero" -> "\"block:" + block + ":0\"";
          case "leading" -> "\"block:" + block + ":01\"";
          case "overflow" -> "\"block:" + block + ":9223372036854775808\"";
          default -> revision;
        };
    var path =
        "/api/v1/projects/"
            + (defect.equals("project") ? "1" : project)
            + "/tasks/"
            + (defect.equals("task") ? "1" : task)
            + "/blocks/"
            + (defect.equals("block") ? "1" : block)
            + "/cancel";
    var request =
        post(path)
            .with(user("persona-a"))
            .with(csrf().asHeader())
            .contentType("application/json")
            .content("{}");
    if (defect.equals("query")) request.queryParam("extra", "1");
    if (!Set.of("absent", "query", "project", "task", "block").contains(defect))
      request.header("If-Match", revision);
    if (defect.equals("repeat")) request.header("If-Match", revision);
    if (!defect.equals("keyMissing"))
      request.header("Idempotency-Key", defect.equals("keyBad") ? "1" : UUID.randomUUID());
    if (defect.equals("keyRepeated")) request.header("Idempotency-Key", UUID.randomUUID());
    var result =
        mvc.perform(request)
            .andExpect(status().is(status))
            .andExpect(
                jsonPath("$.code")
                    .value(status == 428 ? "PRECONDITION_REQUIRED" : "VALIDATION_ERROR"));
    if (status != 428) result.andExpect(jsonPath("$.errors[0].field").value(field));
    for (var table : List.of("block_projections", "block_changes", "outbox_events"))
      assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class)).isZero();
  }

  @Test
  void s15_cancelReplayReturnsOriginalReceiptBeforeRevisionAndClock() throws Exception {
    var requestKey = UUID.randomUUID();
    var first =
        mvc.perform(
                post(base() + "/" + block + "/cancel")
                    .with(user("persona-a"))
                    .with(csrf().asHeader())
                    .contentType("application/json")
                    .content("{}")
                    .header("If-Match", "\"block:" + block + ":1\"")
                    .header("Idempotency-Key", requestKey))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();
    var before = jdbc.queryForList("SELECT * FROM block_projections");
    org.mockito.Mockito.clearInvocations(clock);
    org.mockito.Mockito.when(clock.instant())
        .thenThrow(new AssertionError("Replay must not read clock"));
    var replay =
        mvc.perform(
                post(base() + "/" + block + "/cancel")
                    .with(user("persona-a"))
                    .with(csrf().asHeader())
                    .contentType("application/json")
                    .content("{}")
                    .header("If-Match", "\"block:" + block + ":1\"")
                    .header("Idempotency-Key", requestKey))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    assertThat(replay.getContentAsString()).isEqualTo(first.getContentAsString());
    assertThat(replay.getHeader("Location")).isEqualTo(first.getHeader("Location"));
    assertThat(jdbc.queryForList("SELECT * FROM block_projections")).isEqualTo(before);
    for (var table : List.of("block_changes", "outbox_events"))
      assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class)).isEqualTo(1);
    org.mockito.Mockito.verifyNoInteractions(clock);
  }

  @Test
  void s12_s13_s26_cancelCommitsReceiptProjectionAndEventWithoutPreference() throws Exception {
    var original = jdbc.queryForList("SELECT * FROM planned_blocks");
    var response =
        mvc.perform(
                post(base() + "/" + block + "/cancel")
                    .with(user("persona-a"))
                    .with(csrf().asHeader())
                    .contentType("application/json")
                    .content("{}")
                    .header("If-Match", "\"block:" + block + ":1\"")
                    .header("Idempotency-Key", UUID.randomUUID()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse();
    var receipt = json.readTree(response.getContentAsString());
    assertThat(receipt.size()).isEqualTo(7);
    var change = UUID.fromString(receipt.get("id").asText());
    assertThat(response.getHeader("Location")).isEqualTo(base() + "/changes/" + change);
    assertThat(receipt.get("blockId").asText()).isEqualTo(block.toString());
    assertThat(receipt.get("kind").asText()).isEqualTo("CANCELLED");
    assertThat(receipt.get("revision").asText()).isEqualTo("\"block:" + block + ":2\"");
    assertThat(receipt.get("occurredAt").asText()).isEqualTo("2030-01-07T09:00:00.123456Z");
    assertThat(receipt.get("before").size()).isEqualTo(9);
    assertThat(receipt.get("before").get("id").asText()).isEqualTo(block.toString());
    assertThat(receipt.get("after").isNull()).isTrue();
    mvc.perform(get(base() + "/" + block + "/state").with(user("persona-a")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("cancelled"))
        .andExpect(jsonPath("$.updatedAt").value("2030-01-07T09:00:00.123456Z"));
    assertThat(jdbc.queryForList("SELECT * FROM planned_blocks")).isEqualTo(original);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_changes", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM availability_preferences", Long.class))
        .isZero();
    var event =
        json.readTree(jdbc.queryForObject("SELECT payload::text FROM outbox_events", String.class));
    assertThat(event.size()).isEqualTo(13);
    assertThat(event.get("type").asText()).isEqualTo("BlockChanged.v1");
    assertThat(event.get("schemaVersion").asInt()).isEqualTo(1);
    assertThat(event.get("aggregateId").asText()).isEqualTo(project.toString());
    assertThat(event.get("taskId").asText()).isEqualTo(task.toString());
    assertThat(event.get("ownerId").asText()).isEqualTo("persona-a");
    assertThat(event.get("blockId").asText()).isEqualTo(block.toString());
    assertThat(event.get("changeId").asText()).isEqualTo(change.toString());
    assertThat(event.get("eventId").asText()).isNotEqualTo(change.toString());
    assertThat(event.get("kind").asText()).isEqualTo("CANCELLED");
    assertThat(event.get("revision").asLong()).isEqualTo(2);
    assertThat(event.get("occurredAt")).isEqualTo(receipt.get("occurredAt"));
    assertThat(event.get("before"))
        .isEqualTo(
            json.readTree(
                "{\"startAt\":\"2030-01-07T10:00:00Z\",\"endAt\":\"2030-01-07T11:00:00Z\",\"zoneId\":\"UTC\",\"durationMinutes\":60}"));
    assertThat(event.get("after").isNull()).isTrue();
    org.mockito.Mockito.verify(clock, org.mockito.Mockito.times(1)).instant();
  }

  @Test
  void s18_missingBlockHasSpecificProblemWithoutDisclosingOtherResources() throws Exception {
    mvc.perform(get(base() + "/" + UUID.randomUUID() + "/state").with(user("persona-a")))
        .andExpect(status().isNotFound())
        .andExpect(content().contentType("application/problem+json"))
        .andExpect(jsonPath("$.code").value("BLOCK_NOT_FOUND"));
    mvc.perform(get(base() + "/" + block + "/state").with(user("persona-b")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void s2_s3_projectionDoesNotRewriteCreationReceipt() throws Exception {
    var original = jdbc.queryForList("SELECT * FROM planned_blocks");
    jdbc.update(
        "INSERT INTO"
            + " block_projections(block_id,version,status,updated_at,start_local,end_local,zone_id,start_offset,end_offset,start_at,end_at,duration_minutes)"
            + " VALUES (?,2,'planned',TIMESTAMPTZ '2030-01-07 09:00Z',TIMESTAMP '2030-01-07"
            + " 13:00',TIMESTAMP '2030-01-07 14:30','Europe/Madrid','+01:00','+01:00',TIMESTAMPTZ"
            + " '2030-01-07 12:00Z',TIMESTAMPTZ '2030-01-07 13:30Z',90)",
        block);
    mvc.perform(get(base() + "/" + block + "/state").with(user("persona-a")))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", "\"block:" + block + ":2\""))
        .andExpect(jsonPath("$.block.startAt").value("2030-01-07T12:00:00Z"))
        .andExpect(jsonPath("$.block.endAt").value("2030-01-07T13:30:00Z"))
        .andExpect(jsonPath("$.block.durationMinutes").value(90))
        .andExpect(jsonPath("$.block.zoneId").value("Europe/Madrid"));
    mvc.perform(get(base() + "/by-request/" + key).with(user("persona-a")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.startAt").value("2030-01-07T10:00:00Z"))
        .andExpect(jsonPath("$.durationMinutes").value(60))
        .andExpect(jsonPath("$.zoneId").value("UTC"));
    assertThat(jdbc.queryForList("SELECT * FROM planned_blocks")).isEqualTo(original);
  }

  @Test
  void s3_s4_readsCancelledProjectionWithExactLargeRevisionAndLastBlock() throws Exception {
    jdbc.update(
        "INSERT INTO block_projections(block_id,version,status,updated_at) VALUES"
            + " (?,9007199254740993,'cancelled',TIMESTAMPTZ '2030-01-07 08:59:59.123456Z')",
        block);
    var response =
        mvc.perform(get(base() + "/" + block + "/state").with(user("persona-a")))
            .andExpect(status().isOk())
            .andExpect(header().string("ETag", "\"block:" + block + ":9007199254740993\""))
            .andReturn()
            .getResponse();
    var state = json.readTree(response.getContentAsString());
    assertThat(state.size()).isEqualTo(3);
    assertThat(state.get("status").asText()).isEqualTo("cancelled");
    assertThat(state.get("updatedAt").asText()).isEqualTo("2030-01-07T08:59:59.123456Z");
    assertThat(state.get("block").size()).isEqualTo(9);
    assertThat(state.get("block").get("startAt").asText()).isEqualTo("2030-01-07T10:00:00Z");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM block_projections", Long.class))
        .isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class)).isZero();
  }

  @Test
  void s1_readsOriginalStateWithoutMaterializingOrChangingFacts() throws Exception {
    var original = jdbc.queryForList("SELECT * FROM planned_blocks");
    var response =
        mvc.perform(get(base() + "/" + block + "/state").with(user("persona-a")))
            .andExpect(status().isOk())
            .andExpect(header().string("ETag", "\"block:" + block + ":1\""))
            .andExpect(
                header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
            .andReturn()
            .getResponse();
    assertThat(json.readTree(response.getContentAsString()))
        .isEqualTo(
            json.readTree(
                """
                {"block":{"id":"%s","projectId":"%s","taskId":"%s","objective":"Preparar borrador","startAt":"2030-01-07T10:00:00Z","endAt":"2030-01-07T11:00:00Z","zoneId":"UTC","durationMinutes":60,"createdAt":"2030-01-06T10:00:00Z"},"status":"planned","updatedAt":"2030-01-06T10:00:00Z"}
                """
                    .formatted(block, project, task)));
    assertThat(jdbc.queryForList("SELECT * FROM planned_blocks")).isEqualTo(original);
    for (var table : List.of("availability_preferences", "outbox_events"))
      assertThat(jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class)).isZero();
  }
}
