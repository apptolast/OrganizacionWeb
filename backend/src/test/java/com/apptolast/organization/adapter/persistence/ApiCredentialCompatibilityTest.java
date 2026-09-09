package com.apptolast.organization.adapter.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.application.*;
import java.io.*;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;

class ApiCredentialCompatibilityTest {
  @Test
  void s42_additiveUpgradePreservesBusinessAndExportImportNeverCopiesCredentials()
      throws Exception {
    var parent = ApiCredentialPersistenceTest.Database.JDBC;
    var pg = ApiCredentialPersistenceTest.Database.PG;
    var database = "api_upgrade_" + UUID.randomUUID().toString().replace("-", "");
    parent.execute("CREATE DATABASE " + database);
    try {
      var source =
          new DriverManagerDataSource(
              pg.getJdbcUrl().replace("/" + pg.getDatabaseName(), "/" + database),
              pg.getUsername(),
              pg.getPassword());
      Flyway.configure().dataSource(source).target("21").load().migrate();
      var jdbc = new JdbcTemplate(source);
      var transactions = new DataSourceTransactionManager(source);
      var owner = "compat-" + UUID.randomUUID();
      var project = UUID.randomUUID();
      jdbc.update(
          "INSERT INTO projects(id,owner_id,name,description,status,created_at,updated_at) VALUES (?,?,'Business','','idea','2026-09-08Z','2026-09-08Z')",
          project,
          owner);
      var columns =
          jdbc.queryForList(
              "SELECT table_name,column_name,data_type FROM information_schema.columns WHERE table_schema='public' ORDER BY table_name,ordinal_position");
      var before =
          jdbc.queryForObject(
              "SELECT row_to_json(p)::text||xmin::text||ctid::text FROM projects p WHERE id=?",
              String.class,
              project);
      var clock = Clock.fixed(Instant.parse("2026-09-08T12:00:00Z"), ZoneOffset.UTC);
      var export = new PrepareExportData(new PostgresExportDataQueries(jdbc, transactions), clock);
      var original = new ByteArrayOutputStream();
      export.prepare(owner).writeTo(original);
      Flyway.configure().dataSource(source).load().migrate();
      // The claim is that the upgrade is ADDITIVE: no table that already existed at V21
      // loses or moves a column. Comparing against the tables captured before the upgrade
      // says exactly that, and does not need a denylist that every later feature has to
      // edit (V22 credentials, V23 webhooks, V24 calendar...) and that fails whenever
      // someone forgets.
      var priorTables =
          columns.stream().map(row -> (String) row.get("table_name")).distinct().toList();
      assertEquals(
          columns,
          jdbc
              .queryForList(
                  "SELECT table_name,column_name,data_type FROM information_schema.columns WHERE table_schema='public' ORDER BY table_name,ordinal_position")
              .stream()
              .filter(row -> priorTables.contains((String) row.get("table_name")))
              .toList());
      assertEquals(
          before,
          jdbc.queryForObject(
              "SELECT row_to_json(p)::text||xmin::text||ctid::text FROM projects p WHERE id=?",
              String.class,
              project));
      var store = new PostgresApiCredentialStore(jdbc, transactions);
      var credential =
          new CreateApiCredential(store, clock, new SecureRandom())
              .create(owner, UUID.randomUUID(), "Private integration", List.of("tasks:read"), 7);
      var access =
          new AuthenticateApiCredential(store, clock, owner::equals)
              .authenticate(credential.secret());
      new ConsumeApiQuota(store, clock, owner::equals).consume(access);
      var durable =
          jdbc.queryForObject(
              "SELECT row_to_json(c)::text||xmin::text||ctid::text FROM api_credentials c WHERE id=?",
              String.class,
              access.id());
      var ownerQuota =
          jdbc.queryForObject(
              "SELECT row_to_json(q)::text||xmin::text||ctid::text FROM api_owner_quotas q WHERE owner_id=?",
              String.class,
              owner);
      var tokenQuota =
          jdbc.queryForObject(
              "SELECT row_to_json(q)::text||xmin::text||ctid::text FROM api_credential_quotas q WHERE credential_id=?",
              String.class,
              access.id());
      var after = new ByteArrayOutputStream();
      export.prepare(owner).writeTo(after);
      assertArrayEquals(original.toByteArray(), after.toByteArray());
      var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(after.toByteArray());
      assertEquals(14, json.get("data").size());
      assertEquals(14, json.get("counts").size());
      assertFalse(
          after.toString(java.nio.charset.StandardCharsets.UTF_8).contains(credential.secret()));
      var imports = new PostgresImportDataStore(jdbc, transactions);
      var preview =
          new PreviewImportData(imports)
              .preview(owner, new ByteArrayInputStream(after.toByteArray()));
      var receipt =
          new ApplyImportData(imports, clock)
              .apply(
                  owner,
                  UUID.randomUUID(),
                  preview.fileSha256(),
                  new ByteArrayInputStream(after.toByteArray()));
      assertEquals("NO_CHANGE", receipt.outcome());
      assertEquals(
          durable,
          jdbc.queryForObject(
              "SELECT row_to_json(c)::text||xmin::text||ctid::text FROM api_credentials c WHERE id=?",
              String.class,
              access.id()));
      assertEquals(
          ownerQuota,
          jdbc.queryForObject(
              "SELECT row_to_json(q)::text||xmin::text||ctid::text FROM api_owner_quotas q WHERE owner_id=?",
              String.class,
              owner));
      assertEquals(
          tokenQuota,
          jdbc.queryForObject(
              "SELECT row_to_json(q)::text||xmin::text||ctid::text FROM api_credential_quotas q WHERE credential_id=?",
              String.class,
              access.id()));
      assertEquals(
          0,
          jdbc.queryForObject(
              "SELECT count(*) FROM outbox_events WHERE owner_id=?", Integer.class, owner));
    } finally {
      parent.execute("DROP DATABASE " + database);
    }
  }
}
