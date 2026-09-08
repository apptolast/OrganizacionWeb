package com.apptolast.organization.adapter.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.application.*;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ApiCredentialAuthenticationTest {
  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(
      strings = {"unknown", "secret", "revoked", "disabled", "renamed", "before", "at", "after"})
  void s19_s21_s31_verifierOwnerRevocationAndExpiryAreIndistinguishable(String state) {
    var jdbc = ApiCredentialPersistenceTest.Database.JDBC;
    var owner = "auth-boundary-" + UUID.randomUUID();
    var store =
        new PostgresApiCredentialStore(jdbc, ApiCredentialPersistenceTest.Database.TRANSACTIONS);
    var createdAt = Instant.parse("2026-09-08T12:00:00Z");
    var created =
        new CreateApiCredential(store, Clock.fixed(createdAt, ZoneOffset.UTC), new SecureRandom())
            .create(owner, UUID.randomUUID(), "Boundary", List.of("projects:read"), 7);
    if (state.equals("revoked"))
      new RevokeApiCredential(store, Clock.fixed(createdAt, ZoneOffset.UTC))
          .revoke(owner, created.credential().id());
    var time =
        switch (state) {
          case "before" -> created.credential().expiresAt().minusNanos(1);
          case "at" -> created.credential().expiresAt();
          case "after" -> created.credential().expiresAt().plusNanos(1);
          default -> createdAt;
        };
    var token = created.secret();
    if (state.equals("unknown")) token = "owp_" + UUID.randomUUID() + token.substring(40);
    if (state.equals("secret"))
      token = token.substring(0, 41) + (token.charAt(41) == 'A' ? "B" : "A") + token.substring(42);
    var supplied = token;
    var auth =
        new AuthenticateApiCredential(
            store,
            Clock.fixed(time, ZoneOffset.UTC),
            value ->
                !state.equals("disabled")
                    && value.equals(state.equals("renamed") ? "renamed-owner" : owner));
    if (state.equals("before")) assertEquals(owner, auth.authenticate(supplied).owner());
    else {
      var error =
          assertThrows(ApiUnauthenticatedException.class, () -> auth.authenticate(supplied));
      assertFalse(error.getMessage().contains(supplied));
      assertFalse(error.getMessage().contains(owner));
    }
  }

  @Test
  void s20_validBearerAuthenticatesOwnerAndIndependentScopesWithoutWriting() {
    var jdbc = ApiCredentialPersistenceTest.Database.JDBC;
    var owner = "bearer-" + UUID.randomUUID();
    var store =
        new PostgresApiCredentialStore(jdbc, ApiCredentialPersistenceTest.Database.TRANSACTIONS);
    var clock = Clock.fixed(Instant.parse("2026-09-08T12:00:00Z"), ZoneOffset.UTC);
    var created =
        new CreateApiCredential(store, clock, new SecureRandom())
            .create(
                owner, UUID.randomUUID(), "Consumer", List.of("projects:write", "agenda:read"), 7);
    var before =
        jdbc.queryForObject(
            "SELECT row_to_json(c)::text || xmin::text || ctid::text FROM api_credentials c WHERE id=?",
            String.class,
            created.credential().id());
    AuthenticateApiCredentialUseCase authenticate =
        new AuthenticateApiCredential(store, clock, owner::equals);
    var access = authenticate.authenticate(created.secret());
    assertEquals(created.credential().id(), access.id());
    assertEquals(owner, access.owner());
    assertEquals(List.of("projects:write", "agenda:read"), access.scopes());
    assertEquals(
        before,
        jdbc.queryForObject(
            "SELECT row_to_json(c)::text || xmin::text || ctid::text FROM api_credentials c WHERE id=?",
            String.class,
            created.credential().id()));
  }
}
