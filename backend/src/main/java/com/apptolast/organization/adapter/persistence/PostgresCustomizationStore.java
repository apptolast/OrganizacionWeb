package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.CustomFieldValuesEditing;
import com.apptolast.organization.application.CustomFieldValuesQueries;
import com.apptolast.organization.application.CustomizationEditing;
import com.apptolast.organization.application.CustomizationQueries;
import com.apptolast.organization.application.StorageUnavailableException;
import com.apptolast.organization.domain.CustomFieldDefinition;
import com.apptolast.organization.domain.CustomFieldValues;
import com.apptolast.organization.domain.CustomFieldValuesCollection;
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
    implements CustomizationQueries,
        CustomizationEditing,
        CustomFieldValuesQueries,
        CustomFieldValuesEditing {
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
    return storedTransaction(
        writing,
        status -> {
          jdbc.query(
              "SELECT pg_advisory_xact_lock(hashtextextended(?,0))",
              row -> {},
              "customization:" + owner + ":" + scope.name());
          var previous = select(owner, scope);
          var changed = operation.apply(previous);
          if (previous.isPresent() && previous.get().equals(changed)) return changed;
          try {
            int affected =
                jdbc.update(
                    "INSERT INTO customization_preferences (id,owner_id,scope,visible_fields,custom_fields,version,updated_at) VALUES (?,?,?,?::jsonb,?::jsonb,?,?) ON CONFLICT (owner_id,scope) DO UPDATE SET visible_fields=EXCLUDED.visible_fields,custom_fields=EXCLUDED.custom_fields,version=EXCLUDED.version,updated_at=EXCLUDED.updated_at",
                    changed.id(),
                    owner,
                    scope.name(),
                    json.writeValueAsString(changed.visibleFields()),
                    json.writeValueAsString(changed.customFields()),
                    changed.version(),
                    changed.updatedAt().atOffset(java.time.ZoneOffset.UTC));
            if (affected != 1)
              throw new StorageUnavailableException(
                  new IllegalStateException("Customization write affected no single row"));
          } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
            throw new StorageUnavailableException(error);
          }
          return changed;
        });
  }

  public Optional<Customization> find(String owner, CustomizationScope scope) {
    try {
      return storedTransaction(reading, status -> select(owner, scope));
    } catch (RuntimeException error) {
      throw new StorageUnavailableException(error);
    }
  }

  public CustomFieldValues changeValues(
      String owner,
      CustomizationScope scope,
      UUID projectId,
      UUID entityId,
      java.util.function.BiFunction<
              Optional<Customization>,
              Optional<CustomFieldValuesCollection>,
              CustomFieldValuesCollection>
          operation) {
    return storedTransaction(
        writing,
        status -> {
          requireOwned(owner, scope, projectId, entityId);
          jdbc.query(
              "SELECT pg_advisory_xact_lock(hashtextextended(?,0))",
              row -> {},
              "customization:" + owner + ":" + scope.name());
          var configuration = select(owner, scope);
          var previous = selectValues(owner, scope, entityId, configuration);
          var changed = operation.apply(configuration, previous);
          if (previous.isPresent() && previous.get().equals(changed))
            return CustomFieldValues.project(scope, entityId, configuration, previous);
          var table =
              scope == CustomizationScope.PROJECT
                  ? "project_custom_field_values"
                  : "task_custom_field_values";
          var column = scope == CustomizationScope.PROJECT ? "project_id" : "task_id";
          try {
            int affected =
                jdbc.update(
                    "INSERT INTO "
                        + table
                        + " (id,owner_id,"
                        + column
                        + ",field_values,version,updated_at) VALUES (?,?,?,?::jsonb,?,?) ON CONFLICT (owner_id,"
                        + column
                        + ") DO UPDATE SET field_values=EXCLUDED.field_values,version=EXCLUDED.version,updated_at=EXCLUDED.updated_at",
                    changed.id(),
                    owner,
                    entityId,
                    json.writeValueAsString(changed.values()),
                    changed.version(),
                    changed.updatedAt().atOffset(java.time.ZoneOffset.UTC));
            if (affected != 1)
              throw new StorageUnavailableException(
                  new IllegalStateException("Customization write affected no single row"));
          } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
            throw new StorageUnavailableException(error);
          }
          return CustomFieldValues.project(scope, entityId, configuration, Optional.of(changed));
        });
  }

  public CustomFieldValues find(
      String owner, CustomizationScope scope, UUID projectId, UUID entityId) {
    return storedTransaction(
        reading,
        status -> {
          requireOwned(owner, scope, projectId, entityId);
          var configuration = select(owner, scope);
          return CustomFieldValues.project(
              scope, entityId, configuration, selectValues(owner, scope, entityId, configuration));
        });
  }

  private void requireOwned(String owner, CustomizationScope scope, UUID projectId, UUID entityId) {
    if (!Boolean.TRUE.equals(
        jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM projects WHERE id=? AND owner_id=?)",
            Boolean.class,
            projectId,
            owner))) throw new com.apptolast.organization.application.ResourceNotFoundException();
    if (scope == CustomizationScope.TASK
        && !Boolean.TRUE.equals(
            jdbc.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM tasks WHERE id=? AND project_id=?)",
                Boolean.class,
                entityId,
                projectId)))
      throw new com.apptolast.organization.application.ResourceNotFoundException();
  }

  private Optional<CustomFieldValuesCollection> selectValues(
      String owner,
      CustomizationScope scope,
      UUID entityId,
      Optional<Customization> configuration) {
    var table =
        scope == CustomizationScope.PROJECT
            ? "project_custom_field_values"
            : "task_custom_field_values";
    var column = scope == CustomizationScope.PROJECT ? "project_id" : "task_id";
    return jdbc
        .query(
            "SELECT * FROM " + table + " WHERE owner_id=? AND " + column + "=?",
            (row, index) -> {
              try {
                var stored =
                    json.reader()
                        .with(
                            com.fasterxml.jackson.databind.DeserializationFeature
                                .USE_BIG_DECIMAL_FOR_FLOATS)
                        .readTree(row.getString("field_values"));
                if (!stored.isObject())
                  throw new IllegalArgumentException("Invalid stored values object");
                var values = new java.util.LinkedHashMap<UUID, Object>();
                var entries = stored.fields();
                while (entries.hasNext()) {
                  var entry = entries.next();
                  var id = storedUuid(entry.getKey());
                  var definition =
                      configuration.orElseThrow().customFields().stream()
                          .filter(field -> field.id().equals(id))
                          .findFirst()
                          .orElseThrow();
                  var node = entry.getValue();
                  Object value =
                      node.isNull()
                          ? null
                          : node.isNumber()
                              ? node.decimalValue()
                              : node.isTextual()
                                  ? node.textValue()
                                  : node.isBoolean() ? node.booleanValue() : node;
                  values.put(
                      id,
                      new com.apptolast.organization.domain.CustomFieldInput(id, value)
                          .canonical(definition.type(), 0));
                }
                return new CustomFieldValuesCollection(
                    java.util.Objects.requireNonNull(row.getObject("id", UUID.class)),
                    values,
                    storedVersion(row),
                    storedTime(row.getObject("updated_at", OffsetDateTime.class)));
              } catch (com.fasterxml.jackson.core.JsonProcessingException
                  | RuntimeException error) {
                throw new StorageUnavailableException(error);
              }
            },
            owner,
            entityId)
        .stream()
        .findFirst();
  }

  private Optional<Customization> select(String owner, CustomizationScope scope) {
    return jdbc
        .query(
            "SELECT * FROM customization_preferences WHERE owner_id=? AND scope=?",
            (row, index) -> {
              try {
                var definitions = json.readTree(row.getString("custom_fields"));
                if (!definitions.isArray() || definitions.size() > 12)
                  throw new IllegalArgumentException("Invalid stored definitions array");
                var ids = new java.util.HashSet<UUID>();
                var labels = new java.util.HashSet<String>();
                for (var definition : definitions) {
                  if (!definition.isObject()
                      || definition.size() != 4
                      || !definition.path("id").isTextual()
                      || !definition.path("label").isTextual()
                      || !definition.path("type").isTextual()
                      || !definition.path("active").isBoolean())
                    throw new StorageUnavailableException(
                        new IllegalArgumentException("Invalid stored custom field shape"));
                  var id = storedUuid(definition.get("id").textValue());
                  var label = definition.get("label").textValue();
                  if (!ids.add(id)
                      || !labels.add(label)
                      || !new com.apptolast.organization.domain.CustomFieldLabel(label)
                          .value()
                          .equals(label))
                    throw new IllegalArgumentException("Invalid stored custom field definition");
                }
                var view =
                    new com.apptolast.organization.domain.CustomizationView(
                        scope,
                        json.readValue(
                            row.getString("visible_fields"), new TypeReference<List<String>>() {}));
                return new Customization(
                    java.util.Objects.requireNonNull(row.getObject("id", UUID.class)),
                    owner,
                    scope,
                    view.visibleFields(),
                    json.readValue(
                        row.getString("custom_fields"),
                        new TypeReference<List<CustomFieldDefinition>>() {}),
                    storedVersion(row),
                    storedTime(row.getObject("updated_at", OffsetDateTime.class)));
              } catch (com.fasterxml.jackson.core.JsonProcessingException
                  | RuntimeException error) {
                throw new StorageUnavailableException(error);
              }
            },
            owner,
            scope.name())
        .stream()
        .findFirst();
  }

  private static java.time.Instant storedTime(OffsetDateTime value) {
    var utc = value.withOffsetSameInstant(java.time.ZoneOffset.UTC);
    if (utc.getYear() < 1 || utc.getYear() > 9999)
      throw new IllegalArgumentException("Invalid stored customization timestamp");
    return utc.toInstant();
  }

  private static long storedVersion(java.sql.ResultSet row) throws java.sql.SQLException {
    long version = row.getLong("version");
    if (row.wasNull() || version < 0)
      throw new IllegalArgumentException("Invalid stored customization version");
    return version;
  }

  private static UUID storedUuid(String text) {
    var id = UUID.fromString(text);
    if (!id.toString().equals(text)) throw new IllegalArgumentException("Invalid stored UUID text");
    return id;
  }

  private static <T> T storedTransaction(
      TransactionTemplate transaction,
      org.springframework.transaction.support.TransactionCallback<T> operation) {
    try {
      return transaction.execute(operation);
    } catch (org.springframework.dao.DataAccessException
        | org.springframework.transaction.TransactionException error) {
      throw new StorageUnavailableException(error);
    }
  }
}
