package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.ImportDataQueries;
import com.apptolast.organization.application.ImportInvalidFileException;
import com.apptolast.organization.application.ImportPreview;
import com.apptolast.organization.application.StorageUnavailableException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

public final class PostgresImportDataStore
    implements ImportDataQueries,
        com.apptolast.organization.application.ImportDataCommands,
        com.apptolast.organization.application.ImportReceiptQueries {
  private final JdbcTemplate jdbc;
  private final TransactionTemplate reading;
  private final TransactionTemplate writing;

  public PostgresImportDataStore(JdbcTemplate jdbc, PlatformTransactionManager transactions) {
    this.jdbc = jdbc;
    reading = new TransactionTemplate(transactions);
    reading.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    writing = new TransactionTemplate(transactions);
    writing.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
  }

  public ImportPreview preview(String owner, InputStream body) {
    return stored(
        () ->
            reading.execute(
                status -> {
                  jdbc.queryForObject("SELECT pg_current_snapshot()::text", String.class);
                  try {
                    var header = stage(body);
                    if (!owner.equals(header.owner())) throw new ImportInvalidFileException();
                    validate(owner);
                    var identical = compare(owner);
                    return new ImportPreview(
                        header.fileSha256(),
                        header.byteLength(),
                        header.owner(),
                        header.exportedAt(),
                        header.counts(),
                        header.counts().minus(identical),
                        identical,
                        List.of());
                  } catch (IOException failure) {
                    throw new StorageUnavailableException(failure);
                  }
                }));
  }

  private ImportJsonReader.Header stage(InputStream body) throws IOException {
    var json =
        com.fasterxml.jackson.databind.json.JsonMapper.builder()
            .enable(com.fasterxml.jackson.core.json.JsonWriteFeature.ESCAPE_NON_ASCII)
            .build();
    jdbc.execute(
        "CREATE TEMP TABLE import_stage(collection TEXT NOT NULL,raw TEXT NOT NULL,payload JSONB) ON COMMIT DROP");
    return new ImportJsonReader()
        .read(
            body,
            (collection, row) ->
                jdbc.update(
                    "INSERT INTO import_stage(collection,raw) VALUES (?,?)",
                    collection,
                    json.writeValueAsString(row)));
  }

  private com.apptolast.organization.application.ImportCounts compare(String owner) {
    long projects =
        jdbc.queryForObject(
            """
        SELECT count(*) FROM import_stage s JOIN projects p ON p.id::text=s.payload->>'id'
        WHERE s.collection='projects' AND p.owner_id=?
          AND p.name=s.payload->>'name'
          AND p.description IS NOT DISTINCT FROM s.payload->>'description'
          AND p.status=s.payload->>'status'
          AND p.version::text=s.payload->>'version'
          AND p.created_at=(s.payload->>'createdAt')::timestamptz
          AND p.updated_at=(s.payload->>'updatedAt')::timestamptz
        """,
            Long.class,
            owner);
    long occupiedProjects =
        jdbc.queryForObject(
            "SELECT count(*) FROM import_stage s JOIN projects p ON p.id::text=s.payload->>'id' WHERE s.collection='projects'",
            Long.class);
    if (occupiedProjects != projects)
      throw new com.apptolast.organization.application.ImportConflictException();
    long tasks =
        jdbc.queryForObject(
            """
        SELECT count(*) FROM import_stage s JOIN tasks t ON t.id::text=s.payload->>'id'
          JOIN projects p ON p.id=t.project_id
        WHERE s.collection='tasks' AND p.owner_id=?
          AND t.project_id::text=s.payload->>'projectId'
          AND t.parent_id::text IS NOT DISTINCT FROM s.payload->>'parentId'
          AND t.title=s.payload->>'title'
          AND t.completion_criterion=s.payload->>'completionCriterion'
          AND t.estimated_minutes IS NOT DISTINCT FROM (s.payload->>'estimatedMinutes')::integer
          AND t.status=s.payload->>'status' AND t.version::text=s.payload->>'version'
          AND t.completed_at IS NOT DISTINCT FROM (s.payload->>'completedAt')::timestamptz
          AND t.created_at=(s.payload->>'createdAt')::timestamptz
          AND t.updated_at=(s.payload->>'updatedAt')::timestamptz
        """,
            Long.class,
            owner);
    long occupiedTasks =
        jdbc.queryForObject(
            "SELECT count(*) FROM import_stage s JOIN tasks t ON t.id::text=s.payload->>'id' WHERE s.collection='tasks'",
            Long.class);
    if (occupiedTasks != tasks)
      throw new com.apptolast.organization.application.ImportConflictException();
    return new com.apptolast.organization.application.ImportCounts(
        projects, tasks, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
  }

  private void validate(String owner) {
    if (jdbc.queryForObject(
        "SELECT EXISTS(SELECT 1 FROM import_stage WHERE NOT pg_input_is_valid(raw,'jsonb'))",
        Boolean.class)) throw new ImportInvalidFileException();
    jdbc.update("UPDATE import_stage SET payload=raw::jsonb");
    if (jdbc.queryForObject(
        """
        SELECT EXISTS(SELECT 1 FROM import_stage WHERE collection='projects' AND
          (jsonb_typeof(payload)='object'
            AND jsonb_typeof(payload->'version')='string'
            AND (payload->>'version') ~ '^(0|[1-9][0-9]*)$'
            AND pg_input_is_valid(payload->>'version','bigint')
            AND payload ?& ARRAY['id','name','description','status','version','createdAt','updatedAt']
            AND payload-ARRAY['id','name','description','status','version','createdAt','updatedAt']='{}'::jsonb) IS NOT TRUE)
        """,
        Boolean.class)) throw new ImportInvalidFileException();
    jdbc.query(
        connection -> {
          var statement =
              connection.prepareStatement(
                  "SELECT payload::text FROM import_stage WHERE collection='projects'");
          statement.setFetchSize(1);
          return statement;
        },
        (org.springframework.jdbc.core.ResultSetExtractor<Void>)
            rows -> {
              var json = new com.fasterxml.jackson.databind.ObjectMapper();
              while (rows.next()) {
                try {
                  ImportRecordValidator.project(json.readTree(rows.getString(1)), owner);
                } catch (IOException invalidStorage) {
                  throw new StorageUnavailableException(invalidStorage);
                }
              }
              return null;
            });
  }

  public java.util.Optional<com.apptolast.organization.application.ImportReceipt> find(
      String owner, java.util.UUID key) {
    return stored(
        () ->
            jdbc
                .query(
                    "SELECT request_key,file_sha256,byte_length,recorded_at,outcome,inserted_counts::text,identical_counts::text FROM import_receipts WHERE owner_id=? AND request_key=?",
                    (row, index) -> {
                      try {
                        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        return new com.apptolast.organization.application.ImportReceipt(
                            row.getObject("request_key", java.util.UUID.class),
                            row.getString("file_sha256"),
                            row.getLong("byte_length"),
                            row.getObject("recorded_at", java.time.OffsetDateTime.class)
                                .toInstant(),
                            row.getString("outcome"),
                            mapper.readValue(
                                row.getString("inserted_counts"),
                                com.apptolast.organization.application.ImportCounts.class),
                            mapper.readValue(
                                row.getString("identical_counts"),
                                com.apptolast.organization.application.ImportCounts.class));
                      } catch (IOException invalid) {
                        throw new StorageUnavailableException(invalid);
                      }
                    },
                    owner,
                    key)
                .stream()
                .findFirst());
  }

  public com.apptolast.organization.application.ImportReceipt apply(
      String owner,
      java.util.UUID key,
      String expectedSha256,
      InputStream body,
      java.util.function.Supplier<java.time.Instant> recordedAt) {
    return stored(
        () ->
            writing.execute(
                status -> {
                  jdbc.execute("SET LOCAL lock_timeout='2s'");
                  jdbc.execute("SET LOCAL statement_timeout='10s'");
                  final ImportJsonReader.Header header;
                  try {
                    header = stage(body);
                  } catch (IOException failure) {
                    throw new StorageUnavailableException(failure);
                  }
                  if (!expectedSha256.equals(header.fileSha256()))
                    throw new com.apptolast.organization.application.ImportFileChangedException();
                  if (!owner.equals(header.owner())) throw new ImportInvalidFileException();
                  validate(owner);
                  for (var scope : List.of("PROJECT", "TASK")) {
                    jdbc.queryForObject(
                        "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))",
                        Object.class,
                        "customization:" + owner + ":" + scope);
                  }
                  for (var table :
                      List.of(
                          "appearance_preferences",
                          "availability_preferences",
                          "block_changes",
                          "block_projections",
                          "customization_preferences",
                          "import_receipts",
                          "planned_blocks",
                          "project_custom_field_values",
                          "projects",
                          "task_custom_field_values",
                          "task_status_history",
                          "tasks",
                          "work_session_changes",
                          "work_session_intervals",
                          "work_sessions")) {
                    jdbc.execute("LOCK TABLE " + table + " IN EXCLUSIVE MODE");
                  }
                  var previous = find(owner, key);
                  if (previous.isPresent()) {
                    if (!previous.get().fileSha256().equals(header.fileSha256()))
                      throw new com.apptolast.organization.application.ImportKeyReusedException();
                    return previous.get();
                  }
                  var identical = compare(owner);
                  var inserted = header.counts().minus(identical);
                  int insertedProjects =
                      jdbc.update(
                          """
                      INSERT INTO projects(id,owner_id,name,description,status,version,created_at,updated_at)
                      SELECT (payload->>'id')::uuid,?,payload->>'name',payload->>'description',payload->>'status',
                        (payload->>'version')::bigint,(payload->>'createdAt')::timestamptz,(payload->>'updatedAt')::timestamptz
                      FROM import_stage s WHERE collection='projects'
                        AND NOT EXISTS(SELECT 1 FROM projects p WHERE p.id::text=s.payload->>'id')
                      """,
                          owner);
                  if (insertedProjects != inserted.projects())
                    throw new StorageUnavailableException(
                        new IllegalStateException("Imported projects were not persisted"));
                  int insertedTasks =
                      jdbc.update(
                          """
                      INSERT INTO tasks(id,project_id,parent_id,title,completion_criterion,estimated_minutes,status,version,completed_at,created_at,updated_at)
                      SELECT (payload->>'id')::uuid,(payload->>'projectId')::uuid,(payload->>'parentId')::uuid,
                        payload->>'title',payload->>'completionCriterion',(payload->>'estimatedMinutes')::integer,
                        payload->>'status',(payload->>'version')::bigint,(payload->>'completedAt')::timestamptz,
                        (payload->>'createdAt')::timestamptz,(payload->>'updatedAt')::timestamptz
                      FROM import_stage s WHERE collection='tasks'
                        AND NOT EXISTS(SELECT 1 FROM tasks t WHERE t.id::text=s.payload->>'id')
                      """);
                  if (insertedTasks != inserted.tasks())
                    throw new StorageUnavailableException(
                        new IllegalStateException("Imported tasks were not persisted"));
                  var receipt =
                      new com.apptolast.organization.application.ImportReceipt(
                          key,
                          header.fileSha256(),
                          header.byteLength(),
                          recordedAt.get(),
                          insertedProjects + insertedTasks == 0 ? "NO_CHANGE" : "IMPORTED",
                          inserted,
                          identical);
                  try {
                    var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    int affected =
                        jdbc.update(
                            "INSERT INTO import_receipts(owner_id,request_key,file_sha256,byte_length,recorded_at,outcome,inserted_counts,identical_counts) VALUES (?,?,?,?,?,?,?::jsonb,?::jsonb)",
                            owner,
                            key,
                            receipt.fileSha256(),
                            receipt.byteLength(),
                            java.time.OffsetDateTime.ofInstant(
                                receipt.recordedAt(), java.time.ZoneOffset.UTC),
                            receipt.outcome(),
                            mapper.writeValueAsString(receipt.insertedCounts()),
                            mapper.writeValueAsString(receipt.identicalCounts()));
                    if (affected != 1)
                      throw new StorageUnavailableException(
                          new IllegalStateException("Import receipt was not persisted"));
                  } catch (IOException failure) {
                    throw new StorageUnavailableException(failure);
                  }
                  return receipt;
                }));
  }

  private static <T> T stored(java.util.function.Supplier<T> operation) {
    try {
      return operation.get();
    } catch (org.springframework.dao.DataAccessException
        | org.springframework.transaction.TransactionException failure) {
      throw new StorageUnavailableException(failure);
    }
  }
}
