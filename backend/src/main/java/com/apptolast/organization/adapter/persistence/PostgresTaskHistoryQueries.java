package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.*;
import java.util.*;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public final class PostgresTaskHistoryQueries implements TaskHistoryQueries {
  private final JdbcTemplate jdbc;

  public PostgresTaskHistoryQueries(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<TaskHistoryEntry> list(
      String owner, UUID project, UUID task, TaskHistoryPosition after) {
    try {
      if (!Boolean.TRUE.equals(
          jdbc.queryForObject(
              "SELECT EXISTS(SELECT 1 FROM tasks t JOIN projects p ON p.id=t.project_id WHERE p.owner_id=? AND p.id=? AND t.id=?)",
              Boolean.class,
              owner,
              project,
              task))) throw new ResourceNotFoundException();
      return jdbc.query(
          "SELECT h.* FROM task_status_history h JOIN projects p ON p.id=h.project_id WHERE p.owner_id=? AND h.project_id=? AND h.task_id=? AND (?::bigint IS NULL OR h.task_version<?) ORDER BY h.task_version DESC LIMIT 21",
          (row, n) ->
              new TaskHistoryEntry(
                  row.getObject("id", UUID.class),
                  row.getLong("task_version"),
                  row.getString("from_status"),
                  row.getString("to_status"),
                  row.getTimestamp("occurred_at").toInstant()),
          owner,
          project,
          task,
          after == null ? null : after.taskVersion(),
          after == null ? null : after.taskVersion());
    } catch (DataAccessException error) {
      throw new StorageUnavailableException(error);
    }
  }
}
