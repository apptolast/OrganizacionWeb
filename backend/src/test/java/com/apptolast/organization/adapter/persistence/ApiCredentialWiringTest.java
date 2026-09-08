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
