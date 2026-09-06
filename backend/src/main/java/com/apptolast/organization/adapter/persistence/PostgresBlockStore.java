package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.*;
import java.util.*;
import java.util.function.Function;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public final class PostgresBlockStore
    implements BlockPlanning, BlockCommit, BlockQueries, BlockEditing, BlockMoving {
  public BlockChangeConfirmation cancel(
      String owner,
      UUID project,
      UUID task,
      UUID id,
      UUID key,
      Function<BlockState, BlockMutation> operation) {
    return execute(
        status -> {
          // Reuse the ownership/context locks before the owner mutex and original block row.
          find(owner, project, task, id, "id");
          var prior = changeReplay(task, key);
          if (prior.isPresent()) return cancelledReplay(prior.get(), id);
          jdbc.queryForList(
              "SELECT id FROM availability_preferences WHERE owner_id=? FOR UPDATE", owner);
          jdbc.queryForList("SELECT id FROM planned_blocks WHERE id=? FOR UPDATE", id);
          prior = changeReplay(task, key);
          if (prior.isPresent()) return cancelledReplay(prior.get(), id);
          var mutation = operation.apply(currentState(owner, project, task, id));
          return persistChange(owner, project, task, id, key, mutation);
        });
  }

  private BlockChangeConfirmation cancelledReplay(BlockChangeReceipt receipt, UUID block) {
    if (!receipt.blockId().equals(block) || !receipt.kind().equals("CANCELLED"))
      throw new BlockIdempotencyConflictException();
    return new BlockChangeConfirmation(receipt, true);
  }

  public BlockPreview preview(
      String owner,
      UUID project,
      UUID task,
      UUID block,
      Function<MoveContext, BlockPreview> operation) {
    return preview(
        owner,
        project,
        task,
        planning -> {
          jdbc.queryForList("SELECT id FROM planned_blocks WHERE id=? FOR SHARE", block);
          return operation.apply(
              new MoveContext(currentState(owner, project, task, block), planning));
        });
  }

  public BlockChangeConfirmation commit(
      String owner,
      UUID project,
      UUID task,
      UUID block,
      UUID key,
      BlockMoveRequest request,
      Function<MoveContext, BlockMutation> operation) {
    return execute(
        status -> {
          find(owner, project, task, block, "id");
          var prior = changeReplay(task, key);
          if (prior.isPresent()) return movedReplay(prior.get(), block, request);
          var locked =
              jdbc.queryForList(
                  "SELECT id FROM availability_preferences WHERE owner_id=? FOR UPDATE", owner);
          jdbc.queryForList("SELECT id FROM planned_blocks WHERE id=? FOR UPDATE", block);
          prior = changeReplay(task, key);
          if (prior.isPresent()) return movedReplay(prior.get(), block, request);
          var current = currentState(owner, project, task, block);
          var planning =
              new BlockPlanningContext(
                  jdbc.queryForObject(
                      "SELECT status FROM projects WHERE id=?", String.class, project),
                  jdbc.queryForObject("SELECT status FROM tasks WHERE id=?", String.class, task),
                  locked.isEmpty() ? Optional.empty() : availability.find(owner),
                  ownerBlocks(owner));
          return persistChange(
              owner,
              project,
              task,
              block,
              key,
              operation.apply(new MoveContext(current, planning)));
        });
  }

  private BlockChangeConfirmation movedReplay(
      BlockChangeReceipt receipt, UUID block, BlockMoveRequest request) {
    if (!receipt.blockId().equals(block)
        || !receipt.kind().equals("RESCHEDULED")
        || !receipt
            .after()
            .request()
            .equals(request.withObjective(receipt.after().request().objective())))
      throw new BlockIdempotencyConflictException();
    return new BlockChangeConfirmation(receipt, true);
  }

  private BlockChangeConfirmation persistChange(
      String owner, UUID project, UUID task, UUID id, UUID key, BlockMutation mutation) {
    var next = mutation.state();
    var request = next.block().request();
    var time = next.block().time();
    int projected =
        jdbc.update(
            "INSERT INTO"
                + " block_projections(block_id,version,status,updated_at,start_local,end_local,zone_id,start_offset,end_offset,start_at,end_at,duration_minutes)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(block_id) DO UPDATE SET"
                + " version=excluded.version,status=excluded.status,updated_at=excluded.updated_at,start_local=excluded.start_local,end_local=excluded.end_local,zone_id=excluded.zone_id,start_offset=excluded.start_offset,end_offset=excluded.end_offset,start_at=excluded.start_at,end_at=excluded.end_at,duration_minutes=excluded.duration_minutes",
            id,
            next.version(),
            next.status(),
            java.sql.Timestamp.from(next.updatedAt()),
            request.startLocal(),
            request.endLocal(),
            request.zoneId(),
            request.startOffset().getId(),
            request.endOffset().getId(),
            java.sql.Timestamp.from(time.startAt()),
            java.sql.Timestamp.from(time.endAt()),
            time.durationMinutes());
    if (projected != 1)
      throw new StorageUnavailableException(
          new IllegalStateException("Projection write did not affect one row"));
    var receipt = mutation.receipt();
    var event = mutation.event();
    try {
      int recorded =
          jdbc.update(
              "INSERT INTO"
                  + " block_changes(id,project_id,task_id,block_id,request_key,kind,version,occurred_at,receipt)"
                  + " VALUES (?,?,?,?,?,?,?,?,?::jsonb) ON CONFLICT(task_id,request_key) DO"
                  + " NOTHING",
              receipt.id(),
              project,
              task,
              id,
              key,
              receipt.kind(),
              receipt.version(),
              java.sql.Timestamp.from(receipt.occurredAt()),
              json.writeValueAsString(receipt));
      if (recorded == 0 && changeReplay(task, key).isPresent())
        throw new BlockIdempotencyConflictException();
      if (recorded != 1)
        throw new StorageUnavailableException(
            new IllegalStateException("Receipt insert did not affect one row"));
      int published =
          jdbc.update(
              "INSERT INTO"
                  + " outbox_events(event_id,aggregate_id,owner_id,event_type,schema_version,occurred_at,payload)"
                  + " VALUES (?,?,?,?,?,?,?::jsonb)",
              event.eventId(),
              project,
              owner,
              event.type(),
              event.schemaVersion(),
              java.sql.Timestamp.from(event.occurredAt()),
              json.writeValueAsString(event));
      if (published != 1)
        throw new StorageUnavailableException(
            new IllegalStateException("Outbox insert did not affect one row"));
    } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
      throw new IllegalStateException("Event serialization failed", error);
    }
    return new BlockChangeConfirmation(receipt, false);
  }

  private Optional<BlockChangeReceipt> changeReplay(UUID task, UUID key) {
    return jdbc
        .query(
            "SELECT receipt::text FROM block_changes WHERE task_id=? AND request_key=?",
            (row, n) -> {
              try {
                return json.readValue(row.getString(1), BlockChangeReceipt.class);
              } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
                throw new IllegalStateException("Stored receipt is invalid", error);
              }
            },
            task,
            key)
        .stream()
        .findFirst();
  }

  public BlockState state(String owner, UUID project, UUID task, UUID id) {
    return execute(
        readTransaction,
        status -> projected(findInTransaction(owner, project, task, id, "id", false)));
  }

  private BlockState currentState(String owner, UUID project, UUID task, UUID id) {
    return projected(find(owner, project, task, id, "id"));
  }

  static final String CURRENT_COLUMNS =
      """
      b.id,b.project_id,b.task_id,b.objective,b.allow_over_budget,b.created_at,
      coalesce(p.start_local,b.start_local) AS start_local,
      coalesce(p.end_local,b.end_local) AS end_local,
      coalesce(p.zone_id,b.zone_id) AS zone_id,
      coalesce(p.start_offset,b.start_offset) AS start_offset,
      coalesce(p.end_offset,b.end_offset) AS end_offset,
      coalesce(p.start_at,b.start_at) AS start_at,
      coalesce(p.end_at,b.end_at) AS end_at,
      coalesce(p.duration_minutes,b.duration_minutes) AS duration_minutes
      """;

  private BlockState projected(PlannedBlock block) {
    return jdbc
        .query(
            "SELECT "
                + CURRENT_COLUMNS
                + ",p.version,p.status,p.updated_at FROM planned_blocks b JOIN block_projections p"
                + " ON p.block_id=b.id WHERE b.id=?",
            (row, n) ->
                new BlockState(
                    MAPPER.mapRow(row, n),
                    row.getLong("version"),
                    row.getString("status"),
                    row.getTimestamp("updated_at").toInstant()),
            block.id())
        .stream()
        .findFirst()
        .orElseGet(() -> BlockState.initial(block));
  }

  private final JdbcTemplate jdbc;
  private final TransactionTemplate transaction;
  private final TransactionTemplate readTransaction;
  private final PostgresAvailabilityStore availability;
  private final com.fasterxml.jackson.databind.ObjectMapper json;

  public List<PlannedBlock> list(String owner, UUID project, UUID task, BlockPosition after) {
    return execute(
        status -> {
          if (jdbc.queryForList(
                  "SELECT status FROM projects WHERE id=? AND owner_id=? FOR SHARE",
                  String.class,
                  project,
                  owner)
              .isEmpty()) throw new ResourceNotFoundException();
          if (jdbc.queryForList(
                  "SELECT status FROM tasks WHERE project_id=? AND id=? FOR SHARE",
                  String.class,
                  project,
                  task)
              .isEmpty()) throw new ResourceNotFoundException();
          if (after == null)
            return jdbc.query(
                "SELECT "
                    + CURRENT_COLUMNS
                    + " FROM planned_blocks b LEFT JOIN block_projections p ON p.block_id=b.id"
                    + " WHERE b.project_id=? AND b.task_id=? AND"
                    + " coalesce(p.status,'planned')='planned' ORDER BY b.created_at DESC,b.id DESC"
                    + " LIMIT 21",
                MAPPER,
                project,
                task);
          return jdbc.query(
              "SELECT "
                  + CURRENT_COLUMNS
                  + " FROM planned_blocks b LEFT JOIN block_projections p ON p.block_id=b.id WHERE"
                  + " b.project_id=? AND b.task_id=? AND (b.created_at,b.id)<(?,?) AND"
                  + " coalesce(p.status,'planned')='planned' ORDER BY b.created_at DESC,b.id DESC"
                  + " LIMIT 21",
              MAPPER,
              project,
              task,
              java.sql.Timestamp.from(after.createdAt()),
              after.id());
        });
  }

  public PlannedBlock detail(String owner, UUID project, UUID task, UUID block) {
    var current = state(owner, project, task, block);
    if (current.status().equals("cancelled")) throw new BlockNotFoundException();
    return current.block();
  }

  public PlannedBlock byRequest(String owner, UUID project, UUID task, UUID key) {
    return find(owner, project, task, key, "request_key");
  }

  private PlannedBlock find(String owner, UUID project, UUID task, UUID id, String column) {
    return execute(status -> findInTransaction(owner, project, task, id, column, true));
  }

  private PlannedBlock findInTransaction(
      String owner, UUID project, UUID task, UUID id, String column, boolean locking) {
    String lock = locking ? " FOR SHARE" : "";
    if (jdbc.queryForList(
            "SELECT status FROM projects WHERE id=? AND owner_id=?" + lock,
            String.class,
            project,
            owner)
        .isEmpty()) throw new ResourceNotFoundException();
    if (jdbc.queryForList(
            "SELECT status FROM tasks WHERE project_id=? AND id=?" + lock,
            String.class,
            project,
            task)
        .isEmpty()) throw new ResourceNotFoundException();
    return jdbc
        .query(
            "SELECT * FROM planned_blocks WHERE project_id=? AND task_id=? AND " + column + "=?",
            MAPPER,
            project,
            task,
            id)
        .stream()
        .findFirst()
        .orElseThrow(BlockNotFoundException::new);
  }

  public PostgresBlockStore(
      JdbcTemplate jdbc,
      TransactionTemplate transaction,
      PostgresAvailabilityStore availability,
      com.fasterxml.jackson.databind.ObjectMapper json) {
    this.json = json;
    this.jdbc = jdbc;
    this.transaction = transaction;
    this.readTransaction = new TransactionTemplate(transaction.getTransactionManager());
    this.readTransaction.setReadOnly(true);
    this.readTransaction.setIsolationLevel(
        org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ);
    this.availability = availability;
  }

  static final org.springframework.jdbc.core.RowMapper<PlannedBlock> MAPPER =
      (row, n) -> {
        var request =
            new BlockRequest(
                row.getString("objective"),
                row.getObject("start_local", java.time.LocalDateTime.class),
                row.getObject("end_local", java.time.LocalDateTime.class),
                row.getString("zone_id"),
                java.time.ZoneOffset.of(row.getString("start_offset")),
                java.time.ZoneOffset.of(row.getString("end_offset")),
                row.getBoolean("allow_over_budget"));
        var time =
            new ResolvedBlockTime(
                row.getTimestamp("start_at").toInstant(),
                row.getTimestamp("end_at").toInstant(),
                request.startOffset(),
                request.endOffset(),
                row.getInt("duration_minutes"));
        return new PlannedBlock(
            row.getObject("id", UUID.class),
            row.getObject("project_id", UUID.class),
            row.getObject("task_id", UUID.class),
            request,
            time,
            row.getTimestamp("created_at").toInstant());
      };

  public BlockPreview preview(
      String owner,
      UUID project,
      UUID task,
      Function<BlockPlanningContext, BlockPreview> operation) {
    return execute(
        status -> {
          var projectStatus =
              jdbc
                  .queryForList(
                      "SELECT status FROM projects WHERE id=? AND owner_id=? FOR SHARE",
                      String.class,
                      project,
                      owner)
                  .stream()
                  .findFirst()
                  .orElseThrow(ResourceNotFoundException::new);
          var taskStatus =
              jdbc
                  .queryForList(
                      "SELECT status FROM tasks WHERE project_id=? AND id=? FOR SHARE",
                      String.class,
                      project,
                      task)
                  .stream()
                  .findFirst()
                  .orElseThrow(ResourceNotFoundException::new);
          var locked =
              jdbc.queryForList(
                  "SELECT id FROM availability_preferences WHERE owner_id=? FOR SHARE", owner);
          return operation.apply(
              new BlockPlanningContext(
                  projectStatus,
                  taskStatus,
                  locked.isEmpty() ? Optional.empty() : availability.find(owner),
                  ownerBlocks(owner)));
        });
  }

  private List<PlannedBlock> ownerBlocks(String owner) {
    // ponytail: load current owner reservations; narrow days if measured volume warrants it.
    return jdbc.query(
        "SELECT "
            + CURRENT_COLUMNS
            + " FROM planned_blocks b LEFT JOIN block_projections p ON p.block_id=b.id JOIN"
            + " projects owner_project ON owner_project.id=b.project_id WHERE"
            + " owner_project.owner_id=? AND coalesce(p.status,'planned')='planned'",
        MAPPER,
        owner);
  }

  private <T> T execute(org.springframework.transaction.support.TransactionCallback<T> operation) {
    return execute(transaction, operation);
  }

  private <T> T execute(
      TransactionTemplate template,
      org.springframework.transaction.support.TransactionCallback<T> operation) {
    try {
      return template.execute(operation);
    } catch (org.springframework.dao.DataAccessException
        | org.springframework.transaction.TransactionException error) {
      throw new StorageUnavailableException(error);
    }
  }

  private Optional<BlockCreation> replay(UUID project, UUID task, UUID key, BlockRequest request) {
    return jdbc
        .query(
            "SELECT * FROM planned_blocks WHERE project_id=? AND task_id=? AND request_key=?",
            MAPPER,
            project,
            task,
            key)
        .stream()
        .findFirst()
        .map(
            block -> {
              if (!block.request().equals(request)) throw new BlockIdempotencyConflictException();
              return new BlockCreation(block, true);
            });
  }

  public BlockCreation commit(
      String owner,
      UUID project,
      UUID task,
      UUID key,
      BlockRequest request,
      Function<BlockPlanningContext, BlockChange> operation) {
    return execute(
        status -> {
          var projectStatus =
              jdbc
                  .queryForList(
                      "SELECT status FROM projects WHERE id=? AND owner_id=? FOR SHARE",
                      String.class,
                      project,
                      owner)
                  .stream()
                  .findFirst()
                  .orElseThrow(ResourceNotFoundException::new);
          var taskStatus =
              jdbc
                  .queryForList(
                      "SELECT status FROM tasks WHERE project_id=? AND id=? FOR SHARE",
                      String.class,
                      project,
                      task)
                  .stream()
                  .findFirst()
                  .orElseThrow(ResourceNotFoundException::new);
          var prior = replay(project, task, key, request);
          if (prior.isPresent()) return prior.get();
          var locked =
              jdbc.queryForList(
                  "SELECT id FROM availability_preferences WHERE owner_id=? FOR UPDATE", owner);
          prior = replay(project, task, key, request);
          if (prior.isPresent()) return prior.get();
          var change =
              operation.apply(
                  new BlockPlanningContext(
                      projectStatus,
                      taskStatus,
                      locked.isEmpty() ? Optional.empty() : availability.find(owner),
                      ownerBlocks(owner)));
          var block = change.block();
          var time = block.time();
          int inserted =
              jdbc.update(
                  "INSERT INTO"
                      + " planned_blocks(id,project_id,task_id,request_key,objective,start_local,end_local,zone_id,start_offset,end_offset,allow_over_budget,start_at,end_at,duration_minutes,created_at)"
                      + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                  block.id(),
                  project,
                  task,
                  key,
                  request.objective(),
                  request.startLocal(),
                  request.endLocal(),
                  request.zoneId(),
                  request.startOffset().getId(),
                  request.endOffset().getId(),
                  request.allowOverBudget(),
                  java.sql.Timestamp.from(time.startAt()),
                  java.sql.Timestamp.from(time.endAt()),
                  time.durationMinutes(),
                  java.sql.Timestamp.from(block.createdAt()));
          if (inserted != 1)
            throw new StorageUnavailableException(
                new IllegalStateException("Block insert did not affect one row"));
          var event = change.event();
          try {
            int published =
                jdbc.update(
                    "INSERT INTO"
                        + " outbox_events(event_id,aggregate_id,owner_id,event_type,schema_version,occurred_at,payload)"
                        + " VALUES (?,?,?,?,?,?,?::jsonb)",
                    event.eventId(),
                    project,
                    owner,
                    event.type(),
                    event.schemaVersion(),
                    java.sql.Timestamp.from(event.occurredAt()),
                    json.writeValueAsString(event));
            if (published != 1)
              throw new StorageUnavailableException(
                  new IllegalStateException("Outbox insert did not affect one row"));
          } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
            throw new IllegalStateException("Event serialization failed", error);
          }
          return new BlockCreation(block, false);
        });
  }
}
