package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.ApiCredentialIntent;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public final class PostgresApiCredentialStore implements ApiCredentialCommit {
  private final JdbcTemplate jdbc;
  private final TransactionTemplate writing;

  public PostgresApiCredentialStore(JdbcTemplate jdbc, PlatformTransactionManager transactions) {
    this.jdbc = jdbc;
    this.writing = new TransactionTemplate(transactions);
  }

  @Override
  public ApiCredentialCreation create(
      String owner, UUID id, ApiCredentialIntent intent, Supplier<ApiCredentialIssuance> issue) {
    try {
      return writing.execute(
          status -> {
            jdbc.queryForObject(
                "SELECT pg_advisory_xact_lock(hashtextextended(?,0))",
                Object.class,
                "api-credential-owner:" + owner);
            jdbc.queryForObject(
                "SELECT pg_advisory_xact_lock(hashtextextended(?,0))",
                Object.class,
                "api-credential-id:" + id);
            var prior =
                jdbc.query(
                    "SELECT * FROM api_credentials WHERE id=?",
                    (rs, index) ->
                        new com.apptolast.organization.domain.ApiCredential(
                            rs.getObject("id", UUID.class),
                            rs.getString("name"),
                            java.util.List.of((String[]) rs.getArray("scopes").getArray()),
                            rs.getTimestamp("created_at").toInstant(),
                            rs.getTimestamp("expires_at").toInstant(),
                            rs.getTimestamp("revoked_at") == null
                                ? null
                                : rs.getTimestamp("revoked_at").toInstant()),
                    id);
            if (!prior.isEmpty()) {
              var value = prior.getFirst();
              var matches =
                  jdbc.queryForObject(
                      "SELECT owner_id=? AND expires_in_days=? FROM api_credentials WHERE id=?",
                      Boolean.class,
                      owner,
                      intent.expiresInDays(),
                      id);
              if (!Boolean.TRUE.equals(matches)
                  || !value.name().equals(intent.name())
                  || !value.scopes().equals(intent.scopes()))
                throw new ApiCredentialConflictException();
              return new ApiCredentialCreation(value, null);
            }
            var issued = issue.get();
            var credential = issued.credential();
            if (jdbc.queryForObject(
                    "SELECT count(*) FROM api_credentials WHERE owner_id=? AND expires_at>?::timestamptz AND revoked_at IS NULL",
                    Integer.class,
                    owner,
                    credential.createdAt().toString())
                >= 10) throw new ApiCredentialLimitException();
            int affected =
                jdbc.update(
                    connection -> {
                      var statement =
                          connection.prepareStatement(
                              "INSERT INTO api_credentials (id,owner_id,name,scopes,expires_in_days,verifier,created_at,expires_at) VALUES (?,?,?,?,?,?,?,?)");
                      statement.setObject(1, id);
                      statement.setString(2, owner);
                      statement.setString(3, credential.name());
                      statement.setArray(
                          4, connection.createArrayOf("text", credential.scopes().toArray()));
                      statement.setInt(5, intent.expiresInDays());
                      statement.setBytes(6, issued.verifier());
                      statement.setObject(
                          7, credential.createdAt().atOffset(java.time.ZoneOffset.UTC));
                      statement.setObject(
                          8, credential.expiresAt().atOffset(java.time.ZoneOffset.UTC));
                      return statement;
                    });
            if (affected != 1)
              throw new StorageUnavailableException(
                  new IllegalStateException("Credential insert was not confirmed"));
            return new ApiCredentialCreation(credential, issued.secret());
          });
    } catch (org.springframework.dao.DataAccessException
        | org.springframework.transaction.TransactionException error) {
      throw new StorageUnavailableException(error);
    }
  }
}
