package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.ConnectorConnectionStore;
import com.apptolast.organization.application.StoredConnection;
import com.apptolast.organization.application.StorageUnavailableException;
import java.sql.Timestamp;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

/** La conexión de GitHub del propietario. Guardar otra vez sustituye la única fila que existe. */
@Component
public final class PostgresConnectorConnectionStore implements ConnectorConnectionStore {
  private static final String PROVIDER = "github";

  private final JdbcTemplate jdbc;
  private final TransactionTemplate transaction;

  public PostgresConnectorConnectionStore(JdbcTemplate jdbc, TransactionTemplate transaction) {
    this.jdbc = jdbc;
    this.transaction = transaction;
  }

  @Override
  public Optional<StoredConnection> find(String ownerId) {
    return guarded(
        () ->
            jdbc
                .query(
                    "SELECT repository, login, status, token_ciphertext, connected_at"
                        + " FROM connector_connections WHERE owner_id=? AND provider=?",
                    (row, index) ->
                        new StoredConnection(
                            row.getString("repository"),
                            row.getString("login"),
                            row.getString("status"),
                            row.getBytes("token_ciphertext"),
                            row.getTimestamp("connected_at").toInstant()),
                    ownerId,
                    PROVIDER)
                .stream()
                .findFirst());
  }

  @Override
  public void save(String ownerId, StoredConnection connection) {
    guarded(
        () ->
            jdbc.update(
                "INSERT INTO connector_connections(owner_id,provider,repository,login,status,token_ciphertext,connected_at)"
                    + " VALUES (?,?,?,?,?,?,?)"
                    + " ON CONFLICT (owner_id, provider) DO UPDATE SET repository=EXCLUDED.repository,"
                    + " login=EXCLUDED.login, status=EXCLUDED.status,"
                    + " token_ciphertext=EXCLUDED.token_ciphertext, connected_at=EXCLUDED.connected_at",
                ownerId,
                PROVIDER,
                connection.repository(),
                connection.login(),
                connection.status(),
                connection.tokenCiphertext(),
                Timestamp.from(connection.connectedAt())));
  }

  @Override
  public boolean delete(String ownerId) {
    return guarded(
        () ->
            jdbc.update(
                    "DELETE FROM connector_connections WHERE owner_id=? AND provider=?",
                    ownerId,
                    PROVIDER)
                == 1);
  }

  @Override
  public void invalidate(String ownerId) {
    guarded(
        () ->
            jdbc.update(
                "UPDATE connector_connections SET status=? WHERE owner_id=? AND provider=?",
                StoredConnection.INVALID,
                ownerId,
                PROVIDER));
  }

  private <T> T guarded(java.util.function.Supplier<T> work) {
    try {
      return transaction.execute(status -> work.get());
    } catch (DataAccessException | TransactionException error) {
      throw new StorageUnavailableException(error);
    }
  }
}
