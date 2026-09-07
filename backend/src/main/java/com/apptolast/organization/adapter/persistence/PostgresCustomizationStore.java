package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.CustomFieldValuesQueries;
import com.apptolast.organization.application.CustomizationEditing;
import com.apptolast.organization.application.CustomizationQueries;
import com.apptolast.organization.application.StorageUnavailableException;
import com.apptolast.organization.domain.CustomFieldDefinition;
import com.apptolast.organization.domain.CustomFieldValue;
import com.apptolast.organization.domain.CustomFieldValues;
import com.apptolast.organization.domain.Customization;
import com.apptolast.organization.domain.CustomizationRevision;
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
    implements CustomizationQueries, CustomizationEditing, CustomFieldValuesQueries {
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

  public CustomFieldValues find(
      String owner, CustomizationScope scope, UUID projectId, UUID entityId) {
    return reading.execute(
        status -> {
          if (!Boolean.TRUE.equals(
              jdbc.queryForObject(
                  "SELECT EXISTS(SELECT 1 FROM projects WHERE id=? AND owner_id=?)",
                  Boolean.class,
                  projectId,
                  owner)))
            throw new com.apptolast.organization.application.ResourceNotFoundException();
          if (scope == CustomizationScope.TASK
              && !Boolean.TRUE.equals(
                  jdbc.queryForObject(
                      "SELECT EXISTS(SELECT 1 FROM tasks WHERE id=? AND project_id=?)",
                      Boolean.class,
                      entityId,
                      projectId)))
            throw new com.apptolast.organization.application.ResourceNotFoundException();
          var configuration = select(owner, scope);
          var valuesTable =
              scope == CustomizationScope.PROJECT
                  ? "project_custom_field_values"
                  : "task_custom_field_values";
          var entityColumn = scope == CustomizationScope.PROJECT ? "project_id" : "task_id";
          var stored =
              jdbc
                  .query(
                      "SELECT * FROM "
                          + valuesTable
                          + " WHERE owner_id=? AND "
                          + entityColumn
                          + "=?",
                      (row, index) -> {
                        try {
                          return new StoredValues(
                              new CustomizationRevision(
                                  row.getObject("id", UUID.class), row.getLong("version")),
                              row.getObject("updated_at", OffsetDateTime.class).toInstant(),
                              json.readValue(
                                  row.getString("field_values"),
                                  new TypeReference<java.util.Map<String, Object>>() {}));
                        } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
                          throw new StorageUnavailableException(error);
                        }
                      },
                      owner,
                      entityId)
                  .stream()
                  .findFirst();
          var fields =
              configuration.map(Customization::customFields).orElse(List.of()).stream()
                  .filter(CustomFieldDefinition::active)
                  .map(
                      field ->
                          new CustomFieldValue(
                              field.id(),
                              field.label(),
                              field.type(),
                              stored
                                  .map(value -> value.values().get(field.id().toString()))
                                  .orElse(null)))
                  .toList();
          return new CustomFieldValues(
              entityId,
              scope,
              configuration
                  .map(value -> new CustomizationRevision(value.id(), value.version()))
                  .orElse(new CustomizationRevision(null, 0)),
              stored.map(StoredValues::revision).orElse(new CustomizationRevision(null, 0)),
              fields,
              stored.map(StoredValues::updatedAt).orElse(null));
        });
  }

  private record StoredValues(
      CustomizationRevision revision,
      java.time.Instant updatedAt,
      java.util.Map<String, Object> values) {}

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
