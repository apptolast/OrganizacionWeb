package com.apptolast.organization.application;

import com.apptolast.organization.domain.WebhookEndpoint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** In-memory endpoint store honouring the owner quota, used by the application tests. */
class FakeWebhookEndpoints implements WebhookEndpoints {
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
    return List.copyOf(stored);
  }

  @Override
  public Optional<WebhookEndpoint> find(String owner, UUID id) {
    return stored.stream().filter(endpoint -> endpoint.id().equals(id)).findFirst();
  }

  @Override
  public Optional<WebhookEndpoint> changeStatus(String owner, UUID id, String status, Instant now) {
    return find(owner, id)
        .map(
            endpoint -> {
              var changed = endpoint.withStatus(status, now);
              stored.set(stored.indexOf(endpoint), changed);
              return changed;
            });
  }

  @Override
  public boolean delete(String owner, UUID id) {
    return stored.removeIf(endpoint -> endpoint.id().equals(id));
  }
}
