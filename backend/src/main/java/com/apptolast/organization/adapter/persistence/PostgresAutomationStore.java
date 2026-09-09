package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.AutomationConflictException;
import com.apptolast.organization.application.AutomationLimitException;
import com.apptolast.organization.application.AutomationRuleStore;
import com.apptolast.organization.application.ResourceNotFoundException;
import com.apptolast.organization.application.StorageUnavailableException;
import com.apptolast.organization.domain.AutomationDraft;
import com.apptolast.organization.domain.AutomationRule;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** Owner-scoped rule storage: the quota is serialised per owner and versions are optimistic. */
public final class PostgresAutomationStore implements AutomationRuleStore {
  private static final String COLUMNS =
      "id, name, enabled, event_type, condition_project_id, action, version, created_at, updated_at";

  private final JdbcTemplate jdbc;
  private final ObjectMapper json;
  private final TransactionTemplate reading;
  private final TransactionTemplate writing;

  public PostgresAutomationStore(
      JdbcTemplate jdbc, PlatformTransactionManager transactions, ObjectMapper json) {
    this.jdbc = jdbc;
    this.json = json;
    this.reading = new TransactionTemplate(transactions);
    this.reading.setReadOnly(true);
    this.reading.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    this.writing = new TransactionTemplate(transactions);
  }

  @Override
  public AutomationRule create(String owner, AutomationRule rule) {
    return write(
        () ->
            writing.execute(
                status -> {
                  lockOwner(owner);
                  var used =
                      jdbc.queryForObject(
                          "SELECT count(*) FROM automation_rules WHERE owner_id = ?",
                          Long.class,
                          owner);
                  if (used != null && used >= RULE_LIMIT) throw new AutomationLimitException();
                  jdbc.update(
                      "INSERT INTO automation_rules(id, owner_id, name, enabled, event_type,"
                          + " condition_project_id, action, version, created_at, updated_at)"
                          + " VALUES (?,?,?,?,?,?,?::jsonb,?,?,?)",
                      rule.id(),
                      owner,
                      rule.draft().name(),
                      rule.draft().enabled(),
                      rule.draft().eventType(),
                      rule.draft().conditionProjectId(),
                      AutomationActionJson.encode(json, rule.draft().action()),
                      rule.version(),
                      Timestamp.from(rule.createdAt()),
                      Timestamp.from(rule.updatedAt()));
                  return rule;
                }));
  }

  @Override
  public List<AutomationRule> list(String owner) {
    return write(
        () ->
            reading.execute(
                status ->
                    jdbc.query(
                        "SELECT "
                            + COLUMNS
                            + " FROM automation_rules WHERE owner_id = ?"
                            + " ORDER BY created_at, id",
                        mapper(),
                        owner)));
  }

  @Override
  public Optional<AutomationRule> find(String owner, UUID id) {
    return write(
        () ->
            reading.execute(
                status ->
                    jdbc
                        .query(
                            "SELECT "
                                + COLUMNS
                                + " FROM automation_rules"
                                + " WHERE owner_id = ? AND id = ?",
                            mapper(),
                            owner,
                            id)
                        .stream()
                        .findFirst()));
  }

  @Override
  public AutomationRule replace(
      String owner, UUID id, long expectedVersion, AutomationDraft draft, Instant now) {
    return write(
        () ->
            writing.execute(
                status -> {
                  var current = locked(owner, id, expectedVersion);
                  jdbc.update(
                      "UPDATE automation_rules SET name = ?, enabled = ?, event_type = ?,"
                          + " condition_project_id = ?, action = ?::jsonb, version = ?,"
                          + " updated_at = ? WHERE owner_id = ? AND id = ?",
                      draft.name(),
                      draft.enabled(),
                      draft.eventType(),
                      draft.conditionProjectId(),
                      AutomationActionJson.encode(json, draft.action()),
                      expectedVersion + 1,
                      Timestamp.from(now),
                      owner,
                      id);
                  return new AutomationRule(
                      id, draft, expectedVersion + 1, current.createdAt(), now);
                }));
  }

  @Override
  public void delete(String owner, UUID id, long expectedVersion) {
    write(
        () ->
            writing.execute(
                status -> {
                  locked(owner, id, expectedVersion);
                  jdbc.update(
                      "DELETE FROM automation_rules WHERE owner_id = ? AND id = ?", owner, id);
                  return null;
                }));
  }

  private AutomationRule locked(String owner, UUID id, long expectedVersion) {
    var current =
        jdbc
            .query(
                "SELECT "
                    + COLUMNS
                    + " FROM automation_rules WHERE owner_id = ? AND id = ?"
                    + " FOR UPDATE",
                mapper(),
                owner,
                id)
            .stream()
            .findFirst()
            .orElseThrow(ResourceNotFoundException::new);
    if (current.version() != expectedVersion) throw new AutomationConflictException();
    return current;
  }

  /**
   * Serialises the quota check per owner. A hashtext collision only makes two owners take turns,
   * which is harmless; counting rows cannot be locked any other way.
   */
  private void lockOwner(String owner) {
    jdbc.query(
        "SELECT pg_advisory_xact_lock(hashtext(?))",
        (org.springframework.jdbc.core.ResultSetExtractor<Void>) results -> null,
        owner);
  }

  private RowMapper<AutomationRule> mapper() {
    return (ResultSet row, int index) -> read(row);
  }

  private AutomationRule read(ResultSet row) throws SQLException {
    var draft =
        new AutomationDraft(
            row.getString("name"),
            row.getBoolean("enabled"),
            row.getString("event_type"),
            row.getObject("condition_project_id", UUID.class),
            AutomationActionJson.decode(json, row.getString("action")));
    return new AutomationRule(
        row.getObject("id", UUID.class),
        draft,
        row.getLong("version"),
        row.getTimestamp("created_at").toInstant(),
        row.getTimestamp("updated_at").toInstant());
  }

  private static <T> T write(java.util.function.Supplier<T> work) {
    try {
      return work.get();
    } catch (DataAccessException error) {
      throw new StorageUnavailableException(error);
    }
  }
}
