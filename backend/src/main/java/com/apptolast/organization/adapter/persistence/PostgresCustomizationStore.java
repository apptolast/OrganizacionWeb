package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.CustomizationEditing;
import com.apptolast.organization.application.CustomizationQueries;
import com.apptolast.organization.application.StorageUnavailableException;
import com.apptolast.organization.domain.CustomFieldDefinition;
import com.apptolast.organization.domain.Customization;
import com.apptolast.organization.domain.CustomizationScope;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

public final class PostgresCustomizationStore
    implements CustomizationQueries, CustomizationEditing {
  private final JdbcTemplate jdbc;
  private final ObjectMapper json;
  private final TransactionTemplate reading;
  private final TransactionTemplate writing;

  public PostgresCustomizationStore(
      JdbcTemplate jdbc, PlatformTransactionManager transactions, ObjectMapper json) {
    this.jdbc = jdbc;
    this.json = json;
    reading = new TransactionTemplate(transactions);
    reading.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    reading.setReadOnly(true);
    writing = new TransactionTemplate(transactions);
    writing.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
  }

  public Customization change(
      String owner,
      CustomizationScope scope,
      Function<Optional<Customization>, Customization> operation) {
    return writing.execute(
        status -> {
          jdbc.query(
              "SELECT pg_advisory_xact_lock(hashtextextended(?,0))",
              row -> {},
              "customization:" + owner + ":" + scope.name());
          var previous = select(owner, scope);
          var changed = operation.apply(previous);
          if (previous.isPresent() && previous.get().equals(changed)) return changed;
          try {
            jdbc.update(
                "INSERT INTO customization_preferences (id,owner_id,scope,visible_fields,custom_fields,version,updated_at) VALUES (?,?,?,?::jsonb,?::jsonb,?,?) ON CONFLICT (owner_id,scope) DO UPDATE SET visible_fields=EXCLUDED.visible_fields,custom_fields=EXCLUDED.custom_fields,version=EXCLUDED.version,updated_at=EXCLUDED.updated_at",
                changed.id(),
                owner,
                scope.name(),
                json.writeValueAsString(changed.visibleFields()),
                json.writeValueAsString(changed.customFields()),
                changed.version(),
                changed.updatedAt().atOffset(java.time.ZoneOffset.UTC));
          } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
            throw new StorageUnavailableException(error);
          }
          return changed;
        });
  }

  public Optional<Customization> find(String owner, CustomizationScope scope) {
    try {
      return reading.execute(status -> select(owner, scope));
    } catch (RuntimeException error) {
      throw new StorageUnavailableException(error);
    }
  }

  private Optional<Customization> select(String owner, CustomizationScope scope) {
    return jdbc
        .query(
            "SELECT * FROM customization_preferences WHERE owner_id=? AND scope=?",
            (row, index) -> {
              try {
                return new Customization(
                    row.getObject("id", UUID.class),
                    owner,
                    scope,
                    json.readValue(
                        row.getString("visible_fields"), new TypeReference<List<String>>() {}),
                    json.readValue(
                        row.getString("custom_fields"),
                        new TypeReference<List<CustomFieldDefinition>>() {}),
                    row.getLong("version"),
                    row.getObject("updated_at", OffsetDateTime.class).toInstant());
              } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
                throw new StorageUnavailableException(error);
              }
            },
            owner,
            scope.name())
        .stream()
        .findFirst();
  }
}
