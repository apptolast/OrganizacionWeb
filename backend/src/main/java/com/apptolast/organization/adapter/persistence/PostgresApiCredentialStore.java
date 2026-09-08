package com.apptolast.organization.adapter.persistence;

import com.apptolast.organization.application.*;
import com.apptolast.organization.domain.ApiCredentialIntent;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public final class PostgresApiCredentialStore
    implements ApiCredentialCommit,
        ApiCredentialQueries,
        ApiCredentialRevocations,
        ApiCredentialAuthentication,
        ApiQuotaAdmission {
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

  @Override
  public java.util.Optional<com.apptolast.organization.domain.ApiCredential> find(
      String owner, UUID id) {
    try {
      return jdbc
          .query(
              "SELECT * FROM api_credentials WHERE owner_id=? AND id=?",
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
              owner,
              id)
          .stream()
          .findFirst();
    } catch (org.springframework.dao.DataAccessException error) {
      throw new StorageUnavailableException(error);
    }
  }

  @Override
  public com.apptolast.organization.domain.ApiCredentialPage list(String owner, String cursor) {
    try {
      String sql =
          "SELECT id,name,scopes,created_at,expires_at,revoked_at FROM api_credentials WHERE owner_id=?";
      var args = new java.util.ArrayList<Object>();
      args.add(owner);
      if (cursor != null) {
        try {
          if (cursor.length() > 256) throw new IllegalArgumentException();
          var decoded = java.util.Base64.getUrlDecoder().decode(cursor);
          if (!java.util.Base64.getUrlEncoder()
              .withoutPadding()
              .encodeToString(decoded)
              .equals(cursor)) throw new IllegalArgumentException();
          var parts = new String(decoded, java.nio.charset.StandardCharsets.UTF_8).split("[|]", -1);
          if (parts.length != 2) throw new IllegalArgumentException();
          var time = java.time.Instant.parse(parts[0]);
          var id = UUID.fromString(parts[1]);
          int year = time.atOffset(java.time.ZoneOffset.UTC).getYear();
          if (!id.toString().equals(parts[1])
              || time.getNano() % 1000 != 0
              || year < 1
              || year > 9999) throw new IllegalArgumentException();
          args.add(time.atOffset(java.time.ZoneOffset.UTC));
          args.add(id);
        } catch (RuntimeException error) {
          throw new com.apptolast.organization.domain.ApiCredentialInvalidException("cursor");
        }
        sql += " AND (created_at,id)<(?,?)";
      }
      sql += " ORDER BY created_at DESC,id DESC LIMIT 51";
      var rows =
          jdbc.query(
              sql,
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
              args.toArray());
      String next = null;
      if (rows.size() > 50) {
        var last = rows.get(49);
        next =
            java.util.Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                    (last.createdAt() + "|" + last.id())
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        rows = rows.subList(0, 50);
      }
      return new com.apptolast.organization.domain.ApiCredentialPage(rows, next);
    } catch (org.springframework.dao.DataAccessException error) {
      throw new StorageUnavailableException(error);
    }
  }

  @Override
  public java.util.Optional<com.apptolast.organization.domain.ApiCredential> revoke(
      String owner, UUID id, Supplier<java.time.Instant> now) {
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
            var prior = find(owner, id);
            if (prior.isEmpty() || prior.get().revokedAt() != null) return prior;
            var value = prior.get();
            var revoked = now.get();
            if (jdbc.update(
                    "UPDATE api_credentials SET revoked_at=?::timestamptz WHERE owner_id=? AND id=?",
                    revoked.toString(),
                    owner,
                    id)
                != 1)
              throw new StorageUnavailableException(
                  new IllegalStateException("Credential revocation was not confirmed"));
            return java.util.Optional.of(
                new com.apptolast.organization.domain.ApiCredential(
                    value.id(),
                    value.name(),
                    value.scopes(),
                    value.createdAt(),
                    value.expiresAt(),
                    revoked));
          });
    } catch (org.springframework.dao.DataAccessException
        | org.springframework.transaction.TransactionException error) {
      throw new StorageUnavailableException(error);
    }
  }

  @Override
  public java.util.Optional<ApiCredentialAccess> authenticate(
      UUID id, byte[] verifier, java.time.Instant now) {
    try {
      return jdbc
          .query(
              "SELECT owner_id,scopes,verifier FROM api_credentials WHERE id=? AND revoked_at IS NULL AND expires_at>?::timestamptz",
              (rs, index) -> {
                if (!java.security.MessageDigest.isEqual(verifier, rs.getBytes("verifier")))
                  return null;
                return new ApiCredentialAccess(
                    id,
                    rs.getString("owner_id"),
                    java.util.List.of((String[]) rs.getArray("scopes").getArray()));
              },
              id,
              now.toString())
          .stream()
          .filter(java.util.Objects::nonNull)
          .findFirst();
    } catch (org.springframework.dao.DataAccessException error) {
      throw new StorageUnavailableException(error);
    }
  }

  @Override
  public void consume(
      ApiCredentialAccess access,
      Supplier<java.time.Instant> now,
      java.util.function.Predicate<String> enabledOwner) {
    try {
      writing.executeWithoutResult(
          status -> {
            jdbc.queryForObject(
                "SELECT pg_advisory_xact_lock(hashtextextended(?,0))",
                Object.class,
                "api-credential-owner:" + access.owner());
            jdbc.queryForObject(
                "SELECT pg_advisory_xact_lock(hashtextextended(?,0))",
                Object.class,
                "api-credential-id:" + access.id());
            var instant = now.get();
            var credential =
                find(access.owner(), access.id()).orElseThrow(ApiUnauthenticatedException::new);
            if (!enabledOwner.test(access.owner())
                || credential.revokedAt() != null
                || !instant.isBefore(credential.expiresAt())
                || !credential.scopes().equals(access.scopes()))
              throw new ApiUnauthenticatedException();
            var window = instant.truncatedTo(java.time.temporal.ChronoUnit.MINUTES).toString();
            var ownerUsed =
                jdbc
                    .queryForList(
                        "SELECT used FROM api_owner_quotas WHERE owner_id=? AND window_start=?::timestamptz",
                        Integer.class,
                        access.owner(),
                        window)
                    .stream()
                    .findFirst()
                    .orElse(0);
            var credentialUsed =
                jdbc
                    .queryForList(
                        "SELECT used FROM api_credential_quotas WHERE credential_id=? AND window_start=?::timestamptz",
                        Integer.class,
                        access.id(),
                        window)
                    .stream()
                    .findFirst()
                    .orElse(0);
            if (ownerUsed >= 120 || credentialUsed >= 60)
              throw new ApiRateLimitedException(
                  60 - (int) Math.floorMod(instant.getEpochSecond(), 60));
            jdbc.update(
                "INSERT INTO api_owner_quotas VALUES (?,?::timestamptz,?) ON CONFLICT(owner_id) DO UPDATE SET window_start=EXCLUDED.window_start,used=EXCLUDED.used",
                access.owner(),
                window,
                ownerUsed + 1);
            jdbc.update(
                "INSERT INTO api_credential_quotas VALUES (?,?::timestamptz,?) ON CONFLICT(credential_id) DO UPDATE SET window_start=EXCLUDED.window_start,used=EXCLUDED.used",
                access.id(),
                window,
                credentialUsed + 1);
          });
    } catch (org.springframework.dao.DataAccessException
        | org.springframework.transaction.TransactionException error) {
      throw new StorageUnavailableException(error);
    }
  }
}
