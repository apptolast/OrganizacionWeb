package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.AutomationCandidate;
import com.apptolast.organization.application.AutomationClaimedException;
import com.apptolast.organization.application.AutomationCommit;
import com.apptolast.organization.application.AutomationEffect;
import com.apptolast.organization.application.AutomationOutcome;
import com.apptolast.organization.application.AutomationWork;
import com.apptolast.organization.application.CreateTaskUseCase;
import com.apptolast.organization.application.StorageUnavailableException;
import com.apptolast.organization.domain.AutomationCursor;
import com.apptolast.organization.domain.AutomationEvent;
import com.apptolast.organization.domain.AutomationRun;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The one transactional port of the worker over PostgreSQL. A confirmation writes the effect, the
 * run and the cursor inside a single transaction; only {@link #record} writes on its own, precisely
 * because it is the row that has to survive a rollback.
 */
public final class PostgresAutomationWork implements AutomationWork {
  /** How many outbox rows one cycle looks at per owner. */
  private static final int BATCH = 200;

  private static final String RUN_COLUMNS =
      "id, rule_id, owner_id, event_id, event_type, occurred_at, attempt, status,"
          + " created_task_id, delivery_id, error_code, executed_at";

  private final JdbcTemplate jdbc;
  private final ObjectMapper json;
  private final CreateTaskUseCase createTask;
  private final TransactionTemplate confirming;
  private final TransactionTemplate apart;

  public PostgresAutomationWork(
      JdbcTemplate jdbc,
      PlatformTransactionManager transactions,
      ObjectMapper json,
      CreateTaskUseCase createTask) {
    this.jdbc = jdbc;
    this.json = json;
    this.createTask = createTask;
    this.confirming = new TransactionTemplate(transactions);
    this.apart = new TransactionTemplate(transactions);
    this.apart.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @Override
  public List<String> ownersWithRules() {
    return read(
        () -> jdbc.queryForList("SELECT DISTINCT owner_id FROM automation_rules", String.class));
  }

  @Override
  public Optional<AutomationCursor> cursor(String owner) {
    return read(
        () ->
            jdbc
                .query(
                    "SELECT occurred_at, event_id FROM automation_cursors WHERE owner_id = ?",
                    PostgresAutomationWork::cursorOf,
                    owner)
                .stream()
                .findFirst());
  }

  @Override
  public void startCursor(String owner, AutomationCursor present) {
    // Another worker may have started the same owner first; whoever wrote it, the walk goes on.
    write(
        () ->
            jdbc.update(
                "INSERT INTO automation_cursors(owner_id, occurred_at, event_id) VALUES (?,?,?)"
                    + " ON CONFLICT (owner_id) DO NOTHING",
                owner,
                Timestamp.from(present.occurredAt()),
                present.eventId()));
  }

  @Override
  public List<AutomationCandidate> after(String owner, AutomationCursor from) {
    return read(
        () -> {
          var events =
              jdbc.query(
                  window(
                      "event_id, aggregate_id, owner_id, event_type, occurred_at, payload,"
                          + " status"),
                  this::event,
                  arguments(owner, from));
          return events.isEmpty() ? List.of() : withTheirRuns(owner, from, events);
        });
  }

  @Override
  public void commit(AutomationCommit commit) {
    try {
      confirming.executeWithoutResult(
          status -> {
            for (var outcome : commit.outcomes()) apply(commit.owner(), outcome);
            if (commit.reached() != null) advance(commit.owner(), commit.reached());
          });
    } catch (DataAccessException | TransactionException error) {
      throw new StorageUnavailableException(error);
    }
  }

  @Override
  public void record(AutomationRun run) {
    try {
      apart.executeWithoutResult(status -> upsert(run));
    } catch (DataAccessException | TransactionException error) {
      throw new StorageUnavailableException(error);
    }
  }

  private void apply(String owner, AutomationOutcome outcome) {
    UUID createdTaskId = null;
    UUID deliveryId = null;
    switch (outcome.effect()) {
      case AutomationEffect.CreateTask task ->
          createdTaskId =
              createTask
                  .execute(
                      owner,
                      task.projectId(),
                      task.title(),
                      task.completionCriterion(),
                      task.estimatedMinutes())
                  .id();
      case AutomationEffect.Notify notify -> deliveryId = queue(owner, notify);
      case AutomationEffect.None ignored -> {}
    }
    claim(outcome.run(), createdTaskId, deliveryId);
  }

  /**
   * One pending delivery of feature 25 carrying the outbox row untransformed. The body is copied
   * from the outbox by the database itself, so no re-serialisation can alter a single byte, and the
   * subscription of the endpoint is deliberately not consulted: the rule is the subscription.
   */
  private UUID queue(String owner, AutomationEffect.Notify notify) {
    var deliveryId = UUID.randomUUID();
    var now = Timestamp.from(notify.event().occurredAt());
    var affected =
        jdbc.update(
            "INSERT INTO webhook_deliveries(id, endpoint_id, owner_id, event_id, event_type, body,"
                + " status, attempt, next_attempt_at, created_at, updated_at)"
                + " SELECT ?, e.id, e.owner_id, o.event_id, o.event_type, o.payload::text,"
                + " 'pending', 0, ?, ?, ?"
                + " FROM webhook_endpoints e, outbox_events o"
                + " WHERE e.id = ? AND e.owner_id = ? AND e.status = 'active' AND o.event_id = ?",
            deliveryId,
            now,
            now,
            now,
            notify.endpointId(),
            owner,
            notify.event().eventId());
    if (affected == 0) throw new AutomationClaimedException();
    return deliveryId;
  }

  /**
   * A first attempt inserts and a later one renews its own row. Either way, affecting no row means
   * another worker owns this (rule, event): the whole confirmation is abandoned, not retried.
   */
  private void claim(AutomationRun run, UUID createdTaskId, UUID deliveryId) {
    var affected =
        run.attempt() == 1
            ? jdbc.update(
                insert() + " ON CONFLICT (rule_id, event_id) DO NOTHING",
                values(run, createdTaskId, deliveryId))
            : jdbc.update(
                "UPDATE automation_runs SET attempt = ?, status = ?, created_task_id = ?,"
                    + " delivery_id = ?, error_code = ?, executed_at = ?"
                    + " WHERE rule_id = ? AND event_id = ? AND status = 'retry'",
                run.attempt(),
                run.status(),
                createdTaskId,
                deliveryId,
                run.errorCode(),
                Timestamp.from(run.executedAt()),
                run.ruleId(),
                run.eventId());
    if (affected == 0) throw new AutomationClaimedException();
  }

  private void upsert(AutomationRun run) {
    jdbc.update(
        insert()
            + " ON CONFLICT (rule_id, event_id) DO UPDATE SET attempt = EXCLUDED.attempt,"
            + " status = EXCLUDED.status, created_task_id = EXCLUDED.created_task_id,"
            + " delivery_id = EXCLUDED.delivery_id, error_code = EXCLUDED.error_code,"
            + " executed_at = EXCLUDED.executed_at",
        values(run, run.createdTaskId(), run.deliveryId()));
  }

  private void advance(String owner, AutomationCursor reached) {
    jdbc.update(
        "INSERT INTO automation_cursors(owner_id, occurred_at, event_id) VALUES (?,?,?)"
            + " ON CONFLICT (owner_id) DO UPDATE SET occurred_at = EXCLUDED.occurred_at,"
            + " event_id = EXCLUDED.event_id",
        owner,
        Timestamp.from(reached.occurredAt()),
        reached.eventId());
  }

  private static String insert() {
    return "INSERT INTO automation_runs(" + RUN_COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
  }

  private static Object[] values(AutomationRun run, UUID createdTaskId, UUID deliveryId) {
    return new Object[] {
      run.id(),
      run.ruleId(),
      run.ownerId(),
      run.eventId(),
      run.eventType(),
      Timestamp.from(run.occurredAt()),
      run.attempt(),
      run.status(),
      createdTaskId,
      deliveryId,
      run.errorCode(),
      Timestamp.from(run.executedAt())
    };
  }

  private List<AutomationCandidate> withTheirRuns(
      String owner, AutomationCursor from, List<Candidate> events) {
    var runs =
        jdbc
            .query(
                "SELECT "
                    + RUN_COLUMNS
                    + " FROM automation_runs WHERE owner_id = ? AND event_id IN ("
                    + window("event_id")
                    + ")",
                PostgresAutomationWork::runOf,
                prepend(owner, arguments(owner, from)))
            .stream()
            .collect(Collectors.groupingBy(AutomationRun::eventId));
    return events.stream()
        .map(
            candidate ->
                new AutomationCandidate(
                    candidate.event(),
                    candidate.blocked(),
                    runs.getOrDefault(candidate.event().eventId(), List.of())))
        .toList();
  }

  /** The rows of one owner strictly after the cursor, in the tuple order PostgreSQL imposes. */
  private static String window(String columns) {
    return "SELECT "
        + columns
        + " FROM outbox_events WHERE owner_id = ? AND (occurred_at, event_id) > (?, ?)"
        + " ORDER BY occurred_at, event_id LIMIT ?";
  }

  private static Object[] arguments(String owner, AutomationCursor from) {
    return new Object[] {owner, Timestamp.from(from.occurredAt()), from.eventId(), BATCH};
  }

  private static Object[] prepend(Object first, Object[] rest) {
    var all = new Object[rest.length + 1];
    all[0] = first;
    System.arraycopy(rest, 0, all, 1, rest.length);
    return all;
  }

  private record Candidate(AutomationEvent event, boolean blocked) {}

  private Candidate event(ResultSet row, int index) throws SQLException {
    return new Candidate(
        new AutomationEvent(
            row.getObject("event_id", UUID.class),
            row.getString("owner_id"),
            row.getString("event_type"),
            row.getObject("aggregate_id", UUID.class),
            row.getTimestamp("occurred_at").toInstant(),
            payload(row.getString("payload"))),
        "blocked".equals(row.getString("status")));
  }

  private Map<String, Object> payload(String raw) {
    try {
      return json.readValue(raw, new TypeReference<Map<String, Object>>() {});
    } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
      throw new IllegalStateException("El payload almacenado no se pudo leer.", error);
    }
  }

  private static AutomationCursor cursorOf(ResultSet row, int index) throws SQLException {
    return new AutomationCursor(
        row.getTimestamp("occurred_at").toInstant(), row.getObject("event_id", UUID.class));
  }

  private static AutomationRun runOf(ResultSet row, int index) throws SQLException {
    return new AutomationRun(
        row.getObject("id", UUID.class),
        row.getObject("rule_id", UUID.class),
        row.getString("owner_id"),
        row.getObject("event_id", UUID.class),
        row.getString("event_type"),
        row.getTimestamp("occurred_at").toInstant(),
        row.getInt("attempt"),
        row.getString("status"),
        row.getObject("created_task_id", UUID.class),
        row.getObject("delivery_id", UUID.class),
        row.getString("error_code"),
        row.getTimestamp("executed_at").toInstant());
  }

  private <T> T read(java.util.function.Supplier<T> query) {
    try {
      return query.get();
    } catch (DataAccessException error) {
      throw new StorageUnavailableException(error);
    }
  }

  private void write(Runnable statement) {
    try {
      statement.run();
    } catch (DataAccessException error) {
      throw new StorageUnavailableException(error);
    }
  }
}
