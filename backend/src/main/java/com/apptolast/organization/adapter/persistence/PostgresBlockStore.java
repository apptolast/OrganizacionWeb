package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.*;
import java.util.*;
import java.util.function.Function;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public final class PostgresBlockStore implements BlockPlanning, BlockCommit, BlockQueries, BlockEditing {
  public BlockChangeConfirmation cancel(String owner,UUID project,UUID task,UUID id,UUID key,Function<BlockState,BlockMutation> operation) {
    return execute(status -> {
      // Reuse the ownership/context locks before the owner mutex and original block row.
      detail(owner,project,task,id);
      var prior=changeReplay(task,key);
      if(prior.isPresent()) return new BlockChangeConfirmation(prior.get(),true);
      jdbc.queryForList("SELECT id FROM availability_preferences WHERE owner_id=? FOR UPDATE",owner);
      jdbc.queryForList("SELECT id FROM planned_blocks WHERE id=? FOR UPDATE",id);
      var mutation=operation.apply(state(owner,project,task,id));
      var next=mutation.state();
      jdbc.update("INSERT INTO block_projections(block_id,version,status,updated_at) VALUES (?,?,?,?) ON CONFLICT(block_id) DO UPDATE SET version=excluded.version,status=excluded.status,updated_at=excluded.updated_at",id,next.version(),next.status(),java.sql.Timestamp.from(next.updatedAt()));
      var receipt=mutation.receipt();
      var event=mutation.event();
      try {
        jdbc.update("INSERT INTO block_changes(id,project_id,task_id,block_id,request_key,kind,version,occurred_at,receipt) VALUES (?,?,?,?,?,?,?,?,?::jsonb)",receipt.id(),project,task,id,key,receipt.kind(),receipt.version(),java.sql.Timestamp.from(receipt.occurredAt()),json.writeValueAsString(receipt));
        jdbc.update("INSERT INTO outbox_events(event_id,aggregate_id,owner_id,event_type,schema_version,occurred_at,payload) VALUES (?,?,?,?,?,?,?::jsonb)",event.eventId(),project,owner,event.type(),event.schemaVersion(),java.sql.Timestamp.from(event.occurredAt()),json.writeValueAsString(event));
      } catch(com.fasterxml.jackson.core.JsonProcessingException error) { throw new IllegalStateException("Event serialization failed",error); }
      return new BlockChangeConfirmation(receipt,false);
    });
  }
  private Optional<BlockChangeReceipt> changeReplay(UUID task,UUID key) {
    return jdbc.query("SELECT receipt::text FROM block_changes WHERE task_id=? AND request_key=?", (row,n) -> {
      try { return json.readValue(row.getString(1),BlockChangeReceipt.class); }
      catch(com.fasterxml.jackson.core.JsonProcessingException error) { throw new IllegalStateException("Stored receipt is invalid",error); }
    },task,key).stream().findFirst();
  }
  public BlockState state(String owner, UUID project, UUID task, UUID id) {
    return execute(status -> {
      var block = detail(owner,project,task,id);
      return jdbc.query("""
          SELECT b.id,b.project_id,b.task_id,b.objective,b.allow_over_budget,b.created_at,
                 coalesce(p.start_local,b.start_local) AS start_local,
                 coalesce(p.end_local,b.end_local) AS end_local,
                 coalesce(p.zone_id,b.zone_id) AS zone_id,
                 coalesce(p.start_offset,b.start_offset) AS start_offset,
                 coalesce(p.end_offset,b.end_offset) AS end_offset,
                 coalesce(p.start_at,b.start_at) AS start_at,
                 coalesce(p.end_at,b.end_at) AS end_at,
                 coalesce(p.duration_minutes,b.duration_minutes) AS duration_minutes,
                 p.version,p.status,p.updated_at
          FROM planned_blocks b JOIN block_projections p ON p.block_id=b.id WHERE b.id=?
          """, (row,n) -> new BlockState(MAPPER.mapRow(row,n),row.getLong("version"),row.getString("status"),row.getTimestamp("updated_at").toInstant()),id)
        .stream().findFirst().orElseGet(() -> BlockState.initial(block));
    });
  }
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transaction;
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
                "SELECT * FROM planned_blocks WHERE project_id=? AND task_id=? ORDER BY created_at DESC,id DESC LIMIT 21",
                MAPPER,
                project,
                task);
          return jdbc.query(
              "SELECT * FROM planned_blocks WHERE project_id=? AND task_id=? AND (created_at,id)<(?,?) ORDER BY created_at DESC,id DESC LIMIT 21",
              MAPPER,
              project,
              task,
              java.sql.Timestamp.from(after.createdAt()),
              after.id());
        });
  }

  public PlannedBlock detail(String owner, UUID project, UUID task, UUID block) {
    return find(owner, project, task, block, "id");
  }

  public PlannedBlock byRequest(String owner, UUID project, UUID task, UUID key) {
    return find(owner, project, task, key, "request_key");
  }

  private PlannedBlock find(String owner, UUID project, UUID task, UUID id, String column) {
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
          return jdbc
              .query(
                  "SELECT * FROM planned_blocks WHERE project_id=? AND task_id=? AND "
                      + column
                      + "=?",
                  MAPPER,
                  project,
                  task,
                  id)
              .stream()
              .findFirst()
              .orElseThrow(BlockNotFoundException::new);
        });
  }

  public PostgresBlockStore(
      JdbcTemplate jdbc,
      TransactionTemplate transaction,
      PostgresAvailabilityStore availability,
      com.fasterxml.jackson.databind.ObjectMapper json) {
    this.json = json;
    this.jdbc = jdbc;
    this.transaction = transaction;
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
    // ponytail: load owner history for now; query intersecting days if measured volume warrants it.
    return jdbc.query(
        "SELECT b.* FROM planned_blocks b JOIN projects p ON p.id=b.project_id WHERE p.owner_id=?",
        MAPPER,
        owner);
  }

  private <T> T execute(org.springframework.transaction.support.TransactionCallback<T> operation) {
    try {
      return transaction.execute(operation);
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
                  "INSERT INTO planned_blocks(id,project_id,task_id,request_key,objective,start_local,end_local,zone_id,start_offset,end_offset,allow_over_budget,start_at,end_at,duration_minutes,created_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
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
                    "INSERT INTO outbox_events(event_id,aggregate_id,owner_id,event_type,schema_version,occurred_at,payload) VALUES (?,?,?,?,?,?,?::jsonb)",
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
