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
