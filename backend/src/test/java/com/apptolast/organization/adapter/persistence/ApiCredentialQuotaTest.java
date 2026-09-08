package com.apptolast.organization.adapter.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.application.*;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ApiCredentialQuotaTest {
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({"60,100", "10,120"})
  void s26_rejectingEitherLimitSpendsNeitherCounter(int tokenUsed, int ownerUsed) {
    var jdbc = ApiCredentialPersistenceTest.Database.JDBC;
    var owner = "quota-reject-" + UUID.randomUUID();
    var store =
        new PostgresApiCredentialStore(jdbc, ApiCredentialPersistenceTest.Database.TRANSACTIONS);
    var clock = Clock.fixed(Instant.parse("2026-09-08T12:00:20.123456Z"), ZoneOffset.UTC);
    var created =
        new CreateApiCredential(store, clock, new SecureRandom())
            .create(owner, UUID.randomUUID(), "Quota", List.of("projects:read"), 7);
    var access =
        new AuthenticateApiCredential(store, clock, owner::equals).authenticate(created.secret());
    jdbc.update(
        "INSERT INTO api_owner_quotas VALUES (?,'2026-09-08T12:00:00Z',?)", owner, ownerUsed);
    jdbc.update(
        "INSERT INTO api_credential_quotas VALUES (?,'2026-09-08T12:00:00Z',?)",
        access.id(),
        tokenUsed);
    var error =
        assertThrows(
            ApiRateLimitedException.class,
            () -> new ConsumeApiQuota(store, clock, owner::equals).consume(access));
    assertEquals(40, error.retryAfterSeconds());
    assertEquals(
        ownerUsed,
        jdbc.queryForObject(
            "SELECT used FROM api_owner_quotas WHERE owner_id=?", Integer.class, owner));
    assertEquals(
        tokenUsed,
        jdbc.queryForObject(
            "SELECT used FROM api_credential_quotas WHERE credential_id=?",
            Integer.class,
            access.id()));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"revoked", "expired", "disabled", "renamed"})
  void s21_s30_s31_admissionRechecksCredentialAndOwnerWithoutSpending(String change) {
    var jdbc = ApiCredentialPersistenceTest.Database.JDBC;
    var owner = "admission-" + UUID.randomUUID();
    var store =
        new PostgresApiCredentialStore(jdbc, ApiCredentialPersistenceTest.Database.TRANSACTIONS);
    var now = Instant.parse("2026-09-08T12:00:00Z");
    var clock = Clock.fixed(now, ZoneOffset.UTC);
    var created =
        new CreateApiCredential(store, clock, new SecureRandom())
            .create(owner, UUID.randomUUID(), "Admission", List.of("tasks:read"), 7);
    var access =
        new AuthenticateApiCredential(store, clock, owner::equals).authenticate(created.secret());
    if (change.equals("revoked")) new RevokeApiCredential(store, clock).revoke(owner, access.id());
    var admissionClock =
        change.equals("expired")
            ? Clock.fixed(created.credential().expiresAt(), ZoneOffset.UTC)
            : clock;
    var consume =
        new ConsumeApiQuota(
            store,
            admissionClock,
            value ->
                !change.equals("disabled")
                    && value.equals(change.equals("renamed") ? "renamed" : owner));
    assertThrows(ApiUnauthenticatedException.class, () -> consume.consume(access));
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT count(*) FROM api_owner_quotas WHERE owner_id=?", Integer.class, owner));
    assertEquals(
        0,
        jdbc.queryForObject(
            "SELECT count(*) FROM api_credential_quotas WHERE credential_id=?",
            Integer.class,
            access.id()));
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "owner,reject",
    "owner,skip",
    "owner,commit",
    "credential,reject",
    "credential,skip",
    "credential,commit"
  })
  void s29_storageFailureOrZeroRowsRollsBackBothCounters(String counter, String failure) {
    var jdbc = ApiCredentialPersistenceTest.Database.JDBC;
    var owner = "quota-failure-" + UUID.randomUUID();
    var store =
        new PostgresApiCredentialStore(jdbc, ApiCredentialPersistenceTest.Database.TRANSACTIONS);
    var clock = Clock.fixed(Instant.parse("2026-09-08T12:00:00Z"), ZoneOffset.UTC);
    var created =
        new CreateApiCredential(store, clock, new SecureRandom())
            .create(owner, UUID.randomUUID(), "Failure", List.of("tasks:read"), 7);
    var access =
        new AuthenticateApiCredential(store, clock, owner::equals).authenticate(created.secret());
    jdbc.update("INSERT INTO api_owner_quotas VALUES (?,'2026-09-08T12:00:00Z',5)", owner);
    jdbc.update(
        "INSERT INTO api_credential_quotas VALUES (?,'2026-09-08T12:00:00Z',4)", access.id());
    var beforeOwner =
        jdbc.queryForObject(
            "SELECT row_to_json(q)::text||xmin::text||ctid::text FROM api_owner_quotas q WHERE owner_id=?",
            String.class,
            owner);
    var beforeCredential =
        jdbc.queryForObject(
            "SELECT row_to_json(q)::text||xmin::text||ctid::text FROM api_credential_quotas q WHERE credential_id=?",
            String.class,
            access.id());
    var table = counter.equals("owner") ? "api_owner_quotas" : "api_credential_quotas";
    var condition =
        counter.equals("owner")
            ? "NEW.owner_id='" + owner + "'"
            : "NEW.credential_id='" + access.id() + "'";
    var name = "quota_fault_" + UUID.randomUUID().toString().replace("-", "");
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
              + " INSERT OR UPDATE ON "
              + table
              + " "
              + (failure.equals("commit") ? "DEFERRABLE INITIALLY DEFERRED " : "")
              + "FOR EACH ROW WHEN ("
              + condition
              + ") EXECUTE FUNCTION "
              + name
              + "()");
      assertThrows(
          StorageUnavailableException.class,
          () -> new ConsumeApiQuota(store, clock, owner::equals).consume(access));
      assertEquals(
          beforeOwner,
          jdbc.queryForObject(
              "SELECT row_to_json(q)::text||xmin::text||ctid::text FROM api_owner_quotas q WHERE owner_id=?",
              String.class,
              owner));
      assertEquals(
          beforeCredential,
          jdbc.queryForObject(
              "SELECT row_to_json(q)::text||xmin::text||ctid::text FROM api_credential_quotas q WHERE credential_id=?",
              String.class,
              access.id()));
    } finally {
      jdbc.execute("DROP TRIGGER IF EXISTS " + name + " ON " + table);
      jdbc.execute("DROP FUNCTION " + name + "()");
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
  void s27_twoReplicasCannotSpendLastCredentialOrOwnerSlotTwice(boolean sameCredential)
      throws Exception {
    var jdbc = ApiCredentialPersistenceTest.Database.JDBC;
    var owner = "quota-race-" + UUID.randomUUID();
    var store =
        new PostgresApiCredentialStore(jdbc, ApiCredentialPersistenceTest.Database.TRANSACTIONS);
    var other =
        new PostgresApiCredentialStore(
            jdbc,
            new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                jdbc.getDataSource()));
    var now = Instant.parse("2026-09-08T12:00:00Z");
    var clock = Clock.fixed(now, ZoneOffset.UTC);
    var create = new CreateApiCredential(store, clock, new SecureRandom());
    var a = create.create(owner, UUID.randomUUID(), "First", List.of("tasks:read"), 7);
    var b =
        sameCredential
            ? a
            : create.create(owner, UUID.randomUUID(), "Second", List.of("tasks:read"), 7);
    var auth = new AuthenticateApiCredential(store, clock, owner::equals);
    var firstAccess = auth.authenticate(a.secret());
    var secondAccess = auth.authenticate(b.secret());
    jdbc.update(
        "INSERT INTO api_owner_quotas VALUES (?,'2026-09-08T12:00:00Z',?)",
        owner,
        sameCredential ? 0 : 119);
    jdbc.update(
        "INSERT INTO api_credential_quotas VALUES (?,'2026-09-08T12:00:00Z',?)",
        firstAccess.id(),
        sameCredential ? 59 : 0);
    var locked = new java.util.concurrent.CountDownLatch(1);
    var release = new java.util.concurrent.CountDownLatch(1);
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var first =
          pool.submit(
              () ->
                  store.consume(
                      firstAccess,
                      () -> {
                        locked.countDown();
                        try {
                          if (!release.await(10, java.util.concurrent.TimeUnit.SECONDS))
                            throw new AssertionError("Race fixture not released");
                        } catch (InterruptedException error) {
                          throw new AssertionError(error);
                        }
                        return now;
                      },
                      owner::equals));
      try {
        assertTrue(locked.await(5, java.util.concurrent.TimeUnit.SECONDS));
        var second =
            pool.submit(
                () -> new ConsumeApiQuota(other, clock, owner::equals).consume(secondAccess));
        boolean blocked = false;
        var deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        while (!second.isDone() && System.nanoTime() < deadline) {
          if (jdbc.queryForObject(
                  "SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND cardinality(pg_blocking_pids(pid))>0",
                  Integer.class)
              > 0) {
            blocked = true;
            break;
          }
          Thread.sleep(10);
        }
        try {
          assertTrue(blocked, "Second replica must overlap and wait");
        } finally {
          release.countDown();
        }
        first.get(5, java.util.concurrent.TimeUnit.SECONDS);
        assertInstanceOf(
            ApiRateLimitedException.class,
            assertThrows(
                    java.util.concurrent.ExecutionException.class,
                    () -> second.get(5, java.util.concurrent.TimeUnit.SECONDS))
                .getCause());
        assertEquals(
            sameCredential ? 1 : 120,
            jdbc.queryForObject(
                "SELECT used FROM api_owner_quotas WHERE owner_id=?", Integer.class, owner));
        assertEquals(
            sameCredential ? 60 : 1,
            jdbc.queryForObject(
                "SELECT used FROM api_credential_quotas WHERE credential_id=?",
                Integer.class,
                firstAccess.id()));
        if (!sameCredential)
          assertTrue(
              jdbc.queryForList(
                      "SELECT used FROM api_credential_quotas WHERE credential_id=?",
                      Integer.class,
                      secondAccess.id())
                  .isEmpty());
      } finally {
        release.countDown();
      }
    }
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
  void s30_revocationAndAdmissionShareOneObservableCommitBoundary(boolean revocationFirst)
      throws Exception {
    var jdbc = ApiCredentialPersistenceTest.Database.JDBC;
    var owner = "revoke-race-" + UUID.randomUUID();
    var store =
        new PostgresApiCredentialStore(jdbc, ApiCredentialPersistenceTest.Database.TRANSACTIONS);
    var other =
        new PostgresApiCredentialStore(
            jdbc,
            new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                jdbc.getDataSource()));
    var now = Instant.parse("2026-09-08T12:00:00Z");
    var clock = Clock.fixed(now, ZoneOffset.UTC);
    var created =
        new CreateApiCredential(store, clock, new SecureRandom())
            .create(owner, UUID.randomUUID(), "Boundary", List.of("tasks:read"), 7);
    var auth = new AuthenticateApiCredential(store, clock, owner::equals);
    var access = auth.authenticate(created.secret());
    var locked = new java.util.concurrent.CountDownLatch(1);
    var release = new java.util.concurrent.CountDownLatch(1);
    java.util.function.Supplier<Instant> delayed =
        () -> {
          locked.countDown();
          try {
            if (!release.await(10, java.util.concurrent.TimeUnit.SECONDS))
              throw new AssertionError("Race fixture not released");
          } catch (InterruptedException error) {
            throw new AssertionError(error);
          }
          return now;
        };
    try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
      var first =
          pool.submit(
              () -> {
                if (revocationFirst) return store.revoke(owner, access.id(), delayed);
                store.consume(access, delayed, owner::equals);
                return null;
              });
      try {
        assertTrue(locked.await(5, java.util.concurrent.TimeUnit.SECONDS));
        var second =
            pool.submit(
                () -> {
                  if (!revocationFirst)
                    return new RevokeApiCredential(other, clock).revoke(owner, access.id());
                  new ConsumeApiQuota(other, clock, owner::equals).consume(access);
                  return null;
                });
        boolean blocked = false;
        var deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        while (!second.isDone() && System.nanoTime() < deadline) {
          if (jdbc.queryForObject(
                  "SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND cardinality(pg_blocking_pids(pid))>0",
                  Integer.class)
              > 0) {
            blocked = true;
            break;
          }
          Thread.sleep(10);
        }
        try {
          assertTrue(blocked, "Revocation and admission must overlap");
        } finally {
          release.countDown();
        }
        first.get(5, java.util.concurrent.TimeUnit.SECONDS);
        if (revocationFirst)
          assertInstanceOf(
              ApiUnauthenticatedException.class,
              assertThrows(
                      java.util.concurrent.ExecutionException.class,
                      () -> second.get(5, java.util.concurrent.TimeUnit.SECONDS))
                  .getCause());
        else
          assertNotNull(
              second.get(5, java.util.concurrent.TimeUnit.SECONDS).orElseThrow().revokedAt());
        assertThrows(ApiUnauthenticatedException.class, () -> auth.authenticate(created.secret()));
        assertEquals(
            revocationFirst ? List.of() : List.of(1),
            jdbc.queryForList(
                "SELECT used FROM api_owner_quotas WHERE owner_id=?", Integer.class, owner));
        assertEquals(
            revocationFirst ? List.of() : List.of(1),
            jdbc.queryForList(
                "SELECT used FROM api_credential_quotas WHERE credential_id=?",
                Integer.class,
                access.id()));
      } finally {
        release.countDown();
      }
    }
  }

  @Test
  void s28_nextUtcMinuteResetsBothCompactCountersAndRetryAfterIsOne() {
    var jdbc = ApiCredentialPersistenceTest.Database.JDBC;
    var owner = "window-" + UUID.randomUUID();
    var store =
        new PostgresApiCredentialStore(jdbc, ApiCredentialPersistenceTest.Database.TRANSACTIONS);
    var before = Clock.fixed(Instant.parse("2026-09-08T12:00:59.999999Z"), ZoneOffset.UTC);
    var created =
        new CreateApiCredential(store, before, new SecureRandom())
            .create(owner, UUID.randomUUID(), "Window", List.of("tasks:read"), 7);
    var access =
        new AuthenticateApiCredential(store, before, owner::equals).authenticate(created.secret());
    jdbc.update("INSERT INTO api_owner_quotas VALUES (?,'2026-09-08T12:00:00Z',120)", owner);
    jdbc.update(
        "INSERT INTO api_credential_quotas VALUES (?,'2026-09-08T12:00:00Z',60)", access.id());
    assertEquals(
        1,
        assertThrows(
                ApiRateLimitedException.class,
                () -> new ConsumeApiQuota(store, before, owner::equals).consume(access))
            .retryAfterSeconds());
    for (var instant : List.of("2026-09-08T12:01:00Z", "2026-09-08T12:02:00Z")) {
      new ConsumeApiQuota(store, Clock.fixed(Instant.parse(instant), ZoneOffset.UTC), owner::equals)
          .consume(access);
      assertEquals(
          List.of(1),
          jdbc.queryForList(
              "SELECT used FROM api_owner_quotas WHERE owner_id=?", Integer.class, owner));
      assertEquals(
          List.of(1),
          jdbc.queryForList(
              "SELECT used FROM api_credential_quotas WHERE credential_id=?",
              Integer.class,
              access.id()));
      assertEquals(
          Instant.parse(instant),
          jdbc.queryForObject(
                  "SELECT window_start FROM api_owner_quotas WHERE owner_id=?",
                  java.sql.Timestamp.class,
                  owner)
              .toInstant());
      assertEquals(
          Instant.parse(instant),
          jdbc.queryForObject(
                  "SELECT window_start FROM api_credential_quotas WHERE credential_id=?",
                  java.sql.Timestamp.class,
                  access.id())
              .toInstant());
    }
  }

  @Test
  void s26_lastAllowedRequestConsumesBothInclusiveLimits() {
    var jdbc = ApiCredentialPersistenceTest.Database.JDBC;
    var owner = "quota-" + UUID.randomUUID();
    var store =
        new PostgresApiCredentialStore(jdbc, ApiCredentialPersistenceTest.Database.TRANSACTIONS);
    var clock = Clock.fixed(Instant.parse("2026-09-08T12:00:00Z"), ZoneOffset.UTC);
    var created =
        new CreateApiCredential(store, clock, new SecureRandom())
            .create(owner, UUID.randomUUID(), "Quota", List.of("projects:read"), 7);
    var access =
        new AuthenticateApiCredential(store, clock, owner::equals).authenticate(created.secret());
    jdbc.update("INSERT INTO api_owner_quotas VALUES (?,'2026-09-08T12:00:00Z',119)", owner);
    jdbc.update(
        "INSERT INTO api_credential_quotas VALUES (?,'2026-09-08T12:00:00Z',59)", access.id());
    ConsumeApiQuotaUseCase consume = new ConsumeApiQuota(store, clock, owner::equals);
    consume.consume(access);
    assertEquals(
        120,
        jdbc.queryForObject(
            "SELECT used FROM api_owner_quotas WHERE owner_id=?", Integer.class, owner));
    assertEquals(
        60,
        jdbc.queryForObject(
            "SELECT used FROM api_credential_quotas WHERE credential_id=?",
            Integer.class,
            access.id()));
  }
}
