package com.apptolast.organization.application;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.domain.WebhookInvalidException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class CreateWebhookTest {
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-09-08T10:00:00.000000Z"), ZoneOffset.UTC);
  static final WebhookDestinationGuard PUBLIC_ONLY =
      new WebhookDestinationGuard(
          CreateWebhookTest::resolve, AddressPolicy.blockingPrivateAddresses());

  private static InetAddress[] resolve(String host) throws UnknownHostException {
    return switch (host) {
      case "example.com" -> new InetAddress[] {InetAddress.getByName("203.0.113.5")};
      case "mixed.example" ->
          new InetAddress[] {
            InetAddress.getByName("203.0.113.5"), InetAddress.getByName("10.0.0.5")
          };
      default -> throw new UnknownHostException(host);
    };
  }

  private static CreateWebhook create(FakeWebhookEndpoints endpoints, WebhookSecrets secrets) {
    return new CreateWebhook(endpoints, secrets, PUBLIC_ONLY, CLOCK, new SecureRandom());
  }

  @Test
  void s1_creationReturnsOneTimeSecretAndActiveEndpointStampedWithTheClock() {
    var endpoints = new FakeWebhookEndpoints();
    var creation =
        create(endpoints, FakeWebhookSecrets.KEYED)
            .create(
                "owner-a",
                "https://example.com/hooks",
                "  Mi hook  ",
                List.of("TaskStatusChanged.v1", "TaskCreated.v1"));
    var endpoint = creation.endpoint();
    assertEquals("https://example.com/hooks", endpoint.url());
    assertEquals("Mi hook", endpoint.description());
    assertEquals(List.of("TaskCreated.v1", "TaskStatusChanged.v1"), endpoint.eventTypes());
    assertEquals("active", endpoint.status());
    assertNull(endpoint.disabledReason());
    assertNull(endpoint.disabledAt());
    assertEquals(Instant.parse("2026-09-08T10:00:00Z"), endpoint.createdAt());
    assertEquals(endpoint.createdAt(), endpoint.updatedAt());
    assertTrue(creation.secret().matches("whsec_[A-Za-z0-9_-]{43}"), creation.secret());
    assertEquals(List.of(endpoint), endpoints.stored);
    assertFalse(creation.toString().contains("whsec_"), "secret must not leak through toString");
  }

  @Test
  void s1_s8_b6_theStoredCiphertextIsBoundToTheOwnerAndTheEndpoint() {
    var endpoints = new FakeWebhookEndpoints();
    var creation =
        create(endpoints, FakeWebhookSecrets.KEYED)
            .create("owner-a", "https://example.com/hooks", "", List.of("TaskCreated.v1"));
    var stored = endpoints.ciphertexts.getFirst();
    assertEquals(
        creation.secret(),
        FakeWebhookSecrets.KEYED.decrypt("owner-a", creation.endpoint().id(), stored));
    assertThrows(
        IllegalStateException.class,
        () -> FakeWebhookSecrets.KEYED.decrypt("owner-b", creation.endpoint().id(), stored));
  }

  @Test
  void s1_secretsAreUniquePerCreation() {
    var create = create(new FakeWebhookEndpoints(), FakeWebhookSecrets.KEYED);
    var first = create.create("owner-a", "https://example.com/a", null, List.of("TaskCreated.v1"));
    var second = create.create("owner-a", "https://example.com/b", null, List.of("TaskCreated.v1"));
    assertNotEquals(first.secret(), second.secret());
    assertNotEquals(first.endpoint().id(), second.endpoint().id());
  }

  @Test
  void s5_blockedOrUnresolvableDestinationsAreRejectedWithoutInserting() {
    var endpoints = new FakeWebhookEndpoints();
    var create = create(endpoints, FakeWebhookSecrets.KEYED);
    for (var url :
        List.of(
            "https://127.0.0.1/h",
            "https://10.1.2.3/h",
            "https://[::1]/h",
            "https://[::ffff:10.0.0.1]/h",
            "https://mixed.example/h")) {
      var error =
          assertThrows(
              WebhookOperationException.class,
              () -> create.create("owner-a", url, "", List.of("TaskCreated.v1")));
      assertEquals(WebhookOperationException.Code.URL_BLOCKED, error.code(), url);
    }
    var unresolvable =
        assertThrows(
            WebhookOperationException.class,
            () ->
                create.create(
                    "owner-a", "https://missing.example/h", "", List.of("TaskCreated.v1")));
    assertEquals(WebhookOperationException.Code.URL_UNRESOLVABLE, unresolvable.code());
    assertTrue(endpoints.stored.isEmpty());
  }

  @Test
  void s34_errorsResolveInTheFixedOrderValuesKeyDestinationThenQuota() {
    var full = new FakeWebhookEndpoints();
    full.limit = 0;
    var disabled = create(full, WebhookSecrets.DISABLED);
    assertThrows(
        WebhookInvalidException.class,
        () -> disabled.create("owner-a", "http://10.0.0.1/h", "", List.of("TaskCreated.v1")));
    assertEquals(
        WebhookOperationException.Code.CONNECTORS_DISABLED,
        assertThrows(
                WebhookOperationException.class,
                () ->
                    disabled.create("owner-a", "https://10.0.0.1/h", "", List.of("TaskCreated.v1")))
            .code());
    var keyed = create(full, FakeWebhookSecrets.KEYED);
    assertEquals(
        WebhookOperationException.Code.URL_BLOCKED,
        assertThrows(
                WebhookOperationException.class,
                () -> keyed.create("owner-a", "https://10.0.0.1/h", "", List.of("TaskCreated.v1")))
            .code());
    assertEquals(
        WebhookOperationException.Code.LIMIT,
        assertThrows(
                WebhookOperationException.class,
                () ->
                    keyed.create("owner-a", "https://example.com/h", "", List.of("TaskCreated.v1")))
            .code());
    assertTrue(full.stored.isEmpty());
  }
}
