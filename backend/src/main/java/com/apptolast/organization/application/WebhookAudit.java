package com.apptolast.organization.application;

import java.util.UUID;

/**
 * The webhook audit trail.
 *
 * <p>Every parameter here is an identifier or a short stable code, on purpose: there is no
 * parameter through which a target URL, a secret, a signature or a response body could reach the
 * log, so @s35 holds structurally rather than by the care of each caller.
 */
public interface WebhookAudit {
  /** One finished attempt, by its endpoint, its event, the resulting status and the error class. */
  void attempt(UUID endpointId, UUID eventId, String status, String errorClass);

  /** An outbox row that produced no delivery: INVALID_EVENT or UNSUPPORTED_EVENT. */
  void discarded(UUID endpointId, UUID eventId, String code);

  /** A worker-level problem, such as CONFIGURATION_ERROR at startup. */
  void workerError(String code);
}
