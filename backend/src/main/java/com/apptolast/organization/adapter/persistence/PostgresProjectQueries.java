package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.ProjectQueries;
import com.apptolast.organization.domain.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public final class PostgresProjectQueries implements ProjectQueries {
  private final org.springframework.jdbc.core.JdbcTemplate jdbc;

  public PostgresProjectQueries(org.springframework.jdbc.core.JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<ProjectSummary> list(String ownerId, ProjectPosition after, int limit) {
    return access(() -> listRows(ownerId, after, limit));
  }

  private List<ProjectSummary> listRows(String ownerId, ProjectPosition after, int limit) {
    var arguments = new ArrayList<Object>();
    arguments.add(ownerId);
    String position = "";
    if (after != null) {
      position = " AND (created_at,id)<(?,?)";
      arguments.add(java.sql.Timestamp.from(after.createdAt()));
      arguments.add(after.id());
    }
    arguments.add(limit);
    return jdbc.query(
        "SELECT id,name,status,created_at,updated_at FROM projects WHERE owner_id=?"
            + position
            + " ORDER BY created_at DESC,id DESC LIMIT ?",
        (rs, index) ->
            new ProjectSummary(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()),
        arguments.toArray());
  }

  public Optional<ProjectSnapshot> find(String ownerId, UUID id) {
    return access(() -> findRows(ownerId, id, ""));
  }

  Optional<ProjectSnapshot> findForUpdate(String ownerId, UUID id) {
    return findRows(ownerId, id, " FOR UPDATE");
  }

  private Optional<ProjectSnapshot> findRows(String ownerId, UUID id, String suffix) {
    return jdbc
        .query(
            "SELECT * FROM projects WHERE owner_id=? AND id=?" + suffix,
            (rs, index) ->
                new ProjectSnapshot(
                    new Project(
                        rs.getObject("id", UUID.class),
                        rs.getString("owner_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("status"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("updated_at").toInstant()),
                    rs.getLong("version")),
            ownerId,
            id)
        .stream()
        .findFirst();
  }

  private <T> T access(java.util.function.Supplier<T> operation) {
    try {
      return operation.get();
    } catch (org.springframework.dao.DataAccessException error) {
      throw new com.apptolast.organization.application.StorageUnavailableException(error);
    }
  }
}
