package com.apptolast.organization.adapter.persistence;

import static org.assertj.core.api.Assertions.*;

import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

class AutomationPersistenceTest {
  private static final Instant T0 = Instant.parse("2026-09-08T10:00:00Z");

  private final PostgresAutomationStore store =
      new PostgresAutomationStore(Database.JDBC, Database.TRANSACTIONS, new ObjectMapper());

  private static String owner() {
    return "automations-" + UUID.randomUUID();
  }

  private UUID project(String owner) {
    var id = UUID.randomUUID();
    Database.JDBC.update(
        "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at)"
            + " VALUES (?,?,?,?, 'idea', ?, ?)",
        id,
        owner,
        "Marketing",
        "",
        java.sql.Timestamp.from(T0),
        java.sql.Timestamp.from(T0));
    return id;
  }

  private static AutomationDraft draft(String name, UUID project, UUID condition) {
    return new AutomationDraft(
        name,
        true,
        "TaskCreated.v1",
        condition,
        new CreateTaskAction(project, "Revisar {{task.title}}", "{{event.type}}", 30));
  }

  private AutomationRule create(String owner, String name, UUID project, Instant createdAt) {
    return store.create(
        owner,
        new AutomationRule(UUID.randomUUID(), draft(name, project, null), 1, createdAt, createdAt));
  }

  @Test
  void s1_s11_storesEveryFieldOfBothActionShapesAndReadsThemBack() {
    var owner = owner();
    var project = project(owner);
    var endpoint = UUID.randomUUID();
    var task = create(owner, "Con tarea", project, T0);
    var webhook =
        store.create(
            owner,
            new AutomationRule(
                UUID.randomUUID(),
                new AutomationDraft(
                    "Con webhook",
                    false,
                    "ProjectStatusChanged.v1",
                    project,
                    new NotifyWebhookAction(endpoint)),
                1,
                T0.plusSeconds(1),
                T0.plusSeconds(1)));
    assertThat(store.find(owner, task.id())).hasValue(task);
    assertThat(store.find(owner, webhook.id())).hasValue(webhook);
    assertThat(store.find(owner(), task.id())).isEmpty();
  }

  @Test
  void s11_ordersByCreationThenIdAndNeverLeaksAnotherOwner() {
    var owner = owner();
    var project = project(owner);
    var older = create(owner, "Antigua", project, T0.minusSeconds(1));
    var sameInstantHigh =
        store.create(
            owner,
            new AutomationRule(
                UUID.fromString("ffffffff-ffff-4fff-8fff-ffffffffffff"),
                draft("Alta", project, null),
                1,
                T0,
                T0));
    var sameInstantLow =
        store.create(
            owner,
            new AutomationRule(
                UUID.fromString("00000000-0000-4000-8000-000000000001"),
                draft("Baja", project, null),
                1,
                T0,
                T0));
    var stranger = owner();
    create(stranger, "Ajena", project(stranger), T0.minusSeconds(5));
    assertThat(store.list(owner)).containsExactly(older, sameInstantLow, sameInstantHigh);
    assertThat(store.list(owner())).isEmpty();
  }

  @Test
  void s9_theQuotaIsTwentyPerOwnerCountingDisabledRules() {
    var owner = owner();
    var project = project(owner);
    for (int index = 0; index < AutomationRuleStore.RULE_LIMIT; index++)
      store.create(
          owner,
          new AutomationRule(
              UUID.randomUUID(),
              new AutomationDraft(
                  "R" + index,
                  index % 2 == 0,
                  "TaskCreated.v1",
                  null,
                  new CreateTaskAction(project, "Revisar", null, null)),
              1,
              T0.plusSeconds(index),
              T0.plusSeconds(index)));
    assertThatThrownBy(() -> create(owner, "Extra", project, T0))
        .isInstanceOf(AutomationLimitException.class);
    assertThat(store.list(owner)).hasSize(AutomationRuleStore.RULE_LIMIT);
    var other = owner();
    assertThat(create(other, "Suya", project(other), T0)).isNotNull();
  }

  @Test
  void s10_twoConcurrentCreationsForTheLastSlotLeaveExactlyTwentyRules() throws Exception {
    var owner = owner();
    var project = project(owner);
    for (int index = 0; index < AutomationRuleStore.RULE_LIMIT - 1; index++)
      create(owner, "R" + index, project, T0.plusSeconds(index));
    var start = new CountDownLatch(1);
    var pool = Executors.newFixedThreadPool(2);
    try {
      List<Future<Boolean>> attempts =
          List.of(
              pool.submit(() -> race(start, owner, project)),
              pool.submit(() -> race(start, owner, project)));
      start.countDown();
      long accepted = 0;
      long rejected = 0;
      for (var attempt : attempts)
        if (attempt.get()) accepted++;
        else rejected++;
      assertThat(accepted).isEqualTo(1);
      assertThat(rejected).isEqualTo(1);
    } finally {
      pool.shutdownNow();
    }
    assertThat(store.list(owner)).hasSize(AutomationRuleStore.RULE_LIMIT);
  }

  private boolean race(CountDownLatch start, String owner, UUID project) throws Exception {
    start.await();
    try {
      create(owner, "Ultima", project, T0);
      return true;
    } catch (AutomationLimitException expected) {
      return false;
    }
  }

  @Test
  void s12_replacingBumpsTheVersionKeepsCreatedAtAndDemandsTheExpectedOne() {
    var owner = owner();
    var project = project(owner);
    var rule = create(owner, "Original", project, T0);
    var later = T0.plusSeconds(60);
    var replaced = store.replace(owner, rule.id(), 1, draft("Cambiada", project, project), later);
    assertThat(replaced.version()).isEqualTo(2);
    assertThat(replaced.createdAt()).isEqualTo(T0);
    assertThat(replaced.updatedAt()).isEqualTo(later);
    assertThat(store.find(owner, rule.id())).hasValue(replaced);
    assertThatThrownBy(() -> store.replace(owner, rule.id(), 1, rule.draft(), later))
        .isInstanceOf(AutomationConflictException.class);
    assertThatThrownBy(() -> store.replace(owner(), rule.id(), 2, rule.draft(), later))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThat(store.find(owner, rule.id()).orElseThrow().version()).isEqualTo(2);
  }

  @Test
  void s13_s14_deletingDemandsTheExpectedVersionAndFreesASlot() {
    var owner = owner();
    var project = project(owner);
    var rule = create(owner, "Victima", project, T0);
    assertThatThrownBy(() -> store.delete(owner, rule.id(), 2))
        .isInstanceOf(AutomationConflictException.class);
    assertThatThrownBy(() -> store.delete(owner(), rule.id(), 1))
        .isInstanceOf(ResourceNotFoundException.class);
    store.delete(owner, rule.id(), 1);
    assertThat(store.list(owner)).isEmpty();
    assertThatThrownBy(() -> store.delete(owner, rule.id(), 1))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  static final class Database {
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:17.9-alpine");
    static final JdbcTemplate JDBC;
    static final DataSourceTransactionManager TRANSACTIONS;

    static {
      PG.start();
      var source = new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
      Flyway.configure().dataSource(source).load().migrate();
      JDBC = new JdbcTemplate(source);
      TRANSACTIONS = new DataSourceTransactionManager(source);
    }
  }
}
