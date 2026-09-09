package com.apptolast.organization.application;

import static org.junit.jupiter.api.Assertions.*;

import com.apptolast.organization.domain.WebhookEndpoint;
import com.apptolast.organization.domain.WebhookInvalidException;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CreateWebhookTest {
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-09-08T10:00:00.000000Z"), ZoneOffset.UTC);
  private static final WebhookSecrets KEYED =
      new WebhookSecrets() {
        @Override
        public boolean available() {
          return true;
        }

        @Override
        public byte[] encrypt(UUID endpointId, String secret) {
          return ("cipher:" + endpointId + ":" + secret).getBytes();
        }

        @Override
        public String decrypt(UUID endpointId, byte[] ciphertext) {
          return new String(ciphertext).substring(("cipher:" + endpointId + ":").length());
        }
      };
  private static final WebhookDestinationGuard PUBLIC_ONLY =
      new WebhookDestinationGuard(
          host ->
              switch (host) {
                case "example.com" -> new InetAddress[] {InetAddress.getByName("203.0.113.5")};
                case "mixed.example" ->
                    new InetAddress[] {
                      InetAddress.getByName("203.0.113.5"), InetAddress.getByName("10.0.0.5")
                    };
                default -> throw new java.net.UnknownHostException(host);
              },
          AddressPolicy::isBlocked);

  static final class Endpoints implements WebhookEndpoints {
    final List<WebhookEndpoint> stored = new ArrayList<>();
    final List<byte[]> ciphertexts = new ArrayList<>();
    int limit = 5;

    @Override
    public void insert(String owner, WebhookEndpoint endpoint, byte[] secretCiphertext) {
      if (stored.size() >= limit)
        throw new WebhookOperationException(WebhookOperationException.Code.LIMIT);
      stored.add(endpoint);
      ciphertexts.add(secretCiphertext);
    }

    @Override
    public List<WebhookEndpoint> list(String owner) {
      return stored;
    }

    @Override
    public java.util.Optional<WebhookEndpoint> find(String owner, UUID id) {
      return stored.stream().filter(item -> item.id().equals(id)).findFirst();
    }

    @Override
    public java.util.Optional<WebhookEndpoint> changeStatus(
        String owner, UUID id, String status, Instant now) {
      return java.util.Optional.empty();
    }

    @Override
    public boolean delete(String owner, UUID id) {
      return false;
    }
  }

  @Test
  void s1_creationReturnsOneTimeSecretAndActiveEndpointStampedWithTheClock() {
    var endpoints = new Endpoints();
    var creation =
        new CreateWebhook(endpoints, KEYED, PUBLIC_ONLY, CLOCK, new SecureRandom())
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
    assertEquals(creation.secret(), KEYED.decrypt(endpoint.id(), endpoints.ciphertexts.getFirst()));
    assertFalse(creation.toString().contains("whsec_"), "secret must not leak through toString");
  }

  @Test
  void s1_secretsAreUniquePerCreation() {
    var endpoints = new Endpoints();
    var create = new CreateWebhook(endpoints, KEYED, PUBLIC_ONLY, CLOCK, new SecureRandom());
    var first = create.create("owner-a", "https://example.com/a", null, List.of("TaskCreated.v1"));
    var second = create.create("owner-a", "https://example.com/b", null, List.of("TaskCreated.v1"));
    assertNotEquals(first.secret(), second.secret());
    assertNotEquals(first.endpoint().id(), second.endpoint().id());
  }

  @Test
  void s5_blockedOrUnresolvableDestinationsAreRejectedWithoutInserting() {
    var endpoints = new Endpoints();
    var create = new CreateWebhook(endpoints, KEYED, PUBLIC_ONLY, CLOCK, new SecureRandom());
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
    var full = new Endpoints();
    full.limit = 0;
    var disabled =
        new CreateWebhook(full, WebhookSecrets.DISABLED, PUBLIC_ONLY, CLOCK, new SecureRandom());
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
    var keyed = new CreateWebhook(full, KEYED, PUBLIC_ONLY, CLOCK, new SecureRandom());
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
