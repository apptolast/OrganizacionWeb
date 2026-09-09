package com.apptolast.organization.application;

import com.apptolast.organization.domain.WebhookEndpoint;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebhookEndpoints {
  /** Inserts under the owner's quota lock; throws LIMIT when five endpoints already exist. */
  void insert(String owner, WebhookEndpoint endpoint, byte[] secretCiphertext);

  List<WebhookEndpoint> list(String owner);

  Optional<WebhookEndpoint> find(String owner, UUID id);

  Optional<WebhookEndpoint> changeStatus(String owner, UUID id, String status, Instant now);

  boolean delete(String owner, UUID id);
}
