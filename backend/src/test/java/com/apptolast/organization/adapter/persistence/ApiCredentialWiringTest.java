package com.apptolast.organization.adapter.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.application.CreateApiCredentialUseCase;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example",
      "app.publisher.enabled=false"
    })
class ApiCredentialWiringTest {
  @DynamicPropertySource
  static void database(DynamicPropertyRegistry properties) {
    var pg = ApiCredentialPersistenceTest.Database.PG;
    properties.add("spring.datasource.url", pg::getJdbcUrl);
    properties.add("spring.datasource.username", pg::getUsername);
    properties.add("spring.datasource.password", pg::getPassword);
  }

  @Autowired CreateApiCredentialUseCase create;
  @Autowired JdbcTemplate jdbc;

  @Autowired com.apptolast.organization.application.ReadApiCredentialsUseCase read;
  @Autowired com.apptolast.organization.application.RevokeApiCredentialUseCase revoke;

  @Autowired com.apptolast.organization.application.AuthenticateApiCredentialUseCase authenticate;
  @Autowired com.apptolast.organization.application.ConsumeApiQuotaUseCase consume;

  @Autowired org.springframework.security.core.userdetails.UserDetailsService users;

  @Test
  void s21_realDisabledBootstrapUserCannotAuthenticateOrConsume() {
    var created = create.create("owner", UUID.randomUUID(), "Disabled", List.of("tasks:read"), 7);
    var access = authenticate.authenticate(created.secret());
    var original = users.loadUserByUsername("owner");
    var manager = (org.springframework.security.provisioning.UserDetailsManager) users;
    var before =
        jdbc.queryForList(
            "SELECT row_to_json(q)::text||xmin::text||ctid::text FROM api_owner_quotas q WHERE owner_id='owner'",
            String.class);
    try {
      manager.updateUser(
          org.springframework.security.core.userdetails.User.withUserDetails(original)
              .disabled(true)
              .build());
      assertThrows(
          com.apptolast.organization.application.ApiUnauthenticatedException.class,
          () -> authenticate.authenticate(created.secret()));
      assertThrows(
          com.apptolast.organization.application.ApiUnauthenticatedException.class,
          () -> consume.consume(access));
      assertEquals(
          before,
          jdbc.queryForList(
              "SELECT row_to_json(q)::text||xmin::text||ctid::text FROM api_owner_quotas q WHERE owner_id='owner'",
              String.class));
      assertEquals(
          0,
          jdbc.queryForObject(
              "SELECT count(*) FROM api_credential_quotas WHERE credential_id=?",
              Integer.class,
              access.id()));
    } finally {
      manager.updateUser(original);
    }
  }

  @Test
  void s20_s26_realAuthenticationAndQuotaBeansUseEnabledBootstrapOwner() {
    var created =
        create.create("owner", UUID.randomUUID(), "Bootstrap", List.of("projects:write"), 7);
    var access = authenticate.authenticate(created.secret());
    assertEquals("owner", access.owner());
    assertEquals(List.of("projects:write"), access.scopes());
    consume.consume(access);
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT used FROM api_credential_quotas WHERE credential_id=?",
            Integer.class,
            access.id()));
    var orphan =
        create.create("other-owner", UUID.randomUUID(), "Orphan", List.of("projects:read"), 7);
    assertThrows(
        com.apptolast.organization.application.ApiUnauthenticatedException.class,
        () -> authenticate.authenticate(orphan.secret()));
  }

  @Test
  void s12_s13_s15_realBeansReadListAndRevoke() {
    var owner = "management-wiring-" + UUID.randomUUID();
    var created =
        create.create(owner, UUID.randomUUID(), "Management", List.of("projects:read"), 7);
    assertEquals(Optional.of(created.credential()), read.find(owner, created.credential().id()));
    assertEquals(List.of(created.credential()), read.list(owner, null).items());
    var revoked = revoke.revoke(owner, created.credential().id()).orElseThrow();
    assertNotNull(revoked.revokedAt());
    assertEquals(Optional.of(revoked), read.find(owner, revoked.id()));
  }

  @Test
  void s1_realBeanCreatesDurableCredential() {
    var owner = "wiring-" + UUID.randomUUID();
    var id = UUID.randomUUID();
    var result = create.create(owner, id, "Real bean", List.of("history:read"), 90);
    assertNotNull(result.secret());
    assertEquals(id, result.credential().id());
    assertEquals(
        1,
        jdbc.queryForObject(
            "SELECT count(*) FROM api_credentials WHERE owner_id=?", Integer.class, owner));
  }
}
