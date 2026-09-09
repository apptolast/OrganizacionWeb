package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.GitlabConnection;
import com.apptolast.organization.application.GitlabConnectionStore;
import com.apptolast.organization.application.StorageUnavailableException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * La conexión de GitLab del propietario. Guardar otra vez sustituye la única fila que existe, así
 * que sustituir el token no toca ni las tareas ni sus enlaces: viven en otras tablas porque son
 * trabajo propio y no credenciales.
 */
@Component
public final class PostgresGitlabConnectionStore implements GitlabConnectionStore {
  private static final String COLUMNS =
      "project_path, project_id, token_hint, status, token_ciphertext, last_activity_at,"
          + " last_error_code, last_error_at, version";

  private final JdbcTemplate jdbc;
  private final TransactionTemplate transaction;

  public PostgresGitlabConnectionStore(JdbcTemplate jdbc, TransactionTemplate transaction) {
    this.jdbc = jdbc;
    this.transaction = transaction;
  }

  @Override
  public Optional<GitlabConnection> find(String ownerId) {
    return guarded(
        () ->
            jdbc
                .query(
                    "SELECT " + COLUMNS + " FROM gitlab_connections WHERE owner_id=?",
                    (row, index) ->
                        new GitlabConnection(
                            row.getString("project_path"),
                            row.getLong("project_id"),
                            row.getString("token_hint"),
                            row.getString("status"),
                            row.getBytes("token_ciphertext"),
                            row.getTimestamp("last_activity_at").toInstant(),
                            row.getString("last_error_code"),
                            instant(row, "last_error_at"),
                            row.getLong("version")),
                    ownerId)
                .stream()
                .findFirst());
  }

  @Override
  public void save(String ownerId, GitlabConnection connection) {
    guarded(
        () ->
            jdbc.update(
                "INSERT INTO gitlab_connections(owner_id,project_path,project_id,token_hint,status,"
                    + "token_ciphertext,last_activity_at,last_error_code,last_error_at,version)"
                    + " VALUES (?,?,?,?,?,?,?,?,?,?)"
                    + " ON CONFLICT (owner_id) DO UPDATE SET project_path=EXCLUDED.project_path,"
                    + " project_id=EXCLUDED.project_id, token_hint=EXCLUDED.token_hint,"
                    + " status=EXCLUDED.status, token_ciphertext=EXCLUDED.token_ciphertext,"
                    + " last_activity_at=EXCLUDED.last_activity_at,"
                    + " last_error_code=EXCLUDED.last_error_code,"
                    + " last_error_at=EXCLUDED.last_error_at, version=EXCLUDED.version",
                ownerId,
                connection.projectPath(),
                connection.projectId(),
                connection.tokenHint(),
                connection.status(),
                connection.tokenCiphertext(),
                Timestamp.from(connection.lastActivityAt()),
                connection.lastErrorCode(),
                timestamp(connection.lastErrorAt()),
                connection.version()));
  }

  @Override
  public boolean delete(String ownerId) {
    return guarded(
        () -> jdbc.update("DELETE FROM gitlab_connections WHERE owner_id=?", ownerId) == 1);
  }

  private static Timestamp timestamp(Instant value) {
    return value == null ? null : Timestamp.from(value);
  }

  private static Instant instant(ResultSet row, String column) throws SQLException {
    var value = row.getTimestamp(column);
    return value == null ? null : value.toInstant();
  }

  private <T> T guarded(java.util.function.Supplier<T> work) {
    try {
      return transaction.execute(status -> work.get());
    } catch (DataAccessException | TransactionException error) {
      throw new StorageUnavailableException(error);
    }
  }
}
