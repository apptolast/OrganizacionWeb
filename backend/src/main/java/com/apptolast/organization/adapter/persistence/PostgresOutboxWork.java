package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.OutboxWork;
import com.apptolast.organization.domain.OutboxMessage;
import com.apptolast.organization.domain.PublicationAttempt;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

public final class PostgresOutboxWork implements OutboxWork {
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transaction;
  private final ObjectMapper json;

  public PostgresOutboxWork(JdbcTemplate jdbc, TransactionTemplate transaction, ObjectMapper json) {
    this.jdbc = jdbc;
    this.transaction = transaction;
    this.json = json;
  }

  @Override
  public Optional<PublicationAttempt> processNext(
      Instant now, Set<UUID> excluded, Function<OutboxMessage, PublicationAttempt> operation) {
    try {
      return transaction.execute(
          status -> {
            var parameters =
                new org.springframework.jdbc.core.namedparam.MapSqlParameterSource(
                        "excluded", excluded)
                    .addValue("now", Timestamp.from(now));
            String excludedSql = excluded.isEmpty() ? "" : " AND event_id NOT IN (:excluded)";
            var rows =
                new org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate(jdbc)
                    .query(
                        "SELECT * FROM outbox_events WHERE status='pending' AND next_attempt_at <= :now"
                            + excludedSql
                            + " ORDER BY occurred_at,event_id LIMIT 1 FOR UPDATE SKIP LOCKED",
                        parameters,
                        (rs, index) ->
                            new OutboxMessage(
                                rs.getObject("event_id", UUID.class),
                                rs.getObject("aggregate_id", UUID.class),
                                rs.getString("owner_id"),
                                rs.getTimestamp("occurred_at").toInstant(),
                                rs.getString("event_type"),
                                rs.getInt("schema_version"),
                                rs.getString("payload"),
                                payload(rs.getString("payload")),
                                rs.getLong("attempts")));
            if (rows.isEmpty()) return Optional.empty();
            PublicationAttempt result = operation.apply(rows.getFirst());
            jdbc.update(
                "UPDATE outbox_events SET status=?, attempts=?, published_at=?, next_attempt_at=COALESCE(?,next_attempt_at), last_error_code=? WHERE event_id=?",
                result.outcome().equals("retry") ? "pending" : result.outcome(),
                result.attempt(),
                result.outcome().equals("published") ? Timestamp.from(result.completedAt()) : null,
                result.nextAttemptAt() == null ? null : Timestamp.from(result.nextAttemptAt()),
                result.code(),
                result.eventId());
            return Optional.of(result);
          });
    } catch (org.springframework.dao.DataAccessException
        | org.springframework.transaction.TransactionException error) {
      throw new com.apptolast.organization.application.StorageUnavailableException(error);
    }
  }

  private Map<String, Object> payload(String value) {
    try {
      Map<String, Object> decoded =
          json.readValue(value, new TypeReference<Map<String, Object>>() {});
      return decoded == null ? Map.of() : decoded;
    } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
      return Map.of();
    }
  }
}
