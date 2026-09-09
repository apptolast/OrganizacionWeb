package com.apptolast.organization.adapter.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.application.CreateWebhookUseCase;
import com.apptolast.organization.application.ManageWebhookUseCase;
import com.apptolast.organization.application.WebhookOperationException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** The real beans, over the real schema: creating and reading a webhook end to end. */
@SpringBootTest(
    properties = {
      "app.auth.username=owner",
      "app.auth.password=test-only-secret",
      "app.public-origin=https://organization.example",
      "app.publisher.enabled=false",
      "app.connectors.key=AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="
    })
class WebhookWiringTest {
  @DynamicPropertySource
  static void database(DynamicPropertyRegistry properties) {
    var pg = WebhookPersistenceTest.Database.PG;
    properties.add("spring.datasource.url", pg::getJdbcUrl);
    properties.add("spring.datasource.username", pg::getUsername);
    properties.add("spring.datasource.password", pg::getPassword);
  }

  @Autowired CreateWebhookUseCase create;
  @Autowired ManageWebhookUseCase manage;

  @Test
  void s1_s8_theRealBeansCreateReadAndKeepTheSecretOutOfEveryRead() {
    var owner = "wiring-" + UUID.randomUUID();
    var created =
        create.create(owner, "https://example.com/hooks", " Mi hook ", List.of("TaskCreated.v1"));

    assertTrue(created.secret().startsWith("whsec_"));
    assertEquals(43, created.secret().substring("whsec_".length()).length());
    assertEquals("Mi hook", created.endpoint().description());
    assertEquals("active", created.endpoint().status());
    assertEquals(
        List.of(created.endpoint().id()),
        manage.list(owner).stream().map(e -> e.id()).toList());
    assertFalse(manage.list(owner).toString().contains(created.secret()));
    assertFalse(created.toString().contains(created.secret()));
  }

  @Test
  void s5_aBlockedDestinationIsRejectedBeforeAnyInsert() {
    var owner = "blocked-" + UUID.randomUUID();
    assertEquals(
        WebhookOperationException.Code.URL_BLOCKED,
        assertThrows(
                WebhookOperationException.class,
                () -> create.create(owner, "https://10.0.0.1/h", "", List.of("TaskCreated.v1")))
            .code());
    assertTrue(manage.list(owner).isEmpty());
  }
}
