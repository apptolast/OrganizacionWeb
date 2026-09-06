package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.Task;
import com.apptolast.organization.domain.TaskPosition;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Component;

@Component
public final class PostgresTaskQueries implements TaskQueries, SubtaskQueries {
  private final JdbcTemplate jdbc;

  public PostgresTaskQueries(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  static final RowMapper<Task> MAPPER =
      (row, n) ->
          new Task(
              row.getObject("id", UUID.class),
              row.getObject("project_id", UUID.class),
              row.getString("title"),
              row.getString("completion_criterion"),
              row.getObject("estimated_minutes", Integer.class),
              row.getString("status"),
              row.getTimestamp("created_at").toInstant(),
              row.getTimestamp("updated_at").toInstant());

  public List<Task> list(String owner, UUID project, TaskPosition after) {
    try {
      requireProject(owner, project);
      var sql =
          "SELECT t.* FROM tasks t JOIN projects p ON p.id=t.project_id WHERE p.owner_id=? AND p.id=?";
      if (after == null)
        return jdbc.query(
            sql + " ORDER BY t.created_at DESC,t.id DESC LIMIT 21", MAPPER, owner, project);
      return jdbc.query(
          sql + " AND (t.created_at,t.id)<(?,?) ORDER BY t.created_at DESC,t.id DESC LIMIT 21",
          MAPPER,
          owner,
          project,
          Timestamp.from(after.createdAt()),
          after.id());
    } catch (DataAccessException error) {
      throw new StorageUnavailableException(error);
    }
  }

  public Task detail(String owner, UUID project, UUID id) {
    try {
      var rows =
          jdbc.query(
              "SELECT t.* FROM tasks t JOIN projects p ON p.id=t.project_id WHERE p.owner_id=? AND p.id=? AND t.id=?",
              MAPPER,
              owner,
              project,
              id);
      return rows.stream().findFirst().orElseThrow(ResourceNotFoundException::new);
    } catch (DataAccessException error) {
      throw new StorageUnavailableException(error);
    }
  }

  private void requireProject(String owner, UUID project) {
    if (!Boolean.TRUE.equals(
        jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM projects WHERE owner_id=? AND id=?)",
            Boolean.class,
            owner,
            project))) throw new ResourceNotFoundException();
  }

  public Optional<Task> parent(String owner, UUID project, UUID id) {
    try {
      var rows =
          jdbc.query(
              "SELECT parent.* FROM tasks child JOIN projects p ON p.id=child.project_id LEFT JOIN tasks parent ON parent.project_id=child.project_id AND parent.id=child.parent_id WHERE p.owner_id=? AND p.id=? AND child.id=?",
              (row, n) ->
                  row.getObject("id") == null
                      ? Optional.<Task>empty()
                      : Optional.of(MAPPER.mapRow(row, n)),
              owner,
              project,
              id);
      return rows.stream().findFirst().orElseThrow(ResourceNotFoundException::new);
    } catch (DataAccessException error) {
      throw new StorageUnavailableException(error);
    }
  }

  public List<Task> list(String owner, UUID project, UUID parent, TaskPosition after) {
    try {
      detail(owner, project, parent);
      if (after != null)
        return jdbc.query(
            "SELECT * FROM tasks WHERE project_id=? AND parent_id=? AND (created_at,id)<(?,?) ORDER BY created_at DESC,id DESC LIMIT 21",
            MAPPER,
            project,
            parent,
            Timestamp.from(after.createdAt()),
            after.id());
      return jdbc.query(
          "SELECT * FROM tasks WHERE project_id=? AND parent_id=? ORDER BY created_at DESC,id DESC LIMIT 21",
          MAPPER,
          project,
          parent);
    } catch (DataAccessException error) {
      throw new StorageUnavailableException(error);
    }
  }
}
