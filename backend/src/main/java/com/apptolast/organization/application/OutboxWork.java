package com.apptolast.organization.application;

import com.apptolast.organization.domain.OutboxMessage;
import com.apptolast.organization.domain.PublicationAttempt;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public interface OutboxWork {
  /** Claims one eligible row exclusively; callback and recorded result commit together. */
  Optional<PublicationAttempt> processNext(
      Instant now, Set<UUID> excluded, Function<OutboxMessage, PublicationAttempt> operation);
}
