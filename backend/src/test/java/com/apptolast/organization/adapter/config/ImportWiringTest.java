package com.apptolast.organization.adapter.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.apptolast.organization.application.ApplyImportDataUseCase;
import com.apptolast.organization.application.ExportDataUseCase;
import com.apptolast.organization.application.ImportDataUseCase;
import com.apptolast.organization.application.ReadImportReceiptUseCase;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
    properties = {
      "app.auth.username=owner", "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example", "app.publisher.enabled=false"
    })
@Testcontainers
class ImportWiringTest {
  @Container
  static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.9-alpine");

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry properties) {
    properties.add("spring.datasource.url", postgres::getJdbcUrl);
    properties.add("spring.datasource.username", postgres::getUsername);
    properties.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired ExportDataUseCase export;
  @Autowired ImportDataUseCase preview;
  @Autowired ApplyImportDataUseCase apply;
  @Autowired ReadImportReceiptUseCase read;
  @Autowired JdbcTemplate jdbc;

  @Test
  void s1_s18_s22_realBeansPreviewConfirmAndRecoverAnEmptyOwnExport() throws Exception {
    var owner = "import-wiring";
    var bytes = new ByteArrayOutputStream();
    export.prepare(owner).writeTo(bytes);
    var prepared = preview.preview(owner, new ByteArrayInputStream(bytes.toByteArray()));
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM import_receipts WHERE owner_id=?", Integer.class, owner))
        .isZero();
    var key = UUID.randomUUID();
    var receipt =
        apply.apply(
            owner, key, prepared.fileSha256(), new ByteArrayInputStream(bytes.toByteArray()));
    assertThat(receipt.outcome()).isEqualTo("NO_CHANGE");
    assertThat(receipt.byteLength()).isEqualTo(bytes.size());
    assertThat(read.find(owner, key)).contains(receipt);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM outbox_events WHERE owner_id=?", Integer.class, owner))
        .isZero();
  }
}
