package com.apptolast.organization.adapter.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.application.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

class ApiCredentialPersistenceTest {
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"current", "expired", "revoked"})
  void s9_replayReturnsOriginalWithoutClockEntropyOrPhysicalWrite(String state) {
    var jdbc = Database.JDBC;
    var owner = "replay-" + UUID.randomUUID();
    var id = UUID.randomUUID();
    var store = new PostgresApiCredentialStore(jdbc, Database.TRANSACTIONS);
    var first =
        new CreateApiCredential(
                store,
                Clock.fixed(
                    Instant.parse(
                        state.equals("expired") ? "2026-01-01T12:00:00Z" : "2026-09-08T12:00:00Z"),
                    ZoneOffset.UTC),
                new SecureRandom())
            .create(owner, id, "Same", List.of("projects:read"), 7);
    if (state.equals("revoked"))
      jdbc.update("UPDATE api_credentials SET revoked_at='2026-09-09Z' WHERE id=?", id);
    var before =
        jdbc.queryForObject(
            "SELECT row_to_json(c)::text || xmin::text || ctid::text FROM api_credentials c WHERE id=?",
            String.class,
            id);
    var result =
        store.create(
            owner,
            id,
            new com.apptolast.organization.domain.ApiCredentialIntent(
                "Same", List.of("projects:read"), 7),
            () -> {
              throw new AssertionError("Replay requested new secret or clock");
            });
    assertNull(result.secret());
    assertEquals(first.credential().createdAt(), result.credential().createdAt());
    assertEquals(id, result.credential().id());
    assertEquals(
        state.equals("revoked") ? Instant.parse("2026-09-09T00:00:00Z") : null,
        result.credential().revokedAt());
    assertEquals(
        before,
        jdbc.queryForObject(
            "SELECT row_to_json(c)::text || xmin::text || ctid::text FROM api_credentials c WHERE id=?",
            String.class,
            id));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"owner", "name", "scopes", "days"})
  void s10_collisionCannotRevealOrReplaceExistingCredential(String difference) {
    var jdbc = Database.JDBC;
    var owner = "collision-" + UUID.randomUUID();
    var id = UUID.randomUUID();
    var store = new PostgresApiCredentialStore(jdbc, Database.TRANSACTIONS);
    var useCase = new CreateApiCredential(store, Clock.systemUTC(), new SecureRandom());
    useCase.create(owner, id, "Original", List.of("projects:read"), 30);
    var before =
        jdbc.queryForObject(
            "SELECT row_to_json(c)::text || xmin::text || ctid::text FROM api_credentials c WHERE id=?",
            String.class,
            id);
    assertThrows(
        ApiCredentialConflictException.class,
        () ->
            useCase.create(
                difference.equals("owner") ? "other" : owner,
                id,
                difference.equals("name") ? "Changed" : "Original",
                difference.equals("scopes") ? List.of("projects:write") : List.of("projects:read"),
                difference.equals("days") ? 7 : 30));
    assertEquals(
        before,
        jdbc.queryForObject(
            "SELECT row_to_json(c)::text || xmin::text || ctid::text FROM api_credentials c WHERE id=?",
            String.class,
            id));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(ints = {9, 10})
  void s7_onlyCurrentUnrevokedCredentialsUseCapacity(int active) {
    var jdbc = Database.JDBC;
    var owner = "capacity-" + UUID.randomUUID();
    var now = Instant.parse("2026-09-08T12:00:00Z");
    var store = new PostgresApiCredentialStore(jdbc, Database.TRANSACTIONS);
    var create =
        new CreateApiCredential(store, Clock.fixed(now, ZoneOffset.UTC), new SecureRandom());
    for (int i = 0; i < active + 2; i++)
      jdbc.update(
          "INSERT INTO api_credentials VALUES (?,?,'Old',ARRAY['projects:read'],7,?,?::timestamptz,?::timestamptz,?::timestamptz)",
          UUID.randomUUID(),
          owner,
          new byte[32],
          now.minusSeconds(86400).toString(),
          i == active ? now.toString() : now.plusSeconds(86400).toString(),
          i == active + 1 ? now.toString() : null);
    if (active == 9)
      assertNotNull(
          create.create(owner, UUID.randomUUID(), "New", List.of("tasks:read"), 7).secret());
    else
      assertThrows(
          ApiCredentialLimitException.class,
          () -> create.create(owner, UUID.randomUUID(), "New", List.of("tasks:read"), 7));
    assertEquals(
        10,
        jdbc.queryForObject(
            "SELECT count(*) FROM api_credentials WHERE owner_id=? AND expires_at>?::timestamptz AND revoked_at IS NULL",
            Integer.class,
            owner,
            now.toString()));
    assertEquals(
        12,
        jdbc.queryForObject(
            "SELECT count(*) FROM api_credentials WHERE owner_id=?", Integer.class, owner));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
  void s8_s11_concurrentCreationSerializesReplayAndLastSlot(boolean sameId) throws Exception {
    var owner = "race-" + UUID.randomUUID();
    var store = new PostgresApiCredentialStore(Database.JDBC, Database.TRANSACTIONS);
    var clock = Clock.fixed(Instant.parse("2026-09-08T12:00:00Z"), ZoneOffset.UTC);
    if (!sameId)
      for (int i = 0; i < 9; i++)
        new CreateApiCredential(store, clock, new SecureRandom())
            .create(owner, UUID.randomUUID(), "Old", List.of("projects:read"), 7);
    var firstIssued = new java.util.concurrent.CountDownLatch(1);
    var release = new java.util.concurrent.CountDownLatch(1);
    var random =
        new SecureRandom() {
          @Override
          public void nextBytes(byte[] bytes) {
            super.nextBytes(bytes);
            firstIssued.countDown();
            try {
              if (!release.await(10, java.util.concurrent.TimeUnit.SECONDS))
                throw new AssertionError("Race fixture not released");
            } catch (InterruptedException error) {
              throw new AssertionError(error);
            }
          }
        };
    var id = UUID.randomUUID();
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var first =
          pool.submit(
              () ->
                  new CreateApiCredential(store, clock, random)
                      .create(owner, id, "Same", List.of("projects:read"), 7));
      try {
        assertTrue(firstIssued.await(5, java.util.concurrent.TimeUnit.SECONDS));
        var second =
            pool.submit(
                () ->
                    new CreateApiCredential(store, clock, new SecureRandom())
                        .create(
                            owner,
                            sameId ? id : UUID.randomUUID(),
                            "Same",
                            List.of("projects:read"),
                            7));
        boolean observedBlocked = false;
        var deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        while (!second.isDone() && System.nanoTime() < deadline) {
          if (Database.JDBC.queryForObject(
                  "SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND cardinality(pg_blocking_pids(pid))>0",
                  Integer.class)
              > 0) {
            observedBlocked = true;
            break;
          }
          Thread.sleep(10);
        }
        try {
          assertTrue(observedBlocked, "Both database transactions must overlap");
        } finally {
          release.countDown();
        }
        assertNotNull(first.get(5, java.util.concurrent.TimeUnit.SECONDS).secret());
        if (sameId) assertNull(second.get(5, java.util.concurrent.TimeUnit.SECONDS).secret());
        else
          assertInstanceOf(
              ApiCredentialLimitException.class,
              assertThrows(
                      java.util.concurrent.ExecutionException.class,
                      () -> second.get(5, java.util.concurrent.TimeUnit.SECONDS))
                  .getCause());
        assertEquals(
            sameId ? 1 : 10,
            Database.JDBC.queryForObject(
                "SELECT count(*) FROM api_credentials WHERE owner_id=?", Integer.class, owner));
      } finally {
        release.countDown();
      }
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"reject", "skip", "commit"})
  void s17_failedDurableCreationNeverReturnsSecret(String failure) {
    var jdbc = Database.JDBC;
    var owner = "failure-" + UUID.randomUUID();
    var id = UUID.randomUUID();
    var name = "credential_fault_" + UUID.randomUUID().toString().replace("-", "");
    jdbc.execute(
        "CREATE FUNCTION "
            + name
            + "() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN "
            + (failure.equals("skip") ? "RETURN NULL;" : "RAISE EXCEPTION 'fixture rejection';")
            + " END $$");
    try {
      jdbc.execute(
          "CREATE "
              + (failure.equals("commit") ? "CONSTRAINT " : "")
              + "TRIGGER "
              + name
              + " "
              + (failure.equals("commit") ? "AFTER" : "BEFORE")
              + " INSERT ON api_credentials "
              + (failure.equals("commit") ? "DEFERRABLE INITIALLY DEFERRED " : "")
              + "FOR EACH ROW WHEN (NEW.owner_id='"
              + owner
              + "') EXECUTE FUNCTION "
              + name
              + "()");
      var create =
          new CreateApiCredential(
              new PostgresApiCredentialStore(jdbc, Database.TRANSACTIONS),
              Clock.systemUTC(),
              new SecureRandom());
      assertThrows(
          StorageUnavailableException.class,
          () -> create.create(owner, id, "Failure", List.of("tasks:read"), 30));
      assertEquals(
          0,
          jdbc.queryForObject(
              "SELECT count(*) FROM api_credentials WHERE owner_id=?", Integer.class, owner));
    } finally {
      jdbc.execute("DROP TRIGGER IF EXISTS " + name + " ON api_credentials");
      jdbc.execute("DROP FUNCTION " + name + "()");
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {
        "",
        "not-base64!",
        "eA",
        "2026-09-08T12:00:00Z|1-1-1-1-1",
        "2026-09-08T12:00:00.0000001Z|00000000-0000-0000-0000-000000000001"
      })
  void s14_badCursorHasPublicValidationError(String input) {
    var cursor =
        input.contains("|")
            ? Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(input.getBytes(java.nio.charset.StandardCharsets.UTF_8))
            : input;
    var read =
        new ReadApiCredentials(
            new PostgresApiCredentialStore(Database.JDBC, Database.TRANSACTIONS));
    var error =
        assertThrows(
            com.apptolast.organization.domain.ApiCredentialInvalidException.class,
            () -> read.list("owner", cursor));
    assertEquals("cursor", error.errors().getFirst().field());
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"reject", "skip", "commit"})
  void s17_failedRevocationPreservesCredential(String failure) {
    var jdbc = Database.JDBC;
    var owner = "revoke-failure-" + UUID.randomUUID();
    var id = UUID.randomUUID();
    var store = new PostgresApiCredentialStore(jdbc, Database.TRANSACTIONS);
    new CreateApiCredential(store, Clock.systemUTC(), new SecureRandom())
        .create(owner, id, "Keep", List.of("tasks:read"), 30);
    var before =
        jdbc.queryForObject(
            "SELECT row_to_json(c)::text || xmin::text || ctid::text FROM api_credentials c WHERE id=?",
            String.class,
            id);
    var name = "revocation_fault_" + UUID.randomUUID().toString().replace("-", "");
    jdbc.execute(
        "CREATE FUNCTION "
            + name
            + "() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN "
            + (failure.equals("skip") ? "RETURN NULL;" : "RAISE EXCEPTION 'fixture rejection';")
            + " END $$");
    try {
      jdbc.execute(
          "CREATE "
              + (failure.equals("commit") ? "CONSTRAINT " : "")
              + "TRIGGER "
              + name
              + " "
              + (failure.equals("commit") ? "AFTER" : "BEFORE")
              + " UPDATE ON api_credentials "
              + (failure.equals("commit") ? "DEFERRABLE INITIALLY DEFERRED " : "")
              + "FOR EACH ROW WHEN (NEW.owner_id='"
              + owner
              + "') EXECUTE FUNCTION "
              + name
              + "()");
      assertThrows(
          StorageUnavailableException.class,
          () -> new RevokeApiCredential(store, Clock.systemUTC()).revoke(owner, id));
      assertEquals(
          before,
          jdbc.queryForObject(
              "SELECT row_to_json(c)::text || xmin::text || ctid::text FROM api_credentials c WHERE id=?",
              String.class,
              id));
    } finally {
      jdbc.execute("DROP TRIGGER IF EXISTS " + name + " ON api_credentials");
      jdbc.execute("DROP FUNCTION " + name + "()");
    }
  }

  @Test
  void s13_storageFailureInListUsesStorageUnavailable() {
    var unavailable =
        new DriverManagerDataSource(
            Database.PG.getJdbcUrl(), Database.PG.getUsername(), "fixture-invalid-password");
    var store =
        new PostgresApiCredentialStore(
            new JdbcTemplate(unavailable), new DataSourceTransactionManager(unavailable));
    assertThrows(
        StorageUnavailableException.class, () -> new ReadApiCredentials(store).list("owner", null));
  }

  @Test
  void s15_s16_revocationIsOwnedDurableAndKeepsFirstClockEvenWhenItRegresses() {
    var owner = "revoke-" + UUID.randomUUID();
    var store = new PostgresApiCredentialStore(Database.JDBC, Database.TRANSACTIONS);
    var id = UUID.randomUUID();
    var created =
        new CreateApiCredential(
                store,
                Clock.fixed(Instant.parse("2026-09-08T12:00:00Z"), ZoneOffset.UTC),
                new SecureRandom())
            .create(owner, id, "Revoke", List.of("tasks:read"), 7);
    var now = Instant.parse("2026-09-08T11:00:00.123456789Z");
    RevokeApiCredentialUseCase revoke =
        new RevokeApiCredential(store, Clock.fixed(now, ZoneOffset.UTC));
    assertTrue(revoke.revoke("other", id).isEmpty());
    assertTrue(revoke.revoke(owner, UUID.randomUUID()).isEmpty());
    var first = revoke.revoke(owner, id).orElseThrow();
    assertEquals(Instant.parse("2026-09-08T11:00:00.123456Z"), first.revokedAt());
    assertEquals(created.credential().expiresAt(), first.expiresAt());
    var before =
        Database.JDBC.queryForObject(
            "SELECT row_to_json(c)::text || xmin::text || ctid::text FROM api_credentials c WHERE id=?",
            String.class,
            id);
    assertEquals(
        Optional.of(first),
        store.revoke(
            owner,
            id,
            () -> {
              throw new AssertionError("Replay reads clock");
            }));
    assertEquals(
        before,
        Database.JDBC.queryForObject(
            "SELECT row_to_json(c)::text || xmin::text || ctid::text FROM api_credentials c WHERE id=?",
            String.class,
            id));
  }

  @Test
  void s13_pagesFiftyOwnHistoricalCredentialsInStableDescendingOrder() {
    var jdbc = Database.JDBC;
    var owner = "pages-" + UUID.randomUUID();
    var prefix = UUID.randomUUID().getMostSignificantBits();
    var expected = new java.util.ArrayList<UUID>();
    for (int i = 1; i <= 52; i++) {
      var id = new UUID(prefix, i);
      jdbc.update(
          "INSERT INTO api_credentials VALUES (?,?,'Old',ARRAY['projects:read'],7,?,'2026-01-01Z','2026-01-08Z',NULL)",
          id,
          i == 52 ? "other-" + owner : owner,
          new byte[32]);
      if (i < 52) expected.addFirst(id);
    }
    var read = new ReadApiCredentials(new PostgresApiCredentialStore(jdbc, Database.TRANSACTIONS));
    var first = read.list(owner, null);
    assertEquals(
        expected.subList(0, 50),
        first.items().stream().map(com.apptolast.organization.domain.ApiCredential::id).toList());
    assertNotNull(first.nextCursor());
    var second = read.list(owner, first.nextCursor());
    assertEquals(
        expected.subList(50, 51),
        second.items().stream().map(com.apptolast.organization.domain.ApiCredential::id).toList());
    assertNull(second.nextCursor());
    assertTrue(read.list("unknown-owner", null).items().isEmpty());
    assertEquals(
        51,
        jdbc.queryForObject(
            "SELECT count(*) FROM api_credentials WHERE owner_id=?", Integer.class, owner));
  }

  @Test
  void s12_readsOnlyOwnMetadataWithoutWriting() {
    var jdbc = Database.JDBC;
    var owner = "read-" + UUID.randomUUID();
    var store = new PostgresApiCredentialStore(jdbc, Database.TRANSACTIONS);
    var created =
        new CreateApiCredential(store, Clock.systemUTC(), new SecureRandom())
            .create(owner, UUID.randomUUID(), "Read", List.of("agenda:read"), 30);
    var before =
        jdbc.queryForObject(
            "SELECT row_to_json(c)::text || xmin::text || ctid::text FROM api_credentials c WHERE id=?",
            String.class,
            created.credential().id());
    ReadApiCredentialsUseCase read = new ReadApiCredentials(store);
    assertEquals(Optional.of(created.credential()), read.find(owner, created.credential().id()));
    assertTrue(read.find("other-owner", created.credential().id()).isEmpty());
    assertTrue(read.find(owner, UUID.randomUUID()).isEmpty());
    assertEquals(
        before,
        jdbc.queryForObject(
            "SELECT row_to_json(c)::text || xmin::text || ctid::text FROM api_credentials c WHERE id=?",
            String.class,
            created.credential().id()));
  }

  @Test
  void s1_commitsOnlyVerifierAndLeavesBusinessUntouched() throws Exception {
    var jdbc = Database.JDBC;
    var owner = "credential-" + UUID.randomUUID();
    var id = UUID.randomUUID();
    var business = jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class);
    var store = new PostgresApiCredentialStore(jdbc, Database.TRANSACTIONS);
    var result =
        new CreateApiCredential(
                store,
                Clock.fixed(Instant.parse("2026-09-08T12:00:00.123456789Z"), ZoneOffset.UTC),
                new SecureRandom())
            .create(owner, id, "Integration", List.of("projects:read"), 30);
    var row = jdbc.queryForMap("SELECT * FROM api_credentials WHERE id=?", id);
    assertEquals(owner, row.get("owner_id"));
    assertEquals("Integration", row.get("name"));
    var secret =
        Base64.getUrlDecoder().decode(result.secret().substring(result.secret().indexOf('.') + 1));
    assertEquals(32, secret.length);
    assertArrayEquals(
        MessageDigest.getInstance("SHA-256").digest(secret), (byte[]) row.get("verifier"));
    assertEquals(
        result.credential().createdAt(), ((java.sql.Timestamp) row.get("created_at")).toInstant());
    assertEquals(
        result.credential().expiresAt(), ((java.sql.Timestamp) row.get("expires_at")).toInstant());
    assertNull(row.get("revoked_at"));
    assertEquals(
        Set.of(
            "id",
            "owner_id",
            "name",
            "scopes",
            "expires_in_days",
            "verifier",
            "created_at",
            "expires_at",
            "revoked_at"),
        row.keySet());
    assertEquals(business, jdbc.queryForObject("SELECT count(*) FROM outbox_events", Long.class));
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT count(*) FROM api_credentials WHERE owner_id=?", Integer.class, owner));
  }

  // One PostgreSQL process and migration per JVM, independent of test-instance lifecycle.
  static final class Database {
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:17-alpine");
    static final JdbcTemplate JDBC;
    static final DataSourceTransactionManager TRANSACTIONS;

    static {
      PG.start();
      var source = new DriverManagerDataSource(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
      Flyway.configure().dataSource(source).load().migrate();
      JDBC = new JdbcTemplate(source);
      TRANSACTIONS = new DataSourceTransactionManager(source);
    }
  }
}
